package ch.imagic.ffmpeg.builder;

import static ch.imagic.ffmpeg.FFmpeg.FPS_30;
import static ch.imagic.ffmpeg.builder.FFmpegBuilder.Verbosity;
import static ch.imagic.ffmpeg.builder.MetadataSpecifier.*;
import static ch.imagic.ffmpeg.builder.StreamSpecifier.tag;
import static ch.imagic.ffmpeg.builder.StreamSpecifier.usable;
import static ch.imagic.ffmpeg.builder.StreamSpecifierType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 */
public class FFmpegBuilderTest {

    public FFmpegBuilderTest() throws IOException {}

    @Test
    public void testNormal() {

        List<String> args = new FFmpegBuilder()
                .setVerbosity(Verbosity.TRACE)
                .setInput("input")
                .setUserAgent(
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36")
                .setStartOffset(1500, TimeUnit.MILLISECONDS)
                .done()
                .overrideOutputFiles(true)
                .addOutput("output")
                .setFormat("mp4")
                .setStartOffset(500, TimeUnit.MILLISECONDS)
                .setAudioCodec("aac")
                .setAudioChannels(1)
                .setAudioSampleRate(48000)
                .setAudioBitStreamFilter("bar")
                .setAudioQuality(1)
                .setVideoCodec("libx264")
                .setVideoFrameRate(FPS_30)
                .setVideoResolution(320, 240)
                .setVideoBitStreamFilter("foo")
                .setVideoQuality(2)
                .done()
                .build();

        assertEquals(
                args,
                List.of(
                        "-y",
                        "-v",
                        "trace",
                        "-user_agent",
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36",
                        "-ss",
                        "00:00:01.5",
                        "-i",
                        "input",
                        "-f",
                        "mp4",
                        "-ss",
                        "00:00:00.5",
                        "-vcodec",
                        "libx264",
                        "-s",
                        "320x240",
                        "-r",
                        "30/1",
                        "-qscale:v",
                        "2",
                        "-bsf:v",
                        "foo",
                        "-acodec",
                        "aac",
                        "-ac",
                        "1",
                        "-ar",
                        "48000",
                        "-qscale:a",
                        "1",
                        "-bsf:a",
                        "bar",
                        "output"));
    }

    @Test
    public void testDisabled() {

        List<String> args = new FFmpegBuilder()
                .setInput("input")
                .done()
                .addOutput("output")
                .disableAudio()
                .disableSubtitle()
                .disableVideo()
                .done()
                .build();

        assertEquals(args, List.of("-y", "-v", "error", "-i", "input", "-vn", "-an", "-sn", "output"));
    }

    @Test
    public void testSafe() {
        FFmpegBuilder safeBuilder = new FFmpegBuilder();
        FFmpegInputBuilder safeInput = safeBuilder.setInput("input").setSafe(true);
        safeInput.done().addOutput("output");

        assertEquals(true, safeInput.getSafe());
        assertEquals(List.of("-y", "-v", "error", "-safe", "1", "-i", "input", "output"), safeBuilder.build());

        FFmpegBuilder unsafeBuilder = new FFmpegBuilder();
        FFmpegInputBuilder unsafeInput = unsafeBuilder.setInput("input").setSafe(false);
        unsafeInput.done().addOutput("output");

        assertEquals(false, unsafeInput.getSafe());
        assertEquals(List.of("-y", "-v", "error", "-safe", "0", "-i", "input", "output"), unsafeBuilder.build());
    }

    @Test
    public void testExitOnError() {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setExitOnError(true)
                .setInput("input")
                .done()
                .addOutput("output")
                .done();

        assertEquals(true, builder.getExitOnError());
        assertEquals(List.of("-y", "-v", "error", "-xerror", "-i", "input", "output"), builder.build());

        builder.setExitOnError(false);

        assertEquals(false, builder.getExitOnError());
        assertEquals(List.of("-y", "-v", "error", "-i", "input", "output"), builder.build());
    }

    @Test
    public void testFilter() {

        List<String> args = new FFmpegBuilder()
                .setInput("input")
                .done()
                .addOutput("output")
                .disableAudio()
                .disableSubtitle()
                .setVideoFilter("scale='trunc(ow/a/2)*2:320'")
                .done()
                .build();

        assertEquals(
                args,
                List.of(
                        "-y",
                        "-v",
                        "error",
                        "-i",
                        "input",
                        "-vf",
                        "scale='trunc(ow/a/2)*2:320'",
                        "-an",
                        "-sn",
                        "output"));
    }

    @Test
    public void testFilterAndScale() {

        List<String> args = new FFmpegBuilder()
                .setInput("input")
                .done()
                .addOutput("output")
                .setVideoResolution(320, 240)
                .setVideoFilter("scale='trunc(ow/a/2)*2:320'")
                .done()
                .build();

        assertEquals(
                args,
                List.of(
                        "-y",
                        "-v",
                        "error",
                        "-i",
                        "input",
                        "-s",
                        "320x240",
                        "-vf",
                        "scale='trunc(ow/a/2)*2:320'",
                        "output"));
    }

    @Test
    public void testMultipleOutputs() {

        List<String> args = new FFmpegBuilder()
                .setInput("input")
                .done()
                .addOutput("output1")
                .setVideoResolution(320, 240)
                .done()
                .addOutput("output2")
                .setVideoResolution(640, 480)
                .done()
                .addOutput("output3")
                .setVideoResolution("ntsc")
                .done()
                .build();

        assertEquals(
                args,
                List.of(
                        "-y", "-v", "error", "-i", "input", "-s", "320x240", "output1", "-s", "640x480", "output2",
                        "-s", "ntsc", "output3"));
    }

    @Test
    public void testConflictingVideoSize() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FFmpegBuilder()
                        .setInput("input")
                        .done()
                        .addOutput("output")
                        .setVideoResolution(320, 240)
                        .setVideoResolution("ntsc")
                        .done()
                        .build());
    }

    @Test
    public void testURIOutput() {

        List<String> args = new FFmpegBuilder()
                .setInput("input")
                .done()
                .addOutput(URI.create("udp://10.1.0.102:1234"))
                .setVideoResolution(320, 240)
                .done()
                .build();

        assertEquals(args, List.of("-y", "-v", "error", "-i", "input", "-s", "320x240", "udp://10.1.0.102:1234"));
    }

    @Test
    public void testURIAndFilenameOutput() {
        assertThrows(
                IllegalStateException.class,
                () -> new FFmpegBuilder()
                        .setInput("input")
                        .done()
                        .addOutput(URI.create("udp://10.1.0.102:1234"))
                        .setFilename("filename")
                        .done()
                        .build());
    }

    @Test
    public void testAddEmptyFilename() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FFmpegBuilder()
                        .setInput("input")
                        .done()
                        .addOutput("")
                        .done()
                        .build());
    }

    @Test
    public void testSetEmptyFilename() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FFmpegBuilder()
                        .setInput("input")
                        .done()
                        .addOutput("output")
                        .setFilename("")
                        .done()
                        .build());
    }

    @Test
    public void testMetaTags() {

        List<String> args = new FFmpegBuilder()
                .setInput("input")
                .done()
                .addOutput("output")
                .addMetaTag("comment", "My Comment")
                .addMetaTag("title", "\"Video\"")
                .addMetaTag("author", "a=b:c")
                .done()
                .build();

        assertEquals(
                args,
                List.of(
                        "-y",
                        "-v",
                        "error",
                        "-i",
                        "input",
                        "-metadata",
                        "comment=My Comment",
                        "-metadata",
                        "title=\"Video\"",
                        "-metadata",
                        "author=a=b:c",
                        "output"));
    }

    @Test
    public void testMetaTagsWithSpecifier() {

        List<String> args = new FFmpegBuilder()
                .setInput("input")
                .done()
                .addOutput("output")
                .addMetaTag("title", "Movie Title")
                .addMetaTag(chapter(0), "author", "Bob")
                .addMetaTag(program(0), "comment", "Awesome")
                .addMetaTag(stream(0), "copyright", "Megacorp")
                .addMetaTag(stream(Video), "framerate", "24fps")
                .addMetaTag(stream(Video, 0), "artist", "Joe")
                .addMetaTag(stream(Audio, 0), "language", "eng")
                .addMetaTag(stream(Subtitle, 0), "language", "fre")
                .addMetaTag(stream(usable()), "year", "2010")
                .addMetaTag(stream(tag("key")), "a", "b")
                .addMetaTag(stream(tag("key", "value")), "a", "b")
                .done()
                .build();

        assertEquals(
                args,
                List.of(
                        "-y",
                        "-v",
                        "error",
                        "-i",
                        "input",
                        "-metadata",
                        "title=Movie Title",
                        "-metadata:c:0",
                        "author=Bob",
                        "-metadata:p:0",
                        "comment=Awesome",
                        "-metadata:s:0",
                        "copyright=Megacorp",
                        "-metadata:s:v",
                        "framerate=24fps",
                        "-metadata:s:v:0",
                        "artist=Joe",
                        "-metadata:s:a:0",
                        "language=eng",
                        "-metadata:s:s:0",
                        "language=fre",
                        "-metadata:s:u",
                        "year=2010",
                        "-metadata:s:m:key",
                        "a=b",
                        "-metadata:s:m:key:value",
                        "a=b",
                        "output"));
    }

    @Test
    public void testExtraArgs() {
        List<String> args = new FFmpegBuilder()
                .setInput("input")
                .addExtraArgs("-a", "b")
                .addExtraArgs("-c", "d")
                .done()
                .addOutput("output")
                .addExtraArgs("-e", "f")
                .disableAudio()
                .disableSubtitle()
                .done()
                .build();

        assertEquals(
                args,
                List.of("-y", "-v", "error", "-a", "b", "-c", "d", "-i", "input", "-an", "-sn", "-e", "f", "output"));
    }

    @Test
    public void testInputExtraArgsApplyToIndividualInputs() {
        FFmpegBuilder builder = new FFmpegBuilder();
        FFmpegInputBuilder firstInput = builder.addInput("input1")
                .setFormat("rawvideo")
                .setInputPixelFormat("bgr24")
                .setVideoSize(640, 360)
                .addExtraArgs("-thread_queue_size", "512");
        FFmpegInputBuilder secondInput =
                firstInput.done().addInput("input2").setFormat("mpegts").setStartOffset(2, TimeUnit.SECONDS);

        secondInput.done().addOutput("output");

        assertEquals(List.of("-thread_queue_size", "512"), firstInput.getExtraArgs());
        assertEquals(List.of(), secondInput.getExtraArgs());
        assertEquals(
                List.of(
                        "-y",
                        "-v",
                        "error",
                        "-f",
                        "rawvideo",
                        "-pix_fmt",
                        "bgr24",
                        "-video_size",
                        "640x360",
                        "-thread_queue_size",
                        "512",
                        "-i",
                        "input1",
                        "-ss",
                        "00:00:02",
                        "-f",
                        "mpegts",
                        "-i",
                        "input2",
                        "output"),
                builder.build());
    }

    @Test
    public void testInputExtraArgsRequireValues() {
        FFmpegInputBuilder input = new FFmpegBuilder().addInput("input");

        assertThrows(IllegalArgumentException.class, input::addExtraArgs);
        assertThrows(IllegalArgumentException.class, () -> input.addExtraArgs(""));
        assertThrows(NullPointerException.class, () -> input.addExtraArgs("-f", null));
    }

    @Test
    public void testGlobalExtraArgsArePlacedBeforeInputs() {
        FFmpegBuilder builder = new FFmpegBuilder().addExtraArgs("-benchmark").addExtraArgs("-filter_threads", "2");
        builder.addInput("input")
                .addExtraArgs("-thread_queue_size", "512")
                .done()
                .addOutput("output")
                .done();

        assertEquals(List.of("-benchmark", "-filter_threads", "2"), builder.getExtraArgs());
        assertEquals(
                List.of(
                        "-y",
                        "-v",
                        "error",
                        "-benchmark",
                        "-filter_threads",
                        "2",
                        "-thread_queue_size",
                        "512",
                        "-i",
                        "input",
                        "output"),
                builder.build());
    }

    @Test
    public void testGlobalExtraArgsRequireValues() {
        FFmpegBuilder builder = new FFmpegBuilder();

        assertThrows(IllegalArgumentException.class, builder::addExtraArgs);
        assertThrows(IllegalArgumentException.class, () -> builder.addExtraArgs(""));
        assertThrows(NullPointerException.class, () -> builder.addExtraArgs("-flag", null));
    }

    @Test
    public void testNothing() {
        assertThrows(IllegalArgumentException.class, () -> new FFmpegBuilder().build());
    }

    @Test
    public void testNoOutput() {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setVerbosity(Verbosity.TRACE)
                .setInput("input.mp4")
                .done()
                .noOutput();

        assertEquals(true, builder.getNoOutput());
        assertEquals(List.of("-y", "-v", "trace", "-i", "input.mp4"), builder.build());
    }

    @Test
    public void testAddingOutputClearsNoOutput() {
        FFmpegBuilder filenameBuilder = new FFmpegBuilder().noOutput();
        filenameBuilder.addOutput("output.mp4");
        assertEquals(false, filenameBuilder.getNoOutput());

        FFmpegBuilder uriBuilder = new FFmpegBuilder().noOutput();
        uriBuilder.addOutput(URI.create("udp://localhost:1234"));
        assertEquals(false, uriBuilder.getNoOutput());

        FFmpegBuilder outputBuilder = new FFmpegBuilder().noOutput();
        outputBuilder.addOutput(new FFmpegOutputBuilder().setFilename("output.mp4"));
        assertEquals(false, outputBuilder.getNoOutput());

        FFmpegBuilder stdoutBuilder = new FFmpegBuilder().noOutput();
        stdoutBuilder.addStdoutOutput();
        assertEquals(false, stdoutBuilder.getNoOutput());
    }

    @Test
    public void testMultipleInput() {
        List<String> args = new FFmpegBuilder()
                .addInput("input1")
                .done()
                .addInput("input2")
                .done()
                .addOutput("output")
                .done()
                .build();

        assertEquals(args, List.of("-y", "-v", "error", "-i", "input1", "-i", "input2", "output"));
    }

    @Test
    public void testAlternativeBuilderPattern() {
        List<String> args = new FFmpegBuilder()
                .addInput("input")
                .done()
                .addOutput(new FFmpegOutputBuilder().setFilename("output.mp4").setVideoCodec("libx264"))
                .addOutput(new FFmpegOutputBuilder().setFilename("output.flv").setVideoCodec("flv"))
                .build();

        assertEquals(
                args,
                List.of(
                        "-y",
                        "-v",
                        "error",
                        "-i",
                        "input",
                        "-vcodec",
                        "libx264",
                        "output.mp4",
                        "-vcodec",
                        "flv",
                        "output.flv"));
    }

    @Test
    public void testPresets() {
        List<String> args = new FFmpegBuilder()
                .addInput("input")
                .done()
                .addOutput("output")
                .setPreset("a")
                .setPresetFilename("b")
                .setVideoPreset("c")
                .setAudioPreset("d")
                .setSubtitlePreset("e")
                .done()
                .build();

        assertEquals(
                args,
                List.of(
                        "-y", "-v", "error", "-i", "input", "-preset", "a", "-fpre", "b", "-vpre", "c", "-apre", "d",
                        "-spre", "e", "output"));
    }

    @Test
    public void testFirstPassCommand() {
        List<String> args = new FFmpegBuilder()
                .setPass(1)
                .setPassDirectory("logs/")
                .setPassPrefix("encode")
                .setInput("input.mp4")
                .done()
                .addOutput("output.mp4")
                .setFormat("mp4")
                .setVideoCodec("libx264")
                .setVideoBitRate(800000)
                .setAudioCodec("aac")
                .done()
                .build();

        assertEquals(
                List.of(
                        "-y",
                        "-v",
                        "error",
                        "-i",
                        "input.mp4",
                        "-pass",
                        "1",
                        "-passlogfile",
                        "logs/encode",
                        "-f",
                        "mp4",
                        "-vcodec",
                        "libx264",
                        "-b:v",
                        "800000",
                        "-an",
                        "/dev/null"),
                args);
    }

    @Test
    public void testSecondPassCommand() {
        List<String> args = new FFmpegBuilder()
                .setPass(2)
                .setInput("input.mp4")
                .done()
                .addOutput("output.mp4")
                .setFormat("mp4")
                .setVideoBitRate(800000)
                .setAudioCodec("aac")
                .setAudioBitRate(128000)
                .done()
                .build();

        assertEquals(
                List.of(
                        "-y",
                        "-v",
                        "error",
                        "-i",
                        "input.mp4",
                        "-pass",
                        "2",
                        "-f",
                        "mp4",
                        "-b:v",
                        "800000",
                        "-acodec",
                        "aac",
                        "-b:a",
                        "128000",
                        "output.mp4"),
                args);
    }

    @Test
    public void testTwoPassRequiresFormat() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FFmpegBuilder()
                        .setPass(1)
                        .setInput("input.mp4")
                        .done()
                        .addOutput("output.mp4")
                        .setVideoBitRate(800000)
                        .done()
                        .build());
    }

    @Test
    public void testTwoPassRequiresTargetSizeOrVideoBitRate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FFmpegBuilder()
                        .setPass(1)
                        .setInput("input.mp4")
                        .done()
                        .addOutput("output.mp4")
                        .setFormat("mp4")
                        .done()
                        .build());
    }

    @Test
    public void testInputAndGlobalFlagOrdering() {
        List<String> args = new FFmpegBuilder()
                .overrideOutputFiles(false)
                .setVerbosity(Verbosity.INFO)
                .addInput("input1")
                .setUserAgent("agent")
                .setStartOffset(2, TimeUnit.SECONDS)
                .setFormat("lavfi")
                .readAtNativeFrameRate()
                .addExtraArgs("-thread_queue_size", "512")
                .done()
                .addInput("input2")
                .done()
                .setPass(2)
                .setPassDirectory("logs/")
                .setPassPrefix("pass")
                .setAudioFilter("volume=1")
                .setComplexFilter("[0:v][1:v]hstack")
                .addOutput("output.mp4")
                .setFormat("mp4")
                .setVideoBitRate(800000)
                .done()
                .build();

        assertEquals(
                List.of(
                        "-n",
                        "-v",
                        "info",
                        "-user_agent",
                        "agent",
                        "-ss",
                        "00:00:02",
                        "-f",
                        "lavfi",
                        "-re",
                        "-thread_queue_size",
                        "512",
                        "-i",
                        "input1",
                        "-i",
                        "input2",
                        "-pass",
                        "2",
                        "-passlogfile",
                        "logs/pass",
                        "-af",
                        "volume=1",
                        "-filter_complex",
                        "[0:v][1:v]hstack",
                        "-f",
                        "mp4",
                        "-b:v",
                        "800000",
                        "output.mp4"),
                args);
    }
}
