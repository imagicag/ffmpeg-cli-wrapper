package ch.imagic.ffmpeg.process;

import ch.imagic.ffmpeg.FFMpegStreamConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class FFmpegStdinOutputStream extends OutputStream implements FFMpegStreamConsumer<Void> {
    private final OutputStream delegate;
    private volatile boolean done;

    FFmpegStdinOutputStream(OutputStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public void write(int b) {
        if (done) {
            return;
        }

        try {
            delegate.write(b);
        } catch (IOException ioe) {
            done = true;
        }
    }

    @Override
    public void write(byte[] b) {
        if (done) {
            return;
        }

        try {
            delegate.write(b);
        } catch (IOException ioe) {
            done = true;
        }
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (done) {
            return;
        }

        try {
            delegate.write(b, off, len);
        } catch (IOException ioe) {
            done = true;
        }
    }

    @Override
    public void flush() {
        if (done) {
            return;
        }

        try {
            delegate.flush();
        } catch (IOException ioe) {
            done = true;
        }
    }

    @Override
    public void close() throws IOException {
        // Close is called even if done is true.
        done = true;
        try {
            delegate.close();
        } catch (IOException ioe) {
            // The process may close its input before it exits.
        }
    }

    public boolean isDone() {
        return done;
    }

    @Override
    public Void consume(InputStream source) throws IOException {
        byte[] buffer = new byte[0x1_0000];
        try (this) {
            while (!isDone()) {
                int i = source.read(buffer);
                if (i <= 0) {
                    return null;
                }
                this.write(buffer, 0, i);
            }
        }

        return null;
    }
}
