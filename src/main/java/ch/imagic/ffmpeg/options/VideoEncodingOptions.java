package ch.imagic.ffmpeg.options;

import ch.imagic.ffmpeg.probe.Fraction;

/**
 * Encoding options for video
 *
 */
public class VideoEncodingOptions {
    public final boolean enabled;
    public final String codec;
    public final Fraction frameRate;
    public final int width;
    public final int height;
    public final long bitRate;
    public final Integer frames;
    public final String filter;
    public final String preset;

    public VideoEncodingOptions(
            boolean enabled,
            String codec,
            Fraction frameRate,
            int width,
            int height,
            long bitRate,
            Integer frames,
            String filter,
            String preset) {
        this.enabled = enabled;
        this.codec = codec;
        this.frameRate = frameRate;
        this.width = width;
        this.height = height;
        this.bitRate = bitRate;
        this.frames = frames;
        this.filter = filter;
        this.preset = preset;
    }
}
