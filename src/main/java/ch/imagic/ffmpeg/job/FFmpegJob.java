package ch.imagic.ffmpeg.job;

import ch.imagic.ffmpeg.FFmpeg;
import ch.imagic.ffmpeg.progress.ProgressListener;
import java.util.Objects;

/**
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
