package ch.imagic.ffmpeg.options;

/**
 */
public class MainEncodingOptions {
    public final String format;
    public final Long startOffset;
    public final Long duration;

    public MainEncodingOptions(String format, Long startOffset, Long duration) {
        this.format = format;
        this.startOffset = startOffset;
        this.duration = duration;
    }
}
