package com.cts.admin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import com.cts.admin.model.User;
import com.cts.inward.config.ConnectionPool;

/**
 * UserDao — handles both legacy authentication and CRUD interface.
 */
public class UserDao {

    // ================================================================
    // LEGACY AUTH — used by LoginComposer via UserServiceImpl
    // ================================================================

    public User authenticate(String username, String password) {

        String sql =
                "SELECT u.user_id, u.username, u.password, "
                + "u.role_id, r.role_name, u.status, u.last_login "
                + "FROM \"user\" u "
                + "JOIN \"role\" r ON u.role_id = r.role_id "
                + "WHERE u.username = ? "
                + "AND u.password = ? "
                + "AND u.status = 'ACTIVE' "
                + "AND r.status = 'ACTIVE'";

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, username);
            stmt.setString(2, password);

            System.out.println("AUTH ATTEMPT: username=" + username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("AUTH SUCCESS: userId=" + rs.getLong("user_id"));
                    User user = new User();
                    user.setUserId(rs.getLong("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setRoleId(rs.getLong("role_id"));
                    user.setRoleName(rs.getString("role_name"));
                    user.setStatus(rs.getString("status"));
                    user.setLastLogin(rs.getTimestamp("last_login"));
                    return user;
                } else {
                    System.out.println("AUTH FAILED: no matching row for username=" + username);
                }
            }
        } catch (Exception e) {
            System.out.println("AUTH ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // ================================================================
    // CRUD METHODS
    // ================================================================

    public List<User> getUsers(int limit, int offset,
            String searchText, Long roleId, String status) {
        return new UserDAOImpl().getUsers(limit, offset, searchText, roleId, status);
    }

    public int getUserCount() {
        return new UserDAOImpl().getUserCount();
    }

    public User getUserById(Long userId) {
        return new UserDAOImpl().getUserById(userId);
    }

    public boolean usernameExists(String username) {
        return new UserDAOImpl().usernameExists(username);
    }

    public boolean createUser(User user) {
        return new UserDAOImpl().createUser(user);
    }

    public boolean updateUser(User user) {
        return new UserDAOImpl().updateUser(user);
    }

    public boolean updateUserStatus(Long userId, String status) {
        return new UserDAOImpl().updateUserStatus(userId, status);
    }

    public void deleteUser(Long userId) {
        new UserDAOImpl().deleteUser(userId);
    }
}
