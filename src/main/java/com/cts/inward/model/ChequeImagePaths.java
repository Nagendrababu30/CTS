package com.cts.inward.model;

public class ChequeImagePaths {

    private final String frontImagePath;
    private final String backImagePath;

    private ChequeImagePaths(
            String frontImagePath,
            String backImagePath) {

        this.frontImagePath = frontImagePath;
        this.backImagePath = backImagePath;
    }

    public static ChequeImagePaths of(
            String frontImagePath,
            String backImagePath) {

        return new ChequeImagePaths(
                frontImagePath,
                backImagePath);
    }

    public String getFrontImagePath() {
        return frontImagePath;
    }

    public String getBackImagePath() {
        return backImagePath;
    }
}