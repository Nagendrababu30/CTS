package com.iispl.cts.service.outward.checker;

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

        this.chequeDao = new CheckerChequeDAO();
        this.assignmentDao = new CheckerAssignmentDAO();
    }

    // =========================
    // GET CURRENT CHEQUE
    // =========================

    public OutwardCheque getCheque(
            String batchNumber,
            String chequeNumber) {

        return chequeDao.getCheque(
                batchNumber,
                chequeNumber);
    }

    // =========================
    // GET PROCESSING DETAILS
    // =========================

    public ChequeProcessing getChequeProcessing(
            String batchNumber,
            String chequeNumber) {

        return chequeDao.getChequeProcessing(
                batchNumber,
                chequeNumber);
    }

    // =========================
    // CBS VALIDATION
    // =========================

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
                        accountNumber);

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

    // =========================
    // CBS UI MESSAGE
    // =========================

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

    // =========================
    // ACCEPT ALLOWED
    // =========================

    public boolean isAcceptAllowed(
            String cbsResult) {

        return "PASS".equals(cbsResult);
    }

    // =========================
    // RETURN REASONS
    // =========================

    public List<ReturnReason> getReturnReasons() {

        return chequeDao.getReturnReasons();
    }

    // =========================
    // SAVE CHECKER DECISION
    // =========================

    /*
     * ACCEPT:
     *     CBS validation is performed again.
     *
     * REJECT:
     *     Reason is mandatory.
     *
     * SEND_BACK:
     *     Reason is mandatory.
     *
     * checkerRemarks:
     *     Optional checker remarks.
     */
    public boolean saveCheckerDecision(
            String batchNumber,
            String chequeNumber,
            long checkerId,
            String checkerAction,
            Integer checkerReasonId,
            String checkerRemarks) {

        // =========================
        // BASIC VALIDATION
        // =========================

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            return false;
        }

        if (chequeNumber == null
                || chequeNumber.trim().isEmpty()) {

            return false;
        }

        if (checkerAction == null
                || checkerAction.trim().isEmpty()) {

            return false;
        }

        // =========================
        // VERIFY BATCH ASSIGNMENT
        // =========================

        /*
         * Only the Checker who currently owns
         * the batch can make a decision.
         */
        if (!assignmentDao.isBatchAssignedToChecker(
                batchNumber,
                checkerId)) {

            return false;
        }

        // =========================
        // ACCEPT
        // =========================

        /*
         * Re-read the cheque from DB.
         *
         * Never trust the CBS result coming
         * from the UI.
         */
        if ("ACCEPT".equalsIgnoreCase(
                checkerAction)) {

            OutwardCheque cheque =
                    chequeDao.getCheque(
                            batchNumber,
                            chequeNumber);

            if (cheque == null) {

                return false;
            }

            /*
             * Drawer account is the account
             * that is validated against CBS.
             */
            String accountNumber =
                    cheque.getDrawerAccountNumber();

            String cbsResult =
                    validateCbsAccount(
                            accountNumber);

            if (!isAcceptAllowed(
                    cbsResult)) {

                return false;
            }
        }

        // =========================
        // REJECT / SEND BACK
        // =========================

        /*
         * Reason is mandatory for
         * Reject and Send Back.
         */
        if ("REJECT".equalsIgnoreCase(
                checkerAction)
                || "SEND_BACK".equalsIgnoreCase(
                        checkerAction)) {

            if (checkerReasonId == null) {

                return false;
            }
        }

        // =========================
        // SAVE TO DATABASE
        // =========================

        return chequeDao.saveCheckerDecision(
                batchNumber,
                chequeNumber,
                checkerId,
                checkerAction,
                checkerReasonId,
                checkerRemarks);
    }
}