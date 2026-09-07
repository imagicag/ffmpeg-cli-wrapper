package net.bramp.ffmpeg.nut;

public class FrameCode {

  long flags;
  int streamId;
  int dataSizeMul;
  int dataSizeLsb;
  long ptsDelta;
  int reservedCount;
  long matchTimeDelta;
  int headerIdx;

  @Override
  public String toString() {
    return "FrameCode{flags=" + flags + ", id=" + streamId + ", dataSizeMul=" + dataSizeMul
        + ", dataSizeLsb=" + dataSizeLsb + ", ptsDelta=" + ptsDelta + ", reservedCount="
        + reservedCount + ", matchTimeDelta=" + matchTimeDelta + ", headerIdx=" + headerIdx + '}';
  }
}
