package ch.imagic.ffmpeg.builder;

import static ch.imagic.ffmpeg.FFmpegUtils.checkNotEmpty;

import ch.imagic.ffmpeg.FFmpegUtils;
import ch.imagic.ffmpeg.probe.FFmpegProbeResult;
import ch.imagic.ffmpeg.probe.Fraction;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Builds a ffmpeg command line
 *
 */
public class FFmpegBuilder {

    /** Log level options: https://ffmpeg.org/ffmpeg.html#Generic-options */
    public enum Verbosity {
        QUIET,
        PANIC,
        FATAL,
        ERROR,
        WARNING,
        INFO,
        VERBOSE,
        DEBUG,
        TRACE;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    // Global Settings
    protected boolean override = true;
    protected int pass = 0;
    protected String passDirectory = "";
    protected String passPrefix;
    protected Verbosity verbosity = Verbosity.ERROR;
    protected URI progress;
    protected String userAgent;
    protected boolean exitOnError;
    protected Boolean hideBanner;
    protected Boolean disableStdin;

    // Input settings
    protected String format;
    protected String inputPixelFormat;
    protected Fraction inputFrameRate;
    protected Long startOffset; // in millis
    protected Long inputDuration; // in millis.
    protected boolean readAtNativeFrameRate = false;
    protected Boolean safe;
    protected Strict strict = Strict.NORMAL;
    protected Integer videoWidth;
    protected Integer videoHeight;
    protected final List<String> inputs = new ArrayList<>();
    protected final Map<String, FFmpegProbeResult> inputProbes = new TreeMap<>();

    protected final List<String> extraArgs = new ArrayList<>();

    // Output
    protected boolean noOutput;
    protected final List<FFmpegOutputBuilder> outputs = new ArrayList<>();

    // Filters
    protected String audioFilter;
    protected String videoFilter;
    protected String complexFilter;

    public FFmpegBuilder overrideOutputFiles(boolean override) {
        this.override = override;
        return this;
    }

    public boolean getOverrideOutputFiles() {
        return this.override;
    }

    public int getPass() {
        return pass;
    }

    public String getPassDirectory() {
        return passDirectory;
    }

    public String getPassPrefix() {
        return passPrefix;
    }

    public Verbosity getVerbosity() {
        return verbosity;
    }

