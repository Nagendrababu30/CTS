package com.iispl.cts.dao.outward;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.iispl.cts.data.CTSStaticData;
import com.iispl.cts.model.outward.OutwardBatch;

public class OutwardMakerSendCheckerDAO {

    // =========================================================
    // GET BATCHES READY FOR CHECKER
    // =========================================================

    public List<OutwardBatch> getBatchesReadyForChecker(int userId) {
        List<OutwardBatch> batches = new ArrayList<>();

        String sql = "SELECT b.batch_number, b.branch_code, b.cheque_count, b.batch_folder_path, b.batch_status "
                   + "FROM public.outward_batch b "
                   + "INNER JOIN public.outward_batch_assignment ba ON b.batch_number = ba.batch_number "
                   + "WHERE ba.user_id = ? "
                   + "  AND ba.assignment_role = 'MAKER' "
                   + "  AND b.batch_status = 'READY_TO_SUBMIT' "
                   + "ORDER BY b.batch_number ASC";

        try (Connection con = CTSStaticData.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OutwardBatch batch = new OutwardBatch();
                    batch.setBatchNumber(rs.getString("batch_number"));
                    batch.setBranchCode(rs.getString("branch_code"));
                    batch.setNumberOfCheques(rs.getInt("cheque_count"));
                    batch.setBatchFolderPath(rs.getString("batch_folder_path"));
                    batch.setBatchStatus(rs.getString("batch_status"));
                    batches.add(batch);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return batches;
    }

    // =========================================================
    // UPDATE BATCH STATUS
    // =========================================================

    public boolean updateBatchStatus(String batchNumber, String newStatus) {
        String sql = "UPDATE public.outward_batch SET batch_status = ? WHERE batch_number = ?";

        try (Connection con = CTSStaticData.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setString(2, batchNumber);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================================================
    // SEND BATCH TO CHECKER
    // =========================================================

    public boolean sendToChecker(String batchNumber, int userId) throws Exception {
        // Uses PostgreSQL UPDATE ... FROM syntax to verify ownership and update status
        String sql = "UPDATE public.outward_batch b "
                   + "SET batch_status = ? "
                   + "FROM public.outward_batch_assignment ba "
                   + "WHERE b.batch_number = ba.batch_number "
                   + "  AND b.batch_number = ? "
                   + "  AND ba.user_id = ? "
                   + "  AND ba.assignment_role = 'MAKER' "
                   + "  AND b.batch_status = ?";

        System.out.println("======================================");
        System.out.println("SEND BATCH TO CHECKER");
        System.out.println("======================================");
        System.out.println("Batch ID = " + batchNumber);
        System.out.println("Maker ID = " + userId);
        System.out.println("SQL = " + sql);

        try (Connection con = CTSStaticData.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "SUBMITTED_TO_CHECKER");
            ps.setString(2, batchNumber);
            ps.setInt(3, userId);
            ps.setString(4, "READY_TO_SUBMIT");

            int rowsUpdated = ps.executeUpdate();
            System.out.println("Rows Updated = " + rowsUpdated);

            if (rowsUpdated > 0) {
                System.out.println("Batch " + batchNumber + " successfully sent to Checker.");
                return true;
            }

            System.out.println("Batch " + batchNumber + " was NOT sent to Checker.");
            return false;

        } catch (Exception e) {
            System.err.println("ERROR WHILE SENDING BATCH TO CHECKER");
            e.printStackTrace();
            throw e;
        }
    }
}