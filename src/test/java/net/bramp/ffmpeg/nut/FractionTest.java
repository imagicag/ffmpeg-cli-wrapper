package net.bramp.ffmpeg.nut;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class FractionTest {
    @Test
    public void normalizesFractions() {
        assertEquals(Fraction.getFraction(1, 2), Fraction.getFraction(2, 4));
        assertEquals(Fraction.getFraction(-1, 2), Fraction.getFraction(1, -2));
        assertEquals(Fraction.ZERO, Fraction.getFraction(0, 7));
    }

    @Test
    public void parsesSupportedFormats() {
        assertEquals(Fraction.getFraction(1, 2), Fraction.getFraction("1/2"));
        assertEquals(Fraction.getFraction(3, 2), Fraction.getFraction("1 1/2"));
        assertEquals(Fraction.getFraction(-3, 2), Fraction.getFraction("-1 1/2"));
        assertEquals(Fraction.getFraction(23, 10), Fraction.getFraction("2.3"));
        assertEquals(Fraction.getFraction(2, 1), Fraction.getFraction("2"));
    }

    @Test
    public void convertsDoublesToFractions() {
        assertEquals(Fraction.getFraction(2997, 125), Fraction.getFraction(23.976));
        assertEquals(Fraction.getFraction(-1, 2), Fraction.getFraction(-0.5));
    }

    @Test
    public void rendersFractions() {
        assertEquals("1/2", Fraction.getFraction(1, 2).toString());
        assertEquals("1 1/2", Fraction.getFraction(3, 2).toProperString());
        assertEquals("-1 1/2", Fraction.getFraction(-3, 2).toProperString());
        assertEquals("2", Fraction.getFraction(2, 1).toProperString());
        assertEquals("0", Fraction.ZERO.toProperString());
    }

    @Test
    public void convertsToNumberTypes() {
        Fraction fraction = Fraction.getFraction(3, 2);
        assertEquals(1, fraction.intValue());
        assertEquals(1L, fraction.longValue());
        assertEquals(1.5f, fraction.floatValue(), 0);
        assertEquals(1.5, fraction.doubleValue(), 0);
        assertEquals(1.5, fraction.asDouble(), 0);
    }

    @Test
    public void comparesExactly() {
        Fraction larger = Fraction.getFraction(Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
        Fraction smaller = Fraction.getFraction(Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1);
        assertEquals(1, larger.compareTo(smaller));
        assertEquals(-1, smaller.compareTo(larger));
        assertEquals(0, Fraction.getFraction(1, 2).compareTo(Fraction.getFraction(2, 4)));
    }

    @Test
    public void rejectsInvalidFractions() {
        assertThrows(ArithmeticException.class, () -> Fraction.getFraction(1, 0));
        assertThrows(ArithmeticException.class, () -> Fraction.getFraction(1, -1, 2));
        assertThrows(ArithmeticException.class, () -> Fraction.getFraction(1, 1, -2));
        assertThrows(ArithmeticException.class, () -> Fraction.getFraction(Integer.MIN_VALUE, 1, 2));
        assertThrows(ArithmeticException.class, () -> Fraction.getFraction(Double.NaN));
        assertThrows(NumberFormatException.class, () -> Fraction.getFraction("1 2"));
    }
}
