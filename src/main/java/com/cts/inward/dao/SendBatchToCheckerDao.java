package com.cts.inward.dao;

import java.util.List;
import com.cts.inward.model.NpciBatchData;

public interface SendBatchToCheckerDao {

    // Fetch batches that are ready to be sent to the checker
    List<NpciBatchData> getReadyBatches();

    List<NpciBatchData> getReadyBatches(Long userId);

    // Update the batch status
    void updateBatchStatusToChecker(Long batchId);
}