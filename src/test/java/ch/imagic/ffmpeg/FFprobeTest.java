package ch.imagic.ffmpeg;

import static ch.imagic.ffmpeg.FFmpegTest.argThatHasItem;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ch.imagic.ffmpeg.fixtures.Samples;
import ch.imagic.ffmpeg.lang.NewProcessAnswer;
import ch.imagic.ffmpeg.probe.FFmpegChapter;
import ch.imagic.ffmpeg.probe.FFmpegProbeResult;
import ch.imagic.ffmpeg.probe.FFmpegStream;
import ch.imagic.ffmpeg.probe.Fraction;
import ch.imagic.ffmpeg.process.FFMpegProcessFactory;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FFprobeTest {

    @Mock
    FFMpegProcessFactory runFunc;

    FFprobe ffprobe;

    static final Gson gson = FFmpegUtils.getGson();

    @BeforeEach
    public void before() throws IOException {
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), argThatHasItem("-version")))
                .thenAnswer(new NewProcessAnswer("ffprobe-version"));

        when(runFunc.createProcess(
                        Mockito.any(),
                        Mockito.any(),
                        argThatHasItem(new File(Samples.big_buck_bunny_720p_1mb).getAbsolutePath())))
                .thenAnswer(new NewProcessAnswer("ffprobe-big_buck_bunny_720p_1mb.mp4"));

        when(runFunc.createProcess(
                        Mockito.any(),
                        Mockito.any(),
                        argThatHasItem(new File(Samples.always_on_my_mind).getAbsolutePath())))
                .thenAnswer(new NewProcessAnswer("ffprobe-always_on_my_mind.mp4"));

        when(runFunc.createProcess(
                        Mockito.any(),
                        Mockito.any(),
                        argThatHasItem(new File(Samples.start_pts_test).getAbsolutePath())))
                .thenAnswer(new NewProcessAnswer("ffprobe-start_pts_test"));

        when(runFunc.createProcess(
                        Mockito.any(),
                        Mockito.any(),
                        argThatHasItem(new File(Samples.divide_by_zero).getAbsolutePath())))
                .thenAnswer(new NewProcessAnswer("ffprobe-divide-by-zero"));

        when(runFunc.createProcess(
                        Mockito.any(),
                        Mockito.any(),
                        argThatHasItem(new File(Samples.book_with_chapters).getAbsolutePath())))
                .thenAnswer(new NewProcessAnswer("book_with_chapters.m4b"));

        when(runFunc.createProcess(
                        Mockito.any(),
                        Mockito.any(),
                        argThatHasItem(new File(Samples.side_data_list).getAbsolutePath())))
                .thenAnswer(new NewProcessAnswer("ffprobe-side_data_list"));

        ffprobe = new FFprobe(runFunc);
    }

    @Test
    public void testVersion() throws Exception {
        assertEquals("ffprobe version 3.0.2 Copyright (c) 2007-2016 the FFmpeg developers", ffprobe.version());
        assertEquals("ffprobe version 3.0.2 Copyright (c) 2007-2016 the FFmpeg developers", ffprobe.version());

        verify(runFunc, times(1)).createProcess(Mockito.any(), Mockito.any(), argThatHasItem("-version"));
    }

    @Test
    public void testProbeVideo() throws IOException {
        FFmpegProbeResult info =
                ffprobe.probe(new File(Samples.big_buck_bunny_720p_1mb)).get();
        assertFalse(info.hasError());

        // Only a quick sanity check until we do something better
        assertEquals(2, info.getStreams().size());
        assertEquals(FFmpegStream.CodecType.VIDEO, info.getStreams().get(0).getCodecType());
        assertEquals(FFmpegStream.CodecType.AUDIO, info.getStreams().get(1).getCodecType());

        assertEquals(6, info.getStreams().get(1).getChannels());
        assertEquals(48_000, info.getStreams().get(1).getSampleRate());

        assertTrue(info.getChapters().isEmpty());
        // System.out.println(FFmpegUtils.getGson().toJson(info));
    }

    @Test
    public void testProbeBookWithChapters() throws IOException {
        FFmpegProbeResult info =
                ffprobe.probe(new File(Samples.book_with_chapters)).get();
        assertFalse(info.hasError());
        assertEquals(24, info.getChapters().size());

        FFmpegChapter firstChapter = info.getChapters().get(0);
        assertEquals("1/44100", firstChapter.getTimeBase());
        assertEquals(0L, firstChapter.getStart());
        assertEquals("0.000000", firstChapter.getStartTime());
        assertEquals(11951309L, firstChapter.getEnd());
        assertEquals("271.004739", firstChapter.getEndTime());
        assertEquals("01 - Sammy Jay Makes a Fuss", firstChapter.getTags().getTitle());

        FFmpegChapter lastChapter = info.getChapters().get(info.getChapters().size() - 1);
        assertEquals("1/44100", lastChapter.getTimeBase());
        assertEquals(237875790L, lastChapter.getStart());
        assertEquals("5394.008844", lastChapter.getStartTime());
        assertEquals(248628224L, lastChapter.getEnd());
        assertEquals("5637.828209", lastChapter.getEndTime());
        assertEquals(
                "24 - Chatterer Has His Turn to Laugh", lastChapter.getTags().getTitle());
    }

    @Test
    public void testProbeVideo2() throws IOException {
        FFmpegProbeResult info =
                ffprobe.probe(new File(Samples.always_on_my_mind)).get();
        assertFalse(info.hasError());

        // Only a quick sanity check until we do something better
        assertEquals(2, info.getStreams().size());
        assertEquals(FFmpegStream.CodecType.VIDEO, info.getStreams().get(0).getCodecType());
        assertEquals(FFmpegStream.CodecType.AUDIO, info.getStreams().get(1).getCodecType());

        assertEquals(2, info.getStreams().get(1).getChannels());
        assertEquals(48_000, info.getStreams().get(1).getSampleRate());

        // Test a UTF-8 name
        assertEquals(
                "c:\\Users\\Bob\\Always On My Mind [Program Only] - Adelén.mp4",
                info.getFormat().getFilename());

        // System.out.println(FFmpegUtils.getGson().toJson(info));
    }

    @Test
    public void testProbeStartPts() throws IOException {
        FFmpegProbeResult info = ffprobe.probe(new File(Samples.start_pts_test)).get();
        assertFalse(info.hasError());

        // Check edge case with a time larger than an integer
        assertEquals(8570867078L, info.getStreams().get(0).getStartPts());
    }

    @Test
    public void testProbeDivideByZero() throws IOException {
        // https://github.com/bramp/ffmpeg-cli-wrapper/issues/10
        FFmpegProbeResult info = ffprobe.probe(new File(Samples.divide_by_zero)).get();
        assertFalse(info.hasError());

        assertEquals(Fraction.ZERO, info.getStreams().get(1).getCodecTimeBase());

        // System.out.println(FFmpegUtils.getGson().toJson(info));
    }

    @Test
    public void testProbeSideDataList() throws IOException {
        FFmpegProbeResult info = ffprobe.probe(new File(Samples.side_data_list)).get();

        // Check edge case with a time larger than an integer
        assertEquals(1, info.getStreams().get(0).getSideDataList().length);
        assertEquals(
                "Display Matrix", info.getStreams().get(0).getSideDataList()[0].getSideDataType());
        assertEquals(
                "\n00000000:            0      -65536           0\n00000001:        65536           0           0\n00000002:            0           0  1073741824\n",
                info.getStreams().get(0).getSideDataList()[0].getDisplayMatrix());
        assertEquals(90, info.getStreams().get(0).getSideDataList()[0].getRotation());
    }
}
