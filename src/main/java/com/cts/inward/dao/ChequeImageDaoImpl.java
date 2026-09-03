package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.model.ChequeImage;

public class ChequeImageDaoImpl
        implements ChequeImageDao {

    private final DataSource dataSource;

    private ChequeImageDaoImpl(
            DataSource dataSource) {

        this.dataSource =
                dataSource;
    }

    public static ChequeImageDaoImpl of() {

        return new ChequeImageDaoImpl(
                ConnectionPool.getDataSource());
    }

    @Override
    public void save(
            ChequeImage chequeImage) {

        String sql =
                "INSERT INTO cheque_image "
                + "(chequenumber, frontpath, backpath) "
                + "VALUES (?, ?, ?)";

        try (Connection connection =
                     dataSource.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    chequeImage.getChequeNumber());

            statement.setString(
                    2,
                    chequeImage.getFrontPath());

            statement.setString(
                    3,
                    chequeImage.getBackPath());

            statement.executeUpdate();

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Failed to save cheque image paths",
                    e);
        }
    }

    @Override
    public ChequeImage findByChequeNumber(
            String chequeNumber) {

        String sql =
                "SELECT imageid, chequenumber, "
                + "frontpath, backpath "
                + "FROM cheque_image "
                + "WHERE chequenumber = ?";

        try (Connection connection =
                     dataSource.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    chequeNumber);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                return ChequeImage.of(
                        resultSet.getLong(
                                "imageid"),
                        resultSet.getString(
                                "chequenumber"),
                        resultSet.getString(
                                "frontpath"),
                        resultSet.getString(
                                "backpath"));
            }

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Failed to retrieve image paths",
                    e);
        }
    }
}