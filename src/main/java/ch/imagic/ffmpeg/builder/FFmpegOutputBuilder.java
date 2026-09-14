package ch.imagic.ffmpeg.builder;

import static ch.imagic.ffmpeg.FFmpegUtils.checkNotEmpty;
import static ch.imagic.ffmpeg.FFmpegUtils.checkValidStream;
import static ch.imagic.ffmpeg.FFmpegUtils.toTimecode;
import static ch.imagic.ffmpeg.builder.MetadataSpecifier.checkValidKey;

import ch.imagic.ffmpeg.probe.FFmpegProbeResult;
import ch.imagic.ffmpeg.probe.Fraction;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** Builds a representation of a single output/encoding setting. */
public class FFmpegOutputBuilder {

    static final Pattern trailingZero = Pattern.compile("\\.0*$");

    final FFmpegBuilder parent;

    /** Output filename or uri. Only one may be set */
    protected String filename;

    protected URI uri;

    protected String format;

    protected Long startOffset; // in milliseconds
    protected Long duration; // in milliseconds

    protected final List<String> metaTags = new ArrayList<>();

    protected Long maxFileSize; // In bytes
    protected String videoMovFlags;

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
    protected Integer videoFrames;
    protected String videoPixelFormat;
    protected String videoSync;

    protected boolean subtitleEnabled = true;
    protected String subtitlePreset;
    private String subtitleCodec;

    protected String preset;
    protected String presetFilename;
    protected final List<String> extraArgs = new ArrayList<>();

    protected Strict strict = Strict.NORMAL;

    protected long targetSize = 0; // in bytes
    protected long passPaddingBitrate = 1024; // in bits per second

    public Double constantRateFactor;

    public String audioSampleFormat;
    public long audioBitRate;
    public Double audioQuality;
    public String audioBitStreamFilter;
    public String audioFilter;

    public long videoBitRate;
    public Double videoQuality;
    public String videoPreset;
    public String videoFilter;
    public String videoBitStreamFilter;
    protected Integer videoBFrames;

    public FFmpegOutputBuilder() {
        this.parent = null;
    }

    protected FFmpegOutputBuilder(FFmpegBuilder parent, String filename) {
        this.parent = Objects.requireNonNull(parent);
        this.filename = checkNotEmpty(filename, "filename must not be empty");
    }

    protected FFmpegOutputBuilder(FFmpegBuilder parent, URI uri) {
        this.parent = Objects.requireNonNull(parent);
        this.uri = checkValidStream(uri);
    }

    public FFmpegOutputBuilder disableVideo() {
        this.videoEnabled = false;
        return this;
    }

    public FFmpegOutputBuilder disableAudio() {
        this.audioEnabled = false;
        return this;
    }

    public FFmpegOutputBuilder disableSubtitle() {
        this.subtitleEnabled = false;
        return this;
    }

    /**
     * Sets a file to use containing presets.
     *
     * <p>Uses `-fpre`.
     *
     * @param presetFilename the preset by filename
     * @return this
     */
    public FFmpegOutputBuilder setPresetFilename(String presetFilename) {
        this.presetFilename = checkNotEmpty(presetFilename, "file preset must not be empty");
        return this;
    }

    /**
     * Sets a preset by name (this only works with some codecs).
     *
     * <p>Uses `-preset`.
     *
     * @param preset the preset
     * @return this
     */
    public FFmpegOutputBuilder setPreset(String preset) {
        this.preset = checkNotEmpty(preset, "preset must not be empty");
        return this;
    }

    public FFmpegOutputBuilder setFilename(String filename) {
        this.filename = checkNotEmpty(filename, "filename must not be empty");
        return this;
    }

    public String getFilename() {
        return filename;
    }

