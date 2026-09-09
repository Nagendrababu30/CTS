package com.cts.admin.service;

import java.util.List;
import com.cts.admin.model.AuditLog;

public interface AuditLogService {

    List<AuditLog> getAuditLogs(int page, int pageSize);

    int getTotalAuditLogCount();

    /* Called on login — inserts into user_session, returns session_id */
    String createAuditLog(Long userId);

    /* Called on logout — updates logout_time in user_session */
    void endAuditLog(String sessionId);
}
