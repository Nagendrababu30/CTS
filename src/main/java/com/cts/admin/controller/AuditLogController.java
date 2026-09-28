
package com.cts.admin.controller;

import java.sql.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.event.PagingEvent;

import com.cts.admin.model.AuditLog;
import com.cts.admin.report.AuditLogReportService;
import com.cts.admin.service.AuditLogService;
import com.cts.admin.service.AuditLogServiceImpl;
import com.cts.admin.service.RoleService;
import com.cts.admin.service.RoleServiceImpl;

public class AuditLogController extends GenericForwardComposer<Component> {

	private static final long serialVersionUID = 1L;

	private static final int PAGE_SIZE = 10;

	private static final java.util.TimeZone IST = java.util.TimeZone.getTimeZone("Asia/Kolkata");

	private Listbox auditLogListbox;

	private Paging auditLogPaging;

	private Textbox auditSearchTextbox;

	private Button auditSearchButton;

	private Button auditClearButton;

	private Combobox auditRoleCombobox;

	private Datebox auditFromDate;

	private Datebox auditToDate;

	private Button auditDownloadPdfButton;

	private AuditLogService auditLogService;

	private RoleService roleService;

	private AuditLogReportService auditLogReportService;

	@Override
	public void doAfterCompose(Component comp) throws Exception {

		super.doAfterCompose(comp);

		auditLogService = new AuditLogServiceImpl();

		roleService = new RoleServiceImpl();

		auditLogReportService = new AuditLogReportService();

		loadRoleFilter();

		auditLogPaging.setPageSize(PAGE_SIZE);

		auditLogPaging.setTotalSize(auditLogService.getTotalAuditLogCount(null, null, null, null));

		loadAuditLogs(0);

		auditLogPaging.addEventListener("onPaging", new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception {

				PagingEvent pagingEvent = (PagingEvent) event;

				loadAuditLogs(pagingEvent.getActivePage() * PAGE_SIZE);
			}
		});

