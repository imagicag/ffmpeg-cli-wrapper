package net.bramp.ffmpeg.nut;

import java.io.Serial;
import java.util.Objects;

/** An immutable fraction backed by a 32-bit numerator and denominator. */
public final class Fraction extends Number implements Comparable<Fraction> {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final Fraction ZERO = new Fraction(0, 1);

    private final int numerator;
    private final int denominator;

    private Fraction(int numerator, int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public static Fraction getFraction(int numerator, int denominator) {
        if (denominator == 0) {
            throw new ArithmeticException("The denominator must not be zero");
        }

        long normalizedNumerator = numerator;
        long normalizedDenominator = denominator;
        if (normalizedDenominator < 0) {
            normalizedNumerator = -normalizedNumerator;
            normalizedDenominator = -normalizedDenominator;
        }

        long divisor = greatestCommonDivisor(normalizedNumerator, normalizedDenominator);
        return createChecked(normalizedNumerator / divisor, normalizedDenominator / divisor);
    }

    public static Fraction getFraction(int whole, int numerator, int denominator) {
        if (denominator == 0) {
            throw new ArithmeticException("The denominator must not be zero");
        }
        if (denominator < 0) {
            throw new ArithmeticException("The denominator must not be negative");
        }
        if (numerator < 0) {
            throw new ArithmeticException("The numerator must not be negative");
        }

        long improperNumerator = Math.abs((long) whole) * denominator + numerator;
        if (whole < 0) {
            improperNumerator = -improperNumerator;
        }
        return getFraction(toIntExact(improperNumerator, "Numerator"), denominator);
    }

    public static Fraction getFraction(double value) {
        if (!Double.isFinite(value) || Math.abs(value) > Integer.MAX_VALUE) {
            throw new ArithmeticException("The value must be finite and fit in an integer fraction");
        }
        if (value == 0) {
            return ZERO;
        }

        boolean negative = value < 0;
        double target = Math.abs(value);
        long whole = (long) Math.floor(target);
        double remainder = target - whole;
        if (remainder == 0) {
            return getFraction(negative ? (int) -whole : (int) whole, 1);
        }

        long previousNumerator = 0;
        long numerator = 1;
        long previousDenominator = 1;
        long denominator = 0;
        double approximation = remainder;

        for (int i = 0; i < 25 && approximation != 0; i++) {
            long coefficient = (long) Math.floor(approximation);
            long nextNumerator = coefficient * numerator + previousNumerator;
            long nextDenominator = coefficient * denominator + previousDenominator;
            if (nextDenominator > 10_000 || nextNumerator > Integer.MAX_VALUE) {
                break;
            }

            previousNumerator = numerator;
            numerator = nextNumerator;
            previousDenominator = denominator;
            denominator = nextDenominator;

            double fractionalPart = approximation - coefficient;
            if (fractionalPart == 0) {
                break;
            }
            approximation = 1.0 / fractionalPart;
        }

        long improperNumerator = whole * denominator + numerator;
        if (negative) {
            improperNumerator = -improperNumerator;
        }
        return getFraction(toIntExact(improperNumerator, "Numerator"), (int) denominator);
    }

    public static Fraction getFraction(String value) {
        Objects.requireNonNull(value, "value");
        String fraction = value.trim();

        if (fraction.indexOf('.') >= 0) {
            return getFraction(Double.parseDouble(fraction));
        }

        int space = fraction.indexOf(' ');
        if (space > 0) {
            int whole = Integer.parseInt(fraction.substring(0, space));
            String remainder = fraction.substring(space + 1);
            int slash = remainder.indexOf('/');
            if (slash < 0) {
                throw new NumberFormatException("The fraction must have the format X Y/Z");
            }
            return getFraction(
                    whole,
                    Integer.parseInt(remainder.substring(0, slash)),
                    Integer.parseInt(remainder.substring(slash + 1)));
        }

        int slash = fraction.indexOf('/');
        if (slash < 0) {
            return getFraction(Integer.parseInt(fraction), 1);
        }
        return getFraction(
                Integer.parseInt(fraction.substring(0, slash)), Integer.parseInt(fraction.substring(slash + 1)));
    }

    private static Fraction createChecked(long numerator, long denominator) {
        if (numerator == 0) {
            return ZERO;
        }
        return new Fraction(toIntExact(numerator, "Numerator"), toIntExact(denominator, "Denominator"));
    }

    private static int toIntExact(long value, String name) {
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new ArithmeticException(name + " is too large to represent as an integer");
        }
        return (int) value;
    }

    private static long greatestCommonDivisor(long left, long right) {
        left = Math.abs(left);
        right = Math.abs(right);
        while (right != 0) {
            long remainder = left % right;
            left = right;
            right = remainder;
        }
        return left;
    }

    public int getNumerator() {
        return numerator;
    }

    public int getDenominator() {
        return denominator;
    }

    @Override
    public int intValue() {
        return numerator / denominator;
    }

    @Override
    public long longValue() {
        return (long) numerator / denominator;
    }

    @Override
    public float floatValue() {
        return (float) numerator / denominator;
    }

    @Override
    public double doubleValue() {
        return (double) numerator / denominator;
    }

    public double asDouble() {
        return doubleValue();
    }

    public String toProperString() {
        if (numerator == 0) {
            return "0";
        }
        if (Math.abs((long) numerator) < denominator) {
            return toString();
        }

        int whole = numerator / denominator;
        int remainder = (int) Math.abs((long) numerator % denominator);
        return remainder == 0 ? Integer.toString(whole) : whole + " " + remainder + "/" + denominator;
    }

    @Override
    public String toString() {
        return numerator + "/" + denominator;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Fraction)) {
            return false;
        }
        Fraction other = (Fraction) obj;
        return numerator == other.numerator && denominator == other.denominator;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numerator, denominator);
    }

    @Override
    public int compareTo(Fraction other) {
        Objects.requireNonNull(other, "other");
        long left = (long) numerator * other.denominator;
        long right = (long) other.numerator * denominator;
        return Long.compare(left, right);
    }
}
