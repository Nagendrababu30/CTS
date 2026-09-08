package com.cts.inward.service;

import java.util.ArrayList;
import java.util.List;

import com.cts.inward.dao.BatchDao;
import com.cts.inward.model.InwardBatch;
import com.cts.inward.model.NpciBatchData;

public class BatchServiceImpl
        implements BatchService {

    private final BatchDao batchDao;

    private BatchServiceImpl(
            BatchDao batchDao) {

        this.batchDao = batchDao;
    }

    public static BatchServiceImpl of(
            BatchDao batchDao) {

        return new BatchServiceImpl(
                batchDao);
    }

    // ---------------------------------------------------------
    // Save batch
    // ---------------------------------------------------------

    @Override
    public void saveBatch(
            NpciBatchData batchData) {

        batchDao.saveBatch(batchData);
    }

    // ---------------------------------------------------------
    // Maker Data Entry queue
    // ---------------------------------------------------------

    @Override
    public List<InwardBatch>
            getAvailableBatchesForMaker(
                    String userId) {

        List<NpciBatchData> batches =
                batchDao.getAllBatches();

        List<InwardBatch> result =
                new ArrayList<>();

        if (batches == null) {
            return result;
        }

        for (NpciBatchData batch : batches) {

            result.add(
                    convert(batch));
        }

        return result;
    }

    // ---------------------------------------------------------
    // Checker queue
    // ---------------------------------------------------------

    @Override
    public List<InwardBatch>
            getBatchesForChecker(
                    String userId) {

        /*
         * Checker filtering will be implemented
         * when the checker workflow is connected.
         */
        return getAvailableBatchesForMaker(userId);
    }

    // ---------------------------------------------------------
    // Get one batch
    // ---------------------------------------------------------

    @Override
    public InwardBatch getBatch(
            String batchId) {

        long id =
                Long.parseLong(batchId);

        List<NpciBatchData> batches =
                batchDao.getAllBatches();

        if (batches == null) {
            return null;
        }

        for (NpciBatchData batch : batches) {

            if (batch.getBatchId() == id) {

                return convert(batch);
            }
        }

        return null;
    }

    // ---------------------------------------------------------
    // Lock
    // ---------------------------------------------------------

    @Override
    public boolean acquireLock(
            String batchId,
            String userId) {

        /*
         * Locking will be connected after
         * the batch_lock table/DAO flow is wired.
         */
        return true;
    }

    // ---------------------------------------------------------
    // Release lock
    // ---------------------------------------------------------

    @Override
    public void releaseLock(
            String batchId,
            String userId) {

        /*
         * Will be implemented with batch_lock.
         */
    }

    // ---------------------------------------------------------
    // Send to checker
    // ---------------------------------------------------------

    @Override
    public void sendToChecker(
            String batchId,
            String userId) {

        /*
         * Will be implemented when the
         * Maker -> Checker workflow is connected.
         */
    }

    // ---------------------------------------------------------
    // Convert DAO model to domain model
    // ---------------------------------------------------------

    private InwardBatch convert(
            NpciBatchData batch) {

        return new InwardBatch(
                batch.getBatchId(),
                batch.getFileId(),
                batch.getPresentingBankName(),
                batch.getTotalCheques());
    }
}