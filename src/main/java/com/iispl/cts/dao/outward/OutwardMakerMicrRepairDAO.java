package com.iispl.cts.dao.outward;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cts.inward.config.ConnectionPool;
import com.iispl.cts.model.outward.OutwardBatch;

public class OutwardMakerMicrRepairDAO {
	
	private final javax.sql.DataSource dataSource =ConnectionPool.getDataSource();

    /*
     * Stores MICR error count separately.
     * No new field is added to OutwardBatch.
     */
    private Map<String, Integer> micrErrorCounts = new HashMap<>();
    
    public List<OutwardBatch> getMicrErrorBatches(long userId) {
        List<OutwardBatch> batches = new ArrayList<>();

        String sql = "SELECT ob.batch_number, " +
                "ob.cheque_count, " +
                "COUNT(oc.cheque_number) " +
                "AS micr_error_count " +
                "FROM outward_batch ob " +
                "INNER JOIN outward_batch_assignment oba " +
                "ON ob.batch_number = oba.batch_number " +
                "INNER JOIN outward_cheque oc " +
                "ON ob.batch_number = oc.batch_number " +
                "WHERE oba.user_id = ? " +
                "AND oba.assignment_role = 'MAKER' " +
                "AND oba.assignment_status " +
                "IN ('ASSIGNED', 'IN_PROGRESS') " +
                "AND ob.batch_status = 'MICR_REPAIR' " +
                "AND oc.cheque_status = 'MICR_ERROR' " +
                "GROUP BY ob.batch_number, " +
                "ob.cheque_count " +
                "ORDER BY ob.batch_number";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            // LOGGED-IN USER ID
            // userId is long, so use setLong()
            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {

                micrErrorCounts.clear();

                while (rs.next()) {

                    String batchNumber =rs.getString("batch_number");
                    int chequeCount =rs.getInt("cheque_count");
                    int micrErrorCount = rs.getInt("micr_error_count");

                    OutwardBatch batch = new OutwardBatch();

                    batch.setBatchNumber(batchNumber);
                    batch.setNumberOfCheques(chequeCount);

                    batches.add(batch);
                    
                    micrErrorCounts.put(batchNumber,micrErrorCount);
                }
            }

        } catch (Exception e) {
            System.out.println("Error loading MICR repair batches");
            e.printStackTrace();
        }

        return batches;
    }

    public int getMicrErrorCount(String batchNumber) {
        return micrErrorCounts.getOrDefault(batchNumber,0);
    }
}