package ch.imagic.ffmpeg;

import java.net.URI;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PreconditionsCheckValidStreamTest {

    @ParameterizedTest(name = "{0}")
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
    public void testUri(String url) {
        Preconditions.checkValidStream(URI.create(url));
    }
}
