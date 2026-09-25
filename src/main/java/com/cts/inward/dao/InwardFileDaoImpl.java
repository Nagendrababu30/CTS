package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.enums.FileType;
import com.cts.inward.model.InwardFile;

public class InwardFileDaoImpl
        implements InwardFileDao {

    private final DataSource dataSource;

    private InwardFileDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static InwardFileDaoImpl of() {
        return new InwardFileDaoImpl(ConnectionPool.getDataSource());
    }
    
    @Override
    public List<InwardFile> getChiFiles() {

        String sql =
                "SELECT file_id, file_name, file_path, file_type "
                + "FROM inward_file "
                + "WHERE status = 'PENDING'";

        List<InwardFile> files = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {

                FileType fileType =
                        FileType.valueOf(
                                resultSet.getString("file_type").toUpperCase());

                files.add(
                        InwardFile.of(
                                resultSet.getLong("file_id"),
                                resultSet.getString("file_name"),
                                resultSet.getString("file_path"),
                                fileType));
            }

            return files;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to retrieve CHI files from inward_file", e);
        }
    }

    @Override
    public void markAsProcessed(long fileId) {

        String sql =
                "UPDATE inward_file "
                + "SET status = 'PROCESSED' "
                + "WHERE file_id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, fileId);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to mark file as processed, fileId: " + fileId, e);
        }
    }

    @Override
    public long getFileIdByPath(String filePath) {

        String sql =
                "SELECT file_id FROM inward_file WHERE file_path = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, filePath);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("file_id");
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to get file_id for path: " + filePath, e);
        }

        return -1L;
    }

    @Override
    public long getFileIdByFileName(String fileName) {

        String sql =
                "SELECT file_id FROM inward_file WHERE file_name = ? ORDER BY file_id DESC LIMIT 1";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, fileName);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("file_id");
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to get file_id for file_name: " + fileName, e);
        }

        return -1L;
    }
}
