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
                "SELECT batch_number, " +
                "       cheque_number, " +
                "       drawer_account_number, " +
                "       drawer_name, " +
                "       payee_account_number, " +
                "       payee_name, " +
                "       amount, " +
                "       amount_in_words, " +
                "       cheque_date, " +
                "       front_image_path, " +
                "       back_image_path, " +
                "       cheque_status, " +
                "       bank_code, " +
                "       branch_code, " +
                "       city_code, " +
                "       return_reason_id, " +
                "       checker_remarks " +
                "FROM outward_cheque " +
                "WHERE batch_number = ? " +
                "AND cheque_number = ?";

        try (Connection connection = CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, batchNumber);
            statement.setString(2, chequeNumber);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {

                    OutwardCheque cheque = new OutwardCheque();

                    cheque.setBatchNumber(
                            rs.getString("batch_number"));

                    cheque.setChequeNumber(
                            rs.getString("cheque_number"));

                    cheque.setDrawerAccountNumber(
                            rs.getString("drawer_account_number"));

                    cheque.setDrawerName(
                            rs.getString("drawer_name"));

                    cheque.setPayeeAccountNumber(
                            rs.getString("payee_account_number"));

                    cheque.setPayeeName(
                            rs.getString("payee_name"));

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
                            rs.getString("front_image_path"));

                    cheque.setBackImagePath(
                            rs.getString("back_image_path"));

                    cheque.setChequeStatus(
                            rs.getString("cheque_status"));

                    cheque.setBankCode(
                            rs.getString("bank_code"));

                    cheque.setBranchCode(
                            rs.getString("branch_code"));

                    cheque.setCityCode(
                            rs.getString("city_code"));

                    Object reasonObject =
                            rs.getObject("return_reason_id");

                    if (reasonObject != null) {
                        cheque.setReturnReasonId(
                                ((Number) reasonObject).intValue());
                    }

                    cheque.setCheckerRemarks(
                            rs.getString("checker_remarks"));

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
     * GET CHEQUE PROCESSING INFORMATION
     * ============================================================
     */

    public ChequeProcessing getChequeProcessing(
            String batchNumber,
            String chequeNumber) {

        String sql =
                "SELECT batch_number, " +
                "       cheque_number, " +
                "       maker_id, " +
                "       maker_action, " +
                "       maker_reason_code, " +
                "       checker_id, " +
                "       checker_action, " +
                "       checker_reason_code " +
                "FROM cheque_processing " +
                "WHERE batch_number = ? " +
                "AND cheque_number = ?";

        try (Connection connection = CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, batchNumber);
            statement.setString(2, chequeNumber);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {

                    ChequeProcessing processing =
                            new ChequeProcessing();

                    processing.setBatchNumber(
                            rs.getString("batch_number"));

                    processing.setChequeNumber(
                            rs.getString("cheque_number"));

                    Object makerId =
                            rs.getObject("maker_id");

                    if (makerId != null) {
                        processing.setMakerId(
                                ((Number) makerId).intValue());
                    }

                    processing.setMakerAction(
                            rs.getString("maker_action"));

                    processing.setMakerReasonCode(
                            rs.getString("maker_reason_code"));

                    Object checkerId =
                            rs.getObject("checker_id");

                    if (checkerId != null) {
                        processing.setCheckerId(
                                ((Number) checkerId).intValue());
                    }

                    processing.setCheckerAction(
                            rs.getString("checker_action"));

                    processing.setCheckerReasonCode(
                            rs.getString("checker_reason_code"));

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
     * GET CHECKER RETURN / REJECTION REASONS
     * ============================================================
     */

    public List<ReturnReason> getReturnReasons(
            String reasonType) {

        List<ReturnReason> reasons =
                new ArrayList<>();

        String sql =
                "SELECT reason_code, " +
                "       reason_name, " +
                "       active " +
                "FROM return_reason_master " +
                "WHERE active = true " +
                "AND role_name = 'CHECKER' " +
                "AND reason_type = ? " +
                "ORDER BY reason_name";

        try (Connection connection = CTSStaticData.getConnection();
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
     * ACCEPT:
     *     No reason.
     *     Cheque -> CHECKER_ACCEPTED
     *
     * REJECT:
     *     Reason mandatory.
     *     Cheque -> CHECKER_REJECTED
     *
     * SEND_BACK:
     *     Reason mandatory.
     *     Cheque -> SENT_BACK_TO_MAKER
     *     Batch  -> ON_HOLD
     *
     * If ACCEPT / REJECT makes every cheque in the batch final:
     *
     *     Batch -> CHECKER_COMPLETED
     *
     * Batch completion is determined from cheque_processing
     * checker_action, not from Maker statuses.
     *
     * All changes are performed in one transaction.
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


        /*
         * ========================================================
         * DETERMINE CHEQUE STATUS
         * ========================================================
         */

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
         * ========================================================
         * NORMALIZE REASON
         * ========================================================
         */

        if (checkerReasonCode != null) {

            checkerReasonCode =
                    checkerReasonCode.trim();

            if (checkerReasonCode.isEmpty()) {
                checkerReasonCode = null;
            }
        }


        /*
         * ========================================================
         * VALIDATE REASON
         * ========================================================
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
         * NORMALIZE REMARKS
         * ========================================================
         */

        if (checkerRemarks != null
                && checkerRemarks.trim().isEmpty()) {

            checkerRemarks = null;
        }


        /*
         * ========================================================
         * SQL
         * ========================================================
         */

        String updateProcessingSql =
                "UPDATE cheque_processing " +
                "SET checker_id = ?, " +
                "    checker_action = ?, " +
                "    checker_reason_code = ? " +
                "WHERE batch_number = ? " +
                "AND cheque_number = ?";

        String updateChequeSql =
                "UPDATE outward_cheque " +
                "SET cheque_status = ?, " +
                "    checker_remarks = ? " +
                "WHERE batch_number = ? " +
                "AND cheque_number = ?";


        /*
         * Used after ACCEPT / REJECT.
         *
         * Only ACCEPT and REJECT are considered final
         * Checker decisions.
         */
        String remainingChequeSql =
                "SELECT COUNT(*) " +
                "FROM cheque_processing " +
                "WHERE batch_number = ? " +
                "AND (checker_action IS NULL " +
                "OR UPPER(checker_action) NOT IN " +
                "('ACCEPT', 'REJECT'))";


        String updateBatchCompletedSql =
                "UPDATE outward_batch " +
                "SET batch_status = 'CHECKER_COMPLETED' " +
                "WHERE batch_number = ?";


        String updateBatchHoldSql =
                "UPDATE outward_batch " +
                "SET batch_status = 'ON_HOLD' " +
                "WHERE batch_number = ?";


        /*
         * ========================================================
         * DATABASE TRANSACTION
         * ========================================================
         */

        try (Connection connection =
                     CTSStaticData.getConnection()) {

            connection.setAutoCommit(false);

            try {

                /*
                 * =================================================
                 * 1. UPDATE CHEQUE PROCESSING
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
                 * 2. UPDATE OUTWARD CHEQUE
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
                 * 3. UPDATE BATCH STATUS
                 * =================================================
                 *
                 * SEND_BACK:
                 *
                 *     Batch becomes ON_HOLD immediately.
                 *
                 * ACCEPT / REJECT:
                 *
                 *     Check whether any cheque still has no
                 *     final Checker decision.
                 */

                if ("SEND_BACK".equals(checkerAction)) {

                    try (PreparedStatement statement =
                                 connection.prepareStatement(
                                         updateBatchHoldSql)) {

                        statement.setString(
                                1,
                                batchNumber);

                        int batchRows =
                                statement.executeUpdate();

                        if (batchRows != 1) {

                            throw new RuntimeException(
                                    "Batch record not found");
                        }
                    }

                } else {

                    int remainingCheques;

                    try (PreparedStatement statement =
                                 connection.prepareStatement(
                                         remainingChequeSql)) {

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
                     * No unfinished Checker decisions remain.
                     */
                    if (remainingCheques == 0) {

                        try (PreparedStatement statement =
                                     connection.prepareStatement(
                                             updateBatchCompletedSql)) {

                            statement.setString(
                                    1,
                                    batchNumber);

                            int batchRows =
                                    statement.executeUpdate();

                            if (batchRows != 1) {

                                throw new RuntimeException(
                                        "Batch record not found");
                            }
                        }
                    }
                }


                /*
                 * =================================================
                 * 4. COMMIT
                 * =================================================
                 */

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
                "SELECT reason_code " +
                "FROM return_reason_master " +
                "WHERE reason_code = ? " +
                "AND active = true " +
                "AND role_name = 'CHECKER' " +
                "AND reason_type = ?";

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
     *
     * Only drawer_account_number is validated.
     *
     * Conditions:
     *
     * 1. Account exists.
     * 2. Account is ACTIVE.
     */

    public Map<String, String> getCbsAccount(
            String accountNumber) {

        String sql =
                "SELECT account_number, " +
                "       account_holder_name, " +
                "       account_status " +
                "FROM account_master " +
                "WHERE account_number = ?";

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
