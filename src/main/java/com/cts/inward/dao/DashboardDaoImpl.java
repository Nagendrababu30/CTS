package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.dto.DashboardBatchDto;

public class DashboardDaoImpl implements DashboardDao {

    @Override
    public List<DashboardBatchDto> getDashboardBatches() {

        String sql = """
                SELECT
                    b.batch_id,
                    b.total_cheques,

                    h.batch_status,

                    l.user_id AS lock_user_id,
                    l.lock_status

                FROM inward_batch b

                LEFT JOIN (
                    SELECT DISTINCT ON (batch_id)
                        batch_id,
                        batch_status
                    FROM inward_batch_history
                    ORDER BY batch_id, changed_on DESC
                ) h
                    ON b.batch_id = h.batch_id

                LEFT JOIN inward_batch_lock l
                    ON b.batch_id = l.batch_id
                    AND l.lock_status = 'LOCKED'

                ORDER BY b.batch_id
                """;

        List<DashboardBatchDto> batches =
                new ArrayList<>();

        try (
            Connection connection =
                    ConnectionPool.getDataSource()
                            .getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql);

            ResultSet resultSet =
                    statement.executeQuery()
        ) {

            while (resultSet.next()) {

                Long lockUserId = null;

                /*
                 * Do not use:
                 *
                 * resultSet.getObject(
                 *     "lock_user_id",
                 *     Long.class);
                 *
                 * because your old c3p0 ResultSet proxy
                 * does not support that JDBC method.
                 */
                Object lockUserIdObject =
                        resultSet.getObject(
                                "lock_user_id");

                if (lockUserIdObject != null) {

                    lockUserId =
                            resultSet.getLong(
                                    "lock_user_id");
                }

                DashboardBatchDto batch =
                        new DashboardBatchDto(
                                resultSet.getLong(
                                        "batch_id"),

                                resultSet.getInt(
                                        "total_cheques"),

                                resultSet.getString(
                                        "batch_status"),

                                lockUserId,

                                resultSet.getString(
                                        "lock_status")
                        );

                batches.add(batch);
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error retrieving dashboard batches",
                    e);
        }

        return batches;
    }

    @Override
    public boolean lockBatch(
            Long batchId,
            Long userId) {

        String lockSql = """
                INSERT INTO inward_batch_lock
                (
                    batch_id,
                    user_id,
                    locked_time,
                    lock_status
                )
                VALUES
                (
                    ?,
                    ?,
                    CURRENT_TIMESTAMP,
                    'LOCKED'
                )
                """;

        String historySql = """
                INSERT INTO inward_batch_history
                (
                    batch_id,
                    batch_status,
                    changed_on,
                    changed_by,
                    reason,
                    remarks
                )
                VALUES
                (
                    ?,
                    'LOCKED',
                    CURRENT_TIMESTAMP,
                    ?,
                    'Batch locked for processing',
                    'Batch locked by inward maker'
                )
                """;

        Connection connection = null;

        try {

            connection =
                    ConnectionPool.getDataSource()
                            .getConnection();

            /*
             * Both database operations must succeed
             * together.
             */
            connection.setAutoCommit(false);

            /*
             * 1. Create the current lock.
             */
            try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                lockSql)
            ) {

                statement.setLong(1, batchId);
                statement.setLong(2, userId);

                statement.executeUpdate();
            }

            /*
             * 2. Add LOCKED to batch history.
             *
             * We do NOT modify the old RECEIVED record.
             * LOCKED is a new history event.
             */
            try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                historySql)
            ) {

                statement.setLong(1, batchId);
                statement.setLong(2, userId);

                statement.executeUpdate();
            }

            /*
             * Both operations succeeded.
             */
            connection.commit();

            return true;

        } catch (Exception e) {

            /*
             * If either operation fails,
             * undo everything.
             */
            if (connection != null) {

                try {
                    connection.rollback();
                } catch (Exception rollbackException) {
                    rollbackException.printStackTrace();
                }
            }

            throw new RuntimeException(
                    "Error locking batch: "
                            + batchId,
                    e);

        } finally {

            if (connection != null) {

                try {
                    connection.close();
                } catch (Exception closeException) {
                    closeException.printStackTrace();
                }
            }
        }
    }
}