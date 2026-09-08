package ch.imagic.ffmpeg;

import ch.imagic.ffmpeg.process.FFMpegProcess;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.OptionalInt;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

class BasicFFMpegJob<T> implements FFMpegJob<T> {

    private final FFMpegProcess process;

    private final CompletableFuture<T> future;

    private volatile boolean closed = false;

    BasicFFMpegJob(CompletableFuture<T> future, FFMpegProcess process) {
        this.future = future;
        this.process = process;
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        if (closed) {
            return true;
        }

        try {
            if (timeout < 0) {
                future.get();
                return true;
            }
            future.get(timeout, unit);
            return true;
        } catch (ExecutionException e) {
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    @Override
    public T get() throws IOException {
        if (closed) {
            throw new IOException("Job has been closed");
        }
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        } catch (ExecutionException e) {
            if (closed) {
                throw new IOException("Job has been closed");
            }

            var cause = e.getCause();
            if (cause == null) {
                throw new IOException(e);
            }
            if (cause instanceof IOException ioe) {
                throw ioe;
            }

            throw new IOException(cause);
        }
    }

    @Override
    public void whenComplete(BiConsumer<T, Throwable> action) {
        future.whenComplete(action);
    }

    @Override
    public OptionalInt getExitCode() {
        return process.exitCode();
    }

    public void kill() {
        future.completeExceptionally(new CancellationException());
        process.close();
    }

    @Override
    public void close() {
        closed = true;
        kill();
    }
}
