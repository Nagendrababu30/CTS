package com.cts.inward.model;

import com.cts.inward.enums.FileStage;

public class FileSummary {

    private final long fileSummaryId;
    private final long fileId;
    private final String fileName;
    private final FileStage fileStage;

    private FileSummary(
            long fileSummaryId,
            long fileId,
            String fileName,
            FileStage fileStage) {

        this.fileSummaryId =
                fileSummaryId;

        this.fileId =
                fileId;

        this.fileName =
                fileName;

        this.fileStage =
                fileStage;
    }

    public static FileSummary of(
            long fileSummaryId,
            long fileId,
            String fileName,
            FileStage fileStage) {

        return new FileSummary(
                fileSummaryId,
                fileId,
                fileName,
                fileStage);
    }

    public long getFileSummaryId() {
        return fileSummaryId;
    }

    public long getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public FileStage getFileStage() {
        return fileStage;
    }
}