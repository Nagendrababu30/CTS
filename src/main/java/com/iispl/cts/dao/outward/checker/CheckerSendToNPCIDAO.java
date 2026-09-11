package com.iispl.cts.dao.outward.checker;

import com.cts.inward.config.ConnectionPool;
import com.iispl.cts.model.outward.OutwardBatch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * CHECKER SEND TO NPCI DAO
 * ============================================================
 *
 * Handles database operations for the Send to NPCI screen.
 *
 * Main responsibilities:
 *
 * 1. Get batches which are ready to be sent to NPCI.
 * 2. Get cheque counts for each batch.
 * 3. Check whether a particular batch is ready.
 * 4. Mark a batch as NPCI_SENT after successful submission.
 *
 * IMPORTANT:
 *
 * Once batch_status becomes NPCI_SENT, the existing
 * CheckerDashboardDAO will automatically stop displaying
 * that batch because its dashboard query only selects
 * specific Checker statuses.
 *
 * ============================================================
 */
public class CheckerSendToNPCIDAO {

    // ============================================================
    // DATA SOURCE
    // ============================================================

    private final javax.sql.DataSource dataSource =
            ConnectionPool.getDataSource();

    // ============================================================
    // GET BATCHES READY FOR NPCI
    // ============================================================

    /**
     * Gets batches which have completed Checker processing
     * and are ready to be sent to NPCI.
     *
     * The current project uses ASSIGNED as the temporary
     * completed/report status, so that status is preserved here.
     *
     * @return list of batches ready for NPCI
     */
    public List<OutwardBatch> getBatchesReadyForNPCI() {

        List<OutwardBatch> batches =
                new ArrayList<>();

        String sql =
                "SELECT " +
                "    ob.batch_number, " +
                "    ob.branch_code, " +
                "    ob.cheque_count, " +
                "    ob.batch_folder_path, " +
                "    ob.created_by, " +
                "    ob.created_at, " +
                "    ob.batch_status, " +
                "    COUNT(oc.cheque_number) AS total_cheques, " +
                "    COUNT(CASE " +
                "        WHEN UPPER(COALESCE(oc.cheque_status, '')) " +
                "             <> 'REJECT' " +
                "        THEN 1 " +
                "    END) AS accepted_cheques, " +
                "    COUNT(CASE " +
                "        WHEN UPPER(COALESCE(cp.checker_action, '')) " +
                "             = 'REJECT' " +
                "        THEN 1 " +
                "    END) AS rejected_cheques " +
                "FROM public.outward_batch ob " +
                "LEFT JOIN public.outward_cheque oc " +
                "    ON ob.batch_number = oc.batch_number " +
                "LEFT JOIN public.cheque_processing cp " +
                "    ON cp.batch_number = oc.batch_number " +
                "   AND cp.cheque_number = oc.cheque_number " +
                "WHERE UPPER(ob.batch_status) = 'ASSIGNED' " +
                "GROUP BY " +
                "    ob.batch_number, " +
                "    ob.branch_code, " +
                "    ob.cheque_count, " +
                "    ob.batch_folder_path, " +
                "    ob.created_by, " +
                "    ob.created_at, " +
                "    ob.batch_status " +
                "ORDER BY ob.created_at DESC";

        try (
                Connection con =
                        dataSource.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql);

                ResultSet rs =
                        ps.executeQuery()
        ) {

            while (rs.next()) {

                OutwardBatch batch =
                        new OutwardBatch();

                // ====================================================
                // BATCH NUMBER
                // ====================================================

                batch.setBatchNumber(
                        rs.getString("batch_number")
                );

                // ====================================================
                // BRANCH
                // ====================================================

                batch.setBranchCode(
                        rs.getString("branch_code")
                );

                // ====================================================
                // TOTAL CHEQUE COUNT
                // ====================================================

                batch.setNumberOfCheques(
                        rs.getInt("total_cheques")
                );

                // ====================================================
                // FOLDER PATH
                // ====================================================

                batch.setBatchFolderPath(
                        rs.getString("batch_folder_path")
                );

                // ====================================================
                // CREATED BY
                // ====================================================

                int createdBy =
                        rs.getInt("created_by");

                if (!rs.wasNull()) {

                    batch.setCreatedBy(
                            String.valueOf(createdBy)
                    );
                }

                // ====================================================
                // CREATED AT
                // ====================================================

                if (rs.getTimestamp("created_at") != null) {

                    batch.setCreatedAt(
                            rs.getTimestamp("created_at")
                                    .toLocalDateTime()
                    );
                }

                // ====================================================
                // STATUS
                // ====================================================

                batch.setBatchStatus(
                        rs.getString("batch_status")
                );

                // ====================================================
                // ADD BATCH
                // ====================================================

                batches.add(batch);
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to load batches ready for NPCI.",
                    e
            );
        }

        return batches;
    }

    // ============================================================
    // CHECK WHETHER BATCH IS READY
    // ============================================================

    /**
     * Checks whether a particular batch is currently
     * ready for NPCI submission.
     *
     * @param batchNumber batch number
     * @return true when batch is ready
     */
    public boolean isBatchReadyForNPCI(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return false;
        }

        String sql =
                "SELECT EXISTS (" +
                "    SELECT 1 " +
                "    FROM public.outward_batch " +
                "    WHERE batch_number = ? " +
                "      AND UPPER(batch_status) = 'ASSIGNED' " +
                ")";

        try (
                Connection con =
                        dataSource.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setString(
                    1,
                    batchNumber.trim()
            );

            try (
                    ResultSet rs =
                            ps.executeQuery()
            ) {

                if (rs.next()) {

                    return rs.getBoolean(1);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to check NPCI batch status.",
                    e
            );
        }

        return false;
    }

    // ============================================================
    // MARK BATCH AS NPCI SENT
    // ============================================================

    /**
     * Changes the batch status to NPCI_SENT.
     *
     * This must be called ONLY after the actual NPCI submission
     * has succeeded.
     *
     * Once the status becomes NPCI_SENT:
     *
     *     Checker Dashboard
     *              ↓
     *     does not return this batch
     *
     * because CheckerDashboardDAO only loads its allowed
     * Checker statuses.
     *
     * @param batchNumber batch to update
     * @return true if successfully updated
     */
    public boolean markBatchAsNPCISent(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return false;
        }

        String sql =
                "UPDATE public.outward_batch " +
                "SET batch_status = 'NPCI_SENT' " +
                "WHERE batch_number = ? " +
                "  AND UPPER(batch_status) <> 'NPCI_SENT'";

        try (
                Connection con =
                        dataSource.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setString(
                    1,
                    batchNumber.trim()
            );

            int updated =
                    ps.executeUpdate();

            return updated == 1;

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to mark batch as NPCI_SENT.",
                    e
            );
        }
    }

    // ============================================================
    // GET BATCH STATUS
    // ============================================================

    /**
     * Returns the current database status of a batch.
     *
     * @param batchNumber batch number
     * @return current batch status, or null when not found
     */
    public String getBatchStatus(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return null;
        }

        String sql =
                "SELECT batch_status " +
                "FROM public.outward_batch " +
                "WHERE batch_number = ?";

        try (
                Connection con =
                        dataSource.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setString(
                    1,
                    batchNumber.trim()
            );

            try (
                    ResultSet rs =
                            ps.executeQuery()
            ) {

                if (rs.next()) {

                    return rs.getString(
                            "batch_status"
                    );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to get batch status.",
                    e
            );
        }

        return null;
    }
}