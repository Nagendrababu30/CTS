package com.iispl.cts.service.outward.checker;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.iispl.cts.dao.outward.checker.CheckerAssignmentDAO;
import com.iispl.cts.dao.outward.checker.CheckerChequeDAO;
import com.iispl.cts.model.outward.ChequeProcessing;
import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.model.outward.ReturnReason;

public class CheckerProcessingService {

    private final CheckerChequeDAO chequeDao;

    private final CheckerAssignmentDAO assignmentDao;


    public CheckerProcessingService() {

        this.chequeDao =
                new CheckerChequeDAO();

        this.assignmentDao =
                new CheckerAssignmentDAO();
    }


    // ============================================================
    // GET CURRENT CHEQUE
    // ============================================================

    public OutwardCheque getCheque(
            String batchNumber,
            String chequeNumber) {

        return chequeDao.getCheque(
                batchNumber,
                chequeNumber);
    }


    // ============================================================
    // GET PROCESSING DETAILS
    // ============================================================

    public ChequeProcessing getChequeProcessing(
            String batchNumber,
            String chequeNumber) {

        return chequeDao.getChequeProcessing(
                batchNumber,
                chequeNumber);
    }


    // ============================================================
    // CBS VALIDATION
    // ============================================================

    /*
     * Validate drawer account in CBS.
     *
     * Possible results:
     *
     * ACCOUNT_NOT_FOUND
     * ACCOUNT_INACTIVE
     * PASS
     */

    public String validateCbsAccount(
            String accountNumber) {

        if (accountNumber == null
                || accountNumber.trim().isEmpty()) {

            return "ACCOUNT_NOT_FOUND";
        }

        Map<String, String> account =
                chequeDao.getCbsAccount(
                        accountNumber.trim());

        if (account == null) {

            return "ACCOUNT_NOT_FOUND";
        }

        String accountStatus =
                account.get("accountStatus");

        if (!"ACTIVE".equalsIgnoreCase(
                accountStatus)) {

            return "ACCOUNT_INACTIVE";
        }

        return "PASS";
    }


    // ============================================================
    // CBS UI MESSAGE
    // ============================================================

    public String getCbsValidationMessage(
            String cbsResult) {

        if ("ACCOUNT_NOT_FOUND".equals(
                cbsResult)) {

            return "Drawer account does not exist in CBS records.";
        }

        if ("ACCOUNT_INACTIVE".equals(
                cbsResult)) {

            return "Drawer account is inactive.";
        }

        if ("PASS".equals(cbsResult)) {

            return "Drawer account exists and is active.";
        }

        return "CBS validation failed.";
    }


    // ============================================================
    // ACCEPT ALLOWED
    // ============================================================

    public boolean isAcceptAllowed(
            String cbsResult) {

        return "PASS".equals(cbsResult);
    }


    // ============================================================
    // RETURN REASONS
    // ============================================================

    public List<ReturnReason> getReturnReasons(
            String reasonType) {

        if (reasonType == null
                || reasonType.trim().isEmpty()) {

            return Collections.emptyList();
        }

        return chequeDao.getReturnReasons(
                reasonType.trim().toUpperCase());
    }


    // ============================================================
    // SAVE CHECKER DECISION
    // ============================================================

    /*
     * ACCEPT:
     *
     *     CBS must PASS.
     *     No reason.
     *     Final decision.
     *
     * REJECT:
     *
     *     Reason mandatory.
     *     Final decision.
     *
     * SEND_BACK:
     *
     *     CBS must PASS.
     *     Reason mandatory.
     *     Not a final Checker decision.
     *
     * Important:
     *
     *     ACCEPT / REJECT cannot be performed again on
     *     an already-final cheque.
     *
     *     SEND_BACK can be followed by another Checker
     *     decision after Maker rework and resubmission.
     */

    public boolean saveCheckerDecision(
            String batchNumber,
            String chequeNumber,
            long checkerId,
            String checkerAction,
            String checkerReasonCode,
            String checkerRemarks) {

        // ========================================================
        // BASIC VALIDATION
        // ========================================================

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            return false;
        }

