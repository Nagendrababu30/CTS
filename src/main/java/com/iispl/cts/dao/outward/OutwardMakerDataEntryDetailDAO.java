package com.iispl.cts.dao.outward;

import java.security.Timestamp;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cts.inward.config.ConnectionPool;
import com.iispl.cts.data.CTSStaticData;
import com.iispl.cts.model.outward.OutwardCheque;

public class OutwardMakerDataEntryDetailDAO {
	private final javax.sql.DataSource dataSource =ConnectionPool.getDataSource();


    public List<OutwardCheque> getCheques(String batchNumber) {
        List<OutwardCheque> cheques = new ArrayList<>();
        String sql = "SELECT batch_number, cheque_number, drawer_account_number, drawer_name, "
                   + "       payee_account_number, payee_name, amount, amount_in_words, "
                   + "       cheque_date, front_image_path, back_image_path, cheque_status "
                   + "FROM public.outward_cheque "
                   + "WHERE batch_number = ? "
                   + "ORDER BY cheque_number ASC";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, batchNumber);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OutwardCheque cheque = new OutwardCheque();
                    cheque.setBatchNumber(rs.getString("batch_number"));
                    cheque.setChequeNumber(rs.getString("cheque_number"));
                    cheque.setDrawerAccountNumber(rs.getString("drawer_account_number"));
                    cheque.setDrawerName(rs.getString("drawer_name"));
                    cheque.setDepositorAccountNumber(rs.getString("payee_account_number"));
                    cheque.setPayeeName(rs.getString("payee_name"));
                    cheque.setAmount(rs.getBigDecimal("amount"));
                    cheque.setAmountInWords(rs.getString("amount_in_words"));

                    Date dt = rs.getDate("cheque_date");
                    if (dt != null) {
                        cheque.setChequeDate(dt.toLocalDate());
                    }

                    cheque.setFrontImagePath(rs.getString("front_image_path"));
                    cheque.setBackImagePath(rs.getString("back_image_path"));
                    cheque.setChequeStatus(rs.getString("cheque_status"));
                    cheques.add(cheque);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cheques;
    }

    public void saveCheque(OutwardCheque cheque) {
        String updateCheque = "UPDATE public.outward_cheque "
                            + "SET drawer_account_number = ?, drawer_name = ?, payee_name = ?, "
                            + "    amount = ?, amount_in_words = ?, cheque_date = ?, cheque_status = 'VERIFIED' "
                            + "WHERE batch_number = ? AND cheque_number = ?";

        String insertVerification = "INSERT INTO public.cheque_verification "
                                  + "(batch_number, cheque_number, verified_by, verification_status, verified_at) "
                                  + "VALUES (?, ?, 103, 'VERIFIED', CURRENT_TIMESTAMP) "
                                  + "ON CONFLICT (batch_number, cheque_number, verified_by) "
                                  + "DO UPDATE SET verification_status = 'VERIFIED', verified_at = CURRENT_TIMESTAMP";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps1 = con.prepareStatement(updateCheque);
             PreparedStatement ps2 = con.prepareStatement(insertVerification)) {

            ps1.setString(1, cheque.getDrawerAccountNumber());
            ps1.setString(2, cheque.getDrawerName());
            ps1.setString(3, cheque.getPayeeName());
            ps1.setBigDecimal(4, cheque.getAmount());
            ps1.setString(5, cheque.getAmountInWords());
            ps1.setDate(6, cheque.getChequeDate() != null ? Date.valueOf(cheque.getChequeDate()) : null);
            ps1.setString(7, cheque.getBatchNumber());
            ps1.setString(8, cheque.getChequeNumber());
            ps1.executeUpdate();

            ps2.setString(1, cheque.getBatchNumber());
            ps2.setString(2, cheque.getChequeNumber());
            ps2.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void rejectCheque(OutwardCheque cheque, String reason) {
        String updateCheque = "UPDATE public.outward_cheque "
                            + "SET cheque_status = 'REJECTED' "
                            + "WHERE batch_number = ? AND cheque_number = ?";

        String insertRejection = "INSERT INTO public.cheque_rejection "
                               + "(batch_number, cheque_number, rejected_by, rejection_reason) "
                               + "VALUES (?, ?, 103, ?) "
                               + "ON CONFLICT (batch_number, cheque_number) "
                               + "DO UPDATE SET rejected_by = EXCLUDED.rejected_by, rejection_reason = EXCLUDED.rejection_reason";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps1 = con.prepareStatement(updateCheque);
             PreparedStatement ps2 = con.prepareStatement(insertRejection)) {

            ps1.setString(1, cheque.getBatchNumber());
            ps1.setString(2, cheque.getChequeNumber());
            ps1.executeUpdate();

            ps2.setString(1, cheque.getBatchNumber());
            ps2.setString(2, cheque.getChequeNumber());
            ps2.setString(3, reason);
            ps2.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public boolean completeBatchDataEntry(String batchNumber, int userId) {
        String updateBatchSql = "UPDATE public.outward_batch "
                              + "SET batch_status = 'READY_TO_SUBMIT' "
                              + "WHERE batch_number = ?";

        String updateAssignmentSql = "UPDATE public.outward_batch_assignment "
                                   + "SET completed_at = ?, assignment_status = 'COMPLETED' "
                                   + "WHERE batch_number = ? AND user_id = ? AND assignment_role = 'MAKER'";

        Connection con = null;

        try {
            con = dataSource.getConnection();
            con.setAutoCommit(false); // Start transaction

            // 1. Update batch status to READY_TO_SUBMIT
            try (PreparedStatement psBatch = con.prepareStatement(updateBatchSql)) {
                psBatch.setString(1, batchNumber);
                int batchRows = psBatch.executeUpdate();
                if (batchRows == 0) {
                    con.rollback();
                    return false;
                }
            }

            // 2. Update assignment completion timestamp and status
            try (PreparedStatement psAssign = con.prepareStatement(updateAssignmentSql)) {
            	psAssign.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis()));
                psAssign.setString(2, batchNumber);
                psAssign.setInt(3, userId);
                int assignRows = psAssign.executeUpdate();
                if (assignRows == 0) {
                    con.rollback();
                    return false;
                }
            }

            con.commit(); // Commit transaction
            return true;

        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback(); // Rollback on error
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
}
    /**
     * Records Maker Verification on Save & Next.
     * maker_reason_id is explicitly set to NULL.
     */
   
    public boolean saveMakerVerify(String batchNumber, String chequeNumber, int makerId) throws SQLException {
        String sql = "INSERT INTO public.cheque_processing "
                   + "(batch_number, cheque_number, maker_id, maker_action, maker_reason_code) "
                   + "VALUES (?, ?, ?, 'VERIFY', NULL) "
                   + "ON CONFLICT (batch_number, cheque_number) DO UPDATE SET "
                   + "  maker_id = EXCLUDED.maker_id, "
                   + "  maker_action = 'VERIFY', "
                   + "  maker_reason_code = NULL";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, batchNumber);
            ps.setString(2, chequeNumber);
            ps.setInt(3, makerId);

            return ps.executeUpdate() > 0;
        }
    }
    /**
     * Records Maker Rejection Request with the selected reason ID.
     */
    /**
     * Records Maker Rejection Request and marks the cheque as REJECT_REQUESTED.
     */
    
