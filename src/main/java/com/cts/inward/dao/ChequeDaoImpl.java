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
    	        + "    SELECT h.status "
    	        + "    FROM public.inward_cheque_status_history h "
    	        + "    WHERE h.cheque_number = c.cheque_number "
    	        + "    ORDER BY h.status_history_id DESC "
    	        + "    LIMIT 1 "
    	        + ") latest ON TRUE "
    	        + "WHERE c.batch_id = ? "
    	        + "AND COALESCE(latest.status, '') <> 'RETURN_BY_MAKER' "
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
}
