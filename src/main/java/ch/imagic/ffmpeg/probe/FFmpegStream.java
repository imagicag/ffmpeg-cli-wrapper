package ch.imagic.ffmpeg.probe;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class FFmpegStream {

    @SerializedName("index")
    private int index;

    @SerializedName("codec_name")
    private String codecName;

    @SerializedName("codec_long_name")
    private String codecLongName;

    @SerializedName("profile")
    private String profile;

    @SerializedName("codec_type")
    private FFmpegCodecType codecType;

    @SerializedName("codec_time_base")
    private Fraction codecTimeBase;

    @SerializedName("codec_tag_string")
    private String codecTagString;

    @SerializedName("codec_tag")
    private String codecTag;

    @SerializedName("width")
    private int width;

    @SerializedName("height")
    private int height;

    @SerializedName("has_b_frames")
    private int hasBFrames;

    // TODO Change to a Ratio/Fraction object
    @SerializedName("sample_aspect_ratio")
    private String sampleAspectRatio;

    @SerializedName("display_aspect_ratio")
    private String displayAspectRatio;

    @SerializedName("pix_fmt")
    private String pixFmt;

    @SerializedName("level")
    private int level;

    @SerializedName("chroma_location")
    private String chromaLocation;

    @SerializedName("refs")
    private int refs;

    @SerializedName("is_avc")
    private String avc;

    @SerializedName("nal_length_size")
    private String nalLengthSize;

    @SerializedName("r_frame_rate")
    private Fraction rFrameRate;

    @SerializedName("avg_frame_rate")
    private Fraction avgFrameRate;

    @SerializedName("time_base")
    private Fraction timeBase;

    @SerializedName("start_pts")
    private long startPts;

    @SerializedName("start_time")
    private double startTime;

    @SerializedName("duration_ts")
    private long durationTs;

    @SerializedName("duration")
    private double duration;

    @SerializedName("bit_rate")
    private long bitRate;

    @SerializedName("max_bit_rate")
    private long maxBitRate;

    @SerializedName("bits_per_raw_sample")
    private int bitsPerRawSample;

    @SerializedName("bits_per_sample")
    private int bitsPerSample;

    @SerializedName("nb_frames")
    private long nbFrames;

    @SerializedName("sample_fmt")
    private String sampleFmt;

    @SerializedName("sample_rate")
    private int sampleRate;

    @SerializedName("channels")
    private int channels;

    @SerializedName("channel_layout")
    private String channelLayout;

    @SerializedName("color_range")
    private String colorRange;

    @SerializedName("color_space")
    private String colorSpace;

    @SerializedName("color_transfer")
    private String colorTransfer;

    @SerializedName("color_primaries")
    private String colorPrimaries;

    @SerializedName("disposition")
    private FFmpegDisposition disposition;

    @SerializedName("tags")
    private Map<String, String> tags;

    @SerializedName("side_data_list")
    private FFmpegSideData[] sideDataList;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getCodecName() {
        return codecName;
    }

    public void setCodecName(String codecName) {
        this.codecName = codecName;
    }

    public String getCodecLongName() {
        return codecLongName;
    }

    public void setCodecLongName(String codecLongName) {
        this.codecLongName = codecLongName;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public FFmpegCodecType getCodecType() {
        return codecType;
    }

    public void setCodecType(FFmpegCodecType codecType) {
        this.codecType = codecType;
    }

    public Fraction getCodecTimeBase() {
        return codecTimeBase;
    }

    public void setCodecTimeBase(Fraction codecTimeBase) {
        this.codecTimeBase = codecTimeBase;
    }

    public String getCodecTagString() {
        return codecTagString;
    }

    public void setCodecTagString(String codecTagString) {
        this.codecTagString = codecTagString;
    }

    public String getCodecTag() {
        return codecTag;
    }

    public void setCodecTag(String codecTag) {
        this.codecTag = codecTag;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getHasBFrames() {
        return hasBFrames;
    }

    public void setHasBFrames(int hasBFrames) {
        this.hasBFrames = hasBFrames;
    }

    public String getSampleAspectRatio() {
        return sampleAspectRatio;
    }

    public void setSampleAspectRatio(String sampleAspectRatio) {
        this.sampleAspectRatio = sampleAspectRatio;
    }

    public String getDisplayAspectRatio() {
        return displayAspectRatio;
    }

    public void setDisplayAspectRatio(String displayAspectRatio) {
        this.displayAspectRatio = displayAspectRatio;
    }

    public String getPixFmt() {
        return pixFmt;
    }

    public void setPixFmt(String pixFmt) {
        this.pixFmt = pixFmt;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getChromaLocation() {
        return chromaLocation;
    }

    public void setChromaLocation(String chromaLocation) {
        this.chromaLocation = chromaLocation;
    }

    public int getRefs() {
        return refs;
    }

    public void setRefs(int refs) {
        this.refs = refs;
    }

    public String getAvc() {
        return avc;
    }

    public void setAvc(String avc) {
        this.avc = avc;
    }

    public String getNalLengthSize() {
        return nalLengthSize;
    }

    public void setNalLengthSize(String nalLengthSize) {
        this.nalLengthSize = nalLengthSize;
    }

    public Fraction getRFrameRate() {
        return rFrameRate;
    }

    public void setRFrameRate(Fraction rFrameRate) {
        this.rFrameRate = rFrameRate;
    }

    public Fraction getAvgFrameRate() {
        return avgFrameRate;
    }

    public void setAvgFrameRate(Fraction avgFrameRate) {
        this.avgFrameRate = avgFrameRate;
    }

    public Fraction getTimeBase() {
        return timeBase;
    }

    public void setTimeBase(Fraction timeBase) {
        this.timeBase = timeBase;
    }

    public long getStartPts() {
        return startPts;
    }

    public void setStartPts(long startPts) {
        this.startPts = startPts;
    }

    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public long getDurationTs() {
        return durationTs;
    }

    public void setDurationTs(long durationTs) {
        this.durationTs = durationTs;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public long getBitRate() {
        return bitRate;
    }

    public void setBitRate(long bitRate) {
        this.bitRate = bitRate;
    }

    public long getMaxBitRate() {
        return maxBitRate;
    }

    public void setMaxBitRate(long maxBitRate) {
        this.maxBitRate = maxBitRate;
    }

    public int getBitsPerRawSample() {
        return bitsPerRawSample;
    }

    public void setBitsPerRawSample(int bitsPerRawSample) {
        this.bitsPerRawSample = bitsPerRawSample;
    }

    public int getBitsPerSample() {
        return bitsPerSample;
    }

    public void setBitsPerSample(int bitsPerSample) {
        this.bitsPerSample = bitsPerSample;
    }

    public long getNbFrames() {
        return nbFrames;
    }

    public void setNbFrames(long nbFrames) {
        this.nbFrames = nbFrames;
    }

    public String getSampleFmt() {
        return sampleFmt;
    }

    public void setSampleFmt(String sampleFmt) {
        this.sampleFmt = sampleFmt;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public String getChannelLayout() {
        return channelLayout;
    }

    public void setChannelLayout(String channelLayout) {
        this.channelLayout = channelLayout;
    }

    public String getColorRange() {
        return colorRange;
    }

    public void setColorRange(String colorRange) {
        this.colorRange = colorRange;
    }

    public String getColorSpace() {
        return colorSpace;
    }

    public void setColorSpace(String colorSpace) {
        this.colorSpace = colorSpace;
    }

    public String getColorTransfer() {
        return colorTransfer;
    }

    public void setColorTransfer(String colorTransfer) {
        this.colorTransfer = colorTransfer;
    }

    public String getColorPrimaries() {
        return colorPrimaries;
    }

    public void setColorPrimaries(String colorPrimaries) {
        this.colorPrimaries = colorPrimaries;
    }

    public FFmpegDisposition getDisposition() {
        return disposition;
    }

    public void setDisposition(FFmpegDisposition disposition) {
        this.disposition = disposition;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public FFmpegSideData[] getSideDataList() {
        return sideDataList;
    }

    public void setSideDataList(FFmpegSideData[] sideDataList) {
        this.sideDataList = sideDataList;
    }
}
