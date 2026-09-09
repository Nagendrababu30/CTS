package com.cts.inward.dao;

import java.util.List;
import java.util.Map;

import com.cts.inward.model.NpciBatchData;

public interface BatchDao {

    void saveBatch(NpciBatchData batchData);

    List<NpciBatchData> getAllBatches();

    List<Map<String, Object>> getBatchesForVerification(
            String userId);

    List<Map<String, Object>> searchBatchesForVerification(
            String batchId,
            String userId);
}