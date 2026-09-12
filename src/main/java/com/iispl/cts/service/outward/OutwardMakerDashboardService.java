package com.iispl.cts.service.outward;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.iispl.cts.dao.outward.OutwardMakerDashboardDAO;
import com.iispl.cts.model.outward.ChequeProcessing;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.model.outward.OutwardValidationResult;

public class OutwardMakerDashboardService {

    // =========================================================
    // DAO
    // =========================================================

    private final OutwardMakerDashboardDAO dao;

    // =========================================================
    // VALIDATION SERVICE
    // =========================================================

    private final OutwardValidationService validationService;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public OutwardMakerDashboardService() {

        this.dao = new OutwardMakerDashboardDAO();
        this.validationService = new OutwardValidationService();
    }

    // =========================================================
    // GET ALL BATCHES
    // =========================================================

    public List<OutwardBatch> getBatches()
            throws SQLException {

        return dao.getBatches();
    }

    // =========================================================
    // FIND BATCH
    // =========================================================

    public OutwardBatch findBatch(
            String batchNumber)
            throws SQLException {

        if (isEmpty(batchNumber)) {
            return null;
        }

        List<OutwardBatch> batches =
                dao.getBatches();

        if (batches == null) {
            return null;
        }

        String cleanBatchNumber =
                batchNumber.trim();

        for (OutwardBatch batch : batches) {

            if (batch == null) {
                continue;
            }

            String currentBatchNumber =
                    batch.getBatchNumber();

            if (currentBatchNumber != null
                    && cleanBatchNumber.equalsIgnoreCase(
                            currentBatchNumber.trim())) {

                return batch;
            }
        }

        return null;
    }

    // =========================================================
    // CHECK HOLD BATCH
    //
    // HOLD means Checker has returned one or more cheques
    // back to the original Maker.
    //
    // IMPORTANT:
    // This method ONLY checks the batch status.
    // It does not modify the batch.
    // =========================================================

    public boolean isHoldBatch(
            String batchNumber)
            throws SQLException {

        if (isEmpty(batchNumber)) {
            return false;
        }

        OutwardBatch batch =
                findBatch(
                        batchNumber.trim()
                );

        if (batch == null) {
            return false;
        }

        String batchStatus =
                batch.getBatchStatus();

        return batchStatus != null
                && "HOLD".equalsIgnoreCase(
                        batchStatus.trim()
                );
    }

    // =========================================================
    // GET RETURNED CHEQUES
    //
    // Only cheques having:
    //
    //     SENT_BACK_TO_MAKER
    //
    // are returned.
    //
    // Example:
    //
    // Original batch:
    //     10 cheques
    //
    // Checker returns:
    //     2 cheques
    //
    // This method returns only those 2 returned cheques.
    //
    // IMPORTANT:
    // outward_batch.cheque_count is NOT changed.
    // =========================================================

    public List<OutwardCheque> getReturnedCheques(
            String batchNumber)
            throws SQLException {

        List<OutwardCheque> returnedCheques =
                new ArrayList<>();

        if (isEmpty(batchNumber)) {
            return returnedCheques;
        }

        List<OutwardCheque> cheques =
                dao.getCheques(
                        batchNumber.trim()
                );

        if (cheques == null
                || cheques.isEmpty()) {

            return returnedCheques;
        }

        for (OutwardCheque cheque : cheques) {

            if (cheque == null) {
                continue;
            }

            String chequeStatus =
                    cheque.getChequeStatus();

            if (chequeStatus != null
                    && "SENT_BACK_TO_MAKER".equalsIgnoreCase(
                            chequeStatus.trim()
                    )) {

                returnedCheques.add(
                        cheque
                );
            }
        }

        return returnedCheques;
    }

    // =========================================================
    // ASSIGN + VALIDATE
    //
    // FLOW:
    //
    // Maker clicks Open
    //        ↓
    // Assign batch
    //        ↓
    // Get cheques
    //        ↓
    // Validate MICR
    //        ↓
    // Update cheque status
    //        ↓
    // Update batch status
    //        ↓
    // Return validation result
    //        ↓
    // Controller opens required module
    // =========================================================

