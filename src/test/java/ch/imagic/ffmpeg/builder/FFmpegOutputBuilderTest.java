package ch.imagic.ffmpeg.builder;

import static ch.imagic.ffmpeg.FFmpeg.AUDIO_FORMAT_S16;
import static ch.imagic.ffmpeg.FFmpeg.AUDIO_SAMPLE_48000;
import static ch.imagic.ffmpeg.FFmpeg.FPS_30;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.imagic.ffmpeg.options.AudioEncodingOptions;
import ch.imagic.ffmpeg.options.EncodingOptions;
import ch.imagic.ffmpeg.options.MainEncodingOptions;
import ch.imagic.ffmpeg.options.VideoEncodingOptions;
import ch.imagic.ffmpeg.probe.FFmpegFormat;
import ch.imagic.ffmpeg.probe.FFmpegProbeResult;
import java.util.List;
import org.junit.jupiter.api.Test;

public class FFmpegOutputBuilderTest {

    @Test
    public void testTargetSizeCalculatesVideoBitRate() {
        List<String> args = new FFmpegBuilder()
                .setInput(probe("input.mp4", 10))
                .addOutput("output.mp4")
                .setTargetSize(1_000_000)
                .setPassPaddingBitrate(1024)
                .setAudioBitRate(128000)
                .done()
                .build();

        assertEquals(
                List.of("-y", "-v", "error", "-i", "input.mp4", "-b:v", "670976", "-b:a", "128000", "output.mp4"),
                args);
    }

    @Test
    public void testTargetSizeCalculatesAudioBitRateForAudioOnlyOutput() {
        List<String> args = new FFmpegBuilder()
                .setInput(probe("input.wav", 10))
                .addOutput("output.m4a")
                .setTargetSize(1_000_000)
                .setPassPaddingBitrate(1024)
                .disableVideo()
                .done()
                .build();

        assertEquals(List.of("-y", "-v", "error", "-i", "input.wav", "-vn", "-b:a", "798976", "output.m4a"), args);
    }

    @Test
    public void testTargetSizeMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new FFmpegOutputBuilder().setTargetSize(0));
        assertThrows(IllegalArgumentException.class, () -> new FFmpegOutputBuilder().setTargetSize(-1));
    }

    @Test
    public void testPassPaddingBitRateMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new FFmpegOutputBuilder().setPassPaddingBitrate(0));
        assertThrows(IllegalArgumentException.class, () -> new FFmpegOutputBuilder().setPassPaddingBitrate(-1));
    }

    @Test
    public void testTargetSizeRequiresProbeInput() {
        assertThrows(
                IllegalStateException.class,
                () -> new FFmpegBuilder()
                        .setInput("input.mp4")
                        .addOutput("output.mp4")
                        .setTargetSize(1_000_000)
                        .done()
                        .build());
    }

    @Test
    public void testTargetSizeRejectsMultipleInputs() {
        assertThrows(
                IllegalStateException.class,
                () -> new FFmpegBuilder()
                        .addInput(probe("input1.mp4", 10))
                        .addInput(probe("input2.mp4", 10))
                        .addOutput("output.mp4")
                        .setTargetSize(1_000_000)
                        .done()
                        .build());
    }

    @Test
    public void testTargetSizeRejectsConstantRateFactor() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FFmpegBuilder()
                        .setInput(probe("input.mp4", 10))
                        .addOutput("output.mp4")
                        .setTargetSize(1_000_000)
                        .setConstantRateFactor(23)
                        .done()
                        .build());
    }

    @Test
    public void testDetachedOutputRequiresDestination() {
        assertThrows(
                IllegalStateException.class,
                () -> new FFmpegBuilder()
                        .setInput("input.mp4")
                        .addOutput(new FFmpegOutputBuilder().setVideoCodec("libx264"))
                        .build());
    }

    @Test
    public void testOutputVideoFilterRejectsMultipleInputs() {
        assertThrows(
                IllegalStateException.class,
                () -> new FFmpegBuilder()
                        .addInput("input1.mp4")
                        .addInput("input2.mp4")
                        .addOutput("output.mp4")
                        .setVideoFilter("scale=320:240")
                        .done()
                        .build());
    }

    @Test
    public void testVideoBitRateAndQualityConflict() {
        assertThrows(
                IllegalStateException.class,
                () -> new FFmpegBuilder()
                        .setInput("input.mp4")
                        .addOutput("output.mp4")
                        .setVideoBitRate(800000)
                        .setVideoQuality(2)
                        .done()
                        .build());
    }

    @Test
    public void testAudioBitRateAndQualityConflict() {
        assertThrows(
                IllegalStateException.class,
                () -> new FFmpegBuilder()
                        .setInput("input.mp4")
                        .addOutput("output.mp4")
                        .setAudioBitRate(128000)
                        .setAudioQuality(2)
                        .done()
                        .build());
    }

    @Test
    public void testEnabledCompositeEncodingOptionsDelegateAllSections() {
        EncodingOptions options = new EncodingOptions(
                new MainEncodingOptions("mp4", 1500L, 2000L),
                new AudioEncodingOptions(true, "aac", 2, AUDIO_SAMPLE_48000, AUDIO_FORMAT_S16, 128000, 2.0),
                new VideoEncodingOptions(true, "libx264", FPS_30, 320, 240, 800000, 60, "scale=320:240", "fast"));

        EncodingOptions actual = new FFmpegOutputBuilder()
                .disableAudio()
                .disableVideo()
                .useOptions(options)
                .buildOptions();

        assertEquals("mp4", actual.main.format);
        assertEquals(Long.valueOf(1500), actual.main.startOffset);
        assertEquals(Long.valueOf(2000), actual.main.duration);
        assertEquals(true, actual.audio.enabled);
        assertEquals("aac", actual.audio.codec);
        assertEquals(2, actual.audio.channels);
        assertEquals(AUDIO_SAMPLE_48000, actual.audio.sample_rate);
        assertEquals(AUDIO_FORMAT_S16, actual.audio.sample_format);
        assertEquals(128000, actual.audio.bit_rate);
        assertEquals(Double.valueOf(2), actual.audio.quality);
        assertEquals(true, actual.video.enabled);
        assertEquals("libx264", actual.video.codec);
        assertEquals(FPS_30, actual.video.frame_rate);
        assertEquals(320, actual.video.width);
        assertEquals(240, actual.video.height);
        assertEquals(800000, actual.video.bit_rate);
        assertEquals(Integer.valueOf(60), actual.video.frames);
        assertEquals("scale=320:240", actual.video.filter);
        assertEquals("fast", actual.video.preset);
    }

    private static FFmpegProbeResult probe(String filename, double duration) {
        FFmpegFormat format = new FFmpegFormat();
        format.setFilename(filename);
        format.setDuration(duration);

        FFmpegProbeResult result = new FFmpegProbeResult();
        result.setFormat(format);
        return result;
    }
}
