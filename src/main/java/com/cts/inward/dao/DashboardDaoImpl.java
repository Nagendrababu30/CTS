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

                /*
                 * Get latest workflow status for each batch.
                 */
                LEFT JOIN (
                    SELECT DISTINCT ON (batch_id)
                        batch_id,
                        batch_status
                    FROM public.inward_batch_history
                    ORDER BY
                        batch_id,
                        changed_on DESC,
                        batch_history_id DESC
                ) h
                    ON b.batch_id = h.batch_id

                /*
                 * Get latest lock record for each batch.
                 *
                 * IMPORTANT:
                 * We do NOT filter lock_status here.
                 *
                 * We first find the latest lock record and then
                 * consider it active only when its latest status
                 * is LOCKED.
                 */
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

                    WHERE u.status = 'ACTIVE'
                      AND r.role_name = 'INWARD_MAKER'

                    ORDER BY
                        bl.batch_id,
                        bl.locked_time DESC,
                        bl.lock_id DESC
                ) l
                    ON b.batch_id = l.batch_id
                   AND l.lock_status = 'LOCKED'

                /*
                 * Maker dashboard must not show batches that
                 * have already gone to Checker or completed.
                 */
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
                        ConnectionPool
                                .getDataSource()
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

        String existingLockSql =
                """
                SELECT 1
                FROM public.inward_batch_lock bl
                WHERE bl.batch_id = ?
                  AND bl.lock_status = 'LOCKED'
                LIMIT 1
                """;

        String lockSql =
                """
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

        String historySql =
                """
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

            connection.setAutoCommit(false);

            /*
             * ---------------------------------------------------------
             * 1. Check whether batch already has an active lock.
             * ---------------------------------------------------------
             */
            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    existingLockSql)
            ) {

                statement.setLong(
                        1,
                        batchId);

                try (
                        ResultSet resultSet =
                                statement.executeQuery()
                ) {

                    if (resultSet.next()) {

                        connection.rollback();

                        return false;
                    }
                }
            }

            /*
             * ---------------------------------------------------------
             * 2. Create Maker lock.
             * ---------------------------------------------------------
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

                int inserted =
                        statement.executeUpdate();

                if (inserted != 1) {

                    connection.rollback();

                    return false;
                }
            }

            /*
             * ---------------------------------------------------------
             * 3. Add LOCKED batch history.
             * ---------------------------------------------------------
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

                int inserted =
                        statement.executeUpdate();

                if (inserted != 1) {

                    connection.rollback();

                    return false;
                }
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
         * Current Maker workflow statuses.
         *
         * LOCKED:
         *     created by lockBatch(), not normally through this method.
         *
         * MICR_REPAIR:
         *     batch requires MICR repair.
         *
         * DATA_ENTRY:
         *     MICR stage completed; Maker continues with Data Entry.
         *
         * SENT_TO_CHECKER:
         *     Maker has completed processing.
         *     At this point the Maker lock is released.
         */
        if (!"MICR_REPAIR".equals(normalizedStatus)
                && !"DATA_ENTRY".equals(normalizedStatus)
                && !"SENT_TO_CHECKER".equals(normalizedStatus)) {

            return false;
        }


        Connection connection = null;

        try {

            connection =
                    ConnectionPool
                            .getDataSource()
                            .getConnection();

            connection.setAutoCommit(false);


            /*
             * ---------------------------------------------------------
             * 1. Add the new batch workflow status.
             * ---------------------------------------------------------
             */
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
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    historySql)
            ) {

                statement.setLong(
                        1,
                        batchId);

                statement.setString(
                        2,
                        normalizedStatus);

                statement.setLong(
                        3,
                        userId);


                if ("MICR_REPAIR".equals(
                        normalizedStatus)) {

                    statement.setString(
                            4,
                            "MICR validation requires repair");

                    statement.setString(
                            5,
                            "Batch moved to MICR Repair");


                } else if ("DATA_ENTRY".equals(
                        normalizedStatus)) {

                    statement.setString(
                            4,
                            "MICR validation completed");

                    statement.setString(
                            5,
                            "Batch moved to Data Entry");


                } else {

                    /*
                     * SENT_TO_CHECKER
                     */
                    statement.setString(
                            4,
                            "Maker processing completed");

                    statement.setString(
                            5,
                            "Batch sent to Checker");
                }


                int inserted =
                        statement.executeUpdate();

                if (inserted != 1) {

                    connection.rollback();
                    return false;
                }
            }


            /*
             * ---------------------------------------------------------
             * 2. Release Maker lock ONLY when batch is sent to Checker.
             * ---------------------------------------------------------
             *
             * DATA_ENTRY does NOT release the lock.
             *
             * The lock table keeps lock history, so we insert a new
             * UNLOCKED record instead of deleting the old LOCKED row.
             */
            if ("SENT_TO_CHECKER".equals(
                    normalizedStatus)) {

                String unlockSql = """
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
                            'UNLOCKED'
                        )
                        """;

                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        unlockSql)
                ) {

                    statement.setLong(
                            1,
                            batchId);

                    statement.setLong(
                            2,
                            userId);

                    int inserted =
                            statement.executeUpdate();

                    if (inserted != 1) {

                        connection.rollback();
                        return false;
                    }
                }
            }


            /*
             * ---------------------------------------------------------
             * 3. Everything succeeded.
             * ---------------------------------------------------------
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
                    "Error updating batch status for Batch ID: "
                            + batchId
                            + ", status: "
                            + normalizedStatus,
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
}