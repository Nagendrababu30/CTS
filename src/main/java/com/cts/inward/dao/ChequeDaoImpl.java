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

		String sql = "INSERT INTO cheque " + "(chequenumber, batchid, accountnumber, drawername, "
				+ "amount, micrcode, chequedate, presentingdate) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

		try (Connection connection = dataSource.getConnection();

				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, cheque.getChequeNumber());

			statement.setLong(2, cheque.getBatchId());

			statement.setString(3, cheque.getAccountNumber());

			statement.setString(4, cheque.getDrawerName());

			statement.setBigDecimal(5, cheque.getChequeAmount());

			statement.setString(6, cheque.getMicrCode());

			statement.setObject(7, cheque.getChequeDate());

			/*
			 * PXF data does not currently contain presenting date, so store NULL.
			 */
			statement.setObject(8, null);

			statement.executeUpdate();

		} catch (SQLException e) {

			throw new IllegalStateException("Failed to save cheque: " + cheque.getChequeNumber(), e);
		}

	}

	@Override
	public List<InwardCheque> getChequesForBatch(String batchId) {

		String sql = "SELECT cheque_number, batch_id, account_number, "
				+ "drawer_name, amount, micr_code, cheque_date, " + "presenting_date " + "FROM inward_cheque "
				+ "WHERE batch_id = ? " + "ORDER BY cheque_number";

		List<InwardCheque> cheques = new ArrayList<>();

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, Long.parseLong(batchId));

			try (ResultSet resultSet = statement.executeQuery()) {

				while (resultSet.next()) {

					InwardCheque cheque = InwardCheque.of(resultSet.getString("cheque_number"),

							resultSet.getString("batch_id"),

							resultSet.getString("account_number"),

							resultSet.getString("drawer_name"),

							resultSet.getBigDecimal("amount"),

							resultSet.getString("micr_code"),

							resultSet.getDate("cheque_date") != null ? resultSet.getDate("cheque_date").toLocalDate()
									: null,

							resultSet.getDate("presenting_date") != null
									? resultSet.getDate("presenting_date").toLocalDate()
									: null);

					cheques.add(cheque);
				}
			}

			return cheques;

		} catch (SQLException e) {

			throw new IllegalStateException("Failed to retrieve cheques for batch: " + batchId, e);
		}
	}

	@Override
	public void updateCheque(String chequeNumber, long batchId, String accountNumber, BigDecimal amount,
			LocalDate chequeDate) {

		String sql = "UPDATE inward_cheque SET " + "account_number = ?, " + "amount = ?, " + "cheque_date = ? "
				+ "WHERE cheque_number = ? " + "AND batch_id = ?";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, accountNumber);
			statement.setBigDecimal(2, amount);
			statement.setObject(3, chequeDate);
			statement.setString(4, chequeNumber);
			statement.setLong(5, batchId);

			int updatedRows = statement.executeUpdate();

			if (updatedRows == 0) {
				throw new IllegalStateException("Cheque not found: " + chequeNumber + " for batch " + batchId);
			}

		} catch (SQLException e) {
			throw new IllegalStateException("Failed to update cheque: " + chequeNumber, e);
		}
	}

	@Override
	public void updateDataEntryCheque(String chequeNumber, long batchId, String accountNumber, BigDecimal amount,
			LocalDate chequeDate, long userId) {

		Connection connection = null;

		try {

			connection = dataSource.getConnection();

			connection.setAutoCommit(false);
			// 1. Update cheque data
						String updateChequeSql = "UPDATE inward_cheque SET " + "account_number = ?, " + "amount = ?, "
					+ "cheque_date = ? " + "WHERE cheque_number = ? " + "AND batch_id = ?";

			try (PreparedStatement statement = connection.prepareStatement(updateChequeSql)) {

				statement.setString(1, accountNumber);
				statement.setBigDecimal(2, amount);
				statement.setObject(3, chequeDate);
				statement.setString(4, chequeNumber);
				statement.setLong(5, batchId);

				int updatedRows = statement.executeUpdate();

				if (updatedRows != 1) {

					throw new IllegalStateException("Cheque not found: " + chequeNumber + " for batch " + batchId);
				}
			}
			// 2. Add cheque status history
				String statusHistorySql = "INSERT INTO public.inward_cheque_status_history " + "(cheque_number, status, "
					+ "rejection_reason_code, return_reason_code, " + "maker_id, maker_action, maker_action_on, "
					+ "checker_id, checker_action, checker_action_on, " + "remarks) "
					+ "VALUES (?, ?, NULL, NULL, ?, ?, " + "CURRENT_TIMESTAMP, NULL, NULL, NULL, ?)";

			try (PreparedStatement statement = connection.prepareStatement(statusHistorySql)) {

				statement.setString(1, chequeNumber);

				statement.setString(2, "DATA_ENTRY_COMPLETED");

				statement.setLong(3, userId);

				statement.setString(4, "DATA_ENTRY_COMPLETED");

				statement.setString(5, "Cheque data entry completed");

				statement.executeUpdate();
			}

			connection.commit();

		} catch (Exception e) {

			if (connection != null) {

				try {
					connection.rollback();
				} catch (Exception rollbackException) {
					rollbackException.printStackTrace();
				}
			}

			throw new RuntimeException("Failed to save Data Entry for cheque " + chequeNumber, e);

		} finally {

			if (connection != null) {

				try {
					connection.setAutoCommit(true);
					connection.close();
				} catch (Exception closeException) {
					closeException.printStackTrace();
				}
			}
		}
	}

}