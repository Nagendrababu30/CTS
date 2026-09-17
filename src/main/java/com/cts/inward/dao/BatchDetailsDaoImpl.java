package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, chequeNumber);

            try (ResultSet resultSet = statement.executeQuery()) {

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

        if (batchId == null) {
            return new java.util.ArrayList<>();
        }

        // Check if this batch is a Re-Verify batch (has cheques returned to maker by checker)
        String checkReverifySql = """
                SELECT EXISTS (
                    SELECT 1 FROM inward_cheque c
                    JOIN inward_cheque_status_history sh ON sh.cheque_number = c.cheque_number
                    WHERE c.batch_id = ?
                      AND (
                          sh.status = 'RETURN_TO_MAKER'
                          OR sh.checker_action = 'Sent Back'
                          OR (sh.return_reason_code IS NOT NULL AND (sh.return_reason_code LIKE 'CR-%' OR sh.return_reason_code = 'OTHER'))
                      )
                ) AS is_reverify
                """;

        boolean isReverifyBatch = false;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(checkReverifySql)) {
            ps.setLong(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    isReverifyBatch = rs.getBoolean("is_reverify");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String sql;
        if (isReverifyBatch) {
            // In Re-Verify batches, ONLY load cheques that were returned to maker by checker
            sql = """
                SELECT
                    c.cheque_number,
                    c.batch_id,
                    c.account_number,
                    c.payee_name,
                    c.amount,
                    c.micr_code,
                    c.cheque_date,
                    latest.status AS cheque_status,
                    latest.return_reason_code,
                    r.description AS return_reason_description,
                    latest.remarks AS maker_remarks,
                    ocr.micr_code AS ocr_micr_code
                FROM inward_cheque c
                LEFT JOIN LATERAL (
                    SELECT h.status, h.return_reason_code, h.remarks
                    FROM inward_cheque_status_history h
                    WHERE h.cheque_number = c.cheque_number
                    ORDER BY h.status_history_id DESC
                    LIMIT 1
                ) latest ON TRUE
                LEFT JOIN inward_cheque_return_reason r ON latest.return_reason_code = r.return_reason_code
                LEFT JOIN LATERAL (
                    SELECT o.micr_code
                    FROM public.ocr_cheque_data o
                    WHERE o.cheque_number = c.cheque_number OR o.inward_cheque_id = c.inward_cheque_id
                    ORDER BY o.inward_cheque_id DESC
                    LIMIT 1
                ) ocr ON TRUE
                WHERE c.batch_id = ?
                  AND EXISTS (
                      SELECT 1 FROM inward_cheque_status_history sh
                      WHERE sh.cheque_number = c.cheque_number
                        AND (
                            sh.status = 'RETURN_TO_MAKER'
                            OR sh.checker_action = 'Sent Back'
                            OR (sh.return_reason_code IS NOT NULL AND (sh.return_reason_code LIKE 'CR-%' OR sh.return_reason_code = 'OTHER'))
                        )
                  )
                ORDER BY c.cheque_number
                """;
        } else {
            // Normal batch: load all cheques
            sql = """
                SELECT
                    c.cheque_number,
                    c.batch_id,
                    c.account_number,
                    c.payee_name,
                    c.amount,
                    c.micr_code,
                    c.cheque_date,
                    latest.status AS cheque_status,
                    latest.return_reason_code,
                    r.description AS return_reason_description,
                    latest.remarks AS maker_remarks,
                    ocr.micr_code AS ocr_micr_code
                FROM inward_cheque c
                LEFT JOIN LATERAL (
                    SELECT h.status, h.return_reason_code, h.remarks
                    FROM inward_cheque_status_history h
                    WHERE h.cheque_number = c.cheque_number
                    ORDER BY h.status_history_id DESC
                    LIMIT 1
                ) latest ON TRUE
                LEFT JOIN inward_cheque_return_reason r ON latest.return_reason_code = r.return_reason_code
                LEFT JOIN LATERAL (
                    SELECT o.micr_code
                    FROM public.ocr_cheque_data o
                    WHERE o.cheque_number = c.cheque_number OR o.inward_cheque_id = c.inward_cheque_id
                    ORDER BY o.inward_cheque_id DESC
                    LIMIT 1
                ) ocr ON TRUE
                WHERE c.batch_id = ?
                ORDER BY c.cheque_number
                """;
        }

        List<Map<String, Object>> cheques = new java.util.ArrayList<>();

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, batchId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {

                    Map<String, Object> cheque = new HashMap<>();

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

                    cheque.put(
                            "status",
                            resultSet.getString(
                                    "cheque_status"));

                    cheque.put(
                            "returnReasonCode",
                            resultSet.getString(
                                    "return_reason_code"));

                    cheque.put(
                            "returnReasonDescription",
                            resultSet.getString(
                                    "return_reason_description"));

                    cheque.put(
                            "makerRemarks",
                            resultSet.getString(
                                    "maker_remarks"));

                    cheque.put(
                            "ocrMicrCode",
                            resultSet.getString(
                                    "ocr_micr_code"));

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

        Map<String, Object> details = new HashMap<>();

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

        try (Connection connection = dataSource.getConnection()) {

            // =====================================================
            // 1. GET OLD VALUES FROM inward_cheque
            // =====================================================

            try (PreparedStatement statement = connection.prepareStatement(chequeSql)) {

                statement.setString(1, chequeNumber);

                try (ResultSet rs = statement.executeQuery()) {

                    if (rs.next()) {

                        details.put(
                                "chequeNumber",
                                rs.getString(
                                        "cheque_number"));

                        details.put(
                                "oldChequeNumber",
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

            try (PreparedStatement statement = connection.prepareStatement(historySql)) {

                statement.setString(1, chequeNumber);

                try (ResultSet rs = statement.executeQuery()) {

                    while (rs.next()) {

                        String fieldName = rs.getString("field_name");

                        String newValue = rs.getString("new_value");

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

                        } else if ("CHEQUE_NUMBER".equals(
                                fieldName)) {

                            details.put(
                                    "newChequeNumber",
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
                    a.available_balance,

                    (
                        SELECT COUNT(*)
                        FROM inward_cheque ic2
                        WHERE ic2.cheque_number = c.cheque_number
                    ) AS dup_count

                FROM inward_cheque c

                LEFT JOIN inward_account_master a
                    ON a.account_number =
                       c.account_number

                WHERE c.cheque_number = ?
                """;

        Map<String, Object> cbsDetails = new HashMap<>();

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, chequeNumber);

            try (ResultSet resultSet = statement.executeQuery()) {

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

                    int dupCount = resultSet.getInt("dup_count");
                    cbsDetails.put("isDuplicate", dupCount > 1);
                    cbsDetails.put("duplicateCount", dupCount);
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
    public List<Map<String, String>> getCheckerReturnReasons() {
        List<Map<String, String>> reasons = new ArrayList<>();
        String sql = """
                SELECT return_reason_code, description
                FROM public.inward_cheque_return_reason
                WHERE applicable_role = 'CHECKER'
                  AND (return_reason_code LIKE 'CR-%' OR return_reason_code = 'OTHER')
                ORDER BY return_reason_code
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, String> map = new HashMap<>();
                String code = rs.getString("return_reason_code");
                String desc = rs.getString("description");
                map.put("code", code);
                map.put("return_reason_code", code);
                map.put("description", desc);
                reasons.add(map);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch checker return reasons", e);
        }
        return reasons;
    }

    @Override
    public List<Map<String, String>> getCheckerRejectionReasons() {
        List<Map<String, String>> reasons = new ArrayList<>();
        String sql = """
                SELECT rejection_reason_code, description
                FROM public.inward_cheque_rejection_reason
                ORDER BY rejection_reason_code
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, String> map = new HashMap<>();
                String code = rs.getString("rejection_reason_code");
                String desc = rs.getString("description");
                map.put("code", code);
                map.put("rejection_reason_code", code);
                map.put("description", desc);
                reasons.add(map);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch checker rejection reasons", e);
        }
        return reasons;
    }

    @Override
    public List<Map<String, String>> getMakerReturnReasons(String chequeNumber) {
        List<Map<String, String>> list = new ArrayList<>();
        if (chequeNumber == null || chequeNumber.trim().isEmpty()) {
            return list;
        }
        String sql = """
                SELECT DISTINCT h.return_reason_code, r.description, h.remarks
                FROM inward_cheque_status_history h
                LEFT JOIN inward_cheque_return_reason r ON h.return_reason_code = r.return_reason_code
                WHERE h.cheque_number = ?
                  AND h.status = 'RETURN_BY_MAKER'
                  AND h.return_reason_code IS NOT NULL
                ORDER BY h.return_reason_code
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, chequeNumber.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> map = new HashMap<>();
                    map.put("returnReasonCode", rs.getString("return_reason_code"));
                    map.put("description", rs.getString("description"));
                    map.put("remarks", rs.getString("remarks"));
                    list.add(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void saveCheckerDecision(
            String chequeNumber,
            String status,
            List<String> rejectionReasonCodes,
            List<String> returnReasonCodes,
            Integer checkerId,
            String checkerAction,
            String remarks) {

        String primaryRejectionCode = (rejectionReasonCodes != null && !rejectionReasonCodes.isEmpty())
                ? rejectionReasonCodes.get(0) : null;
        String primaryReturnCode = (returnReasonCodes != null && !returnReasonCodes.isEmpty())
                ? returnReasonCodes.get(0) : null;

        StringBuilder remarksBuilder = new StringBuilder();
        if (rejectionReasonCodes != null && rejectionReasonCodes.size() > 1) {
            remarksBuilder.append("[Rejection Reasons: ").append(String.join(", ", rejectionReasonCodes)).append("] ");
        }
        if (returnReasonCodes != null && returnReasonCodes.size() > 1) {
            remarksBuilder.append("[Return Reasons: ").append(String.join(", ", returnReasonCodes)).append("] ");
        }
        if (remarks != null && !remarks.trim().isEmpty()) {
            remarksBuilder.append(remarks.trim());
        }
        String finalRemarks = remarksBuilder.length() > 0 ? remarksBuilder.toString() : null;

        String updateSql = """
                UPDATE inward_cheque_status_history
                SET
                    status = ?,
                    rejection_reason_code = ?,
                    return_reason_code = ?,
                    checker_id = ?,
                    checker_action = ?,
                    checker_action_on = CURRENT_TIMESTAMP,
                    remarks = ?
                WHERE status_history_id = (
                    SELECT status_history_id
                    FROM inward_cheque_status_history
                    WHERE cheque_number = ?
                    ORDER BY status_history_id DESC
                    LIMIT 1
                )
                """;

        String insertSql = """
                INSERT INTO inward_cheque_status_history
                (cheque_number, status, rejection_reason_code, return_reason_code,
                 checker_id, checker_action, checker_action_on, remarks)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)
                """;

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int updatedRows = 0;
                try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                    ps.setString(1, status);
                    if (primaryRejectionCode != null) ps.setString(2, primaryRejectionCode);
                    else ps.setNull(2, java.sql.Types.VARCHAR);
                    if (primaryReturnCode != null) ps.setString(3, primaryReturnCode);
                    else ps.setNull(3, java.sql.Types.VARCHAR);
                    if (checkerId != null) ps.setInt(4, checkerId);
                    else ps.setNull(4, java.sql.Types.INTEGER);
                    if (checkerAction != null) ps.setString(5, checkerAction);
                    else ps.setNull(5, java.sql.Types.VARCHAR);
                    if (finalRemarks != null) ps.setString(6, finalRemarks);
                    else ps.setNull(6, java.sql.Types.VARCHAR);
                    ps.setString(7, chequeNumber);
                    updatedRows = ps.executeUpdate();
                }

                if (updatedRows == 0) {
                    try (PreparedStatement insertPs = connection.prepareStatement(insertSql)) {
                        insertPs.setString(1, chequeNumber);
                        insertPs.setString(2, status);
                        if (primaryRejectionCode != null) insertPs.setString(3, primaryRejectionCode);
                        else insertPs.setNull(3, java.sql.Types.VARCHAR);
                        if (primaryReturnCode != null) insertPs.setString(4, primaryReturnCode);
                        else insertPs.setNull(4, java.sql.Types.VARCHAR);
                        if (checkerId != null) insertPs.setInt(5, checkerId);
                        else insertPs.setNull(5, java.sql.Types.INTEGER);
                        if (checkerAction != null) insertPs.setString(6, checkerAction);
                        else insertPs.setNull(6, java.sql.Types.VARCHAR);
                        if (finalRemarks != null) insertPs.setString(7, finalRemarks);
                        else insertPs.setNull(7, java.sql.Types.VARCHAR);
                        insertPs.executeUpdate();
                    }
                }

                if (rejectionReasonCodes != null && rejectionReasonCodes.size() > 1) {
                    for (int i = 1; i < rejectionReasonCodes.size(); i++) {
                        String code = rejectionReasonCodes.get(i);
                        if (code == null || code.trim().isEmpty()) continue;
                        try (PreparedStatement insertPs = connection.prepareStatement(insertSql)) {
                            insertPs.setString(1, chequeNumber);
                            insertPs.setString(2, status);
                            insertPs.setString(3, code.trim());
                            insertPs.setNull(4, java.sql.Types.VARCHAR);
                            if (checkerId != null) insertPs.setInt(5, checkerId);
                            else insertPs.setNull(5, java.sql.Types.INTEGER);
                            if (checkerAction != null) insertPs.setString(6, checkerAction);
                            else insertPs.setNull(6, java.sql.Types.VARCHAR);
                            if (finalRemarks != null) insertPs.setString(7, finalRemarks);
                            else insertPs.setNull(7, java.sql.Types.VARCHAR);
                            insertPs.executeUpdate();
                        }
                    }
                }

                if (returnReasonCodes != null && returnReasonCodes.size() > 1) {
                    for (int i = 1; i < returnReasonCodes.size(); i++) {
                        String code = returnReasonCodes.get(i);
                        if (code == null || code.trim().isEmpty()) continue;
                        try (PreparedStatement insertPs = connection.prepareStatement(insertSql)) {
                            insertPs.setString(1, chequeNumber);
                            insertPs.setString(2, status);
                            insertPs.setNull(3, java.sql.Types.VARCHAR);
                            insertPs.setString(4, code.trim());
                            if (checkerId != null) insertPs.setInt(5, checkerId);
                            else insertPs.setNull(5, java.sql.Types.INTEGER);
                            if (checkerAction != null) insertPs.setString(6, checkerAction);
                            else insertPs.setNull(6, java.sql.Types.VARCHAR);
                            if (finalRemarks != null) insertPs.setString(7, finalRemarks);
                            else insertPs.setNull(7, java.sql.Types.VARCHAR);
                            insertPs.executeUpdate();
                        }
                    }
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save checker decision for cheque " + chequeNumber, e);
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
            // 1. Check if ANY cheque in current batch has LATEST status = RETURN_TO_MAKER
            // =========================================================
            String checkReturnedSql = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM inward_cheque c
                        JOIN LATERAL (
                            SELECT sh.status
                            FROM inward_cheque_status_history sh
                            WHERE sh.cheque_number = c.cheque_number
                            ORDER BY sh.status_history_id DESC
                            LIMIT 1
                        ) latest ON TRUE
                        WHERE c.batch_id = ?
                          AND latest.status = 'RETURN_TO_MAKER'
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
            // Complete the batch: Unlock Checker & mark batch COMPLETED
            // =========================================================
            if (!hasReturnedCheques) {
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
                        ps.executeUpdate();
                    }
                }

                String updateCompletedHistorySql = """
                        UPDATE inward_batch_history
                        SET batch_status = 'COMPLETED',
                            changed_on = CURRENT_TIMESTAMP,
                            changed_by = ?,
                            reason = 'Verification completed by Checker',
                            remarks = 'All cheques verified successfully'
                        WHERE batch_id = ?
                        """;

                int updatedComp = 0;
                try (PreparedStatement ps = connection.prepareStatement(updateCompletedHistorySql)) {
                    if (checkerId != null) {
                        ps.setInt(1, checkerId);
                    } else {
                        ps.setNull(1, java.sql.Types.INTEGER);
                    }
                    ps.setLong(2, batchId);
                    updatedComp = ps.executeUpdate();
                }

                if (updatedComp == 0) {
                    String insertCompletedHistorySql = """
                            INSERT INTO inward_batch_history
                            (batch_id, batch_status, changed_on, changed_by, reason, remarks)
                            VALUES (?, 'COMPLETED', CURRENT_TIMESTAMP, ?, 'Verification completed by Checker', 'All cheques verified successfully')
                            """;

                    try (PreparedStatement ps = connection.prepareStatement(insertCompletedHistorySql)) {
                        ps.setLong(1, batchId);
                        if (checkerId != null) {
                            ps.setInt(2, checkerId);
                        } else {
                            ps.setNull(2, java.sql.Types.INTEGER);
                        }
                        ps.executeUpdate();
                    }
                }

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

            // Fallback: Check inward_batch_lock for Maker
            if (makerId == null) {
                String findMakerFromLockSql = """
                        SELECT bl.user_id
                        FROM inward_batch_lock bl
                        JOIN public."user" u ON u.user_id = bl.user_id AND u.role_id = 1
                        WHERE bl.batch_id = ?
                        ORDER BY bl.locked_time DESC, bl.lock_id DESC
                        LIMIT 1
                        """;
                try (PreparedStatement ps = connection.prepareStatement(findMakerFromLockSql)) {
                    ps.setLong(1, batchId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            makerId = rs.getInt("user_id");
                        }
                    }
                }
            }

            if (makerId == null) {
                throw new IllegalStateException(
                        "Maker ID could not be determined from batch history for batch " + batchId);
            }

            // =========================================================
            // Step 2: Update/Insert RETURN_TO_MAKER record in inward_batch_history
            // =========================================================
            String updateReturnHistorySql = """
                    UPDATE inward_batch_history
                    SET batch_status = 'RETURN_TO_MAKER',
                        changed_on = CURRENT_TIMESTAMP,
                        changed_by = ?,
                        reason = 'Returned to Maker by Checker',
                        remarks = 'Cheque(s) returned for reprocessing'
                    WHERE batch_id = ?
                    """;

            int returnHistUpdated = 0;
            try (PreparedStatement ps = connection.prepareStatement(updateReturnHistorySql)) {
                ps.setInt(1, makerId);
                ps.setLong(2, batchId);
                returnHistUpdated = ps.executeUpdate();
            }

            if (returnHistUpdated == 0) {
                String insertHistorySql = """
                        INSERT INTO inward_batch_history
                        (batch_id, batch_status, changed_on, changed_by, reason, remarks)
                        VALUES (?, 'RETURN_TO_MAKER', CURRENT_TIMESTAMP, ?, 'Returned to Maker by Checker', 'Cheque(s) returned for reprocessing')
                        """;

                try (PreparedStatement ps = connection.prepareStatement(insertHistorySql)) {
                    ps.setLong(1, batchId);
                    ps.setInt(2, makerId);
                    ps.executeUpdate();
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
                    ps.executeUpdate();
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

            int makerLockRows = 0;
            try (PreparedStatement ps = connection.prepareStatement(lockMakerSql)) {
                ps.setLong(1, batchId);
                ps.setInt(2, makerId);
                makerLockRows = ps.executeUpdate();
            }

            if (makerLockRows == 0) {
                String insertMakerLockSql = """
                        INSERT INTO inward_batch_lock
                        (batch_id, user_id, locked_time, lock_status)
                        VALUES (?, ?, CURRENT_TIMESTAMP, 'LOCKED')
                        """;
                try (PreparedStatement ps = connection.prepareStatement(insertMakerLockSql)) {
                    ps.setLong(1, batchId);
                    ps.setInt(2, makerId);
                    ps.executeUpdate();
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