package com.iispl.cts.dao.outward;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.cts.inward.config.ConnectionPool;
import com.iispl.cts.data.CTSStaticData;
import com.iispl.cts.model.outward.OutwardCheque;

public class OutwardMakerMicrRepairDetailDAO {
	private final javax.sql.DataSource dataSource =ConnectionPool.getDataSource();


public List<OutwardCheque> getMicrErrorCheques(String batchNumber) {

    List<OutwardCheque> cheques = new ArrayList<>();

    String sql =
            "SELECT batch_number, " +
            "       cheque_number, " +
            "       city_code, " +
            "       bank_code, " +
            "       branch_code, " +
            "       drawer_account_number, " +
            "       drawer_name, " +
            "       amount, " +
            "       amount_in_words, " +
            "       cheque_date, " +
            "       front_image_path, " +
            "       back_image_path, " +
            "       cheque_status " +
            "FROM outward_cheque " +
            "WHERE batch_number = ? " +
            "AND cheque_status = 'MICR_ERROR' " +
            "ORDER BY cheque_number";

    try (Connection connection = dataSource.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {

        ps.setString(1, batchNumber);

        try (ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                OutwardCheque cheque = new OutwardCheque();

                cheque.setBatchNumber(
                        rs.getString("batch_number"));

                cheque.setChequeNumber(
                        rs.getString("cheque_number"));

                cheque.setCityCode(
                        rs.getString("city_code"));

                cheque.setBankCode(
                        rs.getString("bank_code"));

                cheque.setBranchCode(
                        rs.getString("branch_code"));

                cheque.setDrawerAccountNumber(
                        rs.getString("drawer_account_number"));

                cheque.setDrawerName(
                        rs.getString("drawer_name"));

                cheque.setAmount(
                        rs.getBigDecimal("amount"));

                cheque.setAmountInWords(
                        rs.getString("amount_in_words"));

                if (rs.getDate("cheque_date") != null) {
                    cheque.setChequeDate(
                            rs.getDate("cheque_date").toLocalDate());
                }

                cheque.setFrontImagePath(
                        rs.getString("front_image_path"));

                cheque.setBackImagePath(
                        rs.getString("back_image_path"));

                cheque.setChequeStatus(
                        rs.getString("cheque_status"));

                cheques.add(cheque);
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
    }

    return cheques;
}


public boolean updateCorrectedMicr(
        String batchNumber,
        String chequeNumber,
        String cityCode,
        String bankCode,
        String branchCode) {

    String sql =
            "UPDATE outward_cheque " +
            "SET city_code = ?, " +
            "    bank_code = ?, " +
            "    branch_code = ?, " +
            "    cheque_status = 'MICR_REPAIRED' " +
            "WHERE batch_number = ? " +
            "AND cheque_number = ? " +
            "AND cheque_status = 'MICR_ERROR'";

    try (Connection connection = dataSource.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {

        ps.setString(1, cityCode);
        ps.setString(2, bankCode);
        ps.setString(3, branchCode);
        ps.setString(4, batchNumber);
        ps.setString(5, chequeNumber);

        int rowsUpdated = ps.executeUpdate();

        return rowsUpdated > 0;

    } catch (Exception e) {
        e.printStackTrace();
    }

    return false;
}


public boolean hasRemainingMicrErrors(String batchNumber) {

    String sql =
            "SELECT COUNT(*) " +
            "FROM outward_cheque " +
            "WHERE batch_number = ? " +
            "AND cheque_status = 'MICR_ERROR'";

    try (Connection connection = dataSource.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {

        ps.setString(1, batchNumber);

        try (ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }

    } catch (Exception e) {
        e.printStackTrace();
    }

    return false;
}


public boolean updateBatchStatus(String batchNumber) {

    String sql =
            "UPDATE outward_batch " +
            "SET batch_status = 'MICR_REPAIR_COMPLETED' " +
            "WHERE batch_number = ? " +
            "AND batch_status = 'MICR_REPAIR'";

    try (Connection connection = dataSource.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {

        ps.setString(1, batchNumber);

        int rowsUpdated = ps.executeUpdate();

        return rowsUpdated > 0;

    } catch (Exception e) {
        e.printStackTrace();
    }

    return false;
}


}
