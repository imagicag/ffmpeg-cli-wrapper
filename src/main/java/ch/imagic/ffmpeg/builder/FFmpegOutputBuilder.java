package ch.imagic.ffmpeg.builder;

import static ch.imagic.ffmpeg.Preconditions.checkNotEmpty;

import ch.imagic.ffmpeg.options.AudioEncodingOptions;
import ch.imagic.ffmpeg.options.EncodingOptions;
import ch.imagic.ffmpeg.options.MainEncodingOptions;
import ch.imagic.ffmpeg.options.VideoEncodingOptions;
import ch.imagic.ffmpeg.probe.FFmpegProbeResult;
import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

/** Builds a representation of a single output/encoding setting */
public class FFmpegOutputBuilder extends AbstractFFmpegStreamBuilder<FFmpegOutputBuilder> {

    static final Pattern trailingZero = Pattern.compile("\\.0*$");

    public Double constantRateFactor;

    public String audio_sample_format;
    public long audio_bit_rate;
    public Double audio_quality;
    public String audio_bit_stream_filter;
    public String audio_filter;

    public long video_bit_rate;
    public Double video_quality;
    public String video_preset;
    public String video_filter;
    public String video_bit_stream_filter;

    public FFmpegOutputBuilder() {
        super();
    }

    protected FFmpegOutputBuilder(FFmpegBuilder parent, String filename) {
        super(parent, filename);
    }

    protected FFmpegOutputBuilder(FFmpegBuilder parent, URI uri) {
        super(parent, uri);
    }

    @Override
    public FFmpegOutputBuilder useOptions(MainEncodingOptions opts) {
        super.useOptions(opts);
        return this;
    }

    @Override
    public FFmpegOutputBuilder useOptions(AudioEncodingOptions opts) {
        super.useOptions(opts);
        if (opts.sample_format != null) audio_sample_format = opts.sample_format;
        if (opts.bit_rate != 0) audio_bit_rate = opts.bit_rate;
        if (opts.quality != null) audio_quality = opts.quality;
        return this;
    }

    @Override
    public FFmpegOutputBuilder useOptions(VideoEncodingOptions opts) {
        super.useOptions(opts);
        if (opts.bit_rate != 0) video_bit_rate = opts.bit_rate;
        if (opts.filter != null) video_filter = opts.filter;
        if (opts.preset != null) video_preset = opts.preset;
        return this;
    }

    public FFmpegOutputBuilder setConstantRateFactor(double factor) {
        requireArgument(factor >= 0, "constant rate factor must be greater or equal to zero");
        this.constantRateFactor = factor;
        return this;
    }

    public FFmpegOutputBuilder setVideoBitRate(long bit_rate) {
        requireArgument(bit_rate > 0, "bit rate must be positive");
        this.video_enabled = true;
        this.video_bit_rate = bit_rate;
        return this;
    }

    public FFmpegOutputBuilder setVideoQuality(double quality) {
        requireArgument(quality > 0, "quality must be positive");
        this.video_enabled = true;
        this.video_quality = quality;
        return this;
    }

    public FFmpegOutputBuilder setVideoBitStreamFilter(String filter) {
        this.video_bit_stream_filter = checkNotEmpty(filter, "filter must not be empty");
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
        this.video_enabled = true;
        this.video_preset = checkNotEmpty(preset, "video preset must not be empty");
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
        this.video_enabled = true;
        this.video_filter = checkNotEmpty(filter, "filter must not be empty");
        return this;
    }

    /**
     * Sets the audio bit depth.
     *
     * @param bit_depth The sample format, one of the ch.imagic.ffmpeg.FFmpeg#AUDIO_DEPTH_* constants.
     * @return this
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_DEPTH_U8
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_DEPTH_S16
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_DEPTH_S32
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_DEPTH_FLT
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_DEPTH_DBL
     * @deprecated use {@link #setAudioSampleFormat} instead.
     */
    @Deprecated
    public FFmpegOutputBuilder setAudioBitDepth(String bit_depth) {
        return setAudioSampleFormat(bit_depth);
    }

    /**
     * Sets the audio sample format.
     *
     * @param sample_format The sample format, one of the ch.imagic.ffmpeg.FFmpeg#AUDIO_FORMAT_*
     *     constants.
     * @return this
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_FORMAT_U8
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_FORMAT_S16
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_FORMAT_S32
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_FORMAT_FLT
     * @see ch.imagic.ffmpeg.FFmpeg#AUDIO_FORMAT_DBL
     */
    public FFmpegOutputBuilder setAudioSampleFormat(String sample_format) {
        this.audio_enabled = true;
        this.audio_sample_format = checkNotEmpty(sample_format, "sample format must not be empty");
        return this;
    }

    /**
     * Sets the Audio bit rate
     *
     * @param bit_rate Audio bitrate in bits per second.
     * @return this
     */
    public FFmpegOutputBuilder setAudioBitRate(long bit_rate) {
        requireArgument(bit_rate > 0, "bit rate must be positive");
        this.audio_enabled = true;
        this.audio_bit_rate = bit_rate;
        return this;
    }

    public FFmpegOutputBuilder setAudioQuality(double quality) {
        requireArgument(quality > 0, "quality must be positive");
        this.audio_enabled = true;
        this.audio_quality = quality;
        return this;
    }

