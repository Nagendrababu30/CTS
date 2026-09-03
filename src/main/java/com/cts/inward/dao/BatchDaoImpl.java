package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.sql.DataSource;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.model.NpciBatchData;

public class BatchDaoImpl implements BatchDao {

    private final DataSource dataSource;

    private BatchDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static BatchDao of() {
        return new BatchDaoImpl(
                ConnectionPool.getDataSource());
    }

    @Override
    public void saveBatch(NpciBatchData batchData) {

        String sql =
                "INSERT INTO batch " +
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

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to save batch: "
                            + batchData.getBatchId(),
                    e);
        }
    }
}