        if (chequeNumber == null
                || chequeNumber.trim().isEmpty()) {

            return false;
        }

        if (checkerId <= 0) {

            return false;
        }

        if (checkerAction == null
                || checkerAction.trim().isEmpty()) {

            return false;
        }

        batchNumber =
                batchNumber.trim();

        chequeNumber =
                chequeNumber.trim();

        checkerAction =
                checkerAction.trim().toUpperCase();


        // ========================================================
        // VALID ACTION
        // ========================================================

        if (!"ACCEPT".equals(checkerAction)
                && !"REJECT".equals(checkerAction)
                && !"SEND_BACK".equals(checkerAction)) {

            return false;
        }


        // ========================================================
        // VERIFY BATCH ASSIGNMENT
        // ========================================================

        /*
         * Only the Checker who currently owns
         * the batch can make a decision.
         */

        if (!assignmentDao.isBatchAssignedToChecker(
                batchNumber,
                checkerId)) {

            return false;
        }


        // ========================================================
        // GET CURRENT CHEQUE
        // ========================================================

        OutwardCheque cheque =
                chequeDao.getCheque(
                        batchNumber,
                        chequeNumber);

        if (cheque == null) {

            return false;
        }


        // ========================================================
        // GET CURRENT PROCESSING STATE
        // ========================================================

        ChequeProcessing processing =
                chequeDao.getChequeProcessing(
                        batchNumber,
                        chequeNumber);

        if (processing == null) {

            return false;
        }


        // ========================================================
        // PREVENT DUPLICATE FINAL DECISION
        // ========================================================

        /*
         * ACCEPT and REJECT are final Checker decisions.
         *
         * Once either has been saved, the same cheque must
         * not be processed again.
         *
         * SEND_BACK is intentionally not treated as final,
         * because Maker can rework and submit the cheque again.
         */

        String existingCheckerAction =
                processing.getCheckerAction();

        if (existingCheckerAction != null) {

            existingCheckerAction =
                    existingCheckerAction.trim().toUpperCase();

            if (("ACCEPT".equals(existingCheckerAction)
                    || "REJECT".equals(existingCheckerAction))) {

                return false;
            }
        }


        // ========================================================
        // CBS VALIDATION
        // ========================================================

        /*
         * ACCEPT and SEND_BACK require successful CBS
         * validation.
         *
         * REJECT does not require CBS PASS because the
         * Checker may reject the cheque for a valid reason
         * even when CBS validation fails.
         */

        if ("ACCEPT".equals(checkerAction)
                || "SEND_BACK".equals(checkerAction)) {

            String accountNumber =
                    cheque.getDrawerAccountNumber();

            String cbsResult =
                    validateCbsAccount(
                            accountNumber);

            if (!"PASS".equals(cbsResult)) {

                return false;
            }
        }


        // ========================================================
        // REASON VALIDATION
        // ========================================================

        if ("REJECT".equals(checkerAction)
                || "SEND_BACK".equals(checkerAction)) {

            if (checkerReasonCode == null
                    || checkerReasonCode.trim().isEmpty()) {

                return false;
            }

            checkerReasonCode =
                    checkerReasonCode.trim();
        }


        // ========================================================
        // ACCEPT MUST NOT HAVE A REASON
        // ========================================================

        if ("ACCEPT".equals(checkerAction)) {

            checkerReasonCode = null;
        }


        // ========================================================
        // NORMALIZE REMARKS
        // ========================================================

        if (checkerRemarks != null) {

            checkerRemarks =
                    checkerRemarks.trim();

            if (checkerRemarks.isEmpty()) {

                checkerRemarks = null;
            }
        }


        // ========================================================
        // SAVE TO DATABASE
        // ========================================================

        return chequeDao.saveCheckerDecision(
                batchNumber,
                chequeNumber,
                checkerId,
                checkerAction,
                checkerReasonCode,
                checkerRemarks);
    }
}
