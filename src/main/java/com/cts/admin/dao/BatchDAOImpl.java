package com.cts.admin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.cts.admin.model.Batch;
import com.cts.inward.config.ConnectionPool;

public class BatchDAOImpl implements BatchDAO {

    /* ================================================================
     * BATCH CAPTURE TAB
     * Source: outward_batch joined with "user" for created_by name
     * Columns: Batch ID | Branch | Cheque Count | Created By | Created At
     * ================================================================ */

    @Override
    public List<Batch> getBatchCaptureBatches() {

        String sql =
                "SELECT ob.batch_number        AS batch_id, "
                + "       ob.branch_code        AS branch, "
                + "       ob.cheque_count, "
                + "       u.username            AS captured_by_username, "
                + "       ob.created_at "
                + "FROM   outward_batch ob "
                + "LEFT   JOIN \"user\" u ON ob.created_by = u.user_id "
                + "ORDER  BY ob.created_at DESC";

        List<Batch> list = new ArrayList<>();

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                Batch b = new Batch();
                b.setBatchType("BATCH_CAPTURE");

                /* batch_number is varchar — store as string in batchId display */
                String batchNum = rs.getString("batch_id");
                try { b.setBatchId(Long.parseLong(batchNum)); }
                catch (NumberFormatException e) { b.setBatchId(0L); }

                b.setBranch(rs.getString("branch"));
                b.setChequeCount(rs.getInt("cheque_count"));
                b.setCapturedBy(rs.getString("captured_by_username"));
                b.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(b);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch Batch Capture batches.", e);
        }

