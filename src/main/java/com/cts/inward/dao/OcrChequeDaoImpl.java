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
        return new OcrChequeDaoImpl(
                ConnectionPool.getDataSource());
    }

    @Override
    public void saveCheque(OcrChequeData chequeData) {

        String sql =
                "INSERT INTO inward_ocr " +
                "(batchid, chequenumber, accountnumber, drawername, " +
                "amount, micrcode, chequedate, branchcode, citycode, bankcode) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setLong(
                    1,
                    chequeData.getBatchId());

            statement.setString(
                    2,
                    chequeData.getChequeNumber());

            statement.setString(
                    3,
                    chequeData.getAccountNumber());

            statement.setString(
                    4,
                    chequeData.getDrawerName());

            statement.setBigDecimal(
                    5,
                    chequeData.getChequeAmount());

            statement.setString(
                    6,
                    chequeData.getMicrCode());

            statement.setObject(
                    7,
                    chequeData.getChequeDate());

            statement.setString(
                    8,
                    chequeData.getBranchSpecificCode());

            statement.setString(
                    9,
                    chequeData.getCityCode());

            statement.setString(
                    10,
                    chequeData.getBankCode());

            statement.executeUpdate();

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Failed to save OCR cheque: "
                            + chequeData.getChequeNumber(),
                    e);
        }
    }
}