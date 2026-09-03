package com.cts.inward.model;

import com.cts.inward.enums.FileType;

public class InwardFile {

    private final long fileId;
    private final String fileName;
    private final String filePath;
    private final FileType fileType;

    private InwardFile(
            long fileId,
            String fileName,
            String filePath,
            FileType fileType) {

        this.fileId = fileId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
    }

    public static InwardFile of(
            long fileId,
            String fileName,
            String filePath,
            FileType fileType) {

        return new InwardFile(
                fileId,
                fileName,
                filePath,
                fileType);
    }

    public static InwardFile of(
            String fileName,
            String filePath,
            FileType fileType) {

        return new InwardFile(
                0,
                fileName,
                filePath,
                fileType);
    }

    public long getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public FileType getFileType() {
        return fileType;
    }
}