package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.model.NpciBatchData;

public class CheckerReportDaoImpl implements CheckerReportDao {

    private final DataSource dataSource;

    private CheckerReportDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static CheckerReportDao of() {
        return new CheckerReportDaoImpl(ConnectionPool.getDataSource());
    }

    @Override
    public List<NpciBatchData> getCompletedBatches() {
        // Query to filter for 'COMPLETED' status with case/whitespace safety
        String sql = """
                SELECT 
                    b.batch_id, 
                    b.file_id, 
                    b.presenting_bank_name, 
                    b.total_cheques
                FROM inward_batch b
                WHERE UPPER(TRIM((
                    SELECT h.batch_status
                    FROM inward_batch_history h
                    WHERE h.batch_id = b.batch_id
                    ORDER BY h.changed_on DESC, h.batch_history_id DESC
                    LIMIT 1
                ))) = 'COMPLETED'
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
            e.printStackTrace();
            throw new RuntimeException("Error retrieving completed batches for report: " + e.getMessage(), e);
        }

        return batches;
    }
    
    @Override
    public Map<String, Object> getCheckerReportDetails(Long batchId) {
        String sql = """
                SELECT 
                    b.batch_id,
                    b.total_cheques,
                    (SELECT COUNT(*) FROM inward_cheque c WHERE c.batch_id = b.batch_id AND c.status = 'APPROVED') as approved_count,
                    (SELECT COUNT(*) FROM inward_cheque c WHERE c.batch_id = b.batch_id AND c.status = 'REJECTED') as rejected_count
                FROM inward_batch b
                WHERE b.batch_id = ?
                """;

        Map<String, Object> params = new HashMap<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
             
            statement.setLong(1, batchId);
            
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    params.put("BATCH_ID", String.format("BATCH%03d", rs.getLong("batch_id")));
                    params.put("TOTAL_CHEQUES", rs.getInt("total_cheques"));
                    params.put("APPROVED_CHEQUES", rs.getInt("approved_count"));
                    params.put("REJECTED_CHEQUES", rs.getInt("rejected_count"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("DB Error: " + e.getMessage(), e);
        }
        return params;
    }
}