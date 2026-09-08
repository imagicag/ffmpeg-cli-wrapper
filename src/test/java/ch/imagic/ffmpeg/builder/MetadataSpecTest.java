package ch.imagic.ffmpeg.builder;

import static ch.imagic.ffmpeg.builder.MetadataSpecifier.*;
import static ch.imagic.ffmpeg.builder.StreamSpecifier.id;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MetadataSpecTest {

    @Test
    public void testMetaSpec() {
        assertEquals("g", global().spec());
        assertEquals("c:1", chapter(1).spec());
        assertEquals("p:1", program(1).spec());
        assertEquals("s:1", stream(1).spec());
        assertEquals("s:i:1", stream(id(1)).spec());
    }
}
