package ch.imagic.ffmpeg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ch.imagic.ffmpeg.fixtures.Samples;
import ch.imagic.ffmpeg.probe.FFmpegFormat;
import ch.imagic.ffmpeg.probe.FFmpegProbeResult;
import ch.imagic.ffmpeg.probe.FFmpegStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import org.junit.Test;

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
