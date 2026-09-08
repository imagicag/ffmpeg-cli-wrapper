package ch.imagic.ffmpeg.probe;

import com.google.gson.annotations.SerializedName;

public class FFmpegChapter {

    @SerializedName("id")
    private int id;

    @SerializedName("time_base")
    private String timeBase;

    @SerializedName("start")
    private long start;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("end")
    private long end;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("tags")
    private FFmpegChapterTag tags;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTimeBase() {
        return timeBase;
    }

    public void setTimeBase(String timeBase) {
        this.timeBase = timeBase;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public FFmpegChapterTag getTags() {
        return tags;
    }

    public void setTags(FFmpegChapterTag tags) {
        this.tags = tags;
    }
}
