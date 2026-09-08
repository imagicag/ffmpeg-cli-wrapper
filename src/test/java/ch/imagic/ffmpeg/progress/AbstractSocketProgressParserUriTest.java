package ch.imagic.ffmpeg.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetAddress;
import java.net.URI;
import org.junit.jupiter.api.Test;

public class AbstractSocketProgressParserUriTest {

    @Test
    public void createsIpv4Uri() throws Exception {
        InetAddress address = InetAddress.getByAddress(new byte[] {(byte) 192, 0, 2, 1});

        URI uri = AbstractSocketProgressParser.createUri("udp", address, 1234);

        assertEquals("udp://192.0.2.1:1234", uri.toASCIIString());
    }

    @Test
    public void createsIpv6Uri() throws Exception {
        InetAddress address = InetAddress.getByAddress(new byte[] {
            0x20, 0x01, 0x0d, (byte) 0xb8,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x01
        });

        URI uri = AbstractSocketProgressParser.createUri("tcp", address, 4321);

        assertEquals("tcp://[2001:db8:0:0:0:0:0:1]:4321", uri.toASCIIString());
    }
}
