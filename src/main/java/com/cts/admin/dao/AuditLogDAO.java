package com.cts.admin.dao;

import java.util.Date;
import java.util.List;

import com.cts.admin.model.AuditLog;

public interface AuditLogDAO {

    public List<AuditLog> getAuditLogs(int page, int pageSize);

    public List<AuditLog> getAuditLogs(
            int page,
            int pageSize,
            String searchText,
            String roleName,
            Date fromDate,
            Date toDate);
    
    public int getTotalAuditLogCount();
    
    public int getTotalAuditLogCount(
            String searchText,
            String roleName,
            Date fromDate,
            Date toDate);

    /* Inserts a row into user_session on login — returns generated session_id */
    public String createAuditLog(Long userId);

    /* Updates logout_time and status on logout */
    public void endAuditLog(String sessionId);
}
