package ch.imagic.ffmpeg;

import java.util.List;
import java.util.function.Consumer;

public interface FFMpegLogger {

    /**
     * should onRawStdout be called.
     *
     * The return value should not change during the lifetime of this instance.
     */
    default boolean wantsRawStdout() {
        return false;
    }

    /**
     * should onRawStderr be called.
     *
     * The return value should not change during the lifetime of this instance.
     */
    default boolean wantsRawStderr() {
        return false;
    }

    /**
     * should onCommandLine be called.
     *
     * The return value should not change during the lifetime of this instance.
     */
    default boolean wantsCommandLine() {
        return false;
    }

    /**
     * should onProcessDeath be called.
     *
     * The return value should not change during the lifetime of this instance.
     */
    default boolean wantsProcessDeath() {
        return false;
    }

    default void onCommandLine(long pid, List<String> commandLine) {
        // NOTHING
    }

    /**
     * Note: rawData may contain multiple lines or only partial lines, rawData likely has to be buffered.
     * It may even split utf-8 characters!
     */
    default void onRawStdout(long pid, byte[] rawData, int off, int len) {
        // NOTHING
    }

    /**
     * Note: rawData may contain multiple lines or only partial lines, rawData likely has to be buffered.
     * It may even split utf-8 characters!
     */
    default void onRawStderr(long pid, byte[] rawData, int off, int len) {
        // NOTHING
    }

    default void onProcessDeath(long pid, int exitCode) {
        // NOTHING
    }

    /**
     * Returns a logger that does not log anything. Aka NOOP.
     */
    static FFMpegLogger noop() {
        return new FFMpegLogger() {};
    }

    /**
     * Returns a logger that logs to stdout and stderr of the java process.
     * Only really useful in unit tests/simple main methods.
     */
    static FFMpegLogger stdLogger() {
        return naiveLogger(System.out::println, null, System.err::println);
    }

    /**
     * Native implementation suitable for commonly used logger frameworks such as log4j/slf4j/logback/jul.
     *
     * Note: stdout of ffmpeg is normally empty and sometimes used to write the output file to.
     * Logging stdout of ffmpeg is not advisable as it may contain a media file.
     * Logging stdout of ffprobe should not be a problem since it mostly outputs json.
     *
     * <br>
     * Example for slf4j:
     * <p>
     * <br> private static final Logger LOGGER = LoggerFactory.getLogger(MyClass.class);
     * <br> //...
     * <br> FFMpegLogger logger = naiveLogger(LOGGER::info, null, LOGGER::error);
     * <br> //...
     * </p>
     */
    static FFMpegLogger naiveLogger(Consumer<String> status, Consumer<String> stdout, Consumer<String> stderr) {
        return new NaiveFFmpegLogger(status, stdout, stderr);
    }
}
