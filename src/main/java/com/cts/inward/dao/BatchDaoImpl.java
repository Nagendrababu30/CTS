package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

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

        String sql = """
                INSERT INTO inward_batch
                (
                    batch_id,
                    file_id,
                    presenting_bank_name,
                    total_cheques
                )
                VALUES (?, ?, ?, ?)
                """;

        try (
            Connection connection = ConnectionPool.getDataSource().getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, batchData.getBatchId());
            statement.setLong(2, batchData.getFileId());
            statement.setString(3, batchData.getPresentingBankName());
            statement.setInt(4, batchData.getTotalCheques());

            statement.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Error saving batch", e);
        }
    }

    @Override
    public List<NpciBatchData> getAllBatches() {

        String sql = """
                SELECT
                    batch_id,
                    file_id,
                    presenting_bank_name,
                    total_cheques
                FROM inward_batch
                ORDER BY batch_id
                """;

        List<NpciBatchData> batches = new ArrayList<>();

        try (
            Connection connection = ConnectionPool.getDataSource().getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()
        ) {

            while (resultSet.next()) {

                NpciBatchData batch = new NpciBatchData(
                        resultSet.getLong("batch_id"),
                        resultSet.getLong("file_id"),
                        resultSet.getString("presenting_bank_name"),
                        resultSet.getInt("total_cheques")
                );

                batches.add(batch);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error retrieving batches", e);
        }

        return batches;
    }
}