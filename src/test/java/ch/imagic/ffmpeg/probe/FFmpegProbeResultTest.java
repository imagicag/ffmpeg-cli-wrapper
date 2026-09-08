package ch.imagic.ffmpeg.probe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class FFmpegProbeResultTest {

    @Test
    public void collectionGettersReturnImmutableSnapshots() {
        FFmpegProbeResult result = new FFmpegProbeResult();

        FFmpegStream stream = new FFmpegStream();
        ArrayList<FFmpegStream> mutableStreams = new ArrayList<>(List.of(stream));
        result.setStreams(mutableStreams);
        List<FFmpegStream> streams = result.getStreams();
        mutableStreams.clear();
        assertEquals(List.of(stream), streams);
        assertThrows(UnsupportedOperationException.class, streams::clear);

        FFmpegChapter chapter = new FFmpegChapter();
        ArrayList<FFmpegChapter> mutableChapters = new ArrayList<>(List.of(chapter));
        result.setChapters(mutableChapters);
        List<FFmpegChapter> chapters = result.getChapters();
        mutableChapters.clear();
        assertEquals(List.of(chapter), chapters);
        assertThrows(UnsupportedOperationException.class, chapters::clear);
    }
}
