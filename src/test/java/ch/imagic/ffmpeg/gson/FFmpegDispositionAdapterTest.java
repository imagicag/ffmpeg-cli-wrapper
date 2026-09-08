package ch.imagic.ffmpeg.gson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ch.imagic.ffmpeg.probe.FFmpegDisposition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.StringReader;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;

public class FFmpegDispositionAdapterTest {

    static Gson gson;

    @BeforeClass
    public static void setupGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(FFmpegDisposition.class, new FFmpegDispositionAdapter());
        gson = builder.create();
    }

    @Test
    public void readsNumericAndBooleanFlags() {
        FFmpegDisposition disposition = gson.fromJson(
                "{\"default\":1,\"dub\":0,\"forced\":true,\"hearing_impaired\":2}", FFmpegDisposition.class);

        assertTrue(disposition.isDefaultDisposition());
        assertFalse(disposition.isDub());
        assertTrue(disposition.isForced());
        assertTrue(disposition.isHearingImpaired());
    }

    @Test
    public void skipsUnknownAndUnsupportedProperties() {
        FFmpegDisposition disposition =
                gson.fromJson("{\"unknown\":1,\"dub\":\"ignored\",\"original\":true}", FFmpegDisposition.class);

        assertFalse(disposition.isDub());
        assertTrue(disposition.isOriginal());
    }

    @Test
    public void writesAllDispositionFlags() {
        FFmpegDisposition disposition = new FFmpegDisposition();
        disposition.setDefaultDisposition(true);
        disposition.setForced(true);

        JsonObject json = JsonParser.parseString(gson.toJson(disposition)).getAsJsonObject();

        assertEquals(14, json.size());
        assertTrue(json.get("default").getAsBoolean());
        assertTrue(json.get("forced").getAsBoolean());
        assertFalse(json.get("attached_pic").getAsBoolean());
    }

    @Test
    public void readsAndWritesNull() {
        assertNull(gson.fromJson("null", FFmpegDisposition.class));
        assertEquals("null", gson.toJson(null, FFmpegDisposition.class));
    }

    @Test
    public void readBooleanReturnsJavaOptional() throws Exception {
        FFmpegDispositionAdapter adapter = new FFmpegDispositionAdapter();

        assertEquals(Optional.of(true), adapter.readBoolean(new JsonReader(new StringReader("true"))));
        assertEquals(Optional.of(false), adapter.readBoolean(new JsonReader(new StringReader("0"))));
        assertEquals(Optional.of(true), adapter.readBoolean(new JsonReader(new StringReader("2"))));
        assertEquals(Optional.empty(), adapter.readBoolean(new JsonReader(new StringReader("\"ignored\""))));
    }
}
