package ch.imagic.ffmpeg.info;

import java.util.Objects;

/**
 * Information about supported Codecs
 *
 */
public class Codec {

    public enum Type {
        VIDEO,
        AUDIO,
        SUBTITLE,
        DATA
    }

    final String name;
    final String longName;

    /** Can I decode with this codec */
    final boolean canDecode;

    /** Can I encode with this codec */
    final boolean canEncode;

    /** What type of codec is this */
    final Type type;

    /**
     * @param name short codec name
     * @param longName long codec name
     * @param flags is expected to be in the following format:
     *     <pre>
     * D..... = Decoding supported
     * .E.... = Encoding supported
     * ..V... = Video codec
     * ..A... = Audio codec
     * ..S... = Subtitle codec
     * ...I.. = Intra frame-only codec
     * ....L. = Lossy compression
     * .....S = Lossless compression
     * </pre>
     */
    public Codec(String name, String longName, String flags) {
        this.name = Objects.requireNonNull(name).trim();
        this.longName = Objects.requireNonNull(longName).trim();

        Objects.requireNonNull(flags);
        if (flags.length() != 6) {
            throw new IllegalArgumentException("Format flags is invalid '" + flags + "'");
        }
        this.canDecode = flags.charAt(0) == 'D';
        this.canEncode = flags.charAt(1) == 'E';

        switch (flags.charAt(2)) {
            case 'V':
                this.type = Type.VIDEO;
                break;
            case 'A':
                this.type = Type.AUDIO;
                break;
            case 'S':
                this.type = Type.SUBTITLE;
                break;
            case 'D':
                this.type = Type.DATA;
                break;
            default:
                throw new IllegalArgumentException("Invalid codec type '" + flags.charAt(2) + "'");
        }

        // TODO There are more flags to parse
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
        if (!(obj instanceof Codec)) {
            return false;
        }
        Codec other = (Codec) obj;
        return canDecode == other.canDecode
                && canEncode == other.canEncode
                && name.equals(other.name)
                && longName.equals(other.longName)
                && type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, longName, canDecode, canEncode, type);
    }

    public String getName() {
        return name;
    }

    public String getLongName() {
        return longName;
    }

    public boolean getCanDecode() {
        return canDecode;
    }

    public boolean getCanEncode() {
        return canEncode;
    }

    public Type getType() {
        return type;
    }
}
