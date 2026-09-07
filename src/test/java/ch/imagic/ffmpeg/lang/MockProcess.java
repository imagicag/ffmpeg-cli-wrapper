package ch.imagic.ffmpeg.lang;

import ch.imagic.ffmpeg.process.FFMpegProcess;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.OptionalInt;

/**
 * A Mock Process, which exits with zero, and returns the provided streams.
 *
 */
class MockProcess implements FFMpegProcess {
    final OutputStream stdin;
    final InputStream stdout;
    final InputStream stderr;

    public MockProcess(InputStream stdout) {
        this.stdin = new ByteArrayOutputStream();
        this.stdout = stdout;
        this.stderr = new ByteArrayInputStream(new byte[0]);
    }

    public MockProcess(OutputStream stdin, InputStream stdout, InputStream stderr) {
        this.stdin = stdin;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    @Override
    public long pid() {
        return 0;
    }

    @Override
    public boolean await(long timeoutInMillis) throws InterruptedException {
        return true;
    }

    @Override
    public OptionalInt exitCode() {
        return OptionalInt.of(0);
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
    public void close() {}
}
