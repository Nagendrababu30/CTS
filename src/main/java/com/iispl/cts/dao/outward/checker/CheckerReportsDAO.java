package com.iispl.cts.dao.outward.checker;

import com.cts.inward.config.ConnectionPool;
import com.iispl.cts.data.CTSStaticData;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * CHECKER REPORTS DAO
 * ============================================================
 *
 * Responsibilities:
 *
 * 1. Get batches for Reports screen
 * 2. Get selected batch
 * 3. Get rejected cheques
 * 4. Check RRF availability
 * 5. Get rejected cheque count
 *
 * IMPORTANT:
 *
 * RRF availability is determined from:
 *
 *     cheque_processing.checker_action = 'REJECT'
 *
 * NOT simply from:
 *
 *     outward_cheque.return_reason_id
 *
 * CFX and CIBF are available for every selected batch.
 *
 * ============================================================
 */
public class CheckerReportsDAO {
	private final javax.sql.DataSource dataSource =ConnectionPool.getDataSource();


    // ============================================================
    // GET BATCHES AVAILABLE FOR REPORTS
    // ============================================================
    //
    // Existing Reports implementation was using ASSIGNED as the
    // temporary report status. That behaviour is preserved here.
    //
    // ============================================================

    public List<OutwardBatch> getCheckerCompletedBatches() {

        List<OutwardBatch> batches =
                new ArrayList<OutwardBatch>();

        String sql =
                "SELECT batch_number, "
                        + "       branch_code, "
                        + "       cheque_count, "
                        + "       batch_folder_path, "
                        + "       created_by, "
                        + "       created_at, "
                        + "       batch_status "
                        + "FROM public.outward_batch "
                        + "WHERE UPPER(batch_status) = 'ASSIGNED' "
                        + "ORDER BY batch_number DESC";

        try (Connection connection =
        		dataSource.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql);

             ResultSet rs =
                     statement.executeQuery()) {

            while (rs.next()) {

                OutwardBatch batch =
                        new OutwardBatch();

                // =================================================
                // BATCH NUMBER
                // =================================================

                batch.setBatchNumber(
                        rs.getString("batch_number")
                );

                // =================================================
                // BRANCH
                // =================================================

                batch.setBranchCode(
                        rs.getString("branch_code")
                );

                // =================================================
                // CHEQUE COUNT
                // =================================================

                batch.setNumberOfCheques(
                        rs.getInt("cheque_count")
                );

                // =================================================
                // FOLDER PATH
                // =================================================

                batch.setBatchFolderPath(
                        rs.getString("batch_folder_path")
                );

                // =================================================
                // CREATED BY
                // =================================================

                batch.setCreatedBy(
                        String.valueOf(
                                rs.getInt("created_by")
                        )
                );

                // =================================================
                // CREATED AT
                // =================================================

                if (rs.getTimestamp("created_at") != null) {

                    batch.setCreatedAt(
                            rs.getTimestamp("created_at")
                                    .toLocalDateTime()
                    );
                }

                // =================================================
                // STATUS
                // =================================================

                batch.setBatchStatus(
                        rs.getString("batch_status")
                );

                batches.add(batch);
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching Checker Reports batches",
                    e
            );
        }

