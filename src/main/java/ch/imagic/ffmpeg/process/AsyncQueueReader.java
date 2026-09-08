package ch.imagic.ffmpeg.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

class AsyncQueueReader extends InputStream {

    private static final int MAX_DATA_IN_QUEUE = 1_000_000;
    private static final byte[] WAKEUP_GUARD = new byte[0];
    private final InputStream originalStream;

    private volatile IOException error = null;
    private volatile boolean eof = false;
    private volatile boolean closed = false;
    private final AtomicLong dataInQueue = new AtomicLong(0);
    private byte[] current = WAKEUP_GUARD;
    private int currentIndex;
    private final TransferQueue<byte[]> queue = new LinkedTransferQueue<>();
    private final CountDownLatch terminated = new CountDownLatch(1);

    AsyncQueueReader(Executor executor, InputStream input, Consumer<byte[]> logger) {
        this.originalStream = input;
        Thread thread = Thread.currentThread();
        executor.execute(() -> {
            Thread thread2 = Thread.currentThread();
            if (thread == thread2) {
                throw new IllegalArgumentException("bad executor");
            }
            byte[] buffer = new byte[0x1_0000];
            try (input) {
                while (!closed) {
                    int count = input.read(buffer);
                    if (count < 0) {
                        feedEof();
                        return;
                    }
                    if (count == 0) {
                        continue;
                    }
                    byte[] recevied = new byte[count];
                    System.arraycopy(buffer, 0, recevied, 0, count);
                    try {
                        logger.accept(recevied);
                    } catch (Throwable e) {
                        // Don't care, if logger itself fails, loggers that fail are silly.
                    }
                    feed(recevied);
                }
            } catch (IOException e) {
                feedError(e);
            } catch (InterruptedException e) {
                feedError(new InterruptedIOException());
            } finally {
                terminated.countDown();
            }
        });
    }

    boolean awaitAsyncTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return terminated.await(timeout, unit);
    }

    @Override
    public int read() throws IOException {
        byte[] buffer = new byte[1];
        int i = read(buffer, 0, 1);
        if (i < 0) {
            return -1;
        }
        return buffer[0] & 0xFF;
    }

    @Override
    public synchronized int read(byte[] cbuf, int off, int len) throws IOException {
        while (true) {
            if (closed) {
                throw new IOException("Reader closed");
            }
            if (len <= 0) {
                return 0;
            }

            if (currentIndex < current.length) {
                int toCopy = Math.min(current.length - currentIndex, len);
                System.arraycopy(current, currentIndex, cbuf, off, toCopy);
                currentIndex += toCopy;
                return toCopy;
            }

            byte[] next = queue.poll();
            if (next == null) {
                if (error != null) {
                    throw error;
                }
                if (eof) {
                    return -1;
                }
                try {
                    next = queue.take();
                } catch (InterruptedException e) {
                    throw new InterruptedIOException();
                }
            }

            if (next.length == 0) {
                if (error != null) {
                    throw error;
                }
                // WAKEUP_GUARD
                eof = true;
                return -1;
            }
            dataInQueue.addAndGet(-next.length);
            current = next;
            currentIndex = 0;
        }
    }

    private void feedEof() {
        queue.add(WAKEUP_GUARD);
    }

    private void feedError(IOException error) {
        this.error = error;
        queue.add(WAKEUP_GUARD);
    }

    private void feed(byte[] data) throws InterruptedException {
        if (data == null || data.length == 0) {
            return;
        }

        if (closed || eof || error != null) {
            return;
        }
        while (dataInQueue.get() > MAX_DATA_IN_QUEUE) {
            if (!queue.tryTransfer(data, 10, TimeUnit.SECONDS)) {
                if (closed || eof) {
                    return;
                }
                continue;
            }
            dataInQueue.addAndGet(data.length);
            return;
        }

        queue.add(data);
        dataInQueue.addAndGet(data.length);
    }

    @Override
    public void close() throws IOException {
        closed = true;
        queue.clear();
        queue.add(WAKEUP_GUARD);
        originalStream.close();
    }
}
