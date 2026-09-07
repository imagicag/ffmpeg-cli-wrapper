package ch.imagic.ffmpeg.progress;

import static ch.imagic.ffmpeg.Helper.combineResource;
import static org.junit.Assert.assertEquals;

import ch.imagic.ffmpeg.fixtures.Progresses;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;

public class StreamProgressParserTest {

    RecordingProgressListener listener = new RecordingProgressListener();

    @Test
    public void testNormal() throws IOException {
        listener.reset();

        StreamProgressParser parser = new StreamProgressParser(listener);

        InputStream inputStream = combineResource(Progresses.allFiles);
        parser.processStream(inputStream);

        assertEquals((List<Progress>) Progresses.allProgresses, listener.progesses);
    }
}
