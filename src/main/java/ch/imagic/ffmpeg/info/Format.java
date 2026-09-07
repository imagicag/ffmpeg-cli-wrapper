package ch.imagic.ffmpeg.info;

import java.util.Objects;

/**
 * Information about supported Format
 *
 */
public class Format {
    final String name;
    final String longName;

    final boolean canDemux;
    final boolean canMux;

    /**
     * @param name short format name
     * @param longName long format name
     * @param flags is expected to be in the following format:
     *     <pre>
     * D. = Demuxing supported
     * .E = Muxing supported
     * </pre>
     */
    public Format(String name, String longName, String flags) {
        this.name = Objects.requireNonNull(name).trim();
        this.longName = Objects.requireNonNull(longName).trim();

        Objects.requireNonNull(flags);
        if (flags.length() != 2) {
            throw new IllegalArgumentException("Format flags is invalid '" + flags + "'");
        }
        canDemux = flags.charAt(0) == 'D';
        canMux = flags.charAt(1) == 'E';
    }

    @Override
    public String toString() {
        return name + " " + longName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Format)) {
            return false;
        }
        Format other = (Format) obj;
        return canDemux == other.canDemux
                && canMux == other.canMux
                && name.equals(other.name)
                && longName.equals(other.longName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, longName, canDemux, canMux);
    }

    public String getName() {
        return name;
    }

    public String getLongName() {
        return longName;
    }

    public boolean getCanDemux() {
        return canDemux;
    }

    public boolean getCanMux() {
        return canMux;
    }
}
