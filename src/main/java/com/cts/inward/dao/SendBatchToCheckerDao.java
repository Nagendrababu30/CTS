package com.cts.inward.dao;

import java.util.List;
import com.cts.inward.model.NpciBatchData;

public interface SendBatchToCheckerDao {

   
    List<NpciBatchData> getReadyBatches();

    List<NpciBatchData> getReadyBatches(Long userId);

    void updateBatchStatusToChecker(Long batchId);
}