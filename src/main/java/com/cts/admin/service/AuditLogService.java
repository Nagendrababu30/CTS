package com.cts.admin.service;

import java.util.Date;
import java.util.List;

import com.cts.admin.model.AuditLog;

public interface AuditLogService {

	List<AuditLog> getAuditLogs(int page, int pageSize);

	List<AuditLog> getAuditLogs(int page, int pageSize, String searchText, String roleName, Date fromDate, Date toDate);

	List<AuditLog> getAllAuditLogs(String searchText, String roleName, Date fromDate, Date toDate);

	int getTotalAuditLogCount();

	int getTotalAuditLogCount(String searchText, String roleName, Date fromDate, Date toDate);

	String createAuditLog(Long userId);

	void endAuditLog(String sessionId);
}