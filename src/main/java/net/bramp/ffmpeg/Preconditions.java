package net.bramp.ffmpeg;

import java.net.URI;
import java.util.List;
import java.util.Objects;

public final class Preconditions {

    private static final List<String> rtps = List.of("rtsp", "rtp", "rtmp");
    private static final List<String> udpTcp = List.of("udp", "tcp");

    Preconditions() {
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
        boolean empty = arg == null || arg.chars().allMatch(Preconditions::isWhitespace);
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

        if (rtps.contains(scheme)) {
            return uri;
        }

        if (udpTcp.contains(scheme)) {
            if (uri.getPort() == -1) {
                throw new IllegalArgumentException("must set port when using udp or tcp scheme");
            }
            return uri;
        }

        throw new IllegalArgumentException("not a valid output URL, must use rtp/tcp/udp scheme");
    }
}
