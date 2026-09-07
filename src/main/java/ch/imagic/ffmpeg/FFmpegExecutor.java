package ch.imagic.ffmpeg;

import java.io.IOException;
import java.util.Objects;
import ch.imagic.ffmpeg.builder.FFmpegBuilder;
import ch.imagic.ffmpeg.job.FFmpegJob;
import ch.imagic.ffmpeg.job.SinglePassFFmpegJob;
import ch.imagic.ffmpeg.job.TwoPassFFmpegJob;
import ch.imagic.ffmpeg.progress.ProgressListener;

public class FFmpegExecutor {

    final FFmpeg ffmpeg;
    final FFprobe ffprobe;

    public FFmpegExecutor() throws IOException {
        this(new FFmpeg(), new FFprobe());
    }

    public FFmpegExecutor(FFmpeg ffmpeg) throws IOException {
        this(ffmpeg, new FFprobe());
    }

    public FFmpegExecutor(FFmpeg ffmpeg, FFprobe ffprobe) {
        this.ffmpeg = Objects.requireNonNull(ffmpeg);
        this.ffprobe = Objects.requireNonNull(ffprobe);
    }

    public FFmpegJob createJob(FFmpegBuilder builder) {
        return new SinglePassFFmpegJob(ffmpeg, builder);
    }

    public FFmpegJob createJob(FFmpegBuilder builder, ProgressListener listener) {
        return new SinglePassFFmpegJob(ffmpeg, builder, listener);
    }

    /**
     * Creates a two pass job, which will execute FFmpeg twice to produce a better quality output.
     * More info: https://trac.ffmpeg.org/wiki/x264EncodingGuide#twopass
     *
     * @param builder The FFmpegBuilder
     * @return A new two-pass FFmpegJob
     */
    public FFmpegJob createTwoPassJob(FFmpegBuilder builder) {
        return new TwoPassFFmpegJob(ffmpeg, builder);
    }
}
