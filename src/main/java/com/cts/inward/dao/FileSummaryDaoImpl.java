package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.sql.DataSource;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.enums.FileStage;

public class FileSummaryDaoImpl
        implements FileSummaryDao {

    private final DataSource dataSource;

    private FileSummaryDaoImpl(
            DataSource dataSource) {

        this.dataSource =
                dataSource;
    }

    public static FileSummaryDaoImpl of() {

        return new FileSummaryDaoImpl(
                ConnectionPool.getDataSource());
    }

    @Override
    public void updateFileStage(
            long fileId,
            FileStage fileStage) {

        String sql =
                "UPDATE file_summary " +
                "SET filestage = ? " +
                "WHERE fileid = ?";

        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    fileStage.name());

            statement.setLong(
                    2,
                    fileId);

            statement.executeUpdate();

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to update file stage for fileId: "
                            + fileId,
                    e);
        }
    }
}