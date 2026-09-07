package net.bramp.ffmpeg;

import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Random test helper methods. */
public class Helper {

    /**
     * Simple wrapper around "new SequenceInputStream", so the user doesn't have to deal with the
     * horribly dated Enumeration type.
     *
     * @param input
     * @return
     */
    public static InputStream sequenceInputStream(Iterable<InputStream> input) {
        Objects.requireNonNull(input);
        List<InputStream> streams = new ArrayList<>();
        for (InputStream stream : input) {
            streams.add(stream);
        }
        return new SequenceInputStream(Collections.enumeration(streams));
    }

    public static InputStream loadResource(String name) {
        Objects.requireNonNull(name);
        return FFmpegTest.class.getResourceAsStream("fixtures/" + name);
    }

    /**
     * Loads all resources, and returns one stream containing them all.
     *
     * @param names
     * @return
     */
    public static InputStream combineResource(List<String> names) {
        Objects.requireNonNull(names);
        List<InputStream> streams = new ArrayList<>();
        for (String name : names) {
            streams.add(loadResource(name));
        }
        return sequenceInputStream(streams);
    }
}
