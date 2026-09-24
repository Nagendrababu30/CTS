package com.cts.admin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import com.cts.admin.model.Role;
import com.cts.admin.model.User;
import com.cts.admin.util.PasswordUtil;
import com.cts.inward.config.ConnectionPool;


public class UserDao {

    // LEGACY AUTH — used by LoginComposer via UserServiceImpl

	public User authenticate(String username, String password) {

	    String sql =
	            "SELECT u.user_id, u.username, u.password, "
	            + "u.role_id, r.role_name, r.description, r.status as role_status, u.status, u.last_login "
	            + "FROM \"user\" u "
	            + "JOIN \"role\" r ON u.role_id = r.role_id "
	            + "WHERE u.username = ? "
	            + "AND u.status = 'ACTIVE' "
	            + "AND r.status = 'ACTIVE'";

	    try (
	            Connection conn = ConnectionPool.getDataSource().getConnection();
	            PreparedStatement stmt = conn.prepareStatement(sql)
	    ) {
	        stmt.setString(1, username);

	        System.out.println("AUTH ATTEMPT: username=" + username);

	        try (ResultSet rs = stmt.executeQuery()) {

	            if (rs.next()) {

	                String storedHash = rs.getString("password");

	                if (!PasswordUtil.verifyPassword(password, storedHash)) {
	                    System.out.println("AUTH FAILED: invalid password");
	                    return null;
	                }

	                User user = new User();

	                user.setUserId(rs.getLong("user_id"));
	                user.setUsername(rs.getString("username"));
	                user.setPasswordHash(storedHash);
	                user.setStatus(rs.getString("status"));
	                user.setLastLogin(rs.getTimestamp("last_login"));

	                // Create and set Role object
	                Role role = new Role();
	                role.setRoleId(rs.getLong("role_id"));
	                role.setRoleName(rs.getString("role_name"));
	                role.setDescription(rs.getString("description"));
	                role.setStatus(rs.getString("role_status"));
	                user.setRole(role);

	                System.out.println(
	                        "AUTH SUCCESS: userId=" + rs.getLong("user_id")
	                );

	                return user;

	            } else {

	                System.out.println(
	                        "AUTH FAILED: user not found or inactive"
	                );
	            }
	        }

	    } catch (Exception e) {

	        System.out.println("AUTH ERROR: " + e.getMessage());
	        e.printStackTrace();
	    }

	    return null;
	}

    // CRUD METHODS

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
