package com.iispl.cts.dao.outward.checker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.iispl.cts.data.CTSStaticData;

public class CheckerAssignmentDAO {

    /*
     * Take a batch for Checker processing.
     *
     * A new CHECKER assignment is created.
     * Existing MAKER assignment records are not changed.
     */
    public boolean takeBatch(
            String batchNumber,
            long checkerUserId) {

        /*
         * Lock the batch row first.
         *
         * This prevents two Checkers from taking
         * the same batch at the same time.
         */
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

        try (Connection connection =
                     CTSStaticData.getConnection()) {

            connection.setAutoCommit(false);

            try {

                /*
                 * Lock the batch row and check its status.
                 */
                try (PreparedStatement lockStatement =
                             connection.prepareStatement(lockBatchSql)) {

                    lockStatement.setString(1, batchNumber);

                    try (ResultSet rs =
                                 lockStatement.executeQuery()) {

                        /*
                         * Batch does not exist.
                         */
                        if (!rs.next()) {
                            connection.rollback();
                            return false;
                        }

                        /*
                         * Only batches submitted to Checker
                         * can be taken.
                         */
                        String batchStatus =
                                rs.getString("batch_status");

                        if (!"SUBMITTED_TO_CHECKER"
                                .equalsIgnoreCase(batchStatus)) {

                            connection.rollback();
                            return false;
                        }
                    }
                }

                /*
                 * Check whether another Checker already
                 * has an active assignment.
                 */
                try (PreparedStatement checkStatement =
                             connection.prepareStatement(checkSql)) {

                    checkStatement.setString(1, batchNumber);

                    try (ResultSet rs =
                                 checkStatement.executeQuery()) {

                        if (rs.next()) {
                            connection.rollback();
                            return false;
                        }
                    }
                }

                /*
                 * Create a NEW Checker assignment.
                 *
                 * Existing Maker assignment remains untouched.
                 */
                try (PreparedStatement insertStatement =
                             connection.prepareStatement(insertSql)) {

                    insertStatement.setString(1, batchNumber);
                    insertStatement.setLong(2, checkerUserId);

                    int rows =
                            insertStatement.executeUpdate();

                    if (rows == 1) {
                        connection.commit();
                        return true;
                    }

                    connection.rollback();
                    return false;
                }

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
                            + batchNumber, e);
        }
    }

    /*
     * Check whether a batch is currently assigned
     * to any Checker.
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

            statement.setString(1, batchNumber);

            try (ResultSet rs =
                         statement.executeQuery()) {

                return rs.next();
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while checking batch assignment: "
                            + batchNumber, e);
        }
    }

    /*
     * Check whether this particular Checker currently
     * has the batch.
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

            statement.setString(1, batchNumber);
            statement.setLong(2, checkerUserId);

            try (ResultSet rs =
                         statement.executeQuery()) {

                return rs.next();
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while checking Checker assignment: "
                            + batchNumber, e);
        }
    }

    /*
     * Get the user ID of the Checker currently holding
     * the batch.
     *
     * Returns null when no Checker has the batch.
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

            statement.setString(1, batchNumber);

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {
                    return rs.getLong("user_id");
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while getting assigned Checker: "
                            + batchNumber, e);
        }

        return null;
    }

    /*
     * Mark the current Checker assignment as completed.
     *
     * The assignment row is NOT deleted.
     * It remains in the table for history and tracking.
     */
    public boolean completeBatch(
            String batchNumber,
            long checkerUserId) {

        String sql =
                "UPDATE outward_batch_assignment "
                + "SET assignment_status = 'COMPLETED', "
                + "    completed_at = CURRENT_TIMESTAMP "
                + "WHERE batch_number = ? "
                + "AND user_id = ? "
                + "AND UPPER(assignment_role) = 'CHECKER' "
                + "AND UPPER(assignment_status) "
                + "    IN ('ASSIGNED', 'IN_PROGRESS')";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, batchNumber);
            statement.setLong(2, checkerUserId);

            return statement.executeUpdate() == 1;

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while completing Checker assignment: "
                            + batchNumber, e);
        }
    }

    /*
     * Get the assignment ID of the current Checker
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

            statement.setString(1, batchNumber);
            statement.setLong(2, checkerUserId);

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {
                    return rs.getLong("id");
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while getting assignment ID: "
                            + batchNumber, e);
        }

        return null;
    }
}