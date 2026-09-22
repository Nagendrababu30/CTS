
package com.cts.admin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.cts.admin.model.AuditLog;
import com.cts.inward.config.ConnectionPool;

public class AuditLogDAOImpl implements AuditLogDAO {

    private static final java.util.TimeZone IST =
            java.util.TimeZone.getTimeZone("Asia/Kolkata");

    // ================================================================
    // GET AUDIT LOGS - WITHOUT FILTERS
    // ================================================================

    @Override
    public List<AuditLog> getAuditLogs(int page, int pageSize) {

        return getAuditLogs(
                page,
                pageSize,
                null,
                null,
                null,
                null);
    }

    // ================================================================
    // GET FILTERED AUDIT LOGS
    // ================================================================

    @Override
    public List<AuditLog> getAuditLogs(
            int page,
            int pageSize,
            String searchText,
            String roleName,
            Date fromDate,
            Date toDate) {

        List<AuditLog> logs = new ArrayList<>();

        int offset = (page - 1) * pageSize;

        StringBuilder sql = new StringBuilder();

        sql.append(
                "SELECT us.session_id, "
              + "       us.user_id, "
              + "       u.username, "
              + "       r.role_name, "
              + "       us.login_time, "
              + "       us.logout_time "
              + "FROM user_session us "
              + "JOIN \"user\" u ON us.user_id = u.user_id "
              + "JOIN \"role\" r ON u.role_id = r.role_id "
              + "WHERE 1 = 1 ");

        List<Object> parameters = new ArrayList<>();

        // ============================================================
        // SEARCH BY USERNAME OR USER ID
        // ============================================================

        if (searchText != null && !searchText.trim().isEmpty()) {

            sql.append(
                    "AND (LOWER(u.username) LIKE LOWER(?) "
                  + "OR CAST(u.user_id AS TEXT) LIKE ?) ");

            String searchPattern =
                    "%" + searchText.trim() + "%";

            parameters.add(searchPattern);
            parameters.add(searchPattern);
        }

        // ============================================================
        // ROLE FILTER
        // ============================================================

        if (roleName != null && !roleName.trim().isEmpty()) {

            sql.append("AND r.role_name = ? ");

            parameters.add(roleName.trim());
        }

        // ============================================================
        // FROM DATE
        // ============================================================

        if (fromDate != null) {

            sql.append("AND us.login_time >= ? ");

            parameters.add(
                    new java.sql.Timestamp(fromDate.getTime()));
        }

        // ============================================================
        // TO DATE - INCLUSIVE
        // ============================================================

        if (toDate != null) {

            java.util.Calendar calendar =
                    java.util.Calendar.getInstance(IST);

            calendar.setTime(toDate);

            // Include the complete selected To Date
            calendar.add(
                    java.util.Calendar.DAY_OF_MONTH,
                    1);

            sql.append("AND us.login_time < ? ");

            parameters.add(
                    new java.sql.Timestamp(
                            calendar.getTimeInMillis()));
        }

        // ============================================================
        // PAGINATION
        // ============================================================

        sql.append(
                "ORDER BY us.login_time DESC "
              + "LIMIT ? OFFSET ?");

        parameters.add(pageSize);
        parameters.add(offset);

        try (
                Connection conn =
                        ConnectionPool.getDataSource().getConnection();

                PreparedStatement stmt =
                        conn.prepareStatement(sql.toString())
        ) {

            for (int i = 0; i < parameters.size(); i++) {

                stmt.setObject(
                        i + 1,
                        parameters.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    AuditLog log = new AuditLog();

                    log.setUserId(
                            rs.getLong("user_id"));

                    log.setUserName(
                            rs.getString("username"));

                    log.setRoleName(
                            rs.getString("role_name"));

                    log.setLoginTime(
                            rs.getTimestamp("login_time"));

                    log.setLogoutTime(
                            rs.getTimestamp("logout_time"));

                    logs.add(log);
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to fetch filtered audit logs.",
                    e);
        }

        return logs;
    }

    // ================================================================
    // GET TOTAL AUDIT LOG COUNT - WITHOUT FILTERS
    // ================================================================

    @Override
    public int getTotalAuditLogCount() {

        return getTotalAuditLogCount(
                null,
                null,
                null,
                null);
    }

    // ================================================================
    // GET FILTERED AUDIT LOG COUNT
    // ================================================================

    @Override
    public int getTotalAuditLogCount(
            String searchText,
            String roleName,
            Date fromDate,
            Date toDate) {

        StringBuilder sql = new StringBuilder();

        sql.append(
                "SELECT COUNT(*) "
              + "FROM user_session us "
              + "JOIN \"user\" u ON us.user_id = u.user_id "
              + "JOIN \"role\" r ON u.role_id = r.role_id "
              + "WHERE 1 = 1 ");

        List<Object> parameters = new ArrayList<>();

        // ============================================================
        // SEARCH BY USERNAME OR USER ID
        // ============================================================

        if (searchText != null && !searchText.trim().isEmpty()) {

            sql.append(
                    "AND (LOWER(u.username) LIKE LOWER(?) "
                  + "OR CAST(u.user_id AS TEXT) LIKE ?) ");

            String searchPattern =
                    "%" + searchText.trim() + "%";

            parameters.add(searchPattern);
            parameters.add(searchPattern);
        }

        // ============================================================
        // ROLE FILTER
        // ============================================================

        if (roleName != null && !roleName.trim().isEmpty()) {

            sql.append("AND r.role_name = ? ");

            parameters.add(roleName.trim());
        }

        // ============================================================
        // FROM DATE
        // ============================================================

        if (fromDate != null) {

            sql.append("AND us.login_time >= ? ");

            parameters.add(
                    new java.sql.Timestamp(fromDate.getTime()));
        }

        // ============================================================
        // TO DATE - INCLUSIVE
        // ============================================================

        if (toDate != null) {

            java.util.Calendar calendar =
                    java.util.Calendar.getInstance(IST);

            calendar.setTime(toDate);

            // Include the complete selected To Date
            calendar.add(
                    java.util.Calendar.DAY_OF_MONTH,
                    1);

            sql.append("AND us.login_time < ? ");

            parameters.add(
                    new java.sql.Timestamp(
                            calendar.getTimeInMillis()));
        }

        try (
                Connection conn =
                        ConnectionPool.getDataSource().getConnection();

                PreparedStatement stmt =
                        conn.prepareStatement(sql.toString())
        ) {

            for (int i = 0; i < parameters.size(); i++) {

                stmt.setObject(
                        i + 1,
                        parameters.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to count filtered audit logs.",
                    e);
        }

        return 0;
    }

    // ================================================================
    // CREATE AUDIT LOG - INSERT ON LOGIN
    // ================================================================

    @Override
    public String createAuditLog(Long userId) {

        String sessionId =
                UUID.randomUUID().toString();

        java.sql.Timestamp nowIST =
                new java.sql.Timestamp(
                        java.util.Calendar
                                .getInstance(IST)
                                .getTimeInMillis());

        String sql =
                "INSERT INTO user_session "
              + "(session_id, user_id, login_time, session_status) "
              + "VALUES (?, ?, ?, 'ACTIVE')";

        try (
                Connection conn =
                        ConnectionPool.getDataSource().getConnection();

                PreparedStatement stmt =
                        conn.prepareStatement(sql)
        ) {

            stmt.setString(1, sessionId);
            stmt.setLong(2, userId);
            stmt.setTimestamp(3, nowIST);

            stmt.executeUpdate();

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to create audit log entry.",
                    e);
        }

        return sessionId;
    }

    // ================================================================
    // END AUDIT LOG - UPDATE ON LOGOUT
    // ================================================================

    @Override
    public void endAuditLog(String sessionId) {

        java.sql.Timestamp nowIST =
                new java.sql.Timestamp(
                        java.util.Calendar
                                .getInstance(IST)
                                .getTimeInMillis());

        String sql =
                "UPDATE user_session "
              + "SET logout_time = ?, "
              + "    session_status = 'ENDED' "
              + "WHERE session_id = ?";

        try (
                Connection conn =
                        ConnectionPool.getDataSource().getConnection();

                PreparedStatement stmt =
                        conn.prepareStatement(sql)
        ) {

            stmt.setTimestamp(1, nowIST);
            stmt.setString(2, sessionId);

            stmt.executeUpdate();

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to end audit log entry.",
                    e);
        }
    }
}