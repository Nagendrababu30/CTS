package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.model.MicrMaster;

public class MicrMasterDaoImpl
        implements MicrMasterDao {

    private final DataSource dataSource;


    private MicrMasterDaoImpl(
            DataSource dataSource) {

        this.dataSource = dataSource;
    }


    public static MicrMasterDao of() {

        return new MicrMasterDaoImpl(
                ConnectionPool.getDataSource());
    }


    @Override
    public boolean exists(
            String micrCode) {

        String sql =
                """
                SELECT 1
                FROM public.micr_master
                WHERE micr_code = ?
                """;


        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    micrCode);


            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                return resultSet.next();
            }

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Failed to check MICR master for: "
                            + micrCode,
                    e);
        }
    }


    @Override
    public MicrMaster findByMicrCode(
            String micrCode) {

        String sql =
                """
                SELECT
                    micr_code,
                    bank_name,
                    branch_name,
                    city_code,
                    bank_code,
                    branch_code,
                    city_name
                FROM public.micr_master
                WHERE micr_code = ?
                """;


        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    micrCode);


            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (!resultSet.next()) {
                    return null;
                }


                return MicrMaster.of(
                        resultSet.getString(
                                "micr_code"),

                        resultSet.getString(
                                "bank_name"),

                        resultSet.getString(
                                "branch_name"),

                        resultSet.getString(
                                "city_code"),

                        resultSet.getString(
                                "bank_code"),

                        resultSet.getString(
                                "branch_code"),

                        resultSet.getString(
                                "city_name"));
            }

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Failed to retrieve MICR master for: "
                            + micrCode,
                    e);
        }
    }
}