    public FFmpegOutputBuilder setAudioBitStreamFilter(String filter) {
        this.audio_enabled = true;
        this.audio_bit_stream_filter = checkNotEmpty(filter, "filter must not be empty");
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
        this.audio_enabled = true;
        this.audio_filter = checkNotEmpty(filter, "filter must not be empty");
        return this;
    }

    /**
     * Returns a representation of this Builder that can be safely serialised.
     *
     * <p>NOTE: This method is horribly out of date, and its use should be rethought.
     *
     * @return A new EncodingOptions capturing this Builder's state
     */
    @Override
    public EncodingOptions buildOptions() {
        return new EncodingOptions(
                new MainEncodingOptions(format, startOffset, duration),
                new AudioEncodingOptions(
                        audio_enabled,
                        audio_codec,
                        audio_channels,
                        audio_sample_rate,
                        audio_sample_format,
                        audio_bit_rate,
                        audio_quality),
                new VideoEncodingOptions(
                        video_enabled,
                        video_codec,
                        video_frame_rate,
                        video_width,
                        video_height,
                        video_bit_rate,
                        video_frames,
                        video_filter,
                        video_preset));
    }

    @Override
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
    @Override
    protected List<String> build(FFmpegBuilder parent, int pass) {
        if (pass > 0) {
            requireArgument(
                    targetSize != 0 || video_bit_rate != 0,
                    "Target size, or video bitrate must be specified when using two-pass");
        }
        if (targetSize > 0) {
            requireState(parent.inputs.size() == 1, "Target size does not support multiple inputs");

            requireArgument(constantRateFactor == null, "Target size can not be used with constantRateFactor");

            String firstInput = parent.inputs.iterator().next();
            FFmpegProbeResult input = parent.inputProbes.get(firstInput);

            requireState(input != null, "Target size must be used with setInput(FFmpegProbeResult)");

            // TODO factor in start time and/or number of frames

            double durationInSeconds = input.format.duration;
            long totalBitRate = (long) Math.floor((targetSize * 8) / durationInSeconds) - pass_padding_bitrate;

            // TODO Calculate audioBitRate

            if (video_enabled && video_bit_rate == 0) {
                // Video (and possibly audio)
                long audioBitRate = audio_enabled ? audio_bit_rate : 0;
                video_bit_rate = totalBitRate - audioBitRate;
            } else if (audio_enabled && audio_bit_rate == 0) {
                // Just Audio
                audio_bit_rate = totalBitRate;
            }
        }

        return super.build(parent, pass);
    }

    /**
     * Returns a double formatted as a string. If the double is an integer, then trailing zeros are
     * striped.
     *
     * @param d the double to format.
     * @return The formatted double.
     */
    protected static String formatDecimalInteger(double d) {
        return trailingZero.matcher(String.valueOf(d)).replaceAll("");
    }

    @Override
    protected void addGlobalFlags(FFmpegBuilder parent, List<String> args) {
        super.addGlobalFlags(parent, args);

        if (constantRateFactor != null) {
            args.addAll(List.of("-crf", formatDecimalInteger(constantRateFactor)));
        }
    }

    @Override
    protected void addVideoFlags(FFmpegBuilder parent, List<String> args) {
        super.addVideoFlags(parent, args);

        if (video_bit_rate > 0 && video_quality != null) {
            // I'm not sure, but it seems video_quality overrides video_bit_rate, so don't allow both
            throw new IllegalStateException("Only one of video_bit_rate and video_quality can be set");
        }

        if (video_bit_rate > 0) {
            args.addAll(List.of("-b:v", String.valueOf(video_bit_rate)));
        }

        if (video_quality != null) {
            args.addAll(List.of("-qscale:v", formatDecimalInteger(video_quality)));
        }

        if (video_preset != null && !video_preset.isEmpty()) {
            args.addAll(List.of("-vpre", video_preset));
        }

        if (video_filter != null && !video_filter.isEmpty()) {
            requireState(
                    parent.inputs.size() == 1,
                    "Video filter only works with one input, instead use setComplexVideoFilter(..)");
            args.addAll(List.of("-vf", video_filter));
        }

        if (video_bit_stream_filter != null && !video_bit_stream_filter.isEmpty()) {
            args.addAll(List.of("-bsf:v", video_bit_stream_filter));
        }
    }

    @Override
    protected void addAudioFlags(List<String> args) {
        super.addAudioFlags(args);

        if (audio_sample_format != null && !audio_sample_format.isEmpty()) {
            args.addAll(List.of("-sample_fmt", audio_sample_format));
        }

        if (audio_bit_rate > 0 && audio_quality != null && throwWarnings) {
            // I'm not sure, but it seems audio_quality overrides audio_bit_rate, so don't allow both
            throw new IllegalStateException("Only one of audio_bit_rate and audio_quality can be set");
        }

        if (audio_bit_rate > 0) {
            args.addAll(List.of("-b:a", String.valueOf(audio_bit_rate)));
        }

        if (audio_quality != null) {
            args.addAll(List.of("-qscale:a", formatDecimalInteger(audio_quality)));
        }

        if (audio_bit_stream_filter != null && !audio_bit_stream_filter.isEmpty()) {
            args.addAll(List.of("-bsf:a", audio_bit_stream_filter));
        }

        if (audio_filter != null && !audio_filter.isEmpty()) {
            args.addAll(List.of("-af", audio_filter));
        }
    }

    @Override
    protected FFmpegOutputBuilder getThis() {
        return this;
    }
}
