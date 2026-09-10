package com.iispl.cts.model.outward;

import java.io.Serializable;
import java.time.LocalDateTime;

public class UserSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private int userId;
    private String username;
    private String password;
    private int roleId;
    private String status;
    private LocalDateTime lastLogin;

    public UserSession() {
    }

    public UserSession(
            int userId,
            String username,
            String password,
            int roleId,
            String status,
            LocalDateTime lastLogin) {

        this.userId = userId;
        this.username = username;
        this.password = password;
        this.roleId = roleId;
        this.status = status;
        this.lastLogin = lastLogin;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    @Override
    public String toString() {
        return "UserSession{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", roleId=" + roleId +
                ", status='" + status + '\'' +
                ", lastLogin=" + lastLogin +
                '}';
    }
}