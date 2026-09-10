package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.model.NpciBatchData;

public class BatchDaoImpl implements BatchDao {

	private final DataSource dataSource;

	private BatchDaoImpl(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static BatchDao of() {
		return new BatchDaoImpl(ConnectionPool.getDataSource());
	}

	@Override
	public void saveBatch(NpciBatchData batchData) {

		String sql = """
				INSERT INTO inward_batch
				(
				    batch_id,
				    file_id,
				    presenting_bank_name,
				    total_cheques
				)
				VALUES (?, ?, ?, ?)
				""";

		try (Connection connection = ConnectionPool.getDataSource().getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, batchData.getBatchId());
			statement.setLong(2, batchData.getFileId());
			statement.setString(3, batchData.getPresentingBankName());
			statement.setInt(4, batchData.getTotalCheques());

			statement.executeUpdate();

		} catch (Exception e) {
			throw new RuntimeException("Error saving batch", e);
		}
	}

	// =========================================================
	// GET ALL BATCHES
	// =========================================================

	@Override
	public List<NpciBatchData> getAllBatches() {

		String sql = """
				SELECT
				    batch_id,
				    file_id,
				    presenting_bank_name,
				    total_cheques
				FROM inward_batch
				ORDER BY batch_id
				""";

		List<NpciBatchData> batches = new ArrayList<>();

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql);
				ResultSet resultSet = statement.executeQuery()) {

			while (resultSet.next()) {

				NpciBatchData batch = new NpciBatchData(resultSet.getLong("batch_id"), resultSet.getLong("file_id"),
						resultSet.getString("presenting_bank_name"), resultSet.getInt("total_cheques"));

				batches.add(batch);
			}

		} catch (Exception e) {
			throw new RuntimeException("Error retrieving batches", e);
		}

		return batches;
	}
	
	@Override
	public int getDataEntryPendingCount(long batchId) {

		String sql =
		        "SELECT COUNT(*) " +
		        "FROM public.inward_cheque c " +
		        "LEFT JOIN LATERAL ( " +
		        "    SELECT h.status " +
		        "    FROM public.inward_cheque_status_history h " +
		        "    WHERE h.cheque_number = c.cheque_number " +
		        "    ORDER BY h.status_history_id DESC " +
		        "    LIMIT 1 " +
		        ") latest ON TRUE " +
		        "WHERE c.batch_id = ? " +
		        "AND COALESCE(latest.status, '') NOT IN " +
		        "('DATA_ENTRY_COMPLETED', 'RETURN_BY_MAKER')";

	    try (Connection connection = ConnectionPool.getDataSource().getConnection();
	         PreparedStatement statement = connection.prepareStatement(sql)) {

	        statement.setLong(1, batchId);

	        try (ResultSet resultSet = statement.executeQuery()) {

	            if (resultSet.next()) {
	                return resultSet.getInt(1);
	            }
	        }

	    } catch (Exception e) {
	        throw new RuntimeException(
	                "Error retrieving Data Entry pending count for batch " + batchId, e);
	    }

	    return 0;
	}
	
	@Override
	public boolean completeDataEntry(long batchId, long userId) {

		Connection connection = null;

		try {

			connection = ConnectionPool.getDataSource().getConnection();

			connection.setAutoCommit(false);

			// ---------------------------------------------------------
			// 1. Check whether ALL cheques are completed
			// ---------------------------------------------------------

			String checkSql = "SELECT " + "COUNT(*) AS total_count, " + "COUNT(*) FILTER "
					+ "(WHERE latest.status = 'DATA_ENTRY_COMPLETED') " + "AS completed_count "
					+ "FROM public.inward_cheque c " + "LEFT JOIN LATERAL ( " + "    SELECT h.status "
					+ "    FROM public.inward_cheque_status_history h " + "    WHERE h.cheque_number = c.cheque_number "
					+ "    ORDER BY h.status_history_id DESC " + "    LIMIT 1 " + ") latest ON TRUE "
					+ "WHERE c.batch_id = ?";

			int totalCount =0;
			int completedCount = 0;

			try (PreparedStatement statement = connection.prepareStatement(checkSql)) {

				statement.setLong(1, batchId);

				try (ResultSet resultSet = statement.executeQuery()) {

					if (resultSet.next()) {

						totalCount = resultSet.getInt("total_count");

						completedCount = resultSet.getInt("completed_count");
					}
				}
			}

			// ---------------------------------------------------------
			// 2. Do not complete batch if even one cheque is pending
			// ---------------------------------------------------------

			if (totalCount == 0 || totalCount != completedCount) {

				connection.rollback();

				return false;
			}

			// 3. Add DATA_ENTRY_COMPLETED to batch history
			String historySql = "INSERT INTO public.inward_batch_history " + "(batch_id, batch_status, changed_on, "
					+ "changed_by, reason, remarks) " + "VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?, ?)";

			try (PreparedStatement statement = connection.prepareStatement(historySql)) {

				statement.setLong(1, batchId);

				statement.setString(2, "DATA_ENTRY_COMPLETED");

				statement.setLong(3, userId);

				statement.setString(4, "DATA_ENTRY_COMPLETED");

				statement.setString(5, "All cheques completed Data Entry");

				statement.executeUpdate();
			}

			connection.commit();

			return true;

		} catch (Exception e) {

			if (connection != null) {

				try {
					connection.rollback();
				} catch (Exception rollbackException) {
					rollbackException.printStackTrace();
				}
			}

			throw new RuntimeException("Failed to complete Data Entry for batch " + batchId, e);

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

	@Override
	public List<NpciBatchData> getBatchesByStatus(String batchStatus) {

		String sql = "SELECT " + "b.batch_id, " + "b.file_id, " + "b.presenting_bank_name, " + "b.total_cheques "
				+ "FROM public.inward_batch b " + "INNER JOIN LATERAL ( " + "    SELECT h.batch_status "
				+ "    FROM public.inward_batch_history h " + "    WHERE h.batch_id = b.batch_id "
				+ "    ORDER BY h.changed_on DESC " + "    LIMIT 1 " + ") latest ON TRUE "
				+ "WHERE latest.batch_status = ? " + "ORDER BY b.batch_id";

		List<NpciBatchData> batches = new ArrayList<>();

		try (Connection connection = ConnectionPool.getDataSource().getConnection();

				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, batchStatus);

			try (ResultSet resultSet = statement.executeQuery()) {

				while (resultSet.next()) {

					batches.add(new NpciBatchData(resultSet.getLong("batch_id"), resultSet.getLong("file_id"),
							resultSet.getString("presenting_bank_name"), resultSet.getInt("total_cheques")));
				}
			}

		} catch (Exception e) {

			throw new RuntimeException("Error retrieving batches by status", e);
		}

		return batches;
	}
}