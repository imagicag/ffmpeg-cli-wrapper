package ch.imagic.ffmpeg.nut;

import java.io.IOException;

public class PacketFooter {
    int checksum;

    public void read(NutDataInputStream in) throws IOException {
        long expected = in.getCRC();
        checksum = in.readInt();
        if (checksum != expected) {
            // throw new IOException(String.format("invalid packet checksum %X want %X", expected,
            // checksum));
            Packet.LOG.debug("invalid packet checksum {} want {}", expected, checksum);
        }
        in.resetCRC();
    }

    @Override
    public String toString() {
        return "PacketFooter{checksum=" + checksum + '}';
    }
}
