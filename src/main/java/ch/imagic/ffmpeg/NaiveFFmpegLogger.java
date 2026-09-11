package ch.imagic.ffmpeg;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

class NaiveFFmpegLogger implements FFMpegLogger {
    private static final int MAX_LINE_BUFFER = 0x1_0000;

    private final Consumer<String> status;
    private final Consumer<String> info;
    private final Consumer<String> error;
    private final ConcurrentHashMap<Long, State> state = new ConcurrentHashMap<>();

    NaiveFFmpegLogger(Consumer<String> status, Consumer<String> info, Consumer<String> error) {
        this.status = status;
        this.info = info;
        this.error = error;
    }

    static class State {
        final ByteArrayOutputStream infoBuffer = new ByteArrayOutputStream(MAX_LINE_BUFFER);
        final ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream(MAX_LINE_BUFFER);
    }

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
        return status != null;
    }

    @Override
    public boolean wantsProcessDeath() {
        return info != null || error != null || status != null;
    }

    @Override
    public void onCommandLine(long pid, List<String> commandLine) {
        status.accept("Started child process with pid " + pid + " " + String.join(" ", commandLine));
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
                int lineSize = (off + i) - mystart;
                if (lineSize > MAX_LINE_BUFFER) {
                    downstream.accept("Child pid " + pid + " produced a output line that is longer than ~64k.");
                    continue;
                }
                downstream.accept("Child pid " + pid + prefix
                        + new String(rawData, mystart, lineSize, StandardCharsets.UTF_8).replace("\r", ""));
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
                downstream.accept("Child pid " + pid + " produced a unfinished output line that is longer than ~64k.");
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
                            + st.infoBuffer.toString(StandardCharsets.UTF_8).replace("\r", ""));
                    st.infoBuffer.reset();
                }
            }
            synchronized (st.errorBuffer) {
                if (error != null && st.errorBuffer.size() > 0) {
                    error.accept("Child pid " + pid + " trailing STDERR: "
                            + st.errorBuffer.toString(StandardCharsets.UTF_8).replace("\r", ""));
                    st.errorBuffer.reset();
                }
            }
        }

        if (status == null) {
            return;
        }

        if (exitCode == 0) {
            status.accept("Child pid " + pid + " finished successfully");
            return;
        }

        status.accept("Child pid " + pid + " finished with error " + exitCode);
    }
}
