package ch.imagic.ffmpeg;

import static java.util.concurrent.TimeUnit.*;

import ch.imagic.ffmpeg.gson.FFmpegDispositionAdapter;
import ch.imagic.ffmpeg.gson.FractionAdapter;
import ch.imagic.ffmpeg.gson.LowercaseEnumTypeAdapterFactory;
import ch.imagic.ffmpeg.probe.FFmpegDisposition;
import ch.imagic.ffmpeg.probe.Fraction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** Helper class with commonly used methods */
public final class FFmpegUtils {

    private static final BigInteger NANOSECONDS_PER_SECOND = BigInteger.valueOf(1_000_000_000);
    private static final BigInteger SECONDS_PER_MINUTE = BigInteger.valueOf(60);
    private static final BigInteger MINUTES_PER_HOUR = BigInteger.valueOf(60);
    private static final String BITS_PER_SECOND_SUFFIX = "kbits/s";
    private static final BigDecimal BITS_PER_KILOBIT = BigDecimal.valueOf(1_000);
    private static final List<String> RTP_SCHEMES = List.of("rtsp", "rtp", "rtmp");
    private static final List<String> PORT_REQUIRED_SCHEMES = List.of("udp", "tcp");

    static final Gson gson = FFmpegUtils.setupGson();

    FFmpegUtils() {
        throw new AssertionError("No instances for you!");
    }

    /**
     * Ensures the argument is not null, empty string, or just whitespace.
     *
     * @param arg The argument
     * @param errorMessage The exception message to use if the check fails
     * @return The passed in argument if it is not blank
     */
    public static String checkNotEmpty(String arg, Object errorMessage) {
        boolean empty = arg == null || arg.chars().allMatch(FFmpegUtils::isWhitespace);
        if (empty) {
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        }
        return arg;
    }

    private static boolean isWhitespace(int character) {
        return Character.isWhitespace(character)
                || Character.isSpaceChar(character)
                || character == 0x85; // Unicode NEXT LINE
    }

    /**
     * Checks if the URI is valid for streaming to.
     *
     * @param uri The URI to check
     * @return The passed in URI if it is valid
     * @throws IllegalArgumentException if the URI is not valid.
     */
    public static URI checkValidStream(URI uri) throws IllegalArgumentException {
        String scheme = Objects.requireNonNull(uri).getScheme();
        scheme = Objects.requireNonNull(scheme, "URI is missing a scheme").toLowerCase();

        if (RTP_SCHEMES.contains(scheme)) {
            return uri;
        }

        if (PORT_REQUIRED_SCHEMES.contains(scheme)) {
            if (uri.getPort() == -1) {
                throw new IllegalArgumentException("must set port when using udp or tcp scheme");
            }
            return uri;
        }

        throw new IllegalArgumentException("not a valid output URL, must use rtp/tcp/udp scheme");
    }

    /**
     * Convert the duration to "hh:mm:ss" timecode representation, where ss (seconds) can be decimal.
     *
     * @param duration the duration.
     * @param units the unit the duration is in.
     * @return the timecode representation.
     */
    public static String toTimecode(long duration, TimeUnit units) {
        Objects.requireNonNull(units);

        BigInteger totalNanoseconds = BigInteger.valueOf(duration).multiply(BigInteger.valueOf(units.toNanos(1)));

        boolean negative = totalNanoseconds.signum() < 0;

        BigInteger[] secondsAndNanoseconds = totalNanoseconds.abs().divideAndRemainder(NANOSECONDS_PER_SECOND);

        BigInteger[] minutesAndSeconds = secondsAndNanoseconds[0].divideAndRemainder(SECONDS_PER_MINUTE);

        BigInteger[] hoursAndMinutes = minutesAndSeconds[0].divideAndRemainder(MINUTES_PER_HOUR);

        BigInteger hours = hoursAndMinutes[0];
        long minutes = hoursAndMinutes[1].longValueExact();
        long seconds = minutesAndSeconds[1].longValueExact();
        long nanoseconds = secondsAndNanoseconds[1].longValueExact();

        String time;
        if (nanoseconds == 0) {
            time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            time = String.format("%02d:%02d:%02d.%09d", hours, minutes, seconds, nanoseconds)
                    .replaceFirst("0+$", "");
        }

        if (negative) {
            time = "-" + time;
        }

        return time;
    }

