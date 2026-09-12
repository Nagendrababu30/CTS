package com.iispl.cts.dao.outward.checker;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.iispl.cts.data.CTSStaticData;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;

public class CheckerBatchDAO {

    /*
     * Get batches currently assigned to a particular Checker.
     *
     * These batches come from outward_batch_assignment.
     */
	public List<OutwardBatch> getCheckerBatches(
	        String checkerUserId,
	        String searchText,
	        int pageSize,
	        int offset) {

	    List<OutwardBatch> batches = new ArrayList<>();

	    String sql = "SELECT ob.batch_number, "
	            + "       ob.branch_code, "
	            + "       ob.cheque_count, "
	            + "       ob.batch_folder_path, "
	            + "       ob.created_by, "
	            + "       ob.created_at, "
	            + "       ob.batch_status, "
	            + "       oba.user_id, "
	            + "       oba.assignment_status, "
	            + "       oba.assigned_at, "
	            + "       oba.started_at, "
	            + "       oba.completed_at "
	            + "FROM outward_batch ob "
	            + "JOIN outward_batch_assignment oba "
	            + "  ON ob.batch_number = oba.batch_number "
	            + "WHERE oba.user_id = ? "
	            + "  AND UPPER(oba.assignment_role) = 'CHECKER' "
	            + "  AND UPPER(oba.assignment_status) "
	            + "      IN ('ASSIGNED', 'IN_PROGRESS') "
	            + "  AND UPPER(ob.batch_status) <> 'CHECKER_COMPLETED' "
	            + "  AND (? IS NULL OR ? = '' OR "
	            + "       LOWER(ob.batch_number) LIKE LOWER(?)) "
	            + "ORDER BY oba.assigned_at DESC "
	            + "LIMIT ? OFFSET ?";

	    try (Connection connection = CTSStaticData.getConnection();
	         PreparedStatement statement =
	                 connection.prepareStatement(sql)) {

	        statement.setLong(
	                1,
	                Long.parseLong(checkerUserId));

	        String search =
	                searchText == null
	                        ? ""
	                        : searchText.trim();

	        String searchPattern =
	                "%" + search + "%";

	        statement.setString(2, search);
	        statement.setString(3, search);
	        statement.setString(4, searchPattern);

	        statement.setInt(5, pageSize);
	        statement.setInt(6, offset);

	        try (ResultSet rs = statement.executeQuery()) {

	            while (rs.next()) {

	                OutwardBatch batch =
	                        new OutwardBatch();

	                batch.setBatchNumber(
	                        rs.getString("batch_number"));

	                batch.setBranchCode(
	                        rs.getString("branch_code"));

	                batch.setNumberOfCheques(
	                        rs.getInt("cheque_count"));

	                batch.setBatchFolderPath(
	                        rs.getString("batch_folder_path"));

	                batch.setCreatedBy(
	                        String.valueOf(
	                                rs.getInt("created_by")));

	                if (rs.getTimestamp("created_at") != null) {

	                    batch.setCreatedAt(
	                            rs.getTimestamp("created_at")
	                                    .toLocalDateTime());
	                }

	                batch.setBatchStatus(
	                        rs.getString("batch_status"));

	                batch.setCheckerUserNumber(
	                        checkerUserId);

	                if (rs.getTimestamp("started_at") != null) {

	                    batch.setCheckerStartedAt(
	                            rs.getTimestamp("started_at")
	                                    .toLocalDateTime());
	                }

	                if (rs.getTimestamp("completed_at") != null) {

	                    batch.setCheckerCompletedAt(
	                            rs.getTimestamp("completed_at")
	                                    .toLocalDateTime());
	                }

	                batch.setLockStatus(
	                        rs.getString("assignment_status"));

	                batches.add(batch);
	            }
	        }

	    } catch (Exception e) {

	        e.printStackTrace();

	        throw new RuntimeException(
	                "Error while fetching Checker batches",
	                e);
	    }

	    return batches;
	}



