package net.bramp.ffmpeg.job;

import java.util.List;
import java.util.Objects;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.progress.ProgressListener;

public class SinglePassFFmpegJob extends FFmpegJob {

  public final FFmpegBuilder builder;

  public SinglePassFFmpegJob(FFmpeg ffmpeg, FFmpegBuilder builder) {
    this(ffmpeg, builder, null);
  }

  public SinglePassFFmpegJob(
      FFmpeg ffmpeg, FFmpegBuilder builder, ProgressListener listener) {
    super(ffmpeg, listener);
    this.builder = Objects.requireNonNull(builder);

    // Build the args now (but throw away the results). This allows the illegal arguments to be
    // caught early, but also allows the ffmpeg command to actually alter the arguments when
    // running.
    List<String> unused = this.builder.build();
  }

  @Override
  public void run() {

    state = State.RUNNING;

    try {
      ffmpeg.run(builder, listener);
      state = State.FINISHED;

    } catch (Throwable t) {
      state = State.FAILED;

      if (t instanceof RuntimeException runtimeException) throw runtimeException;
      if (t instanceof Error error) throw error;
      throw new RuntimeException(t);
    }
  }
}
