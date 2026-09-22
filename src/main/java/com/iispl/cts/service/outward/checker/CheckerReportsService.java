package com.iispl.cts.service.outward.checker;

import java.util.ArrayList;
import java.util.List;

import com.iispl.cts.dao.outward.checker.CheckerReportsDAO;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;

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

    public int getRejectedChequeCount(
            String batchNumber) {

        validateBatchNumber(batchNumber);

        return dao.getRejectedChequeCount(
                batchNumber.trim()
        );
    }

    // ============================================================
    // GET RRF CHEQUES
    // ============================================================

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
    // GET VALID CHEQUES
    // ============================================================

    public List<OutwardCheque> getValidCheques(
            String batchNumber) {

        validateBatchNumber(batchNumber);

        List<OutwardCheque> allCheques =
                getBatchCheques(batchNumber);

        List<OutwardCheque> validCheques =
                new ArrayList<OutwardCheque>();

        for (OutwardCheque cheque : allCheques) {

            if (cheque != null &&
                    "CHECKER_ACCEPTED".equalsIgnoreCase(
                            cheque.getChequeStatus())) {

                validCheques.add(cheque);
            }
        }

        return validCheques;
    }

    // ============================================================
    // SAVE NPCI SUBMISSION
    // ============================================================

    public boolean saveNPCISubmission(
            String batchNumber,
            int validChequeCount,
            int invalidChequeCount,
            String validXmlPath) {

        validateBatchNumber(batchNumber);

        if (validChequeCount <= 0) {

            return false;
        }

        if (validXmlPath == null ||
                validXmlPath.trim().isEmpty()) {

            return false;
        }

        return dao.saveNPCISubmission(
                batchNumber.trim(),
                validChequeCount,
                invalidChequeCount,
                validXmlPath.trim()
        );
    }

    // ============================================================
    // CHECK NPCI READINESS
    // ============================================================

    public boolean isBatchReadyForNPCI(
            String batchNumber) {

        validateBatchNumber(batchNumber);

        return dao.isBatchReadyForNPCI(
                batchNumber.trim()
        );
    }

    // ============================================================
    // MARK NPCI SENT
    // ============================================================

    public boolean markBatchAsNPCISent(
            String batchNumber) {

        validateBatchNumber(batchNumber);

        return dao.markBatchAsNPCISent(
                batchNumber.trim()
        );
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