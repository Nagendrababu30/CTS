package com.iispl.cts.service.outward.checker;

import java.time.LocalDate;
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

        this.chequeDao = new CheckerChequeDAO();
        this.assignmentDao = new CheckerAssignmentDAO();
    }

    // ============================================================
    // GET CURRENT CHEQUE
    // ============================================================

    public OutwardCheque getCheque(
            String batchNumber,
            String chequeNumber) {

        if (batchNumber == null
                || batchNumber.trim().isEmpty()
                || chequeNumber == null
                || chequeNumber.trim().isEmpty()) {

            return null;
        }

        return chequeDao.getCheque(
                batchNumber.trim(),
                chequeNumber.trim());
    }

    // ============================================================
    // GET PROCESSING DETAILS
    // ============================================================

    public ChequeProcessing getChequeProcessing(
            String batchNumber,
            String chequeNumber) {

        if (batchNumber == null
                || batchNumber.trim().isEmpty()
                || chequeNumber == null
                || chequeNumber.trim().isEmpty()) {

            return null;
        }

        return chequeDao.getChequeProcessing(
                batchNumber.trim(),
                chequeNumber.trim());
    }

    // ============================================================
    // GET RE-VERIFIED CHEQUES
    // ============================================================
    /*
     * Re-Verify is based ONLY on:
     *
     * 1. Same batch
     * 2. Same/original Checker
     * 3. Previous Checker action = SEND_BACK
     * 4. Cheque status = RE_VERIFIED
     *
     * Batch status is deliberately NOT checked.
     */

    public List<OutwardCheque> getReVerifiedCheques(
            String batchNumber,
            long checkerUserId) {

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            return Collections.emptyList();
        }

        if (checkerUserId <= 0) {

            return Collections.emptyList();
        }

        return chequeDao.getReVerifiedCheques(
                batchNumber.trim(),
                checkerUserId);
    }

    // ============================================================
    // MAKER REJECTION INFORMATION
    // ============================================================

    public boolean isMakerRejected(
            String batchNumber,
            String chequeNumber) {

        ChequeProcessing processing =
                getChequeProcessing(
                        batchNumber,
                        chequeNumber);

        if (processing == null) {

            return false;
        }

        return "REJECT_REQUEST".equalsIgnoreCase(
                processing.getMakerAction());
    }

    // ============================================================
    // START CHECKER PROCESSING
    // ============================================================

    public boolean startCheckerProcessing(
            String batchNumber,
            String chequeNumber) {

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            return false;
        }

        if (chequeNumber == null
                || chequeNumber.trim().isEmpty()) {

            return false;
        }

        return chequeDao.startCheckerProcessing(
                batchNumber.trim(),
                chequeNumber.trim());
    }

    // ============================================================
    // GET MAKER REASON CODE
    // ============================================================

    public String getMakerReasonCode(
            String batchNumber,
            String chequeNumber) {

        ChequeProcessing processing =
                getChequeProcessing(
                        batchNumber,
                        chequeNumber);

        if (processing == null) {

            return null;
        }

        if (!"REJECT_REQUEST".equalsIgnoreCase(
                processing.getMakerAction())) {

            return null;
        }

        return processing.getMakerReasonCode();
    }

    // ============================================================
    // CBS ACCOUNT VALIDATION
    // ============================================================

    /*
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
    // CHEQUE DATE VALIDATION
    // ============================================================

    /*
     * Cheque date:
     *
     * - Must not be older than 3 months
     * - Must not be future/post-dated
     */

    public String validateChequeDate(
            LocalDate chequeDate) {

        if (chequeDate == null) {

            return "CHEQUE_DATE_EXPIRED";
        }

        LocalDate today =
                LocalDate.now();

        LocalDate minimumDate =
                today.minusMonths(3);

        if (chequeDate.isBefore(minimumDate)) {

            return "CHEQUE_DATE_EXPIRED";
        }

        if (chequeDate.isAfter(today)) {

            return "CHEQUE_DATE_POST_DATED";
        }

        return "PASS";
    }

    // ============================================================
    // CBS / CHEQUE VALIDATION MESSAGE
    // ============================================================

    public String getCbsValidationMessage(
            String validationResult) {

        if ("ACCOUNT_NOT_FOUND".equals(
                validationResult)) {

            return "Drawer account does not exist in CBS records.";
        }

        if ("ACCOUNT_INACTIVE".equals(
                validationResult)) {

            return "Drawer account is inactive.";
        }

        if ("CHEQUE_DATE_EXPIRED".equals(
                validationResult)) {

            return "Cheque date is older than 3 months.";
        }

        if ("CHEQUE_DATE_POST_DATED".equals(
                validationResult)) {

            return "Post-dated cheque is not allowed.";
        }

        if ("PASS".equals(validationResult)) {

            return "Validation successful.";
        }

        return "Validation failed.";
    }

    // ============================================================
    // ACCEPT ALLOWED
    // ============================================================

    public boolean isAcceptAllowed(
            String validationResult) {

        return "PASS".equals(validationResult);
    }

    // ============================================================
    // GET CHECKER RETURN / REJECTION REASONS
    // ============================================================

    /*
     * reasonType must be:
     *
     * SEND_BACK
     * REJECT
     */

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
     * - CBS must PASS
     * - Cheque date must PASS
     * - No reason
     *
     * REJECT:
     * - Reason mandatory
     * - Final decision
     * - CBS does not have to PASS
     *
     * SEND_BACK:
     * - CBS must PASS
     * - Cheque date must PASS
     * - Reason mandatory
     * - Not final
     *
     * Re-verification:
     * - Same Checker
     * - Same batch
     * - Previous action SEND_BACK
     * - Maker corrected cheque
     * - Cheque status RE_VERIFIED
     */

    public boolean saveCheckerDecision(
            String batchNumber,
            String chequeNumber,
            long checkerId,
            String checkerAction,
            String checkerReasonCode,
            String checkerRemarks) {

        // --------------------------------------------------------
        // BASIC VALIDATION
        // --------------------------------------------------------

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

        // --------------------------------------------------------
        // VALID ACTION
        // --------------------------------------------------------

        if (!"ACCEPT".equals(checkerAction)
                && !"REJECT".equals(checkerAction)
                && !"SEND_BACK".equals(checkerAction)) {

            return false;
        }

        // --------------------------------------------------------
        // CHECK CURRENT BATCH ASSIGNMENT
        // --------------------------------------------------------

        if (!assignmentDao.isBatchAssignedToChecker(
                batchNumber,
                checkerId)) {

            return false;
        }

        // --------------------------------------------------------
        // GET CHEQUE
        // --------------------------------------------------------

        OutwardCheque cheque =
                chequeDao.getCheque(
                        batchNumber,
                        chequeNumber);

        if (cheque == null) {

            return false;
        }

        // --------------------------------------------------------
        // GET PROCESSING RECORD
        // --------------------------------------------------------

        ChequeProcessing processing =
                chequeDao.getChequeProcessing(
                        batchNumber,
                        chequeNumber);

        if (processing == null) {

            return false;
        }

        // --------------------------------------------------------
        // PREVENT DUPLICATE FINAL DECISION
        // --------------------------------------------------------

        String existingCheckerAction =
                processing.getCheckerAction();

        if (existingCheckerAction != null) {

            existingCheckerAction =
                    existingCheckerAction
                            .trim()
                            .toUpperCase();

            if ("ACCEPT".equals(existingCheckerAction)
                    || "REJECT".equals(existingCheckerAction)) {

                return false;
            }
        }

        // --------------------------------------------------------
        // ACCEPT / SEND_BACK VALIDATION
        // --------------------------------------------------------

        if ("ACCEPT".equals(checkerAction)
                || "SEND_BACK".equals(checkerAction)) {

            String cbsResult =
                    validateCbsAccount(
                            cheque.getDrawerAccountNumber());

            if (!"PASS".equals(cbsResult)) {

                return false;
            }

            String chequeDateResult =
                    validateChequeDate(
                            cheque.getChequeDate());

            if (!"PASS".equals(chequeDateResult)) {

                return false;
            }
        }

        // --------------------------------------------------------
        // REASON VALIDATION
        // --------------------------------------------------------

        if ("REJECT".equals(checkerAction)
                || "SEND_BACK".equals(checkerAction)) {

            if (checkerReasonCode == null
                    || checkerReasonCode.trim().isEmpty()) {

                return false;
            }

            checkerReasonCode =
                    checkerReasonCode.trim();
        }

        // --------------------------------------------------------
        // ACCEPT HAS NO REASON
        // --------------------------------------------------------

        if ("ACCEPT".equals(checkerAction)) {

            checkerReasonCode = null;
        }

        // --------------------------------------------------------
        // NORMALIZE REMARKS
        // --------------------------------------------------------

        if (checkerRemarks != null) {

            checkerRemarks =
                    checkerRemarks.trim();

            if (checkerRemarks.isEmpty()) {

                checkerRemarks = null;
            }
        }

        // --------------------------------------------------------
        // SAVE
        // --------------------------------------------------------

        return chequeDao.saveCheckerDecision(
                batchNumber,
                chequeNumber,
                checkerId,
                checkerAction,
                checkerReasonCode,
                checkerRemarks);
    }

    // ============================================================
    // GET MAKER REASON NAME
    // ============================================================

    public String getMakerReasonName(
            String reasonCode) {

        if (reasonCode == null
                || reasonCode.trim().isEmpty()) {

            return null;
        }

        return chequeDao.getReasonName(
                reasonCode.trim());
    }
}