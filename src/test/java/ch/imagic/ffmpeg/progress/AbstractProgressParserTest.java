package ch.imagic.ffmpeg.progress;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
public abstract class AbstractProgressParserTest {

    final List<Progress> progesses = Collections.synchronizedList(new ArrayList<Progress>());

    ProgressParser parser;
    URI uri;

    final ProgressListener listener = new ProgressListener() {
        @Override
        public void progress(Progress p) {
            progesses.add(p);
        }
    };

    @BeforeEach
    public void setupParser() throws IOException, URISyntaxException {
        synchronized (progesses) {
            progesses.clear();
        }

        parser = newParser(listener);
        uri = parser.getUri();
    }

    public abstract ProgressParser newParser(ProgressListener listener) throws IOException, URISyntaxException;

    @Test
    public void testNoConnection() throws IOException, InterruptedException {
        parser.start();
        parser.stop();
        assertTrue(progesses.isEmpty());
    }

    @Test
    public void testDoubleStop() throws IOException, InterruptedException {
        parser.start();
        parser.stop();
        parser.stop();
        assertTrue(progesses.isEmpty());
    }

    @Test
    public void testDoubleStart() throws IOException {
        parser.start();
        assertThrows(IllegalThreadStateException.class, parser::start);
        assertTrue(progesses.isEmpty());
    }

    @Test
    public void testStopNoStart() throws IOException {
        parser.stop();
        assertTrue(progesses.isEmpty());
    }
}