        return batches;
    }

    // ============================================================
    // GET SINGLE BATCH
    // ============================================================

    public OutwardBatch getBatchByNumber(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return null;
        }

        String sql =
                "SELECT batch_number, "
                        + "       branch_code, "
                        + "       cheque_count, "
                        + "       batch_folder_path, "
                        + "       created_by, "
                        + "       created_at, "
                        + "       batch_status "
                        + "FROM public.outward_batch "
                        + "WHERE batch_number = ?";

        try (Connection connection =
                     CTSStaticData.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber.trim()
            );

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {

                    OutwardBatch batch =
                            new OutwardBatch();

                    batch.setBatchNumber(
                            rs.getString("batch_number")
                    );

                    batch.setBranchCode(
                            rs.getString("branch_code")
                    );

                    batch.setNumberOfCheques(
                            rs.getInt("cheque_count")
                    );

                    batch.setBatchFolderPath(
                            rs.getString("batch_folder_path")
                    );

                    batch.setCreatedBy(
                            String.valueOf(
                                    rs.getInt("created_by")
                            )
                    );

                    if (rs.getTimestamp("created_at") != null) {

                        batch.setCreatedAt(
                                rs.getTimestamp("created_at")
                                        .toLocalDateTime()
                        );
                    }

                    batch.setBatchStatus(
                            rs.getString("batch_status")
                    );

                    return batch;
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching batch: "
                            + batchNumber,
                    e
            );
        }

        return null;
    }

    // ============================================================
    // GET ALL CHEQUES FOR SELECTED BATCH
    // ============================================================
    //
    // Used by CFX and CIBF.
    //
    // These reports are available for every batch.
    //
    // ============================================================

    public List<OutwardCheque> getBatchCheques(
            String batchNumber) {

        List<OutwardCheque> cheques =
                new ArrayList<OutwardCheque>();

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return cheques;
        }

        String sql =
                "SELECT batch_number, "
                        + "       cheque_number, "
                        + "       city_code, "
                        + "       bank_code, "
                        + "       branch_code, "
                        + "       drawer_account_number, "
                        + "       drawer_name, "
                        + "       depositor_account_number, "
                        + "       depositor_name, "
                        + "       payee_account_number, "
                        + "       payee_name, "
                        + "       amount, "
                        + "       amount_in_words, "
                        + "       cheque_date, "
                        + "       front_image_path, "
                        + "       back_image_path, "
                        + "       cheque_status, "
                        + "       return_reason_id, "
                        + "       checker_remarks, "
                        + "       created_by, "
                        + "       created_at "
                        + "FROM public.outward_cheque "
                        + "WHERE batch_number = ? "
                        + "ORDER BY cheque_number";

        try (Connection connection =
        		dataSource.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber.trim()
            );

            try (ResultSet rs =
                         statement.executeQuery()) {

                while (rs.next()) {

                    OutwardCheque cheque =
                            mapCheque(rs);

                    cheques.add(cheque);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching cheques for batch: "
                            + batchNumber,
                    e
            );
        }

        return cheques;
    }

    // ============================================================
    // GET REJECTED CHEQUES
    // ============================================================
    //
    // VERY IMPORTANT:
    //
    // The rejection decision comes from:
    //
    // cheque_processing
    //
    // checker_action = 'REJECT'
    //
    // Therefore RRF uses this query.
    //
    // We join outward_cheque only to obtain the cheque details
    // required for RRF generation/display.
    //
    // ============================================================

    public List<OutwardCheque> getRejectedCheques(
            String batchNumber) {

        List<OutwardCheque> rejectedCheques =
                new ArrayList<OutwardCheque>();

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return rejectedCheques;
        }

        String sql =
                "SELECT oc.batch_number, "
                        + "       oc.cheque_number, "
                        + "       oc.city_code, "
                        + "       oc.bank_code, "
                        + "       oc.branch_code, "
                        + "       oc.drawer_account_number, "
                        + "       oc.drawer_name, "
                        + "       oc.depositor_account_number, "
                        + "       oc.depositor_name, "
                        + "       oc.payee_account_number, "
                        + "       oc.payee_name, "
                        + "       oc.amount, "
                        + "       oc.amount_in_words, "
                        + "       oc.cheque_date, "
                        + "       oc.front_image_path, "
                        + "       oc.back_image_path, "
                        + "       oc.cheque_status, "
                        + "       oc.return_reason_id, "
                        + "       oc.checker_remarks, "
                        + "       oc.created_by, "
                        + "       oc.created_at "
                        + "FROM public.outward_cheque oc "
                        + "INNER JOIN public.cheque_processing cp "
                        + "        ON cp.batch_number = oc.batch_number "
                        + "       AND cp.cheque_number = oc.cheque_number "
                        + "WHERE oc.batch_number = ? "
                        + "  AND UPPER(cp.checker_action) = 'REJECT' "
                        + "ORDER BY oc.cheque_number";

        try (Connection connection =
        		dataSource.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber.trim()
            );

            try (ResultSet rs =
                         statement.executeQuery()) {

                while (rs.next()) {

                    OutwardCheque cheque =
                            mapCheque(rs);

                    rejectedCheques.add(
                            cheque
                    );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching rejected cheques for batch: "
                            + batchNumber,
                    e
            );
        }

        return rejectedCheques;
    }

    // ============================================================
    // CHECK WHETHER RRF IS AVAILABLE
    // ============================================================
    //
    // TRUE:
    //     At least one rejected checker decision exists.
    //
    // FALSE:
    //     No rejected checker decision exists.
    //
    // ============================================================

    public boolean hasRejectedCheques(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return false;
        }

        String sql =
                "SELECT EXISTS ( "
                        + "    SELECT 1 "
                        + "    FROM public.cheque_processing "
                        + "    WHERE batch_number = ? "
                        + "      AND UPPER(checker_action) = 'REJECT' "
                        + ")";

        try (Connection connection =
        		dataSource.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber.trim()
            );

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {

                    return rs.getBoolean(1);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while checking RRF availability for batch: "
                            + batchNumber,
                    e
            );
        }

        return false;
    }

    // ============================================================
    // GET REJECTED CHEQUE COUNT
    // ============================================================

    public int getRejectedChequeCount(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return 0;
        }

        String sql =
                "SELECT COUNT(*) "
                        + "FROM public.cheque_processing "
                        + "WHERE batch_number = ? "
                        + "  AND UPPER(checker_action) = 'REJECT'";

        try (Connection connection =
        		dataSource.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber.trim()
            );

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {

                    return rs.getInt(1);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while counting rejected cheques for batch: "
                            + batchNumber,
                    e
            );
        }

        return 0;
    }

    // ============================================================
    // MAP OUTWARD CHEQUE
    // ============================================================

    private OutwardCheque mapCheque(
            ResultSet rs)
            throws Exception {

        OutwardCheque cheque =
                new OutwardCheque();

        // ========================================================
        // BASIC INFORMATION
        // ========================================================

        cheque.setBatchNumber(
                rs.getString("batch_number")
        );

        cheque.setChequeNumber(
                rs.getString("cheque_number")
        );

        cheque.setCityCode(
                rs.getString("city_code")
        );

        cheque.setBankCode(
                rs.getString("bank_code")
        );

        cheque.setBranchCode(
                rs.getString("branch_code")
        );

        // ========================================================
        // DRAWER
        // ========================================================

        cheque.setDrawerAccountNumber(
                rs.getString("drawer_account_number")
        );

        cheque.setDrawerName(
                rs.getString("drawer_name")
        );

        // ========================================================
        // DEPOSITOR
        // ========================================================

        cheque.setDepositorAccountNumber(
                rs.getString(
                        "depositor_account_number"
                )
        );

        cheque.setDepositorName(
                rs.getString(
                        "depositor_name"
                )
        );

        // ========================================================
        // PAYEE
        // ========================================================

        cheque.setPayeeAccountNumber(
                rs.getString(
                        "payee_account_number"
                )
        );

        cheque.setPayeeName(
                rs.getString(
                        "payee_name"
                )
        );

        // ========================================================
        // AMOUNT
        // ========================================================

        BigDecimal amount =
                rs.getBigDecimal("amount");

        cheque.setAmount(amount);

        cheque.setAmountInWords(
                rs.getString(
                        "amount_in_words"
                )
        );

        // ========================================================
        // CHEQUE DATE
        // ========================================================

        if (rs.getDate("cheque_date") != null) {

            cheque.setChequeDate(
                    rs.getDate("cheque_date")
                            .toLocalDate()
            );
        }

        // ========================================================
        // IMAGES
        // ========================================================

        cheque.setFrontImagePath(
                rs.getString(
                        "front_image_path"
                )
        );

        cheque.setBackImagePath(
                rs.getString(
                        "back_image_path"
                )
        );

        // ========================================================
        // STATUS
        // ========================================================

        cheque.setChequeStatus(
                rs.getString(
                        "cheque_status"
                )
        );

        // ========================================================
        // RETURN REASON
        // ========================================================

        Object reasonObject =
                rs.getObject(
                        "return_reason_id"
                );

        if (reasonObject != null) {

            cheque.setReturnReasonId(
                    ((Number) reasonObject)
                            .intValue()
            );
        }

        // ========================================================
        // CHECKER REMARKS
        // ========================================================

        cheque.setCheckerRemarks(
                rs.getString(
                        "checker_remarks"
                )
        );

        // ========================================================
        // CREATED BY
        // ========================================================

        cheque.setCreatedBy(
                rs.getString(
                        "created_by"
                )
        );

        // ========================================================
        // CREATED AT
        // ========================================================

        if (rs.getTimestamp("created_at") != null) {

            cheque.setCreatedAt(
                    rs.getTimestamp("created_at")
                            .toLocalDateTime()
            );
        }

        return cheque;
    }
}