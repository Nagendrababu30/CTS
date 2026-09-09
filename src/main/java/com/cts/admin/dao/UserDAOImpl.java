package com.cts.admin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.cts.admin.model.Role;
import com.cts.admin.model.User;
import com.cts.inward.config.ConnectionPool;

public class UserDAOImpl {


    public List<User> getUsers(int limit, int offset,
            String searchText, Long roleId, String status) {

        List<User> users = new ArrayList<>();

        StringBuilder sql = new StringBuilder();

        /* CTS DB uses reserved words "user" and "role" — must be quoted */
        sql.append(
                "SELECT u.user_id, "
                + "       u.username, "
                + "       u.role_id, "
                + "       r.role_name, "
                + "       r.description AS role_description, "
                + "       r.status      AS role_status, "
                + "       u.status, "
                + "       u.last_login "
                + "FROM   \"user\" u "
                + "LEFT   JOIN \"role\" r ON u.role_id = r.role_id "
                + "WHERE  1=1 ");

        List<Object> parameters = new ArrayList<>();

        if (searchText != null && !searchText.trim().isEmpty()) {
            sql.append("AND (LOWER(u.username) LIKE ? "
                    + "OR CAST(u.user_id AS TEXT) LIKE ?) ");
            String v = "%" + searchText.trim().toLowerCase() + "%";
            parameters.add(v);
            parameters.add(v);
        }

        if (roleId != null) {
            sql.append("AND u.role_id = ? ");
            parameters.add(roleId);
        }

        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND u.status = ? ");
            parameters.add(status);
        }

        sql.append("ORDER BY u.user_id LIMIT ? OFFSET ?");
        parameters.add(limit);
        parameters.add(offset);

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql.toString())
        ) {
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch users.", e);
        }

        return users;
    }


    public int getUserCount() {

        String sql = "SELECT COUNT(*) FROM \"user\"";

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to count users.", e);
        }

        return 0;
    }


    public User getUserById(Long userId) {

        String sql =
                "SELECT u.user_id, "
                + "       u.username, "
                + "       u.password, "
                + "       u.role_id, "
                + "       r.role_name, "
                + "       r.description AS role_description, "
                + "       r.status      AS role_status, "
                + "       u.status, "
                + "       u.last_login "
                + "FROM   \"user\" u "
                + "LEFT   JOIN \"role\" r ON u.role_id = r.role_id "
                + "WHERE  u.user_id = ?";

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Unable to fetch user.", e);
        }

        return null;
    }


    public boolean usernameExists(String username) {

        String sql = "SELECT COUNT(*) FROM \"user\" WHERE username = ?";

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Unable to check username.", e);
        }

        return false;
    }


    public boolean createUser(User user) {

        /* CTS "user" table columns: user_id, username, password, role_id, status, last_login */
        String sql =
                "INSERT INTO \"user\" "
                + "(username, password, role_id, status) "
                + "VALUES (?, ?, ?, ?)";

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setLong(3, user.getRole().getRoleId());
            stmt.setString(4, user.getStatus());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Unable to create user.", e);
        }
    }


    public boolean updateUser(User user) {

        String sql =
                "UPDATE \"user\" "
                + "SET role_id = ? "
                + "WHERE user_id = ?";

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setLong(1, user.getRole().getRoleId());
            stmt.setLong(2, user.getUserId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Unable to update user.", e);
        }
    }


    public boolean updateUserStatus(Long userId, String status) {

        String sql = "UPDATE \"user\" SET status = ? WHERE user_id = ?";

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, status);
            stmt.setLong(2, userId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Unable to update user status.", e);
        }
    }


    public void deleteUser(Long userId) {

        String sql = "DELETE FROM \"user\" WHERE user_id = ?";

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Unable to delete user.", e);
        }
    }

    /* ------------------------------------------------------------------ */
    /* MAPPER                                                               */
    /* ------------------------------------------------------------------ */

    private User mapUser(ResultSet rs) throws SQLException {

        User user = new User();

        user.setUserId(rs.getLong("user_id"));
        user.setUsername(rs.getString("username"));
        user.setStatus(rs.getString("status"));

        /* password column — only present in getUserById query */
        try {
            String pwd = rs.getString("password");
            if (pwd != null) {
                user.setPasswordHash(pwd);
            }
        } catch (SQLException ignored) {}

        /* last_login */
        try {
            user.setLastLoginAt(rs.getTimestamp("last_login"));
        } catch (SQLException ignored) {}

        /* Role */
        long roleIdValue = rs.getLong("role_id");
        if (!rs.wasNull()) {
            Role role = new Role();
            role.setRoleId(roleIdValue);
            role.setRoleName(rs.getString("role_name"));
            role.setDescription(rs.getString("role_description"));
            role.setStatus(rs.getString("role_status"));
            user.setRole(role);
        }

        return user;
    }
}
