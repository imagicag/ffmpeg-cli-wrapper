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
        List<String> info = new ArrayList<>();
        FFMpegLogger logger = FFMpegLogger.naiveLogger(info::add, null);

        sendStdout(logger, 7, "hel");
        sendStdout(logger, 7, "lo\r\nnext\r\n");

        assertEquals(List.of("Child pid 7 STDOUT: hello", "Child pid 7 STDOUT: next"), info);
    }

    @Test
    public void routesStderrToErrorConsumer() {
        List<String> info = new ArrayList<>();
        List<String> error = new ArrayList<>();
        FFMpegLogger logger = FFMpegLogger.naiveLogger(info::add, error::add);

        sendStderr(logger, 8, "failure\r\n");

        assertTrue(info.isEmpty());
        assertEquals(List.of("Child pid 8 STDERR: failure"), error);
    }

    @Test
    public void flushesTrailingOutputWithCorrectStreamLabels() {
        List<String> info = new ArrayList<>();
        List<String> error = new ArrayList<>();
        FFMpegLogger logger = FFMpegLogger.naiveLogger(info::add, error::add);

        sendStdout(logger, 9, "last output\r");
        sendStderr(logger, 9, "last error\r");
        logger.onProcessDeath(9, 1);

        assertEquals(List.of("Child pid 9 trailing STDOUT: last output"), info);
        assertEquals(List.of("Child pid 9 trailing STDERR: last error", "Child pid 9 finished with error 1"), error);
    }

    @Test
    public void supportsEitherConsumerBeingAbsent() {
        FFMpegLogger errorOnly = FFMpegLogger.naiveLogger(null, ignored -> {});
        FFMpegLogger infoOnly = FFMpegLogger.naiveLogger(ignored -> {}, null);

        assertFalse(errorOnly.wantsRawStdout());
        assertTrue(errorOnly.wantsRawStderr());
        assertTrue(infoOnly.wantsRawStdout());
        assertFalse(infoOnly.wantsRawStderr());

        sendStderr(errorOnly, 10, "error\n");
        sendStdout(infoOnly, 11, "info\n");
        errorOnly.onProcessDeath(10, 1);
        infoOnly.onProcessDeath(11, 0);
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
