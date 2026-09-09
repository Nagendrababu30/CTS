package com.cts.admin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.cts.admin.model.AuditLog;
import com.cts.inward.config.ConnectionPool;

public class AuditLogDAOImpl implements AuditLogDAO {

    private static final java.util.TimeZone IST =
            java.util.TimeZone.getTimeZone("Asia/Kolkata");

    /* ================================================================
     * GET AUDIT LOGS — paginated from user_session
     * ================================================================ */

    @Override
    public List<AuditLog> getAuditLogs(int page, int pageSize) {

        List<AuditLog> logs = new ArrayList<>();
        int offset = (page - 1) * pageSize;

        String sql =
                "SELECT us.session_id, "
                + "       us.user_id, "
                + "       u.username, "
                + "       r.role_name, "
                + "       us.login_time, "
                + "       us.logout_time "
                + "FROM   user_session us "
                + "JOIN   \"user\" u  ON us.user_id  = u.user_id "
                + "JOIN   \"role\" r  ON u.role_id   = r.role_id "
                + "ORDER  BY us.login_time DESC "
                + "LIMIT  ? OFFSET ?";

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, pageSize);
            stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AuditLog log = new AuditLog();
                    log.setUserId(rs.getLong("user_id"));
                    log.setUserName(rs.getString("username"));
                    log.setRoleName(rs.getString("role_name"));
                    log.setLoginTime(rs.getTimestamp("login_time"));
                    log.setLogoutTime(rs.getTimestamp("logout_time"));
                    logs.add(log);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch audit logs.", e);
        }

        return logs;
    }

    /* ================================================================
     * COUNT
     * ================================================================ */

    @Override
    public int getTotalAuditLogCount() {

        String sql = "SELECT COUNT(*) FROM user_session";

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            if (rs.next()) return rs.getInt(1);

        } catch (SQLException e) {
            throw new RuntimeException("Unable to count audit logs.", e);
        }

        return 0;
    }

    /* ================================================================
     * CREATE AUDIT LOG — insert into user_session on login
     * Returns the generated session_id (UUID)
     * ================================================================ */

    @Override
    public String createAuditLog(Long userId) {

        String sessionId = UUID.randomUUID().toString();

        java.sql.Timestamp nowIST = new java.sql.Timestamp(
                java.util.Calendar.getInstance(IST).getTimeInMillis());

        String sql =
                "INSERT INTO user_session "
                + "(session_id, user_id, login_time, session_status) "
                + "VALUES (?, ?, ?, 'ACTIVE')";

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, sessionId);
            stmt.setLong(2, userId);
            stmt.setTimestamp(3, nowIST);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Unable to create audit log entry.", e);
        }

        return sessionId;
    }

    /* ================================================================
     * END AUDIT LOG — update logout_time in user_session on logout
     * ================================================================ */

    @Override
    public void endAuditLog(String sessionId) {

        java.sql.Timestamp nowIST = new java.sql.Timestamp(
                java.util.Calendar.getInstance(IST).getTimeInMillis());

        String sql =
                "UPDATE user_session "
                + "SET logout_time = ?, session_status = 'ENDED' "
                + "WHERE session_id = ?";

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setTimestamp(1, nowIST);
            stmt.setString(2, sessionId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Unable to end audit log entry.", e);
        }
    }
}
