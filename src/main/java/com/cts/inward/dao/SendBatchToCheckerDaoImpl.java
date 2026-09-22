package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.model.NpciBatchData;

public class SendBatchToCheckerDaoImpl implements SendBatchToCheckerDao {

    private final DataSource dataSource;

    private SendBatchToCheckerDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static SendBatchToCheckerDao of() {
        return new SendBatchToCheckerDaoImpl(ConnectionPool.getDataSource());
    }

    @Override
    public List<NpciBatchData> getReadyBatches() {
        return getReadyBatches(null);
    }

    @Override
    public List<NpciBatchData> getReadyBatches(Long userId) {
        String sql = """
                SELECT 
                    b.batch_id, 
                    b.file_id, 
                    b.presenting_bank_name, 
                    b.total_cheques
                FROM inward_batch b
                INNER JOIN LATERAL (
                    SELECT bl.user_id, bl.lock_status
                    FROM inward_batch_lock bl
                    INNER JOIN public."user" u ON u.user_id = bl.user_id
                    INNER JOIN public."role" r ON r.role_id = u.role_id
                    WHERE bl.batch_id = b.batch_id
                      AND u.status = 'ACTIVE'
                      AND r.role_name = 'Inward Maker'
                    ORDER BY bl.locked_time DESC, bl.lock_id DESC
                    LIMIT 1
                ) l ON TRUE
                WHERE (
                    SELECT h.batch_status
                    FROM inward_batch_history h
                    WHERE h.batch_id = b.batch_id
                    ORDER BY h.changed_on DESC NULLS LAST, h.batch_history_id DESC
                    LIMIT 1
                ) = 'DATA_ENTRY_COMPLETED'
                  AND l.lock_status = 'LOCKED'
                """ + (userId != null ? """
                  AND (
                      l.user_id = ?
                      OR (
                          SELECT h2.changed_by
                          FROM inward_batch_history h2
                          WHERE h2.batch_id = b.batch_id
                            AND h2.batch_status = 'DATA_ENTRY_COMPLETED'
                          ORDER BY h2.changed_on DESC NULLS LAST, h2.batch_history_id DESC
                          LIMIT 1
                      ) = ?
                  )
                """ : "") + """
                ORDER BY b.batch_id
                """;

        List<NpciBatchData> batches = new ArrayList<>();

        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            if (userId != null) {
                statement.setLong(1, userId);
                statement.setLong(2, userId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    NpciBatchData batch = new NpciBatchData(
                            resultSet.getLong("batch_id"),
                            resultSet.getLong("file_id"),
                            resultSet.getString("presenting_bank_name"),
                            resultSet.getInt("total_cheques")
                    );
                    batches.add(batch);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving DATA_ENTRY_COMPLETED batches", e);
        }

        return batches;
    }

    @Override
    public void updateBatchStatusToChecker(Long batchId) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            try {
                // 1. Check if this batch has a previous Checker (i.e. was returned to maker)
                Integer previousCheckerId = null;
                String findCheckerSql = """
                        SELECT bl.user_id
                        FROM inward_batch_lock bl
                        JOIN public."user" u ON u.user_id = bl.user_id
                        JOIN public."role" r ON r.role_id = u.role_id
                        WHERE bl.batch_id = ?
                          AND r.role_name = 'Inward Checker'
                        ORDER BY bl.locked_time DESC, bl.lock_id DESC
                        LIMIT 1
                        """;

                try (PreparedStatement checkStmt = connection.prepareStatement(findCheckerSql)) {
                    checkStmt.setLong(1, batchId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            previousCheckerId = rs.getInt("user_id");
                        }
                    }
                }

                // 2. Unlock Maker's current lock
                String unlockMakerSql = """
                        UPDATE inward_batch_lock
                        SET lock_status = 'UNLOCKED',
                            locked_time = CURRENT_TIMESTAMP
                        WHERE batch_id = ?
                          AND lock_status = 'LOCKED'
                        """;
                try (PreparedStatement unlockStmt = connection.prepareStatement(unlockMakerSql)) {
                    unlockStmt.setLong(1, batchId);
                    unlockStmt.executeUpdate();
                }

                // 3. If this batch had a previous Checker, re-lock for that Checker
                if (previousCheckerId != null) {
                    String relockCheckerSql = """
                            UPDATE inward_batch_lock
                            SET lock_status = 'LOCKED',
                                locked_time = CURRENT_TIMESTAMP
                            WHERE batch_id = ?
                              AND user_id = ?
                            """;
                    int relockedRows = 0;
                    try (PreparedStatement relockStmt = connection.prepareStatement(relockCheckerSql)) {
                        relockStmt.setLong(1, batchId);
                        relockStmt.setInt(2, previousCheckerId);
                        relockedRows = relockStmt.executeUpdate();
                    }

                    if (relockedRows == 0) {
                        String insertCheckerLockSql = """
                                INSERT INTO inward_batch_lock
                                (batch_id, user_id, locked_time, lock_status)
                                VALUES (?, ?, CURRENT_TIMESTAMP, 'LOCKED')
                                """;
                        try (PreparedStatement insertLockStmt = connection.prepareStatement(insertCheckerLockSql)) {
                            insertLockStmt.setLong(1, batchId);
                            insertLockStmt.setInt(2, previousCheckerId);
                            insertLockStmt.executeUpdate();
                        }
                    }
                }

                // 4. Update batch history status (update existing row, fallback to insert if none exists)
                String updateHistorySql = """
                        UPDATE inward_batch_history
                        SET batch_status = 'SENT_TO_CHECKER',
                            changed_on = CURRENT_TIMESTAMP,
                            reason = 'Forwarded to Inward Checker',
                            remarks = 'Actioned from UI'
                        WHERE batch_id = ?
                        """;
                int rowsAffected = 0;
                try (PreparedStatement historyStmt = connection.prepareStatement(updateHistorySql)) {
                    historyStmt.setLong(1, batchId);
                    rowsAffected = historyStmt.executeUpdate();
                }

                if (rowsAffected == 0) {
                    String insertHistorySql = """
                            INSERT INTO inward_batch_history
                            (batch_id, batch_status, changed_on, changed_by, reason, remarks)
                            VALUES (?, 'SENT_TO_CHECKER', CURRENT_TIMESTAMP, NULL, 'Forwarded to Inward Checker', 'Actioned from UI')
                            """;
                    try (PreparedStatement insertHistStmt = connection.prepareStatement(insertHistorySql)) {
                        insertHistStmt.setLong(1, batchId);
                        insertHistStmt.executeUpdate();
                    }
                }

                // 5. Update existing status to SENT_TO_CHECKER for cheques in the batch (excluding RETURN_BY_MAKER, ACCEPT, REJECT)
                String updateChequeSql = """
                        UPDATE inward_cheque_status_history
                        SET status = 'SENT_TO_CHECKER'
                        WHERE cheque_number IN (
                            SELECT c.cheque_number
                            FROM inward_cheque c
                            WHERE c.batch_id = ?
                        )
                        AND status NOT IN ('RETURN_BY_MAKER', 'ACCEPT', 'REJECT')
                        """;
                try (PreparedStatement chequeStmt = connection.prepareStatement(updateChequeSql)) {
                    chequeStmt.setLong(1, batchId);
                    chequeStmt.executeUpdate();
                }

                // 5b. Fallback: insert for any cheque in batch that has no row in status history yet
                String insertMissingChequeSql = """
                        INSERT INTO inward_cheque_status_history
                        (cheque_number, status)
                        SELECT c.cheque_number, 'SENT_TO_CHECKER'
                        FROM inward_cheque c
                        WHERE c.batch_id = ?
                          AND NOT EXISTS (
                              SELECT 1 FROM inward_cheque_status_history h
                              WHERE h.cheque_number = c.cheque_number
                          )
                        """;
                try (PreparedStatement insertMissingStmt = connection.prepareStatement(insertMissingChequeSql)) {
                    insertMissingStmt.setLong(1, batchId);
                    insertMissingStmt.executeUpdate();
                }

                connection.commit();

            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error updating batch status and forwarding to checker", e);
        }
    }
}