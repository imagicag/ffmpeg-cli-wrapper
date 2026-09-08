package ch.imagic.ffmpeg;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PreconditionsCheckInvalidStreamTest {

    @ParameterizedTest(name = "{0}")
    @ValueSource(
            strings = {
                "http://www.example.com/",
                "https://live.twitch.tv/app/live_",
                "ftp://236.0.0.1:2000",
                "udp://10.1.0.102/",
                "tcp://127.0.0.1/"
            })
    public void testUri(String url) {
        URI uri = URI.create(url);
        assertThrows(IllegalArgumentException.class, () -> Preconditions.checkValidStream(uri));
    }
}
