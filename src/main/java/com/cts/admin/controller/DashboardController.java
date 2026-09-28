package com.cts.admin.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Vlayout;

import com.cts.admin.model.AuditLog;
import com.cts.admin.model.Session;
import com.cts.admin.service.AuditLogService;
import com.cts.admin.service.AuditLogServiceImpl;
import com.cts.admin.service.SessionService;
import com.cts.admin.service.SessionServiceImpl;
import com.cts.inward.config.ConnectionPool;

public class DashboardController extends GenericForwardComposer<Component> {

	private static final long serialVersionUID = 1L;

	private static final int RECENT_LOG_COUNT = 5;

	private static final java.util.TimeZone IST = java.util.TimeZone.getTimeZone("Asia/Kolkata");

	private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy hh:mm a");

	private Label totalUsersValue;
	private Label activeUsersValue;
	private Label inactiveUsersValue;
	private Label userManageLink;

	private Vlayout sessionStatusBox;
	private Label sessionCurrentLbl;
	private Label sessionNameLbl;
	private Div sessionStatusBadge;
	private Label sessionBadgeLbl;
	private Label sessionManageLink;

	private Button viewAuditLogsBtn;
	private Listbox recentAuditListbox;

	private AuditLogService auditLogService;
	private SessionService sessionService;

	@Override
	public void doAfterCompose(Component comp) throws Exception {

		super.doAfterCompose(comp);

		DATE_FMT.setTimeZone(IST);

		auditLogService = new AuditLogServiceImpl();
		sessionService = new SessionServiceImpl();

		userManageLink.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
			@Override
			public void onEvent(Event e) throws Exception {
				Executions.sendRedirect("/zul/admin/admin-user-management.zul");
			}
		});

		sessionManageLink.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
			@Override
			public void onEvent(Event e) throws Exception {
				Executions.sendRedirect("/zul/admin/admin-session-management.zul");
			}
		});

		viewAuditLogsBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
			@Override
			public void onEvent(Event e) throws Exception {
				Executions.sendRedirect("/zul/admin/admin-audit-logs.zul");
			}
		});

		loadUserCounts();
		loadSessionState();
		loadRecentAuditLogs();
	}

	// Load user counts

	private void loadUserCounts() {

		try (Connection conn = ConnectionPool.getDataSource().getConnection();
				PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) AS total, "
						+ "SUM(CASE WHEN status = 'ACTIVE'   THEN 1 ELSE 0 END) AS active, "
						+ "SUM(CASE WHEN status = 'INACTIVE' THEN 1 ELSE 0 END) AS inactive " + "FROM \"user\"");
				ResultSet rs = stmt.executeQuery()) {
			if (rs.next()) {
				totalUsersValue.setValue(String.valueOf(rs.getInt("total")));
				activeUsersValue.setValue(String.valueOf(rs.getInt("active")));
				inactiveUsersValue.setValue(String.valueOf(rs.getInt("inactive")));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// load session state

	private void loadSessionState() {

		try {

			Session active = sessionService.getActiveSession();

			if (active == null) {

				sessionStatusBox.setSclass("session-status-box session-box-inactive");
				sessionCurrentLbl.setSclass("session-current-label");
				sessionCurrentLbl.setValue("CURRENT SESSION");
				sessionNameLbl.setValue("No Active Session");
				sessionStatusBadge.setSclass("status-badge status-inactive");
				sessionBadgeLbl.setValue("NOT STARTED");

			} else {

				sessionStatusBox.setSclass("session-status-box session-box-active");
				sessionCurrentLbl.setSclass("session-current-label-active");
				sessionCurrentLbl.setValue("CURRENT SESSION");
				sessionNameLbl.setValue(active.getSessionName() != null ? active.getSessionName() : "-");
				sessionStatusBadge.setSclass("status-badge status-active");
				sessionBadgeLbl.setValue("Active");

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Recent audit logs - top 5 from user_session

	private void loadRecentAuditLogs() {

		try {

			List<AuditLog> logs = auditLogService.getAuditLogs(1, RECENT_LOG_COUNT);

			recentAuditListbox.getItems().clear();

			for (AuditLog log : logs) {

				Listitem item = new Listitem();

				Listcell userIdCell = new Listcell();
				Label userIdLabel = new Label(log.getUserId() == null ? "-" : String.valueOf(log.getUserId()));
				userIdLabel.setSclass("audit-user-label");
				userIdCell.appendChild(userIdLabel);
				item.appendChild(userIdCell);

				Listcell roleCell = new Listcell();
				Label roleLabel = new Label(log.getRoleName() == null ? "-" : log.getRoleName());
				roleLabel.setSclass("audit-module-label");
				roleCell.appendChild(roleLabel);
				item.appendChild(roleCell);

				Listcell loginDateCell = new Listcell();
				String loginDateStr = "-";
				if (log.getLoginTime() != null) {
					SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy");
					dateFmt.setTimeZone(IST);
					loginDateStr = dateFmt.format(log.getLoginTime());
				}
				Label loginDateLabel = new Label(loginDateStr);
				loginDateLabel.setSclass("audit-datetime-label");
				loginDateCell.appendChild(loginDateLabel);
				item.appendChild(loginDateCell);

				Listcell loginTimeCell = new Listcell();
				String loginTimeStr = "-";
				if (log.getLoginTime() != null) {
					SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a");
					timeFmt.setTimeZone(IST);
					loginTimeStr = timeFmt.format(log.getLoginTime());
				}
				Label loginTimeLabel = new Label(loginTimeStr);
				loginTimeLabel.setSclass("audit-datetime-label");
				loginTimeCell.appendChild(loginTimeLabel);
				item.appendChild(loginTimeCell);

				Listcell logoutDateCell = new Listcell();
				String logoutDateStr = "-";
				if (log.getLogoutTime() != null) {
					SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy");
					dateFmt.setTimeZone(IST);
					logoutDateStr = dateFmt.format(log.getLogoutTime());
				}
				Label logoutDateLabel = new Label(logoutDateStr);
				logoutDateLabel.setSclass("audit-datetime-label");
				logoutDateCell.appendChild(logoutDateLabel);
				item.appendChild(logoutDateCell);

				Listcell logoutTimeCell = new Listcell();
				String logoutTimeStr = "-";
				if (log.getLogoutTime() != null) {
					SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a");
					timeFmt.setTimeZone(IST);
					logoutTimeStr = timeFmt.format(log.getLogoutTime());
				}
				Label logoutTimeLabel = new Label(logoutTimeStr);
				logoutTimeLabel.setSclass("audit-datetime-label");
				logoutTimeCell.appendChild(logoutTimeLabel);
				item.appendChild(logoutTimeCell);

				item.setValue(log);
				recentAuditListbox.appendChild(item);
			}

		} catch (Exception e) {
			e.printStackTrace();
			Messagebox.show("Unable to load recent audit logs.", "Dashboard", Messagebox.OK, Messagebox.ERROR);
		}
	}
}
