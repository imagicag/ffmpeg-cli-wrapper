package ch.imagic.ffmpeg.builder;

import static ch.imagic.ffmpeg.FFmpeg.AUDIO_FORMAT_S16;
import static ch.imagic.ffmpeg.FFmpeg.AUDIO_SAMPLE_48000;
import static ch.imagic.ffmpeg.FFmpeg.FPS_30;
import static ch.imagic.ffmpeg.builder.FFmpegBuilder.Verbosity;
import static ch.imagic.ffmpeg.builder.MetadataSpecifier.*;
import static ch.imagic.ffmpeg.builder.StreamSpecifier.tag;
import static ch.imagic.ffmpeg.builder.StreamSpecifier.usable;
import static ch.imagic.ffmpeg.builder.StreamSpecifierType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.imagic.ffmpeg.options.AudioEncodingOptions;
import ch.imagic.ffmpeg.options.EncodingOptions;
import ch.imagic.ffmpeg.options.MainEncodingOptions;
import ch.imagic.ffmpeg.options.VideoEncodingOptions;
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
                .setVerbosity(Verbosity.DEBUG)
                .setUserAgent(
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36")
                .setInput("input")
                .setStartOffset(1500, TimeUnit.MILLISECONDS)
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
                        "debug",
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
                .addOutput("output")
                .disableAudio()
                .disableSubtitle()
                .disableVideo()
                .done()
                .build();

        assertEquals(args, List.of("-y", "-v", "error", "-i", "input", "-vn", "-an", "-sn", "output"));
    }

    @Test
    public void testFilter() {

        List<String> args = new FFmpegBuilder()
                .setInput("input")
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

    /** Tests if all the various encoding options actually get stored and used correctly */
    @Test
    public void testSetOptions() {
        MainEncodingOptions main = new MainEncodingOptions("mp4", 1500L, 2L);
        AudioEncodingOptions audio =
                new AudioEncodingOptions(true, "aac", 1, AUDIO_SAMPLE_48000, AUDIO_FORMAT_S16, 1, 2.0);
        VideoEncodingOptions video = new VideoEncodingOptions(true, "libx264", FPS_30, 320, 240, 1, null, null, null);

        EncodingOptions options = new FFmpegBuilder()
                .setInput("input")
                .addOutput("output")
                .useOptions(main)
                .useOptions(audio)
                .useOptions(video)
                .buildOptions();

        MainEncodingOptions actualMain = options.getMain();
        assertEquals(main.format, actualMain.format);
        assertEquals(main.startOffset, actualMain.startOffset);
        assertEquals(main.duration, actualMain.duration);

        AudioEncodingOptions actualAudio = options.getAudio();
        assertEquals(audio.enabled, actualAudio.enabled);
        assertEquals(audio.codec, actualAudio.codec);
        assertEquals(audio.channels, actualAudio.channels);
        assertEquals(audio.sample_rate, actualAudio.sample_rate);
        assertEquals(audio.sample_format, actualAudio.sample_format);
        assertEquals(audio.bit_rate, actualAudio.bit_rate);
        assertEquals(audio.quality, actualAudio.quality);

        VideoEncodingOptions actualVideo = options.getVideo();
        assertEquals(video.enabled, actualVideo.enabled);
        assertEquals(video.codec, actualVideo.codec);
        assertEquals(video.frame_rate, actualVideo.frame_rate);
        assertEquals(video.width, actualVideo.width);
        assertEquals(video.height, actualVideo.height);
        assertEquals(video.bit_rate, actualVideo.bit_rate);
        assertEquals(video.frames, actualVideo.frames);
        assertEquals(video.filter, actualVideo.filter);
        assertEquals(video.preset, actualVideo.preset);
    }

    @Test
    public void testCopyFromPreservesValuesForDefaultOptions() {
        FFmpegOutputBuilder output = new FFmpegOutputBuilder()
                .setFormat("mp4")
                .setAudioCodec("aac")
                .setAudioChannels(2)
                .setVideoCodec("libx264")
                .setVideoResolution(320, 240);

        output.useOptions(new MainEncodingOptions(null, null, null));
        output.useOptions(new AudioEncodingOptions(false, null, 0, 0, null, 0, null));
        output.useOptions(new VideoEncodingOptions(false, null, null, 0, 0, 0, null, null, null));

        EncodingOptions options = output.buildOptions();
        assertEquals("mp4", options.main.format);
        assertEquals("aac", options.audio.codec);
        assertEquals(2, options.audio.channels);
        assertEquals("libx264", options.video.codec);
        assertEquals(320, options.video.width);
        assertEquals(240, options.video.height);
    }

    @Test
    public void testCopyFromSkipsDisabledSections() {
        EncodingOptions disabledOptions = new EncodingOptions(
                new MainEncodingOptions("mp4", 0L, 0L),
                new AudioEncodingOptions(false, "aac", 2, AUDIO_SAMPLE_48000, AUDIO_FORMAT_S16, 128000, 2.0),
                new VideoEncodingOptions(false, "libx264", FPS_30, 320, 240, 1000000, 10, "scale=320:240", "fast"));

        EncodingOptions actual =
                new FFmpegOutputBuilder().useOptions(disabledOptions).buildOptions();

        assertEquals("mp4", actual.main.format);
        assertEquals(Long.valueOf(0), actual.main.startOffset);
        assertEquals(Long.valueOf(0), actual.main.duration);
        assertEquals(null, actual.audio.codec);
        assertEquals(0, actual.audio.channels);
        assertEquals(null, actual.video.codec);
        assertEquals(0, actual.video.width);
    }

    @Test
    public void testMultipleOutputs() {

        List<String> args = new FFmpegBuilder()
                .setInput("input")
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
                        .addOutput(URI.create("udp://10.1.0.102:1234"))
                        .setFilename("filename")
                        .done()
                        .build());
    }

    @Test
    public void testAddEmptyFilename() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FFmpegBuilder().setInput("input").addOutput("").done().build());
    }

    @Test
    public void testSetEmptyFilename() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FFmpegBuilder()
                        .setInput("input")
                        .addOutput("output")
                        .setFilename("")
                        .done()
                        .build());
    }

    @Test
    public void testMetaTags() {

        List<String> args = new FFmpegBuilder()
                .setInput("input")
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
                .addExtraArgs("-a", "b")
                .setInput("input")
                .addOutput("output")
                .addExtraArgs("-c", "d")
                .disableAudio()
                .disableSubtitle()
                .done()
                .build();

        assertEquals(args, List.of("-y", "-v", "error", "-a", "b", "-i", "input", "-an", "-sn", "-c", "d", "output"));
    }

    @Test
    public void testNothing() {
        assertThrows(IllegalArgumentException.class, () -> new FFmpegBuilder().build());
    }

    @Test
    public void testMultipleInput() {
        List<String> args = new FFmpegBuilder()
                .addInput("input1")
                .addInput("input2")
                .addOutput("output")
                .done()
                .build();

        assertEquals(args, List.of("-y", "-v", "error", "-i", "input1", "-i", "input2", "output"));
    }

    @Test
    public void testAlternativeBuilderPattern() {
        List<String> args = new FFmpegBuilder()
                .addInput("input")
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
}
