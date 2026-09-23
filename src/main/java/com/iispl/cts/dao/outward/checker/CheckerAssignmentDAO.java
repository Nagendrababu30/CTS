package com.iispl.cts.dao.outward.checker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.iispl.cts.data.CTSStaticData;

public class CheckerAssignmentDAO {

    // ============================================================
    // TAKE BATCH FOR CHECKER
    // ============================================================

    /*
     * Take a batch for Checker processing.
     *
     * A new CHECKER assignment is created.
     *
     * Existing MAKER assignment records are not changed.
     *
     * Batch status:
     *
     * SUBMITTED_TO_CHECKER
     *          ↓
     * CHECKER_PROCESSING
     *
     * This method is only for a newly submitted batch.
     *
     * ON_HOLD batches are NOT taken through this method.
     * The same Checker continues ownership for re-verification.
     */

    public boolean takeBatch(
            String batchNumber,
            long checkerUserId) {

        String lockBatchSql =
                "SELECT batch_number, batch_status "
                + "FROM outward_batch "
                + "WHERE batch_number = ? "
                + "FOR UPDATE";

        String checkSql =
                "SELECT id "
                + "FROM outward_batch_assignment "
                + "WHERE batch_number = ? "
                + "AND UPPER(assignment_role) = 'CHECKER' "
                + "AND UPPER(assignment_status) "
                + "    IN ('ASSIGNED', 'IN_PROGRESS') "
                + "LIMIT 1";

        String insertSql =
                "INSERT INTO outward_batch_assignment "
                + "(batch_number, user_id, assignment_role, assigned_at, "
                + " started_at, assignment_status) "
                + "VALUES (?, ?, 'CHECKER', CURRENT_TIMESTAMP, "
                + " CURRENT_TIMESTAMP, 'IN_PROGRESS')";

        String updateBatchProcessingSql =
                "UPDATE outward_batch "
                + "SET batch_status = 'CHECKER_PROCESSING' "
                + "WHERE batch_number = ? "
                + "AND UPPER(TRIM(batch_status)) = "
                + "'SUBMITTED_TO_CHECKER'";

        try (Connection connection =
                     CTSStaticData.getConnection()) {

            connection.setAutoCommit(false);

            try {

                // ====================================================
                // LOCK BATCH
                // ====================================================

                try (PreparedStatement lockStatement =
                             connection.prepareStatement(
                                     lockBatchSql)) {

                    lockStatement.setString(
                            1,
                            batchNumber);

                    try (ResultSet rs =
                                 lockStatement.executeQuery()) {

                        if (!rs.next()) {

                            connection.rollback();

                            return false;
                        }

                        String batchStatus =
                                rs.getString(
                                        "batch_status");

                        if (!"SUBMITTED_TO_CHECKER"
                                .equalsIgnoreCase(
                                        batchStatus)) {

                            connection.rollback();

                            return false;
                        }
                    }
                }

                // ====================================================
                // CHECK EXISTING ACTIVE CHECKER ASSIGNMENT
                // ====================================================

                try (PreparedStatement checkStatement =
                             connection.prepareStatement(
                                     checkSql)) {

                    checkStatement.setString(
                            1,
                            batchNumber);

                    try (ResultSet rs =
                                 checkStatement.executeQuery()) {

                        if (rs.next()) {

                            connection.rollback();

                            return false;
                        }
                    }
                }

                // ====================================================
                // CREATE CHECKER ASSIGNMENT
                // ====================================================

                try (PreparedStatement insertStatement =
                             connection.prepareStatement(
                                     insertSql)) {

                    insertStatement.setString(
                            1,
                            batchNumber);

                    insertStatement.setLong(
                            2,
                            checkerUserId);

                    int rows =
                            insertStatement.executeUpdate();

                    if (rows != 1) {

                        connection.rollback();

                        return false;
                    }
                }

                // ====================================================
                // MOVE BATCH TO CHECKER PROCESSING
                // ====================================================

                try (PreparedStatement updateStatement =
                             connection.prepareStatement(
                                     updateBatchProcessingSql)) {

                    updateStatement.setString(
                            1,
                            batchNumber);

                    int updatedRows =
                            updateStatement.executeUpdate();

                    if (updatedRows != 1) {

                        throw new RuntimeException(
                                "Unable to move batch to Checker processing");
                    }
                }

                // ====================================================
                // COMMIT
                // ====================================================

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

            throw new RuntimeException(
                    "Error while taking Checker batch: "
                            + batchNumber,
                    e);
        }
    }

    // ============================================================
    // CHECK WHETHER BATCH IS CURRENTLY ASSIGNED
    // ============================================================

    /*
     * Returns true when the batch has an active Checker assignment.
     *
     * ASSIGNED / IN_PROGRESS are considered active.
     */

    public boolean isBatchAssigned(
            String batchNumber) {

        String sql =
                "SELECT 1 "
                + "FROM outward_batch_assignment "
                + "WHERE batch_number = ? "
                + "AND UPPER(assignment_role) = 'CHECKER' "
                + "AND UPPER(assignment_status) "
                + "    IN ('ASSIGNED', 'IN_PROGRESS') "
                + "LIMIT 1";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber);

            try (ResultSet rs =
                         statement.executeQuery()) {

                return rs.next();
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while checking batch assignment: "
                            + batchNumber,
                    e);
        }
    }

    // ============================================================
    // CHECK WHETHER THIS CHECKER HAS THE BATCH
    // ============================================================

    /*
     * Returns true when this particular Checker currently owns
     * the batch.
     *
     * This remains true while the batch is ON_HOLD because the
     * Checker assignment intentionally remains IN_PROGRESS.
     *
     * This allows the same Checker to perform re-verification
     * after Maker correction.
     */

    public boolean isBatchAssignedToChecker(
            String batchNumber,
            long checkerUserId) {

        String sql =
                "SELECT 1 "
                + "FROM outward_batch_assignment "
                + "WHERE batch_number = ? "
                + "AND user_id = ? "
                + "AND UPPER(assignment_role) = 'CHECKER' "
                + "AND UPPER(assignment_status) "
                + "    IN ('ASSIGNED', 'IN_PROGRESS') "
                + "LIMIT 1";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber);

            statement.setLong(
                    2,
                    checkerUserId);

            try (ResultSet rs =
                         statement.executeQuery()) {

                return rs.next();
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while checking Checker assignment: "
                            + batchNumber,
                    e);
        }
    }

    // ============================================================
    // GET ASSIGNED CHECKER
    // ============================================================

    /*
     * Get the Checker currently holding the batch.
     *
     * Returns null when no active Checker assignment exists.
     */

    public Long getAssignedChecker(
            String batchNumber) {

        String sql =
                "SELECT user_id "
                + "FROM outward_batch_assignment "
                + "WHERE batch_number = ? "
                + "AND UPPER(assignment_role) = 'CHECKER' "
                + "AND UPPER(assignment_status) "
                + "    IN ('ASSIGNED', 'IN_PROGRESS') "
                + "ORDER BY assigned_at DESC "
                + "LIMIT 1";

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

                    return rs.getLong(
                            "user_id");
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while getting assigned Checker: "
                            + batchNumber,
                    e);
        }

        return null;
    }

    // ============================================================
    // COMPLETE CHECKER ASSIGNMENT
    // ============================================================

    /*
     * Complete the Checker assignment only when the batch has
     * reached its final Checker state.
     *
     * FINAL:
     *
     * CHECKER_VERIFIED
     *       ↓
     * assignment COMPLETED
     *
     * ON_HOLD:
     *
     * ON_HOLD
     *       ↓
     * assignment remains IN_PROGRESS
     *
     * This is important because the same Checker must continue
     * with re-verification after Maker correction.
     *
     * Therefore an ON_HOLD batch is treated as successfully
     * left active, not as a completed assignment.
     */

    public boolean completeBatch(
            String batchNumber,
            long checkerUserId) {

        String batchStatusSql =
                "SELECT batch_status "
                + "FROM outward_batch "
                + "WHERE batch_number = ?";

        String completeSql =
                "UPDATE outward_batch_assignment "
                + "SET assignment_status = 'COMPLETED', "
                + "    completed_at = CURRENT_TIMESTAMP "
                + "WHERE batch_number = ? "
                + "AND user_id = ? "
                + "AND UPPER(assignment_role) = 'CHECKER' "
                + "AND UPPER(assignment_status) "
                + "    IN ('ASSIGNED', 'IN_PROGRESS')";

        try (Connection connection =
                     CTSStaticData.getConnection()) {

            connection.setAutoCommit(false);

            try {

                // ====================================================
                // CHECK CURRENT BATCH STATUS
                // ====================================================

                String batchStatus = null;

                try (PreparedStatement statusStatement =
                             connection.prepareStatement(
                                     batchStatusSql)) {

                    statusStatement.setString(
                            1,
                            batchNumber);

                    try (ResultSet rs =
                                 statusStatement.executeQuery()) {

                        if (!rs.next()) {

                            connection.rollback();

                            return false;
                        }

                        batchStatus =
                                rs.getString(
                                        "batch_status");
                    }
                }

                // ====================================================
                // ON HOLD
                // ====================================================

                /*
                 * Do NOT complete the Checker assignment.
                 *
                 * The same Checker must continue ownership
                 * for future re-verification.
                 */

                if ("ON_HOLD".equalsIgnoreCase(
                        batchStatus)) {

                    connection.commit();

                    return true;
                }

                // ====================================================
                // CHECKER VERIFIED
                // ====================================================

                if (!"CHECKER_VERIFIED".equalsIgnoreCase(
                        batchStatus)) {

                    connection.rollback();

                    return false;
                }

                // ====================================================
                // COMPLETE ASSIGNMENT
                // ====================================================

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     completeSql)) {

                    statement.setString(
                            1,
                            batchNumber);

                    statement.setLong(
                            2,
                            checkerUserId);

                    int updatedRows =
                            statement.executeUpdate();

                    if (updatedRows != 1) {

                        connection.rollback();

                        return false;
                    }
                }

                // ====================================================
                // COMMIT
                // ====================================================

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

            throw new RuntimeException(
                    "Error while completing Checker assignment: "
                            + batchNumber,
                    e);
        }
    }

    // ============================================================
    // GET CHECKER ASSIGNMENT ID
    // ============================================================

    /*
     * Get the assignment ID of the current active Checker
     * assignment.
     *
     * Database column is "id".
     */

    public Long getAssignmentId(
            String batchNumber,
            long checkerUserId) {

        String sql =
                "SELECT id "
                + "FROM outward_batch_assignment "
                + "WHERE batch_number = ? "
                + "AND user_id = ? "
                + "AND UPPER(assignment_role) = 'CHECKER' "
                + "AND UPPER(assignment_status) "
                + "    IN ('ASSIGNED', 'IN_PROGRESS') "
                + "ORDER BY assigned_at DESC "
                + "LIMIT 1";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber);

            statement.setLong(
                    2,
                    checkerUserId);

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {

                    return rs.getLong(
                            "id");
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while getting assignment ID: "
                            + batchNumber,
                    e);
        }

        return null;
    }
    public boolean releaseBatchLock(
            String batchNumber,
            long checkerUserId) {

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            return false;
        }

        if (checkerUserId <= 0) {

            return false;
        }

        String cleanBatchNumber =
                batchNumber.trim();

        try (Connection connection =
                     CTSStaticData.getConnection()) {

            connection.setAutoCommit(false);

            try {

                // =====================================================
                // 1. LOCK THE BATCH
                // =====================================================

                String lockBatchSql =
                        "SELECT batch_number, batch_status "
                        + "FROM public.outward_batch "
                        + "WHERE batch_number = ? "
                        + "FOR UPDATE";

                String batchStatus = null;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     lockBatchSql)) {

                    statement.setString(
                            1,
                            cleanBatchNumber);

                    try (ResultSet rs =
                                 statement.executeQuery()) {

                        if (!rs.next()) {

                            connection.rollback();
                            return false;
                        }

                        batchStatus =
                                rs.getString(
                                        "batch_status");
                    }
                }

                // =====================================================
                // 2. DO NOT RELEASE ON-HOLD BATCH
                // =====================================================

                if ("ON_HOLD".equalsIgnoreCase(
                        batchStatus)) {

                    connection.rollback();
                    return false;
                }

                // =====================================================
                // 3. VERIFY CURRENT CHECKER OWNS THE BATCH
                // =====================================================

                String ownershipSql =
                        "SELECT id "
                        + "FROM public.outward_batch_assignment "
                        + "WHERE batch_number = ? "
                        + "AND user_id = ? "
                        + "AND UPPER(TRIM(assignment_role)) = 'CHECKER' "
                        + "AND UPPER(TRIM(assignment_status)) "
                        + "IN ('ASSIGNED', 'IN_PROGRESS') "
                        + "ORDER BY assigned_at DESC "
                        + "LIMIT 1 "
                        + "FOR UPDATE";

                Long assignmentId = null;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     ownershipSql)) {

                    statement.setString(
                            1,
                            cleanBatchNumber);

                    statement.setLong(
                            2,
                            checkerUserId);

                    try (ResultSet rs =
                                 statement.executeQuery()) {

                        if (rs.next()) {

                            assignmentId =
                                    rs.getLong("id");
                        }
                    }
                }

                // Current Checker does not own the batch
                if (assignmentId == null) {

                    connection.rollback();
                    return false;
                }

                // =====================================================
                // 4. RESET ALL CHEQUE STATUSES
                //
                // CHECKER_ACCEPTED
                // CHECKER_REJECTED
                // SENT_BACK_TO_MAKER
                // etc.
                //
                //                ↓
                //
                //             VERIFIED
                // =====================================================

                String resetChequeSql =
                        "UPDATE public.outward_cheque "
                        + "SET cheque_status = 'VERIFIED' "
                        + "WHERE batch_number = ?";

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     resetChequeSql)) {

                    statement.setString(
                            1,
                            cleanBatchNumber);

                    statement.executeUpdate();
                }

                // =====================================================
                // 5. RESET CHEQUE PROCESSING
                //
                // Remove previous Checker decisions so the batch
                // starts completely fresh.
                // =====================================================

                String deleteProcessingSql =
                        "DELETE FROM public.cheque_processing "
                        + "WHERE batch_number = ?";

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     deleteProcessingSql)) {

                    statement.setString(
                            1,
                            cleanBatchNumber);

                    statement.executeUpdate();
                }

                // =====================================================
                // 6. RESET BATCH STATUS
                //
                // Checker takes:
                //
                // SUBMITTED_TO_CHECKER
                //          ↓
                // CHECKER_PROCESSING
                //
                // Release:
                //
                // CHECKER_PROCESSING
                //          ↓
                // SUBMITTED_TO_CHECKER
                // =====================================================

                String resetBatchSql =
                        "UPDATE public.outward_batch "
                        + "SET batch_status = 'SUBMITTED_TO_CHECKER' "
                        + "WHERE batch_number = ? "
                        + "AND UPPER(TRIM(batch_status)) "
                        + "= 'CHECKER_PROCESSING'";

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     resetBatchSql)) {

                    statement.setString(
                            1,
                            cleanBatchNumber);

                    int updatedRows =
                            statement.executeUpdate();

                    if (updatedRows != 1) {

                        connection.rollback();
                        return false;
                    }
                }

                // =====================================================
                // 7. RELEASE CHECKER ASSIGNMENT
                // =====================================================

                String releaseAssignmentSql =
                        "UPDATE public.outward_batch_assignment "
                        + "SET assignment_status = 'RELEASED', "
                        + "completed_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? "
                        + "AND user_id = ? "
                        + "AND UPPER(TRIM(assignment_role)) = 'CHECKER' "
                        + "AND UPPER(TRIM(assignment_status)) "
                        + "IN ('ASSIGNED', 'IN_PROGRESS')";

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     releaseAssignmentSql)) {

                    statement.setLong(
                            1,
                            assignmentId);

                    statement.setLong(
                            2,
                            checkerUserId);

                    int updatedRows =
                            statement.executeUpdate();

                    if (updatedRows != 1) {

                        connection.rollback();
                        return false;
                    }
                }

                // =====================================================
                // 8. COMMIT EVERYTHING
                // =====================================================

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

            throw new RuntimeException(
                    "Error while releasing Checker batch: "
                            + cleanBatchNumber,
                    e);
        }
    
    }
}