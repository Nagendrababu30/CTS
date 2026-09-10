package com.iispl.cts.service.outward;

import java.util.List;

import com.iispl.cts.dao.outward.OutwardMakerSendCheckerDAO;
import com.iispl.cts.model.outward.OutwardBatch;

public class OutwardMakerSendCheckerService {

    private final OutwardMakerSendCheckerDAO dao;

    public OutwardMakerSendCheckerService() {
        this.dao = new OutwardMakerSendCheckerDAO();
    }

    /**
     * Get batches which are ready for Checker and belong to the current Maker.
     */
    public List<OutwardBatch> getReadyBatches(int userId) throws Exception {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid Maker user ID: " + userId);
        }

        return dao.getBatchesReadyForChecker(userId);
    }

    /**
     * Send batch to Checker.
     */
    public boolean sendToChecker(String batchId, int userId) throws Exception {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID is required.");
        }

        if (userId <= 0) {
            throw new IllegalArgumentException("Maker user ID is required.");
        }

        return dao.sendToChecker(batchId.trim(), userId);
    }
}