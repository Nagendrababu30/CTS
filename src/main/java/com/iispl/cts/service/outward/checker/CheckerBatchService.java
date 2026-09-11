package com.iispl.cts.service.outward.checker;

import java.util.List;

import com.iispl.cts.dao.outward.checker.CheckerAssignmentDAO;
import com.iispl.cts.dao.outward.checker.CheckerBatchDAO;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;

public class CheckerBatchService {

    private final CheckerBatchDAO batchDao;
    private final CheckerAssignmentDAO assignmentDao;

    public CheckerBatchService() {

        this.batchDao = new CheckerBatchDAO();
        this.assignmentDao = new CheckerAssignmentDAO();
    }

    /*
     * Get batches available for Checker.
     */
    public List<OutwardBatch> getCheckerBatches(
            String checkerUserId,
            String searchText,
            int pageNo,
            int pageSize) {

        int offset = pageNo * pageSize;

        return batchDao.getCheckerBatches(
                checkerUserId,
                searchText,
                pageSize,
                offset);
    }

    /*
     * Find a batch by batch number.
     */
    public OutwardBatch findBatch(String batchNumber) {

        return batchDao.getBatchByNumber(batchNumber);
    }

    /*
     * Get all cheques belonging to a batch.
     */
    public List<OutwardCheque> getChequesByBatchNumber(
            String batchNumber) {

        return batchDao.getChequesByBatchNumber(batchNumber);
    }

    /*
     * Checker takes a batch.
     *
     * A new Checker assignment is created.
     * Existing Maker assignment is not changed.
     */
    public boolean assignBatch(
            String batchNumber,
            long checkerUserId) {

        return assignmentDao.takeBatch(
                batchNumber,
                checkerUserId);
    }

    /*
     * Check whether any Checker currently has the batch.
     */
    public boolean isBatchAssigned(String batchNumber) {

        return assignmentDao.isBatchAssigned(
                batchNumber);
    }

    /*
     * Check whether this Checker currently has the batch.
     */
    public boolean isAssignedToChecker(
            String batchNumber,
            long checkerUserId) {

        return assignmentDao.isBatchAssignedToChecker(
                batchNumber,
                checkerUserId);
    }

    /*
     * Complete the current Checker assignment.
     *
     * Assignment history remains in the database.
     */
    public boolean completeBatch(
            String batchNumber,
            long checkerUserId) {

        return assignmentDao.completeBatch(
                batchNumber,
                checkerUserId);
    }

    /*
     * Get the current Checker assignment ID.
     */
    public Long getAssignmentId(
            String batchNumber,
            long checkerUserId) {

        return assignmentDao.getAssignmentId(
                batchNumber,
                checkerUserId);
    }

    /*
     * Get total number of Checker batches.
     */
    public int getCheckerBatchCount(
            String checkerUserId,
            String searchText) {

        return batchDao.getCheckerBatchCount(
                checkerUserId,
                searchText);
    }
    
 // ============================================
    // VERIFY DRAWER ACCOUNT
    // ============================================

    public boolean verifyAccount(
            String accountNumber) {

        if (accountNumber == null
                || accountNumber.trim().isEmpty()) {

            return false;
        }

        return batchDao.accountExists(
                accountNumber.trim());
    }
}