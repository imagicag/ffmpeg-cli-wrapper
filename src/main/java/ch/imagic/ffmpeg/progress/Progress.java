package ch.imagic.ffmpeg.progress;

import static ch.imagic.ffmpeg.FFmpegUtils.fromTimecode;

import ch.imagic.ffmpeg.FFmpegUtils;
import ch.imagic.ffmpeg.probe.Fraction;
import java.util.Objects;

// TODO Change to be immutable
public class Progress {

    public enum Status {
        CONTINUE("continue"),
        END("end");

        private final String status;

        Status(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return status;
        }

        /**
         * Returns the canonical status for this String or throws a IllegalArgumentException.
         *
         * @param status the status to convert to a Status enum.
         * @return the Status enum.
         * @throws IllegalArgumentException if the status is unknown.
         */
        public static Status of(String status) {
            for (Status s : Status.values()) {
                if (status.equalsIgnoreCase(s.status)) {
                    return s;
                }
            }

            throw new IllegalArgumentException("invalid progress status '" + status + "'");
        }
    }

    /** The frame number being processed */
    public long frame = 0;

    /** The current frames per second */
    public Fraction fps = Fraction.ZERO;

    /** Current bitrate */
    public long bitrate = 0;

    /** Output file size (in bytes) */
    public long totalSize = 0;

    /** Output time (in nanoseconds) */
    // TODO Change this to a java.time.Duration
    public long outTimeNs = 0;

    public long dupFrames = 0;

    /** Number of frames dropped */
    public long dropFrames = 0;

    /** Speed of transcoding. 1 means realtime, 2 means twice realtime. */
    public float speed = 0;

    /** Current status, can be one of "continue", or "end" */
    public Status status = null;

    public Progress() {
        // Nothing
    }

    public Progress(
            long frame,
            float fps,
            long bitrate,
            long totalSize,
            long outTimeNs,
            long dupFrames,
            long dropFrames,
            float speed,
            Status status) {
        this.frame = frame;
        this.fps = Fraction.getFraction(fps);
        this.bitrate = bitrate;
        this.totalSize = totalSize;
        this.outTimeNs = outTimeNs;
        this.dupFrames = dupFrames;
        this.dropFrames = dropFrames;
        this.speed = speed;
        this.status = status;
    }

    /**
     * Parses values from the line, into this object.
     *
     * <p>The value options are defined in ffmpeg.c's print_report function
     * https://github.com/FFmpeg/FFmpeg/blob/master/ffmpeg.c
     *
     * @param line A single line of output from ffmpeg
     * @return true if the record is finished
     */
    protected boolean parseLine(String line) {
        line = Objects.requireNonNull(line).trim();
        if (line.isEmpty()) {
            return false; // Skip empty lines
        }

        final String[] args = line.split("=", 2);
        if (args.length != 2) {
            // invalid argument, so skip
            return false;
        }

        final String key = Objects.requireNonNull(args[0]);
        final String value = Objects.requireNonNull(args[1]);

        switch (key) {
            case "frame":
                frame = Long.parseLong(value);
                return false;

            case "fps":
                fps = Fraction.getFraction(value);
                return false;

            case "bitrate":
                if (value.equals("N/A")) {
                    bitrate = -1;
                } else {
                    bitrate = FFmpegUtils.parseBitrate(value);
                }
                return false;

            case "total_size":
                if (value.equals("N/A")) {
                    totalSize = -1;
                } else {
                    totalSize = Long.parseLong(value);
                }
                return false;

            case "out_time_ms":
                // This is a duplicate of the "out_time" field, but expressed as a int instead of string.
                // Note this value is in microseconds, not milliseconds, and is based on AV_TIME_BASE which
                // could change.
                // outTimeNs = Long.parseLong(value) * 1000;
                return false;

            case "out_time_us":
                return false;

            case "out_time":
                outTimeNs = fromTimecode(value);
                return false;

            case "dup_frames":
                dupFrames = Long.parseLong(value);
                return false;

            case "drop_frames":
                dropFrames = Long.parseLong(value);
                return false;

            case "speed":
                if (value.equals("N/A")) {
                    speed = -1;
                } else {
                    speed = Float.parseFloat(value.replace("x", ""));
                }
                return false;

            case "progress":
                // TODO After "end" stream is closed
                status = Status.of(value);
                return true; // The status field is always last in the record

            default:
                if (key.startsWith("stream_")) {
                    // TODO handle stream_0_0_q=0.0:
                    // stream_%d_%d_q= file_index, index, quality
                    // stream_%d_%d_psnr_%c=%2.2f, file_index, index, type{Y, U, V}, quality // Enable with
                    // AV_CODEC_FLAG_PSNR
                    // stream_%d_%d_psnr_all
                } else {
                    // LOG.warn("skipping unhandled key: {} = {}", key, value);
                }

                return false; // Either way, not supported
        }
    }

    public boolean isEnd() {
        return status == Status.END;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Progress progress1 = (Progress) o;
        return frame == progress1.frame
                && bitrate == progress1.bitrate
                && totalSize == progress1.totalSize
                && outTimeNs == progress1.outTimeNs
                && dupFrames == progress1.dupFrames
                && dropFrames == progress1.dropFrames
                && Float.compare(progress1.speed, speed) == 0
                && Objects.equals(fps, progress1.fps)
                && Objects.equals(status, progress1.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(frame, fps, bitrate, totalSize, outTimeNs, dupFrames, dropFrames, speed, status);
    }

    @Override
    public String toString() {
        return "Progress{frame="
                + frame
                + ", fps="
                + fps
                + ", bitrate="
                + bitrate
                + ", totalSize="
                + totalSize
                + ", outTimeNs="
                + outTimeNs
                + ", dupFrames="
                + dupFrames
                + ", dropFrames="
                + dropFrames
                + ", speed="
                + speed
                + ", status="
                + status
                + '}';
    }
}
