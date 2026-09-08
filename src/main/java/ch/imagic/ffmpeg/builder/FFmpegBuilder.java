package ch.imagic.ffmpeg.builder;

import static ch.imagic.ffmpeg.FFmpegUtils.checkNotEmpty;

import ch.imagic.ffmpeg.FFmpegUtils;
import ch.imagic.ffmpeg.probe.FFmpegProbeResult;
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

    public enum Strict {
        VERY, // strictly conform to a older more strict version of the specifications or reference
        // software
        STRICT, // strictly conform to all the things in the specificiations no matter what consequences
        NORMAL, // normal
        UNOFFICIAL, // allow unofficial extensions
        EXPERIMENTAL;

        // ffmpeg command line requires these options in lower case
        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    /** Log level options: https://ffmpeg.org/ffmpeg.html#Generic-options */
    public enum Verbosity {
        QUIET,
        PANIC,
        FATAL,
        ERROR,
        WARNING,
        INFO,
        VERBOSE,
        DEBUG;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    // Global Settings
    boolean override = true;
    int pass = 0;
    String passDirectory = "";
    String passPrefix;
    Verbosity verbosity = Verbosity.ERROR;
    URI progress;
    String userAgent;

    // Input settings
    String format;
    Long startOffset; // in millis
    boolean readAtNativeFrameRate = false;
    final List<String> inputs = new ArrayList<>();
    final Map<String, FFmpegProbeResult> inputProbes = new TreeMap<>();

    final List<String> extraArgs = new ArrayList<>();

    // Output
    final List<FFmpegOutputBuilder> outputs = new ArrayList<>();

    // Filters
    String audioFilter;
    String videoFilter;
    String complexFilter;

    public FFmpegBuilder overrideOutputFiles(boolean override) {
        this.override = override;
        return this;
    }

    public boolean getOverrideOutputFiles() {
        return this.override;
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

    public FFmpegBuilder readAtNativeFrameRate() {
        this.readAtNativeFrameRate = true;
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
     * Add additional ouput arguments (for flags which aren't currently supported).
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

    /**
     * Adds new output file.
     *
     * @param filename output file path
     * @return A new {@link FFmpegOutputBuilder}
     */
    public FFmpegOutputBuilder addOutput(String filename) {
        FFmpegOutputBuilder output = new FFmpegOutputBuilder(this, filename);
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
        if (outputs.isEmpty()) {
            throw new IllegalArgumentException("At least one output must be specified");
        }

        args.add(override ? "-y" : "-n");
        args.addAll(List.of("-v", this.verbosity.toString()));

        if (userAgent != null) {
            args.addAll(List.of("-user_agent", userAgent));
        }

        if (startOffset != null) {
            args.addAll(List.of("-ss", FFmpegUtils.toTimecode(startOffset, TimeUnit.MILLISECONDS)));
        }

        if (format != null) {
            args.addAll(List.of("-f", format));
        }

        if (readAtNativeFrameRate) {
            args.add("-re");
        }

        if (progress != null) {
            args.addAll(List.of("-progress", progress.toString()));
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
