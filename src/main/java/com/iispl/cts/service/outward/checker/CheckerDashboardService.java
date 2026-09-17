package com.iispl.cts.service.outward.checker;

import java.util.Collections;
import java.util.List;

import com.iispl.cts.dao.outward.checker.CheckerAssignmentDAO;
import com.iispl.cts.dao.outward.checker.CheckerDashboardDAO;
import com.iispl.cts.model.outward.OutwardBatch;

public class CheckerDashboardService {

    private final CheckerDashboardDAO dao;
    private final CheckerAssignmentDAO assignmentDao;

    public CheckerDashboardService() {

        this.dao = new CheckerDashboardDAO();
        this.assignmentDao = new CheckerAssignmentDAO();
    }

    // ============================================================
    // GET CHECKER DASHBOARD BATCHES
    // ============================================================
    public List<OutwardBatch> getBatches(String checkerUserId) {

        if (checkerUserId == null ||
                checkerUserId.trim().isEmpty()) {

            return Collections.emptyList();
        }

        try {
            return dao.getCheckerBatches(
                    checkerUserId.trim());

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    // ============================================================
    // CHECK RE-VERIFIED CHEQUES
    // ============================================================

    public boolean hasReVerifiedCheques(
            String batchNumber,
            String checkerUserId) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()
                || checkerUserId == null
                || checkerUserId.trim().isEmpty()) {

            return false;
        }

        try {

            return dao.hasReVerifiedCheques(
                    batchNumber.trim(),
                    checkerUserId.trim());

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    // ============================================================
    // GET RE-VERIFIED CHEQUE COUNT
    // ============================================================

    public int getReVerifiedChequeCount(
            String batchNumber,
            String checkerUserId) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()
                || checkerUserId == null
                || checkerUserId.trim().isEmpty()) {

            return 0;
        }

        try {

            return dao.getReVerifiedChequeCount(
                    batchNumber.trim(),
                    checkerUserId.trim());

        } catch (Exception e) {

            e.printStackTrace();

            return 0;
        }
    }

    // ============================================================
    // FIND BATCH
    // ============================================================

    public OutwardBatch findBatch(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return null;
        }

        try {

            return dao.findBatch(
                    batchNumber.trim());

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }

    // ============================================================
    // ASSIGN BATCH TO CHECKER
    // ============================================================

    /*
     * The actual Checker batch assignment is handled by
     * CheckerAssignmentDAO.takeBatch().
     *
     * This method:
     *
     * 1. Locks the batch row.
     * 2. Checks that the batch is SUBMITTED_TO_CHECKER.
     * 3. Checks that no other active Checker assignment exists.
     * 4. Creates the Checker assignment.
     * 5. Changes batch status to CHECKER_PROCESSING.
     *
     * This keeps Dashboard assignment and Queue assignment
     * on the same assignment logic.
     */

    public boolean assignBatch(
            String batchNumber,
            long checkerUserId) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return false;
        }

        try {

            return assignmentDao.takeBatch(
                    batchNumber.trim(),
                    checkerUserId);

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    // ============================================================
    // CHECK BATCH AVAILABLE
    // ============================================================

    public boolean isBatchAvailable(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return false;
        }

        try {

            OutwardBatch batch =
                    findBatch(batchNumber);

            if (batch == null) {

                return false;
            }

            return "AVAILABLE".equalsIgnoreCase(
                    batch.getLockStatus());

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    // ============================================================
    // CHECK ASSIGNED TO CURRENT CHECKER
    // ============================================================

    public boolean isAssignedToChecker(
            String batchNumber,
            String checkerUserId) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()
                || checkerUserId == null
                || checkerUserId.trim().isEmpty()) {

            return false;
        }

        try {

            long checkerId =
                    Long.parseLong(
                            checkerUserId.trim());

            return assignmentDao.isBatchAssignedToChecker(
                    batchNumber.trim(),
                    checkerId);

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    // ============================================================
    // GET RE-VERIFY BATCHES
    // ============================================================

    public List<OutwardBatch> getReVerifyBatches(
            String checkerUserId) {

        if (checkerUserId == null ||
                checkerUserId.trim().isEmpty()) {

            return Collections.emptyList();
        }

        try {

            return dao.getReVerifyBatches(
                    checkerUserId.trim());

        } catch (Exception e) {

            e.printStackTrace();

            return Collections.emptyList();
        }
    }

    // ============================================================
    // CHECK PENDING MAKER CHEQUES
    // ============================================================

    /**
     * Checks whether any cheque sent back by the current Checker
     * is still pending with the Maker.
     *
     * This method does NOT use outward_batch.batch_status.
     *
     * A pending Maker cheque does not prevent another already
     * corrected RE_VERIFIED cheque from being re-verified.
     *
     * The Controller checks RE_VERIFIED eligibility before using
     * this method.
     */

    public boolean hasPendingMakerCheques(
            String batchNumber,
            String checkerUserId) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()
                || checkerUserId == null
                || checkerUserId.trim().isEmpty()) {

            return false;
        }

        try {

            return dao.hasPendingMakerCheques(
                    batchNumber.trim(),
                    checkerUserId.trim());

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    // ============================================================
    // GET RE-VERIFIED CHEQUE NUMBERS
    // ============================================================

    public List<String> getReVerifiedChequeNumbers(
            String batchNumber,
            String checkerUserId) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()
                || checkerUserId == null
                || checkerUserId.trim().isEmpty()) {

            return Collections.emptyList();
        }

        try {

            return dao.getReVerifiedChequeNumbers(
                    batchNumber.trim(),
                    checkerUserId.trim());

        } catch (Exception e) {

            e.printStackTrace();

            return Collections.emptyList();
        }
    }
}