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

    private static final java.util.TimeZone IST =
            java.util.TimeZone.getTimeZone("Asia/Kolkata");

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a");

    /* ------------------------------------------------------------------ */
    /* ZUL COMPONENTS                                                       */
    /* ------------------------------------------------------------------ */

    /* User card */
    private Label totalUsersValue;
    private Label activeUsersValue;
    private Label inactiveUsersValue;
    private Label userManageLink;

    /* Session card */
    private Vlayout sessionStatusBox;
    private Label   sessionCurrentLbl;
    private Label   sessionNameLbl;
    private Div     sessionStatusBadge;
    private Label   sessionBadgeLbl;
    private Label   sessionManageLink;

    /* Batch card */
    private Label totalBatchesValue;
    private Label inProgressBatchesValue;
    private Label completedBatchesValue;
    private Label batchTrackLink;

    /* Recent logs */
    private Button  viewAuditLogsBtn;
    private Listbox recentAuditListbox;

    /* ------------------------------------------------------------------ */
    /* SERVICES                                                             */
    /* ------------------------------------------------------------------ */

    private AuditLogService auditLogService;
    private SessionService  sessionService;

    /* ------------------------------------------------------------------ */
    /* LIFECYCLE                                                            */
    /* ------------------------------------------------------------------ */

    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

        DATE_FMT.setTimeZone(IST);

        auditLogService = new AuditLogServiceImpl();
        sessionService  = new SessionServiceImpl();

        /* Navigation links */
        userManageLink.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
            @Override public void onEvent(Event e) throws Exception {
                Executions.sendRedirect("/zul/admin/admin-user-management.zul");
            }
        });

        sessionManageLink.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
            @Override public void onEvent(Event e) throws Exception {
                Executions.sendRedirect("/zul/admin/admin-session-management.zul");
            }
        });

        batchTrackLink.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
            @Override public void onEvent(Event e) throws Exception {
                Executions.sendRedirect("/zul/admin/admin-batch-monitoring.zul");
            }
        });

        viewAuditLogsBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
            @Override public void onEvent(Event e) throws Exception {
                Executions.sendRedirect("/zul/admin/admin-audit-logs.zul");
            }
        });

        /* Load all dashboard data */
        loadUserCounts();
        loadSessionState();
        loadBatchCounts();
        loadRecentAuditLogs();
    }

    /* ------------------------------------------------------------------ */
    /* USER COUNTS                                                          */
    /* ------------------------------------------------------------------ */

    private void loadUserCounts() {

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT COUNT(*) AS total, "
                        + "SUM(CASE WHEN status = 'ACTIVE'   THEN 1 ELSE 0 END) AS active, "
                        + "SUM(CASE WHEN status = 'INACTIVE' THEN 1 ELSE 0 END) AS inactive "
                        + "FROM \"user\"");
                ResultSet rs = stmt.executeQuery()
        ) {
            if (rs.next()) {
                totalUsersValue.setValue(String.valueOf(rs.getInt("total")));
                activeUsersValue.setValue(String.valueOf(rs.getInt("active")));
                inactiveUsersValue.setValue(String.valueOf(rs.getInt("inactive")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ------------------------------------------------------------------ */
    /* SESSION STATE                                                        */
    /* ------------------------------------------------------------------ */

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
                sessionNameLbl.setValue(
                        active.getSessionName() != null ? active.getSessionName() : "-");
                sessionStatusBadge.setSclass("status-badge status-active");
                sessionBadgeLbl.setValue("ACTIVE");

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ------------------------------------------------------------------ */
    /* BATCH COUNTS                                                         */
    /* Combines inward_batch (via inward_batch_history) +
     * outward_batch for total / in-progress / completed counts            */
    /* ------------------------------------------------------------------ */

    private void loadBatchCounts() {

        try (Connection conn = ConnectionPool.getDataSource().getConnection()) {

            /* Count outward batches by status */
            int outwardTotal = 0, outwardInProgress = 0, outwardCompleted = 0;

            try (
                    PreparedStatement stmt = conn.prepareStatement(
                            "SELECT batch_status, COUNT(*) AS cnt "
                            + "FROM outward_batch "
                            + "GROUP BY batch_status");
                    ResultSet rs = stmt.executeQuery()
            ) {
                while (rs.next()) {
                    String s = rs.getString("batch_status");
                    int cnt  = rs.getInt("cnt");
                    outwardTotal += cnt;
                    if (s != null) {
                        String upper = s.toUpperCase();
                        if (upper.contains("COMPLET") || upper.contains("VERIFIED")) {
                            outwardCompleted += cnt;
                        } else if (!upper.contains("CANCEL")) {
                            outwardInProgress += cnt;
                        }
                    }
                }
            }

            /* Count inward batches via latest history status */
            int inwardTotal = 0, inwardInProgress = 0, inwardCompleted = 0;

            try (
                    PreparedStatement stmt = conn.prepareStatement(
                            "SELECT DISTINCT ON (batch_id) batch_id, batch_status "
                            + "FROM inward_batch_history "
                            + "ORDER BY batch_id, changed_on DESC");
                    ResultSet rs = stmt.executeQuery()
            ) {
                while (rs.next()) {
                    String s = rs.getString("batch_status");
                    inwardTotal++;
                    if (s != null) {
                        String upper = s.toUpperCase();
                        if (upper.contains("COMPLET") || upper.contains("ACCEPT")) {
                            inwardCompleted++;
                        } else {
                            inwardInProgress++;
                        }
                    }
                }
            }

            int total      = outwardTotal      + inwardTotal;
            int inProgress = outwardInProgress + inwardInProgress;
            int completed  = outwardCompleted  + inwardCompleted;

            totalBatchesValue.setValue(String.valueOf(total));
            inProgressBatchesValue.setValue(String.valueOf(inProgress));
            completedBatchesValue.setValue(String.valueOf(completed));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ------------------------------------------------------------------ */
    /* RECENT AUDIT LOGS — top 5 from user_session                        */
    /* ------------------------------------------------------------------ */

    private void loadRecentAuditLogs() {

        try {

            List<AuditLog> logs =
                    auditLogService.getAuditLogs(1, RECENT_LOG_COUNT);

            recentAuditListbox.getItems().clear();

            for (AuditLog log : logs) {

                Listitem item = new Listitem();

                /* User ID */
                Listcell userIdCell = new Listcell();
                Label userIdLabel = new Label(
                        log.getUserId() == null ? "-" : String.valueOf(log.getUserId()));
                userIdLabel.setSclass("audit-user-label");
                userIdCell.appendChild(userIdLabel);
                item.appendChild(userIdCell);

                /* Role */
                Listcell roleCell = new Listcell();
                Label roleLabel = new Label(
                        log.getRoleName() == null ? "-" : log.getRoleName());
                roleLabel.setSclass("audit-module-label");
                roleCell.appendChild(roleLabel);
                item.appendChild(roleCell);

                /* Login */
                Listcell loginCell = new Listcell();
                Label loginLabel = new Label(
                        log.getLoginTime() == null ? "-"
                                : DATE_FMT.format(log.getLoginTime()));
                loginLabel.setSclass("audit-datetime-label");
                loginCell.appendChild(loginLabel);
                item.appendChild(loginCell);

                /* Logout */
                Listcell logoutCell = new Listcell();
                Label logoutLabel = new Label(
                        log.getLogoutTime() == null ? "-"
                                : DATE_FMT.format(log.getLogoutTime()));
                logoutLabel.setSclass("audit-datetime-label");
                logoutCell.appendChild(logoutLabel);
                item.appendChild(logoutCell);

                item.setValue(log);
                recentAuditListbox.appendChild(item);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Messagebox.show("Unable to load recent audit logs.", "Dashboard",
                    Messagebox.OK, Messagebox.ERROR);
        }
    }
}
