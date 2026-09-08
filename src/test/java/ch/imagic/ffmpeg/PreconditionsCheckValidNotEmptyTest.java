package ch.imagic.ffmpeg;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PreconditionsCheckValidNotEmptyTest {
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"bob", " hello "})
    public void testUri(String input) {
        Preconditions.checkNotEmpty(input, "test must not throw exception");
    }
}