    /**
     * Returns the number of nanoseconds this timecode represents. The string is expected to be in the
     * format "[-]hour:minute:second", where second can be a decimal number.
     *
     * @param time the timecode to parse.
     * @return the number of nanoseconds.
     */
    public static long fromTimecode(String time) {
        checkNotEmpty(time, "time must not be empty string");
        if (time.equalsIgnoreCase("N/A")) {
            return 0;
        }

        boolean negative = time.charAt(0) == '-';
        String unsignedTime = negative ? time.substring(1) : time;
        String[] fields = unsignedTime.split(":", -1);
        if (fields.length != 3) {
            throw new IllegalArgumentException("invalid time '" + time + "'");
        }

        long hours = parseTimeField(fields[0], time);
        long minutes = parseTimeField(fields[1], time);
        if (minutes >= 60) {
            throw new IllegalArgumentException("invalid time '" + time + "'");
        }

        String[] secondFields = fields[2].split("\\.", -1);
        if (secondFields.length > 2) {
            throw new IllegalArgumentException("invalid time '" + time + "'");
        }
        long seconds = parseTimeField(secondFields[0], time);
        if (seconds >= 60) {
            throw new IllegalArgumentException("invalid time '" + time + "'");
        }

        long nanoseconds = 0;
        if (secondFields.length == 2) {
            String fraction = secondFields[1];
            parseTimeField(fraction, time);
            String nanos =
                    fraction.length() > 9 ? fraction.substring(0, 9) : fraction + "0".repeat(9 - fraction.length());
            nanoseconds = Long.parseLong(nanos);
        }

        try {
            long result = Math.addExact(
                    Math.addExact(
                            Math.multiplyExact(hours, HOURS.toNanos(1)),
                            Math.multiplyExact(minutes, MINUTES.toNanos(1))),
                    Math.addExact(Math.multiplyExact(seconds, SECONDS.toNanos(1)), nanoseconds));
            return negative ? Math.negateExact(result) : result;
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("invalid time '" + time + "'", e);
        }
    }

    private static long parseTimeField(String field, String time) {
        if (field.isEmpty()) {
            throw new IllegalArgumentException("invalid time '" + time + "'");
        }
        for (int i = 0; i < field.length(); i++) {
            if (!Character.isDigit(field.charAt(i))) {
                throw new IllegalArgumentException("invalid time '" + time + "'");
            }
        }
        try {
            return Long.parseLong(field);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid time '" + time + "'", e);
        }
    }

    /**
     * Converts a string representation of bitrate to a long of bits per second
     *
     * @param bitrate in the form of 12.3kbits/s
     * @return the bitrate in bits per second or -1 if bitrate is 'N/A'
     */
    public static long parseBitrate(String bitrate) {
        if ("N/A".equals(bitrate)) {
            return -1;
        }

        if (bitrate == null || !bitrate.endsWith(BITS_PER_SECOND_SUFFIX)) {
            throw new IllegalArgumentException("Invalid bitrate '" + bitrate + "'");
        }

        String value = bitrate.substring(0, bitrate.length() - BITS_PER_SECOND_SUFFIX.length())
                .stripLeading();

        boolean decimalPointSeen = false;
        boolean digitSeen = false;

        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (Character.isDigit(character)) {
                digitSeen = true;
            } else if (character == '.' && !decimalPointSeen && digitSeen && i + 1 < value.length()) {
                decimalPointSeen = true;
            } else {
                throw new IllegalArgumentException("Invalid bitrate '" + bitrate + "'");
            }
        }
        if (!digitSeen) {
            throw new IllegalArgumentException("Invalid bitrate '" + bitrate + "'");
        }

        try {
            return new BigDecimal(value)
                    .multiply(BITS_PER_KILOBIT)
                    .toBigInteger()
                    .longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException("Invalid bitrate '" + bitrate + "'", e);
        }
    }

    static Gson getGson() {
        return gson;
    }

    private static Gson setupGson() {
        GsonBuilder builder = new GsonBuilder();

        builder.registerTypeAdapterFactory(new LowercaseEnumTypeAdapterFactory());
        builder.registerTypeAdapter(Fraction.class, new FractionAdapter());
        builder.registerTypeAdapter(FFmpegDisposition.class, new FFmpegDispositionAdapter());

        return builder.create();
    }
}
