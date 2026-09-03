package com.cts.inward.model;

public class ChequeImage {

    private final long imageId;
    private final String chequeNumber;
    private final String frontPath;
    private final String backPath;

    private ChequeImage(
            long imageId,
            String chequeNumber,
            String frontPath,
            String backPath) {

        this.imageId = imageId;
        this.chequeNumber = chequeNumber;
        this.frontPath = frontPath;
        this.backPath = backPath;
    }

    public static ChequeImage of(
            String chequeNumber,
            String frontPath,
            String backPath) {

        return new ChequeImage(
                0,
                chequeNumber,
                frontPath,
                backPath);
    }

    public static ChequeImage of(
            long imageId,
            String chequeNumber,
            String frontPath,
            String backPath) {

        return new ChequeImage(
                imageId,
                chequeNumber,
                frontPath,
                backPath);
    }

    public long getImageId() {
        return imageId;
    }

    public String getChequeNumber() {
        return chequeNumber;
    }

    public String getFrontPath() {
        return frontPath;
    }

    public String getBackPath() {
        return backPath;
    }
}