    public URI getProgress() {
        return progress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public boolean getExitOnError() {
        return exitOnError;
    }

    public String getFormat() {
        return format;
    }

    public Long getStartOffset() {
        return startOffset;
    }

    public boolean getReadAtNativeFrameRate() {
        return readAtNativeFrameRate;
    }

    public Boolean getSafe() {
        return safe;
    }

    public List<String> getInputs() {
        return inputs;
    }

    public Map<String, FFmpegProbeResult> getInputProbes() {
        return inputProbes;
    }

    public List<String> getExtraArgs() {
        return extraArgs;
    }

    public List<FFmpegOutputBuilder> getOutputs() {
        return outputs;
    }

    public boolean getNoOutput() {
        return noOutput;
    }

    public String getAudioFilter() {
        return audioFilter;
    }

    public String getVideoFilter() {
        return videoFilter;
    }

    public String getComplexFilter() {
        return complexFilter;
    }

    public FFmpegBuilder setPass(int pass) {
        this.pass = pass;
        return this;
    }

    public FFmpegBuilder setPassDirectory(String directory) {
        this.passDirectory = Objects.requireNonNull(directory);
        return this;
    }

    public FFmpegBuilder setPassPrefix(String prefix) {
        this.passPrefix = Objects.requireNonNull(prefix);
        return this;
    }

    public FFmpegBuilder setVerbosity(Verbosity verbosity) {
        Objects.requireNonNull(verbosity);
        this.verbosity = verbosity;
        return this;
    }

    public FFmpegBuilder setUserAgent(String userAgent) {
        this.userAgent = Objects.requireNonNull(userAgent);
        return this;
    }

    /**
     * Stops FFmpeg when it encounters a processing error that it could otherwise recover from.
     *
     * @param exitOnError whether to emit {@code -xerror}
     * @return this
     */
    public FFmpegBuilder setExitOnError(boolean exitOnError) {
        this.exitOnError = exitOnError;
        return this;
    }

    public FFmpegBuilder readAtNativeFrameRate() {
        this.readAtNativeFrameRate = true;
        return this;
    }

    /**
     * Enables or disables FFmpeg's input filename safety checks.
     *
     * @param safe whether input filenames must be considered safe
     * @return this
     */
    public FFmpegBuilder setSafe(boolean safe) {
        this.safe = safe;
        return this;
    }

    public FFmpegBuilder addInput(FFmpegProbeResult result) {
        Objects.requireNonNull(result);
        String filename = Objects.requireNonNull(result.getFormat()).getFilename();
        inputProbes.put(filename, result);
        return addInput(filename);
    }

    public FFmpegBuilder addInput(String filename) {
        Objects.requireNonNull(filename);
        inputs.add(filename);
        return this;
    }

    protected void clearInputs() {
        inputs.clear();
        inputProbes.clear();
    }

    public FFmpegBuilder setInput(FFmpegProbeResult result) {
        clearInputs();
        return addInput(result);
    }

    public FFmpegBuilder setInput(String filename) {
        clearInputs();
        return addInput(filename);
    }

    public FFmpegBuilder setFormat(String format) {
        this.format = Objects.requireNonNull(format);
        return this;
    }

    public FFmpegBuilder setStartOffset(long duration, TimeUnit units) {
        Objects.requireNonNull(units);

        this.startOffset = units.toMillis(duration);

        return this;
    }

    public FFmpegBuilder addProgress(URI uri) {
        this.progress = Objects.requireNonNull(uri);
        return this;
    }

    /**
     * Sets the complex filter flag.
     *
     * @param filter
     * @return
     */
    public FFmpegBuilder setComplexFilter(String filter) {
        this.complexFilter = checkNotEmpty(filter, "filter must not be empty");
        return this;
    }

    /**
     * Sets the audio filter flag.
     *
     * @param filter
     * @return
     */
    public FFmpegBuilder setAudioFilter(String filter) {
        this.audioFilter = checkNotEmpty(filter, "filter must not be empty");
        return this;
    }

    /**
     * Sets the video filter flag.
     *
     * @param filter
     * @return
     */
    public FFmpegBuilder setVideoFilter(String filter) {
        this.videoFilter = checkNotEmpty(filter, "filter must not be empty");
        return this;
    }

    /**
     * Add additional arguments (for flags which aren't currently supported).
     * These are placed before the -i flag (the input file/source).
     *
     * @param values The extra arguments.
     * @return this
     */
    public FFmpegBuilder addExtraArgs(String... values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("one or more values must be supplied");
        }
        checkNotEmpty(values[0], "first extra arg may not be empty");

        for (String value : values) {
            extraArgs.add(Objects.requireNonNull(value));
        }
        return this;
    }

    public FFmpegBuilder setStrict(Strict strict) {
        this.strict = Objects.requireNonNull(strict);
        return this;
    }

    public Strict getStrict() {
        return strict;
    }

    /**
     * This function currently only offers millisecond precision.
     * sub millisecond values are trunacted.
     */
    public FFmpegBuilder setInputDuration(long duration, TimeUnit unit) {
        if (duration < 0) {
            throw new IllegalArgumentException("duration must not be negative");
        }
        this.inputDuration = unit.toMillis(duration);
        return this;
    }

    public Long getInputDuration() {
        return inputDuration;
    }

