package ch.imagic.ffmpeg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class FFMpegLoggerTest {
    @Test
    public void logsLinesAcrossChunksAndRemovesCarriageReturns() {
        List<String> stdout = new ArrayList<>();
        FFMpegLogger logger = FFMpegLogger.naiveLogger(null, stdout::add, null);

        sendStdout(logger, 7, "hel");
        sendStdout(logger, 7, "lo\r\nnext\r\n");

        assertEquals(List.of("Child pid 7 STDOUT: hello", "Child pid 7 STDOUT: next"), stdout);
    }

    @Test
    public void routesStderrToErrorConsumer() {
        List<String> stdout = new ArrayList<>();
        List<String> stderr = new ArrayList<>();
        FFMpegLogger logger = FFMpegLogger.naiveLogger(null, stdout::add, stderr::add);

        sendStderr(logger, 8, "failure\r\n");

        assertTrue(stdout.isEmpty());
        assertEquals(List.of("Child pid 8 STDERR: failure"), stderr);
    }

    @Test
    public void flushesTrailingOutputWithCorrectStreamLabels() {
        List<String> status = new ArrayList<>();
        List<String> stdout = new ArrayList<>();
        List<String> stderr = new ArrayList<>();
        FFMpegLogger logger = FFMpegLogger.naiveLogger(status::add, stdout::add, stderr::add);

        sendStdout(logger, 9, "last output\r");
        sendStderr(logger, 9, "last error\r");
        logger.onProcessDeath(9, 1);

        assertEquals(List.of("Child pid 9 trailing STDOUT: last output"), stdout);
        assertEquals(List.of("Child pid 9 trailing STDERR: last error"), stderr);
        assertEquals(List.of("Child pid 9 finished with error 1"), status);
    }

    @Test
    public void supportsConsumersBeingAbsent() {
        FFMpegLogger stderrOnly = FFMpegLogger.naiveLogger(null, null, ignored -> {});
        FFMpegLogger stdoutOnly = FFMpegLogger.naiveLogger(null, ignored -> {}, null);
        FFMpegLogger statusOnly = FFMpegLogger.naiveLogger(ignored -> {}, null, null);

        assertFalse(stderrOnly.wantsRawStdout());
        assertTrue(stderrOnly.wantsRawStderr());
        assertFalse(stderrOnly.wantsCommandLine());
        assertTrue(stdoutOnly.wantsRawStdout());
        assertFalse(stdoutOnly.wantsRawStderr());
        assertFalse(stdoutOnly.wantsCommandLine());
        assertFalse(statusOnly.wantsRawStdout());
        assertFalse(statusOnly.wantsRawStderr());
        assertTrue(statusOnly.wantsCommandLine());

        sendStderr(stderrOnly, 10, "error\n");
        sendStdout(stdoutOnly, 11, "info\n");
        stderrOnly.onProcessDeath(10, 1);
        stdoutOnly.onProcessDeath(11, 0);
        statusOnly.onProcessDeath(12, 0);
    }

    @Test
    public void decodesUtf8CharacterSplitAcrossChunks() {
        List<String> stdout = new ArrayList<>();
        FFMpegLogger logger = FFMpegLogger.naiveLogger(null, stdout::add, null);
        byte[] line = "caf\u00e9\n".getBytes(StandardCharsets.UTF_8);

        logger.onRawStdout(12, line, 0, line.length - 2);
        logger.onRawStdout(12, line, line.length - 2, 2);

        assertEquals(List.of("Child pid 12 STDOUT: caf\u00e9"), stdout);
    }

    @Test
    public void honorsOffsetAndLengthWithoutLoggingAdjacentBytes() {
        List<String> stdout = new ArrayList<>();
        FFMpegLogger logger = FFMpegLogger.naiveLogger(null, stdout::add, null);
        byte[] bytes = "ignorewanted\nignore".getBytes(StandardCharsets.UTF_8);

        logger.onRawStdout(13, bytes, 6, 7);

        assertEquals(List.of("Child pid 13 STDOUT: wanted"), stdout);
    }

    @Test
    public void logsEveryCompleteLineFromSingleChunkAndFlushesRemainder() {
        List<String> status = new ArrayList<>();
        List<String> stdout = new ArrayList<>();
        FFMpegLogger logger = FFMpegLogger.naiveLogger(status::add, stdout::add, null);

        sendStdout(logger, 14, "one\ntwo\nthree");
        logger.onProcessDeath(14, 0);

        assertEquals(
                List.of("Child pid 14 STDOUT: one", "Child pid 14 STDOUT: two", "Child pid 14 trailing STDOUT: three"),
                stdout);
        assertEquals(List.of("Child pid 14 finished successfully"), status);
    }

    @Test
    public void discardsOversizedUnfinishedLineAndRecovers() {
        List<String> stdout = new ArrayList<>();
        FFMpegLogger logger = FFMpegLogger.naiveLogger(null, stdout::add, null);

        sendStdout(logger, 15, "x".repeat(0x1_0001));
        sendStdout(logger, 15, "ok\n");

        assertEquals(
                List.of(
                        "Child pid 15 produced a unfinished output line that is longer than ~64k.",
                        "Child pid 15 STDOUT: ok"),
                stdout);
    }

    @Test
    public void rejectsOversizedCompleteLineInSingleChunk() {
        List<String> stdout = new ArrayList<>();
        FFMpegLogger logger = FFMpegLogger.naiveLogger(null, stdout::add, null);

        sendStdout(logger, 16, "x".repeat(0x1_0001) + "\n");

        assertEquals(List.of("Child pid 16 produced a output line that is longer than ~64k."), stdout);
    }

    private static void sendStdout(FFMpegLogger logger, long pid, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        logger.onRawStdout(pid, bytes, 0, bytes.length);
    }

    private static void sendStderr(FFMpegLogger logger, long pid, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        logger.onRawStderr(pid, bytes, 0, bytes.length);
    }
}
