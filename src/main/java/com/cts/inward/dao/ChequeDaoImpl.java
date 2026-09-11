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
    	        "SELECT c.cheque_number, c.batch_id, c.account_number, "
    	        + "c.drawer_name, c.amount, c.micr_code, c.cheque_date, "
    	        + "c.presenting_date "
    	        + "FROM public.inward_cheque c "
    	        + "INNER JOIN LATERAL ( "
    	        + "    SELECT h.status "
    	        + "    FROM public.inward_cheque_status_history h "
    	        + "    WHERE h.cheque_number = c.cheque_number "
    	        + "    ORDER BY h.status_history_id DESC "
    	        + "    LIMIT 1 "
    	        + ") latest ON TRUE "
    	        + "WHERE c.batch_id = ? "
    	        + "AND latest.status = 'DATA_ENTRY' "
    	        + "ORDER BY c.cheque_number";

        List<InwardCheque> cheques = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, Long.parseLong(batchId));

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
                    "Failed to retrieve cheques for batch: " + batchId, e);
        }
    }

    @Override
    public void saveDataEntryCorrections(String chequeNumber, long batchId,
            String accountNumber, BigDecimal amount, LocalDate chequeDate, long userId) {

        String sql =
                "INSERT INTO inward_cheque_dataentry_history "
                + "(cheque_no, field_name, new_value, changed_by) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (accountNumber != null && !accountNumber.trim().isEmpty()) {
                statement.setString(1, chequeNumber);
                statement.setString(2, "ACCOUNT_NUMBER");
                statement.setString(3, accountNumber);
                statement.setLong(4, userId);
                statement.executeUpdate();
            }

            if (amount != null) {
                statement.setString(1, chequeNumber);
                statement.setString(2, "AMOUNT");
                statement.setString(3, amount.toPlainString());
                statement.setLong(4, userId);
                statement.executeUpdate();
            }

            if (chequeDate != null) {
                statement.setString(1, chequeNumber);
                statement.setString(2, "CHEQUE_DATE");
                statement.setString(3, chequeDate.toString());
                statement.setLong(4, userId);
                statement.executeUpdate();
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to save Data Entry corrections for cheque: " + chequeNumber, e);
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
