package com.cts.inward.service;

import java.util.List;
import java.util.Map;

import com.cts.inward.dao.BatchDao;
import com.cts.inward.model.InwardBatch;
import com.cts.inward.model.NpciBatchData;

public class BatchServiceImpl implements BatchService {

    private final BatchDao batchDao;

    private BatchServiceImpl(BatchDao batchDao) {
        this.batchDao = batchDao;
    }

    public static BatchServiceImpl of(BatchDao batchDao) {
        return new BatchServiceImpl(batchDao);
    }

    @Override
    public void saveBatch(NpciBatchData batchData) {
        batchDao.saveBatch(batchData);
    }

    @Override
    public List<InwardBatch> getAvailableBatchesForMaker(String userId) {
        return null;
    }

    @Override
    public List<InwardBatch> getBatchesForChecker(String userId) {
        return null;
    }

    @Override
    public InwardBatch getBatch(String batchId) {
        return null;
    }

    @Override
    public boolean acquireLock(String batchId, String userId) {
        return false;
    }

    @Override
    public void releaseLock(String batchId, String userId) {
    }

    @Override
    public void sendToChecker(String batchId, String userId) {
    }

    // =========================================================
    // Verify Batch
    // =========================================================

    @Override
    public List<Map<String, Object>> getBatchesForVerification(
            String userId) {

        return batchDao.getBatchesForVerification(userId);
    }

    @Override
    public List<Map<String, Object>> searchBatchesForVerification(
            String batchId,
            String userId) {

        if (batchId == null || batchId.trim().isEmpty()) {
            return getBatchesForVerification(userId);
        }

        return batchDao.searchBatchesForVerification(
                batchId.trim(),
                userId
        );
    }
}