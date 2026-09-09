package com.cts.admin.dao;

import java.util.List;
import com.cts.admin.model.AuditLog;

public interface AuditLogDAO {
    List<AuditLog> getAuditLogs(int page, int pageSize);
    int getTotalAuditLogCount();
}
