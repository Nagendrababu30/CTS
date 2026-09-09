package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.model.NpciBatchData;

public class ReportDaoImpl implements ReportDao {

    private final DataSource dataSource;

    private ReportDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static ReportDao of() {
        return new ReportDaoImpl(ConnectionPool.getDataSource());
    }

    @Override
    public List<NpciBatchData> getSentToCheckerBatches() {
        // This query specifically looks for the SENT_TO_CHECKER status
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
                ) = 'SENT_TO_CHECKER'
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
            throw new RuntimeException("Error retrieving sent batches for report", e);
        }

        return batches;
    }
    
    @Override
    public Map<String, Object> getBatchReportDetails(Long batchId) {
        // Safe query using only inward_batch table columns we know exist
        String sql = """
                SELECT 
                    batch_id,
                    total_cheques
                FROM inward_batch
                WHERE batch_id = ?
                """;

        Map<String, Object> params = new java.util.HashMap<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
             
            statement.setLong(1, batchId);
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    params.put("BATCH_ID", String.format("BATCH%03d", rs.getLong("batch_id")));
                    params.put("TOTAL_CHEQUES", rs.getInt("total_cheques"));
                    // Defaulting these to 0 for now until we check your exact cheque table columns
                    params.put("MICR_REPAIR_COUNT", 0);
                    params.put("DATA_ENTRY_COUNT", 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("DB Error: " + e.getMessage(), e);
        }
        return params;
    }
}