package ch.imagic.ffmpeg;

import static ch.imagic.ffmpeg.FFmpegTest.argThatHasItem;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import ch.imagic.ffmpeg.lang.NewProcessAnswer;
import ch.imagic.ffmpeg.process.FFMpegProcessFactory;
import java.io.IOException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/** Tests what happens when using avconv */
@RunWith(MockitoJUnitRunner.class)
public class FFmpegAvTest {

    @Mock
    FFMpegProcessFactory runFunc;

    FFmpeg ffmpeg;

    @Before
    public void before() throws IOException {
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), argThatHasItem("-version")))
                .thenAnswer(new NewProcessAnswer("avconv-version"));

        ffmpeg = new FFmpeg(runFunc);
    }

    @Test
    public void testVersion() throws Exception {
        assertEquals("avconv version 11.4, Copyright (c) 2000-2014 the Libav developers", ffmpeg.version());
        assertEquals("avconv version 11.4, Copyright (c) 2000-2014 the Libav developers", ffmpeg.version());
    }

    /**
     * We don't support avconv, so all methods should throw an exception.
     *
     * @throws IOException
     */
    @Test(expected = IllegalArgumentException.class)
    public void testProbeVideo() throws IOException {
        ffmpeg.run(Collections.<String>emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCodecs() throws IOException {
        ffmpeg.codecs();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFormats() throws IOException {
        ffmpeg.formats();
    }
}
