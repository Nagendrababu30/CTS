package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.model.NpciBatchData;

public class BatchDaoImpl implements BatchDao {

	private final javax.sql.DataSource dataSource;

	private BatchDaoImpl(javax.sql.DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static BatchDao of() {
		return new BatchDaoImpl(ConnectionPool.getDataSource());
	}

	// =========================================================
	// SAVE BATCH
	// =========================================================

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

		try (Connection connection = dataSource.getConnection();
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

	// =========================================================
	// VERIFY BATCH
	//
	// inward_batch_lock -> identifies current user's batch
	// inward_batch -> total cheque count
	// inward_batch_history -> maker ID
	// =========================================================

	@Override
	public List<Map<String, Object>> getBatchesForVerification(String userId) {

		String sql = """
				SELECT
				    b.batch_id,
				    b.total_cheques,
				    h.changed_by AS maker_id

				FROM inward_batch b

				JOIN (
				    SELECT DISTINCT ON (batch_id, user_id)
				        batch_id,
				        user_id,
				        lock_status,
				        locked_time
				    FROM inward_batch_lock
				    WHERE user_id = ?
				    ORDER BY
				        batch_id,
				        user_id,
				        locked_time DESC
				) l
				    ON b.batch_id = l.batch_id

				LEFT JOIN (
				    SELECT DISTINCT ON (batch_id)
				        batch_id,
				        changed_by,
				        changed_on
				    FROM inward_batch_history
				    WHERE batch_status = 'SENT_TO_CHECKER'
				    ORDER BY
				        batch_id,
				        changed_on DESC
				) h
				    ON b.batch_id = h.batch_id

				WHERE l.lock_status = 'LOCKED'

				ORDER BY b.batch_id
				""";

		List<Map<String, Object>> batches = new ArrayList<>();

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, Long.parseLong(userId));

			try (ResultSet resultSet = statement.executeQuery()) {

				while (resultSet.next()) {

					Map<String, Object> batch = new HashMap<>();

					batch.put("batchId", resultSet.getLong("batch_id"));

					batch.put("chequeCount", resultSet.getInt("total_cheques"));

					/*
					 * Maker ID comes from inward_batch_history.
					 *
					 * Use getObject() so SQL NULL does not automatically become 0. 
					 */
					Object makerId = resultSet.getObject("maker_id");

					batch.put("makerId", makerId);

					batches.add(batch);
				}
			}

		} catch (Exception e) {

			throw new RuntimeException("Error retrieving batches for verification", e);
		}

		return batches;
	}

	// =========================================================
	// SEARCH VERIFY BATCH
	//
	// Search is still restricted to batches currently
	// locked by the logged-in Checker.
	// =========================================================

	@Override
	public List<Map<String, Object>> searchBatchesForVerification(String batchId, String userId) {

		String sql = """
				SELECT
				    b.batch_id,
				    b.total_cheques,
				    h.changed_by AS maker_id

				FROM inward_batch b

				JOIN (
				    SELECT DISTINCT ON (batch_id, user_id)
				        batch_id,
				        user_id,
				        lock_status,
				        locked_time
				    FROM inward_batch_lock
				    WHERE user_id = ?
				    ORDER BY
				        batch_id,
				        user_id,
				        locked_time DESC
				) l
				    ON b.batch_id = l.batch_id

				LEFT JOIN (
				    SELECT DISTINCT ON (batch_id)
				        batch_id,
				        changed_by,
				        changed_on
				    FROM inward_batch_history
				    WHERE batch_status = 'SENT_TO_CHECKER'
				    ORDER BY
				        batch_id,
				        changed_on DESC
				) h
				    ON b.batch_id = h.batch_id

				WHERE l.lock_status = 'LOCKED'
				  AND CAST(b.batch_id AS VARCHAR) LIKE ?

				ORDER BY b.batch_id
				""";

		List<Map<String, Object>> batches = new ArrayList<>();

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setLong(1, Long.parseLong(userId));

			statement.setString(2, "%" + batchId.trim() + "%");

			try (ResultSet resultSet = statement.executeQuery()) {

				while (resultSet.next()) {

					Map<String, Object> batch = new HashMap<>();

					batch.put("batchId", resultSet.getLong("batch_id"));

					batch.put("chequeCount", resultSet.getInt("total_cheques"));

					/*
					 * Maker ID comes from inward_batch_history.
					 */
					Object makerId = resultSet.getObject("maker_id");

					batch.put("makerId", makerId);

					batches.add(batch);
				}
			}

		} catch (Exception e) {

			throw new RuntimeException("Error searching batches for verification", e);
		}

		return batches;
	}
}