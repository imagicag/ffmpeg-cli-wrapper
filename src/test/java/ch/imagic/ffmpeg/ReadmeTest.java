package ch.imagic.ffmpeg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.imagic.ffmpeg.builder.FFmpegBuilder;
import ch.imagic.ffmpeg.fixtures.Samples;
import ch.imagic.ffmpeg.probe.FFmpegCodecType;
import ch.imagic.ffmpeg.probe.FFmpegFormat;
import ch.imagic.ffmpeg.probe.FFmpegProbeResult;
import ch.imagic.ffmpeg.probe.FFmpegStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Ensures the examples in the README continue to work. */
public class ReadmeTest {

    final Locale locale = Locale.US;
    final FFmpeg ffmpeg = new FFmpeg();
    final FFprobe ffprobe = new FFprobe();

    public ReadmeTest() throws IOException {}

    @Test
    public void testCreateFF() throws IOException {
        FFmpeg ffmpeg = new FFmpeg();
        FFprobe ffprobe = new FFprobe();

        // Construct them, and do nothing with them
    }

    @Test
    public void testVideoEncoding() throws IOException, InterruptedException {
        File inputFile = new File(Samples.big_buck_bunny_720p_1mb);
        FFmpegProbeResult input = ffprobe.probe(inputFile).get();
        Path outputPath = Files.createTempFile("readme-encoding-", ".mp4");

        try {
            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(input)
                    .overrideOutputFiles(true)
                    .addOutput(outputPath.toString())
                    .setFormat("mp4")
                    .setTargetSize(250_000)
                    .disableSubtitle()
                    .setAudioChannels(1)
                    .setAudioCodec("aac")
                    .setAudioSampleRate(48_000)
                    .setAudioBitRate(32_768)
                    .setVideoCodec("libx264")
                    .setVideoFrameRate(24, 1)
                    .setVideoResolution(640, 480)
                    .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                    .done();

            FFMpegJob<Void> job = ffmpeg.run(builder);
            if (!job.await(10, TimeUnit.SECONDS)) {
                job.kill();
                throw new IOException("ffmpeg timed out");
            }
            job.get();

            FFmpegProbeResult output = ffprobe.probe(outputPath.toFile()).get();
            FFmpegStream video = output.getStreams().stream()
                    .filter(stream -> stream.getCodecType() == FFmpegCodecType.VIDEO)
                    .findFirst()
                    .orElseThrow();
            FFmpegStream audio = output.getStreams().stream()
                    .filter(stream -> stream.getCodecType() == FFmpegCodecType.AUDIO)
                    .findFirst()
                    .orElseThrow();

            assertEquals("h264", video.getCodecName());
            assertEquals(640, video.getWidth());
            assertEquals(480, video.getHeight());
            assertEquals(24, video.getAvgFrameRate().intValue());
            assertEquals("aac", audio.getCodecName());
            assertEquals(1, audio.getChannels());
            assertEquals(48_000, audio.getSampleRate());
            assertTrue(output.getFormat().getFormatName().contains("mp4"));
            assertTrue(Files.size(outputPath) > 150_000);
            assertTrue(Files.size(outputPath) < 350_000);
        } finally {
            Files.deleteIfExists(outputPath);
        }
    }

    @Test
    public void testGetMediaInformation() throws IOException {
        FFmpegProbeResult probeResult =
                ffprobe.probe(new File(Samples.big_buck_bunny_720p_1mb)).get();

        FFmpegFormat format = probeResult.getFormat();
        String line1 = String.format(
                locale,
                "File: '%s' ; Format: '%s' ; Duration: %.3fs",
                format.getFilename(),
                format.getFormatLongName(),
                format.getDuration());

        FFmpegStream stream = probeResult.getStreams().get(0);
        String line2 = String.format(
                locale,
                "Codec: '%s' ; Width: %dpx ; Height: %dpx",
                stream.getCodecLongName(),
                stream.getWidth(),
                stream.getHeight());

        assertTrue(line1.startsWith("File: '"));
        assertTrue(
                line1.endsWith(
                        "src/test/resources/ch/imagic/ffmpeg/samples/big_buck_bunny_720p_1mb.mp4' ; Format: 'QuickTime / MOV' ; Duration: 5.312s"));

        assertEquals("Codec: 'H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10' ; Width: 1280px ; Height: 720px", line2);
    }
}
