package ch.imagic.ffmpeg;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
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
        return naiveLogger(System.out::println, System.err::println);
    }

    /**
     * Native implementation suitable for commonly used logger frameworks such as log4j/slf4j/logback/jul.
     * <br>
     * Example for slf4j:
     * <p>
     * <br> private static final Logger LOGGER = LoggerFactory.getLogger(MyClass.class);
     * <br> //...
     * <br> FFMpegLogger logger = naiveLogger(LOGGER::info, LOGGER::error);
     * <br> //...
     * </p>
     */
    static FFMpegLogger naiveLogger(Consumer<String> info, Consumer<String> error) {
        return new FFMpegLogger() {
            private static int MAX_LINE_BUFFER = 0x1_0000;

            static class State {
                final ByteArrayOutputStream infoBuffer = new ByteArrayOutputStream(MAX_LINE_BUFFER);
                final ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream(MAX_LINE_BUFFER);
            }

            private final ConcurrentHashMap<Long, State> state = new ConcurrentHashMap<>();

            @Override
            public boolean wantsRawStdout() {
                return info != null;
            }

            @Override
            public boolean wantsRawStderr() {
                return error != null;
            }

            @Override
            public boolean wantsCommandLine() {
                return info != null;
            }

            @Override
            public boolean wantsProcessDeath() {
                return info != null || error != null;
            }

            @Override
            public void onCommandLine(long pid, List<String> commandLine) {
                info.accept("Started child process with pid " + pid + " " + String.join(" ", commandLine));
            }

            private void handle(
                    String prefix,
                    ByteArrayOutputStream baos,
                    long pid,
                    byte[] rawData,
                    int off,
                    int len,
                    Consumer<String> downstream) {
                if (downstream == null) {
                    return;
                }
                int start = off;
                for (int i = 0; i < len; i++) {
                    if (rawData[off + i] != '\n') {
                        continue;
                    }
                    int mystart = start;
                    start = off + i + 1;

                    if (baos.size() == 0) {
                        downstream.accept("Child pid " + pid + prefix
                                + new String(rawData, mystart, (off + i) - mystart, StandardCharsets.UTF_8)
                                        .replace("\r", ""));
                        continue;
                    }

                    int toCopy = (off + i) - mystart;
                    if (baos.size() + toCopy > MAX_LINE_BUFFER) {
                        baos.reset();
                        downstream.accept("Child pid " + pid + " produced a output line that is longer than ~64k.");
                        continue;
                    }
                    baos.write(rawData, mystart, toCopy);
                    downstream.accept("Child pid " + pid + prefix
                            + baos.toString(StandardCharsets.UTF_8).replace("\r", ""));
                    baos.reset();
                }

                if (start < off + len) {
                    int toCopy = (off + len) - start;
                    if (baos.size() + toCopy > MAX_LINE_BUFFER) {
                        baos.reset();
                        downstream.accept(
                                "Child pid " + pid + " produced a unfinished output line that is longer than ~64k.");
                        return;
                    }
                    baos.write(rawData, start, (off + len) - start);
                }
            }

            @Override
            public void onRawStdout(long pid, byte[] rawData, int off, int len) {
                var st = state.computeIfAbsent(pid, _k -> new State());
                synchronized (st.infoBuffer) {
                    handle(" STDOUT: ", st.infoBuffer, pid, rawData, off, len, info);
                }
            }

            @Override
            public void onRawStderr(long pid, byte[] rawData, int off, int len) {
                var st = state.computeIfAbsent(pid, _k -> new State());
                synchronized (st.errorBuffer) {
                    handle(" STDERR: ", st.errorBuffer, pid, rawData, off, len, error);
                }
            }

            @Override
            public void onProcessDeath(long pid, int exitCode) {
                State st = state.remove(pid);
                if (st != null) {
                    synchronized (st.infoBuffer) {
                        if (info != null && st.infoBuffer.size() > 0) {
                            info.accept("Child pid " + pid + " trailing STDOUT: "
                                    + st.infoBuffer
                                            .toString(StandardCharsets.UTF_8)
                                            .replace("\r", ""));
                            st.infoBuffer.reset();
                        }
                    }
                    synchronized (st.errorBuffer) {
                        if (error != null && st.errorBuffer.size() > 0) {
                            error.accept("Child pid " + pid + " trailing STDERR: "
                                    + st.errorBuffer
                                            .toString(StandardCharsets.UTF_8)
                                            .replace("\r", ""));
                            st.errorBuffer.reset();
                        }
                    }
                }

                if (exitCode == 0) {
                    if (info != null) {
                        info.accept("Child pid " + pid + " finished successfully");
                    }
                    return;
                }

                if (error == null) {
                    return;
                }

                error.accept("Child pid " + pid + " finished with error " + exitCode);
            }
        };
    }
}
