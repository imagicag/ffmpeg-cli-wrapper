package net.bramp.ffmpeg.builder;

import static net.bramp.ffmpeg.builder.MetadataSpecifier.*;
import static net.bramp.ffmpeg.builder.StreamSpecifier.id;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
