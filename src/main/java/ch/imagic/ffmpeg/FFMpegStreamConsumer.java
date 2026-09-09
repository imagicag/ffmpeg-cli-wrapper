package ch.imagic.ffmpeg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

@FunctionalInterface
public interface FFMpegStreamConsumer<T> {

    /**
     * Called with a stream from ffmpeg.
     * The input stream does not need to be fully consumed.
     * Any remaining data will simply be thrown away if the function returns early.
     * any IOException thrown may be propagated up unless another exception arrives before it.
     * If this method throws an exception the ffmpeg is usually stopped soon after.
     *
     * This function should NOT close the input stream,
     * otherwise the return value may be ignored and an exception thrown.
     */
    T consume(InputStream fromFFmpeg) throws IOException;

    /**
     * Does nothing, all data will be thrown away.
     */
    static FFMpegStreamConsumer<Void> noop() {
        return input -> null;
    }

    /**
     * Consume a stream to a OutputStream
     */
    static FFMpegStreamConsumer<Void> toOutputStream(OutputStream os) {
        Objects.requireNonNull(os);
        if (os instanceof FFMpegStreamConsumer<?> consumer) {
            return input -> {
                consumer.consume(input);
                return null;
            };
        }

        return input -> {
            try (os) {
                input.transferTo(os);
            }
            return null;
        };
    }
}
