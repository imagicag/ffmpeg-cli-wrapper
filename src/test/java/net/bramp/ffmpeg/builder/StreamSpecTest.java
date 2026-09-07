package net.bramp.ffmpeg.builder;

import static net.bramp.ffmpeg.builder.StreamSpecifier.*;
import static net.bramp.ffmpeg.builder.StreamSpecifierType.*;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StreamSpecTest {

    @Test
    public void testStreamSpec() {
        assertEquals("1", stream(1).spec());
        assertEquals("v", stream(Video).spec());

        assertEquals("v:1", stream(Video, 1).spec());
        assertEquals("V:1", stream(PureVideo, 1).spec());
        assertEquals("a:1", stream(Audio, 1).spec());
        assertEquals("s:1", stream(Subtitle, 1).spec());
        assertEquals("d:1", stream(Data, 1).spec());
        assertEquals("t:1", stream(Attachment, 1).spec());

        assertEquals("p:1", program(1).spec());
        assertEquals("p:1:2", program(1, 2).spec());

        assertEquals("i:1", id(1).spec());

        assertEquals("m:key", tag("key").spec());
        assertEquals("m:key:value", tag("key", "value").spec());
        assertEquals("u", usable().spec());
    }
}
