package com.iispl.cts.dao.outward.checker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.iispl.cts.data.CTSStaticData;
import com.iispl.cts.model.outward.OutwardBatch;

public class CheckerDashboardDAO {

    /*
     * ============================================================
     * GET CHECKER BATCHES
     * ============================================================
     *
     * Shows batches which are ready for Checker.
     *
     * FOR NOW:
     * CAPTURED batches are also accessible by Checker.
     *
     * If no Checker assignment exists:
     *      AVAILABLE
     *
     * If Checker assignment exists:
     *      LOCKED / IN_PROGRESS
     *
     */

    public List<OutwardBatch> getCheckerBatches() {

        List<OutwardBatch> batches = new ArrayList<>();

        String sql =
                "SELECT " +
                "    ob.batch_number, " +
                "    ob.branch_code, " +
                "    ob.cheque_count, " +
                "    ob.batch_folder_path, " +
                "    ob.created_by, " +
                "    ob.created_at, " +
                "    ob.batch_status, " +
                "    cba.user_id AS checker_user_id, " +
                "    cba.assigned_at AS checker_assigned_at, " +
                "    cba.started_at AS checker_started_at, " +
                "    cba.completed_at AS checker_completed_at, " +
                "    cba.assignment_status AS checker_assignment_status " +
                "FROM public.outward_batch ob " +
                "LEFT JOIN public.outward_batch_assignment cba " +
                "    ON ob.batch_number = cba.batch_number " +
                "    AND UPPER(cba.assignment_role) = 'CHECKER' " +
                "    AND UPPER(cba.assignment_status) IN " +
                "        ('ASSIGNED', 'IN_PROGRESS') " +
                "WHERE UPPER(ob.batch_status) IN " +
                "    ('SUBMITTED_TO_CHECKER', " +
                "     'READY_FOR_CHECKER', " +
                "     'SUBMITTED', " +
                "     'CHECKER_PENDING', " +
                "     'PENDING_CHECKER') " +
                "ORDER BY ob.created_at DESC";

        try (Connection con = CTSStaticData.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                OutwardBatch batch = new OutwardBatch();

                /*
                 * ------------------------------------------------
                 * BATCH INFORMATION
                 * ------------------------------------------------
                 */

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

                /*
                 * created_by is INTEGER in database
                 * but String in OutwardBatch model.
                 */

                int createdBy =
                        rs.getInt("created_by");

                if (!rs.wasNull()) {

                    batch.setCreatedBy(
                            String.valueOf(createdBy)
                    );
                }

                /*
                 * Created date
                 */

                if (rs.getTimestamp("created_at") != null) {

                    batch.setCreatedAt(
                            rs.getTimestamp("created_at")
                                    .toLocalDateTime()
                    );
                }

                /*
                 * Batch status
                 */

                batch.setBatchStatus(
                        rs.getString("batch_status")
                );

                /*
                 * ------------------------------------------------
                 * CHECKER ASSIGNMENT
                 * ------------------------------------------------
                 */

                int checkerUserId =
                        rs.getInt("checker_user_id");

                if (!rs.wasNull()) {

                    /*
                     * Checker owns this batch.
                     */

                    batch.setCheckerUserNumber(
                            String.valueOf(checkerUserId)
                    );

                    batch.setLockedBy(
                            String.valueOf(checkerUserId)
                    );

                    /*
                     * Assigned time
                     */

                    if (rs.getTimestamp(
                            "checker_assigned_at") != null) {

                        batch.setLockedAt(
                                rs.getTimestamp(
                                        "checker_assigned_at")
                                        .toLocalDateTime()
                        );
                    }

                    /*
                     * Checker started time
                     */

                    if (rs.getTimestamp(
                            "checker_started_at") != null) {

                        batch.setCheckerStartedAt(
                                rs.getTimestamp(
                                        "checker_started_at")
                                        .toLocalDateTime()
                        );
                    }

                    /*
                     * Checker completed time
                     */

                    if (rs.getTimestamp(
                            "checker_completed_at") != null) {

                        batch.setCheckerCompletedAt(
                                rs.getTimestamp(
                                        "checker_completed_at")
                                        .toLocalDateTime()
                        );
                    }

                    /*
                     * Assignment status
                     */

                    String assignmentStatus =
                            rs.getString(
                                    "checker_assignment_status"
                            );

                    if ("IN_PROGRESS".equalsIgnoreCase(
                            assignmentStatus)) {

                        batch.setLockStatus(
                                "IN_PROGRESS"
                        );

                    } else if ("ASSIGNED".equalsIgnoreCase(
                            assignmentStatus)) {

                        batch.setLockStatus(
                                "LOCKED"
                        );

                    } else {

                        batch.setLockStatus(
                                assignmentStatus
                        );
                    }

                } else {

                    /*
                     * ------------------------------------------------
                     * NO CHECKER ASSIGNMENT
                     * ------------------------------------------------
                     *
                     * Therefore batch is available.
                     */

                    batch.setCheckerUserNumber(null);

                    batch.setLockedBy(null);

                    batch.setLockedAt(null);

                    batch.setCheckerStartedAt(null);

                    batch.setCheckerCompletedAt(null);

                    batch.setLockStatus(
                            "AVAILABLE"
                    );
                }

                /*
                 * Add batch to result
                 */

                batches.add(batch);
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to load checker batches from database.",
                    e
            );
        }

