package com.cts.inward.dao;

import com.cts.inward.enums.FileStage;

public interface FileSummaryDao {

    void updateFileStage(
            long fileId,
            FileStage fileStage);
    
}