package com.cts.admin.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long      userId;
    private String    username;

    /* password — used by existing auth (UserDao.authenticate) */
    private String    password;

    /* passwordHash — used by new CRUD (UserDAOImpl) */
    private String    passwordHash;

    /* Legacy flat fields — used by LoginComposer / AuthorizationComposer */
    private Long      roleId;
    private String    roleName;

    /* Role object — used by UserController / UserDAOImpl */
    private Role      role;

    private String    status;

    /* Legacy lastLogin — used by existing auth */
    private Timestamp lastLogin;

    /* lastLoginAt — used by UserDAOImpl */
    private Timestamp lastLoginAt;

    public User() {}

    /* ------------------------------------------------------------------ */
    /* GETTERS / SETTERS                                                    */
    /* ------------------------------------------------------------------ */

    public Long getUserId()                         { return userId; }
    public void setUserId(Long userId)              { this.userId = userId; }

    public String getUsername()                     { return username; }
    public void setUsername(String username)        { this.username = username; }

    public String getPassword()                     { return password; }
    public void setPassword(String password)        { this.password = password; }

    public String getPasswordHash()                 { return passwordHash != null ? passwordHash : password; }
    public void setPasswordHash(String passwordHash){ this.passwordHash = passwordHash; this.password = passwordHash; }

    public Long getRoleId()                         { return roleId != null ? roleId : (role != null ? role.getRoleId() : null); }
    public void setRoleId(Long roleId)              { this.roleId = roleId; }

    public String getRoleName()                     { return roleName != null ? roleName : (role != null ? role.getRoleName() : null); }
    public void setRoleName(String roleName)        { this.roleName = roleName; }

    public Role getRole()                           { return role; }
    public void setRole(Role role)                  { this.role = role; if (role != null) { this.roleId = role.getRoleId(); this.roleName = role.getRoleName(); } }

    public String getStatus()                       { return status; }
    public void setStatus(String status)            { this.status = status; }

    public Timestamp getLastLogin()                 { return lastLogin != null ? lastLogin : lastLoginAt; }
    public void setLastLogin(Timestamp lastLogin)   { this.lastLogin = lastLogin; this.lastLoginAt = lastLogin; }

    public Timestamp getLastLoginAt()               { return lastLoginAt != null ? lastLoginAt : lastLogin; }
    public void setLastLoginAt(Timestamp lastLoginAt){ this.lastLoginAt = lastLoginAt; this.lastLogin = lastLoginAt; }

    @Override
    public String toString() {
        return "User [userId=" + userId + ", username=" + username
                + ", roleId=" + getRoleId() + ", roleName=" + getRoleName()
                + ", status=" + status + "]";
    }
}
