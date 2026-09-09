package ch.imagic.ffmpeg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.imagic.ffmpeg.process.FFMpegProcess;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

public class BasicFFMpegJobTest {
    @Test
    public void awaitTimesOutWhileJobIsIncomplete() throws Exception {
        BasicFFMpegJob<String> job = new BasicFFMpegJob<>(new CompletableFuture<>(), new TrackingProcess());

        assertFalse(job.await(10, TimeUnit.MILLISECONDS));
    }

    @Test
    public void zeroTimeoutChecksCompletionImmediately() throws Exception {
        BasicFFMpegJob<String> incomplete = new BasicFFMpegJob<>(new CompletableFuture<>(), new TrackingProcess());
        BasicFFMpegJob<String> complete =
                new BasicFFMpegJob<>(CompletableFuture.completedFuture("result"), new TrackingProcess());

        assertFalse(incomplete.await(0, TimeUnit.MILLISECONDS));
        assertTrue(complete.await(0, TimeUnit.MILLISECONDS));
    }

    @Test
    public void negativeTimeoutWaitsIndefinitely() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        BasicFFMpegJob<String> job = new BasicFFMpegJob<>(future, new TrackingProcess());
        CountDownLatch started = new CountDownLatch(1);
        AtomicReference<Boolean> result = new AtomicReference<>();
        Thread waiting = new Thread(() -> {
            started.countDown();
            try {
                result.set(job.await(-1, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        waiting.start();
        assertTrue(started.await(1, TimeUnit.SECONDS));

        waiting.join(50);
        assertTrue(waiting.isAlive());
        future.complete("result");
        waiting.join(1_000);

        assertFalse(waiting.isAlive());
        assertTrue(result.get());
    }

    @Test
    public void awaitAndGetReturnSuccessfulResult() throws Exception {
        BasicFFMpegJob<String> job =
                new BasicFFMpegJob<>(CompletableFuture.completedFuture("result"), new TrackingProcess());

        assertTrue(job.await(1, TimeUnit.SECONDS));
        assertEquals("result", job.get());
    }

    @Test
    public void awaitReportsExceptionalCompletionAsDone() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new IOException("failed"));
        BasicFFMpegJob<String> job = new BasicFFMpegJob<>(future, new TrackingProcess());

        assertTrue(job.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void getPreservesIOExceptionCause() {
        IOException failure = new IOException("failed");
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(failure);
        BasicFFMpegJob<String> job = new BasicFFMpegJob<>(future, new TrackingProcess());

        assertSame(failure, assertThrows(IOException.class, job::get));
    }

    @Test
    public void getWrapsNonIoException() {
        IllegalStateException failure = new IllegalStateException("failed");
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(failure);
        BasicFFMpegJob<String> job = new BasicFFMpegJob<>(future, new TrackingProcess());

        IOException thrown = assertThrows(IOException.class, job::get);
        assertSame(failure, thrown.getCause());
    }

    @Test
    public void interruptedGetThrowsInterruptedIo() throws Exception {
        BasicFFMpegJob<String> job = new BasicFFMpegJob<>(new CompletableFuture<>(), new TrackingProcess());
        CountDownLatch started = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            started.countDown();
            try {
                job.get();
            } catch (Throwable e) {
                failure.set(e);
            }
        });
        thread.start();
        assertTrue(started.await(1, TimeUnit.SECONDS));

        thread.interrupt();
        thread.join(1_000);

        assertFalse(thread.isAlive());
        assertTrue(failure.get() instanceof InterruptedIOException);
    }

    @Test
    public void killAndCloseDelegateToProcessClose() {
        TrackingProcess process = new TrackingProcess();
        BasicFFMpegJob<String> job = new BasicFFMpegJob<>(new CompletableFuture<>(), process);

        job.kill();
        job.close();

        assertEquals(2, process.closeCalls);
    }

    private static final class TrackingProcess implements FFMpegProcess {
        private int closeCalls;

        @Override
        public long pid() {
            return 1;
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public boolean await(long timeoutInMillis) {
            return false;
        }

        @Override
        public OptionalInt exitCode() {
            return OptionalInt.empty();
        }

        @Override
        public OutputStream stdin() {
            return new ByteArrayOutputStream();
        }

        @Override
        public InputStream stdout() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public InputStream stderr() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public void close() {
            closeCalls++;
        }
    }
}
