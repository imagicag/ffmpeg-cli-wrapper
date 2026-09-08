package ch.imagic.ffmpeg.builder;

/** FFmpeg standards compliance levels. */
public enum Strict {
    VERY, // strictly conform to an older, stricter version of the specifications or reference software
    STRICT, // strictly conform to all specification requirements
    NORMAL,
    UNOFFICIAL, // allow unofficial extensions
    EXPERIMENTAL;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
