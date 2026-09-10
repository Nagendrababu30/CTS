package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
        return new OcrBatchDaoImpl(ConnectionPool.getDataSource());
    }

    /*
     * Inserts the OCR batch and returns the generated ocr_batch_id
     * using RETURNING — atomic, no race condition possible.
     */
    @Override
    public long saveBatch(OcrBatchData batchData) {

        String sql =
                "INSERT INTO ocr_batch "
                + "(batch_id, file_id, presenting_bank_name, total_cheques) "
                + "VALUES (?, ?, ?, ?) "
                + "RETURNING ocr_batch_id";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, batchData.getBatchId());
            statement.setLong(2, batchData.getFileId());
            statement.setString(3, batchData.getPresentingBankName());
            statement.setInt(4, batchData.getTotalCheque());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("ocr_batch_id");
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to save OCR batch: " + batchData.getBatchId(), e);
        }

        throw new IllegalStateException(
                "No ocr_batch_id returned after INSERT for batch: "
                + batchData.getBatchId());
    }
}
