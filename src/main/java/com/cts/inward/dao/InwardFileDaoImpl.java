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

    private InwardFileDaoImpl(
            DataSource dataSource) {

        this.dataSource =
                dataSource;
    }

    public static InwardFileDaoImpl of() {

        return new InwardFileDaoImpl(
                ConnectionPool.getDataSource());
    }

    @Override
    public List<InwardFile> getChiFiles() {

        String sql =
                "SELECT fileid, filename, filepath, filetype "
                + "FROM inward_file";

        List<InwardFile> files =
                new ArrayList<>();

        try (Connection connection =
                     dataSource.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet =
                     statement.executeQuery()) {

            while (resultSet.next()) {

                FileType fileType =
                        FileType.valueOf(
                                resultSet
                                        .getString(
                                                "filetype")
                                        .toUpperCase());

                files.add(
                        InwardFile.of(
                                resultSet.getLong(
                                        "fileid"),
                                resultSet.getString(
                                        "filename"),
                                resultSet.getString(
                                        "filepath"),
                                fileType));
            }

            return files;

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Failed to retrieve CHI files "
                    + "from INWARD_FILE",
                    e);
        }
    }
}