    public FFmpegOutputBuilder setUri(URI uri) {
        this.uri = checkValidStream(uri);
        return this;
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

    public String getVideoSync() {
        return videoSync;
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

    public Integer getVideoBFrames() {
        return videoBFrames;
    }

    public Long getMaxFileSize() {
        return maxFileSize;
    }

    public FFmpegOutputBuilder setFormat(String format) {
        this.format = checkNotEmpty(format, "format must not be empty");
        return this;
    }

    public FFmpegOutputBuilder setVideoCodec(String codec) {
        this.videoEnabled = true;
        this.videoCodec = checkNotEmpty(codec, "codec must not be empty");
        return this;
    }

    public FFmpegOutputBuilder setVideoCopyInkf(boolean copyinkf) {
        this.videoEnabled = true;
        this.videoCopyInkf = copyinkf;
        return this;
    }

    public FFmpegOutputBuilder setConstantRateFactor(double factor) {
        this.videoEnabled = true;
        requireArgument(
                factor >= 0 && Double.isFinite(factor), "constant rate factor must be a non negative finite number");
        this.constantRateFactor = factor;
        return this;
    }

    public FFmpegOutputBuilder setVideoBitRate(long bitRate) {
        requireArgument(bitRate > 0, "bit rate must be positive");
        this.videoEnabled = true;
        this.videoBitRate = bitRate;
        return this;
    }

    public FFmpegOutputBuilder setVideoQuality(double quality) {
        requireArgument(quality >= 0 && Double.isFinite(quality), "quality be a non negative finite number");
        this.videoEnabled = true;
        this.videoQuality = quality;
        return this;
    }

    /**
     * Sets the maximum number of consecutive B-frames. A value of zero disables B-frames.
     *
     * @param frames maximum number of consecutive B-frames
     * @return this
     */
    public FFmpegOutputBuilder setVideoBFrames(int frames) {
        requireArgument(frames >= 0, "B-frame count must not be negative");
        this.videoEnabled = true;
        this.videoBFrames = frames;
        return this;
    }

    public FFmpegOutputBuilder setVideoBitStreamFilter(String filter) {
        this.videoEnabled = true;
        this.videoBitStreamFilter = checkNotEmpty(filter, "filter must not be empty");
        return this;
    }

    /**
     * Sets a video preset to use.
     *
     * <p>Uses `-vpre`.
     *
     * @param preset the preset
     * @return this
     */
    public FFmpegOutputBuilder setVideoPreset(String preset) {
        this.videoEnabled = true;
        this.videoPreset = checkNotEmpty(preset, "video preset must not be empty");
        return this;
    }

    /**
     * Sets Video Filter
     *
     * <p>TODO Build a fluent Filter builder
     *
     * @param filter The video filter.
     * @return this
     */
    public FFmpegOutputBuilder setVideoFilter(String filter) {
        this.videoEnabled = true;
        this.videoFilter = checkNotEmpty(filter, "filter must not be empty");
        return this;
    }

    public FFmpegOutputBuilder setVideoMovFlags(String movflags) {
        this.videoMovFlags = checkNotEmpty(movflags, "movflags must not be empty");
        return this;
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
    public FFmpegOutputBuilder setVideoFrameRate(Fraction frameRate) {
        Objects.requireNonNull(frameRate);
        if (frameRate.doubleValue() < 0) {
            throw new IllegalArgumentException("frame rate must not be negative");
        }
        this.videoEnabled = true;
        this.videoFrameRate = frameRate;
        return this;
    }

    /**
     * Set the video frame rate in terms of frames per interval. For example 24fps would be 24/1,
     * however NTSC TV at 23.976fps would be 24000 per 1001.
     *
     * @param frames The number of frames within the given seconds
     * @param per The number of seconds
     * @return this
     */
    public FFmpegOutputBuilder setVideoFrameRate(int frames, int per) {
        return setVideoFrameRate(Fraction.getFraction(frames, per));
    }

    public FFmpegOutputBuilder setVideoFrameRate(double frameRate) {
        return setVideoFrameRate(Fraction.getFraction(frameRate));
    }

    /**
     * Set the number of video frames to record.
     *
     * @param frames The number of frames
     * @return this
     */
    public FFmpegOutputBuilder setFrames(int frames) {
        if (frames < 0) {
            throw new IllegalArgumentException("frames must not be negative");
        }
        this.videoEnabled = true;
        this.videoFrames = frames;
        return this;
    }

    protected static boolean isValidSize(int widthOrHeight) {
        return widthOrHeight > 0 || widthOrHeight == -1;
    }

    public FFmpegOutputBuilder setVideoWidth(int width) {
        requireArgument(isValidSize(width), "Width must be -1 or greater than zero");

        this.videoEnabled = true;
        this.videoWidth = width;
        return this;
    }

    public FFmpegOutputBuilder setVideoHeight(int height) {
        requireArgument(isValidSize(height), "Height must be -1 or greater than zero");

        this.videoEnabled = true;
        this.videoHeight = height;
        return this;
    }

    public FFmpegOutputBuilder setVideoResolution(int width, int height) {
        requireArgument(
                isValidSize(width) && isValidSize(height), "Both width and height must be -1 or greater than zero");

        this.videoEnabled = true;
        this.videoWidth = width;
        this.videoHeight = height;
        return this;
    }

    /**
     * Sets video resolution based on an abbreviation, e.g. "ntsc" for 720x480, or "vga" for 640x480
     *
     * @see <a href="https://www.ffmpeg.org/ffmpeg-utils.html#Video-size">ffmpeg video size</a>
     * @param abbreviation The abbreviation size. No validation is done, instead the value is passed
     *     as is to ffmpeg.
     * @return this
     */
    public FFmpegOutputBuilder setVideoResolution(String abbreviation) {
        this.videoEnabled = true;
        this.videoSize = checkNotEmpty(abbreviation, "video abbreviation must not be empty");
        return this;
    }

    public FFmpegOutputBuilder setVideoPixelFormat(String format) {
        this.videoEnabled = true;
        this.videoPixelFormat = checkNotEmpty(format, "format must not be empty");
        return this;
    }

    public FFmpegOutputBuilder setVideoSync(String mode) {
        this.videoEnabled = true;
        this.videoSync = checkNotEmpty(mode, "mode must not be empty");
        return this;
    }

    /**
     * Add metadata on output streams. Which keys are possible depends on the used codec.
     *
     * @param key Metadata key, e.g. "comment"
     * @param value Value to set for key
     * @return this
     */
    public FFmpegOutputBuilder addMetaTag(String key, String value) {
        checkValidKey(key);
        checkNotEmpty(value, "value must not be empty");
        metaTags.add("-metadata");
        metaTags.add(key + "=" + value);
        return this;
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
    public FFmpegOutputBuilder addMetaTag(MetadataSpecifier spec, String key, String value) {
        checkValidKey(key);
        checkNotEmpty(value, "value must not be empty");
        metaTags.add("-metadata:" + spec.spec());
        metaTags.add(key + "=" + value);
        return this;
    }

    public FFmpegOutputBuilder setAudioCodec(String codec) {
        this.audioEnabled = true;
        this.audioCodec = checkNotEmpty(codec, "codec must not be empty");
        return this;
    }

    public FFmpegOutputBuilder setSubtitleCodec(String codec) {
        this.subtitleEnabled = true;
        this.subtitleCodec = checkNotEmpty(codec, "codec must not be empty");
        return this;
    }

    /**
     * Sets the number of audio channels
     *
     * @param channels Number of channels
     * @return this
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_MONO
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_STEREO
     */
    public FFmpegOutputBuilder setAudioChannels(int channels) {
        requireArgument(channels > 0, "channels must be positive");
        this.audioEnabled = true;
        this.audioChannels = channels;
        return this;
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
    public FFmpegOutputBuilder setAudioSampleRate(int sampleRate) {
        requireArgument(sampleRate > 0, "sample rate must be positive");
        this.audioEnabled = true;
        this.audioSampleRate = sampleRate;
        return this;
    }

    /**
     * Sets the audio sample format.
     *
     * @param sampleFormat The sample format, one of the ch.imagic.ffmpeg.FFmpeg#AUDIO_FORMAT_*
     *     constants.
     * @return this
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_FORMAT_U8
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_FORMAT_S16
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_FORMAT_S32
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_FORMAT_FLT
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_FORMAT_DBL
     */
    public FFmpegOutputBuilder setAudioSampleFormat(String sampleFormat) {
        this.audioEnabled = true;
        this.audioSampleFormat = checkNotEmpty(sampleFormat, "sample format must not be empty");
        return this;
    }

    /**
     * Sets the Audio bit rate
     *
     * @param bitRate Audio bitrate in bits per second.
     * @return this
     */
    public FFmpegOutputBuilder setAudioBitRate(long bitRate) {
        requireArgument(bitRate > 0, "bit rate must be positive");
        this.audioEnabled = true;
        this.audioBitRate = bitRate;
        return this;
    }

    public FFmpegOutputBuilder setAudioQuality(double quality) {
        requireArgument(quality >= 0 && Double.isFinite(quality), "quality be a non negative finite number");
        this.audioEnabled = true;
        this.audioQuality = quality;
        return this;
    }

    public FFmpegOutputBuilder setAudioBitStreamFilter(String filter) {
        this.audioEnabled = true;
        this.audioBitStreamFilter = checkNotEmpty(filter, "filter must not be empty");
        return this;
    }

    public FFmpegOutputBuilder setMaxFileSize(Long bytes) {
        if (bytes != null && bytes <= 0) {
            throw new IllegalArgumentException("bytes must be positive if set");
        }
        this.maxFileSize = bytes;
        return this;
    }

    /**
     * Sets Audio Filter
     *
     * <p>TODO Build a fluent Filter builder
     *
     * @param filter The audio filter.
     * @return this
     */
    public FFmpegOutputBuilder setAudioFilter(String filter) {
        this.audioEnabled = true;
        this.audioFilter = checkNotEmpty(filter, "filter must not be empty");
        return this;
    }

    /**
     * Target output file size (in bytes)
     *
     * @param targetSize The target size in bytes
     * @return this
     */
    public FFmpegOutputBuilder setTargetSize(long targetSize) {
        requireArgument(targetSize > 0, "target size must be positive");
        this.targetSize = targetSize;
        return this;
    }

    /**
     * Decodes but discards input until the offset.
     *
     * @param offset The offset
     * @param units The units the offset is in
     * @return this
     */
    public FFmpegOutputBuilder setStartOffset(long offset, TimeUnit units) {
        Objects.requireNonNull(units);

        this.startOffset = units.toMillis(offset);

        return this;
    }

    /**
     * Stop writing the output after duration is reached.
     *
     * @param duration The duration
     * @param units The units the duration is in
     * @return this
     */
    public FFmpegOutputBuilder setDuration(long duration, TimeUnit units) {
        Objects.requireNonNull(units);
        if (duration < 0) {
            throw new IllegalArgumentException("duration must not be negative");
        }

        this.duration = units.toMillis(duration);

        return this;
    }

    public FFmpegOutputBuilder setStrict(Strict strict) {
        this.strict = Objects.requireNonNull(strict);
        return this;
    }

    /**
     * When doing multi-pass we add a little extra padding, to ensure we reach our target
     *
     * @param bitrate bit rate
     * @return this
     */
    public FFmpegOutputBuilder setPassPaddingBitrate(long bitrate) {
        requireArgument(bitrate > 0, "bitrate must be positive");
        this.passPaddingBitrate = bitrate;
        return this;
    }

    /**
     * Sets a audio preset to use.
     *
     * <p>Uses `-apre`.
     *
     * @param preset the preset
     * @return this
     */
    public FFmpegOutputBuilder setAudioPreset(String preset) {
        this.audioEnabled = true;
        this.audioPreset = checkNotEmpty(preset, "audio preset must not be empty");
        return this;
    }

    /**
     * Sets a subtitle preset to use.
     *
     * <p>Uses `-spre`.
     *
     * @param preset the preset
     * @return this
     */
    public FFmpegOutputBuilder setSubtitlePreset(String preset) {
        this.subtitleEnabled = true;
        this.subtitlePreset = checkNotEmpty(preset, "subtitle preset must not be empty");
        return this;
    }

    /**
     * Add additional output arguments (for flags which aren't currently supported).
     *
     * @param values The extra arguments
     * @return this
     */
    public FFmpegOutputBuilder addExtraArgs(String... values) {
        requireArgument(values.length > 0, "one or more values must be supplied");
        checkNotEmpty(values[0], "first extra arg may not be empty");

        for (String value : values) {
            extraArgs.add(Objects.requireNonNull(value));
        }
        return this;
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
        if (pass > 0) {
            requireArgument(
                    targetSize != 0 || videoBitRate != 0,
                    "Target size, or video bitrate must be specified when using two-pass");
        }
        if (targetSize > 0) {
            requireState(parent.inputs.size() == 1, "Target size does not support multiple inputs");

            requireArgument(constantRateFactor == null, "Target size can not be used with constantRateFactor");

            FFmpegInputBuilder firstInput = parent.inputs.iterator().next();
            FFmpegProbeResult input = firstInput.getInputProbe();

            requireState(input != null, "Target size requires an input probe");

            // TODO factor in start time and/or number of frames
            double durationInSeconds = input.getFormat().getDuration();
            long totalBitRate = (long) Math.floor((targetSize * 8) / durationInSeconds) - passPaddingBitrate;

            // TODO Calculate audioBitRate
            if (videoEnabled && videoBitRate == 0) {
                long currentAudioBitRate = audioEnabled ? audioBitRate : 0;
                videoBitRate = totalBitRate - currentAudioBitRate;
            } else if (audioEnabled && audioBitRate == 0) {
                audioBitRate = totalBitRate;
            }
        }

        Objects.requireNonNull(parent);

        if (pass > 0) {
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

        // This is a global flag, not just for video.
        if (videoMovFlags != null && !videoMovFlags.isEmpty()) {
            args.addAll(List.of("-movflags", videoMovFlags));
        }

        args.addAll(metaTags);

        if (constantRateFactor != null) {
            args.addAll(List.of("-crf", formatDecimalInteger(constantRateFactor)));
        }

        if (maxFileSize != null) {
            args.addAll(List.of("-fs", maxFileSize.toString()));
        }
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

        if (audioSampleFormat != null && !audioSampleFormat.isEmpty()) {
            args.addAll(List.of("-sample_fmt", audioSampleFormat));
        }

        if (audioBitRate > 0 && audioQuality != null) {
            throw new IllegalStateException("Only one of audioBitRate and audioQuality can be set");
        }

        if (audioBitRate > 0) {
            args.addAll(List.of("-b:a", String.valueOf(audioBitRate)));
        }

        if (audioQuality != null) {
            args.addAll(List.of("-qscale:a", formatDecimalInteger(audioQuality)));
        }

        if (audioBitStreamFilter != null && !audioBitStreamFilter.isEmpty()) {
            args.addAll(List.of("-bsf:a", audioBitStreamFilter));
        }

        if (audioFilter != null && !audioFilter.isEmpty()) {
            args.addAll(List.of("-af", audioFilter));
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
        if (videoSync != null) {
            args.addAll(List.of("-vsync", videoSync));
        }

        if (videoCopyInkf) {
            args.add("-copyinkf");
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

        if (videoBitRate > 0 && videoQuality != null) {
            throw new IllegalStateException("Only one of videoBitRate and videoQuality can be set");
        }

        if (videoBitRate > 0) {
            args.addAll(List.of("-b:v", String.valueOf(videoBitRate)));
        }

        if (videoQuality != null) {
            args.addAll(List.of("-qscale:v", formatDecimalInteger(videoQuality)));
        }

        if (videoBFrames != null) {
            args.addAll(List.of("-bf", videoBFrames.toString()));
        }

        if (videoPreset != null && !videoPreset.isEmpty()) {
            args.addAll(List.of("-vpre", videoPreset));
        }

        if (videoFilter != null && !videoFilter.isEmpty()) {
            requireState(
                    parent.inputs.size() == 1,
                    "Video filter only works with one input, instead use setComplexVideoFilter(..)");
            args.addAll(List.of("-vf", videoFilter));
        }

        if (videoBitStreamFilter != null && !videoBitStreamFilter.isEmpty()) {
            args.addAll(List.of("-bsf:v", videoBitStreamFilter));
        }
    }

    /** Returns a double without a trailing decimal fraction when it represents an integer. */
    protected static String formatDecimalInteger(double d) {
        return trailingZero.matcher(String.valueOf(d)).replaceAll("");
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
