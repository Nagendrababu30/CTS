package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import com.cts.inward.config.ConnectionPool;

// Implementation of MicrMasterDao for PostgreSQL MICR directory queries
public class MicrMasterDaoImpl implements MicrMasterDao {

    private final DataSource dataSource;

    private MicrMasterDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static MicrMasterDao of() {
        return new MicrMasterDaoImpl(ConnectionPool.getDataSource());
    }

    public static MicrMasterDao of(DataSource dataSource) {
        return new MicrMasterDaoImpl(dataSource);
    }

    // Checks if a 9-digit MICR code exists in micr_master
    @Override
    public boolean exists(String micrCode) {

        String sql = "SELECT 1 FROM public.micr_master WHERE micr_code = ? LIMIT 1";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, micrCode);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to verify MICR code: " + micrCode, e);
        }
    }

    // Bulk checks existing MICR codes from a collection of candidate codes
    @Override
    public Set<String> findExistingMicrCodes(Set<String> micrCodes) {

        Set<String> existing = new HashSet<>();
        if (micrCodes == null || micrCodes.isEmpty()) {
            return existing;
        }

        StringBuilder sql = new StringBuilder("SELECT micr_code FROM public.micr_master WHERE micr_code IN (");
        boolean first = true;
        for (String code : micrCodes) {
            if (code != null && !code.trim().isEmpty()) {
                if (!first) {
                    sql.append(",");
                }
                sql.append("?");
                first = false;
            }
        }
        sql.append(")");

        if (first) {
            return existing;
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            int idx = 1;
            for (String code : micrCodes) {
                if (code != null && !code.trim().isEmpty()) {
                    statement.setString(idx++, code.trim());
                }
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String code = resultSet.getString("micr_code");
                    if (code != null) {
                        existing.add(code.trim());
                    }
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to check MICR master codes in bulk", e);
        }

        return existing;
    }
}