package ch.imagic.ffmpeg;

import static ch.imagic.ffmpeg.FFmpegTest.argThatHasItem;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.gson.Gson;
import java.io.IOException;
import ch.imagic.ffmpeg.fixtures.Samples;
import ch.imagic.ffmpeg.lang.NewProcessAnswer;
import ch.imagic.ffmpeg.nut.Fraction;
import ch.imagic.ffmpeg.probe.FFmpegChapter;
import ch.imagic.ffmpeg.probe.FFmpegProbeResult;
import ch.imagic.ffmpeg.probe.FFmpegStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FFprobeTest {

    @Mock
    ProcessFunction runFunc;

    FFprobe ffprobe;

    static final Gson gson = FFmpegUtils.getGson();

    @Before
    public void before() throws IOException {
        when(runFunc.run(argThatHasItem("-version"))).thenAnswer(new NewProcessAnswer("ffprobe-version"));

        when(runFunc.run(argThatHasItem(Samples.big_buck_bunny_720p_1mb)))
                .thenAnswer(new NewProcessAnswer("ffprobe-big_buck_bunny_720p_1mb.mp4"));

        when(runFunc.run(argThatHasItem(Samples.always_on_my_mind)))
                .thenAnswer(new NewProcessAnswer("ffprobe-always_on_my_mind.mp4"));

        when(runFunc.run(argThatHasItem(Samples.start_pts_test)))
                .thenAnswer(new NewProcessAnswer("ffprobe-start_pts_test"));

        when(runFunc.run(argThatHasItem(Samples.divide_by_zero)))
                .thenAnswer(new NewProcessAnswer("ffprobe-divide-by-zero"));

        when(runFunc.run(argThatHasItem(Samples.book_with_chapters)))
                .thenAnswer(new NewProcessAnswer("book_with_chapters.m4b"));

        when(runFunc.run(argThatHasItem(Samples.side_data_list)))
                .thenAnswer(new NewProcessAnswer("ffprobe-side_data_list"));

        ffprobe = new FFprobe(runFunc);
    }

    @Test
    public void testVersion() throws Exception {
        assertEquals("ffprobe version 3.0.2 Copyright (c) 2007-2016 the FFmpeg developers", ffprobe.version());
        assertEquals("ffprobe version 3.0.2 Copyright (c) 2007-2016 the FFmpeg developers", ffprobe.version());

        verify(runFunc, times(1)).run(argThatHasItem("-version"));
    }

    @Test
    public void testProbeVideo() throws IOException {
        FFmpegProbeResult info = ffprobe.probe(Samples.big_buck_bunny_720p_1mb);
        assertFalse(info.hasError());

        // Only a quick sanity check until we do something better
        assertEquals(2, info.getStreams().size());
        assertEquals(FFmpegStream.CodecType.VIDEO, info.getStreams().get(0).codec_type);
        assertEquals(FFmpegStream.CodecType.AUDIO, info.getStreams().get(1).codec_type);

        assertEquals(6, info.getStreams().get(1).channels);
        assertEquals(48_000, info.getStreams().get(1).sample_rate);

        assertTrue(info.getChapters().isEmpty());
        // System.out.println(FFmpegUtils.getGson().toJson(info));
    }

    @Test
    public void testProbeBookWithChapters() throws IOException {
        FFmpegProbeResult info = ffprobe.probe(Samples.book_with_chapters);
        assertFalse(info.hasError());
        assertEquals(24, info.getChapters().size());

        FFmpegChapter firstChapter = info.getChapters().get(0);
        assertEquals("1/44100", firstChapter.time_base);
        assertEquals(0L, firstChapter.start);
        assertEquals("0.000000", firstChapter.start_time);
        assertEquals(11951309L, firstChapter.end);
        assertEquals("271.004739", firstChapter.end_time);
        assertEquals("01 - Sammy Jay Makes a Fuss", firstChapter.tags.title);

        FFmpegChapter lastChapter = info.getChapters().get(info.getChapters().size() - 1);
        assertEquals("1/44100", lastChapter.time_base);
        assertEquals(237875790L, lastChapter.start);
        assertEquals("5394.008844", lastChapter.start_time);
        assertEquals(248628224L, lastChapter.end);
        assertEquals("5637.828209", lastChapter.end_time);
        assertEquals("24 - Chatterer Has His Turn to Laugh", lastChapter.tags.title);
    }

    @Test
    public void testProbeVideo2() throws IOException {
        FFmpegProbeResult info = ffprobe.probe(Samples.always_on_my_mind);
        assertFalse(info.hasError());

        // Only a quick sanity check until we do something better
        assertEquals(2, info.getStreams().size());
        assertEquals(FFmpegStream.CodecType.VIDEO, info.getStreams().get(0).codec_type);
        assertEquals(FFmpegStream.CodecType.AUDIO, info.getStreams().get(1).codec_type);

        assertEquals(2, info.getStreams().get(1).channels);
        assertEquals(48_000, info.getStreams().get(1).sample_rate);

        // Test a UTF-8 name
        assertEquals("c:\\Users\\Bob\\Always On My Mind [Program Only] - Adelén.mp4", info.getFormat().filename);

        // System.out.println(FFmpegUtils.getGson().toJson(info));
    }

    @Test
    public void testProbeStartPts() throws IOException {
        FFmpegProbeResult info = ffprobe.probe(Samples.start_pts_test);
        assertFalse(info.hasError());

        // Check edge case with a time larger than an integer
        assertEquals(8570867078L, info.getStreams().get(0).start_pts);
    }

    @Test
    public void testProbeDivideByZero() throws IOException {
        // https://github.com/bramp/ffmpeg-cli-wrapper/issues/10
        FFmpegProbeResult info = ffprobe.probe(Samples.divide_by_zero);
        assertFalse(info.hasError());

        assertEquals(Fraction.ZERO, info.getStreams().get(1).codec_time_base);

        // System.out.println(FFmpegUtils.getGson().toJson(info));
    }

    @Test
    public void testProbeSideDataList() throws IOException {
        FFmpegProbeResult info = ffprobe.probe(Samples.side_data_list);

        // Check edge case with a time larger than an integer
        assertEquals(1, info.getStreams().get(0).side_data_list.length);
        assertEquals("Display Matrix", info.getStreams().get(0).side_data_list[0].side_data_type);
        assertEquals(
                "\n00000000:            0      -65536           0\n00000001:        65536           0           0\n00000002:            0           0  1073741824\n",
                info.getStreams().get(0).side_data_list[0].displaymatrix);
        assertEquals(90, info.getStreams().get(0).side_data_list[0].rotation);
    }
}
