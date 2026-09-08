package ch.imagic.ffmpeg.process;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.OptionalInt;

public interface FFMpegProcess extends AutoCloseable {

    long pid();

    boolean await(long timeoutInMillis) throws InterruptedException;

    OptionalInt exitCode();

    OutputStream stdin();

    InputStream stdout();

    default BufferedReader stdoutReader() {
        return new BufferedReader(new InputStreamReader(stdout()));
    }

    InputStream stderr();

    default BufferedReader stderrReader() {
        return new BufferedReader(new InputStreamReader(stderr()));
    }

    @Override
    void close();
}