/**
* Fetches all reasons from public.return_reason_master
*/
    public Map<String, String> getReturnReasons() {
        Map<String, String> reasons = new java.util.LinkedHashMap<>();
        
        String sql = "SELECT reason_code, reason_name FROM public.return_reason_master WHERE active = true ORDER BY reason_name ASC";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String code = rs.getString("reason_code");
                String name = rs.getString("reason_name");
                reasons.put(code, name);
            }
            System.out.println("[DEBUG-CTS] Successfully loaded " + reasons.size() + " return reasons from database.");

        } catch (Exception e) {
            System.err.println("[DEBUG-CTS] Failed to load return reasons: " + e.getMessage());
            e.printStackTrace();
        }
        return reasons;
    }
    public boolean saveMakerReject(String batchNumber, String chequeNumber, int makerId, String reasonCode) {
        String insertProcessingSql = "INSERT INTO public.cheque_processing "
                                   + "(batch_number, cheque_number, maker_id, maker_action, maker_reason_code) "
                                   + "VALUES (?, ?, ?, 'REJECT_REQUEST', ?) "
                                   + "ON CONFLICT (batch_number, cheque_number) DO UPDATE SET "
                                   + "  maker_id = EXCLUDED.maker_id, "
                                   + "  maker_action = 'REJECT_REQUEST', "
                                   + "  maker_reason_code = EXCLUDED.maker_reason_code";

        String updateChequeStatusSql = "UPDATE public.outward_cheque "
                                     + "SET cheque_status = 'REJECT_REQUESTED' "
                                     + "WHERE batch_number = ? AND cheque_number = ?";

        Connection con = null;
        try {
            con = dataSource.getConnection();
            con.setAutoCommit(false); // Begin transaction

            // 1. Insert/Update cheque_processing audit record
            try (PreparedStatement ps1 = con.prepareStatement(insertProcessingSql)) {
                ps1.setString(1, batchNumber);
                ps1.setString(2, chequeNumber);
                ps1.setInt(3, makerId);
                ps1.setString(4, reasonCode);
                ps1.executeUpdate();
            }

            // 2. Update outward_cheque table status
            try (PreparedStatement ps2 = con.prepareStatement(updateChequeStatusSql)) {
                ps2.setString(1, batchNumber);
                ps2.setString(2, chequeNumber);
                ps2.executeUpdate();
            }

            con.commit();
            System.out.println("[DEBUG-CTS] Successfully recorded reject for Cheque " + chequeNumber + " with reason: " + reasonCode);
            return true;

        } catch (SQLException e) {
            System.err.println("[DEBUG-CTS] SQL Error in saveMakerReject: " + e.getMessage());
            e.printStackTrace();
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
/**
* Updates cheque status in public.outward_cheque
*/
public boolean updateChequeStatus(String batchNumber, String chequeNumber, String status) throws SQLException {
String sql = "UPDATE public.outward_cheque SET cheque_status = ? WHERE batch_number = ? AND cheque_number = ?";
try (Connection con = dataSource.getConnection();
PreparedStatement ps = con.prepareStatement(sql)) {
ps.setString(1, status);
ps.setString(2, batchNumber);
ps.setString(3, chequeNumber);
return ps.executeUpdate() > 0;
}
}
    }