    public FFmpegBuilder setVideoSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("video size must be positive");
        }
        this.videoWidth = width;
        this.videoHeight = height;
        return this;
    }

    public Integer getVideoWidth() {
        return videoWidth;
    }

    public Integer getVideoHeight() {
        return videoHeight;
    }

    public FFmpegBuilder setDisableStdin(Boolean disableStdin) {
        this.disableStdin = disableStdin;
        return this;
    }

    public Boolean getDisableStdin() {
        return disableStdin;
    }

    public FFmpegBuilder setHideBanner(Boolean hideBanner) {
        this.hideBanner = hideBanner;
        return this;
    }

    public Boolean getHideBanner() {
        return this.hideBanner;
    }

    public FFmpegBuilder setInputFrameRate(double inputFrameRate) {
        return this.setInputFrameRate(Fraction.getFraction(inputFrameRate));
    }

    public FFmpegBuilder setInputFrameRate(Fraction inputFrameRate) {
        if (inputFrameRate == null || inputFrameRate.asDouble() <= 0) {
            throw new IllegalArgumentException("input frame rate must be positive");
        }
        this.inputFrameRate = inputFrameRate;
        return this;
    }

    public Fraction getInputFrameRate() {
        return inputFrameRate;
    }

    public FFmpegBuilder setInputPixelFormat(String pixelFormat) {
        checkNotEmpty(pixelFormat, "pixel format must not be empty");
        this.inputPixelFormat = pixelFormat;
        return this;
    }

    public String getInputPixelFormat() {
        return inputPixelFormat;
    }

    /**
     * Allows building a command without an output destination.
     *
     * Use this function with care because it allows ffmpeg commands to exit with any exit code as
     * any ffmpeg command without an output will never exit with 0.
     *
     * This makes verification if the command ran successfully difficult.
     *
     * This is probably only useful if you are looking to use ffmpeg to parse stdout file headers.
     */
    public FFmpegBuilder noOutput() {
        this.noOutput = true;
        return this;
    }

    /**
     * Adds new output file.
     *
     * @param filename output file path
     * @return A new {@link FFmpegOutputBuilder}
     */
    public FFmpegOutputBuilder addOutput(String filename) {
        FFmpegOutputBuilder output = new FFmpegOutputBuilder(this, filename);
        noOutput = false;
        outputs.add(output);
        return output;
    }

    /**
     * Adds new output file.
     *
     * @param uri output file uri typically a stream
     * @return A new {@link FFmpegOutputBuilder}
     */
    public FFmpegOutputBuilder addOutput(URI uri) {
        FFmpegOutputBuilder output = new FFmpegOutputBuilder(this, uri);
        noOutput = false;
        outputs.add(output);
        return output;
    }

    /**
     * Adds an existing FFmpegOutputBuilder. This is similar to calling the other addOuput methods but
     * instead allows an existing FFmpegOutputBuilder to be used, and reused.
     *
     * <pre>
     * <code>List&lt;String&gt; args = new FFmpegBuilder()
     *   .addOutput(new FFmpegOutputBuilder()
     *     .setFilename(&quot;output.flv&quot;)
     *     .setVideoCodec(&quot;flv&quot;)
     *   )
     *   .build();</code>
     * </pre>
     *
     * @param output FFmpegOutputBuilder to add
     * @return this
     */
    public FFmpegBuilder addOutput(FFmpegOutputBuilder output) {
        noOutput = false;
        outputs.add(output);
        return this;
    }

    /**
     * Create new output (to stdout)
     *
     * @return A new {@link FFmpegOutputBuilder}
     */
    public FFmpegOutputBuilder addStdoutOutput() {
        return addOutput("-");
    }

    public List<String> build() {
        List<String> args = new ArrayList<>();

        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("At least one input must be specified");
        }
        if (outputs.isEmpty() && !noOutput) {
            throw new IllegalArgumentException("At least one output must be specified");
        }

        args.add(override ? "-y" : "-n");
        args.addAll(List.of("-v", this.verbosity.toString()));

        if (Boolean.TRUE.equals(hideBanner)) {
            args.add("-hide_banner");
        }

        if (userAgent != null) {
            args.addAll(List.of("-user_agent", userAgent));
        }

        if (strict != Strict.NORMAL) {
            args.addAll(List.of("-strict", strict.toString()));
        }

        if (exitOnError) {
            args.add("-xerror");
        }

        if (Boolean.TRUE.equals(disableStdin)) {
            args.add("-nostdin");
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

        if (progress != null) {
            args.addAll(List.of("-progress", progress.toString()));
        }

        if (inputDuration != null) {
            args.addAll(List.of("-t", FFmpegUtils.toTimecode(inputDuration, TimeUnit.MILLISECONDS)));
        }

        if (videoHeight != null && videoWidth != null) {
            args.addAll(List.of("-video_size", videoWidth + "x" + videoHeight));
        }

        args.addAll(extraArgs);

        for (String input : inputs) {
            args.addAll(List.of("-i", input));
        }

        if (pass > 0) {
            args.addAll(List.of("-pass", Integer.toString(pass)));

            if (passPrefix != null) {
                args.addAll(List.of("-passlogfile", passDirectory + passPrefix));
            }
        }

        if (audioFilter != null && !audioFilter.isEmpty()) {
            args.addAll(List.of("-af", audioFilter));
        }

        if (videoFilter != null && !videoFilter.isEmpty()) {
            args.addAll(List.of("-vf", videoFilter));
        }

        if (complexFilter != null && !complexFilter.isEmpty()) {
            args.addAll(List.of("-filter_complex", complexFilter));
        }

        for (FFmpegOutputBuilder output : this.outputs) {
            args.addAll(output.build(this, pass));
        }

        return List.copyOf(args);
    }
}
