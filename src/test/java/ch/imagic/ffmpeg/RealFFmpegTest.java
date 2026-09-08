package ch.imagic.ffmpeg;

import ch.imagic.ffmpeg.builder.FFmpegBuilder;
import ch.imagic.ffmpeg.fixtures.Samples;
import ch.imagic.ffmpeg.probe.FFmpegProbeResult;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class RealFFmpegTest {

    final FFmpeg ffmpeg = new FFmpeg();
    final FFprobe ffprobe = new FFprobe();

    @TempDir
    Path temporaryDirectory;

    public RealFFmpegTest() throws IOException {}

    @Test
    public void testMovBeforeMdatCleanWay() throws IOException, InterruptedException {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setVerbosity(FFmpegBuilder.Verbosity.TRACE)
                .setInput(Samples.BIG_BUCK_BUNNY_720P_1MB)
                .addStdoutOutput()
                .setDuration(0, TimeUnit.MILLISECONDS)
                .setFormat("null")
                .done();

        try (FFMpegJob<Boolean> job = ffmpeg.runCaptureStderr(builder, input -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("type:'moov'")) {
                    return true;
                } else if (line.contains("type:'mdat'")) {
                    return false;
                }
            }

            throw new IOException("Failed to parse");
        })) {

            Assertions.assertTrue(job.get());
            Assertions.assertEquals(0, job.getExitCode().orElse(-1));
        }
    }

    @Test
    public void testMovBeforeMdatNoOutputWay() throws IOException {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setVerbosity(FFmpegBuilder.Verbosity.TRACE)
                .setInput(Samples.BIG_BUCK_BUNNY_720P_1MB)
                .noOutput();

        try (FFMpegJob<Boolean> job = ffmpeg.runCaptureStderr(builder, input -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("type:'moov'")) {
                    return true;
                } else if (line.contains("type:'mdat'")) {
                    return false;
                }
            }

            throw new IOException("Failed to parse");
        })) {
            Assertions.assertTrue(job.get());
            Assertions.assertNotEquals(0, job.getExitCode().orElse(-1));
        }
    }

    @Test
    public void testExceptionOccursDuringParsing() throws IOException {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setVerbosity(FFmpegBuilder.Verbosity.TRACE)
                .setInput(Samples.BIG_BUCK_BUNNY_720P_1MB)
                .noOutput();

        try (FFMpegJob<Boolean> job = ffmpeg.runCaptureStderr(builder, input -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            for (int i = 0; i < 10; i++) {
                if (reader.readLine() == null) {
                    break;
                }
            }

            throw new IOException("BEEP");
        })) {

            try {
                job.get();
                Assertions.fail("exception expected");
            } catch (IOException ioe) {
                Assertions.assertEquals("BEEP", ioe.getMessage());
            }
        }
    }

    @Test
    public void testTranscodeFromInputStreamToOutputStream() throws IOException {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput("-")
                .addStdoutOutput()
                .setFormat("avi")
                .done();
        ByteArrayOutputStream aviOutput = new ByteArrayOutputStream();

        try (var mp4Input = Files.newInputStream(Path.of(Samples.BIG_BUCK_BUNNY_720P_1MB));
                FFMpegJob<Void> job = ffmpeg.run(
                        builder,
                        FFMpegStreamConsumer.toOutputStream(aviOutput),
                        FFMpegStreamConsumer.noop(),
                        (stdout, stderr) -> null,
                        mp4Input)) {
            job.get();
        }

        Path aviFile = temporaryDirectory.resolve("transcoded.avi");
        Files.write(aviFile, aviOutput.toByteArray());
        FFmpegProbeResult probeResult = ffprobe.probe(aviFile.toFile()).get();

        Assertions.assertTrue(probeResult.getFormat().getFormatName().contains("avi"));
    }
}
