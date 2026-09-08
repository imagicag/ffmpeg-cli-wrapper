package ch.imagic.ffmpeg.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class FormatDecimalIntegerTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "0.0, 0",
        "1.0, 1",
        "-1.0, -1",
        "0.1, 0.1",
        "1.1, 1.1",
        "1.10, 1.1",
        "1.001, 1.001",
        "100, 100",
        "100.01, 100.01"
    })
    public void formatDecimalInteger(double input, String expected) {
        String got = FFmpegOutputBuilder.formatDecimalInteger(input);

        assertEquals(expected, got);
    }
}
