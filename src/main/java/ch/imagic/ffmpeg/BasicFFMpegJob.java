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

    private AutoCloseable[] dependants;

    BasicFFMpegJob(CompletableFuture<T> future, FFMpegProcess process, AutoCloseable... dependants) {
        this.future = future;
        this.process = process;
        this.dependants = dependants;
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
        } catch (CancellationException ce) {
            throw new IOException(ce);
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
        for (AutoCloseable closeable : dependants) {
            try {
                closeable.close();
            } catch (Exception e) {
                // DONT CARE
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        kill();
    }
}
