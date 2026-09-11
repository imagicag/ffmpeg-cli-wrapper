package ch.imagic.ffmpeg.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    public void testVideoBFrames() {
        FFmpegOutputBuilder output = new FFmpegBuilder()
                .setInput("input.mp4")
                .addOutput("output.mp4")
                .setVideoBFrames(0);

        assertEquals(Integer.valueOf(0), output.getVideoBFrames());
        assertEquals(
                List.of("-y", "-v", "error", "-i", "input.mp4", "-bf", "0", "output.mp4"),
                output.done().build());
    }

    @Test
    public void testVideoBFramesMustNotBeNegative() {
        assertThrows(IllegalArgumentException.class, () -> new FFmpegOutputBuilder().setVideoBFrames(-1));
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

    private static FFmpegProbeResult probe(String filename, double duration) {
        FFmpegFormat format = new FFmpegFormat();
        format.setFilename(filename);
        format.setDuration(duration);

        FFmpegProbeResult result = new FFmpegProbeResult();
        result.setFormat(format);
        return result;
    }
}
