package ch.imagic.ffmpeg.probe;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class FFmpegFormat {
    @SerializedName("filename")
    private String filename;

    @SerializedName("nb_streams")
    private int nbStreams;

    @SerializedName("nb_programs")
    private int nbPrograms;

    @SerializedName("format_name")
    private String formatName;

    @SerializedName("format_long_name")
    private String formatLongName;

    @SerializedName("start_time")
    private double startTime;

    /** Duration in seconds */
    // TODO Change this to java.time.Duration
    @SerializedName("duration")
    private double duration;

    /** File size in bytes */
    @SerializedName("size")
    private long size;

    /** Bitrate */
    @SerializedName("bit_rate")
    private long bitRate;

    @SerializedName("probe_score")
    private int probeScore;

    @SerializedName("tags")
    private Map<String, String> tags;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getNbStreams() {
        return nbStreams;
    }

    public void setNbStreams(int nbStreams) {
        this.nbStreams = nbStreams;
    }

    public int getNbPrograms() {
        return nbPrograms;
    }

    public void setNbPrograms(int nbPrograms) {
        this.nbPrograms = nbPrograms;
    }

    public String getFormatName() {
        return formatName;
    }

    public void setFormatName(String formatName) {
        this.formatName = formatName;
    }

    public String getFormatLongName() {
        return formatLongName;
    }

    public void setFormatLongName(String formatLongName) {
        this.formatLongName = formatLongName;
    }

    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getBitRate() {
        return bitRate;
    }

    public void setBitRate(long bitRate) {
        this.bitRate = bitRate;
    }

    public int getProbeScore() {
        return probeScore;
    }

    public void setProbeScore(int probeScore) {
        this.probeScore = probeScore;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }
}
