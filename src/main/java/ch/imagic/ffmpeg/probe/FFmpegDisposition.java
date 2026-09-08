package ch.imagic.ffmpeg.probe;

import com.google.gson.annotations.SerializedName;

/** Represents the AV_DISPOSITION_* fields */
public class FFmpegDisposition {
    @SerializedName("default")
    private boolean defaultDisposition;

    @SerializedName("dub")
    private boolean dub;

    @SerializedName("original")
    private boolean original;

    @SerializedName("comment")
    private boolean comment;

    @SerializedName("lyrics")
    private boolean lyrics;

    @SerializedName("karaoke")
    private boolean karaoke;

    @SerializedName("forced")
    private boolean forced;

    @SerializedName("hearing_impaired")
    private boolean hearingImpaired;

    @SerializedName("visual_impaired")
    private boolean visualImpaired;

    @SerializedName("clean_effects")
    private boolean cleanEffects;

    @SerializedName("attached_pic")
    private boolean attachedPic;

    @SerializedName("captions")
    private boolean captions;

    @SerializedName("descriptions")
    private boolean descriptions;

    @SerializedName("metadata")
    private boolean metadata;

    public boolean isDefaultDisposition() {
        return defaultDisposition;
    }

    public void setDefaultDisposition(boolean defaultDisposition) {
        this.defaultDisposition = defaultDisposition;
    }

    public boolean isDub() {
        return dub;
    }

    public void setDub(boolean dub) {
        this.dub = dub;
    }

    public boolean isOriginal() {
        return original;
    }

    public void setOriginal(boolean original) {
        this.original = original;
    }

    public boolean isComment() {
        return comment;
    }

    public void setComment(boolean comment) {
        this.comment = comment;
    }

    public boolean isLyrics() {
        return lyrics;
    }

    public void setLyrics(boolean lyrics) {
        this.lyrics = lyrics;
    }

    public boolean isKaraoke() {
        return karaoke;
    }

    public void setKaraoke(boolean karaoke) {
        this.karaoke = karaoke;
    }

    public boolean isForced() {
        return forced;
    }

    public void setForced(boolean forced) {
        this.forced = forced;
    }

    public boolean isHearingImpaired() {
        return hearingImpaired;
    }

    public void setHearingImpaired(boolean hearingImpaired) {
        this.hearingImpaired = hearingImpaired;
    }

    public boolean isVisualImpaired() {
        return visualImpaired;
    }

    public void setVisualImpaired(boolean visualImpaired) {
        this.visualImpaired = visualImpaired;
    }

    public boolean isCleanEffects() {
        return cleanEffects;
    }

    public void setCleanEffects(boolean cleanEffects) {
        this.cleanEffects = cleanEffects;
    }

    public boolean isAttachedPic() {
        return attachedPic;
    }

    public void setAttachedPic(boolean attachedPic) {
        this.attachedPic = attachedPic;
    }

    public boolean isCaptions() {
        return captions;
    }

    public void setCaptions(boolean captions) {
        this.captions = captions;
    }

    public boolean isDescriptions() {
        return descriptions;
    }

    public void setDescriptions(boolean descriptions) {
        this.descriptions = descriptions;
    }

    public boolean isMetadata() {
        return metadata;
    }

    public void setMetadata(boolean metadata) {
        this.metadata = metadata;
    }
}
