package com.cts.inward.service;

import com.cts.inward.dao.FileSummaryDao;
import com.cts.inward.enums.FileStage;

public class FileSummaryServiceImpl
        implements FileSummaryService {

    private final FileSummaryDao fileSummaryDao;

    private FileSummaryServiceImpl(
            FileSummaryDao fileSummaryDao) {

        this.fileSummaryDao =
                fileSummaryDao;
    }

    public static FileSummaryServiceImpl of(
            FileSummaryDao fileSummaryDao) {

        return new FileSummaryServiceImpl(
                fileSummaryDao);
    }

    @Override
    public void updateFileStage(
            long fileId,
            FileStage fileStage) {

        fileSummaryDao.updateFileStage(
                fileId,
                fileStage);
    }
}