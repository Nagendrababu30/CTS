package com.cts.admin.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

import com.cts.admin.model.User;
import com.cts.admin.service.AuditLogService;
import com.cts.admin.service.AuditLogServiceImpl;
import com.cts.admin.service.UserService;
import com.cts.admin.service.UserServiceImpl;
import com.cts.inward.config.ConnectionPool;

public class LoginComposer extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    private static final java.util.TimeZone IST =
            java.util.TimeZone.getTimeZone("Asia/Kolkata");

    private Textbox username;
    private Textbox password;
    private Label   loginMessage;

    private UserService     userService;
    private AuditLogService auditLogService;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        userService     = new UserServiceImpl();
        auditLogService = new AuditLogServiceImpl();
    }

	public void onClick$loginButton() {

        String usernameValue = username.getValue();
        String passwordValue = password.getValue();

        User user = userService.authenticate(usernameValue, passwordValue);

        if (user == null) {
            loginMessage.setValue("Invalid username or password.");
            return;
        }

        /* Insert into user_session via AuditLogService */
        String auditSessionId = auditLogService.createAuditLog(user.getUserId());

        /* Update last_login in "user" table and set on user object */
        java.sql.Timestamp nowIST = new java.sql.Timestamp(
                java.util.Calendar.getInstance(IST).getTimeInMillis());
        updateLastLogin(user.getUserId(), nowIST);
        user.setLastLogin(nowIST);

        Session session = Executions.getCurrent().getSession();
        session.setAttribute("loggedInUser",    user);
        session.setAttribute("userId",          user.getUserId());
        session.setAttribute("username",        user.getUsername());
        session.setAttribute("roleId",          user.getRoleId());
        session.setAttribute("roleName",        user.getRoleName());
        session.setAttribute("auditSessionId",  auditSessionId);

        redirectUser(user);
    }

    /* ------------------------------------------------------------------ */
    /* UPDATE last_login in "user" table                                    */
    /* ------------------------------------------------------------------ */

    private void updateLastLogin(Long userId, java.sql.Timestamp nowIST) {

        String sql = "UPDATE \"user\" SET last_login = ? WHERE user_id = ?";

        try (
                Connection conn = ConnectionPool.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setTimestamp(1, nowIST);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ------------------------------------------------------------------ */

    private void redirectUser(User user) {

        String roleName = user.getRoleName();

        if (roleName == null || roleName.isBlank()) {
            loginMessage.setValue("User role is not configured.");
            return;
        }

        switch (roleName.toUpperCase()) {
            case "ADMIN":
                Executions.sendRedirect("/zul/admin/admin-dashboard.zul"); break;
            case "INWARD_MAKER":
                Executions.sendRedirect("/zul/inward-maker/dashboard.zul"); break;
            case "INWARD_CHECKER":
                Executions.sendRedirect("/zul/inward-checker/dashboard.zul"); break;
            case "OUTWARD_MAKER":
                Executions.sendRedirect("/zul/outward/outward-maker/outward-maker-dashboard.zul"); break;
            case "OUTWARD_CHECKER":
                Executions.sendRedirect("/zul/outward/outward-checker/dashboard.zul"); break;
            case "CAPTURE_OPERATOR":
                Executions.sendRedirect("/zul/outward/outward-maker/capture-operator-batch-capture.zul"); break;
            default:
                loginMessage.setValue("User role is not configured.");
        }
    }
}
