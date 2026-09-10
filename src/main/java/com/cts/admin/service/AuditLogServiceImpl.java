package com.cts.admin.service;

import java.util.List;

import com.cts.admin.dao.AuditLogDAO;
import com.cts.admin.dao.AuditLogDAOImpl;
import com.cts.admin.model.AuditLog;

public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogDAO auditLogDAO;

    public AuditLogServiceImpl() {
        auditLogDAO = new AuditLogDAOImpl();
    }

    @Override
    public List<AuditLog> getAuditLogs(int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;
        return auditLogDAO.getAuditLogs(page, pageSize);
    }

    @Override
    public int getTotalAuditLogCount() {
        return auditLogDAO.getTotalAuditLogCount();
    }

    @Override
    public String createAuditLog(Long userId) {
        if (userId == null) throw new IllegalArgumentException("User ID cannot be null.");
        return auditLogDAO.createAuditLog(userId);
    }

    @Override
    public void endAuditLog(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) return;
        auditLogDAO.endAuditLog(sessionId);
    }
}
