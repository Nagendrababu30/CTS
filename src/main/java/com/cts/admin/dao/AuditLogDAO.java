package com.cts.admin.dao;

import java.util.List;
import com.cts.admin.model.AuditLog;

public interface AuditLogDAO {

    List<AuditLog> getAuditLogs(int page, int pageSize);

    int getTotalAuditLogCount();

    /* Inserts a row into user_session on login — returns generated session_id */
    String createAuditLog(Long userId);

    /* Updates logout_time and status on logout */
    void endAuditLog(String sessionId);
}
