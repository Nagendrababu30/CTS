package com.cts.inward.dao;

import com.cts.inward.enums.FileStage;

public interface FileSummaryDao {

    void insertFileSummary(long fileId, String fileName);

    void updateFileStage(long fileId, FileStage fileStage);
}
