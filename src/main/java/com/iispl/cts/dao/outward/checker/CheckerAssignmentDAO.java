package com.iispl.cts.dao.outward.checker;

import java.sql.Connection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.cts.inward.config.ConnectionPool;

public class CheckerAssignmentDAO {

	private final javax.sql.DataSource dataSource = ConnectionPool.getDataSource();

	// Takes the batch for the current Checker and moves it to Checker processing.
	public boolean takeBatch(String batchNumber, long checkerUserId) {

		String lockBatchSql = "SELECT batch_number, batch_status " + "FROM outward_batch " + "WHERE batch_number = ? "
				+ "FOR UPDATE";

		String checkSql = "SELECT id " + "FROM outward_batch_assignment " + "WHERE batch_number = ? "
				+ "AND UPPER(assignment_role) = 'CHECKER' " + "AND UPPER(assignment_status) "
				+ "    IN ('ASSIGNED', 'IN_PROGRESS') " + "LIMIT 1";

		String insertSql = "INSERT INTO outward_batch_assignment "
				+ "(batch_number, user_id, assignment_role, assigned_at, " + " started_at, assignment_status) "
				+ "VALUES (?, ?, 'CHECKER', CURRENT_TIMESTAMP, " + " CURRENT_TIMESTAMP, 'IN_PROGRESS')";

		String updateBatchProcessingSql = "UPDATE outward_batch " + "SET batch_status = 'CHECKER_PROCESSING' "
				+ "WHERE batch_number = ? " + "AND UPPER(TRIM(batch_status)) = " + "'SUBMITTED_TO_CHECKER'";

		try (Connection connection = dataSource.getConnection()) {

			connection.setAutoCommit(false);

			try {

				try (PreparedStatement lockStatement = connection.prepareStatement(lockBatchSql)) {

					lockStatement.setString(1, batchNumber);

					try (ResultSet rs = lockStatement.executeQuery()) {

						if (!rs.next()) {

							connection.rollback();

							return false;
						}

						String batchStatus = rs.getString("batch_status");

						if (!"SUBMITTED_TO_CHECKER".equalsIgnoreCase(batchStatus)) {

							connection.rollback();

							return false;
						}
					}
				}

				try (PreparedStatement checkStatement = connection.prepareStatement(checkSql)) {

					checkStatement.setString(1, batchNumber);

					try (ResultSet rs = checkStatement.executeQuery()) {

						if (rs.next()) {

							connection.rollback();

							return false;
						}
					}
				}

				try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {

					insertStatement.setString(1, batchNumber);

					insertStatement.setLong(2, checkerUserId);

					int rows = insertStatement.executeUpdate();

					if (rows != 1) {

						connection.rollback();

						return false;
					}
				}

				try (PreparedStatement updateStatement = connection.prepareStatement(updateBatchProcessingSql)) {

					updateStatement.setString(1, batchNumber);

					int updatedRows = updateStatement.executeUpdate();

					if (updatedRows != 1) {

						throw new RuntimeException("Unable to move batch to Checker processing");
					}
				}

				connection.commit();

				return true;

			} catch (Exception e) {

				try {

					connection.rollback();

				} catch (Exception rollbackException) {

					rollbackException.printStackTrace();
				}

				throw e;
			}

		} catch (Exception e) {

			e.printStackTrace();

			throw new RuntimeException("Error while taking Checker batch: " + batchNumber, e);
		}
	}

	// Checks whether the batch already has an active Checker assignment.
	public boolean isBatchAssigned(String batchNumber) {

		String sql = "SELECT 1 " + "FROM outward_batch_assignment " + "WHERE batch_number = ? "
				+ "AND UPPER(assignment_role) = 'CHECKER' " + "AND UPPER(assignment_status) "
				+ "    IN ('ASSIGNED', 'IN_PROGRESS') " + "LIMIT 1";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, batchNumber);

