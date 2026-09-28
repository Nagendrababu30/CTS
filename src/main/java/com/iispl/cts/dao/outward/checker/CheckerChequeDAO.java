package com.iispl.cts.dao.outward.checker;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cts.inward.config.ConnectionPool;
import com.iispl.cts.model.outward.ChequeProcessing;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.model.outward.ReturnReason;

public class CheckerChequeDAO {

	private final javax.sql.DataSource dataSource = ConnectionPool.getDataSource();

	// Fetches a cheque by batch number and cheque number.
	public OutwardCheque getCheque(String batchNumber, String chequeNumber) {

		String sql = "SELECT batch_number, " + "       cheque_number, " + "       drawer_account_number, "
				+ "       drawer_name, " + "       payee_account_number, " + "       payee_name, " + "       amount, "
				+ "       amount_in_words, " + "       cheque_date, " + "       front_image_path, "
				+ "       back_image_path, " + "       cheque_status, " + "       bank_code, " + "       branch_code, "
				+ "       city_code, " + "       return_reason_id, " + "       checker_remarks "
				+ "FROM outward_cheque " + "WHERE batch_number = ? " + "AND cheque_number = ?";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, batchNumber);
			statement.setString(2, chequeNumber);

			try (ResultSet rs = statement.executeQuery()) {

				if (rs.next()) {

					OutwardCheque cheque = new OutwardCheque();

					cheque.setBatchNumber(rs.getString("batch_number"));
					cheque.setChequeNumber(rs.getString("cheque_number"));
					cheque.setDrawerAccountNumber(rs.getString("drawer_account_number"));
					cheque.setDrawerName(rs.getString("drawer_name"));
					cheque.setPayeeAccountNumber(rs.getString("payee_account_number"));
					cheque.setPayeeName(rs.getString("payee_name"));
					cheque.setAmount(rs.getBigDecimal("amount"));
					cheque.setAmountInWords(rs.getString("amount_in_words"));

					Date chequeDate = rs.getDate("cheque_date");

					if (chequeDate != null) {
						cheque.setChequeDate(chequeDate.toLocalDate());
					}

					cheque.setFrontImagePath(rs.getString("front_image_path"));
					cheque.setBackImagePath(rs.getString("back_image_path"));
					cheque.setChequeStatus(rs.getString("cheque_status"));
					cheque.setBankCode(rs.getString("bank_code"));
					cheque.setBranchCode(rs.getString("branch_code"));
					cheque.setCityCode(rs.getString("city_code"));

					Object reasonObject = rs.getObject("return_reason_id");

					if (reasonObject != null) {
						cheque.setReturnReasonId(((Number) reasonObject).intValue());
					}

					cheque.setCheckerRemarks(rs.getString("checker_remarks"));

					return cheque;
				}
			}

		} catch (Exception e) {

			e.printStackTrace();

			throw new RuntimeException("Error while fetching cheque details", e);
		}

		return null;
	}

	// Fetches all cheques belonging to the specified batch.
	public List<OutwardCheque> getChequesByBatch(String batchNumber) {

		List<OutwardCheque> cheques = new ArrayList<>();

		String sql = "SELECT batch_number, " + "       cheque_number, " + "       drawer_account_number, "
				+ "       drawer_name, " + "       payee_account_number, " + "       payee_name, " + "       amount, "
				+ "       amount_in_words, " + "       cheque_date, " + "       front_image_path, "
				+ "       back_image_path, " + "       cheque_status, " + "       bank_code, " + "       branch_code, "
				+ "       city_code, " + "       return_reason_id, " + "       checker_remarks "
				+ "FROM outward_cheque " + "WHERE batch_number = ? " + "ORDER BY cheque_number";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, batchNumber);

			try (ResultSet rs = statement.executeQuery()) {

				while (rs.next()) {

					OutwardCheque cheque = new OutwardCheque();

					cheque.setBatchNumber(rs.getString("batch_number"));
					cheque.setChequeNumber(rs.getString("cheque_number"));
					cheque.setDrawerAccountNumber(rs.getString("drawer_account_number"));
					cheque.setDrawerName(rs.getString("drawer_name"));
					cheque.setPayeeAccountNumber(rs.getString("payee_account_number"));
					cheque.setPayeeName(rs.getString("payee_name"));
					cheque.setAmount(rs.getBigDecimal("amount"));
					cheque.setAmountInWords(rs.getString("amount_in_words"));

					Date chequeDate = rs.getDate("cheque_date");

					if (chequeDate != null) {
						cheque.setChequeDate(chequeDate.toLocalDate());
					}

					cheque.setFrontImagePath(rs.getString("front_image_path"));
					cheque.setBackImagePath(rs.getString("back_image_path"));
					cheque.setChequeStatus(rs.getString("cheque_status"));
					cheque.setBankCode(rs.getString("bank_code"));
					cheque.setBranchCode(rs.getString("branch_code"));
					cheque.setCityCode(rs.getString("city_code"));

					Object reasonObject = rs.getObject("return_reason_id");

					if (reasonObject != null) {
						cheque.setReturnReasonId(((Number) reasonObject).intValue());
					}

					cheque.setCheckerRemarks(rs.getString("checker_remarks"));

					cheques.add(cheque);
				}
			}

		} catch (Exception e) {

			e.printStackTrace();

			throw new RuntimeException("Error while fetching cheques for batch", e);
		}

		return cheques;
	}

	// Fetches batches completed by the checker for reporting.
	public List<OutwardBatch> getCheckerCompletedBatches() {

		List<OutwardBatch> batches = new ArrayList<>();

		String sql = "SELECT ob.batch_number, " + "       COUNT(oc.cheque_number) AS total_cheques "
				+ "FROM outward_batch ob " + "LEFT JOIN outward_cheque oc "
				+ "       ON ob.batch_number = oc.batch_number " + "WHERE UPPER(ob.batch_status) = 'CHECKER_VERIFIED' "
				+ "GROUP BY ob.batch_number " + "ORDER BY ob.batch_number DESC";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql);
				ResultSet rs = statement.executeQuery()) {

			while (rs.next()) {

				OutwardBatch batch = new OutwardBatch();

				batch.setBatchNumber(rs.getString("batch_number"));

				batch.setNumberOfCheques(rs.getInt("total_cheques"));

				batches.add(batch);
			}

		} catch (Exception e) {

			e.printStackTrace();

			throw new RuntimeException("Error while fetching Checker completed batches", e);
		}

		return batches;
	}

	// Fetches cheques re-verified by the specified checker after Maker correction.
	public List<OutwardCheque> getReVerifiedCheques(String batchNumber, long checkerUserId) {

		List<OutwardCheque> cheques = new ArrayList<>();

		String sql = "SELECT oc.batch_number, " + "       oc.cheque_number, " + "       oc.drawer_account_number, "
				+ "       oc.drawer_name, " + "       oc.payee_account_number, " + "       oc.payee_name, "
				+ "       oc.amount, " + "       oc.amount_in_words, " + "       oc.cheque_date, "
				+ "       oc.front_image_path, " + "       oc.back_image_path, " + "       oc.cheque_status, "
				+ "       oc.bank_code, " + "       oc.branch_code, " + "       oc.city_code, "
				+ "       oc.return_reason_id, " + "       oc.checker_remarks " + "FROM outward_cheque oc "
				+ "INNER JOIN cheque_processing cp " + "   ON cp.batch_number = oc.batch_number "
				+ "  AND cp.cheque_number = oc.cheque_number " + "WHERE oc.batch_number = ? " + "AND cp.checker_id = ? "
				+ "AND UPPER(TRIM(cp.checker_action)) = 'SEND_BACK' "
				+ "AND UPPER(TRIM(oc.cheque_status)) = 'RE_VERIFIED' " + "ORDER BY oc.cheque_number";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, batchNumber);
			statement.setLong(2, checkerUserId);

			try (ResultSet rs = statement.executeQuery()) {

				while (rs.next()) {

					OutwardCheque cheque = new OutwardCheque();

					cheque.setBatchNumber(rs.getString("batch_number"));
					cheque.setChequeNumber(rs.getString("cheque_number"));
					cheque.setDrawerAccountNumber(rs.getString("drawer_account_number"));
					cheque.setDrawerName(rs.getString("drawer_name"));
					cheque.setPayeeAccountNumber(rs.getString("payee_account_number"));
					cheque.setPayeeName(rs.getString("payee_name"));
					cheque.setAmount(rs.getBigDecimal("amount"));
					cheque.setAmountInWords(rs.getString("amount_in_words"));

					Date chequeDate = rs.getDate("cheque_date");

					if (chequeDate != null) {
						cheque.setChequeDate(chequeDate.toLocalDate());
					}

					cheque.setFrontImagePath(rs.getString("front_image_path"));
					cheque.setBackImagePath(rs.getString("back_image_path"));
					cheque.setChequeStatus(rs.getString("cheque_status"));
					cheque.setBankCode(rs.getString("bank_code"));
					cheque.setBranchCode(rs.getString("branch_code"));
					cheque.setCityCode(rs.getString("city_code"));

					Object reasonObject = rs.getObject("return_reason_id");

					if (reasonObject != null) {
						cheque.setReturnReasonId(((Number) reasonObject).intValue());
					}

					cheque.setCheckerRemarks(rs.getString("checker_remarks"));

					cheques.add(cheque);
				}
			}

		} catch (Exception e) {

			e.printStackTrace();

			throw new RuntimeException("Error while fetching re-verified cheques", e);
		}

		return cheques;
	}

	// Moves a re-verified cheque into checker processing.
	public boolean startCheckerProcessing(String batchNumber, String chequeNumber) {

		String sql = "UPDATE outward_cheque " + "SET cheque_status = 'CHECKER_PROCESSING' " + "WHERE batch_number = ? "
				+ "AND cheque_number = ? " + "AND UPPER(TRIM(cheque_status)) = " + "'RE_VERIFIED'";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, batchNumber);
			statement.setString(2, chequeNumber);

			int rows = statement.executeUpdate();

			return rows == 1;

		} catch (Exception e) {

			e.printStackTrace();

			throw new RuntimeException("Error while starting Checker processing", e);
		}
	}

	// Fetches the Maker and Checker processing details for a cheque.
	public ChequeProcessing getChequeProcessing(String batchNumber, String chequeNumber) {

		String sql = "SELECT batch_number, " + "       cheque_number, " + "       maker_id, " + "       maker_action, "
				+ "       maker_reason_code, " + "       checker_id, " + "       checker_action, "
				+ "       checker_reason_code " + "FROM cheque_processing " + "WHERE batch_number = ? "
				+ "AND cheque_number = ?";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, batchNumber);
			statement.setString(2, chequeNumber);

			try (ResultSet rs = statement.executeQuery()) {

				if (rs.next()) {

					ChequeProcessing processing = new ChequeProcessing();

					processing.setBatchNumber(rs.getString("batch_number"));
					processing.setChequeNumber(rs.getString("cheque_number"));

					Object makerId = rs.getObject("maker_id");

					if (makerId != null) {
						processing.setMakerId(((Number) makerId).intValue());
					}

					processing.setMakerAction(rs.getString("maker_action"));
					processing.setMakerReasonCode(rs.getString("maker_reason_code"));

					Object checkerId = rs.getObject("checker_id");

					if (checkerId != null) {
						processing.setCheckerId(((Number) checkerId).intValue());
					}

					processing.setCheckerAction(rs.getString("checker_action"));
					processing.setCheckerReasonCode(rs.getString("checker_reason_code"));

					return processing;
				}
			}

		} catch (Exception e) {

			e.printStackTrace();

			throw new RuntimeException("Error while fetching cheque processing details", e);
		}

		return null;
	}

	// Fetches the reason description for the specified reason code.
	public String getReasonName(String reasonCode) {

		if (reasonCode == null || reasonCode.trim().isEmpty()) {
			return null;
		}

		String sql = "SELECT reason_name " + "FROM return_reason_master " + "WHERE reason_code = ? "
				+ "AND active = true";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, reasonCode.trim());

			try (ResultSet rs = statement.executeQuery()) {

				if (rs.next()) {
					return rs.getString("reason_name");
				}
			}

		} catch (Exception e) {

			e.printStackTrace();

			throw new RuntimeException("Error while fetching reason description", e);
		}

		return null;
	}

	// Fetches active Checker return or rejection reasons by type.
	public List<ReturnReason> getReturnReasons(String reasonType) {

		List<ReturnReason> reasons = new ArrayList<>();

		String sql = "SELECT reason_code, " + "       reason_name, " + "       active " + "FROM return_reason_master "
				+ "WHERE active = true " + "AND UPPER(TRIM(role_name)) = 'CHECKER' "
				+ "AND UPPER(TRIM(reason_type)) = UPPER(TRIM(?)) " + "ORDER BY reason_name";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, reasonType);

			try (ResultSet resultSet = statement.executeQuery()) {

				while (resultSet.next()) {

					ReturnReason reason = new ReturnReason();

					reason.setReasonCode(resultSet.getString("reason_code"));
					reason.setReasonName(resultSet.getString("reason_name"));
					reason.setActive(resultSet.getBoolean("active"));

					reasons.add(reason);
				}
			}

		} catch (Exception e) {

			e.printStackTrace();

			throw new RuntimeException("Error while fetching Checker reasons", e);
		}

		return reasons;
	}

	// Saves the Checker decision and updates the related batch status.
	public boolean saveCheckerDecision(String batchNumber, String chequeNumber, long checkerId, String checkerAction,
			String checkerReasonCode, String checkerRemarks) {

		if (batchNumber == null || batchNumber.trim().isEmpty()) {
			throw new IllegalArgumentException("Batch number is required");
		}

		if (chequeNumber == null || chequeNumber.trim().isEmpty()) {
			throw new IllegalArgumentException("Cheque number is required");
		}

		if (checkerAction == null || checkerAction.trim().isEmpty()) {
			throw new IllegalArgumentException("Checker action is required");
		}

		checkerAction = checkerAction.trim().toUpperCase();

		if (checkerReasonCode != null) {
			checkerReasonCode = checkerReasonCode.trim();
			if (checkerReasonCode.isEmpty()) {
				checkerReasonCode = null;
			}
		}

		if (checkerRemarks != null && checkerRemarks.trim().isEmpty()) {
			checkerRemarks = null;
		}

		String chequeStatus;

		if ("ACCEPT".equals(checkerAction)) {

			chequeStatus = "CHECKER_ACCEPTED";
			checkerReasonCode = null;

		} else if ("REJECT".equals(checkerAction)) {

			if (checkerReasonCode == null) {
				throw new IllegalArgumentException("Reject reason is mandatory");
			}

			chequeStatus = "CHECKER_REJECTED";

		} else if ("SEND_BACK".equals(checkerAction)) {

			if (checkerReasonCode == null) {
				throw new IllegalArgumentException("Send Back reason is mandatory");
			}

			chequeStatus = "SENT_BACK_TO_MAKER";

		} else {

			throw new IllegalArgumentException("Invalid Checker action: " + checkerAction);
		}

		if ("REJECT".equals(checkerAction) || "SEND_BACK".equals(checkerAction)) {

			if (!isValidCheckerReason(checkerReasonCode, checkerAction)) {
				throw new IllegalArgumentException("Invalid Checker reason: " + checkerReasonCode);
			}
		}

		String previousCheckerActionSql =
				"SELECT checker_action " +
				"FROM cheque_processing " +
				"WHERE batch_number = ? " +
				"AND cheque_number = ? " +
				"FOR UPDATE";

		String insertProcessingSql =
				"INSERT INTO cheque_processing " +
				"(batch_number, cheque_number, checker_id, checker_action, checker_reason_code) " +
				"VALUES (?, ?, ?, ?, ?)";

		String updateProcessingSql =
				"UPDATE cheque_processing " +
				"SET checker_id = ?, checker_action = ?, checker_reason_code = ? " +
				"WHERE batch_number = ? AND cheque_number = ?";

		String updateChequeSql =
				"UPDATE outward_cheque " +
				"SET cheque_status = ?, checker_remarks = ? " +
				"WHERE batch_number = ? AND cheque_number = ?";

		/*
		 * Count every cheque in the batch that still has no final
		 * Checker decision. A missing cheque_processing row is also
		 * treated as pending.
		 */
		String remainingFirstCycleSql =
				"SELECT COUNT(*) " +
				"FROM outward_cheque oc " +
				"LEFT JOIN cheque_processing cp " +
				"ON cp.batch_number = oc.batch_number " +
				"AND cp.cheque_number = oc.cheque_number " +
				"WHERE oc.batch_number = ? " +
				"AND (cp.checker_action IS NULL " +
				"OR UPPER(TRIM(cp.checker_action)) NOT IN " +
				"('ACCEPT', 'REJECT', 'SEND_BACK'))";

		String sendBackFirstCycleSql =
				"SELECT COUNT(*) " +
				"FROM cheque_processing " +
				"WHERE batch_number = ? " +
				"AND UPPER(TRIM(checker_action)) = 'SEND_BACK'";

		String updateBatchVerifiedSql =
				"UPDATE outward_batch " +
				"SET batch_status = 'CHECKER_VERIFIED' " +
				"WHERE batch_number = ?";

		String updateBatchHoldSql =
				"UPDATE outward_batch " +
				"SET batch_status = 'ON_HOLD' " +
				"WHERE batch_number = ?";

		String updateBatchProcessingSql =
				"UPDATE outward_batch " +
				"SET batch_status = 'CHECKER_PROCESSING' " +
				"WHERE batch_number = ?";

		String pendingMakerSql =
				"SELECT COUNT(*) " +
				"FROM outward_cheque " +
				"WHERE batch_number = ? " +
				"AND UPPER(TRIM(cheque_status)) = 'SENT_BACK_TO_MAKER'";

		String pendingReVerificationSql =
				"SELECT COUNT(*) " +
				"FROM outward_cheque " +
				"WHERE batch_number = ? " +
				"AND UPPER(TRIM(cheque_status)) " +
				"IN ('RE_VERIFIED', 'CHECKER_PROCESSING')";

		try (Connection connection = dataSource.getConnection()) {

			connection.setAutoCommit(false);

			try {

				String previousCheckerAction = null;
				boolean processingExists = false;

				try (PreparedStatement statement =
						connection.prepareStatement(previousCheckerActionSql)) {

					statement.setString(1, batchNumber);
					statement.setString(2, chequeNumber);

					try (ResultSet rs = statement.executeQuery()) {

						if (rs.next()) {
							processingExists = true;
							previousCheckerAction =
									rs.getString("checker_action");
						}
					}
				}

				boolean reVerification =
						processingExists
						&& previousCheckerAction != null
						&& "SEND_BACK".equalsIgnoreCase(
								previousCheckerAction.trim());

				/*
				 * FIRST CHECKER DECISION:
				 * No processing row exists, so INSERT it.
				 */
				if (!processingExists) {

					try (PreparedStatement statement =
							connection.prepareStatement(insertProcessingSql)) {

						statement.setString(1, batchNumber);
						statement.setString(2, chequeNumber);
						statement.setLong(3, checkerId);
						statement.setString(4, checkerAction);

						if (checkerReasonCode == null) {
							statement.setNull(
									5,
									java.sql.Types.VARCHAR);
						} else {
							statement.setString(5, checkerReasonCode);
						}

						if (statement.executeUpdate() != 1) {
							throw new RuntimeException(
									"Unable to create cheque processing record");
						}
					}

				} else {

					/*
					 * EXISTING PROCESSING ROW:
					 * Update Checker decision.
					 */
					try (PreparedStatement statement =
							connection.prepareStatement(updateProcessingSql)) {

						statement.setLong(1, checkerId);
						statement.setString(2, checkerAction);

						if (checkerReasonCode == null) {
							statement.setNull(
									3,
									java.sql.Types.VARCHAR);
						} else {
							statement.setString(3, checkerReasonCode);
						}

						statement.setString(4, batchNumber);
						statement.setString(5, chequeNumber);

						if (statement.executeUpdate() != 1) {
							throw new RuntimeException(
									"Unable to update cheque processing record");
						}
					}
				}

				int chequeRows;

				try (PreparedStatement statement =
						connection.prepareStatement(updateChequeSql)) {

					statement.setString(1, chequeStatus);

					if (checkerRemarks == null) {
						statement.setNull(
								2,
								java.sql.Types.VARCHAR);
					} else {
						statement.setString(2, checkerRemarks);
					}

					statement.setString(3, batchNumber);
					statement.setString(4, chequeNumber);

					chequeRows = statement.executeUpdate();
				}

				if (chequeRows != 1) {
					throw new RuntimeException(
							"Cheque record not found");
				}

				/*
				 * RE-VERIFICATION FLOW
				 */
				if (reVerification) {

					int pendingMakerCheques;

					try (PreparedStatement statement =
							connection.prepareStatement(pendingMakerSql)) {

						statement.setString(1, batchNumber);

						try (ResultSet rs =
								statement.executeQuery()) {

							if (!rs.next()) {
								throw new RuntimeException(
										"Unable to determine pending Maker cheques");
							}

							pendingMakerCheques = rs.getInt(1);
						}
					}

					int pendingReVerificationCheques;

					try (PreparedStatement statement =
							connection.prepareStatement(
									pendingReVerificationSql)) {

						statement.setString(1, batchNumber);

						try (ResultSet rs =
								statement.executeQuery()) {

							if (!rs.next()) {
								throw new RuntimeException(
										"Unable to determine pending re-verification cheques");
							}

							pendingReVerificationCheques =
									rs.getInt(1);
						}
					}

					if (pendingMakerCheques > 0) {

						try (PreparedStatement statement =
								connection.prepareStatement(
										updateBatchHoldSql)) {

							statement.setString(1, batchNumber);
							statement.executeUpdate();
						}

					} else if (pendingReVerificationCheques > 0) {

						try (PreparedStatement statement =
								connection.prepareStatement(
										updateBatchProcessingSql)) {

							statement.setString(1, batchNumber);
							statement.executeUpdate();
						}

					} else {

						try (PreparedStatement statement =
								connection.prepareStatement(
										updateBatchVerifiedSql)) {

							statement.setString(1, batchNumber);
							statement.executeUpdate();
						}
					}

					connection.commit();
					return true;
				}

				/*
				 * FIRST-CYCLE FLOW
				 */
				int remainingCheques;

				try (PreparedStatement statement =
						connection.prepareStatement(
								remainingFirstCycleSql)) {

					statement.setString(1, batchNumber);

					try (ResultSet rs =
							statement.executeQuery()) {

						if (!rs.next()) {
							throw new RuntimeException(
									"Unable to determine batch completion");
						}

						remainingCheques = rs.getInt(1);
					}
				}

				int sendBackCheques;

				try (PreparedStatement statement =
						connection.prepareStatement(
								sendBackFirstCycleSql)) {

					statement.setString(1, batchNumber);

					try (ResultSet rs =
							statement.executeQuery()) {

						if (!rs.next()) {
							throw new RuntimeException(
									"Unable to determine Send Back status");
						}

						sendBackCheques = rs.getInt(1);
					}
				}

				if (sendBackCheques > 0) {

					try (PreparedStatement statement =
							connection.prepareStatement(
									updateBatchHoldSql)) {

						statement.setString(1, batchNumber);
						statement.executeUpdate();
					}

				} else if (remainingCheques > 0) {

					try (PreparedStatement statement =
							connection.prepareStatement(
									updateBatchProcessingSql)) {

						statement.setString(1, batchNumber);
						statement.executeUpdate();
					}

				} else {

					try (PreparedStatement statement =
							connection.prepareStatement(
									updateBatchVerifiedSql)) {

						statement.setString(1, batchNumber);
						statement.executeUpdate();
					}
				}

				connection.commit();
				return true;

			} catch (Exception e) {

				connection.rollback();
				throw e;
			}

		} catch (Exception e) {

			e.printStackTrace();

			throw new RuntimeException(
					"Error while saving Checker decision", e);
		}
	}

	// Validates the Checker reason against the active reason master.
	private boolean isValidCheckerReason(String reasonCode, String action) {

		if (reasonCode == null || reasonCode.trim().isEmpty()) {
			return false;
		}

		String sql = "SELECT reason_code " + "FROM return_reason_master " + "WHERE reason_code = ? "
				+ "AND active = true " + "AND UPPER(TRIM(role_name)) = 'CHECKER' "
				+ "AND UPPER(TRIM(reason_type)) = UPPER(TRIM(?))";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, reasonCode.trim());
			statement.setString(2, action.trim());

			try (ResultSet rs = statement.executeQuery()) {
				return rs.next();
			}

		} catch (Exception e) {

			e.printStackTrace();

			throw new RuntimeException("Error while validating Checker reason", e);
		}
	}

	// Fetches CBS account details for the specified account number.
	public Map<String, String> getCbsAccount(String accountNumber) {

		String sql = "SELECT account_number, " + "       account_holder_name, " + "       account_status "
				+ "FROM account_master " + "WHERE account_number = ?";

		Map<String, String> account = null;

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, accountNumber);

			try (ResultSet rs = statement.executeQuery()) {

				if (rs.next()) {

					account = new HashMap<>();

					account.put("accountNumber", rs.getString("account_number"));

					account.put("accountHolderName", rs.getString("account_holder_name"));

					account.put("accountStatus", rs.getString("account_status"));
				}
			}

		} catch (Exception e) {

			e.printStackTrace();

			throw new RuntimeException("Error while fetching CBS account", e);
		}

		return account;
	}
}