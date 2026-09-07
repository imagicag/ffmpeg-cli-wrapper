package net.bramp.ffmpeg.probe;

import java.util.Collections;
import java.util.List;

/** TODO Make this immutable */
public class FFmpegProbeResult {
  public FFmpegError error;
  public FFmpegFormat format;
  public List<FFmpegStream> streams;
  public List<FFmpegChapter> chapters;

  public FFmpegError getError() {
    return error;
  }

  public boolean hasError() {
    return error != null;
  }

  public FFmpegFormat getFormat() {
    return format;
  }

  public List<FFmpegStream> getStreams() {
    if (streams == null) return Collections.emptyList();
    return List.copyOf(streams);
  }

  public List<FFmpegChapter> getChapters() {
    if (chapters == null) return Collections.emptyList();
    return List.copyOf(chapters);
  }
}
