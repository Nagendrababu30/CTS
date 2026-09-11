package com.iispl.cts.service.outward.checker;

import com.iispl.cts.dao.outward.checker.CheckerBatchDAO;
import com.iispl.cts.model.outward.OutwardCheque;

import java.util.List;

/**
 * ============================================================
 * CHECKER FILE GENERATION SERVICE
 * ============================================================
 *
 * Provides cheque data for Checker report/file generation.
 *
 * Rules:
 *
 * 1. Returns all cheques for the selected batch.
 * 2. CFX uses all batch cheques.
 * 3. CIBF uses all batch cheques.
 * 4. RRF does NOT use this service to determine rejection.
 * 5. RRF rejected-cheque data comes from CheckerReportsService
 *    / CheckerReportsDAO using cheque_processing.
 *
 * ============================================================
 */
public class CheckerFileGenerationService {

    private final CheckerBatchDAO checkerBatchDAO;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public CheckerFileGenerationService() {

        checkerBatchDAO =
                new CheckerBatchDAO();
    }

    // ============================================================
    // GET BATCH CHEQUES
    // ============================================================

    /**
     * Get all cheques belonging to a batch.
     *
     * This method is used by:
     *
     * - CFX generation
     * - CIBF generation
     *
     * It returns ALL cheques, not only rejected cheques.
     *
     * @param batchNumber Batch number
     * @return list of cheques
     */
    public List<OutwardCheque> getBatchCheques(
            String batchNumber) {

        // ========================================================
        // VALIDATE BATCH NUMBER
        // ========================================================

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Batch number is required"
            );
        }

        batchNumber =
                batchNumber.trim();

        // ========================================================
        // GET CHEQUES
        // ========================================================

        List<OutwardCheque> cheques =
                checkerBatchDAO
                        .getChequesByBatchId(
                                batchNumber
                        );

        // ========================================================
        // NEVER RETURN NULL
        // ========================================================

        if (cheques == null) {

            return new java.util.ArrayList<OutwardCheque>();
        }

        return cheques;
    }
}