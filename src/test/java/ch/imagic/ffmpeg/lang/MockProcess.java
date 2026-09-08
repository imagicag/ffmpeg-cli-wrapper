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
public class MockProcess implements FFMpegProcess {
    final OutputStream stdin;
    final InputStream stdout;
    final InputStream stderr;
    final boolean awaited;
    final int exitCode;

    public MockProcess(InputStream stdout) {
        this(new ByteArrayOutputStream(), stdout, new ByteArrayInputStream(new byte[0]));
    }

    public MockProcess(OutputStream stdin, InputStream stdout, InputStream stderr) {
        this(stdin, stdout, stderr, true, 0);
    }

    public MockProcess(OutputStream stdin, InputStream stdout, InputStream stderr, boolean awaited, int exitCode) {
        this.stdin = stdin;
        this.stdout = stdout;
        this.stderr = stderr;
        this.awaited = awaited;
        this.exitCode = exitCode;
    }

    @Override
    public long pid() {
        return 0;
    }

    @Override
    public boolean await(long timeoutInMillis) throws InterruptedException {
        return awaited;
    }

    @Override
    public OptionalInt exitCode() {
        return OptionalInt.of(exitCode);
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
    public void close() {}
}
