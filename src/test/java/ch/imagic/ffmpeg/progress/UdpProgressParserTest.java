package ch.imagic.ffmpeg.progress;

import static ch.imagic.ffmpeg.Helper.loadResource;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.util.List;
import ch.imagic.ffmpeg.fixtures.Progresses;
import org.junit.Test;

public class UdpProgressParserTest extends AbstractProgressParserTest {

    @Override
    public ProgressParser newParser(ProgressListener listener) throws IOException, URISyntaxException {
        return new UdpProgressParser(listener);
    }

    @Test
    public void testNormal() throws IOException, InterruptedException, URISyntaxException {
        parser.start();

        final InetAddress addr = InetAddress.getByName(uri.getHost());
        final int port = uri.getPort();

        try (DatagramSocket socket = new DatagramSocket()) {
            // Load each Progress Fixture, and send in a single datagram packet
            for (String progressFixture : Progresses.allFiles) {
                InputStream inputStream = loadResource(progressFixture);
                byte[] bytes = inputStream.readAllBytes();

                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, addr, port);
                socket.send(packet);
            }
        }

        Thread.sleep(100); // HACK: Wait a short while to avoid closing the receiving socket

        parser.stop();

        assertEquals((List<Progress>) Progresses.allProgresses, progesses);
    }
}
