package ch.imagic.ffmpeg.probe;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;

public class FFmpegProbeResult {
    @SerializedName("error")
    private FFmpegError error;

    @SerializedName("format")
    private FFmpegFormat format;

    @SerializedName("streams")
    private List<FFmpegStream> streams;

    @SerializedName("chapters")
    private List<FFmpegChapter> chapters;

    public FFmpegError getError() {
        return error;
    }

    public void setError(FFmpegError error) {
        this.error = error;
    }

    public boolean hasError() {
        return error != null;
    }

    public FFmpegFormat getFormat() {
        return format;
    }

    public void setFormat(FFmpegFormat format) {
        this.format = format;
    }

    public List<FFmpegStream> getStreams() {
        if (streams == null) return Collections.emptyList();
        return List.copyOf(streams);
    }

    public void setStreams(List<FFmpegStream> streams) {
        this.streams = streams;
    }

    public List<FFmpegChapter> getChapters() {
        if (chapters == null) return Collections.emptyList();
        return List.copyOf(chapters);
    }

    public void setChapters(List<FFmpegChapter> chapters) {
        this.chapters = chapters;
    }
}
