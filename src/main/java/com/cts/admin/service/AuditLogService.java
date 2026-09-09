package com.cts.admin.service;

import java.util.List;
import com.cts.admin.model.AuditLog;

public interface AuditLogService {
    List<AuditLog> getAuditLogs(int page, int pageSize);
    int getTotalAuditLogCount();
}
