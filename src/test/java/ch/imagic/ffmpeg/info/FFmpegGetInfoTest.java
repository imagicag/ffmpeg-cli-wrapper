package ch.imagic.ffmpeg.info;

import static ch.imagic.ffmpeg.FFmpegTest.argThatHasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import ch.imagic.ffmpeg.FFmpeg;
import ch.imagic.ffmpeg.lang.NewProcessAnswer;
import ch.imagic.ffmpeg.process.FFMpegProcessFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FFmpegGetInfoTest {
    @Mock
    FFMpegProcessFactory runFunc;

    @Before
    public void before() throws IOException {
        // when(runFunc.createProcess(Mockito.any(), Mockito.any(), argThatHasItem("-version")))
        //        .thenAnswer(new NewProcessAnswer("ffmpeg-version"));

        when(runFunc.createProcess(Mockito.any(), Mockito.any(), argThatHasItem("-codecs")))
                .thenAnswer(new NewProcessAnswer("ffmpeg-codecs"));
    }

    @Test
    public void getFFmpegCodecSupportTest() throws IOException {
        List<Codec> videoCodecs = new ArrayList<>();
        List<Codec> audioCodecs = new ArrayList<>();
        List<Codec> subtitleCodecs = new ArrayList<>();
        List<Codec> dataCodecs = new ArrayList<>();
        List<Codec> otherCodecs = new ArrayList<>();

        FFmpeg ffmpeg = new FFmpeg(runFunc);
        ffmpeg.codecs();

        for (Codec codec : ffmpeg.codecs()) {
            switch (codec.getType()) {
                case VIDEO:
                    videoCodecs.add(codec);
                    break;
                case AUDIO:
                    audioCodecs.add(codec);
                    break;
                case SUBTITLE:
                    subtitleCodecs.add(codec);
                    break;
                case DATA:
                    dataCodecs.add(codec);
                    break;
                default:
                    otherCodecs.add(codec);
            }
        }

        assertEquals(245, videoCodecs.size());
        assertEquals(180, audioCodecs.size());
        assertEquals(26, subtitleCodecs.size());
        assertEquals(8, dataCodecs.size());
        assertEquals(0, otherCodecs.size());

        assertTrue("Expected video codec h264", videoCodecs.stream().anyMatch(codec -> "h264".equals(codec.getName())));
        assertTrue("Expected audio codec aac", audioCodecs.stream().anyMatch(codec -> "aac".equals(codec.getName())));
        assertTrue(
                "Expected subtitle codec ssa",
                subtitleCodecs.stream().anyMatch(codec -> "ssa".equals(codec.getName())));
        assertTrue(
                "Expected data codec bin_data",
                dataCodecs.stream().anyMatch(codec -> "bin_data".equals(codec.getName())));
    }
}
