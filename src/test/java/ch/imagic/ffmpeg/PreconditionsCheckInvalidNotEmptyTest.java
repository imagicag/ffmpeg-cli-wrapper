package ch.imagic.ffmpeg;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class PreconditionsCheckInvalidNotEmptyTest {
    static Stream<String> data() {
        return Stream.of(null, "", "   ", "\n", " \n ", "\u00a0");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("data")
    public void testUri(String input) {
        assertThrows(
                IllegalArgumentException.class, () -> Preconditions.checkNotEmpty(input, "test must throw exception"));
    }
}
