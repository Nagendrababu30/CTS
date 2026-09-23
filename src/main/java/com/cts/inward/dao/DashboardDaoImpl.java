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
	                u.username AS lock_user_name,
	                l.lock_status

	            FROM public.inward_batch b

	            /*
	             * Get latest batch status.
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
	             */
	            LEFT JOIN (
	                SELECT DISTINCT ON (batch_id)
	                    batch_id,
	                    user_id,
	                    lock_status,
	                    locked_time,
	                    lock_id
	                FROM public.inward_batch_lock
	                ORDER BY
	                    batch_id,
	                    locked_time DESC,
	                    lock_id DESC
	            ) l
	                ON b.batch_id = l.batch_id

	            /*
	             * Get username of the user who owns
	             * the latest lock.
	             */
	            LEFT JOIN public."user" u
	                ON u.user_id = l.user_id

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

	            ResultSet rs =
	                    statement.executeQuery()
	    ) {

	        while (rs.next()) {

	            Long lockUserId = null;

	            if (rs.getObject("lock_user_id") != null) {

	                lockUserId =
	                        rs.getLong("lock_user_id");
	            }


	            String lockUserName =
	                    rs.getString("lock_user_name");


	            DashboardBatchDto batch =
	                    new DashboardBatchDto(

	                            rs.getLong("batch_id"),

	                            rs.getInt("total_cheques"),

	                            rs.getString("batch_status"),

	                            lockUserId,

	                            lockUserName,

	                            rs.getString("lock_status")
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

        if (batchId == null || userId == null) {
            return false;
        }

        String checkSql = """
                SELECT 1
                FROM public.inward_batch_lock
                WHERE batch_id = ?
                  AND lock_status = 'LOCKED'
                LIMIT 1
                """;

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

        try (Connection connection =
                ConnectionPool
                        .getDataSource()
                        .getConnection()) {

            connection.setAutoCommit(false);

            try (PreparedStatement ps =
                    connection.prepareStatement(checkSql)) {

                ps.setLong(1, batchId);

                try (ResultSet rs =
                        ps.executeQuery()) {

                    if (rs.next()) {
                        connection.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement ps =
                    connection.prepareStatement(lockSql)) {

                ps.setLong(1, batchId);
                ps.setLong(2, userId);

                if (ps.executeUpdate() != 1) {
                    connection.rollback();
                    return false;
                }
            }

            try (PreparedStatement ps =
                    connection.prepareStatement(historySql)) {

                ps.setLong(1, batchId);
                ps.setLong(2, userId);

                if (ps.executeUpdate() != 1) {
                    connection.rollback();
                    return false;
                }
            }

            connection.commit();
            return true;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error locking batch: " + batchId,
                    e);
        }
    }


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

        String status =
                batchStatus.trim().toUpperCase();

        if (!status.equals("MICR_REPAIR")
                && !status.equals("DATA_ENTRY")
                && !status.equals("SENT_TO_CHECKER")) {

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

        try (Connection connection =
                ConnectionPool
                        .getDataSource()
                        .getConnection()) {

            connection.setAutoCommit(false);

            try (PreparedStatement ps =
                    connection.prepareStatement(historySql)) {

                ps.setLong(1, batchId);
                ps.setString(2, status);
                ps.setLong(3, userId);

                if (status.equals("MICR_REPAIR")) {

                    ps.setString(
                            4,
                            "MICR validation requires repair");

                    ps.setString(
                            5,
                            "Batch moved to MICR Repair");

                } else if (status.equals("DATA_ENTRY")) {

                    ps.setString(
                            4,
                            "MICR validation completed");

                    ps.setString(
                            5,
                            "Batch moved to Data Entry");

                } else {

                    ps.setString(
                            4,
                            "Maker processing completed");

                    ps.setString(
                            5,
                            "Batch sent to Checker");
                }

                if (ps.executeUpdate() != 1) {
                    connection.rollback();
                    return false;
                }
            }

            if (status.equals("SENT_TO_CHECKER")) {

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

                try (PreparedStatement ps =
                        connection.prepareStatement(unlockSql)) {

                    ps.setLong(1, batchId);
                    ps.setLong(2, userId);

                    if (ps.executeUpdate() != 1) {
                        connection.rollback();
                        return false;
                    }
                }
            }

            connection.commit();
            return true;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error updating batch status: "
                            + batchId,
                    e);
        }
    }


    @Override
    public List<String> getReturnedChequeReasons(
            Long batchId) {

        List<String> reasons =
                new ArrayList<>();

        if (batchId == null) {
            return reasons;
        }

        String sql = """
                SELECT DISTINCT latest.return_reason_code
                FROM public.inward_cheque c

                JOIN LATERAL (
                    SELECT
                        sh.status,
                        sh.return_reason_code
                    FROM public.inward_cheque_status_history sh
                    WHERE sh.cheque_number = c.cheque_number
                    ORDER BY sh.status_history_id DESC
                    LIMIT 1
                ) latest ON TRUE

                WHERE c.batch_id = ?
                  AND latest.status = 'RETURN_TO_MAKER'
                """;

        try (
                Connection connection =
                        ConnectionPool
                                .getDataSource()
                                .getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setLong(1, batchId);

            try (ResultSet rs =
                    ps.executeQuery()) {

                while (rs.next()) {

                    String reason =
                            rs.getString(
                                    "return_reason_code");

                    if (reason != null
                            && !reason.trim().isEmpty()) {

                        reasons.add(
                                reason.trim());
                    }
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return reasons;
    }
}