			try (ResultSet rs = statement.executeQuery()) {

				return rs.next();
			}

		} catch (Exception e) {

			e.printStackTrace();

			throw new RuntimeException("Error while checking batch assignment: " + batchNumber, e);
		}
	}

	// Checks whether the batch is currently assigned to the specified Checker.
	public boolean isBatchAssignedToChecker(String batchNumber, long checkerUserId) {

		String sql = "SELECT 1 " + "FROM outward_batch_assignment " + "WHERE batch_number = ? " + "AND user_id = ? "
				+ "AND UPPER(assignment_role) = 'CHECKER' " + "AND UPPER(assignment_status) "
				+ "    IN ('ASSIGNED', 'IN_PROGRESS') " + "LIMIT 1";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, batchNumber);

			statement.setLong(2, checkerUserId);

			try (ResultSet rs = statement.executeQuery()) {

				return rs.next();
			}

		} catch (Exception e) {

			e.printStackTrace();

			throw new RuntimeException("Error while checking Checker assignment: " + batchNumber, e);
		}
	}

	// Returns the user ID of the Checker currently assigned to the batch.
	public Long getAssignedChecker(String batchNumber) {

		String sql = "SELECT user_id " + "FROM outward_batch_assignment " + "WHERE batch_number = ? "
				+ "AND UPPER(assignment_role) = 'CHECKER' " + "AND UPPER(assignment_status) "
				+ "    IN ('ASSIGNED', 'IN_PROGRESS') " + "ORDER BY assigned_at DESC " + "LIMIT 1";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, batchNumber);

			try (ResultSet rs = statement.executeQuery()) {

				if (rs.next()) {

					return rs.getLong("user_id");
				}
			}

		} catch (Exception e) {

			e.printStackTrace();

			throw new RuntimeException("Error while getting assigned Checker: " + batchNumber, e);
		}

		return null;
	}

	// Completes the Checker assignment when the batch reaches its final verified
	// state.
	public boolean completeBatch(String batchNumber, long checkerUserId) {

		String batchStatusSql = "SELECT batch_status " + "FROM outward_batch " + "WHERE batch_number = ?";

		String completeSql = "UPDATE outward_batch_assignment " + "SET assignment_status = 'COMPLETED', "
				+ "    completed_at = CURRENT_TIMESTAMP " + "WHERE batch_number = ? " + "AND user_id = ? "
				+ "AND UPPER(assignment_role) = 'CHECKER' " + "AND UPPER(assignment_status) "
				+ "    IN ('ASSIGNED', 'IN_PROGRESS')";

		try (Connection connection = dataSource.getConnection()) {

			connection.setAutoCommit(false);

			try {

				String batchStatus = null;

				try (PreparedStatement statusStatement = connection.prepareStatement(batchStatusSql)) {

					statusStatement.setString(1, batchNumber);

					try (ResultSet rs = statusStatement.executeQuery()) {

						if (!rs.next()) {

							connection.rollback();

							return false;
						}

						batchStatus = rs.getString("batch_status");
					}
				}

				if ("ON_HOLD".equalsIgnoreCase(batchStatus)) {

					connection.commit();

					return true;
				}

				if (!"CHECKER_VERIFIED".equalsIgnoreCase(batchStatus)) {

					connection.rollback();

					return false;
				}

				try (PreparedStatement statement = connection.prepareStatement(completeSql)) {

					statement.setString(1, batchNumber);

					statement.setLong(2, checkerUserId);

					int updatedRows = statement.executeUpdate();

					if (updatedRows != 1) {

						connection.rollback();

						return false;
					}
				}

				connection.commit();

				return true;

			} catch (Exception e) {

				try {

					connection.rollback();

				} catch (Exception rollbackException) {

					rollbackException.printStackTrace();
				}

				throw e;
			}

		} catch (Exception e) {

			e.printStackTrace();

			throw new RuntimeException("Error while completing Checker assignment: " + batchNumber, e);
		}
	}

	// Returns the assignment ID of the specified Checker's active batch assignment.
	public Long getAssignmentId(String batchNumber, long checkerUserId) {

		String sql = "SELECT id " + "FROM outward_batch_assignment " + "WHERE batch_number = ? " + "AND user_id = ? "
				+ "AND UPPER(assignment_role) = 'CHECKER' " + "AND UPPER(assignment_status) "
				+ "    IN ('ASSIGNED', 'IN_PROGRESS') " + "ORDER BY assigned_at DESC " + "LIMIT 1";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, batchNumber);

			statement.setLong(2, checkerUserId);

			try (ResultSet rs = statement.executeQuery()) {

				if (rs.next()) {

					return rs.getLong("id");
				}
			}

		} catch (Exception e) {

			e.printStackTrace();

			throw new RuntimeException("Error while getting assignment ID: " + batchNumber, e);
		}

		return null;
	}

	// Releases the batch from the current Checker and resets it for reassignment.
	public boolean releaseBatchLock(String batchNumber, long checkerUserId) {

		if (batchNumber == null || batchNumber.trim().isEmpty()) {

			return false;
		}

		if (checkerUserId <= 0) {

			return false;
		}

		String cleanBatchNumber = batchNumber.trim();

		try (Connection connection = dataSource.getConnection()) {

			connection.setAutoCommit(false);

			try {

				String lockBatchSql = "SELECT batch_number, batch_status " + "FROM public.outward_batch "
						+ "WHERE batch_number = ? " + "FOR UPDATE";

				String batchStatus = null;

				try (PreparedStatement statement = connection.prepareStatement(lockBatchSql)) {

					statement.setString(1, cleanBatchNumber);

					try (ResultSet rs = statement.executeQuery()) {

						if (!rs.next()) {

							connection.rollback();
							return false;
						}

						batchStatus = rs.getString("batch_status");
					}
				}

				if ("ON_HOLD".equalsIgnoreCase(batchStatus)) {

					connection.rollback();
					return false;
				}

				String ownershipSql = "SELECT id " + "FROM public.outward_batch_assignment " + "WHERE batch_number = ? "
						+ "AND user_id = ? " + "AND UPPER(TRIM(assignment_role)) = 'CHECKER' "
						+ "AND UPPER(TRIM(assignment_status)) " + "IN ('ASSIGNED', 'IN_PROGRESS') "
						+ "ORDER BY assigned_at DESC " + "LIMIT 1 " + "FOR UPDATE";

				Long assignmentId = null;

				try (PreparedStatement statement = connection.prepareStatement(ownershipSql)) {

					statement.setString(1, cleanBatchNumber);

					statement.setLong(2, checkerUserId);

					try (ResultSet rs = statement.executeQuery()) {

						if (rs.next()) {

							assignmentId = rs.getLong("id");
						}
					}
				}

				if (assignmentId == null) {

					connection.rollback();
					return false;
				}

				String resetChequeSql = "UPDATE public.outward_cheque " + "SET cheque_status = 'VERIFIED' "
						+ "WHERE batch_number = ?";

				try (PreparedStatement statement = connection.prepareStatement(resetChequeSql)) {

					statement.setString(1, cleanBatchNumber);

					statement.executeUpdate();
				}

				String deleteProcessingSql = "DELETE FROM public.cheque_processing " + "WHERE batch_number = ?";

				try (PreparedStatement statement = connection.prepareStatement(deleteProcessingSql)) {

					statement.setString(1, cleanBatchNumber);

					statement.executeUpdate();
				}

				String resetBatchSql = "UPDATE public.outward_batch " + "SET batch_status = 'SUBMITTED_TO_CHECKER' "
						+ "WHERE batch_number = ? " + "AND UPPER(TRIM(batch_status)) " + "= 'CHECKER_PROCESSING'";

				try (PreparedStatement statement = connection.prepareStatement(resetBatchSql)) {

					statement.setString(1, cleanBatchNumber);

					int updatedRows = statement.executeUpdate();

					if (updatedRows != 1) {

						connection.rollback();
						return false;
					}
				}

				String releaseAssignmentSql = "UPDATE public.outward_batch_assignment "
						+ "SET assignment_status = 'RELEASED', " + "completed_at = CURRENT_TIMESTAMP " + "WHERE id = ? "
						+ "AND user_id = ? " + "AND UPPER(TRIM(assignment_role)) = 'CHECKER' "
						+ "AND UPPER(TRIM(assignment_status)) " + "IN ('ASSIGNED', 'IN_PROGRESS')";

				try (PreparedStatement statement = connection.prepareStatement(releaseAssignmentSql)) {

					statement.setLong(1, assignmentId);

					statement.setLong(2, checkerUserId);

					int updatedRows = statement.executeUpdate();

					if (updatedRows != 1) {

						connection.rollback();
						return false;
					}
				}

				connection.commit();

				return true;

			} catch (Exception e) {

				try {

					connection.rollback();

				} catch (Exception rollbackException) {

					rollbackException.printStackTrace();
				}

				throw e;
			}

		} catch (Exception e) {

			e.printStackTrace();

			throw new RuntimeException("Error while releasing Checker batch: " + cleanBatchNumber, e);
		}
	}
}