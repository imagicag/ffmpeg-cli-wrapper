package ch.imagic.ffmpeg.builder;

import static ch.imagic.ffmpeg.FFmpegUtils.checkNotEmpty;
import static ch.imagic.ffmpeg.FFmpegUtils.checkValidStream;
import static ch.imagic.ffmpeg.FFmpegUtils.toTimecode;
import static ch.imagic.ffmpeg.builder.MetadataSpecifier.checkValidKey;

import ch.imagic.ffmpeg.options.AudioEncodingOptions;
import ch.imagic.ffmpeg.options.EncodingOptions;
import ch.imagic.ffmpeg.options.MainEncodingOptions;
import ch.imagic.ffmpeg.options.VideoEncodingOptions;
import ch.imagic.ffmpeg.probe.Fraction;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * This abstract class holds flags that are both applicable to input and output streams in the
 * ffmpeg command, while flags that apply to a particular direction (input/output) are located in
 * {@link FFmpegOutputBuilder}. <br>
 * <br>
 * All possible flags can be found in the <a href="https://ffmpeg.org/ffmpeg.html#Options">official
 * ffmpeg page</a> The discrimination criteria for flag location are the specifiers for each command
 *
 * <ul>
 *   <li>AbstractFFmpegStreamBuilder
 *       <ul>
 *         <li>(input/output): <code>-t duration (input/output)</code>
 *         <li>(input/output,per-stream): <code>
 *             -codec[:stream_specifier] codec (input/output,per-stream)</code>
 *         <li>(global): <code>-filter_threads nb_threads (global)</code>
 *       </ul>
 *   <li>FFmpegInputBuilder
 *       <ul>
 *         <li>(input): <code>-muxdelay seconds (input)</code>
 *         <li>(input,per-stream): <code>-guess_layout_max channels (input,per-stream)</code>
 *       </ul>
 *   <li>FFmpegOutputBuilder
 *       <ul>
 *         <li>(output): <code>-atag fourcc/tag (output)</code>
 *         <li>(output,per-stream): <code>
 *             -bsf[:stream_specifier] bitstream_filters (output,per-stream)</code>
 *       </ul>
 * </ul>
 *
 * @param <T> A concrete class that extends from the AbstractFFmpegStreamBuilder
 */
public abstract class AbstractFFmpegStreamBuilder<T extends AbstractFFmpegStreamBuilder<T>> {

    final FFmpegBuilder parent;

    /** Output filename or uri. Only one may be set */
    protected String filename;

    protected URI uri;

    protected String format;

    protected Long startOffset; // in milliseconds
    protected Long duration; // in milliseconds

    protected final List<String> metaTags = new ArrayList<>();

    protected boolean audioEnabled = true;
    protected String audioCodec;
    protected int audioChannels;
    protected int audioSampleRate;
    protected String audioPreset;

    protected boolean videoEnabled = true;
    protected String videoCodec;
    protected boolean videoCopyInkf;
    protected Fraction videoFrameRate;
    protected int videoWidth;
    protected int videoHeight;
    protected String videoSize;
    protected String videoMovFlags;
    protected Integer videoFrames;
    protected String videoPixelFormat;

    protected boolean subtitleEnabled = true;
    protected String subtitlePreset;
    private String subtitleCodec;

    protected String preset;
    protected String presetFilename;
    protected final List<String> extraArgs = new ArrayList<>();

    protected Strict strict = Strict.NORMAL;

    protected long targetSize = 0; // in bytes
    protected long passPaddingBitrate = 1024; // in bits per second

    protected AbstractFFmpegStreamBuilder() {
        this.parent = null;
    }

    protected AbstractFFmpegStreamBuilder(FFmpegBuilder parent, String filename) {
        this.parent = Objects.requireNonNull(parent);
        this.filename = checkNotEmpty(filename, "filename must not be empty");
    }

