package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.model.OcrChequeData;

public class OcrChequeDaoImpl implements OcrChequeDao {

    private final DataSource dataSource;

    private OcrChequeDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static OcrChequeDao of() {
        return new OcrChequeDaoImpl(ConnectionPool.getDataSource());
    }

    @Override
    public void saveCheque(OcrChequeData chequeData) {

        String sql =
                "INSERT INTO ocr_cheque_data "
                + "(ocr_batch_id, cheque_number, account_number, "
                + "amount, micr_code, cheque_date, presenting_date, "
                + "branch_code, city_code, bank_code, "
                + "drawer_name, payee_name, payee_account_number) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, chequeData.getBatchId());
            statement.setString(2, chequeData.getChequeNumber());
            statement.setString(3, chequeData.getAccountNumber());
            statement.setBigDecimal(4, chequeData.getChequeAmount());
            statement.setString(5, chequeData.getMicrCode());
            statement.setObject(6, chequeData.getChequeDate());
            statement.setObject(7, chequeData.getPresentingDate());
            statement.setString(8, chequeData.getBranchSpecificCode());
            statement.setString(9, chequeData.getCityCode());
            statement.setString(10, chequeData.getBankCode());
            statement.setString(11, chequeData.getDrawerName());
            statement.setString(12, chequeData.getPayeeName());
            statement.setString(13, chequeData.getPayeeAccountNumber());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to save OCR cheque: " + chequeData.getChequeNumber(), e);
        }
    }
}
