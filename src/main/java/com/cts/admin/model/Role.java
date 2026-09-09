package com.cts.admin.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class Role implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long      roleId;
    private String    roleName;
    private String    description;
    private String    status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Role() {}

    public Role(Long roleId, String roleName, String description,
                String status, Timestamp createdAt, Timestamp updatedAt) {
        this.roleId      = roleId;
        this.roleName    = roleName;
        this.description = description;
        this.status      = status;
        this.createdAt   = createdAt;
        this.updatedAt   = updatedAt;
    }

    public Long      getRoleId()                        { return roleId; }
    public void      setRoleId(Long roleId)             { this.roleId = roleId; }

    public String    getRoleName()                      { return roleName; }
    public void      setRoleName(String roleName)       { this.roleName = roleName; }

    public String    getDescription()                   { return description; }
    public void      setDescription(String description) { this.description = description; }

    public String    getStatus()                        { return status; }
    public void      setStatus(String status)           { this.status = status; }

    public Timestamp getCreatedAt()                     { return createdAt; }
    public void      setCreatedAt(Timestamp createdAt)  { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt()                     { return updatedAt; }
    public void      setUpdatedAt(Timestamp updatedAt)  { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Role [roleId=" + roleId + ", roleName=" + roleName
                + ", status=" + status + "]";
    }
}
