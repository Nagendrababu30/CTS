package com.cts.admin.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long      auditId;
    private Long      userId;
    private String    userName;
    private String    roleName;
    private String    action;
    private Timestamp eventTime;
    private Timestamp loginTime;
    private Timestamp logoutTime;

    public AuditLog() {}

    public Long      getAuditId()                          { return auditId; }
    public void      setAuditId(Long auditId)              { this.auditId = auditId; }
    public Long      getUserId()                           { return userId; }
    public void      setUserId(Long userId)                { this.userId = userId; }
    public String    getUserName()                         { return userName; }
    public void      setUserName(String userName)          { this.userName = userName; }
    public String    getRoleName()                         { return roleName; }
    public void      setRoleName(String roleName)          { this.roleName = roleName; }
    public String    getAction()                           { return action; }
    public void      setAction(String action)              { this.action = action; }
    public Timestamp getEventTime()                        { return eventTime; }
    public void      setEventTime(Timestamp eventTime)     { this.eventTime = eventTime; }
    public Timestamp getLoginTime()                        { return loginTime; }
    public void      setLoginTime(Timestamp loginTime)     { this.loginTime = loginTime; }
    public Timestamp getLogoutTime()                       { return logoutTime; }
    public void      setLogoutTime(Timestamp logoutTime)   { this.logoutTime = logoutTime; }
}
