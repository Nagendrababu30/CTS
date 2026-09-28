package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.enums.FileStage;

public class FileSummaryDaoImpl
        implements FileSummaryDao {

    private final DataSource dataSource;

    private FileSummaryDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static FileSummaryDaoImpl of() {
        return new FileSummaryDaoImpl(ConnectionPool.getDataSource());
    }

    // Called once when a file is first moved to incoming/.
    // Creates or updates the tracking row with file_stage = INCOMING.
    @Override
    public void insertFileSummary(long fileId, String fileName) {

        String checkSql = "SELECT 1 FROM inward_file_summary WHERE file_id = ?";
        String insertSql =
                "INSERT INTO inward_file_summary "
                + "(file_id, file_name, file_stage) "
                + "VALUES (?, ?, 'INCOMING')";
        String updateSql =
                "UPDATE inward_file_summary "
                + "SET file_stage = 'INCOMING', file_name = ? "
                + "WHERE file_id = ?";

        try (Connection connection = dataSource.getConnection()) {

            boolean exists = false;
            try (PreparedStatement checkStatement = connection.prepareStatement(checkSql)) {
                checkStatement.setLong(1, fileId);
                try (ResultSet resultSet = checkStatement.executeQuery()) {
                    exists = resultSet.next();
                }
            }

            if (exists) {
                try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                    updateStatement.setString(1, fileName);
                    updateStatement.setLong(2, fileId);
                    updateStatement.executeUpdate();
                }
            } else {
                try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                    insertStatement.setLong(1, fileId);
                    insertStatement.setString(2, fileName);
                    insertStatement.executeUpdate();
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to save inward_file_summary for fileId: "
                    + fileId, e);
        }
    }

    /*
     * Called when file moves to PROCESSING or ARCHIVE.
     * Updates the existing row's file_stage.
     */
    @Override
    public void updateFileStage(long fileId, FileStage fileStage) {

        String sql =
                "UPDATE inward_file_summary "
                + "SET file_stage = ? "
                + "WHERE file_id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, fileStage.name());
            statement.setLong(2, fileId);
            statement.executeUpdate();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to update file stage for fileId: " + fileId, e);
        }
    }
}
