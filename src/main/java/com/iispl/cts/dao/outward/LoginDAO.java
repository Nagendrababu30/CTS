package com.iispl.cts.dao.outward;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.iispl.cts.data.CTSStaticData;
import com.iispl.cts.model.outward.UserSession;

public class LoginDAO {
	public boolean updateLastLogin(int userId) {

	    String sql =
	            "UPDATE public.\"user\" " +
	            "SET last_login = CURRENT_TIMESTAMP " +
	            "WHERE user_id = ?";

	    try (Connection connection = CTSStaticData.getConnection();
	         PreparedStatement ps = connection.prepareStatement(sql)) {

	        ps.setInt(1, userId);

	        return ps.executeUpdate() > 0;

	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	    }
	}

    public UserSession authenticate(
            String username,
            String password) throws Exception {

        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        if (password == null || password.trim().isEmpty()) {
            return null;
        }

        String sql =
                "SELECT " +
                "user_id, " +
                "username, " +
                "password, " +
                "role_id, " +
                "status, " +
                "last_login " +
                "FROM public.\"user\" " +
                "WHERE username = ? " +
                "AND password = ? " +
                "AND status = 'ACTIVE'";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ps.setString(1, username.trim());
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                int userId =
                        rs.getInt("user_id");

                String dbUsername =
                        rs.getString("username");

                String dbPassword =
                        rs.getString("password");

                int roleId =
                        rs.getInt("role_id");

                String status =
                        rs.getString("status");

                Timestamp timestamp =
                        rs.getTimestamp("last_login");

                LocalDateTime lastLogin = null;

                if (timestamp != null) {
                    lastLogin =
                            timestamp.toLocalDateTime();
                }

                return new UserSession(
                        userId,
                        dbUsername,
                        dbPassword,
                        roleId,
                        status,
                        lastLogin
                );
            }
        }
    }
}