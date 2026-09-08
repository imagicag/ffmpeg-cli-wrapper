package ch.imagic.ffmpeg.process;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.imagic.ffmpeg.FFMpegLogger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class BasicFFMpegProcessTest {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @AfterEach
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void notifiesProcessDeathAfterRawCallbacksFinish() throws Exception {
        CountDownLatch rawCallbackStarted = new CountDownLatch(1);
        CountDownLatch releaseRawCallback = new CountDownLatch(1);
        CountDownLatch processDeath = new CountDownLatch(1);
        FFMpegLogger logger = new FFMpegLogger() {
            @Override
            public boolean wantsRawStdout() {
                return true;
            }

            @Override
            public boolean wantsProcessDeath() {
                return true;
            }

            @Override
            public void onRawStdout(long pid, byte[] rawData, int off, int len) {
                rawCallbackStarted.countDown();
                await(releaseRawCallback);
            }

            @Override
            public void onProcessDeath(long pid, int exitCode) {
                processDeath.countDown();
            }
        };
        BasicFFMpegProcess process = new BasicFFMpegProcess(executor, logger, new CompletedProcess());
        assertTrue(rawCallbackStarted.await(1, TimeUnit.SECONDS));

        Future<?> close = executor.submit(process::close);
        assertFalse(processDeath.await(100, TimeUnit.MILLISECONDS));

        releaseRawCallback.countDown();
        close.get(1, TimeUnit.SECONDS);
        assertTrue(processDeath.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void negativeAwaitWaitsIndefinitelyAndReturnsTrue() throws Exception {
        TrackingProcess underlying = new TrackingProcess();
        BasicFFMpegProcess process = new BasicFFMpegProcess(executor, FFMpegLogger.noop(), underlying);

        assertTrue(process.await(-1));

        assertEquals(1, underlying.indefiniteWaits.get());
        assertEquals(0, underlying.timedWaits.get());
    }

    @Test
    public void timedAwaitReturnsUnderlyingResult() throws Exception {
        TrackingProcess underlying = new TrackingProcess();
        underlying.timedWaitResult = false;
        BasicFFMpegProcess process = new BasicFFMpegProcess(executor, FFMpegLogger.noop(), underlying);

        assertFalse(process.await(25));

        assertEquals(0, underlying.indefiniteWaits.get());
        assertEquals(1, underlying.timedWaits.get());
        assertEquals(25, underlying.lastTimeoutMillis);
    }

    @Test
    public void zeroTimeoutChecksProcessImmediately() throws Exception {
        TrackingProcess running = new TrackingProcess();
        running.timedWaitResult = false;
        BasicFFMpegProcess runningProcess = new BasicFFMpegProcess(executor, FFMpegLogger.noop(), running);
        TrackingProcess complete = new TrackingProcess();
        complete.timedWaitResult = true;
        BasicFFMpegProcess completeProcess = new BasicFFMpegProcess(executor, FFMpegLogger.noop(), complete);

        assertFalse(runningProcess.await(0));
        assertTrue(completeProcess.await(0));
        assertEquals(0, running.indefiniteWaits.get());
        assertEquals(0, complete.indefiniteWaits.get());
        assertEquals(0, running.lastTimeoutMillis);
        assertEquals(0, complete.lastTimeoutMillis);
    }

    @Test
    public void concurrentCloseIsIdempotent() throws Exception {
        TrackingProcess underlying = new TrackingProcess();
        underlying.alive = true;
        BasicFFMpegProcess process = new BasicFFMpegProcess(executor, FFMpegLogger.noop(), underlying);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> closes = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            closes.add(executor.submit(() -> {
                await(start);
                process.close();
            }));
        }

        start.countDown();
        for (Future<?> close : closes) {
            close.get(1, TimeUnit.SECONDS);
        }

        assertEquals(1, underlying.destroyForciblyCalls.get());
        assertEquals(1, underlying.stdin.closeCalls.get());
        assertEquals(1, underlying.stdout.closeCalls.get());
        assertEquals(1, underlying.stderr.closeCalls.get());
        assertEquals(1, underlying.timedWaits.get());
    }

    @Test
    public void streamCloseFailuresDoNotPreventRemainingCleanup() {
        TrackingProcess underlying = new TrackingProcess();
        underlying.stdin.failOnClose = true;
        underlying.stdout.failOnClose = true;
        underlying.stderr.failOnClose = true;
        BasicFFMpegProcess process = new BasicFFMpegProcess(executor, FFMpegLogger.noop(), underlying);

        process.close();

        assertEquals(1, underlying.stdin.closeCalls.get());
        assertEquals(1, underlying.stdout.closeCalls.get());
        assertEquals(1, underlying.stderr.closeCalls.get());
        assertEquals(1, underlying.timedWaits.get());
    }

    @Test
    public void rawStdoutAndStderrAreDrainedBeforeDeathCallback() throws Exception {
        byte[] stdout = "stdout data".getBytes(StandardCharsets.UTF_8);
        byte[] stderr = "stderr data".getBytes(StandardCharsets.UTF_8);
        TrackingProcess underlying = new TrackingProcess(stdout, stderr);
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        ByteArrayOutputStream loggedStdout = new ByteArrayOutputStream();
        ByteArrayOutputStream loggedStderr = new ByteArrayOutputStream();
        FFMpegLogger logger = new FFMpegLogger() {
            @Override
            public boolean wantsRawStdout() {
                return true;
            }

            @Override
            public boolean wantsRawStderr() {
                return true;
            }

            @Override
            public boolean wantsProcessDeath() {
                return true;
            }

            @Override
            public void onRawStdout(long pid, byte[] data, int off, int len) {
                loggedStdout.write(data, off, len);
                events.add("stdout");
            }

            @Override
            public void onRawStderr(long pid, byte[] data, int off, int len) {
                loggedStderr.write(data, off, len);
                events.add("stderr");
            }

            @Override
            public void onProcessDeath(long pid, int exitCode) {
                events.add("death");
            }
        };
        BasicFFMpegProcess process = new BasicFFMpegProcess(executor, logger, underlying);

        assertTrue(underlying.stdout.firstRead.await(1, TimeUnit.SECONDS));
        assertTrue(underlying.stderr.firstRead.await(1, TimeUnit.SECONDS));

        process.close();

        assertArrayEquals(stdout, loggedStdout.toByteArray());
        assertArrayEquals(stderr, loggedStderr.toByteArray());
        assertEquals("death", events.get(events.size() - 1));
        assertTrue(events.indexOf("stdout") < events.indexOf("death"));
        assertTrue(events.indexOf("stderr") < events.indexOf("death"));
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class CompletedProcess extends Process {
        @Override
        public OutputStream getOutputStream() {
            return new ByteArrayOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(new byte[] {1});
        }

        @Override
        public InputStream getErrorStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public int waitFor() {
            return 0;
        }

        @Override
        public int exitValue() {
            return 0;
        }

        @Override
        public void destroy() {}

        @Override
        public boolean isAlive() {
            return false;
        }

        @Override
        public long pid() {
            return 123;
        }
    }

    private static final class TrackingProcess extends Process {
        private final TrackingOutputStream stdin = new TrackingOutputStream();
        private final TrackingInputStream stdout;
        private final TrackingInputStream stderr;
        private final AtomicInteger indefiniteWaits = new AtomicInteger();
        private final AtomicInteger timedWaits = new AtomicInteger();
        private final AtomicInteger destroyForciblyCalls = new AtomicInteger();
        private volatile boolean alive;
        private volatile boolean timedWaitResult = true;
        private volatile long lastTimeoutMillis;

        private TrackingProcess() {
            this(new byte[0], new byte[0]);
        }

        private TrackingProcess(byte[] stdout, byte[] stderr) {
            this.stdout = new TrackingInputStream(stdout);
            this.stderr = new TrackingInputStream(stderr);
        }

        @Override
        public OutputStream getOutputStream() {
            return stdin;
        }

        @Override
        public InputStream getInputStream() {
            return stdout;
        }

        @Override
        public InputStream getErrorStream() {
            return stderr;
        }

        @Override
        public int waitFor() {
            indefiniteWaits.incrementAndGet();
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) {
            timedWaits.incrementAndGet();
            lastTimeoutMillis = unit.toMillis(timeout);
            return timedWaitResult;
        }

        @Override
        public int exitValue() {
            if (alive) {
                throw new IllegalThreadStateException();
            }
            return 0;
        }

        @Override
        public void destroy() {
            alive = false;
        }

        @Override
        public Process destroyForcibly() {
            destroyForciblyCalls.incrementAndGet();
            alive = false;
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public long pid() {
            return 456;
        }
    }

    private static final class TrackingInputStream extends ByteArrayInputStream {
        private final AtomicInteger closeCalls = new AtomicInteger();
        private final CountDownLatch firstRead = new CountDownLatch(1);
        private volatile boolean failOnClose;

        private TrackingInputStream(byte[] data) {
            super(data);
        }

        @Override
        public synchronized int read(byte[] data, int off, int len) {
            firstRead.countDown();
            return super.read(data, off, len);
        }

        @Override
        public void close() throws IOException {
            closeCalls.incrementAndGet();
            if (failOnClose) {
                throw new IOException("close failed");
            }
            super.close();
        }
    }

    private static final class TrackingOutputStream extends ByteArrayOutputStream {
        private final AtomicInteger closeCalls = new AtomicInteger();
        private volatile boolean failOnClose;

        @Override
        public void close() throws IOException {
            closeCalls.incrementAndGet();
            if (failOnClose) {
                throw new IOException("close failed");
            }
            super.close();
        }
    }
}
