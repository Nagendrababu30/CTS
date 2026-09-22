package com.iispl.cts.dao.outward.checker;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.iispl.cts.data.CTSStaticData;
import com.iispl.cts.model.outward.ChequeProcessing;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.model.outward.ReturnReason;

public class CheckerChequeDAO {

    /*
     * ============================================================
     * GET PARTICULAR CHEQUE
     * ============================================================
     */

    public OutwardCheque getCheque(
            String batchNumber,
            String chequeNumber) {

        String sql =
                "SELECT batch_number, "
                + "       cheque_number, "
                + "       drawer_account_number, "
                + "       drawer_name, "
                + "       payee_account_number, "
                + "       payee_name, "
                + "       amount, "
                + "       amount_in_words, "
                + "       cheque_date, "
                + "       front_image_path, "
                + "       back_image_path, "
                + "       cheque_status, "
                + "       bank_code, "
                + "       branch_code, "
                + "       city_code, "
                + "       return_reason_id, "
                + "       checker_remarks "
                + "FROM outward_cheque "
                + "WHERE batch_number = ? "
                + "AND cheque_number = ?";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, batchNumber);
            statement.setString(2, chequeNumber);

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {

                    OutwardCheque cheque =
                            new OutwardCheque();

                    cheque.setBatchNumber(
                            rs.getString("batch_number"));

                    cheque.setChequeNumber(
                            rs.getString("cheque_number"));

                    cheque.setDrawerAccountNumber(
                            rs.getString(
                                    "drawer_account_number"));

                    cheque.setDrawerName(
                            rs.getString("drawer_name"));

                    cheque.setPayeeAccountNumber(
                            rs.getString(
                                    "payee_account_number"));

                    cheque.setPayeeName(
                            rs.getString(
                                    "payee_name"));

                    cheque.setAmount(
                            rs.getBigDecimal("amount"));

                    cheque.setAmountInWords(
                            rs.getString("amount_in_words"));

                    Date chequeDate =
                            rs.getDate("cheque_date");

                    if (chequeDate != null) {
                        cheque.setChequeDate(
                                chequeDate.toLocalDate());
                    }

                    cheque.setFrontImagePath(
                            rs.getString(
                                    "front_image_path"));

                    cheque.setBackImagePath(
                            rs.getString(
                                    "back_image_path"));

                    cheque.setChequeStatus(
                            rs.getString("cheque_status"));

                    cheque.setBankCode(
                            rs.getString("bank_code"));

                    cheque.setBranchCode(
                            rs.getString("branch_code"));

                    cheque.setCityCode(
                            rs.getString("city_code"));

                    Object reasonObject =
                            rs.getObject(
                                    "return_reason_id");

                    if (reasonObject != null) {

                        cheque.setReturnReasonId(
                                ((Number) reasonObject)
                                        .intValue());
                    }

                    cheque.setCheckerRemarks(
                            rs.getString(
                                    "checker_remarks"));

                    return cheque;
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching cheque details",
                    e);
        }

        return null;
    }


    /*
     * ============================================================
     * GET ALL CHEQUES FOR A BATCH
     * ============================================================
     */

    public List<OutwardCheque> getChequesByBatch(
            String batchNumber) {

        List<OutwardCheque> cheques =
                new ArrayList<>();

        String sql =
                "SELECT batch_number, "
                + "       cheque_number, "
                + "       drawer_account_number, "
                + "       drawer_name, "
                + "       payee_account_number, "
                + "       payee_name, "
                + "       amount, "
                + "       amount_in_words, "
                + "       cheque_date, "
                + "       front_image_path, "
                + "       back_image_path, "
                + "       cheque_status, "
                + "       bank_code, "
                + "       branch_code, "
                + "       city_code, "
                + "       return_reason_id, "
                + "       checker_remarks "
                + "FROM outward_cheque "
                + "WHERE batch_number = ? "
                + "ORDER BY cheque_number";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber);

            try (ResultSet rs =
                         statement.executeQuery()) {

                while (rs.next()) {

                    OutwardCheque cheque =
                            new OutwardCheque();

                    cheque.setBatchNumber(
                            rs.getString(
                                    "batch_number"));

                    cheque.setChequeNumber(
                            rs.getString(
                                    "cheque_number"));

                    cheque.setDrawerAccountNumber(
                            rs.getString(
                                    "drawer_account_number"));

                    cheque.setDrawerName(
                            rs.getString(
                                    "drawer_name"));

                    cheque.setPayeeAccountNumber(
                            rs.getString(
                                    "payee_account_number"));

                    cheque.setPayeeName(
                            rs.getString(
                                    "payee_name"));

                    cheque.setAmount(
                            rs.getBigDecimal("amount"));

                    cheque.setAmountInWords(
                            rs.getString(
                                    "amount_in_words"));

                    Date chequeDate =
                            rs.getDate("cheque_date");

                    if (chequeDate != null) {

                        cheque.setChequeDate(
                                chequeDate.toLocalDate());
                    }

                    cheque.setFrontImagePath(
                            rs.getString(
                                    "front_image_path"));

                    cheque.setBackImagePath(
                            rs.getString(
                                    "back_image_path"));

                    cheque.setChequeStatus(
                            rs.getString(
                                    "cheque_status"));

                    cheque.setBankCode(
                            rs.getString(
                                    "bank_code"));

                    cheque.setBranchCode(
                            rs.getString(
                                    "branch_code"));

                    cheque.setCityCode(
                            rs.getString(
                                    "city_code"));

                    Object reasonObject =
                            rs.getObject(
                                    "return_reason_id");

                    if (reasonObject != null) {

                        cheque.setReturnReasonId(
                                ((Number) reasonObject)
                                        .intValue());
                    }

                    cheque.setCheckerRemarks(
                            rs.getString(
                                    "checker_remarks"));

                    cheques.add(cheque);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching cheques for batch",
                    e);
        }

        return cheques;
    }


    
    /*
     * ============================================================
     * GET CHECKER COMPLETED BATCHES FOR REPORTS
     * ============================================================
     */

    public List<OutwardBatch> getCheckerCompletedBatches() {

        List<OutwardBatch> batches =
                new ArrayList<>();

        String sql =
                "SELECT ob.batch_number, "
                + "       COUNT(oc.cheque_number) AS total_cheques "
                + "FROM outward_batch ob "
                + "LEFT JOIN outward_cheque oc "
                + "       ON ob.batch_number = oc.batch_number "
                + "WHERE UPPER(ob.batch_status) = 'CHECKER_VERIFIED' "
                + "GROUP BY ob.batch_number "
                + "ORDER BY ob.batch_number DESC";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs =
                     statement.executeQuery()) {

            while (rs.next()) {

                OutwardBatch batch =
                        new OutwardBatch();

                batch.setBatchNumber(
                        rs.getString("batch_number"));

                batch.setNumberOfCheques(
                        rs.getInt("total_cheques"));

                batches.add(batch);
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching Checker completed batches",
                    e);
        }

        return batches;
    }
    
    
    
    
    /*
     * ============================================================
     * GET RE-VERIFIED CHEQUES
     * ============================================================
     *
     * Re-Verify is based ONLY on:
     *
     * 1. batch_number
     * 2. original checker_id
     * 3. checker_action = SEND_BACK
     * 4. cheque_status = RE_VERIFIED
     *
     * outward_batch.batch_status is deliberately NOT used.
     *
     * This returns only the cheques that were sent back by
     * the current/original Checker and subsequently corrected
     * by Maker.
     */

    public List<OutwardCheque> getReVerifiedCheques(
            String batchNumber,
            long checkerUserId) {

        List<OutwardCheque> cheques =
                new ArrayList<>();

        String sql =
                "SELECT oc.batch_number, "
                + "       oc.cheque_number, "
                + "       oc.drawer_account_number, "
                + "       oc.drawer_name, "
                + "       oc.payee_account_number, "
                + "       oc.payee_name, "
                + "       oc.amount, "
                + "       oc.amount_in_words, "
                + "       oc.cheque_date, "
                + "       oc.front_image_path, "
                + "       oc.back_image_path, "
                + "       oc.cheque_status, "
                + "       oc.bank_code, "
                + "       oc.branch_code, "
                + "       oc.city_code, "
                + "       oc.return_reason_id, "
                + "       oc.checker_remarks "
                + "FROM outward_cheque oc "
                + "INNER JOIN cheque_processing cp "
                + "   ON cp.batch_number = oc.batch_number "
                + "  AND cp.cheque_number = oc.cheque_number "
                + "WHERE oc.batch_number = ? "
                + "AND cp.checker_id = ? "
                + "AND UPPER(TRIM(cp.checker_action)) = 'SEND_BACK' "
                + "AND UPPER(TRIM(oc.cheque_status)) = 'RE_VERIFIED' "
                + "ORDER BY oc.cheque_number";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber);

            statement.setLong(
                    2,
                    checkerUserId);

            try (ResultSet rs =
                         statement.executeQuery()) {

                while (rs.next()) {

                    OutwardCheque cheque =
                            new OutwardCheque();

                    cheque.setBatchNumber(
                            rs.getString(
                                    "batch_number"));

                    cheque.setChequeNumber(
                            rs.getString(
                                    "cheque_number"));

                    cheque.setDrawerAccountNumber(
                            rs.getString(
                                    "drawer_account_number"));

                    cheque.setDrawerName(
                            rs.getString(
                                    "drawer_name"));

                    cheque.setPayeeAccountNumber(
                            rs.getString(
                                    "payee_account_number"));

                    cheque.setPayeeName(
                            rs.getString(
                                    "payee_name"));

                    cheque.setAmount(
                            rs.getBigDecimal(
                                    "amount"));

                    cheque.setAmountInWords(
                            rs.getString(
                                    "amount_in_words"));

                    Date chequeDate =
                            rs.getDate(
                                    "cheque_date");

                    if (chequeDate != null) {

                        cheque.setChequeDate(
                                chequeDate.toLocalDate());
                    }

                    cheque.setFrontImagePath(
                            rs.getString(
                                    "front_image_path"));

                    cheque.setBackImagePath(
                            rs.getString(
                                    "back_image_path"));

                    cheque.setChequeStatus(
                            rs.getString(
                                    "cheque_status"));

                    cheque.setBankCode(
                            rs.getString(
                                    "bank_code"));

                    cheque.setBranchCode(
                            rs.getString(
                                    "branch_code"));

                    cheque.setCityCode(
                            rs.getString(
                                    "city_code"));

                    Object reasonObject =
                            rs.getObject(
                                    "return_reason_id");

                    if (reasonObject != null) {

                        cheque.setReturnReasonId(
                                ((Number) reasonObject)
                                        .intValue());
                    }

                    cheque.setCheckerRemarks(
                            rs.getString(
                                    "checker_remarks"));

                    cheques.add(cheque);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching re-verified cheques",
                    e);
        }

        return cheques;
    }


    /*
     * ============================================================
     * START CHECKER PROCESSING
     * ============================================================
     *
     * Moves a Maker re-verified cheque back into
     * Checker processing.
     *
     * RE_VERIFIED
     *      ↓
     * CHECKER_PROCESSING
     */

    public boolean startCheckerProcessing(
            String batchNumber,
            String chequeNumber) {

        String sql =
                "UPDATE outward_cheque "
                + "SET cheque_status = 'CHECKER_PROCESSING' "
                + "WHERE batch_number = ? "
                + "AND cheque_number = ? "
                + "AND UPPER(TRIM(cheque_status)) = "
                + "'RE_VERIFIED'";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber);

            statement.setString(
                    2,
                    chequeNumber);

            int rows =
                    statement.executeUpdate();

            return rows == 1;

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while starting Checker processing",
                    e);
        }
    }


    /*
     * ============================================================
     * GET CHEQUE PROCESSING INFORMATION
     * ============================================================
     */

    public ChequeProcessing getChequeProcessing(
            String batchNumber,
            String chequeNumber) {

        String sql =
                "SELECT batch_number, "
                + "       cheque_number, "
                + "       maker_id, "
                + "       maker_action, "
                + "       maker_reason_code, "
                + "       checker_id, "
                + "       checker_action, "
                + "       checker_reason_code "
                + "FROM cheque_processing "
                + "WHERE batch_number = ? "
                + "AND cheque_number = ?";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber);

            statement.setString(
                    2,
                    chequeNumber);

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {

                    ChequeProcessing processing =
                            new ChequeProcessing();

                    processing.setBatchNumber(
                            rs.getString(
                                    "batch_number"));

                    processing.setChequeNumber(
                            rs.getString(
                                    "cheque_number"));

                    Object makerId =
                            rs.getObject("maker_id");

                    if (makerId != null) {

                        processing.setMakerId(
                                ((Number) makerId)
                                        .intValue());
                    }

                    processing.setMakerAction(
                            rs.getString(
                                    "maker_action"));

                    processing.setMakerReasonCode(
                            rs.getString(
                                    "maker_reason_code"));

                    Object checkerId =
                            rs.getObject("checker_id");

                    if (checkerId != null) {

                        processing.setCheckerId(
                                ((Number) checkerId)
                                        .intValue());
                    }

                    processing.setCheckerAction(
                            rs.getString(
                                    "checker_action"));

                    processing.setCheckerReasonCode(
                            rs.getString(
                                    "checker_reason_code"));

                    return processing;
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching cheque processing details",
                    e);
        }

        return null;
    }


    /*
     * ============================================================
     * GET MAKER REASON DESCRIPTION
     * ============================================================
     */

    public String getReasonName(
            String reasonCode) {

        if (reasonCode == null
                || reasonCode.trim().isEmpty()) {

            return null;
        }

        String sql =
                "SELECT reason_name "
                + "FROM return_reason_master "
                + "WHERE reason_code = ? "
                + "AND active = true";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    reasonCode.trim());

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {

                    return rs.getString(
                            "reason_name");
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching reason description",
                    e);
        }

        return null;
    }


    /*
     * ============================================================
     * GET CHECKER RETURN / REJECTION REASONS
     * ============================================================
     */

    public List<ReturnReason> getReturnReasons(
            String reasonType) {

        List<ReturnReason> reasons =
                new ArrayList<>();

        String sql =
                "SELECT reason_code, "
                + "       reason_name, "
                + "       active "
                + "FROM return_reason_master "
                + "WHERE active = true "
                + "AND role_name = 'CHECKER' "
                + "AND reason_type = ? "
                + "ORDER BY reason_name";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    reasonType);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {

                    ReturnReason reason =
                            new ReturnReason();

                    reason.setReasonCode(
                            resultSet.getString(
                                    "reason_code"));

                    reason.setReasonName(
                            resultSet.getString(
                                    "reason_name"));

                    reason.setActive(
                            resultSet.getBoolean(
                                    "active"));

                    reasons.add(reason);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching Checker reasons",
                    e);
        }

        return reasons;
    }


    /*
     * ============================================================
     * SAVE CHECKER DECISION
     * ============================================================
     *
     * FLOW:
     *
     * FIRST CHECKER CYCLE
     * -------------------
     *
     * Checker processes every cheque.
     *
     * ACCEPT
     * REJECT
     * SEND_BACK
     *
     * SEND_BACK does NOT immediately change the batch status.
     *
     * After the LAST cheque:
     *
     * Any SEND_BACK
     *      ↓
     * ON_HOLD
     *
     * No SEND_BACK
     *      ↓
     * CHECKER_VERIFIED
     *
     *
     * RE-VERIFICATION
     * ----------------
     *
     * Maker corrects returned cheque.
     *
     * RE_VERIFIED
     *      ↓
     * Checker re-verifies
     *
     * ACCEPT / REJECT
     *      ↓
     * Final
     *
     * SEND_BACK
     *      ↓
     * ON_HOLD
     *
     * When no returned/re-verification cheque remains:
     *
     * CHECKER_VERIFIED
     */

    public boolean saveCheckerDecision(
            String batchNumber,
            String chequeNumber,
            long checkerId,
            String checkerAction,
            String checkerReasonCode,
            String checkerRemarks) {

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Batch number is required");
        }

        if (chequeNumber == null
                || chequeNumber.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Cheque number is required");
        }

        if (checkerAction == null
                || checkerAction.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Checker action is required");
        }

        checkerAction =
                checkerAction.trim().toUpperCase();

        if (checkerReasonCode != null) {

            checkerReasonCode =
                    checkerReasonCode.trim();

            if (checkerReasonCode.isEmpty()) {
                checkerReasonCode = null;
            }
        }

        if (checkerRemarks != null
                && checkerRemarks.trim().isEmpty()) {

            checkerRemarks = null;
        }

        String chequeStatus;

        if ("ACCEPT".equals(checkerAction)) {

            chequeStatus =
                    "CHECKER_ACCEPTED";

            checkerReasonCode = null;

        } else if ("REJECT".equals(checkerAction)) {

            if (checkerReasonCode == null
                    || checkerReasonCode.trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "Reject reason is mandatory");
            }

            chequeStatus =
                    "CHECKER_REJECTED";

        } else if ("SEND_BACK".equals(checkerAction)) {

            if (checkerReasonCode == null
                    || checkerReasonCode.trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "Send Back reason is mandatory");
            }

            chequeStatus =
                    "SENT_BACK_TO_MAKER";

        } else {

            throw new IllegalArgumentException(
                    "Invalid Checker action: "
                            + checkerAction);
        }

        /*
         * Validate Checker reason.
         */
        if ("REJECT".equals(checkerAction)
                || "SEND_BACK".equals(checkerAction)) {

            if (!isValidCheckerReason(
                    checkerReasonCode,
                    checkerAction)) {

                throw new IllegalArgumentException(
                        "Invalid Checker reason: "
                                + checkerReasonCode);
            }
        }

        /*
         * ========================================================
         * SQL
         * ========================================================
         */

        /*
         * Get the previous Checker action BEFORE updating it.
         *
         * If previous action is SEND_BACK, this cheque is
         * being processed again after Maker correction.
         */
        String previousCheckerActionSql =
                "SELECT checker_action "
                + "FROM cheque_processing "
                + "WHERE batch_number = ? "
                + "AND cheque_number = ? "
                + "FOR UPDATE";

        String updateProcessingSql =
                "UPDATE cheque_processing "
                + "SET checker_id = ?, "
                + "    checker_action = ?, "
                + "    checker_reason_code = ? "
                + "WHERE batch_number = ? "
                + "AND cheque_number = ?";

        String updateChequeSql =
                "UPDATE outward_cheque "
                + "SET cheque_status = ?, "
                + "    checker_remarks = ? "
                + "WHERE batch_number = ? "
                + "AND cheque_number = ?";

        /*
         * First-cycle completion check.
         */
        String remainingFirstCycleSql =
                "SELECT COUNT(*) "
                + "FROM cheque_processing "
                + "WHERE batch_number = ? "
                + "AND (checker_action IS NULL "
                + "OR UPPER(TRIM(checker_action)) NOT IN "
                + "('ACCEPT', 'REJECT', 'SEND_BACK'))";

        /*
         * First-cycle Send Back check.
         */
        String sendBackFirstCycleSql =
                "SELECT COUNT(*) "
                + "FROM cheque_processing "
                + "WHERE batch_number = ? "
                + "AND UPPER(TRIM(checker_action)) = "
                + "'SEND_BACK'";

        /*
         * Batch status updates.
         */
        String updateBatchVerifiedSql =
                "UPDATE outward_batch "
                + "SET batch_status = 'CHECKER_VERIFIED' "
                + "WHERE batch_number = ?";

        String updateBatchHoldSql =
                "UPDATE outward_batch "
                + "SET batch_status = 'ON_HOLD' "
                + "WHERE batch_number = ?";

        String updateBatchProcessingSql =
                "UPDATE outward_batch "
                + "SET batch_status = 'CHECKER_PROCESSING' "
                + "WHERE batch_number = ?";

        /*
         * Re-verification checks.
         *
         * SENT_BACK_TO_MAKER
         *     = waiting for Maker
         *
         * RE_VERIFIED
         *     = corrected and waiting for Checker
         *
         * CHECKER_PROCESSING
         *     = currently being re-verified
         */
        String pendingMakerSql =
                "SELECT COUNT(*) "
                + "FROM outward_cheque "
                + "WHERE batch_number = ? "
                + "AND UPPER(TRIM(cheque_status)) = "
                + "'SENT_BACK_TO_MAKER'";

        String pendingReVerificationSql =
                "SELECT COUNT(*) "
                + "FROM outward_cheque "
                + "WHERE batch_number = ? "
                + "AND UPPER(TRIM(cheque_status)) IN "
                + "('RE_VERIFIED', 'CHECKER_PROCESSING')";

        /*
         * ========================================================
         * TRANSACTION
         * ========================================================
         */

        try (Connection connection =
                     CTSStaticData.getConnection()) {

            connection.setAutoCommit(false);

            try {

                /*
                 * =================================================
                 * GET PREVIOUS CHECKER ACTION
                 * =================================================
                 */

                String previousCheckerAction = null;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     previousCheckerActionSql)) {

                    statement.setString(
                            1,
                            batchNumber);

                    statement.setString(
                            2,
                            chequeNumber);

                    try (ResultSet rs =
                                 statement.executeQuery()) {

                        if (!rs.next()) {

                            throw new RuntimeException(
                                    "Cheque processing record not found");
                        }

                        previousCheckerAction =
                                rs.getString(
                                        "checker_action");
                    }
                }

                /*
                 * If the previous action was SEND_BACK,
                 * this is a re-verification decision.
                 */
                boolean reVerification =
                        previousCheckerAction != null
                        && "SEND_BACK".equalsIgnoreCase(
                                previousCheckerAction.trim());

                /*
                 * =================================================
                 * SAVE CHECKER PROCESSING
                 * =================================================
                 */

                int processingRows;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     updateProcessingSql)) {

                    statement.setLong(
                            1,
                            checkerId);

                    statement.setString(
                            2,
                            checkerAction);

                    if (checkerReasonCode == null) {

                        statement.setNull(
                                3,
                                java.sql.Types.VARCHAR);

                    } else {

                        statement.setString(
                                3,
                                checkerReasonCode);
                    }

                    statement.setString(
                            4,
                            batchNumber);

                    statement.setString(
                            5,
                            chequeNumber);

                    processingRows =
                            statement.executeUpdate();
                }

                if (processingRows != 1) {

                    throw new RuntimeException(
                            "Cheque processing record not found");
                }

                /*
                 * =================================================
                 * SAVE CHEQUE STATUS
                 * =================================================
                 */

                int chequeRows;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     updateChequeSql)) {

                    statement.setString(
                            1,
                            chequeStatus);

                    if (checkerRemarks == null) {

                        statement.setNull(
                                2,
                                java.sql.Types.VARCHAR);

                    } else {

                        statement.setString(
                                2,
                                checkerRemarks);
                    }

                    statement.setString(
                            3,
                            batchNumber);

                    statement.setString(
                            4,
                            chequeNumber);

                    chequeRows =
                            statement.executeUpdate();
                }

                if (chequeRows != 1) {

                    throw new RuntimeException(
                            "Cheque record not found");
                }

                /*
                 * =================================================
                 * RE-VERIFICATION FLOW
                 * =================================================
                 */

                if (reVerification) {

                    int pendingMakerCheques;

                    try (PreparedStatement statement =
                                 connection.prepareStatement(
                                         pendingMakerSql)) {

                        statement.setString(
                                1,
                                batchNumber);

                        try (ResultSet rs =
                                     statement.executeQuery()) {

                            if (!rs.next()) {

                                throw new RuntimeException(
                                        "Unable to determine pending Maker cheques");
                            }

                            pendingMakerCheques =
                                    rs.getInt(1);
                        }
                    }

                    int pendingReVerificationCheques;

                    try (PreparedStatement statement =
                                 connection.prepareStatement(
                                         pendingReVerificationSql)) {

                        statement.setString(
                                1,
                                batchNumber);

                        try (ResultSet rs =
                                     statement.executeQuery()) {

                            if (!rs.next()) {

                                throw new RuntimeException(
                                        "Unable to determine pending re-verification cheques");
                            }

                            pendingReVerificationCheques =
                                    rs.getInt(1);
                        }
                    }

                    /*
                     * Another returned cheque is still with Maker.
                     *
                     * Batch remains ON_HOLD.
                     */
                    if (pendingMakerCheques > 0) {

                        try (PreparedStatement statement =
                                     connection.prepareStatement(
                                             updateBatchHoldSql)) {

                            statement.setString(
                                    1,
                                    batchNumber);

                            if (statement.executeUpdate() != 1) {

                                throw new RuntimeException(
                                        "Batch record not found");
                            }
                        }

                    /*
                     * Maker has corrected some/all returned cheques,
                     * but at least one is still waiting for Checker.
                     */
                    } else if (pendingReVerificationCheques > 0) {

                        try (PreparedStatement statement =
                                     connection.prepareStatement(
                                             updateBatchProcessingSql)) {

                            statement.setString(
                                    1,
                                    batchNumber);

                            if (statement.executeUpdate() != 1) {

                                throw new RuntimeException(
                                        "Batch record not found");
                            }
                        }

                    /*
                     * No returned/re-verification cheque remains.
                     *
                     * Batch is completely verified.
                     */
                    } else {

                        try (PreparedStatement statement =
                                     connection.prepareStatement(
                                             updateBatchVerifiedSql)) {

                            statement.setString(
                                    1,
                                    batchNumber);

                            if (statement.executeUpdate() != 1) {

                                throw new RuntimeException(
                                        "Batch record not found");
                            }
                        }
                    }

                    connection.commit();

                    return true;
                }

                /*
                 * =================================================
                 * FIRST CHECKER CYCLE
                 * =================================================
                 *
                 * The current cheque may be SEND_BACK.
                 *
                 * But the batch MUST remain CHECKER_PROCESSING
                 * until every cheque has been processed.
                 */

                int remainingCheques;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     remainingFirstCycleSql)) {

                    statement.setString(
                            1,
                            batchNumber);

                    try (ResultSet rs =
                                 statement.executeQuery()) {

                        if (!rs.next()) {

                            throw new RuntimeException(
                                    "Unable to determine batch completion");
                        }

                        remainingCheques =
                                rs.getInt(1);
                    }
                }

                /*
                 * =================================================
                 * FIRST CYCLE STILL IN PROGRESS
                 * =================================================
                 */

                if (remainingCheques > 0) {

                    /*
                     * IMPORTANT:
                     *
                     * SEND_BACK does NOT put the batch ON_HOLD here.
                     *
                     * Checker continues to the next cheque.
                     */
                    connection.commit();

                    return true;
                }

                /*
                 * =================================================
                 * FIRST CHECKER CYCLE COMPLETED
                 * =================================================
                 *
                 * All cheques have a Checker decision.
                 *
                 * Now check whether any cheque was SEND_BACK.
                 */

                int sendBackCheques;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     sendBackFirstCycleSql)) {

                    statement.setString(
                            1,
                            batchNumber);

                    try (ResultSet rs =
                                 statement.executeQuery()) {

                        if (!rs.next()) {

                            throw new RuntimeException(
                                    "Unable to determine Send Back status");
                        }

                        sendBackCheques =
                                rs.getInt(1);
                    }
                }

                /*
                 * =================================================
                 * FIRST CYCLE RESULT
                 * =================================================
                 */

                if (sendBackCheques > 0) {

                    /*
                     * At least one cheque was sent back.
                     *
                     * The entire first Checker cycle is complete.
                     *
                     * Now batch becomes ON_HOLD.
                     */

                    try (PreparedStatement statement =
                                 connection.prepareStatement(
                                         updateBatchHoldSql)) {

                        statement.setString(
                                1,
                                batchNumber);

                        if (statement.executeUpdate() != 1) {

                            throw new RuntimeException(
                                    "Batch record not found");
                        }
                    }

                } else {

                    /*
                     * No cheque was sent back.
                     *
                     * All cheques are final:
                     *
                     * ACCEPT / REJECT
                     *
                     * Batch becomes CHECKER_VERIFIED.
                     */

                    try (PreparedStatement statement =
                                 connection.prepareStatement(
                                         updateBatchVerifiedSql)) {

                        statement.setString(
                                1,
                                batchNumber);

                        if (statement.executeUpdate() != 1) {

                            throw new RuntimeException(
                                    "Batch record not found");
                        }
                    }
                }

                connection.commit();

                return true;

            } catch (Exception e) {

                connection.rollback();

                throw e;
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while saving Checker decision",
                    e);
        }
    }


    /*
     * ============================================================
     * VALIDATE CHECKER REASON
     * ============================================================
     */

    private boolean isValidCheckerReason(
            String reasonCode,
            String action) {

        if (reasonCode == null
                || reasonCode.trim().isEmpty()) {

            return false;
        }

        String sql =
                "SELECT reason_code "
                + "FROM return_reason_master "
                + "WHERE reason_code = ? "
                + "AND active = true "
                + "AND role_name = 'CHECKER' "
                + "AND reason_type = ?";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    reasonCode);

            statement.setString(
                    2,
                    action.toUpperCase());

            try (ResultSet rs =
                         statement.executeQuery()) {

                return rs.next();
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while validating Checker reason",
                    e);
        }
    }


    /*
     * ============================================================
     * GET CBS ACCOUNT
     * ============================================================
     */

    public Map<String, String> getCbsAccount(
            String accountNumber) {

        String sql =
                "SELECT account_number, "
                + "       account_holder_name, "
                + "       account_status "
                + "FROM account_master "
                + "WHERE account_number = ?";

        Map<String, String> account = null;

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    accountNumber);

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {

                    account =
                            new HashMap<>();

                    account.put(
                            "accountNumber",
                            rs.getString(
                                    "account_number"));

                    account.put(
                            "accountHolderName",
                            rs.getString(
                                    "account_holder_name"));

                    account.put(
                            "accountStatus",
                            rs.getString(
                                    "account_status"));
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching CBS account",
                    e);
        }

        return account;
    }
}