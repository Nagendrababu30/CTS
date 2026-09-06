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

                ORDER BY
                    b.batch_id
                """;


        List<DashboardBatchDto> batches =
                new ArrayList<>();


        try (
                Connection connection =
                        ConnectionPool
                                .getDataSource()
                                .getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()) {


            while (resultSet.next()) {

                Long lockUserId = null;


                /*
                 * Do not use:
                 *
                 * resultSet.getObject(
                 *     "lock_user_id",
                 *     Long.class);
                 *
                 * because of the old c3p0 JDBC proxy.
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


    // -------------------------------------------------------------------------
    // Lock batch
    // -------------------------------------------------------------------------

    @Override
    public boolean lockBatch(
            Long batchId,
            Long userId) {

        if (batchId == null
                || userId == null) {

            return false;
        }


        String lockSql = """
                INSERT INTO public.inward_batch_lock
                (
                    batch_id,
                    user_id,
                    locked_time,
                    lock_status
                )
                SELECT
                    ?,
                    ?,
                    CURRENT_TIMESTAMP,
                    'LOCKED'
                WHERE EXISTS (
                    SELECT 1
                    FROM public."user" u
                    INNER JOIN public."role" r
                        ON r.role_id = u.role_id
                    WHERE u.user_id = ?
                      AND u.status = 'ACTIVE'
                      AND r.role_name = 'INWARD_MAKER'
                )
                AND NOT EXISTS (
                    SELECT 1
                    FROM public.inward_batch_lock bl
                    WHERE bl.batch_id = ?
                      AND bl.lock_status = 'LOCKED'
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
                    ConnectionPool
                            .getDataSource()
                            .getConnection();


            /*
             * Lock and history must be committed
             * together.
             */
            connection.setAutoCommit(false);


            /*
             * -------------------------------------------------------------
             * 1. Create the batch lock.
             *
             * This succeeds only when:
             *
             * - user exists
             * - user is ACTIVE
             * - user role is INWARD_MAKER
             * - batch has no current LOCKED record
             * -------------------------------------------------------------
             */

            int lockInserted;


            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    lockSql)) {


                statement.setLong(
                        1,
                        batchId);

                statement.setLong(
                        2,
                        userId);

                /*
                 * Verify user.
                 */
                statement.setLong(
                        3,
                        userId);

                /*
                 * Check existing lock.
                 */
                statement.setLong(
                        4,
                        batchId);


                lockInserted =
                        statement.executeUpdate();
            }


            /*
             * No lock was created.
             */
            if (lockInserted == 0) {

                connection.rollback();

                return false;
            }


            /*
             * -------------------------------------------------------------
             * 2. Add LOCKED history record.
             * -------------------------------------------------------------
             */

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    historySql)) {


                statement.setLong(
                        1,
                        batchId);

                statement.setLong(
                        2,
                        userId);


                statement.executeUpdate();
            }


            /*
             * Both operations succeeded.
             */
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

                    connection.close();

                } catch (Exception closeException) {

                    closeException.printStackTrace();
                }
            }
        }
    }
}