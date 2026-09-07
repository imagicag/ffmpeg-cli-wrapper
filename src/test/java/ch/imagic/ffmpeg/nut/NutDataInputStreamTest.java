package ch.imagic.ffmpeg.nut;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import org.junit.Test;

public class NutDataInputStreamTest {

    @Test
    public void offsetTracksConsumedBytes() throws Exception {
        byte[] data = {(byte) 0x81, 0x01, 0x00, 0x00, 0x00, 0x2a, 0x06, 0x07, 0x08, 0x09, 0x0a};
        NutDataInputStream in = new NutDataInputStream(new ByteArrayInputStream(data));

        assertEquals(0, in.offset());
        assertEquals(129, in.readVarInt());
        assertEquals(2, in.offset());
        assertEquals(42, in.readInt());
        assertEquals(6, in.offset());
        assertEquals(2, in.skipBytes(2));
        assertEquals(8, in.offset());

        byte[] remaining = new byte[3];
        in.readFully(remaining);
        assertArrayEquals(new byte[] {0x08, 0x09, 0x0a}, remaining);
        assertEquals(11, in.offset());
    }
}
