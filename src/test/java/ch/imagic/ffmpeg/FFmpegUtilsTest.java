package ch.imagic.ffmpeg;

import static ch.imagic.ffmpeg.FFmpegUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class FFmpegUtilsTest {

    @Test
    public void testAbstractUtilsClass() {
        assertThrows(AssertionError.class, FFmpegUtils::new);
    }

    @Test
    public void testToTimecode() {
        assertEquals("00:00:00", toTimecode(0, TimeUnit.NANOSECONDS));
        assertEquals("00:00:00.000000001", toTimecode(1, TimeUnit.NANOSECONDS));
        assertEquals("00:00:00.000001", toTimecode(1, TimeUnit.MICROSECONDS));
        assertEquals("00:00:00.001", toTimecode(1, TimeUnit.MILLISECONDS));
        assertEquals("00:00:01", toTimecode(1, TimeUnit.SECONDS));
        assertEquals("00:01:00", toTimecode(1, TimeUnit.MINUTES));
        assertEquals("01:00:00", toTimecode(1, TimeUnit.HOURS));
        assertEquals("-00:00:00.000000001", toTimecode(-1, TimeUnit.NANOSECONDS));
        assertEquals("-00:00:00.000001", toTimecode(-1, TimeUnit.MICROSECONDS));
        assertEquals("-00:00:00.001", toTimecode(-1, TimeUnit.MILLISECONDS));
    }

    @Test
    public void toTimecodeDoesNotClipAtLongBoundaries() {
        assertEquals("2562047:47:16.854775807", toTimecode(Long.MAX_VALUE, TimeUnit.NANOSECONDS));
        assertEquals("-2562047:47:16.854775808", toTimecode(Long.MIN_VALUE, TimeUnit.NANOSECONDS));
        assertEquals("2562047788015215:30:07", toTimecode(Long.MAX_VALUE, TimeUnit.SECONDS));
        assertEquals("-2562047788015215:30:08", toTimecode(Long.MIN_VALUE, TimeUnit.SECONDS));
        assertEquals("221360928884514619368:00:00", toTimecode(Long.MAX_VALUE, TimeUnit.DAYS));
    }

    @Test
    public void testFromTimecode() {
        assertEquals(63123000000L, fromTimecode("00:01:03.123"));
        assertEquals(63000000000L, fromTimecode("00:01:03"));
        assertEquals(5025678000000L, fromTimecode("01:23:45.678"));
        assertEquals(0, fromTimecode("00:00:00"));
        assertEquals(-500_000_000L, fromTimecode("-00:00:00.500000"));
        assertEquals(0, fromTimecode("N/A"));
    }

    @Test
    public void testParseBitrate() {
        assertEquals(12300, parseBitrate("12.3kbits/s"));
        assertEquals(1000, parseBitrate("1kbits/s"));
        assertEquals(123, parseBitrate("0.123kbits/s"));
        assertEquals(800, parseBitrate("   0.8kbits/s"));
        assertEquals(1_935_500, parseBitrate("1935.5kbits/s"));
        assertEquals(-1, parseBitrate("N/A"));
    }

    @Test
    public void testParseBitrateInvalidEmpty() {
        assertThrows(IllegalArgumentException.class, () -> parseBitrate(""));
    }

    @Test
    public void testParseBitrateInvalidNumber() {
        assertThrows(IllegalArgumentException.class, () -> parseBitrate("12.3"));
    }

    @Test
    public void fromTimecodeAcceptsLargestMinuteSecondAndNanosecondFields() {
        assertEquals(3_599_999_999_999L, fromTimecode("00:59:59.999999999"));
    }

    @Test
    public void fromTimecodeRejectsOutOfRangeMinuteAndSecondFields() {
        assertThrows(IllegalArgumentException.class, () -> fromTimecode("00:60:00"));
        assertThrows(IllegalArgumentException.class, () -> fromTimecode("00:00:60"));
    }

    @Test
    public void fromTimecodeRequiresWholeInputToMatch() {
        assertThrows(IllegalArgumentException.class, () -> fromTimecode("prefix00:00:01"));
        assertThrows(IllegalArgumentException.class, () -> fromTimecode("00:00:01suffix"));
        assertThrows(IllegalArgumentException.class, () -> fromTimecode("00:00:01."));
        assertThrows(IllegalArgumentException.class, () -> fromTimecode("00::01"));
    }

    @Test
    public void parseBitrateRequiresWholeInputToMatch() {
        assertThrows(IllegalArgumentException.class, () -> parseBitrate("rate=12.3kbits/s"));
        assertThrows(IllegalArgumentException.class, () -> parseBitrate("12.3kbits/s extra"));
        assertThrows(IllegalArgumentException.class, () -> parseBitrate("12.3 kbits/s"));
        assertThrows(IllegalArgumentException.class, () -> parseBitrate("12.3kbits/s "));
    }

    @Test
    public void parseBitrateHandlesValuesAtIntegerAndFractionBoundaries() {
        assertEquals(0, parseBitrate("0kbits/s"));
        assertEquals(1, parseBitrate("0.001kbits/s"));
        assertThrows(IllegalArgumentException.class, () -> parseBitrate(".5kbits/s"));
        assertThrows(IllegalArgumentException.class, () -> parseBitrate("1.kbits/s"));
    }

    static Stream<String> emptyStrings() {
        return Stream.of(null, "", "   ", "\n", " \n ", "\u00a0");
    }

    @ParameterizedTest
    @MethodSource("emptyStrings")
    public void checkNotEmptyRejectsEmptyStrings(String input) {
        assertThrows(IllegalArgumentException.class, () -> checkNotEmpty(input, "test must throw exception"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"bob", " hello "})
    public void checkNotEmptyAcceptsNonEmptyStrings(String input) {
        assertEquals(input, checkNotEmpty(input, "test must not throw exception"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "udp://10.1.0.102:1234",
                "tcp://127.0.0.1:2000",
                "udp://236.0.0.1:2000",
                "rtmp://live.twitch.tv/app/live_",
                "rtmp:///live/myStream.sdp",
                "rtp://127.0.0.1:1234",
                "rtsp://localhost:8888/live.sdp",
                "rtsp://localhost:8888/live.sdp?tcp",
                "UDP://10.1.0.102:1234"
            })
    public void checkValidStreamAcceptsValidUris(String url) {
        URI uri = URI.create(url);
        assertEquals(uri, checkValidStream(uri));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "http://www.example.com/",
                "https://live.twitch.tv/app/live_",
                "ftp://236.0.0.1:2000",
                "udp://10.1.0.102/",
                "tcp://127.0.0.1/"
            })
    public void checkValidStreamRejectsInvalidUris(String url) {
        URI uri = URI.create(url);
        assertThrows(IllegalArgumentException.class, () -> checkValidStream(uri));
    }
}