    /*
     * Get batches available for Checkers to take.
     */
    public List<OutwardBatch> getSubmittedBatches() {

        List<OutwardBatch> batches =
                new ArrayList<>();

        String sql = "SELECT ob.batch_number, "
                + "       ob.branch_code, "
                + "       ob.cheque_count, "
                + "       ob.batch_folder_path, "
                + "       ob.created_by, "
                + "       ob.created_at, "
                + "       ob.batch_status "
                + "FROM outward_batch ob "
                + "WHERE UPPER(ob.batch_status) = "
                + "      'SUBMITTED_TO_CHECKER' "
                + "AND NOT EXISTS ( "
                + "    SELECT 1 "
                + "    FROM outward_batch_assignment oba "
                + "    WHERE oba.batch_number = ob.batch_number "
                + "      AND UPPER(oba.assignment_role) = 'CHECKER' "
                + "      AND UPPER(oba.assignment_status) "
                + "          IN ('ASSIGNED', 'IN_PROGRESS') "
                + ") "
                + "ORDER BY ob.created_at ASC";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs =
                     statement.executeQuery()) {

            while (rs.next()) {

                OutwardBatch batch =
                        new OutwardBatch();

                batch.setBatchNumber(
                        rs.getString("batch_number"));

                batch.setBranchCode(
                        rs.getString("branch_code"));

                batch.setNumberOfCheques(
                        rs.getInt("cheque_count"));

                batch.setBatchFolderPath(
                        rs.getString("batch_folder_path"));

                batch.setCreatedBy(
                        String.valueOf(
                                rs.getInt("created_by")));

                if (rs.getTimestamp("created_at") != null) {

                    batch.setCreatedAt(
                            rs.getTimestamp("created_at")
                                    .toLocalDateTime());
                }

                batch.setBatchStatus(
                        rs.getString("batch_status"));

                batches.add(batch);
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching submitted "
                    + "Checker batches",
                    e);
        }

