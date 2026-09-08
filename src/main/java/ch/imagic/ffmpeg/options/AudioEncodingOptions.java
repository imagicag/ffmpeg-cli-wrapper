package ch.imagic.ffmpeg.options;

/**
 * Encoding options for audio
 *
 */
public class AudioEncodingOptions {

    public final boolean enabled;
    public final String codec;
    public final int channels;
    public final int sampleRate;
    public final String sampleFormat;
    public final long bitRate;
    public final Double quality;

    public AudioEncodingOptions(
            boolean enabled,
            String codec,
            int channels,
            int sampleRate,
            String sampleFormat,
            long bitRate,
            Double quality) {
        this.enabled = enabled;
        this.codec = codec;
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.sampleFormat = sampleFormat;
        this.bitRate = bitRate;
        this.quality = quality;
    }
}