        return batches;
    }

    /*
     * ============================================================
     * FIND ONE CHECKER BATCH
     * ============================================================
     */

    public OutwardBatch findBatch(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return null;
        }

        String sql =
                "SELECT " +
                "    ob.batch_number, " +
                "    ob.branch_code, " +
                "    ob.cheque_count, " +
                "    ob.batch_folder_path, " +
                "    ob.created_by, " +
                "    ob.created_at, " +
                "    ob.batch_status, " +
                "    cba.user_id AS checker_user_id, " +
                "    cba.assigned_at AS checker_assigned_at, " +
                "    cba.started_at AS checker_started_at, " +
                "    cba.completed_at AS checker_completed_at, " +
                "    cba.assignment_status AS checker_assignment_status " +
                "FROM public.outward_batch ob " +
                "LEFT JOIN public.outward_batch_assignment cba " +
                "    ON ob.batch_number = cba.batch_number " +
                "    AND UPPER(cba.assignment_role) = 'CHECKER' " +
                "    AND UPPER(cba.assignment_status) IN " +
                "        ('ASSIGNED', 'IN_PROGRESS') " +
                "WHERE ob.batch_number = ?";

        try (Connection con = CTSStaticData.getConnection();
             PreparedStatement ps =
                     con.prepareStatement(sql)) {

            ps.setString(
                    1,
                    batchNumber
            );

            try (ResultSet rs =
                         ps.executeQuery()) {

                if (!rs.next()) {

                    return null;
                }

                OutwardBatch batch =
                        new OutwardBatch();

                /*
                 * Batch information
                 */

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

                int createdBy =
                        rs.getInt("created_by");

                if (!rs.wasNull()) {

                    batch.setCreatedBy(
                            String.valueOf(createdBy)
                    );
                }

                if (rs.getTimestamp(
                        "created_at") != null) {

                    batch.setCreatedAt(
                            rs.getTimestamp(
                                    "created_at")
                                    .toLocalDateTime()
                    );
                }

                batch.setBatchStatus(
                        rs.getString("batch_status")
                );

                /*
                 * Checker assignment
                 */

                int checkerUserId =
                        rs.getInt("checker_user_id");

                if (!rs.wasNull()) {

                    batch.setCheckerUserNumber(
                            String.valueOf(checkerUserId)
                    );

                    batch.setLockedBy(
                            String.valueOf(checkerUserId)
                    );

                    /*
                     * Assigned time
                     */

                    if (rs.getTimestamp(
                            "checker_assigned_at") != null) {

                        batch.setLockedAt(
                                rs.getTimestamp(
                                        "checker_assigned_at")
                                        .toLocalDateTime()
                        );
                    }

                    /*
                     * Started time
                     */

                    if (rs.getTimestamp(
                            "checker_started_at") != null) {

                        batch.setCheckerStartedAt(
                                rs.getTimestamp(
                                        "checker_started_at")
                                        .toLocalDateTime()
                        );
                    }

                    /*
                     * Completed time
                     */

                    if (rs.getTimestamp(
                            "checker_completed_at") != null) {

                        batch.setCheckerCompletedAt(
                                rs.getTimestamp(
                                        "checker_completed_at")
                                        .toLocalDateTime()
                        );
                    }

                    /*
                     * Assignment status
                     */

                    String status =
                            rs.getString(
                                    "checker_assignment_status"
                            );

                    if ("IN_PROGRESS".equalsIgnoreCase(
                            status)) {

                        batch.setLockStatus(
                                "IN_PROGRESS"
                        );

                    } else {

                        batch.setLockStatus(
                                "LOCKED"
                        );
                    }

                } else {

                    /*
                     * No active Checker assignment.
                     */

                    batch.setLockStatus(
                            "AVAILABLE"
                    );
                }

                return batch;
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to find checker batch.",
                    e
            );
        }
    }

    /*
     * ============================================================
     * ASSIGN / LOCK CHECKER BATCH
     * ============================================================
     */

    public boolean assignBatch(
            String batchNumber,
            String checkerUserId) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return false;
        }

        if (checkerUserId == null ||
                checkerUserId.trim().isEmpty()) {

            return false;
        }

        String sql =
                "INSERT INTO public.outward_batch_assignment " +
                "    (batch_number, user_id, assignment_role, " +
                "     assigned_at, started_at, assignment_status) " +
                "SELECT " +
                "    ?, " +
                "    ?, " +
                "    'CHECKER', " +
                "    CURRENT_TIMESTAMP, " +
                "    CURRENT_TIMESTAMP, " +
                "    'IN_PROGRESS' " +
                "WHERE NOT EXISTS ( " +
                "    SELECT 1 " +
                "    FROM public.outward_batch_assignment " +
                "    WHERE batch_number = ? " +
                "      AND UPPER(assignment_role) = 'CHECKER' " +
                "      AND UPPER(assignment_status) " +
                "          IN ('ASSIGNED', 'IN_PROGRESS') " +
                ")";

        try (Connection con = CTSStaticData.getConnection();
             PreparedStatement ps =
                     con.prepareStatement(sql)) {

            ps.setString(
                    1,
                    batchNumber
            );

            ps.setInt(
                    2,
                    Integer.parseInt(checkerUserId)
            );

            ps.setString(
                    3,
                    batchNumber
            );

            int inserted =
                    ps.executeUpdate();

            /*
             * 1 = successfully locked
             * 0 = already locked
             */

            return inserted == 1;

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to assign checker batch.",
                    e
            );
        }
    }
}