    public OutwardValidationResult assignAndValidate(
            String batchNumber,
            String userId)
            throws SQLException {

        // =====================================================
        // VALIDATE INPUT
        // =====================================================

        if (isEmpty(batchNumber)
                || isEmpty(userId)) {

            return null;
        }

        String cleanBatchNumber =
                batchNumber.trim();

        String cleanUserId =
                userId.trim();

        // =====================================================
        // STEP 1: ASSIGN BATCH
        // =====================================================

        boolean assigned =
                dao.assignBatch(
                        cleanBatchNumber,
                        cleanUserId
                );

        /*
         * If assignment failed,
         * controller treats it as locked/unavailable.
         */

        if (!assigned) {
            return null;
        }

        // =====================================================
        // STEP 2: GET CHEQUES
        // =====================================================

        List<OutwardCheque> cheques =
                dao.getCheques(
                        cleanBatchNumber
                );

        // =====================================================
        // STEP 3: VALIDATE BATCH
        // =====================================================

        OutwardValidationResult result =
                validationService.validate(
                        cheques
                );

        // =====================================================
        // STEP 4: UPDATE INDIVIDUAL CHEQUE STATUS
        // =====================================================

        if (cheques != null) {

            for (OutwardCheque cheque : cheques) {

                if (cheque == null) {
                    continue;
                }

                String errorType =
                        validationService.getMicrErrorType(
                                cheque
                        );

                // -------------------------------------------------
                // MICR ERROR
                // -------------------------------------------------

                if (errorType != null) {

                    dao.updateChequeStatus(
                            cleanBatchNumber,
                            cheque.getChequeNumber(),
                            "MICR_ERROR"
                    );

                }

                // -------------------------------------------------
                // MICR VALID
                // -------------------------------------------------

                else {

                    dao.updateChequeStatus(
                            cleanBatchNumber,
                            cheque.getChequeNumber(),
                            "MICR_VERIFIED"
                    );
                }
            }
        }

        // =====================================================
        // STEP 5: UPDATE BATCH STATUS
        // =====================================================

        if (result.getMicrErrors() > 0) {

            dao.updateBatchStatus(
                    cleanBatchNumber,
                    "MICR_REPAIR"
            );

        } else {

            dao.updateBatchStatus(
                    cleanBatchNumber,
                    "READY_FOR_CHECKER"
            );
        }

        // =====================================================
        // STEP 6: RETURN VALIDATION RESULT
        // =====================================================

        return result;
    }

    // =========================================================
    // GET CHEQUES
    // =========================================================

    public List<OutwardCheque> getCheques(
            String batchNumber)
            throws SQLException {

        if (isEmpty(batchNumber)) {
            return null;
        }

        return dao.getCheques(
                batchNumber.trim()
        );
    }

    // =========================================================
    // CHECK BATCH EXISTS
    // =========================================================

    public boolean isBatchValid(
            String batchNumber)
            throws SQLException {

        if (isEmpty(batchNumber)) {
            return false;
        }

        return dao.isBatchValid(
                batchNumber.trim()
        );
    }

    // =========================================================
    // COMPLETE BATCH
    // =========================================================

    public void completeBatch(
            String batchNumber)
            throws SQLException {

        if (isEmpty(batchNumber)) {
            return;
        }

        /*
         * DAO method is void,
         * therefore this service method is also void.
         */

        dao.updateBatchIfCompleted(
                batchNumber.trim()
        );
    }

    // =========================================================
    // GET CHEQUE PROCESSING
    //
    // Used by HOLD batches to determine:
    //
    //     Checker Action
    //     Checker Reason Code
    //
    // This allows the Maker controller to decide whether
    // the returned cheque should open in:
    //
    //     MICR Repair
    //          OR
    //     Data Entry
    // =========================================================

    public ChequeProcessing getChequeProcessing(
            String batchNumber,
            String chequeNumber)
            throws SQLException {

        if (isEmpty(batchNumber)
                || isEmpty(chequeNumber)) {

            return null;
        }

        return dao.getChequeProcessing(
                batchNumber.trim(),
                chequeNumber.trim()
        );
    }

    // =========================================================
    // EMPTY CHECK
    // =========================================================

    private boolean isEmpty(
            String value) {

        return value == null
                || value.trim().isEmpty();
    }
}