    protected AbstractFFmpegStreamBuilder(FFmpegBuilder parent, URI uri) {
        this.parent = Objects.requireNonNull(parent);
        this.uri = checkValidStream(uri);
    }

    protected abstract T getThis();

    public T useOptions(EncodingOptions opts) {
        Objects.requireNonNull(opts);
        useOptions(opts.getMain());

        if (opts.getAudio().enabled) {
            useOptions(opts.getAudio());
        }
        if (opts.getVideo().enabled) {
            useOptions(opts.getVideo());
        }

        return getThis();
    }

    public T useOptions(MainEncodingOptions opts) {
        Objects.requireNonNull(opts);
        if (opts.format != null) format = opts.format;
        if (opts.startOffset != null) startOffset = opts.startOffset;
        if (opts.duration != null) duration = opts.duration;
        return getThis();
    }

    public T useOptions(AudioEncodingOptions opts) {
        Objects.requireNonNull(opts);
        if (opts.enabled) audioEnabled = true;
        if (opts.codec != null) audioCodec = opts.codec;
        if (opts.channels != 0) audioChannels = opts.channels;
        if (opts.sampleRate != 0) audioSampleRate = opts.sampleRate;
        return getThis();
    }

    public T useOptions(VideoEncodingOptions opts) {
        Objects.requireNonNull(opts);
        if (opts.enabled) videoEnabled = true;
        if (opts.codec != null) videoCodec = opts.codec;
        if (opts.frameRate != null) videoFrameRate = opts.frameRate;
        if (opts.width != 0) videoWidth = opts.width;
        if (opts.height != 0) videoHeight = opts.height;
        if (opts.frames != null) videoFrames = opts.frames;
        return getThis();
    }

    public T disableVideo() {
        this.videoEnabled = false;
        return getThis();
    }

    public T disableAudio() {
        this.audioEnabled = false;
        return getThis();
    }

    public T disableSubtitle() {
        this.subtitleEnabled = false;
        return getThis();
    }

    /**
     * Sets a file to use containing presets.
     *
     * <p>Uses `-fpre`.
     *
     * @param presetFilename the preset by filename
     * @return this
     */
    public T setPresetFilename(String presetFilename) {
        this.presetFilename = checkNotEmpty(presetFilename, "file preset must not be empty");
        return getThis();
    }

    /**
     * Sets a preset by name (this only works with some codecs).
     *
     * <p>Uses `-preset`.
     *
     * @param preset the preset
     * @return this
     */
    public T setPreset(String preset) {
        this.preset = checkNotEmpty(preset, "preset must not be empty");
        return getThis();
    }

    public T setFilename(String filename) {
        this.filename = checkNotEmpty(filename, "filename must not be empty");
        return getThis();
    }

    public String getFilename() {
        return filename;
    }

    public T setUri(URI uri) {
        this.uri = checkValidStream(uri);
        return getThis();
    }

    public URI getUri() {
        return uri;
    }

    public String getFormat() {
        return format;
    }

    public Long getStartOffset() {
        return startOffset;
    }

    public Long getDuration() {
        return duration;
    }

    public List<String> getMetaTags() {
        return metaTags;
    }

