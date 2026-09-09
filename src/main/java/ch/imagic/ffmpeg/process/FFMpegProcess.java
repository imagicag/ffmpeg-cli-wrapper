package ch.imagic.ffmpeg.process;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.OptionalInt;

public interface FFMpegProcess extends AutoCloseable {

    /**
     * Returns the long pid of the child process
     */
    long pid();

    /**
     * Returns if the process is still alive
     */
    boolean isAlive();

    /**
     * Waits for the process to exit. A negative timeout waits indefinitely, zero checks immediately,
     * and a positive timeout waits for at most the requested number of milliseconds.
     */
    boolean await(long timeoutInMillis) throws InterruptedException;

    /**
     * Returns the exit code if it is avaialble yet.
     * This is generally the case after get() has returned or await returned true.
     */
    OptionalInt exitCode();

    /**
     * This output stream is connected to the processes input stream.
     * Note: I/O exceptions from the underlying process stream are ignored. After such an exception, further data is
     * silently discarded.
     */
    OutputStream stdin();

    /**
     * Returns the process stdout
     */
    InputStream stdout();

    /**
     * Returns a buffered reader that reads from the process stdout.
     * Note: calling this function multiple times may result in corrupted state in the readers because
     * each instances may have state.
     */
    default BufferedReader stdoutReader() {
        return new BufferedReader(new InputStreamReader(stdout(), StandardCharsets.UTF_8));
    }

    /**
     * Returns a buffered reader that reads from the process stderr.
     */
    InputStream stderr();

    /**
     * Returns a buffered reader that reads from the process stderr.
     * Note: calling this function multiple times may result in corrupted state in the readers because
     * each instances may have state.
     */
    default BufferedReader stderrReader() {
        return new BufferedReader(new InputStreamReader(stderr(), StandardCharsets.UTF_8));
    }

    @Override
    void close();
}
