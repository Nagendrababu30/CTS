package com.iispl.cts.dao.outward;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.cts.inward.config.ConnectionPool;
import com.iispl.cts.data.CTSStaticData;
import com.iispl.cts.model.outward.OutwardBatch;

public class OutwardMakerDataEntryDAO {
	private final javax.sql.DataSource dataSource =ConnectionPool.getDataSource();

	
	private int getUserIdByUsername(Connection con, String username) throws SQLException {
		
        String sql = "SELECT user_id FROM public.\"user\" WHERE username = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        }
        return 0;
    }

    public List<OutwardBatch> getAllBatches() {
        List<OutwardBatch> batches = new ArrayList<>();
        String sql = "SELECT batch_number, cheque_count, batch_status "
                   + "FROM public.outward_batch "
                   + "ORDER BY batch_number ASC";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                OutwardBatch batch = new OutwardBatch();
                batch.setBatchNumber(rs.getString("batch_number"));
                batch.setNumberOfCheques(rs.getInt("cheque_count"));
                batch.setBatchStatus(rs.getString("batch_status"));
                batches.add(batch);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return batches;
    }
    
    public List<OutwardBatch> getBatchesForMaker(long userId) {
        List<OutwardBatch> batches = new ArrayList<>();

        String sql = "SELECT b.batch_number, b.branch_code, b.cheque_count, b.batch_folder_path, "
                   + "       b.batch_status, ba.assignment_status "
                   + "FROM public.outward_batch b "
                   + "INNER JOIN public.outward_batch_assignment ba ON b.batch_number = ba.batch_number "
                   + "WHERE ba.user_id = ? "
                   + "  AND ba.assignment_role = 'MAKER' "
                   + "  AND b.batch_status IN ('MICR_VERIFIED', 'MICR_REPAIR_COMPLETED') "
                   + "ORDER BY b.batch_number ASC";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OutwardBatch batch = new OutwardBatch();
                    batch.setBatchNumber(rs.getString("batch_number"));
                    batch.setBranchCode(rs.getString("branch_code"));
                    batch.setNumberOfCheques(rs.getInt("cheque_count"));
                    batch.setBatchFolderPath(rs.getString("batch_folder_path"));
                    batch.setBatchStatus(rs.getString("batch_status"));
                    batch.setMakerAssignmentStatus(rs.getString("assignment_status"));
                    batches.add(batch);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return batches;
    
    }
}