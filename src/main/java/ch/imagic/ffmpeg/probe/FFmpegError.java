package ch.imagic.ffmpeg.probe;

import com.google.gson.annotations.SerializedName;

public class FFmpegError {
    @SerializedName("code")
    private int code;

    @SerializedName("string")
    private String string;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }
}
