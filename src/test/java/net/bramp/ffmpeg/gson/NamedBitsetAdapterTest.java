package net.bramp.ffmpeg.gson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import java.io.StringReader;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;

public class NamedBitsetAdapterTest {

    static class Set {
        public boolean a;
        public boolean b;
        public int c;
        public int d;
    }

    static class SetWithSkipField extends Set {
        public String e; // Since this is not a boolean or int, it gets skipped
    }

    private static final Set testSet = testSet();
    private static final String testData = "{\"a\":true,\"b\":false,\"c\":true,\"d\":false}";

    private static final SetWithSkipField testSetWithSkip = testSetWithSkip();
    private static final String testDataWithSkipField =
            "{\"e\":\"skip\",\"a\":true,\"b\":false,\"c\":true,\"d\":false}";

    private static Set testSet() {
        Set s = new Set();
        {
            s.a = true;
            s.b = false;
            s.c = 1;
            s.d = 0;
            return s;
        }
    }

    private static SetWithSkipField testSetWithSkip() {
        SetWithSkipField s = new SetWithSkipField();
        {
            s.a = true;
            s.b = false;
            s.c = 1;
            s.d = 0;
            s.e = "skip";
            return s;
        }
    }

    static Gson gson;

    @BeforeClass
    public static void setupGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Set.class, new NamedBitsetAdapter<>(Set.class));
        builder.registerTypeAdapter(SetWithSkipField.class, new NamedBitsetAdapter<>(SetWithSkipField.class));
        gson = builder.create();
    }

    @Test
    public void testRead() throws Exception {
        Set s = gson.fromJson(testData, Set.class);
        assertEquals(testSet.a, s.a);
        assertEquals(testSet.b, s.b);
        assertEquals(testSet.c, s.c);
        assertEquals(testSet.d, s.d);
    }

    @Test
    public void testReadWithSkipField() throws Exception {
        Set s = gson.fromJson(testDataWithSkipField, Set.class);
        assertEquals(testSet.a, s.a);
        assertEquals(testSet.b, s.b);
        assertEquals(testSet.c, s.c);
        assertEquals(testSet.d, s.d);
    }

    @Test
    public void testWrite() throws Exception {
        // TODO This assumes that toJson will print the fields in particular order
        String json = gson.toJson(testSet);
        assertEquals(testData, json);
    }

    @Test
    public void testWriteWithSkipField() throws Exception {
        // TODO This assumes that toJson will print the fields in particular order
        String json = gson.toJson(testSetWithSkip);
        assertEquals(testData, json);
    }

    @Test
    public void testReadNull() throws Exception {
        Set s = gson.fromJson("null", Set.class);
        assertNull(s);
    }

    @Test
    public void testWriteNull() throws Exception {
        String json = gson.toJson(null);
        assertEquals("null", json);
    }

    @Test
    public void readBooleanReturnsJavaOptional() throws Exception {
        NamedBitsetAdapter<Set> adapter = new NamedBitsetAdapter<>(Set.class);

        assertEquals(Optional.of(true), adapter.readBoolean(new JsonReader(new StringReader("true"))));
        assertEquals(Optional.of(false), adapter.readBoolean(new JsonReader(new StringReader("0"))));
        assertEquals(Optional.of(true), adapter.readBoolean(new JsonReader(new StringReader("2"))));
        assertEquals(Optional.empty(), adapter.readBoolean(new JsonReader(new StringReader("\"ignored\""))));
    }
}
