package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.cts.inward.config.ConnectionPool;

public class BatchDetailsDaoImpl implements BatchDetailsDao {

    private final DataSource dataSource;

    private BatchDetailsDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static BatchDetailsDaoImpl of() {
        return new BatchDetailsDaoImpl(
                ConnectionPool.getDataSource());
    }

    @Override
    public Map<String, Object> getMicrDetails(String chequeNumber) {

        String sql = """
                SELECT
                    c.cheque_number,
                    c.micr_code AS current_micr,
                    r.old_value AS old_micr,
                    r.new_value AS corrected_micr
                FROM inward_cheque c
                LEFT JOIN inward_micr_repair_history r
                    ON c.cheque_number = r.cheque_no
                WHERE c.cheque_number = ?
                """;

        Map<String, Object> micrDetails = new HashMap<>();

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setString(1, chequeNumber);

            try (ResultSet resultSet =
                    statement.executeQuery()) {

                if (resultSet.next()) {

                    micrDetails.put(
                            "chequeNumber",
                            resultSet.getString("cheque_number"));

                    micrDetails.put(
                            "currentMicr",
                            resultSet.getString("current_micr"));

                    micrDetails.put(
                            "oldMicr",
                            resultSet.getString("old_micr"));

                    micrDetails.put(
                            "correctedMicr",
                            resultSet.getString("corrected_micr"));
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error retrieving MICR details for cheque: "
                            + chequeNumber,
                    e);
        }

        return micrDetails;
    }

