package com.cts.admin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.cts.admin.model.AuditLog;
import com.cts.inward.config.ConnectionPool;

public class AuditLogDAOImpl implements AuditLogDAO {

    /* ================================================================
     * GET AUDIT LOGS — paginated
     *
     * Source: user_session joined with "user" and "role"
     * Columns: user_id | role_name | login_time | logout_time
     *
     * We use user_session as the primary source since the ZUL page
     * displays: User ID | Role | Login | Logout
     * system_audit_log only has user_id and action — not enough for
     * the 4-column display required.
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
}
