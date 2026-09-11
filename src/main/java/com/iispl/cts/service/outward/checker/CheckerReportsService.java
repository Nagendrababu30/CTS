package com.iispl.cts.service.outward.checker;

import com.iispl.cts.dao.outward.checker.CheckerReportsDAO;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * CHECKER REPORTS SERVICE
 * ============================================================
 *
 * Handles the business logic required by the Checker Reports
 * screen.
 *
 * Report rules:
 *
 * 1. Batch must be selected first.
 * 2. CFX is available for every batch.
 * 3. CIBF is available for every batch.
 * 4. RRF is available only when the batch contains rejected
 *    cheque(s).
 * 5. Rejected cheque information is obtained from
 *    cheque_processing.
 * 6. RRF must never be generated with an empty rejected-cheque
 *    list.
 *
 * ============================================================
 */
public class CheckerReportsService {

    private final CheckerReportsDAO dao;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public CheckerReportsService() {

        dao =
                new CheckerReportsDAO();
    }

    // ============================================================
    // GET AVAILABLE BATCHES
    // ============================================================

    /**
     * Get batches displayed in the Checker Reports screen.
     *
     * @return available batches
     */
    public List<OutwardBatch> getCheckerCompletedBatches() {

        List<OutwardBatch> batches =
                dao.getCheckerCompletedBatches();

        if (batches == null) {

            return new ArrayList<OutwardBatch>();
        }

        return batches;
    }

    // ============================================================
    // GET BATCH BY NUMBER
    // ============================================================

    /**
     * Get a particular batch using its batch number.
     *
     * @param batchNumber batch number
     * @return batch or null
     */
    public OutwardBatch getBatchByNumber(
            String batchNumber) {

        validateBatchNumber(batchNumber);

        return dao.getBatchByNumber(
                batchNumber.trim()
        );
    }

    // ============================================================
    // GET ALL CHEQUES
    // ============================================================

    /**
     * Get all cheques belonging to the selected batch.
     *
     * Used by:
     *
     * - CFX
     * - CIBF
     *
     * This method returns ALL cheques.
     *
     * @param batchNumber batch number
     * @return all batch cheques
     */
    public List<OutwardCheque> getBatchCheques(
            String batchNumber) {

        validateBatchNumber(batchNumber);

        List<OutwardCheque> cheques =
                dao.getBatchCheques(
                        batchNumber.trim()
                );

        if (cheques == null) {

            return new ArrayList<OutwardCheque>();
        }

        return cheques;
    }

    // ============================================================
    // GET REJECTED CHEQUES
    // ============================================================

    /**
     * Get only rejected cheques for a batch.
     *
     * IMPORTANT:
     *
     * Rejection is determined from:
     *
     *     cheque_processing.checker_action = 'REJECT'
     *
     * It is NOT determined only from:
     *
     *     outward_cheque.return_reason_id
     *
     * These rejected cheques are the records that can be
     * supplied to RRF generation.
     *
     * @param batchNumber batch number
     * @return rejected cheques
     */
    public List<OutwardCheque> getRejectedCheques(
            String batchNumber) {

        validateBatchNumber(batchNumber);

        List<OutwardCheque> rejectedCheques =
                dao.getRejectedCheques(
                        batchNumber.trim()
                );

        if (rejectedCheques == null) {

            return new ArrayList<OutwardCheque>();
        }

        return rejectedCheques;
    }

    // ============================================================
    // CHECK RRF AVAILABILITY
    // ============================================================

    /**
     * Check whether RRF is available for a batch.
     *
     * RRF is available only when at least one rejected cheque
     * exists in cheque_processing.
     *
     * @param batchNumber batch number
     * @return true if RRF is available
     */
    public boolean isRrfAvailable(
            String batchNumber) {

        validateBatchNumber(batchNumber);

        return dao.hasRejectedCheques(
                batchNumber.trim()
        );
    }

    // ============================================================
    // GET REJECTED CHEQUE COUNT
    // ============================================================

    /**
     * Get number of rejected cheques for a batch.
     *
     * @param batchNumber batch number
     * @return rejected cheque count
     */
    public int getRejectedChequeCount(
            String batchNumber) {

        validateBatchNumber(batchNumber);

        return dao.getRejectedChequeCount(
                batchNumber.trim()
        );
    }

    // ============================================================
    // VALIDATE RRF AVAILABILITY
    // ============================================================

    /**
     * Validates that RRF can be generated.
     *
     * This provides a service-layer protection against generating
     * an empty RRF.
     *
     * @param batchNumber batch number
     * @return rejected cheques that should go into RRF
     */
    public List<OutwardCheque> getRrfCheques(
            String batchNumber) {

        validateBatchNumber(batchNumber);

        List<OutwardCheque> rejectedCheques =
                getRejectedCheques(
                        batchNumber
                );

        if (rejectedCheques == null ||
                rejectedCheques.isEmpty()) {

            throw new IllegalStateException(
                    "RRF not available for this batch"
            );
        }

        return rejectedCheques;
    }

    // ============================================================
    // CHECK BATCH EXISTS
    // ============================================================

    /**
     * Check whether a batch exists.
     *
     * @param batchNumber batch number
     * @return true if batch exists
     */
    public boolean batchExists(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return false;
        }

        return dao.getBatchByNumber(
                batchNumber.trim()
        ) != null;
    }

    // ============================================================
    // VALIDATE BATCH NUMBER
    // ============================================================

    private void validateBatchNumber(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Batch number is required"
            );
        }
    }
}