    @Override
    public List<Map<String, Object>> getChequesByBatchId(
            Long batchId) {

        String sql = """
                SELECT
                    cheque_number,
                    batch_id,
                    account_number,
                    payee_name,
                    amount,
                    micr_code,
                    cheque_date
                FROM inward_cheque
                WHERE batch_id = ?
                ORDER BY cheque_number
                """;

        List<Map<String, Object>> cheques =
                new java.util.ArrayList<>();

        try (Connection connection =
                    dataSource.getConnection();
                PreparedStatement statement =
                    connection.prepareStatement(sql)) {

            statement.setLong(1, batchId);

            try (ResultSet resultSet =
                    statement.executeQuery()) {

                while (resultSet.next()) {

                    Map<String, Object> cheque =
                            new HashMap<>();

                    cheque.put(
                            "chequeNumber",
                            resultSet.getString(
                                    "cheque_number"));

                    cheque.put(
                            "batchId",
                            resultSet.getLong("batch_id"));

                    cheque.put(
                            "accountNumber",
                            resultSet.getString(
                                    "account_number"));

                    /*
                     * Database column is payee_name.
                     * Existing Java Map key remains
                     * drawerName so existing controller/ZUL
                     * does not break.
                     */
                    cheque.put(
                            "drawerName",
                            resultSet.getString(
                                    "payee_name"));

                    cheque.put(
                            "amount",
                            resultSet.getBigDecimal(
                                    "amount"));

                    cheque.put(
                            "micrCode",
                            resultSet.getString(
                                    "micr_code"));

                    cheque.put(
                            "chequeDate",
                            resultSet.getDate(
                                    "cheque_date"));

                    cheques.add(cheque);
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error retrieving cheques for batch: "
                            + batchId,
                    e);
        }

        return cheques;
    }

    @Override
    public Map<String, Object> getDataEntryDetails(
            String chequeNumber) {

        Map<String, Object> details =
                new HashMap<>();

        String chequeSql = """
                SELECT
                    cheque_number,
                    account_number,
                    amount,
                    cheque_date,
                    payee_name
                FROM inward_cheque
                WHERE cheque_number = ?
                """;

        String historySql = """
                SELECT
                    field_name,
                    new_value
                FROM inward_cheque_dataentry_history
                WHERE cheque_no = ?
                ORDER BY history_id
                """;

        try (Connection connection =
                    dataSource.getConnection()) {

            // =====================================================
            // 1. GET OLD VALUES FROM inward_cheque
            // =====================================================

            try (PreparedStatement statement =
                    connection.prepareStatement(chequeSql)) {

                statement.setString(1, chequeNumber);

                try (ResultSet rs =
                        statement.executeQuery()) {

                    if (rs.next()) {

                        details.put(
                                "chequeNumber",
                                rs.getString(
                                        "cheque_number"));

                        details.put(
                                "oldAccountNumber",
                                rs.getString(
                                        "account_number"));

                        details.put(
                                "oldAmount",
                                rs.getString(
                                        "amount"));

                        details.put(
                                "oldChequeDate",
                                rs.getString(
                                        "cheque_date"));

                        details.put(
                                "oldDrawerName",
                                rs.getString(
                                        "payee_name"));
                    }
                }
            }

            // =====================================================
            // 2. GET NEW VALUES FROM DATA ENTRY HISTORY
            // =====================================================

            try (PreparedStatement statement =
                    connection.prepareStatement(historySql)) {

                statement.setString(1, chequeNumber);

                try (ResultSet rs =
                        statement.executeQuery()) {

                    while (rs.next()) {

                        String fieldName =
                                rs.getString("field_name");

                        String newValue =
                                rs.getString("new_value");

                        if ("ACCOUNT_NUMBER".equals(
                                fieldName)) {

                            details.put(
                                    "newAccountNumber",
                                    newValue);

                        } else if ("AMOUNT".equals(
                                fieldName)) {

                            details.put(
                                    "newAmount",
                                    newValue);

                        } else if ("CHEQUE_DATE".equals(
                                fieldName)) {

                            details.put(
                                    "newChequeDate",
                                    newValue);
                        }
                    }
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error retrieving Data Entry details "
                            + "for cheque: "
                            + chequeNumber,
                    e);
        }

        return details;
    }

    @Override
    public Map<String, Object> getCbsValidation(
            String chequeNumber) {

        String sql = """
                SELECT
                    c.cheque_number,
                    c.account_number,
                    c.cheque_date,

                    COALESCE(
                        (
                            SELECT h.new_value
                            FROM inward_cheque_dataentry_history h
                            WHERE h.cheque_no = c.cheque_number
                              AND h.field_name = 'AMOUNT'
                            ORDER BY h.history_id DESC
                            LIMIT 1
                        ),
                        CAST(c.amount AS VARCHAR)
                    ) AS cheque_amount,

                    a.account_number AS master_account_number,
                    a.account_status,
                    a.available_balance

                FROM inward_cheque c

                LEFT JOIN inward_account_master a
                    ON a.account_number =
                       c.account_number

                WHERE c.cheque_number = ?
                """;

        Map<String, Object> cbsDetails =
                new HashMap<>();

        try (Connection connection =
                    dataSource.getConnection();
                PreparedStatement statement =
                    connection.prepareStatement(sql)) {

            statement.setString(1, chequeNumber);

            try (ResultSet resultSet =
                    statement.executeQuery()) {

                if (resultSet.next()) {

                    cbsDetails.put(
                            "chequeNumber",
                            resultSet.getString(
                                    "cheque_number"));

                    cbsDetails.put(
                            "accountNumber",
                            resultSet.getString(
                                    "account_number"));

                    cbsDetails.put(
                            "chequeDate",
                            resultSet.getDate(
                                    "cheque_date"));

                    cbsDetails.put(
                            "chequeAmount",
                            resultSet.getBigDecimal(
                                    "cheque_amount"));

                    cbsDetails.put(
                            "accountStatus",
                            resultSet.getString(
                                    "account_status"));

                    cbsDetails.put(
                            "availableBalance",
                            resultSet.getBigDecimal(
                                    "available_balance"));
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error retrieving CBS validation details "
                            + "for cheque: "
                            + chequeNumber,
                    e);
        }

        return cbsDetails;
    }

    @Override
    public void saveCheckerDecision(
            String chequeNumber,
            String status,
            String rejectionReasonCode,
            String returnReasonCode,
            Integer checkerId,
            String checkerAction,
            String remarks) {

        String sql = """
                UPDATE inward_cheque_status_history
                SET
                    status = ?,
                    rejection_reason_code = ?,
                    return_reason_code = ?,
                    checker_id = ?,
                    checker_action = ?,
                    checker_action_on =
                        CURRENT_TIMESTAMP,
                    remarks = ?
                WHERE status_history_id = (
                    SELECT status_history_id
                    FROM inward_cheque_status_history
                    WHERE cheque_number = ?
                      AND status = 'SENT_TO_CHECKER'
                    ORDER BY status_history_id DESC
                    LIMIT 1
                )
                """;

        try (Connection connection =
                    dataSource.getConnection();
                PreparedStatement ps =
                    connection.prepareStatement(sql)) {

            // status
            ps.setString(1, status);

            // rejection_reason_code
            if (rejectionReasonCode == null) {

                ps.setNull(
                        2,
                        java.sql.Types.VARCHAR);

            } else {

                ps.setString(
                        2,
                        rejectionReasonCode);
            }

            // return_reason_code
            if (returnReasonCode == null) {

                ps.setNull(
                        3,
                        java.sql.Types.VARCHAR);

            } else {

                ps.setString(
                        3,
                        returnReasonCode);
            }

            // checker_id
            if (checkerId == null) {

                ps.setNull(
                        4,
                        java.sql.Types.INTEGER);

            } else {

                ps.setInt(
                        4,
                        checkerId);
            }

            // checker_action
            if (checkerAction == null) {

                ps.setNull(
                        5,
                        java.sql.Types.VARCHAR);

            } else {

                ps.setString(
                        5,
                        checkerAction);
            }

            // remarks
            if (remarks == null
                    || remarks.trim().isEmpty()) {

                ps.setNull(
                        6,
                        java.sql.Types.VARCHAR);

            } else {

                ps.setString(
                        6,
                        remarks.trim());
            }

            // cheque_number
            ps.setString(
                    7,
                    chequeNumber);

            int updatedRows =
                    ps.executeUpdate();

            if (updatedRows == 0) {

                throw new RuntimeException(
                        "No SENT_TO_CHECKER record found "
                                + "for cheque "
                                + chequeNumber
                                + " and checker "
                                + checkerId);
            }

        } catch (java.sql.SQLException e) {

            throw new RuntimeException(
                    "Failed to save checker decision "
                            + "for cheque "
                            + chequeNumber,
                    e);
        }
    }

    @Override
    public boolean completeVerification(Long batchId, Integer checkerId) {

        if (batchId == null) {
            throw new IllegalArgumentException("batchId cannot be null");
        }

        Connection connection = null;

        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);

            // =========================================================
            // 1. Check if ANY cheque in current batch has RETURN_TO_MAKER
            // =========================================================
            String checkReturnedSql = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM inward_cheque_status_history sh
                        JOIN inward_cheque c ON c.cheque_number = sh.cheque_number
                        WHERE c.batch_id = ?
                          AND sh.status = 'RETURN_TO_MAKER'
                    )
                    """;

            boolean hasReturnedCheques = false;

            try (PreparedStatement ps = connection.prepareStatement(checkReturnedSql)) {
                ps.setLong(1, batchId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        hasReturnedCheques = rs.getBoolean(1);
                    }
                }
            }

            // =========================================================
            // CASE A: NO cheques returned to maker
            // Do not perform return-to-maker transition.
            // =========================================================
            if (!hasReturnedCheques) {
                connection.commit();
                return false;
            }

            // =========================================================
            // CASE B: At least one cheque returned to maker
            // Step 1: Find Maker ID from inward_batch_history
            // =========================================================
            String findMakerSql = """
                    SELECT changed_by
                    FROM inward_batch_history
                    WHERE batch_id = ?
                      AND batch_status IN ('SENT_TO_CHECKER', 'DATA_ENTRY', 'DATA_ENTRY_COMPLETED', 'LOCKED')
                      AND changed_by IS NOT NULL
                    ORDER BY batch_history_id DESC
                    LIMIT 1
                    """;

            Integer makerId = null;

            try (PreparedStatement ps = connection.prepareStatement(findMakerSql)) {
                ps.setLong(1, batchId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int id = rs.getInt("changed_by");
                        if (!rs.wasNull()) {
                            makerId = id;
                        }
                    }
                }
            }

            if (makerId == null) {
                throw new IllegalStateException(
                        "Maker ID could not be determined from batch history for batch " + batchId);
            }

            // =========================================================
            // Step 2: Insert RETURN_TO_MAKER record in inward_batch_history
            // =========================================================
            String insertHistorySql = """
                    INSERT INTO inward_batch_history
                    (batch_id, batch_status, changed_on, changed_by, reason, remarks)
                    VALUES (?, 'RETURN_TO_MAKER', CURRENT_TIMESTAMP, ?, 'Returned to Maker by Checker', 'Cheque(s) returned for reprocessing')
                    """;

            try (PreparedStatement ps = connection.prepareStatement(insertHistorySql)) {
                ps.setLong(1, batchId);
                ps.setInt(2, makerId);

                int historyRows = ps.executeUpdate();
                if (historyRows == 0) {
                    throw new IllegalStateException(
                            "Failed to insert RETURN_TO_MAKER into inward_batch_history for batch " + batchId);
                }
            }

            // =========================================================
            // Step 3: Unlock Checker (lock_status = 'UNLOCKED') in inward_batch_lock
            // Must unlock checker BEFORE locking maker so that the unique
            // constraint uq_inward_batch_active_lock (only one 'LOCKED' per batch_id)
            // is not violated.
            // =========================================================
            if (checkerId != null) {
                String unlockCheckerSql = """
                        UPDATE inward_batch_lock
                        SET lock_status = 'UNLOCKED',
                            locked_time = CURRENT_TIMESTAMP
                        WHERE batch_id = ?
                          AND user_id = ?
                        """;

                try (PreparedStatement ps = connection.prepareStatement(unlockCheckerSql)) {
                    ps.setLong(1, batchId);
                    ps.setInt(2, checkerId);

                    int checkerLockRows = ps.executeUpdate();
                    if (checkerLockRows == 0) {
                        throw new IllegalStateException(
                                "No lock record found for Checker " + checkerId + " in batch " + batchId);
                    }
                }
            }

            // =========================================================
            // Step 4: Lock Maker (lock_status = 'LOCKED') in inward_batch_lock
            // =========================================================
            String lockMakerSql = """
                    UPDATE inward_batch_lock
                    SET lock_status = 'LOCKED',
                        locked_time = CURRENT_TIMESTAMP
                    WHERE batch_id = ?
                      AND user_id = ?
                    """;

            try (PreparedStatement ps = connection.prepareStatement(lockMakerSql)) {
                ps.setLong(1, batchId);
                ps.setInt(2, makerId);

                int makerLockRows = ps.executeUpdate();
                if (makerLockRows == 0) {
                    throw new IllegalStateException(
                            "No lock record found for Maker " + makerId + " in batch " + batchId);
                }
            }

            connection.commit();
            return true;

        } catch (Exception e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (java.sql.SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            throw new RuntimeException("Error executing complete verification for batch " + batchId, e);
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (java.sql.SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }
}