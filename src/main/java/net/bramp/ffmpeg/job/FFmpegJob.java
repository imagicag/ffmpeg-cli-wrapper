package net.bramp.ffmpeg.job;

import java.util.Objects;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.progress.ProgressListener;

/**
 * @author bramp
 */
public abstract class FFmpegJob implements Runnable {

  public enum State {
    WAITING,
    RUNNING,
    FINISHED,
    FAILED,
  }

  final FFmpeg ffmpeg;
  final ProgressListener listener;

  State state = State.WAITING;

  public FFmpegJob(FFmpeg ffmpeg) {
    this(ffmpeg, null);
  }

  public FFmpegJob(FFmpeg ffmpeg, ProgressListener listener) {
    this.ffmpeg = Objects.requireNonNull(ffmpeg);
    this.listener = listener;
  }

  public State getState() {
    return state;
  }
}
