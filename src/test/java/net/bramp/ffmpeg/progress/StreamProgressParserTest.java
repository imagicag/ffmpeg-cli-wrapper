package net.bramp.ffmpeg.progress;

import static net.bramp.ffmpeg.Helper.combineResource;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import net.bramp.ffmpeg.fixtures.Progresses;
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
