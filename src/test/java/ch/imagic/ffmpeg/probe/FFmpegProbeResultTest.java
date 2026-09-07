package ch.imagic.ffmpeg.probe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class FFmpegProbeResultTest {

    @Test
    public void collectionGettersReturnImmutableSnapshots() {
        FFmpegProbeResult result = new FFmpegProbeResult();

        FFmpegStream stream = new FFmpegStream();
        result.streams = new ArrayList<>(List.of(stream));
        List<FFmpegStream> streams = result.getStreams();
        result.streams.clear();
        assertEquals(List.of(stream), streams);
        assertThrows(UnsupportedOperationException.class, streams::clear);

        FFmpegChapter chapter = new FFmpegChapter();
        result.chapters = new ArrayList<>(List.of(chapter));
        List<FFmpegChapter> chapters = result.getChapters();
        result.chapters.clear();
        assertEquals(List.of(chapter), chapters);
        assertThrows(UnsupportedOperationException.class, chapters::clear);
    }
}
