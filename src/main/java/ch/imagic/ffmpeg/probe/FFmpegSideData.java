package ch.imagic.ffmpeg.probe;

import com.google.gson.annotations.SerializedName;

public class FFmpegSideData {
    @SerializedName("side_data_type")
    private String sideDataType;

    @SerializedName("displaymatrix")
    private String displayMatrix;

    @SerializedName("rotation")
    private int rotation;

    public String getSideDataType() {
        return sideDataType;
    }

    public void setSideDataType(String sideDataType) {
        this.sideDataType = sideDataType;
    }

    public String getDisplayMatrix() {
        return displayMatrix;
    }

    public void setDisplayMatrix(String displayMatrix) {
        this.displayMatrix = displayMatrix;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }
}
