package ch.imagic.ffmpeg.probe;

import com.google.gson.annotations.SerializedName;

public class FFmpegChapterTag {
    @SerializedName("title")
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
