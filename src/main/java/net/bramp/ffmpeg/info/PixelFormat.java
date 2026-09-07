package net.bramp.ffmpeg.info;

import java.util.Objects;

public class PixelFormat {
    private final String name;
    private final int numberOfComponents;
    private final int bitsPerPixel;

    private final boolean canDecode;
    private final boolean canEncode;
    private final boolean hardwareAccelerated;
    private final boolean palettedFormat;
    private final boolean bitstreamFormat;

    public PixelFormat(String name, int numberOfComponents, int bitsPerPixel, String flags) {
        this.name = name;
        this.numberOfComponents = numberOfComponents;
        this.bitsPerPixel = bitsPerPixel;

        this.canDecode = flags.charAt(0) == 'I';
        this.canEncode = flags.charAt(1) == 'O';
        this.hardwareAccelerated = flags.charAt(2) == 'H';
        this.palettedFormat = flags.charAt(3) == 'P';
        this.bitstreamFormat = flags.charAt(4) == 'B';
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PixelFormat)) {
            return false;
        }
        PixelFormat other = (PixelFormat) obj;
        return numberOfComponents == other.numberOfComponents
                && bitsPerPixel == other.bitsPerPixel
                && canDecode == other.canDecode
                && canEncode == other.canEncode
                && hardwareAccelerated == other.hardwareAccelerated
                && palettedFormat == other.palettedFormat
                && bitstreamFormat == other.bitstreamFormat
                && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                numberOfComponents,
                bitsPerPixel,
                canDecode,
                canEncode,
                hardwareAccelerated,
                palettedFormat,
                bitstreamFormat);
    }

    public String getName() {
        return name;
    }

    public int getBitsPerPixel() {
        return bitsPerPixel;
    }

    public int getNumberOfComponents() {
        return numberOfComponents;
    }

    public boolean canEncode() {
        return canEncode;
    }

    public boolean canDecode() {
        return canDecode;
    }

    public boolean isHardwareAccelerated() {
        return hardwareAccelerated;
    }

    public boolean isPalettedFormat() {
        return palettedFormat;
    }

    public boolean isBitstreamFormat() {
        return bitstreamFormat;
    }
}
