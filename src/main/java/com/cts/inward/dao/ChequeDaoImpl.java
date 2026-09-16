package com.cts.inward.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.dto.ReturnReasonDto;
import com.cts.inward.model.InwardCheque;
import com.cts.inward.model.NpciChequeData;

public class ChequeDaoImpl implements ChequeDao {

    private final DataSource dataSource;

    private ChequeDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static ChequeDao of() {
        return new ChequeDaoImpl(ConnectionPool.getDataSource());
    }

    @Override
    public void saveCheque(NpciChequeData cheque) {

        String sql =
                "INSERT INTO inward_cheque "
                + "(cheque_number, batch_id, account_number, drawer_name, "
                + "amount, micr_code, cheque_date, presenting_date, "
                + "payee_name, payee_account_number, amount_in_words) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, cheque.getChequeNumber());
            statement.setLong(2, cheque.getBatchId());
            statement.setString(3, cheque.getAccountNumber());
            statement.setString(4, cheque.getDrawerName());
            statement.setBigDecimal(5, cheque.getChequeAmount());
            statement.setString(6, cheque.getMicrCode());
            statement.setObject(7, cheque.getChequeDate());
            statement.setObject(8, cheque.getPresentingDate());
            statement.setString(9, cheque.getPayeeName());
            statement.setString(10, cheque.getPayeeAccountNumber());
            statement.setString(11, cheque.getAmountInWords());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to save cheque: " + cheque.getChequeNumber(), e);
        }
    }

    @Override
    public List<InwardCheque> getChequesForBatch(String batchId) {
    	String sql =
    	        "SELECT c.cheque_number, c.batch_id, c.drawer_name, c.micr_code, c.presenting_date, "
    	        + "COALESCE(acc.new_value, c.account_number) AS account_number, "
    	        + "COALESCE(amt.new_value, CAST(c.amount AS VARCHAR)) AS amount_str, "
    	        + "COALESCE(dt.new_value, CAST(c.cheque_date AS VARCHAR)) AS cheque_date_str, "
    	        + "latest.status AS latest_status "
    	        + "FROM public.inward_cheque c "
    	        + "LEFT JOIN LATERAL ( "
    	        + "    SELECT new_value FROM inward_cheque_dataentry_history "
    	        + "    WHERE cheque_no = c.cheque_number AND field_name = 'ACCOUNT_NUMBER' "
    	        + "    ORDER BY history_id DESC LIMIT 1 "
    	        + ") acc ON TRUE "
    	        + "LEFT JOIN LATERAL ( "
    	        + "    SELECT new_value FROM inward_cheque_dataentry_history "
    	        + "    WHERE cheque_no = c.cheque_number AND field_name = 'AMOUNT' "
    	        + "    ORDER BY history_id DESC LIMIT 1 "
    	        + ") amt ON TRUE "
    	        + "LEFT JOIN LATERAL ( "
    	        + "    SELECT new_value FROM inward_cheque_dataentry_history "
    	        + "    WHERE cheque_no = c.cheque_number AND field_name = 'CHEQUE_DATE' "
    	        + "    ORDER BY history_id DESC LIMIT 1 "
    	        + ") dt ON TRUE "
    	        + "LEFT JOIN LATERAL ( "
    	        + "    SELECT h.status "
    	        + "    FROM public.inward_cheque_status_history h "
    	        + "    WHERE h.cheque_number = c.cheque_number "
    	        + "    ORDER BY h.status_history_id DESC "
    	        + "    LIMIT 1 "
    	        + ") latest ON TRUE "
    	        + "WHERE c.batch_id = ? "
    	        + "AND COALESCE(latest.status, '') NOT IN ('ACCEPT', 'REJECT', 'RETURN_BY_MAKER') "
    	        + "ORDER BY c.cheque_number";

        List<InwardCheque> cheques = new ArrayList<>();
        boolean batchReturned = isBatchReturned(Long.parseLong(batchId));

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, Long.parseLong(batchId));

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    String chqNo = resultSet.getString("cheque_number");
                    String latestStatus = resultSet.getString("latest_status");

                    if ("RETURN_TO_MAKER".equalsIgnoreCase(latestStatus)) {
                        if (chequeNeedsMicrRepair(chqNo)) {
                            // Needs MICR repair first, not ready for Data Entry
                            continue;
                        }
                        if (!chequeNeedsDataEntry(chqNo)) {
                            // Only needed MICR repair, not Data Entry
                            continue;
                        }
                    } else if ("MICR_REPAIRED".equalsIgnoreCase(latestStatus)) {
                        if (batchReturned && !chequeNeedsDataEntry(chqNo)) {
                            // Cheque was returned only for MICR repair and is now repaired
                            continue;
                        }
                    }

                    String amtStr = resultSet.getString("amount_str");
                    BigDecimal amount = null;
                    if (amtStr != null && !amtStr.trim().isEmpty()) {
                        try {
                            amount = new BigDecimal(amtStr.trim());
                        } catch (Exception e) {
                            amount = BigDecimal.ZERO;
                        }
                    }

                    String dateStr = resultSet.getString("cheque_date_str");
                    LocalDate chequeDate = null;
                    if (dateStr != null && !dateStr.trim().isEmpty()) {
                        try {
                            chequeDate = LocalDate.parse(dateStr.trim());
                        } catch (Exception e) {
                            // fallback null
                        }
                    }

                    InwardCheque cheque = InwardCheque.of(
                            chqNo,
                            resultSet.getString("batch_id"),
                            resultSet.getString("account_number"),
                            resultSet.getString("drawer_name"),
                            amount,
                            resultSet.getString("micr_code"),
                            chequeDate,
                            resultSet.getDate("presenting_date") != null
                                    ? resultSet.getDate("presenting_date").toLocalDate() : null);

                    cheques.add(cheque);
                }
            }

            return cheques;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to retrieve cheques for batch: " + batchId, e);
        }
    }

    /*
     * Fetches ALL cheques for a batch with NO status filter.
     * Used by PIBF processing to match images to cheques —
     * freshly parsed cheques have no status history yet.
     */
    @Override
    public List<InwardCheque> getAllChequesForBatch(long batchId) {

        String sql =
                "SELECT cheque_number, batch_id, account_number, "
                + "drawer_name, amount, micr_code, cheque_date, "
                + "presenting_date "
                + "FROM public.inward_cheque "
                + "WHERE batch_id = ? "
                + "ORDER BY cheque_number";

        List<InwardCheque> cheques = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, batchId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    InwardCheque cheque = InwardCheque.of(
                            resultSet.getString("cheque_number"),
                            resultSet.getString("batch_id"),
                            resultSet.getString("account_number"),
                            resultSet.getString("drawer_name"),
                            resultSet.getBigDecimal("amount"),
                            resultSet.getString("micr_code"),
                            resultSet.getDate("cheque_date") != null
                                    ? resultSet.getDate("cheque_date").toLocalDate() : null,
                            resultSet.getDate("presenting_date") != null
                                    ? resultSet.getDate("presenting_date").toLocalDate() : null);
                    cheques.add(cheque);
                }
            }

            return cheques;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to retrieve all cheques for batch: " + batchId, e);
        }
    }

    @Override
    public void saveDataEntryCorrections(String chequeNumber, long batchId,
            String accountNumber, BigDecimal amount, LocalDate chequeDate, long userId) {
        saveDataEntryCorrections(chequeNumber, batchId, null, accountNumber, amount, chequeDate, userId);
    }

    @Override
    public void saveDataEntryCorrections(String chequeNumber, long batchId,
            String correctedChequeNumber, String accountNumber, BigDecimal amount, LocalDate chequeDate, long userId) {

        try (Connection connection = dataSource.getConnection()) {

            if (correctedChequeNumber != null && !correctedChequeNumber.trim().isEmpty()
                    && !correctedChequeNumber.trim().equalsIgnoreCase(chequeNumber != null ? chequeNumber.trim() : "")) {
                upsertDataEntryHistory(connection, chequeNumber, "CHEQUE_NUMBER", correctedChequeNumber.trim(), userId);
            }

            if (accountNumber != null && !accountNumber.trim().isEmpty()) {
                upsertDataEntryHistory(connection, chequeNumber, "ACCOUNT_NUMBER", accountNumber.trim(), userId);
            }

            if (amount != null) {
                upsertDataEntryHistory(connection, chequeNumber, "AMOUNT", amount.toPlainString(), userId);
            }

            if (chequeDate != null) {
                upsertDataEntryHistory(connection, chequeNumber, "CHEQUE_DATE", chequeDate.toString(), userId);
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to save Data Entry corrections for cheque: " + chequeNumber, e);
        }
    }

    private void upsertDataEntryHistory(Connection connection, String chequeNumber,
            String fieldName, String newValue, long userId) throws SQLException {

        String selectSql =
                "SELECT history_id FROM inward_cheque_dataentry_history "
                + "WHERE cheque_no = ? AND field_name = ? "
                + "ORDER BY history_id DESC";

        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setString(1, chequeNumber);
            selectStmt.setString(2, fieldName);

            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    long existingId = rs.getLong("history_id");

                    // Update existing row
                    String updateSql =
                            "UPDATE inward_cheque_dataentry_history "
                            + "SET new_value = ?, changed_by = ? "
                            + "WHERE history_id = ?";

                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                        updateStmt.setString(1, newValue);
                        updateStmt.setLong(2, userId);
                        updateStmt.setLong(3, existingId);
                        updateStmt.executeUpdate();
                    }

                    // Delete duplicate rows if any exist from previous entries
                    String deleteDupSql =
                            "DELETE FROM inward_cheque_dataentry_history "
                            + "WHERE cheque_no = ? AND field_name = ? AND history_id <> ?";

                    try (PreparedStatement deleteStmt = connection.prepareStatement(deleteDupSql)) {
                        deleteStmt.setString(1, chequeNumber);
                        deleteStmt.setString(2, fieldName);
                        deleteStmt.setLong(3, existingId);
                        deleteStmt.executeUpdate();
                    }

                } else {
                    // Insert new row
                    String insertSql =
                            "INSERT INTO inward_cheque_dataentry_history "
                            + "(cheque_no, field_name, new_value, changed_by) "
                            + "VALUES (?, ?, ?, ?)";

                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                        insertStmt.setString(1, chequeNumber);
                        insertStmt.setString(2, fieldName);
                        insertStmt.setString(3, newValue);
                        insertStmt.setLong(4, userId);
                        insertStmt.executeUpdate();
                    }
                }
            }
        }
    }

    @Override
    public void updateChequeStatus(String chequeNumber, String status, long userId) {

        String updateSql =
                "UPDATE public.inward_cheque_status_history "
                + "SET status = ?, maker_id = ?, maker_action = ?, maker_action_on = CURRENT_TIMESTAMP "
                + "WHERE cheque_number = ? AND status != 'RETURN_BY_MAKER'";

        String insertSql =
                "INSERT INTO public.inward_cheque_status_history "
                + "(cheque_number, status, "
                + "rejection_reason_code, return_reason_code, "
                + "maker_id, maker_action, maker_action_on, "
                + "checker_id, checker_action, checker_action_on, remarks) "
                + "VALUES (?, ?, NULL, NULL, ?, ?, CURRENT_TIMESTAMP, NULL, NULL, NULL, NULL)";

        try (Connection connection = dataSource.getConnection()) {

            int updated = 0;
            try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                statement.setString(1, status);
                statement.setLong(2, userId);
                statement.setString(3, status);
                statement.setString(4, chequeNumber);
                updated = statement.executeUpdate();
            }

            if (updated == 0) {
                try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
                    statement.setString(1, chequeNumber);
                    statement.setString(2, status);
                    statement.setLong(3, userId);
                    statement.setString(4, status);
                    statement.executeUpdate();
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to update cheque status for cheque: " + chequeNumber, e);
        }
    }

    @Override
    public java.util.Map<String, String> getChequeReturnInfo(String chequeNumber) {
        String sql = """
                SELECT sh.status, sh.return_reason_code, sh.remarks, r.description
                FROM public.inward_cheque_status_history sh
                LEFT JOIN public.inward_cheque_return_reason r
                  ON r.return_reason_code = sh.return_reason_code
                WHERE sh.cheque_number = ?
                ORDER BY sh.status_history_id DESC
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, chequeNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String latestStatus = rs.getString("status");
                    String remarks = rs.getString("remarks");

                    if ("RETURN_TO_MAKER".equalsIgnoreCase(latestStatus)) {
                        List<String> codes = new ArrayList<>();
                        List<String> descs = new ArrayList<>();

                        do {
                            String currentStatus = rs.getString("status");
                            if (!"RETURN_TO_MAKER".equalsIgnoreCase(currentStatus)) {
                                break;
                            }
                            String code = rs.getString("return_reason_code");
                            String desc = rs.getString("description");
                            if (code != null && !code.trim().isEmpty() && !codes.contains(code.trim())) {
                                codes.add(code.trim());
                                descs.add(desc != null && !desc.trim().isEmpty() ? desc.trim() : code.trim());
                            }
                            if ((remarks == null || remarks.trim().isEmpty()) && rs.getString("remarks") != null) {
                                remarks = rs.getString("remarks");
                            }
                        } while (rs.next());

                        java.util.Map<String, String> map = new java.util.HashMap<>();
                        map.put("status", "RETURN_TO_MAKER");
                        map.put("returnReasonCode", String.join(", ", codes));
                        map.put("returnReasonDescription", String.join("; ", descs));
                        map.put("remarks", remarks != null ? remarks : "");
                        return map;
                    } else {
                        java.util.Map<String, String> map = new java.util.HashMap<>();
                        map.put("status", latestStatus);
                        map.put("returnReasonCode", rs.getString("return_reason_code"));
                        map.put("returnReasonDescription", rs.getString("description"));
                        map.put("remarks", remarks != null ? remarks : "");
                        return map;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return java.util.Collections.emptyMap();
    }

    @Override
    public List<ReturnReasonDto> getDataEntryReturnReasons() {
        List<ReturnReasonDto> reasons = new ArrayList<>();
        String sql = """
                SELECT return_reason_code, description
                FROM public.inward_cheque_return_reason
                WHERE applicable_role = 'MAKER'
                  AND return_reason_code LIKE 'MR-DATA-%'
                ORDER BY return_reason_code
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                reasons.add(new ReturnReasonDto(
                        rs.getString("return_reason_code"),
                        rs.getString("description")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch data entry return reasons", e);
        }
        return reasons;
    }

    @Override
    public boolean saveMakerDataEntryReturn(String chequeNumber, List<String> reasonCodes, String remarks, Long userId) {
        if (chequeNumber == null || chequeNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Cheque number cannot be empty");
        }
        if (reasonCodes == null || reasonCodes.isEmpty()) {
            throw new IllegalArgumentException("At least one return reason is required");
        }

        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);

            // Clean up any non-return rows (e.g. initial DATA_ENTRY row) so only RETURN_BY_MAKER rows exist
            String cleanOldHistorySql = "DELETE FROM public.inward_cheque_status_history WHERE cheque_number = ? AND status != 'RETURN_BY_MAKER'";
            try (PreparedStatement cleanStmt = connection.prepareStatement(cleanOldHistorySql)) {
                cleanStmt.setString(1, chequeNumber.trim());
                cleanStmt.executeUpdate();
            }

            String returnSql = """
                    INSERT INTO public.inward_cheque_return
                    (cheque_number, return_reason_code, maker_remarks, requested_by, return_status)
                    VALUES (?, ?, ?, ?, 'RETURN_BY_MAKER')
                    """;

            String historySql = """
                    INSERT INTO public.inward_cheque_status_history
                    (cheque_number, status, rejection_reason_code, return_reason_code, maker_id, maker_action, maker_action_on, remarks)
                    VALUES (?, 'RETURN_BY_MAKER', NULL, ?, ?, 'RETURN_BY_MAKER', CURRENT_TIMESTAMP, ?)
                    """;

            String formattedRemarks = remarks != null ? remarks.trim() : null;

            for (String code : reasonCodes) {
                if (code == null || code.trim().isEmpty()) continue;
                String cleanCode = code.trim();

                try (PreparedStatement returnStmt = connection.prepareStatement(returnSql)) {
                    returnStmt.setString(1, chequeNumber.trim());
                    returnStmt.setString(2, cleanCode);
                    if (formattedRemarks != null && !formattedRemarks.isEmpty()) {
                        returnStmt.setString(3, formattedRemarks);
                    } else {
                        returnStmt.setNull(3, java.sql.Types.VARCHAR);
                    }
                    if (userId != null) {
                        returnStmt.setLong(4, userId);
                    } else {
                        returnStmt.setNull(4, java.sql.Types.BIGINT);
                    }
                    returnStmt.executeUpdate();
                }

                try (PreparedStatement histStmt = connection.prepareStatement(historySql)) {
                    histStmt.setString(1, chequeNumber.trim());
                    histStmt.setString(2, cleanCode);
                    if (userId != null) {
                        histStmt.setLong(3, userId);
                    } else {
                        histStmt.setNull(3, java.sql.Types.BIGINT);
                    }
                    if (formattedRemarks != null && !formattedRemarks.isEmpty()) {
                        histStmt.setString(4, formattedRemarks);
                    } else {
                        histStmt.setNull(4, java.sql.Types.VARCHAR);
                    }
                    histStmt.executeUpdate();
                }
            }

            connection.commit();
            return true;
        } catch (Exception e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            throw new RuntimeException("Failed to save Data Entry return for cheque " + chequeNumber, e);
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    private boolean isBatchReturned(long batchId) {
        String sql = "SELECT batch_status FROM public.inward_batch_history WHERE batch_id = ? ORDER BY batch_history_id DESC LIMIT 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return "RETURN_TO_MAKER".equalsIgnoreCase(rs.getString("batch_status"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean chequeNeedsDataEntry(String chequeNumber) {
        if (chequeNumber == null || chequeNumber.trim().isEmpty()) {
            return false;
        }

        String checkerReasonsSql = """
                SELECT sh.return_reason_code
                FROM public.inward_cheque_status_history sh
                WHERE sh.cheque_number = ?
                  AND sh.status = 'RETURN_TO_MAKER'
                ORDER BY sh.status_history_id DESC
                """;

        List<String> checkerCodes = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkerReasonsSql)) {
            ps.setString(1, chequeNumber.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("return_reason_code");
                    if (code != null && !code.trim().isEmpty()) {
                        checkerCodes.add(code.trim().toUpperCase());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (checkerCodes.isEmpty()) {
            return true;
        }

        for (String code : checkerCodes) {
            if (code.startsWith("CR-DATA-") || code.startsWith("MR-DATA-") || code.equals("OTHER")) {
                return true;
            }
            if (code.startsWith("CR-IMG-")) {
                String makerSql = """
                        SELECT return_reason_code
                        FROM (
                            SELECT return_reason_code, status_history_id AS ord
                            FROM public.inward_cheque_status_history
                            WHERE cheque_number = ?
                              AND (status = 'RETURN_BY_MAKER' OR maker_action = 'RETURN_BY_MAKER')
                            UNION ALL
                            SELECT return_reason_code, return_id AS ord
                            FROM public.inward_cheque_return
                            WHERE cheque_number = ?
                        ) sub
                        ORDER BY ord DESC
                        """;
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement mPs = conn.prepareStatement(makerSql)) {
                    mPs.setString(1, chequeNumber.trim());
                    mPs.setString(2, chequeNumber.trim());
                    try (ResultSet mRs = mPs.executeQuery()) {
                        while (mRs.next()) {
                            String mCode = mRs.getString("return_reason_code");
                            if (mCode != null && mCode.trim().toUpperCase().startsWith("MR-DATA-")) {
                                return true;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    public boolean chequeNeedsMicrRepair(String chequeNumber) {
        if (chequeNumber == null || chequeNumber.trim().isEmpty()) {
            return false;
        }

        String checkerReasonsSql = """
                SELECT sh.return_reason_code
                FROM public.inward_cheque_status_history sh
                WHERE sh.cheque_number = ?
                  AND sh.status = 'RETURN_TO_MAKER'
                ORDER BY sh.status_history_id DESC
                """;

        List<String> checkerCodes = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkerReasonsSql)) {
            ps.setString(1, chequeNumber.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("return_reason_code");
                    if (code != null && !code.trim().isEmpty()) {
                        checkerCodes.add(code.trim().toUpperCase());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (checkerCodes.isEmpty()) {
            return false;
        }

        for (String code : checkerCodes) {
            if (code.startsWith("CR-MICR-") || code.startsWith("MR-MICR-") || code.startsWith("MICR_")) {
                return true;
            }
            if (code.startsWith("CR-IMG-")) {
                String makerSql = """
                        SELECT return_reason_code
                        FROM (
                            SELECT return_reason_code, status_history_id AS ord
                            FROM public.inward_cheque_status_history
                            WHERE cheque_number = ?
                              AND (status = 'RETURN_BY_MAKER' OR maker_action = 'RETURN_BY_MAKER')
                            UNION ALL
                            SELECT return_reason_code, return_id AS ord
                            FROM public.inward_cheque_return
                            WHERE cheque_number = ?
                        ) sub
                        ORDER BY ord DESC
                        """;
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement mPs = conn.prepareStatement(makerSql)) {
                    mPs.setString(1, chequeNumber.trim());
                    mPs.setString(2, chequeNumber.trim());
                    try (ResultSet mRs = mPs.executeQuery()) {
                        while (mRs.next()) {
                            String mCode = mRs.getString("return_reason_code");
                            if (mCode != null && mCode.trim().toUpperCase().startsWith("MR-MICR-")) {
                                return true;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
}
