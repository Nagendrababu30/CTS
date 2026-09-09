package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.dto.DashboardBatchDto;

public class DashboardDaoImpl
        implements DashboardDao {

    // -------------------------------------------------------------------------
    // Get dashboard batches
    // -------------------------------------------------------------------------

	@Override
    public List<DashboardBatchDto> getDashboardBatches() {

        String sql = """
                SELECT
                    b.batch_id,
                    b.total_cheques,
                    h.batch_status,
                    l.user_id AS lock_user_id,
                    l.lock_status
                FROM public.inward_batch b

                LEFT JOIN (
                    SELECT DISTINCT ON (batch_id)
                        batch_id,
                        batch_status
                    FROM public.inward_batch_history
                    ORDER BY
                        batch_id,
                        changed_on DESC
                ) h
                    ON b.batch_id = h.batch_id

                LEFT JOIN (
                    SELECT DISTINCT ON (bl.batch_id)
                        bl.batch_id,
                        bl.user_id,
                        bl.lock_status
                    FROM public.inward_batch_lock bl
                    INNER JOIN public."user" u
                        ON u.user_id = bl.user_id
                    INNER JOIN public."role" r
                        ON r.role_id = u.role_id
                    WHERE bl.lock_status = 'LOCKED'
                      AND u.status = 'ACTIVE'
                      AND r.role_name = 'INWARD_MAKER'
                    ORDER BY
                        bl.batch_id,
                        bl.locked_time DESC
                ) l
                    ON b.batch_id = l.batch_id

                WHERE h.batch_status IS NULL
                   OR h.batch_status NOT IN (
                        'SENT_TO_CHECKER',
                        'COMPLETED'
                   )

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
                INSERT INTO public.inward_batch_lock
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
                INSERT INTO public.inward_batch_history
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

            connection.setAutoCommit(false);

            /*
             * 1. Create batch lock.
             */
            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    lockSql)
            ) {

                statement.setLong(
                        1,
                        batchId);

                statement.setLong(
                        2,
                        userId);

                statement.executeUpdate();
            }

            /*
             * 2. Add LOCKED to batch history.
             *
             * RECEIVED is not updated.
             * LOCKED is a new history record.
             */
            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    historySql)
            ) {

                statement.setLong(
                        1,
                        batchId);

                statement.setLong(
                        2,
                        userId);

                statement.executeUpdate();
            }

            connection.commit();

            return true;

        } catch (Exception e) {

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
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (Exception closeException) {
                    closeException.printStackTrace();
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Update batch workflow status
    // -------------------------------------------------------------------------

    @Override
    public boolean updateBatchStatus(
            Long batchId,
            String batchStatus,
            Long userId) {

        if (batchId == null
                || batchStatus == null
                || batchStatus.trim().isEmpty()
                || userId == null) {

            return false;
        }

        String normalizedStatus =
                batchStatus.trim().toUpperCase();

        /*
         * Only the statuses required in the current
         * Maker flow are allowed here.
         */
        if (!"MICR_REPAIR".equals(normalizedStatus)
                && !"DATA_ENTRY".equals(normalizedStatus)) {

            return false;
        }

        String historySql = """
                INSERT INTO public.inward_batch_history
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
                    ?,
                    CURRENT_TIMESTAMP,
                    ?,
                    ?,
                    ?
                )
                """;

        try (
                Connection connection =
                        ConnectionPool
                                .getDataSource()
                                .getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(
                                historySql)) {

            statement.setLong(
                    1,
                    batchId);

            statement.setString(
                    2,
                    normalizedStatus);

            statement.setLong(
                    3,
                    userId);

            if ("MICR_REPAIR".equals(normalizedStatus)) {

                statement.setString(
                        4,
                        "MICR validation requires repair");

                statement.setString(
                        5,
                        "Batch moved to MICR Repair");

            } else {

                statement.setString(
                        4,
                        "MICR validation completed");

                statement.setString(
                        5,
                        "Batch moved to Data Entry");
            }

            int inserted =
                    statement.executeUpdate();

            return inserted == 1;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error updating batch status for Batch ID: "
                            + batchId,
                    e);
        }
    }
}