    public boolean getAudioEnabled() {
        return audioEnabled;
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    public int getAudioChannels() {
        return audioChannels;
    }

    public int getAudioSampleRate() {
        return audioSampleRate;
    }

    public String getAudioPreset() {
        return audioPreset;
    }

    public boolean getVideoEnabled() {
        return videoEnabled;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public boolean getVideoCopyInkf() {
        return videoCopyInkf;
    }

    public Fraction getVideoFrameRate() {
        return videoFrameRate;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public String getVideoSize() {
        return videoSize;
    }

    public String getVideoMovFlags() {
        return videoMovFlags;
    }

    public Integer getVideoFrames() {
        return videoFrames;
    }

    public String getVideoPixelFormat() {
        return videoPixelFormat;
    }

    public boolean getSubtitleEnabled() {
        return subtitleEnabled;
    }

    public String getSubtitlePreset() {
        return subtitlePreset;
    }

    public String getPreset() {
        return preset;
    }

    public String getPresetFilename() {
        return presetFilename;
    }

    public List<String> getExtraArgs() {
        return extraArgs;
    }

    public Strict getStrict() {
        return strict;
    }

    public long getTargetSize() {
        return targetSize;
    }

    public long getPassPaddingBitrate() {
        return passPaddingBitrate;
    }

    public T setFormat(String format) {
        this.format = checkNotEmpty(format, "format must not be empty");
        return getThis();
    }

    public T setVideoCodec(String codec) {
        this.videoEnabled = true;
        this.videoCodec = checkNotEmpty(codec, "codec must not be empty");
        return getThis();
    }

    public T setVideoCopyInkf(boolean copyinkf) {
        this.videoEnabled = true;
        this.videoCopyInkf = copyinkf;
        return getThis();
    }

    public T setVideoMovFlags(String movflags) {
        this.videoEnabled = true;
        this.videoMovFlags = checkNotEmpty(movflags, "movflags must not be empty");
        return getThis();
    }

    /**
     * Sets the video's frame rate
     *
     * @param frameRate Frames per second
     * @return this
     * @see ch.imagic.ffmpeg.FFmpeg#FPS_30
     * @see ch.imagic.ffmpeg.FFmpeg#FPS_29_97
     * @see ch.imagic.ffmpeg.FFmpeg#FPS_24
     * @see ch.imagic.ffmpeg.FFmpeg#FPS_23_976
     */
    public T setVideoFrameRate(Fraction frameRate) {
        this.videoEnabled = true;
        this.videoFrameRate = Objects.requireNonNull(frameRate);
        return getThis();
    }

    /**
     * Set the video frame rate in terms of frames per interval. For example 24fps would be 24/1,
     * however NTSC TV at 23.976fps would be 24000 per 1001.
     *
     * @param frames The number of frames within the given seconds
     * @param per The number of seconds
     * @return this
     */
    public T setVideoFrameRate(int frames, int per) {
        return setVideoFrameRate(Fraction.getFraction(frames, per));
    }

    public T setVideoFrameRate(double frameRate) {
        return setVideoFrameRate(Fraction.getFraction(frameRate));
    }

    /**
     * Set the number of video frames to record.
     *
     * @param frames The number of frames
     * @return this
     */
    public T setFrames(int frames) {
        this.videoEnabled = true;
        this.videoFrames = frames;
        return getThis();
    }

    protected static boolean isValidSize(int widthOrHeight) {
        return widthOrHeight > 0 || widthOrHeight == -1;
    }

    public T setVideoWidth(int width) {
        requireArgument(isValidSize(width), "Width must be -1 or greater than zero");

        this.videoEnabled = true;
        this.videoWidth = width;
        return getThis();
    }

    public T setVideoHeight(int height) {
        requireArgument(isValidSize(height), "Height must be -1 or greater than zero");

        this.videoEnabled = true;
        this.videoHeight = height;
        return getThis();
    }

    public T setVideoResolution(int width, int height) {
        requireArgument(
                isValidSize(width) && isValidSize(height), "Both width and height must be -1 or greater than zero");

        this.videoEnabled = true;
        this.videoWidth = width;
        this.videoHeight = height;
        return getThis();
    }

    /**
     * Sets video resolution based on an abbreviation, e.g. "ntsc" for 720x480, or "vga" for 640x480
     *
     * @see <a href="https://www.ffmpeg.org/ffmpeg-utils.html#Video-size">ffmpeg video size</a>
     * @param abbreviation The abbreviation size. No validation is done, instead the value is passed
     *     as is to ffmpeg.
     * @return this
     */
    public T setVideoResolution(String abbreviation) {
        this.videoEnabled = true;
        this.videoSize = checkNotEmpty(abbreviation, "video abbreviation must not be empty");
        return getThis();
    }

    public T setVideoPixelFormat(String format) {
        this.videoEnabled = true;
        this.videoPixelFormat = checkNotEmpty(format, "format must not be empty");
        return getThis();
    }

    /**
     * Add metadata on output streams. Which keys are possible depends on the used codec.
     *
     * @param key Metadata key, e.g. "comment"
     * @param value Value to set for key
     * @return this
     */
    public T addMetaTag(String key, String value) {
        checkValidKey(key);
        checkNotEmpty(value, "value must not be empty");
        metaTags.add("-metadata");
        metaTags.add(key + "=" + value);
        return getThis();
    }

    /**
     * Add metadata on output streams. Which keys are possible depends on the used codec.
     *
     * <pre>{@code
     * import static ch.imagic.ffmpeg.builder.MetadataSpecifier.*;
     * import static ch.imagic.ffmpeg.builder.StreamSpecifier.*;
     * import static ch.imagic.ffmpeg.builder.StreamSpecifierType.*;
     *
     * new FFmpegBuilder()
     *   .addMetaTag("title", "Movie Title") // Annotate whole file
     *   .addMetaTag(chapter(0), "author", "Bob") // Annotate first chapter
     *   .addMetaTag(program(0), "comment", "Awesome") // Annotate first program
     *   .addMetaTag(stream(0), "copyright", "Megacorp") // Annotate first stream
     *   .addMetaTag(stream(Video), "framerate", "24fps") // Annotate all video streams
     *   .addMetaTag(stream(Video, 0), "artist", "Joe") // Annotate first video stream
     *   .addMetaTag(stream(Audio, 0), "language", "eng") // Annotate first audio stream
     *   .addMetaTag(stream(Subtitle, 0), "language", "fre") // Annotate first subtitle stream
     *   .addMetaTag(usable(), "year", "2010") // Annotate all streams with a usable configuration
     * }</pre>
     *
     * @param spec Metadata specifier, e.g `MetadataSpec.stream(Audio, 0)`
     * @param key Metadata key, e.g. "comment"
     * @param value Value to set for key
     * @return this
     */
    public T addMetaTag(MetadataSpecifier spec, String key, String value) {
        checkValidKey(key);
        checkNotEmpty(value, "value must not be empty");
        metaTags.add("-metadata:" + spec.spec());
        metaTags.add(key + "=" + value);
        return getThis();
    }

    public T setAudioCodec(String codec) {
        this.audioEnabled = true;
        this.audioCodec = checkNotEmpty(codec, "codec must not be empty");
        return getThis();
    }

    public T setSubtitleCodec(String codec) {
        this.subtitleEnabled = true;
        this.subtitleCodec = checkNotEmpty(codec, "codec must not be empty");
        return getThis();
    }

    /**
     * Sets the number of audio channels
     *
     * @param channels Number of channels
     * @return this
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_MONO
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_STEREO
     */
    public T setAudioChannels(int channels) {
        requireArgument(channels > 0, "channels must be positive");
        this.audioEnabled = true;
        this.audioChannels = channels;
        return getThis();
    }

    /**
     * Sets the Audio sample rate, for example 44_000.
     *
     * @param sampleRate Samples measured in Hz
     * @return this
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_SAMPLE_8000
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_SAMPLE_11025
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_SAMPLE_12000
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_SAMPLE_16000
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_SAMPLE_22050
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_SAMPLE_32000
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_SAMPLE_44100
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_SAMPLE_48000
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_SAMPLE_96000
     */
    public T setAudioSampleRate(int sampleRate) {
        requireArgument(sampleRate > 0, "sample rate must be positive");
        this.audioEnabled = true;
        this.audioSampleRate = sampleRate;
        return getThis();
    }

    /**
     * Target output file size (in bytes)
     *
     * @param targetSize The target size in bytes
     * @return this
     */
    public T setTargetSize(long targetSize) {
        requireArgument(targetSize > 0, "target size must be positive");
        this.targetSize = targetSize;
        return getThis();
    }

    /**
     * Decodes but discards input until the offset.
     *
     * @param offset The offset
     * @param units The units the offset is in
     * @return this
     */
    public T setStartOffset(long offset, TimeUnit units) {
        Objects.requireNonNull(units);

        this.startOffset = units.toMillis(offset);

        return getThis();
    }

    /**
     * Stop writing the output after duration is reached.
     *
     * @param duration The duration
     * @param units The units the duration is in
     * @return this
     */
    public T setDuration(long duration, TimeUnit units) {
        Objects.requireNonNull(units);

        this.duration = units.toMillis(duration);

        return getThis();
    }

    public T setStrict(Strict strict) {
        this.strict = Objects.requireNonNull(strict);
        return getThis();
    }

    /**
     * When doing multi-pass we add a little extra padding, to ensure we reach our target
     *
     * @param bitrate bit rate
     * @return this
     */
    public T setPassPaddingBitrate(long bitrate) {
        requireArgument(bitrate > 0, "bitrate must be positive");
        this.passPaddingBitrate = bitrate;
        return getThis();
    }

    /**
     * Sets a audio preset to use.
     *
     * <p>Uses `-apre`.
     *
     * @param preset the preset
     * @return this
     */
    public T setAudioPreset(String preset) {
        this.audioEnabled = true;
        this.audioPreset = checkNotEmpty(preset, "audio preset must not be empty");
        return getThis();
    }

    /**
     * Sets a subtitle preset to use.
     *
     * <p>Uses `-spre`.
     *
     * @param preset the preset
     * @return this
     */
    public T setSubtitlePreset(String preset) {
        this.subtitleEnabled = true;
        this.subtitlePreset = checkNotEmpty(preset, "subtitle preset must not be empty");
        return getThis();
    }

    /**
     * Add additional output arguments (for flags which aren't currently supported).
     *
     * @param values The extra arguments
     * @return this
     */
    public T addExtraArgs(String... values) {
        requireArgument(values.length > 0, "one or more values must be supplied");
        checkNotEmpty(values[0], "first extra arg may not be empty");

        for (String value : values) {
            extraArgs.add(Objects.requireNonNull(value));
        }
        return getThis();
    }

    /**
     * Finished with this output
     *
     * @return the parent FFmpegBuilder
     */
    public FFmpegBuilder done() {
        requireState(parent != null, "Can not call done without parent being set");
        return parent;
    }

    /**
     * Returns a representation of this Builder that can be safely serialised.
     *
     * <p>NOTE: This method is horribly out of date, and its use should be rethought.
     *
     * @return A new EncodingOptions capturing this Builder's state
     */
    public abstract EncodingOptions buildOptions();

    protected List<String> build(int pass) {
        requireState(parent != null, "Can not build without parent being set");
        return build(parent, pass);
    }

    /**
     * Builds the arguments
     *
     * @param parent The parent FFmpegBuilder
     * @param pass The particular pass. For one-pass this value will be zero, for multi-pass, it will
     *     be 1 for the first pass, 2 for the second, and so on.
     * @return The arguments
     */
    protected List<String> build(FFmpegBuilder parent, int pass) {
        Objects.requireNonNull(parent);

        if (pass > 0) {
            // TODO Write a test for this:
            requireArgument(format != null, "Format must be specified when using two-pass");
        }

        List<String> args = new ArrayList<>();

        addGlobalFlags(parent, args);

        if (videoEnabled) {
            addVideoFlags(parent, args);
        } else {
            args.add("-vn");
        }

        if (audioEnabled && pass != 1) {
            addAudioFlags(args);
        } else {
            args.add("-an");
        }

        if (subtitleEnabled) {
            if (subtitleCodec != null && !subtitleCodec.isEmpty()) {
                args.addAll(List.of("-scodec", subtitleCodec));
            }
            if (subtitlePreset != null && !subtitlePreset.isEmpty()) {
                args.addAll(List.of("-spre", subtitlePreset));
            }
        } else {
            args.add("-sn");
        }

        args.addAll(extraArgs);

        if (filename != null && uri != null) {
            throw new IllegalStateException("Only one of filename and uri can be set");
        }

        // Output
        if (pass == 1) {
            args.add(isWindows() ? "NUL" : "/dev/null");
        } else if (filename != null) {
            args.add(filename);
        } else if (uri != null) {
            args.add(uri.toString());
        } else {
            throw new IllegalStateException("Output filename or URI must be specified");
        }

        return List.copyOf(args);
    }

    /**
     * Very basic windows detection,
     * if you need more sophisticated windows detection then override this method.
     */
    protected boolean isWindows() {
        return String.valueOf(System.getProperty("os.name")).toLowerCase().contains("windows");
    }

    protected void addGlobalFlags(FFmpegBuilder parent, List<String> args) {
        if (strict != Strict.NORMAL) {
            args.addAll(List.of("-strict", strict.toString()));
        }

        if (format != null && !format.isEmpty()) {
            args.addAll(List.of("-f", format));
        }

        if (preset != null && !preset.isEmpty()) {
            args.addAll(List.of("-preset", preset));
        }

        if (presetFilename != null && !presetFilename.isEmpty()) {
            args.addAll(List.of("-fpre", presetFilename));
        }

        if (startOffset != null) {
            args.addAll(List.of("-ss", toTimecode(startOffset, TimeUnit.MILLISECONDS)));
        }

        if (duration != null) {
            args.addAll(List.of("-t", toTimecode(duration, TimeUnit.MILLISECONDS)));
        }

        args.addAll(metaTags);
    }

    protected void addAudioFlags(List<String> args) {
        if (audioCodec != null && !audioCodec.isEmpty()) {
            args.addAll(List.of("-acodec", audioCodec));
        }

        if (audioChannels > 0) {
            args.addAll(List.of("-ac", String.valueOf(audioChannels)));
        }

        if (audioSampleRate > 0) {
            args.addAll(List.of("-ar", String.valueOf(audioSampleRate)));
        }

        if (audioPreset != null && !audioPreset.isEmpty()) {
            args.addAll(List.of("-apre", audioPreset));
        }
    }

    protected void addVideoFlags(FFmpegBuilder parent, List<String> args) {
        if (videoFrames != null) {
            args.addAll(List.of("-vframes", videoFrames.toString()));
        }

        if (videoCodec != null && !videoCodec.isEmpty()) {
            args.addAll(List.of("-vcodec", videoCodec));
        }

        if (videoPixelFormat != null && !videoPixelFormat.isEmpty()) {
            args.addAll(List.of("-pix_fmt", videoPixelFormat));
        }

        if (videoCopyInkf) {
            args.add("-copyinkf");
        }

        if (videoMovFlags != null && !videoMovFlags.isEmpty()) {
            args.addAll(List.of("-movflags", videoMovFlags));
        }

        if (videoSize != null) {
            requireArgument(
                    videoWidth == 0 && videoHeight == 0,
                    "Can not specific width or height, as well as an abbreviatied video size");
            args.addAll(List.of("-s", videoSize));

        } else if (videoWidth != 0 && videoHeight != 0) {
            args.addAll(List.of("-s", String.format("%dx%d", videoWidth, videoHeight)));
        }

        // TODO What if width is set but heigh isn't. We don't seem to do anything

        if (videoFrameRate != null) {
            args.addAll(List.of("-r", videoFrameRate.toString()));
        }
    }

    protected static void requireArgument(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    protected static void requireState(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
