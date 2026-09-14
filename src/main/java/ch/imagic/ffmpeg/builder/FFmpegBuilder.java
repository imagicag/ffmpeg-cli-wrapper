package ch.imagic.ffmpeg.builder;

import static ch.imagic.ffmpeg.FFmpegUtils.checkNotEmpty;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    protected boolean exitOnError;
    protected Boolean hideBanner;
    protected Boolean disableStdin;

    protected final List<String> extraArgs = new ArrayList<>();
    protected final List<FFmpegInputBuilder> inputs = new ArrayList<>();

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

    public boolean getExitOnError() {
        return exitOnError;
    }

    public List<String> getExtraArgs() {
        return extraArgs;
    }

    public List<FFmpegInputBuilder> getInputs() {
        return inputs;
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

    /**
     * Adds global arguments that are not otherwise supported. They are placed before all input
     * arguments.
     *
     * @param values the additional arguments
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

    public FFmpegInputBuilder addInput(String filename) {
        Objects.requireNonNull(filename);
        var builder = new FFmpegInputBuilder(this, filename);
        inputs.add(builder);
        return builder;
    }

    protected void clearInputs() {
        inputs.clear();
    }

    public FFmpegInputBuilder setInput(String filename) {
        clearInputs();
        return addInput(filename);
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

        if (exitOnError) {
            args.add("-xerror");
        }

        if (Boolean.TRUE.equals(disableStdin)) {
            args.add("-nostdin");
        }

        if (progress != null) {
            args.addAll(List.of("-progress", progress.toString()));
        }

        args.addAll(extraArgs);

        for (FFmpegInputBuilder input : inputs) {
            args.addAll(input.build());
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
