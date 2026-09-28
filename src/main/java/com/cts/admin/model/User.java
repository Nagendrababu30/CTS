package com.cts.admin.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class User implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long userId;
	private String username;

	private String password;

	private String passwordHash;

	private Role role;

	private String status;

	private Timestamp lastLogin;

	public User() {
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
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

	public String getPasswordHash() {
		return passwordHash != null ? passwordHash : password;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
		this.password = passwordHash;
	}

	public Long getRoleId() {
		return role != null ? role.getRoleId() : null;
	}

	public String getRoleName() {
		return role != null ? role.getRoleName() : null;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Timestamp getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(Timestamp lastLogin) {
		this.lastLogin = lastLogin;
	}

	@Override
	public String toString() {
		return "User [userId=" + userId + ", username=" + username + ", roleId=" + getRoleId() + ", roleName="
				+ getRoleName() + ", status=" + status + "]";
	}
}