        return list;
    }

    /* ================================================================
     * INWARD TAB
     * Source: inward_batch
     *   Status  → latest row in inward_batch_history
     *   Maker   → first LOCKED row in inward_batch_lock (earliest locked_time)
     *   Checker → second LOCKED row in inward_batch_lock (latest locked_time
     *             where a prior UNLOCKED exists)
     *   Maker Receive Time   → locked_time of first lock
     *   Checker Receive Time → locked_time of second lock
     *   Batch Completion Time → changed_on from inward_batch_history
     *                           where batch_status ends with COMPLETED/ACCEPTED
     * ================================================================ */

    @Override
    public List<Batch> getInwardBatches() {

        String sql =
                /* Base batch info */
                "SELECT ib.batch_id, "
                + "       ib.total_cheques AS cheque_count, "

                /* Latest status from history */
                + "       (SELECT h.batch_status "
                + "        FROM   inward_batch_history h "
                + "        WHERE  h.batch_id = ib.batch_id "
                + "        ORDER  BY h.changed_on DESC "
                + "        LIMIT  1) AS current_status, "

                /* Maker — first LOCKED row */
                + "       (SELECT u.username "
                + "        FROM   inward_batch_lock l "
                + "        JOIN   \"user\" u ON l.user_id = u.user_id "
                + "        WHERE  l.batch_id = ib.batch_id "
                + "        AND    l.lock_status = 'LOCKED' "
                + "        ORDER  BY l.locked_time ASC "
                + "        LIMIT  1) AS maker_username, "

                /* Maker receive time — first lock time */
                + "       (SELECT l.locked_time "
                + "        FROM   inward_batch_lock l "
                + "        WHERE  l.batch_id = ib.batch_id "
                + "        AND    l.lock_status = 'LOCKED' "
                + "        ORDER  BY l.locked_time ASC "
                + "        LIMIT  1) AS maker_receive_time, "

                /* Checker — second LOCKED row (latest) */
                + "       (SELECT u.username "
                + "        FROM   inward_batch_lock l "
                + "        JOIN   \"user\" u ON l.user_id = u.user_id "
                + "        WHERE  l.batch_id = ib.batch_id "
                + "        AND    l.lock_status = 'LOCKED' "
                + "        ORDER  BY l.locked_time DESC "
                + "        LIMIT  1) AS checker_username, "

                /* Checker receive time — latest lock time */
                + "       (SELECT l.locked_time "
                + "        FROM   inward_batch_lock l "
                + "        WHERE  l.batch_id = ib.batch_id "
                + "        AND    l.lock_status = 'LOCKED' "
                + "        ORDER  BY l.locked_time DESC "
                + "        LIMIT  1) AS checker_receive_time, "

                /* Batch completion time */
                + "       (SELECT h.changed_on "
                + "        FROM   inward_batch_history h "
                + "        WHERE  h.batch_id = ib.batch_id "
                + "        AND    (UPPER(h.batch_status) LIKE '%COMPLET%' "
                + "             OR UPPER(h.batch_status) LIKE '%ACCEPT%') "
                + "        ORDER  BY h.changed_on DESC "
                + "        LIMIT  1) AS batch_completion_time "

                + "FROM   inward_batch ib "
                + "ORDER  BY ib.batch_id DESC";

        List<Batch> list = new ArrayList<>();

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                Batch b = new Batch();
                b.setBatchType("INWARD");
                b.setBatchId(rs.getLong("batch_id"));
                b.setChequeCount(rs.getInt("cheque_count"));
                b.setStatus(rs.getString("current_status"));
                b.setMaker(rs.getString("maker_username"));
                b.setChecker(rs.getString("checker_username"));
                b.setMakerReceiveTime(rs.getTimestamp("maker_receive_time"));
                b.setCheckerReceiveTime(rs.getTimestamp("checker_receive_time"));
                b.setBatchCompletionTime(rs.getTimestamp("batch_completion_time"));
                list.add(b);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch Inward batches.", e);
        }

        return list;
    }

    /* ================================================================
     * OUTWARD TAB
     * Source: outward_batch joined with outward_batch_assignment
     *   Maker   → assignment where assignment_role = 'MAKER'
     *   Checker → assignment where assignment_role = 'CHECKER'
     *   Maker Receive Time   → started_at for MAKER assignment
     *   Checker Receive Time → started_at for CHECKER assignment
     *   Batch Completion Time → completed_at for CHECKER assignment
     * ================================================================ */

    @Override
    public List<Batch> getOutwardBatches() {

        String sql =
                "SELECT ob.batch_number         AS batch_id_str, "
                + "       ob.branch_code         AS branch, "
                + "       ob.cheque_count, "
                + "       ob.batch_status        AS current_status, "
                + "       ob.created_at, "

                /* Maker */
                + "       (SELECT u.username "
                + "        FROM   outward_batch_assignment a "
                + "        JOIN   \"user\" u ON a.user_id = u.user_id "
                + "        WHERE  a.batch_number = ob.batch_number "
                + "        AND    a.assignment_role = 'MAKER' "
                + "        LIMIT  1) AS maker_username, "

                /* Maker receive time */
                + "       (SELECT a.started_at "
                + "        FROM   outward_batch_assignment a "
                + "        WHERE  a.batch_number = ob.batch_number "
                + "        AND    a.assignment_role = 'MAKER' "
                + "        LIMIT  1) AS maker_receive_time, "

                /* Checker */
                + "       (SELECT u.username "
                + "        FROM   outward_batch_assignment a "
                + "        JOIN   \"user\" u ON a.user_id = u.user_id "
                + "        WHERE  a.batch_number = ob.batch_number "
                + "        AND    a.assignment_role = 'CHECKER' "
                + "        LIMIT  1) AS checker_username, "

                /* Checker receive time */
                + "       (SELECT a.started_at "
                + "        FROM   outward_batch_assignment a "
                + "        WHERE  a.batch_number = ob.batch_number "
                + "        AND    a.assignment_role = 'CHECKER' "
                + "        LIMIT  1) AS checker_receive_time, "

                /* Batch completion time */
                + "       (SELECT a.completed_at "
                + "        FROM   outward_batch_assignment a "
                + "        WHERE  a.batch_number = ob.batch_number "
                + "        AND    a.assignment_role = 'CHECKER' "
                + "        LIMIT  1) AS batch_completion_time "

                + "FROM   outward_batch ob "
                + "ORDER  BY ob.created_at DESC";

        List<Batch> list = new ArrayList<>();

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                Batch b = new Batch();
                b.setBatchType("OUTWARD");

                String batchNum = rs.getString("batch_id_str");
                try { b.setBatchId(Long.parseLong(batchNum)); }
                catch (NumberFormatException e) { b.setBatchId(0L); }

                b.setBranch(rs.getString("branch"));
                b.setChequeCount(rs.getInt("cheque_count"));
                b.setStatus(rs.getString("current_status"));
                b.setMaker(rs.getString("maker_username"));
                b.setChecker(rs.getString("checker_username"));
                b.setCreatedAt(rs.getTimestamp("created_at"));
                b.setMakerReceiveTime(rs.getTimestamp("maker_receive_time"));
                b.setCheckerReceiveTime(rs.getTimestamp("checker_receive_time"));
                b.setBatchCompletionTime(rs.getTimestamp("batch_completion_time"));
                list.add(b);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch Outward batches.", e);
        }

        return list;
    }
}
