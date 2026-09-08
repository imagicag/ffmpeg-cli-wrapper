package ch.imagic.ffmpeg.options;

/**
 */
public class EncodingOptions {

    public final MainEncodingOptions main;
    public final AudioEncodingOptions audio;
    public final VideoEncodingOptions video;

    public EncodingOptions(MainEncodingOptions main, AudioEncodingOptions audio, VideoEncodingOptions video) {
        this.main = main;
        this.audio = audio;
        this.video = video;
    }

    public MainEncodingOptions getMain() {
        return main;
    }

    public AudioEncodingOptions getAudio() {
        return audio;
    }

    public VideoEncodingOptions getVideo() {
        return video;
    }
}
