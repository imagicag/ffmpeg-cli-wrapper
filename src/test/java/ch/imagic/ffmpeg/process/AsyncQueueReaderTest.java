package ch.imagic.ffmpeg.process;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class AsyncQueueReaderTest {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @AfterEach
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void preservesAllDataBeforeEof() throws IOException {
        byte[] expected = new byte[200_000];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = (byte) i;
        }

        try (InputStream reader = new AsyncQueueReader(executor, new ByteArrayInputStream(expected), ignored -> {})) {
            assertArrayEquals(expected, reader.readAllBytes());
            assertEquals(-1, reader.read());
        }
    }

    @Test
    public void loggerFailureDoesNotInterruptStream() throws IOException {
        byte[] expected = new byte[100_000];
        Arrays.fill(expected, (byte) 42);

        try (InputStream reader = new AsyncQueueReader(executor, new ByteArrayInputStream(expected), ignored -> {
            throw new IllegalStateException("logger failed");
        })) {
            assertArrayEquals(expected, reader.readAllBytes());
        }
    }

    @Test
    public void closeClosesOriginalStream() throws IOException {
        CloseTrackingInputStream input = new CloseTrackingInputStream();
        AsyncQueueReader reader = new AsyncQueueReader(executor, input, ignored -> {});

        reader.close();

        assertTrue(input.closed);
    }

    @Test
    public void awaitsWorkerTermination() throws IOException, InterruptedException {
        AsyncQueueReader reader =
                new AsyncQueueReader(executor, new ByteArrayInputStream(new byte[] {1}), ignored -> {});

        assertArrayEquals(new byte[] {1}, reader.readAllBytes());

        assertTrue(reader.awaitAsyncTermination(1, TimeUnit.SECONDS));
        reader.close();
    }

    @Test
    public void awaitsWorkerWhileConsumerIsRunning() throws IOException, InterruptedException {
        CountDownLatch consumerStarted = new CountDownLatch(1);
        AsyncQueueReader reader = new AsyncQueueReader(executor, new ByteArrayInputStream(new byte[] {1}), ignored -> {
            consumerStarted.countDown();
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(consumerStarted.await(1, TimeUnit.SECONDS));
        assertFalse(reader.awaitAsyncTermination(200, TimeUnit.MILLISECONDS));

        long start = System.nanoTime();
        assertTrue(reader.awaitAsyncTermination(5, TimeUnit.SECONDS));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue(elapsedMillis >= 1_000, "Expected to wait at least one second, waited " + elapsedMillis + " ms");
        reader.close();
    }

    @Test
    public void preservesQueuedBytesBeforeReportingInputFailure() throws Exception {
        byte[] expected = new byte[] {1, 2, 3, 4};
        AsyncQueueReader reader =
                new AsyncQueueReader(executor, new FailingAfterDataInputStream(expected), ignored -> {});
        assertTrue(reader.awaitAsyncTermination(1, TimeUnit.SECONDS));

        assertArrayEquals(expected, reader.readNBytes(expected.length));
        IOException failure = assertThrows(IOException.class, reader::read);
        assertEquals("input failed", failure.getMessage());
        reader.close();
    }

    @Test
    public void sustainsBackpressureWithoutDroppingData() throws Exception {
        byte[] expected = new byte[2_500_000];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = (byte) (i * 31);
        }
        AsyncQueueReader reader = new AsyncQueueReader(executor, new ByteArrayInputStream(expected), ignored -> {});

        Thread.sleep(100);
        assertArrayEquals(expected, reader.readAllBytes());
        assertTrue(reader.awaitAsyncTermination(1, TimeUnit.SECONDS));
        reader.close();
    }

    @Test
    public void interruptingBlockedReadThrowsInterruptedIo() throws Exception {
        BlockingInputStream input = new BlockingInputStream();
        AsyncQueueReader reader = new AsyncQueueReader(executor, input, ignored -> {});
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread reading = new Thread(() -> {
            try {
                reader.read();
            } catch (Throwable e) {
                failure.set(e);
            }
        });
        reading.start();
        assertTrue(input.readStarted.await(1, TimeUnit.SECONDS));

        reading.interrupt();
        reading.join(1_000);

        assertFalse(reading.isAlive());
        assertTrue(failure.get() instanceof InterruptedIOException);
        reader.close();
    }

    @Test
    public void closeUnblocksBlockedRead() throws Exception {
        BlockingInputStream input = new BlockingInputStream();
        AsyncQueueReader reader = new AsyncQueueReader(executor, input, ignored -> {});
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Integer> result = new AtomicReference<>();
        CountDownLatch reading = new CountDownLatch(1);
        Thread thread = new Thread(() -> {
            reading.countDown();
            try {
                result.set(reader.read());
            } catch (Throwable e) {
                failure.set(e);
            }
        });
        thread.start();
        assertTrue(reading.await(1, TimeUnit.SECONDS));
        assertTrue(input.readStarted.await(1, TimeUnit.SECONDS));

        reader.close();
        thread.join(1_000);

        assertFalse(thread.isAlive());
        assertTrue(Integer.valueOf(-1).equals(result.get()) || failure.get() instanceof IOException);
        if (failure.get() != null) {
            assertEquals("Reader closed", failure.get().getMessage());
        }
        IOException closed = assertThrows(IOException.class, reader::read);
        assertEquals("Reader closed", closed.getMessage());
        assertTrue(reader.awaitAsyncTermination(1, TimeUnit.SECONDS));
    }

    private static final class CloseTrackingInputStream extends ByteArrayInputStream {
        private boolean closed;

        private CloseTrackingInputStream() {
            super(new byte[0]);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    private static final class FailingAfterDataInputStream extends InputStream {
        private final byte[] data;
        private boolean delivered;

        private FailingAfterDataInputStream(byte[] data) {
            this.data = data;
        }

        @Override
        public int read() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read(byte[] target, int off, int len) throws IOException {
            if (delivered) {
                throw new IOException("input failed");
            }
            delivered = true;
            int count = Math.min(data.length, len);
            System.arraycopy(data, 0, target, off, count);
            return count;
        }
    }

    private static final class BlockingInputStream extends InputStream {
        private final CountDownLatch readStarted = new CountDownLatch(1);
        private boolean closed;

        @Override
        public synchronized int read() throws IOException {
            readStarted.countDown();
            while (!closed) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException();
                }
            }
            return -1;
        }

        @Override
        public synchronized void close() {
            closed = true;
            notifyAll();
        }
    }
}
