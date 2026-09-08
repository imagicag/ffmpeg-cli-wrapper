package ch.imagic.ffmpeg;

import java.io.IOException;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public interface FFMpegJob<T> extends AutoCloseable {
    /**
     * This function blocks until the job is done or until the timeout elapses.
     * This function returns true if the job is done before the timeout elapsed.
     * Note that this function also returns true if the job failed or has been canceled/killed.
     * A negative timeout waits indefinitely, zero checks immediately, and a positive timeout waits
     * for at most the requested duration.
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * This function blocks until the job is done and returns the result of the job.
     * IOException is thrown if the job has failed.
     */
    T get() throws IOException;

    /**
     * This function registers a action to be executed when the job is done or canceled for whatever reason.
     * If the job is already completed then the function may instantly execute the action in the current thread.
     */
    void whenComplete(BiConsumer<T, Throwable> action);

    OptionalInt getExitCode();

    /**
     * Kill the job as soon as possible.
     * This function has no effect is the job happens to already be completed or is in the process of completing.
     */
    void kill();

    /**
     * This function does the same thing as kill() with the addition of making
     * concurrent and future calls to get() fail with a fixed IOException instead of a cancellation exception.
     *
     * It has no effect on callbacks registered using thenAccept
     *
     * Not calling this function does not cause a resource leak if the ffmpeg process has already terminated (get or kill were invoked).
     * The resources are also released when ffmpeg eventually terminates by itself, whenever that may be.
     */
    @Override
    void close();
}
