package com.cts.inward.config;

import java.nio.file.Path;

public class FileConfiguration {

    private final Path inwardRootPath;

    private FileConfiguration(
            Path inwardRootPath) {

        this.inwardRootPath =
                inwardRootPath;
    }

    public static FileConfiguration of(
            Path inwardRootPath) {

        return new FileConfiguration(
                inwardRootPath);
    }

    public Path getInwardRootPath() {
        return inwardRootPath;
    }

    public Path getIncomingPath() {
        return inwardRootPath.resolve(
                "incoming");
    }

    public Path getProcessingPath() {
        return inwardRootPath.resolve(
                "processing");
    }

    public Path getArchivePath() {
        return inwardRootPath.resolve(
                "archive");
    }

    public Path getImagesPath() {
        return inwardRootPath.resolve(
                "images");
    }

    public Path getBatchImagePath(
            String batchId) {

        return getImagesPath()
                .resolve(batchId);
    }

    public Path getChequeImagePath(
            String batchId,
            String chequeNumber) {

        return getImagesPath()
                .resolve(batchId)
                .resolve(chequeNumber);
    }
}