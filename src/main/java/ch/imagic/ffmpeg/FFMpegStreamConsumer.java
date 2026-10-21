package ch.imagic.ffmpeg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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

    /**
     * Utility interface that allows for writing functors that accept a UTF-8 BufferedReader instead
     * of a raw input stream. Likely to be most useful for parsing json output that ffmpeg/ffprobe can provide.
     */
    interface ReaderConsumer<T> {

        /**
         * Called with a stream from ffmpeg.
         * The reader does not need to be fully consumed.
         * Any remaining data will simply be thrown away if the function returns early.
         * any IOException thrown may be propagated up unless another exception arrives before it.
         * If this method throws an exception the ffmpeg is usually stopped soon after.
         *
         * This function should NOT close the reader,
         * otherwise the return value may be ignored and an exception thrown.
         */
        T consume(BufferedReader fromFFmpeg) throws IOException;
    }

    /**
     * Allows for writing functors that accept a utf-8 buffered reader instead of raw bytes from ffmpeg.
     * Likely to be most useful for parsing json output that ffmpeg/ffprobe can provide.
     */
    static <T> FFMpegStreamConsumer<T> withReader(ReaderConsumer<T> consumer) {
        Objects.requireNonNull(consumer);
        return stream -> consumer.consume(new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
    }
}
