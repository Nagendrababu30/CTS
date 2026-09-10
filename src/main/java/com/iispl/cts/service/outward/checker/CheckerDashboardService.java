package com.iispl.cts.service.outward.checker;

import java.util.List;

import com.iispl.cts.dao.outward.checker.CheckerDashboardDAO;
import com.iispl.cts.model.outward.OutwardBatch;

public class CheckerDashboardService {

    private final CheckerDashboardDAO dao;

    public CheckerDashboardService() {
        this.dao = new CheckerDashboardDAO();
    }

    /**
     * Load all batches that are ready for Checker.
     */
    public List<OutwardBatch> getBatches() {

        return dao.getCheckerBatches();
    }


    /**
     * Find a particular batch.
     */
    public OutwardBatch findBatch(String batchNumber) {

        if (batchNumber == null ||
            batchNumber.trim().isEmpty()) {

            return null;
        }

        return dao.findBatch(batchNumber);
    }


    /**
     * Assign/lock a batch for the current Checker.
     *
     * Returns true only when the database successfully
     * assigns the batch to this Checker.
     */
    public boolean assignBatch(
            String batchNumber,
            String checkerUserId) {

        if (batchNumber == null ||
            batchNumber.trim().isEmpty()) {

            return false;
        }

        if (checkerUserId == null ||
            checkerUserId.trim().isEmpty()) {

            return false;
        }

        return dao.assignBatch(
                batchNumber,
                checkerUserId
        );
    }


    /**
     * Check whether the batch is currently available.
     */
    public boolean isBatchAvailable(String batchNumber) {

        OutwardBatch batch = findBatch(batchNumber);

        if (batch == null) {
            return false;
        }

        return "AVAILABLE".equalsIgnoreCase(
                batch.getLockStatus()
        );
    }


    /**
     * Check whether this Checker currently owns the batch.
     */
    public boolean isAssignedToChecker(
            String batchNumber,
            String checkerUserId) {

        OutwardBatch batch = findBatch(batchNumber);

        if (batch == null ||
            checkerUserId == null) {

            return false;
        }

        return checkerUserId.equals(
                batch.getCheckerUserNumber()
        );
    }  
}