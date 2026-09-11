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
        String sql = """
                SELECT 
                    b.batch_id, 
                    b.file_id, 
                    b.presenting_bank_name, 
                    b.total_cheques
                FROM inward_batch b
                WHERE (
                    SELECT h.batch_status
                    FROM inward_batch_history h
                    WHERE h.batch_id = b.batch_id
                    ORDER BY h.changed_on DESC
                    LIMIT 1
                ) = 'DATA_ENTRY_COMPLETED'
                ORDER BY b.batch_id
                """;

        List<NpciBatchData> batches = new ArrayList<>();

        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                NpciBatchData batch = new NpciBatchData(
                        resultSet.getLong("batch_id"),
                        resultSet.getLong("file_id"),
                        resultSet.getString("presenting_bank_name"),
                        resultSet.getInt("total_cheques")
                );
                batches.add(batch);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving DATA_ENTRY_COMPLETED batches", e);
        }

        return batches;
    }

    @Override
    public void updateBatchStatusToChecker(Long batchId) {
        // Query 1: Update the history status
        String updateHistorySql = """
                UPDATE inward_batch_history 
                SET batch_status = 'SENT_TO_CHECKER',
                    changed_on = CURRENT_TIMESTAMP,
                    reason = 'Forwarded to Inward Checker',
                    remarks = 'Actioned from UI'
                WHERE batch_id = ? 
                  AND batch_status = 'DATA_ENTRY_COMPLETED'
                """;

        // Query 2: Release the lock in inward_batch_lock
        String unlockBatchSql = """
                UPDATE inward_batch_lock 
                SET lock_status = 'UNLOCKED'
                WHERE batch_id = ?
                """;

        // Query 3: Insert SENT_TO_CHECKER status for all cheques in the batch
        String insertChequeSql = """
                INSERT INTO inward_cheque_status_history
                (cheque_number, status)
                SELECT cheque_number, 'SENT_TO_CHECKER'
                FROM inward_cheque
                WHERE batch_id = ?
                """;

        try (Connection connection = dataSource.getConnection()) {
            
            // 1. Start Transaction
            connection.setAutoCommit(false); 

            try (
                PreparedStatement historyStmt = connection.prepareStatement(updateHistorySql);
                PreparedStatement lockStmt = connection.prepareStatement(unlockBatchSql);
                PreparedStatement chequeStmt = connection.prepareStatement(insertChequeSql)
            ) {
                // 2. Execute History Update
                historyStmt.setLong(1, batchId);
                int rowsAffected = historyStmt.executeUpdate();
                
                if (rowsAffected == 0) {
                    throw new RuntimeException("Update failed. No 'DATA_ENTRY_COMPLETED' status found for Batch ID: " + batchId);
                }
                
                // 3. Execute Unlock Update
                lockStmt.setLong(1, batchId);
                lockStmt.executeUpdate();

                // 4. Insert SENT_TO_CHECKER status for all cheques in the batch
                chequeStmt.setLong(1, batchId);
                chequeStmt.executeUpdate();
                
                // 5. Commit all three queries
                connection.commit(); 
                
            } catch (Exception e) {
                // If anything fails, rollback BOTH queries
                connection.rollback(); 
                throw e;
            } finally {
                // Always reset auto-commit back to true to safely return the connection to the pool
                connection.setAutoCommit(true); 
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Error updating batch status and unlocking batch", e);
        }
    }
}