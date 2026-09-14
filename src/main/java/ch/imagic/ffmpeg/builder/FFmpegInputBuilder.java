package ch.imagic.ffmpeg.builder;

import static ch.imagic.ffmpeg.FFmpegUtils.checkNotEmpty;

import ch.imagic.ffmpeg.FFmpegUtils;
import ch.imagic.ffmpeg.probe.FFmpegProbeResult;
import ch.imagic.ffmpeg.probe.Fraction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class FFmpegInputBuilder {

    protected final FFmpegBuilder parent;
    protected final String inputName;
    protected FFmpegProbeResult inputProbe;
    protected String format;
    protected String inputPixelFormat;
    protected Fraction inputFrameRate;
    protected Long startOffset;
    protected Long inputDuration;
    protected boolean readAtNativeFrameRate;
    protected Boolean safe;
    protected Strict strict = Strict.NORMAL;
    protected Integer videoWidth;
    protected Integer videoHeight;
    protected String userAgent;
    protected final List<String> extraArgs = new ArrayList<>();

    protected FFmpegInputBuilder(FFmpegBuilder parent, String inputName) {
        this.parent = Objects.requireNonNull(parent);
        this.inputName = Objects.requireNonNull(inputName);
    }

    public String getInputName() {
        return inputName;
    }

    public FFmpegProbeResult getInputProbe() {
        return inputProbe;
    }

    public FFmpegInputBuilder setInputProbe(FFmpegProbeResult inputProbe) {
        this.inputProbe = inputProbe;
        return this;
    }

    public String getFormat() {
        return format;
    }

    public FFmpegInputBuilder setFormat(String format) {
        this.format = Objects.requireNonNull(format);
        return this;
    }

    public String getInputPixelFormat() {
        return inputPixelFormat;
    }

    public FFmpegInputBuilder setInputPixelFormat(String pixelFormat) {
        this.inputPixelFormat = checkNotEmpty(pixelFormat, "pixel format must not be empty");
        return this;
    }

    public Fraction getInputFrameRate() {
        return inputFrameRate;
    }

    public FFmpegInputBuilder setInputFrameRate(double inputFrameRate) {
        return setInputFrameRate(Fraction.getFraction(inputFrameRate));
    }

    public FFmpegInputBuilder setInputFrameRate(Fraction inputFrameRate) {
        if (inputFrameRate == null || inputFrameRate.asDouble() <= 0) {
            throw new IllegalArgumentException("input frame rate must be positive");
        }
        this.inputFrameRate = inputFrameRate;
        return this;
    }

    public Long getStartOffset() {
        return startOffset;
    }

    public FFmpegInputBuilder setStartOffset(long duration, TimeUnit units) {
        this.startOffset = Objects.requireNonNull(units).toMillis(duration);
        return this;
    }

    public Long getInputDuration() {
        return inputDuration;
    }

    /**
     * This function currently only offers millisecond precision. Sub-millisecond values are
     * truncated.
     */
    public FFmpegInputBuilder setInputDuration(long duration, TimeUnit unit) {
        if (duration < 0) {
            throw new IllegalArgumentException("duration must not be negative");
        }
        this.inputDuration = Objects.requireNonNull(unit).toMillis(duration);
        return this;
    }

    public boolean getReadAtNativeFrameRate() {
        return readAtNativeFrameRate;
    }

    public FFmpegInputBuilder readAtNativeFrameRate() {
        this.readAtNativeFrameRate = true;
        return this;
    }

    public Boolean getSafe() {
        return safe;
    }

    public FFmpegInputBuilder setSafe(boolean safe) {
        this.safe = safe;
        return this;
    }

    public Strict getStrict() {
        return strict;
    }

    public FFmpegInputBuilder setStrict(Strict strict) {
        this.strict = Objects.requireNonNull(strict);
        return this;
    }

    public Integer getVideoWidth() {
        return videoWidth;
    }

    public Integer getVideoHeight() {
        return videoHeight;
    }

    public FFmpegInputBuilder setVideoSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("video size must be positive");
        }
        this.videoWidth = width;
        this.videoHeight = height;
        return this;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public FFmpegInputBuilder setUserAgent(String userAgent) {
        this.userAgent = Objects.requireNonNull(userAgent);
        return this;
    }

    public List<String> getExtraArgs() {
        return extraArgs;
    }

    /**
     * Adds arguments that apply to this input. They are placed immediately before its {@code -i}
     * flag.
     *
     * @param values the additional arguments
     * @return this
     */
    public FFmpegInputBuilder addExtraArgs(String... values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("one or more values must be supplied");
        }
        checkNotEmpty(values[0], "first extra arg may not be empty");

        for (String value : values) {
            extraArgs.add(Objects.requireNonNull(value));
        }
        return this;
    }

    public FFmpegBuilder done() {
        return Objects.requireNonNull(parent);
    }

    protected List<String> build() {
        List<String> args = new ArrayList<>();

        if (userAgent != null) {
            args.addAll(List.of("-user_agent", userAgent));
        }
        if (strict != Strict.NORMAL) {
            args.addAll(List.of("-strict", strict.toString()));
        }
        if (startOffset != null) {
            args.addAll(List.of("-ss", FFmpegUtils.toTimecode(startOffset, TimeUnit.MILLISECONDS)));
        }
        if (format != null) {
            args.addAll(List.of("-f", format));
        }
        if (inputFrameRate != null) {
            args.addAll(List.of("-r", inputFrameRate.toString()));
        }
        if (inputPixelFormat != null) {
            args.addAll(List.of("-pix_fmt", inputPixelFormat));
        }
        if (readAtNativeFrameRate) {
            args.add("-re");
        }
        if (safe != null) {
            args.addAll(List.of("-safe", safe ? "1" : "0"));
        }
        if (inputDuration != null) {
            args.addAll(List.of("-t", FFmpegUtils.toTimecode(inputDuration, TimeUnit.MILLISECONDS)));
        }
        if (videoHeight != null && videoWidth != null) {
            args.addAll(List.of("-video_size", videoWidth + "x" + videoHeight));
        }

        args.addAll(extraArgs);
        args.addAll(List.of("-i", inputName));
        return args;
    }
}
