package ch.imagic.ffmpeg.process;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.imagic.ffmpeg.FFMpegLogger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
}
