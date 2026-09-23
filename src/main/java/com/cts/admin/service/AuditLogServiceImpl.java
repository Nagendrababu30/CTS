package com.cts.admin.service;

import java.util.Date;
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

		if (page < 1)
			page = 1;
		if (pageSize < 1)
			pageSize = 10;

		return auditLogDAO.getAuditLogs(page, pageSize);
	}

	// ================================================================
	// FILTERED AUDIT LOGS
	// ================================================================

	@Override
	public List<AuditLog> getAuditLogs(int page, int pageSize, String searchText, String roleName, Date fromDate,
			Date toDate) {

		if (page < 1)
			page = 1;
		if (pageSize < 1)
			pageSize = 10;

		return auditLogDAO.getAuditLogs(page, pageSize, searchText, roleName, fromDate, toDate);
	}

	// ================================================================
	// COUNT
	// ================================================================

	@Override
	public int getTotalAuditLogCount() {

		return auditLogDAO.getTotalAuditLogCount();
	}

	
	// ================================================================
	// GET ALL FILTERED AUDIT LOGS - FOR REPORT GENERATION
	// ================================================================

	@Override
	public List<AuditLog> getAllAuditLogs(
	        String searchText,
	        String roleName,
	        Date fromDate,
	        Date toDate) {

	    return auditLogDAO.getAllAuditLogs(
	            searchText,
	            roleName,
	            fromDate,
	            toDate);
	}
	
	
	
	
	
	// ================================================================
	// FILTERED COUNT
	// ================================================================

	@Override
	public int getTotalAuditLogCount(String searchText, String roleName, Date fromDate, Date toDate) {

		return auditLogDAO.getTotalAuditLogCount(searchText, roleName, fromDate, toDate);
	}

	// ================================================================
	// CREATE AUDIT LOG
	// ================================================================

	@Override
	public String createAuditLog(Long userId) {

		if (userId == null) {
			throw new IllegalArgumentException("User ID cannot be null.");
		}

		return auditLogDAO.createAuditLog(userId);
	}

	// ================================================================
	// END AUDIT LOG
	// ================================================================

	@Override
	public void endAuditLog(String sessionId) {

		if (sessionId == null || sessionId.trim().isEmpty()) {
			return;
		}

		auditLogDAO.endAuditLog(sessionId);
	}
}