		auditSearchButton.addEventListener("onClick", new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception {

				refreshAuditLogs();
			}
		});

		auditSearchTextbox.addEventListener("onOK", new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception {

				refreshAuditLogs();
			}
		});

		if (auditClearButton != null) {
			auditClearButton.addEventListener("onClick", new EventListener<Event>() {

				@Override
				public void onEvent(Event event) throws Exception {

					clearAuditFilters();
				}
			});
		}

		auditRoleCombobox.addEventListener("onSelect", new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception {

				refreshAuditLogs();
			}
		});

		auditFromDate.addEventListener("onChange", new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception {

				refreshAuditLogs();
			}
		});

		auditToDate.addEventListener("onChange", new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception {

				refreshAuditLogs();
			}
		});

		auditDownloadPdfButton.addEventListener("onClick", new EventListener<Event>() {

			@Override
			public void onEvent(Event event) throws Exception {

				exportAuditLogsToPdf();
			}
		});
	}

	// Clear all filters and reset table
	private void clearAuditFilters() {

		if (auditSearchTextbox != null) {
			auditSearchTextbox.setValue("");
		}

		if (auditRoleCombobox != null) {
			auditRoleCombobox.setSelectedIndex(0);
		}

		if (auditFromDate != null) {
			auditFromDate.setValue(null);
			auditFromDate.setRawValue(null);
		}

		if (auditToDate != null) {
			auditToDate.setValue(null);
			auditToDate.setRawValue(null);
		}

		auditLogPaging.setTotalSize(auditLogService.getTotalAuditLogCount(null, null, null, null));
		auditLogPaging.setActivePage(0);

		loadAuditLogs(0);
	}

	// Load role filter from database

	private void loadRoleFilter() {

		auditRoleCombobox.getItems().clear();

		Comboitem all = new Comboitem("All Roles");

		all.setValue(null);

		auditRoleCombobox.appendChild(all);

		try {

			roleService.getAllRoles().forEach(role -> {

				Comboitem item = new Comboitem(role.getRoleName());

				item.setValue(role.getRoleName());

				auditRoleCombobox.appendChild(item);
			});

		} catch (Exception e) {

			e.printStackTrace();
		}

		auditRoleCombobox.setSelectedIndex(0);
	}

	// Get selected role

	private String getSelectedRole() {

		if (auditRoleCombobox.getSelectedItem() == null) {
			return null;
		}

		String selected = auditRoleCombobox.getSelectedItem().getLabel();

		if (selected == null || selected.equals("All Roles")) {

			return null;
		}

		return selected;
	}

	// Refresh filtered audit logs

	private void refreshAuditLogs() {

		String searchText = auditSearchTextbox.getValue();

		if (searchText != null) {
			searchText = searchText.trim();
		}

		String roleFilter = getSelectedRole();

		Date fromDate = auditFromDate.getValue();

		Date toDate = auditToDate.getValue();

		if (fromDate != null && toDate != null && fromDate.after(toDate)) {

			Messagebox.show("From Date cannot be later than To Date.", "Invalid Date Range", Messagebox.OK,
					Messagebox.EXCLAMATION);

			return;
		}

		auditLogPaging.setTotalSize(auditLogService.getTotalAuditLogCount(searchText, roleFilter, fromDate, toDate));

		auditLogPaging.setActivePage(0);

		loadAuditLogs(0);
	}

	// Load filtered audit logs

	private void loadAuditLogs(int offset) {

		try {

			int page = (offset / PAGE_SIZE) + 1;

			String searchText = auditSearchTextbox.getValue();

			if (searchText != null) {
				searchText = searchText.trim();
			}

			String roleFilter = getSelectedRole();

			Date fromDate = auditFromDate.getValue();

			Date toDate = auditToDate.getValue();

			if (fromDate != null && toDate != null && fromDate.after(toDate)) {

				return;
			}

			List<AuditLog> logs = auditLogService.getAuditLogs(page, PAGE_SIZE, searchText, roleFilter, fromDate,
					toDate);

			auditLogListbox.getItems().clear();

			for (AuditLog log : logs) {

				Listitem item = new Listitem();

				Listcell userIdCell = new Listcell();

				Label userIdLabel = new Label(log.getUserId() != null ? String.valueOf(log.getUserId()) : "-");

				userIdLabel.setSclass("audit-user-label");

				userIdCell.appendChild(userIdLabel);

				item.appendChild(userIdCell);

				Listcell roleCell = new Listcell();

				Label roleLabel = new Label(log.getRoleName() != null ? log.getRoleName() : "-");

				roleLabel.setSclass("audit-role-label");

				roleCell.appendChild(roleLabel);

				item.appendChild(roleCell);

				Listcell loginDateCell = new Listcell();

				Label loginDateLabel = new Label(log.getLoginTime() != null ? formatDate(log.getLoginTime()) : "-");

				loginDateLabel.setSclass("audit-datetime-label");

				loginDateCell.appendChild(loginDateLabel);

				item.appendChild(loginDateCell);

				Listcell loginTimeCell = new Listcell();

				Label loginTimeLabel = new Label(log.getLoginTime() != null ? formatTime(log.getLoginTime()) : "-");

				loginTimeLabel.setSclass("audit-datetime-label");

				loginTimeCell.appendChild(loginTimeLabel);

				item.appendChild(loginTimeCell);

				Listcell logoutDateCell = new Listcell();

				Label logoutDateLabel = new Label(log.getLogoutTime() != null ? formatDate(log.getLogoutTime()) : "-");

				logoutDateLabel.setSclass("audit-datetime-label");

				logoutDateCell.appendChild(logoutDateLabel);

				item.appendChild(logoutDateCell);

				Listcell logoutTimeCell = new Listcell();

				Label logoutTimeLabel = new Label(log.getLogoutTime() != null ? formatTime(log.getLogoutTime()) : "-");

				logoutTimeLabel.setSclass("audit-datetime-label");

				logoutTimeCell.appendChild(logoutTimeLabel);

				item.appendChild(logoutTimeCell);

				auditLogListbox.appendChild(item);
			}

		} catch (Exception e) {

			e.printStackTrace();

			Messagebox.show("Unable to load audit logs.", "Audit Logs", Messagebox.OK, Messagebox.ERROR);
		}
	}

	// Export audit logs to pdf

	private void exportAuditLogsToPdf() {

		try {

			String searchText = auditSearchTextbox.getValue();

			if (searchText != null) {
				searchText = searchText.trim();
			}

			String roleFilter = getSelectedRole();

			Date fromDate = auditFromDate.getValue();

			Date toDate = auditToDate.getValue();

			if (fromDate != null && toDate != null && fromDate.after(toDate)) {

				Messagebox.show("From Date cannot be later than To Date.", "Invalid Date Range", Messagebox.OK,
						Messagebox.EXCLAMATION);

				return;
			}

			List<AuditLog> auditLogs = auditLogService.getAllAuditLogs(searchText, roleFilter, fromDate, toDate);

			if (auditLogs == null || auditLogs.isEmpty()) {
				Messagebox.show("Unable to download PDF: No records available for the selected filter.",
						"Audit Log Export", Messagebox.OK, Messagebox.INFORMATION);
				return;
			}

			SimpleDateFormat reportDateFormat = new SimpleDateFormat("dd/MM/yyyy");

			reportDateFormat.setTimeZone(IST);

			String generatedDate = reportDateFormat.format(new Date());

			String reportFromDate = fromDate != null ? reportDateFormat.format(fromDate) : "All";

			String reportToDate = toDate != null ? reportDateFormat.format(toDate) : "All";

			byte[] pdfBytes = auditLogReportService.generateAuditLogPdf(auditLogs, generatedDate, reportFromDate,
					reportToDate);

			String fileName = "Audit_Log_Report_" + generatedDate.replace("/", "-") + ".pdf";

			Filedownload.save(pdfBytes, "application/pdf", fileName);

		} catch (Exception e) {

			e.printStackTrace();

			Messagebox.show("Unable to generate audit log report.", "Audit Log Report", Messagebox.OK,
					Messagebox.ERROR);
		}
	}

	private String formatDate(Timestamp timestamp) {

		if (timestamp == null) {
			return "-";
		}

		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

		sdf.setTimeZone(IST);

		return sdf.format(timestamp);
	}

	private String formatTime(Timestamp timestamp) {

		if (timestamp == null) {
			return "-";
		}

		SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");

		sdf.setTimeZone(IST);

		return sdf.format(timestamp);
	}
}