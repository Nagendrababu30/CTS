package com.cts.inward.model;

public class PibfImageData {

    private final byte[] frontImage;
    private final byte[] backImage;

    private PibfImageData(
            byte[] frontImage,
            byte[] backImage) {

        this.frontImage = frontImage;
        this.backImage = backImage;
    }

    public static PibfImageData of(
            byte[] frontImage,
            byte[] backImage) {

        return new PibfImageData(
                frontImage,
                backImage);
    }

    public byte[] getFrontImage() {
        return frontImage;
    }

    public byte[] getBackImage() {
        return backImage;
    }
}