package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.cts.inward.model.CheckerBatch;

public class CheckerDashboardDAOImpl implements CheckerDashboardDAO {

	private final DataSource dataSource;

	public CheckerDashboardDAOImpl(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public int getReceivedBatchCount() {

		String sql = "SELECT COUNT(*) " + "FROM inward_batch_lock";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql);
				ResultSet resultSet = statement.executeQuery()) {

			if (resultSet.next()) {
				return resultSet.getInt(1);
			}

			return 0;

		} catch (SQLException e) {

			throw new IllegalStateException("Failed to get received batch count", e);
		}
	}

	@Override
	public int getAvailableBatchCount() {

		String sql = "SELECT COUNT(*) " + "FROM inward_batch_lock " + "WHERE lock_status = 'AVAILABLE' "
				+ "AND user_id IS NULL";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql);
				ResultSet resultSet = statement.executeQuery()) {

			if (resultSet.next()) {
				return resultSet.getInt(1);
			}

			return 0;

		} catch (SQLException e) {

			throw new IllegalStateException("Failed to get available batch count", e);
		}
	}

	@Override
	public int getMyBatchCount(long userId) {

		String sql = "SELECT COUNT(*) " + "FROM inward_batch_lock " + "WHERE user_id = ? "
				+ "AND lock_status = 'LOCKED'";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, userId);

			try (ResultSet resultSet = statement.executeQuery()) {

				if (resultSet.next()) {
					return resultSet.getInt(1);
				}

				return 0;
			}

		} catch (SQLException e) {

			throw new IllegalStateException("Failed to get my batch count", e);
		}
	}

	@Override
	public List<CheckerBatch> getBatches() {

		String sql = "SELECT " + "l.batch_id, " +

		/*
		 * AVAILABLE: user_id = NULL
		 *
		 * LOCKED: user_id = checker who locked it
		 */
				"CASE " + "WHEN l.lock_status = 'AVAILABLE' " + "THEN NULL " + "ELSE l.user_id " + "END AS checker_id, "
				+

				"h.maker_id, " + "l.lock_status, " + "b.total_cheques " +

				"FROM ( " +

				/*
				 * Select one current lock row for each batch.
				 *
				 * A checker LOCKED record has priority. Otherwise AVAILABLE record is selected.
				 */
				"SELECT DISTINCT ON (batch_id) " + "lock_id, " + "batch_id, " + "user_id, " + "lock_status " +

				"FROM inward_batch_lock " +

				"ORDER BY " + "batch_id, " +

				"CASE " + "WHEN lock_status = 'LOCKED' " + "AND user_id IN ( " + "SELECT user_id " + "FROM public.user "
				+ "WHERE role_id = 2 " + ") " + "THEN 1 " + "ELSE 2 " + "END, " +

				"lock_id DESC " +

				") l " +

				/*
				 * Batch information
				 */
				"JOIN inward_batch b " + "ON b.batch_id = l.batch_id " +

				/*
				 * Find the maker who sent the batch to checker.
				 */
				"JOIN ( " +

				"SELECT " + "bh.batch_id, " + "MAX(bh.changed_by) AS maker_id " +

				"FROM inward_batch_history bh " +

				"JOIN public.user u " + "ON u.user_id = bh.changed_by " + "AND u.role_id = 1 " +

				"WHERE bh.batch_status = 'SENT_TO_CHECKER' " +

				"GROUP BY bh.batch_id " +

				") h " +

				"ON h.batch_id = l.batch_id " +

				/*
				 * Get latest batch status.
				 */
				"JOIN ( " +

				"SELECT DISTINCT ON (batch_id) " + "batch_id, " + "batch_status " +

				"FROM inward_batch_history " +

				"ORDER BY " + "batch_id, " + "changed_on DESC, " + "batch_history_id DESC " +

				") hs " +

				"ON hs.batch_id = l.batch_id " +

				/*
				 * Only batches whose current status is SENT_TO_CHECKER.
				 */
				"WHERE hs.batch_status = 'SENT_TO_CHECKER' " +

				"ORDER BY l.batch_id";

		List<CheckerBatch> batches = new ArrayList<>();

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql);
				ResultSet resultSet = statement.executeQuery()) {

			while (resultSet.next()) {

				CheckerBatch batch = new CheckerBatch();

				/*
				 * Batch ID
				 */
				batch.setBatchId(resultSet.getLong("batch_id"));

				/*
				 * Total Cheques
				 */
				batch.setTotalCheques(resultSet.getInt("total_cheques"));

				/*
				 * Maker ID
				 */
				Object maker = resultSet.getObject("maker_id");

				if (maker != null) {

					batch.setMaker(String.valueOf(maker));

				} else {

					batch.setMaker("Not Assigned");
				}

				/*
				 * Checker ID
				 *
				 * AVAILABLE: userId = null
				 *
				 * LOCKED: userId = checker who locked it
				 */
				Object checkerId = resultSet.getObject("checker_id");

				if (checkerId != null) {

					batch.setUserId(((Number) checkerId).longValue());

				} else {

					batch.setUserId(null);
				}

				/*
				 * Lock Status
				 */
				batch.setLockStatus(resultSet.getString("lock_status"));

				batches.add(batch);
			}

			return batches;

		} catch (SQLException e) {

			throw new IllegalStateException("Failed to retrieve checker batches", e);
		}
	}

	@Override
	public boolean lockBatch(long batchId, long userId) {

		String sql = "UPDATE inward_batch_lock " + "SET user_id = ?, " + "lock_status = 'LOCKED', "
				+ "locked_time = CURRENT_TIMESTAMP " + "WHERE batch_id = ? " + "AND lock_status = 'AVAILABLE' "
				+ "AND user_id IS NULL";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, userId);
			statement.setLong(2, batchId);

			int rowsUpdated = statement.executeUpdate();

			return rowsUpdated == 1;

		} catch (SQLException e) {

			throw new IllegalStateException(
					"Failed to lock batch: " + batchId + " [SQL: " + e.getErrorCode() + ", " + e.getSQLState() + "]",
					e);
		}
	}
}