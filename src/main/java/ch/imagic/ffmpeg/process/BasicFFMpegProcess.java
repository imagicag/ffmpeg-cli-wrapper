package ch.imagic.ffmpeg.process;

import ch.imagic.ffmpeg.FFMpegLogger;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.OptionalInt;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class BasicFFMpegProcess implements FFMpegProcess {
    private static final long CLOSE_TIMEOUT_MILLIS = 10_000;

    private final Executor executor;
    private final FFMpegLogger logger;
    private final Process process;
    private final OutputStream stdin;
    private final InputStream stdout;
    private final InputStream stderr;
    private final AtomicBoolean exitToggle = new AtomicBoolean(false);

    BasicFFMpegProcess(Executor executor, FFMpegLogger logger, Process proc) {
        this.executor = executor;
        this.process = proc;
        this.logger = logger;
        this.stdin = proc.getOutputStream();
        if (logger.wantsRawStdout()) {
            this.stdout = new AsyncQueueReader(
                    executor, proc.getInputStream(), data -> logger.onRawStdout(pid(), data, 0, data.length));
        } else {
            this.stdout = new BufferedInputStream(proc.getInputStream());
        }
        if (logger.wantsRawStderr()) {
            this.stderr = new AsyncQueueReader(
                    executor, proc.getErrorStream(), data -> logger.onRawStderr(pid(), data, 0, data.length));
        } else {
            this.stderr = new BufferedInputStream(proc.getErrorStream());
        }
    }

    @Override
    public long pid() {
        return process.pid();
    }

    @Override
    public boolean await(long timeoutInMillis) throws InterruptedException {
        if (timeoutInMillis < 0) {
            process.waitFor();
        }
        return process.waitFor(timeoutInMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public OptionalInt exitCode() {
        try {
            return OptionalInt.of(process.exitValue());
        } catch (IllegalThreadStateException e) {
            return OptionalInt.empty();
        }
    }

    @Override
    public OutputStream stdin() {
        return stdin;
    }

    @Override
    public InputStream stdout() {
        return stdout;
    }

    @Override
    public InputStream stderr() {
        return stderr;
    }

    @Override
    public void close() {
        if (exitToggle.getAndSet(true)) {
            return;
        }

        if (process.isAlive()) {
            process.destroyForcibly();
        }
        try {
            stdout.close();
        } catch (IOException e) {
            // DC
        }
        try {
            stderr.close();
        } catch (IOException e) {
            // DC
        }
        try {
            stdin.close();
        } catch (IOException e) {
            // DC
        }

        boolean interrupted = awaitReaderTermination(stdout);
        interrupted |= awaitReaderTermination(stderr);

        int exitCode = -1;
        try {
            if (process.waitFor(CLOSE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                exitCode = process.exitValue();
            }
        } catch (InterruptedException e) {
            interrupted = true;
        }

        if (logger.wantsProcessDeath()) {
            try {
                logger.onProcessDeath(process.pid(), exitCode);
            } catch (Throwable e) {
                // Logger failures must not interrupt process cleanup.
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean awaitReaderTermination(InputStream stream) {
        if (!(stream instanceof AsyncQueueReader reader)) {
            return false;
        }
        try {
            reader.awaitAsyncTermination(CLOSE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            return false;
        } catch (InterruptedException e) {
            return true;
        }
    }
}
