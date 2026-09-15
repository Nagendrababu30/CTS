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
    	        + "COALESCE(dt.new_value, CAST(c.cheque_date AS VARCHAR)) AS cheque_date_str "
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
    	        + "    SELECT h.status, h.return_reason_code "
    	        + "    FROM public.inward_cheque_status_history h "
    	        + "    WHERE h.cheque_number = c.cheque_number "
    	        + "    ORDER BY h.status_history_id DESC "
    	        + "    LIMIT 1 "
    	        + ") latest ON TRUE "
    	        + "WHERE c.batch_id = ? "
    	        + "AND COALESCE(latest.status, '') NOT IN ('ACCEPT', 'REJECT', 'RETURN_BY_MAKER') "
    	        + "AND NOT ( "
    	        + "    latest.status = 'RETURN_TO_MAKER' "
    	        + "    AND ( "
    	        + "        latest.return_reason_code LIKE 'CR-MICR-%' "
    	        + "        OR latest.return_reason_code LIKE 'CR-IMG-%' "
    	        + "        OR latest.return_reason_code LIKE 'MR-MICR-%' "
    	        + "        OR latest.return_reason_code LIKE 'MICR_%' "
    	        + "    ) "
    	        + ") "
    	        + "ORDER BY c.cheque_number";

        List<InwardCheque> cheques = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, Long.parseLong(batchId));

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {

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
                            resultSet.getString("cheque_number"),
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

        try (Connection connection = dataSource.getConnection()) {

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

        String checkSql =
                "SELECT status "
                + "FROM public.inward_cheque_status_history "
                + "WHERE cheque_number = ? "
                + "ORDER BY status_history_id DESC "
                + "LIMIT 1";

        String insertSql =
                "INSERT INTO public.inward_cheque_status_history "
                + "(cheque_number, status, "
                + "rejection_reason_code, return_reason_code, "
                + "maker_id, maker_action, maker_action_on, "
                + "checker_id, checker_action, checker_action_on, remarks) "
                + "VALUES (?, ?, NULL, NULL, ?, ?, CURRENT_TIMESTAMP, NULL, NULL, NULL, NULL)";

        try (Connection connection = dataSource.getConnection()) {

            String latestStatus = null;

            try (PreparedStatement statement = connection.prepareStatement(checkSql)) {
                statement.setString(1, chequeNumber);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        latestStatus = resultSet.getString("status");
                    }
                }
            }

            if (status.equals(latestStatus)) {
                return;
            }

            try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
                statement.setString(1, chequeNumber);
                statement.setString(2, status);
                statement.setLong(3, userId);
                statement.setString(4, "DATA_ENTRY");
                statement.executeUpdate();
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
                LIMIT 1
                """;
        java.util.Map<String, String> info = new java.util.HashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, chequeNumber);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    info.put("status", rs.getString("status"));
                    info.put("returnReasonCode", rs.getString("return_reason_code"));
                    info.put("remarks", rs.getString("remarks"));
                    info.put("description", rs.getString("description"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return info;
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
            throw new IllegalArgumentException("Cheque number is required");
        }
        if (reasonCodes == null || reasonCodes.isEmpty()) {
            throw new IllegalArgumentException("At least one return reason is required");
        }

        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);

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
}
