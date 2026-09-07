package net.bramp.ffmpeg.nut;

import java.io.IOException;

public class PacketHeader {

  long startcode;
  long forwardPtr;
  int checksum; // header checksum

  long end; // End byte of packet

  public void read(NutDataInputStream in, long startcode) throws IOException {
    this.startcode = startcode;
    forwardPtr = in.readVarLong();
    if (forwardPtr > 4096) {
      long expected = in.getCRC();
      checksum = in.readInt();
      if (checksum != expected) {
        // TODO This code path has never been tested.
        throw new IOException(
            String.format("invalid header checksum %X want %X", expected, checksum));
      }
    }

    in.resetCRC();
    end = in.offset() + forwardPtr - 4; // 4 bytes for footer CRC
  }

  @Override
  public String toString() {
    return "PacketHeader{startcode=" + Packet.Startcode.toString(startcode) + ", forwardPtr="
        + forwardPtr + (forwardPtr > 4096 ? ", checksum=" + checksum : "") + '}';
  }
}
