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

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, batchNumber);
            statement.setString(2, chequeNumber);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {

                    OutwardCheque cheque =
                            new OutwardCheque();

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
                "       maker_reason_id, " +
                "       checker_id, " +
                "       checker_action, " +
                "       checker_reason_id " +
                "FROM cheque_processing " +
                "WHERE batch_number = ? " +
                "AND cheque_number = ?";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, batchNumber);
            statement.setString(2, chequeNumber);

            try (ResultSet rs =
                         statement.executeQuery()) {

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

                    Object makerReasonId =
                            rs.getObject("maker_reason_id");

                    if (makerReasonId != null) {
                        processing.setMakerReasonId(
                                ((Number) makerReasonId).intValue());
                    }

                    Object checkerId =
                            rs.getObject("checker_id");

                    if (checkerId != null) {
                        processing.setCheckerId(
                                ((Number) checkerId).intValue());
                    }

                    processing.setCheckerAction(
                            rs.getString("checker_action"));

                    Object checkerReasonId =
                            rs.getObject("checker_reason_id");

                    if (checkerReasonId != null) {
                        processing.setCheckerReasonId(
                                ((Number) checkerReasonId).intValue());
                    }

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
     * GET ACTIVE RETURN / REJECTION REASONS
     * ============================================================
     */
    public List<ReturnReason> getReturnReasons() {

        List<ReturnReason> reasons =
                new ArrayList<>();

        String sql =
                "SELECT id, " +
                "       reason_code, " +
                "       reason_name, " +
                "       active " +
                "FROM return_reason_master " +
                "WHERE active = true " +
                "ORDER BY reason_name";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs =
                     statement.executeQuery()) {

            while (rs.next()) {

                ReturnReason reason =
                        new ReturnReason();

                reason.setId(
                        rs.getInt("id"));

                reason.setReasonCode(
                        rs.getString("reason_code"));

                reason.setReasonName(
                        rs.getString("reason_name"));

                reason.setActive(
                        rs.getBoolean("active"));

                reasons.add(reason);
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching return reasons",
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
     *     No reason required.
     *
     * REJECT:
     *     Reason mandatory.
     *
     * SEND_BACK:
     *     Reason mandatory.
     *
     * Checker remarks are optional and are saved in
     * outward_cheque.checker_remarks.
     *
     * Both cheque_processing and outward_cheque are updated
     * inside the same database transaction.
     */
    public boolean saveCheckerDecision(
            String batchNumber,
            String chequeNumber,
            long checkerId,
            String checkerAction,
            Integer checkerReasonId,
            String checkerRemarks) {

        String updateProcessingSql =
                "UPDATE cheque_processing " +
                "SET checker_id = ?, " +
                "    checker_action = ?, " +
                "    checker_reason_id = ? " +
                "WHERE batch_number = ? " +
                "AND cheque_number = ?";

        /*
         * checker_remarks exists in the current
         * outward_cheque table.
         *
         * Do NOT use updated_by / updated_at.
         */
        String updateChequeSql =
                "UPDATE outward_cheque " +
                "SET cheque_status = ?, " +
                "    return_reason_id = ?, " +
                "    checker_remarks = ? " +
                "WHERE batch_number = ? " +
                "AND cheque_number = ?";

        String chequeStatus;

        /*
         * --------------------------------------------------------
         * ACCEPT
         * --------------------------------------------------------
         */
        if ("ACCEPT".equalsIgnoreCase(checkerAction)) {

            chequeStatus =
                    "CHECKER_ACCEPTED";

            checkerReasonId = null;
        }

        /*
         * --------------------------------------------------------
         * REJECT
         * --------------------------------------------------------
         */
        else if ("REJECT".equalsIgnoreCase(checkerAction)) {

            if (checkerReasonId == null) {

                throw new IllegalArgumentException(
                        "Reject reason is mandatory");
            }

            chequeStatus =
                    "CHECKER_REJECTED";
        }

        /*
         * --------------------------------------------------------
         * SEND BACK
         * --------------------------------------------------------
         */
        else if ("SEND_BACK".equalsIgnoreCase(checkerAction)) {

            if (checkerReasonId == null) {

                throw new IllegalArgumentException(
                        "Send Back reason is mandatory");
            }

            chequeStatus =
                    "SENT_BACK_TO_MAKER";
        }

        /*
         * --------------------------------------------------------
         * INVALID ACTION
         * --------------------------------------------------------
         */
        else {

            throw new IllegalArgumentException(
                    "Invalid Checker action: "
                            + checkerAction);
        }


        /*
         * --------------------------------------------------------
         * Normalize remarks
         * --------------------------------------------------------
         *
         * Empty remarks are stored as NULL instead of
         * an empty string.
         */
        if (checkerRemarks != null &&
                checkerRemarks.trim().isEmpty()) {

            checkerRemarks = null;
        }


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
                 * ------------------------------------------------
                 * 1. Update cheque_processing
                 * ------------------------------------------------
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
                            checkerAction.toUpperCase());

                    if (checkerReasonId == null) {

                        statement.setNull(
                                3,
                                java.sql.Types.INTEGER);

                    } else {

                        statement.setInt(
                                3,
                                checkerReasonId);
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

                /*
                 * Processing record must already exist.
                 */
                if (processingRows != 1) {

                    connection.rollback();

                    throw new RuntimeException(
                            "Cheque processing record not found");
                }


                /*
                 * ------------------------------------------------
                 * 2. Update outward_cheque
                 * ------------------------------------------------
                 */
                int chequeRows;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     updateChequeSql)) {

                    statement.setString(
                            1,
                            chequeStatus);

                    if (checkerReasonId == null) {

                        statement.setNull(
                                2,
                                java.sql.Types.INTEGER);

                    } else {

                        statement.setInt(
                                2,
                                checkerReasonId);
                    }

                    if (checkerRemarks == null) {

                        statement.setNull(
                                3,
                                java.sql.Types.VARCHAR);

                    } else {

                        statement.setString(
                                3,
                                checkerRemarks);
                    }

                    statement.setString(
                            4,
                            batchNumber);

                    statement.setString(
                            5,
                            chequeNumber);

                    chequeRows =
                            statement.executeUpdate();
                }

                /*
                 * Cheque record must exist.
                 */
                if (chequeRows != 1) {

                    connection.rollback();

                    throw new RuntimeException(
                            "Cheque record not found");
                }


                /*
                 * ------------------------------------------------
                 * 3. Commit
                 * ------------------------------------------------
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
     * GET RETURN REASON BY ID
     * ============================================================
     */
    public ReturnReason getReturnReasonById(
            int reasonId) {

        String sql =
                "SELECT id, " +
                "       reason_code, " +
                "       reason_name, " +
                "       active " +
                "FROM return_reason_master " +
                "WHERE id = ?";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    reasonId);

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {

                    ReturnReason reason =
                            new ReturnReason();

                    reason.setId(
                            rs.getInt("id"));

                    reason.setReasonCode(
                            rs.getString("reason_code"));

                    reason.setReasonName(
                            rs.getString("reason_name"));

                    reason.setActive(
                            rs.getBoolean("active"));

                    return reason;
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching return reason: "
                            + reasonId,
                    e);
        }

        return null;
    }


    /*
     * ============================================================
     * GET CBS ACCOUNT
     * ============================================================
     *
     * Used by CheckerProcessingService for CBS validation.
     *
     * Only these CBS fields are required currently:
     *
     *     account_number
     *     account_holder_name
     *     account_status
     *
     * We intentionally do not check:
     *
     *     balance
     *     account type
     *     branch
     *     any other CBS condition
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