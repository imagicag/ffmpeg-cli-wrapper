package ch.imagic.ffmpeg.gson;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.imagic.ffmpeg.probe.Fraction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FractionAdapterTest {
    static Gson gson;

    @BeforeAll
    public static void setupGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Fraction.class, new FractionAdapter());
        gson = builder.create();
    }

    private static class TestData {
        final String s;
        final Fraction f;

        public TestData(String s, Fraction f) {
            this.s = s;
            this.f = f;
        }
    }

    static final List<TestData> readTests = List.of(
            new TestData("null", null),
            new TestData("1", Fraction.getFraction(1, 1)),
            new TestData("1.0", Fraction.getFraction(1, 1)),
            new TestData("2", Fraction.getFraction(2, 1)),
            new TestData("0.5", Fraction.getFraction(1, 2)),
            new TestData("\"1\"", Fraction.getFraction(1, 1)),
            new TestData("\"1.0\"", Fraction.getFraction(1, 1)),
            new TestData("\"2\"", Fraction.getFraction(2, 1)),
            new TestData("\"0.5\"", Fraction.getFraction(1, 2)),
            new TestData("\"1/2\"", Fraction.getFraction(1, 2)),
            new TestData("\"1 1/2\"", Fraction.getFraction(1, 1, 2)));

    // Divide by zero
    static final List<TestData> zerosTests =
            List.of(new TestData("\"0/0\"", Fraction.ZERO), new TestData("\"1/0\"", Fraction.ZERO));

    static final List<TestData> writeTests = List.of(
            new TestData("0", Fraction.ZERO),
            new TestData("1", Fraction.getFraction(1, 1)),
            new TestData("2", Fraction.getFraction(2, 1)),
            new TestData("1/2", Fraction.getFraction(1, 2)),
            new TestData("1 1/2", Fraction.getFraction(1, 1, 2)));

    @Test
    public void testRead() {
        for (TestData test : readTests) {
            Fraction f = gson.fromJson(test.s, Fraction.class);
            assertEquals(test.f, f);
        }
    }

    @Test
    public void testZerosRead() {
        for (TestData test : zerosTests) {
            Fraction f = gson.fromJson(test.s, Fraction.class);
            assertEquals(test.f, f);
        }
    }

    @Test
    public void testWrites() {
        for (TestData test : writeTests) {
            String json = gson.toJson(test.f);
            assertEquals('"' + test.s + '"', json);
        }
    }

    @Test
    public void testWriteNull() {
        String json = gson.toJson(null);
        assertEquals("null", json);
    }
}
