package ch.imagic.ffmpeg;

import ch.imagic.ffmpeg.process.FFMpegProcess;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class BasicFFMpegJob<T> implements FFMpegJob<T> {

    private final FFMpegProcess process;

    private final CompletableFuture<T> future;

    BasicFFMpegJob(CompletableFuture<T> future, FFMpegProcess process) {
        this.future = future;
        this.process = process;
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
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
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        } catch (ExecutionException e) {
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

    public void kill() {
        process.close();
    }

    @Override
    public void close() {
        kill();
    }
}