        return batches;
    }


    /*
     * Get a single batch by batch number.
     */
    public OutwardBatch getBatchByNumber(
            String batchNumber) {

        String sql = "SELECT batch_number, "
                + "       branch_code, "
                + "       cheque_count, "
                + "       batch_folder_path, "
                + "       created_by, "
                + "       created_at, "
                + "       batch_status "
                + "FROM outward_batch "
                + "WHERE batch_number = ?";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber);

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {

                    OutwardBatch batch =
                            new OutwardBatch();

                    batch.setBatchNumber(
                            rs.getString("batch_number"));

                    batch.setBranchCode(
                            rs.getString("branch_code"));

                    batch.setNumberOfCheques(
                            rs.getInt("cheque_count"));

                    batch.setBatchFolderPath(
                            rs.getString(
                                    "batch_folder_path"));

                    batch.setCreatedBy(
                            String.valueOf(
                                    rs.getInt("created_by")));

                    batch.setBatchStatus(
                            rs.getString("batch_status"));

                    if (rs.getTimestamp("created_at")
                            != null) {

                        batch.setCreatedAt(
                                rs.getTimestamp(
                                        "created_at")
                                        .toLocalDateTime());
                    }

                    return batch;
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching batch: "
                    + batchNumber,
                    e);
        }

        return null;
    }


    /*
     * Get all cheques belonging to a batch.
     *
     * Only columns that actually exist in
     * outward_cheque are selected here.
     */
    public List<OutwardCheque> getChequesByBatchNumber(
            String batchNumber) {

        List<OutwardCheque> cheques =
                new ArrayList<>();

        String sql = "SELECT batch_number, "
                + "       cheque_number, "
                + "       city_code, "
                + "       bank_code, "
                + "       branch_code, "
                + "       drawer_account_number, "
                + "       drawer_name, "
                + "       payee_account_number, "
                + "       payee_name, "
                + "       amount, "
                + "       amount_in_words, "
                + "       cheque_date, "
                + "       front_image_path, "
                + "       back_image_path, "
                + "       cheque_status, "
                + "       return_reason_id, "
                + "       checker_remarks "
                + "FROM outward_cheque "
                + "WHERE batch_number = ? "
                + "ORDER BY cheque_number";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber);

            try (ResultSet rs =
                         statement.executeQuery()) {

                while (rs.next()) {

                    OutwardCheque cheque =
                            new OutwardCheque();

                    cheque.setBatchNumber(
                            rs.getString(
                                    "batch_number"));

                    cheque.setChequeNumber(
                            rs.getString(
                                    "cheque_number"));

                    cheque.setCityCode(
                            rs.getString(
                                    "city_code"));

                    cheque.setBankCode(
                            rs.getString(
                                    "bank_code"));

                    cheque.setBranchCode(
                            rs.getString(
                                    "branch_code"));

                    cheque.setDrawerAccountNumber(
                            rs.getString(
                                    "drawer_account_number"));

                    cheque.setDrawerName(
                            rs.getString(
                                    "drawer_name"));

                    cheque.setPayeeAccountNumber(
                            rs.getString(
                                    "payee_account_number"));

                    cheque.setPayeeName(
                            rs.getString(
                                    "payee_name"));

                    BigDecimal amount =
                            rs.getBigDecimal("amount");

                    cheque.setAmount(amount);

                    cheque.setAmountInWords(
                            rs.getString(
                                    "amount_in_words"));

                    if (rs.getDate("cheque_date")
                            != null) {

                        cheque.setChequeDate(
                                rs.getDate("cheque_date")
                                        .toLocalDate());
                    }

                    cheque.setFrontImagePath(
                            rs.getString(
                                    "front_image_path"));

                    cheque.setBackImagePath(
                            rs.getString(
                                    "back_image_path"));

                    cheque.setChequeStatus(
                            rs.getString(
                                    "cheque_status"));

                    Object reasonObject =
                            rs.getObject(
                                    "return_reason_id");

                    if (reasonObject != null) {

                        cheque.setReturnReasonId(
                                ((Number) reasonObject)
                                        .intValue());
                    }

                    cheque.setCheckerRemarks(
                            rs.getString(
                                    "checker_remarks"));

                    cheques.add(cheque);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching cheques "
                    + "for batch: "
                    + batchNumber,
                    e);
        }

        return cheques;
    }


    /*
     * Existing compatibility method.
     */
    public List<OutwardCheque> getChequesByBatchId(
            String batchId) {

        return getChequesByBatchNumber(batchId);
    }


    /*
     * Check whether an account exists in account_master.
     *
     * CBS validation itself is handled by
     * CheckerProcessingService.
     */
    public boolean accountExists(
            String accountNumber) {

        String sql = "SELECT 1 "
                + "FROM account_master "
                + "WHERE account_number = ?";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    accountNumber);

            try (ResultSet rs =
                         statement.executeQuery()) {

                return rs.next();
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while checking account: "
                    + accountNumber,
                    e);
        }
    }


    /*
     * Get total number of batches assigned
     * to the current Checker.
     */
    public int getCheckerBatchCount(
            String checkerUserId,
            String searchText) {

        String sql = "SELECT COUNT(*) "
                + "FROM outward_batch ob "
                + "JOIN outward_batch_assignment oba "
                + "  ON ob.batch_number = oba.batch_number "
                + "WHERE oba.user_id = ? "
                + "  AND UPPER(oba.assignment_role) = "
                + "      'CHECKER' "
                + "  AND UPPER(oba.assignment_status) "
                + "      IN ('ASSIGNED', 'IN_PROGRESS') "
                + "  AND UPPER(ob.batch_status) <> "
                + "      'CHECKER_COMPLETED' "
                + "  AND (? IS NULL OR ? = '' OR "
                + "       LOWER(ob.batch_number) "
                + "       LIKE LOWER(?))";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(
                    1,
                    Long.parseLong(checkerUserId));

            String search =
                    searchText == null
                            ? ""
                            : searchText.trim();

            String searchPattern =
                    "%" + search + "%";

            statement.setString(2, search);
            statement.setString(3, search);
            statement.setString(4, searchPattern);

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while counting Checker batches",
                    e);
        }

        return 0;
    }
}	