package net.bramp.ffmpeg.job;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.progress.ProgressListener;

public class TwoPassFFmpegJob extends FFmpegJob {

  final String passlogPrefix;
  final FFmpegBuilder builder;

  public TwoPassFFmpegJob(FFmpeg ffmpeg, FFmpegBuilder builder) {
    this(ffmpeg, builder, null);
  }

  public TwoPassFFmpegJob(
      FFmpeg ffmpeg, FFmpegBuilder builder, ProgressListener listener) {
    super(ffmpeg, listener);

    // Random prefix so multiple runs don't clash
    this.passlogPrefix = UUID.randomUUID().toString();
    this.builder = Objects.requireNonNull(builder).setPassPrefix(passlogPrefix);

    // Build the args now (but throw away the results). This allows the illegal arguments to be
    // caught early, but also allows the ffmpeg command to actually alter the arguments when
    // running.
    List<String> unused = this.builder.setPass(1).build();
  }

  protected void deletePassLog() throws IOException {
    final Path cwd = Paths.get("");
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(cwd, passlogPrefix + "*.log*")) {
      for (Path p : stream) {
        Files.deleteIfExists(p);
      }
    }
  }

  @Override
  public void run() {
    state = State.RUNNING;

    try {
      try {
        // Two pass
        final boolean override = builder.getOverrideOutputFiles();

        FFmpegBuilder b1 = builder.setPass(1).overrideOutputFiles(true);
        ffmpeg.run(b1, listener);

        FFmpegBuilder b2 = builder.setPass(2).overrideOutputFiles(override);
        ffmpeg.run(b2, listener);

      } finally {
        deletePassLog();
      }
      state = State.FINISHED;

    } catch (Throwable t) {
      state = State.FAILED;

      if (t instanceof RuntimeException runtimeException) throw runtimeException;
      if (t instanceof Error error) throw error;
      throw new RuntimeException(t);
    }
  }
}
