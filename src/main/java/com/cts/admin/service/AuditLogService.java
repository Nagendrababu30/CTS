package com.cts.admin.service;

import java.util.Date;
import java.util.List;

import com.cts.admin.model.AuditLog;

public interface AuditLogService {

	// Existing method
	List<AuditLog> getAuditLogs(int page, int pageSize);

	// New filtered method
	List<AuditLog> getAuditLogs(int page, int pageSize, String searchText, String roleName, Date fromDate, Date toDate);

	// Existing count method
	int getTotalAuditLogCount();

	// New filtered count method
	int getTotalAuditLogCount(String searchText, String roleName, Date fromDate, Date toDate);

	/* Called on login */
	String createAuditLog(Long userId);

	/* Called on logout */
	void endAuditLog(String sessionId);
}