package ch.imagic.ffmpeg;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
     * Kill the job as soon as possible.
     * This function has no effect is the job happens to already be completed or is in the process of completing.
     */
    void kill();

    /**
     * Try with resources alias for kill.
     */
    @Override
    void close();
}
