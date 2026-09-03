package com.cts.inward.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystemInitializer {

    private final FileConfiguration fileConfiguration;

    private FileSystemInitializer(
            FileConfiguration fileConfiguration) {

        this.fileConfiguration =
                fileConfiguration;
    }

    public static FileSystemInitializer of(
            FileConfiguration fileConfiguration) {

        return new FileSystemInitializer(
                fileConfiguration);
    }

    public void initialize() {

        try {

            createDirectory(
                    fileConfiguration
                            .getInwardRootPath());

            createDirectory(
                    fileConfiguration
                            .getIncomingPath()
                            .resolve("pxf"));

            createDirectory(
                    fileConfiguration
                            .getIncomingPath()
                            .resolve("pibf"));

            createDirectory(
                    fileConfiguration
                            .getIncomingPath()
                            .resolve("ocr"));

            createDirectory(
                    fileConfiguration
                            .getProcessingPath()
                            .resolve("pxf"));

            createDirectory(
                    fileConfiguration
                            .getProcessingPath()
                            .resolve("pibf"));

            createDirectory(
                    fileConfiguration
                            .getProcessingPath()
                            .resolve("ocr"));

            createDirectory(
                    fileConfiguration
                            .getArchivePath()
                            .resolve("pxf"));

            createDirectory(
                    fileConfiguration
                            .getArchivePath()
                            .resolve("pibf"));

            createDirectory(
                    fileConfiguration
                            .getArchivePath()
                            .resolve("ocr"));

            createDirectory(
                    fileConfiguration
                            .getImagesPath());

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to initialize inward "
                    + "file system",
                    e);
        }
    }

    private void createDirectory(
            Path path) throws IOException {

        Files.createDirectories(path);
    }
}