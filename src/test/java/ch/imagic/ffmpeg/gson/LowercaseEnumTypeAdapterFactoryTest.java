package ch.imagic.ffmpeg.gson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.util.Locale;
import org.junit.jupiter.api.Test;

public class LowercaseEnumTypeAdapterFactoryTest {
    private final LowercaseEnumTypeAdapterFactory factory = new LowercaseEnumTypeAdapterFactory();
    private final Gson gson =
            new GsonBuilder().registerTypeAdapterFactory(factory).create();

    private enum Value {
        FIRST_VALUE,
        CAPITAL_I
    }

    @Test
    public void writesEnumNamesInLowercaseAndReadsThemBack() {
        assertEquals("\"first_value\"", gson.toJson(Value.FIRST_VALUE));
        assertEquals(Value.FIRST_VALUE, gson.fromJson("\"first_value\"", Value.class));
    }

    @Test
    public void handlesJsonNullAndUnknownValues() {
        assertEquals("null", gson.toJson(null, Value.class));
        assertNull(gson.fromJson("null", Value.class));
        assertNull(gson.fromJson("\"missing\"", Value.class));
    }

    @Test
    public void matchingIsCaseSensitive() {
        assertNull(gson.fromJson("\"FIRST_VALUE\"", Value.class));
    }

    @Test
    public void lowercasingIsIndependentOfDefaultLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("\"capital_i\"", gson.toJson(Value.CAPITAL_I));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void ignoresNonEnumTypes() {
        assertNull(factory.create(gson, TypeToken.get(String.class)));
    }
}
