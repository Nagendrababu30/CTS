package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.model.OcrBatchData;

public class OcrBatchDaoImpl implements OcrBatchDao {

    private final DataSource dataSource;

    private OcrBatchDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static OcrBatchDao of() {
        return new OcrBatchDaoImpl(
                ConnectionPool.getDataSource());
    }

    @Override
    public void saveBatch(OcrBatchData batchData) {

        String sql =
                "INSERT INTO inward_ocr_batch " +
                "(batchid, fileid, presentingbankname, totalcheques) " +
                "VALUES (?, ?, ?, ?)";

        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchData.getBatchId());

            statement.setString(
                    2,
                    batchData.getFileId());

            statement.setString(
                    3,
                    batchData.getPresentingBankName());

            statement.setInt(
                    4,
                    batchData.getTotalCheque());

            statement.executeUpdate();

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Failed to save OCR batch: "
                            + batchData.getBatchId(),
                    e);
        }
    }
}