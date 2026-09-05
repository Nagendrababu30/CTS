package com.cts.admin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.cts.admin.model.User;
import com.cts.inward.config.ConnectionPool;

public class UserDao {

    public User authenticate(
            String username,
            String password) {

        String sql =
                "SELECT "
                + "u.user_id, "
                + "u.username, "
                + "u.password, "
                + "u.role_id, "
                + "r.role_name, "
                + "u.status, "
                + "u.last_login "
                + "FROM \"user\" u "
                + "JOIN \"role\" r "
                + "ON u.role_id = r.role_id "
                + "WHERE u.username = ? "
                + "AND u.password = ? "
                + "AND u.status = 'ACTIVE' "
                + "AND r.status = 'ACTIVE'";

        try (
            Connection connection =
                    ConnectionPool.getDataSource()
                            .getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {

            statement.setString(1, username);
            statement.setString(2, password);

            try (ResultSet resultSet =
                    statement.executeQuery()) {

                if (resultSet.next()) {

                    User user = new User();

                    user.setUserId(
                            resultSet.getLong("user_id"));

                    user.setUsername(
                            resultSet.getString("username"));

                    user.setPassword(
                            resultSet.getString("password"));

                    user.setRoleId(
                            resultSet.getLong("role_id"));

                    user.setRoleName(
                            resultSet.getString("role_name"));

                    user.setStatus(
                            resultSet.getString("status"));

                    user.setLastLogin(
                            resultSet.getTimestamp("last_login"));

                    return user;
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return null;
    }
}