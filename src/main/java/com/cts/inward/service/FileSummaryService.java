package com.cts.inward.service;

import com.cts.inward.enums.FileStage;

public interface FileSummaryService {

    void updateFileStage(
            long fileId,
            FileStage fileStage);
}