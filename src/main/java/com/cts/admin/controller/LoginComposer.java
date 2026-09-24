package com.cts.admin.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

import com.cts.admin.model.User;
import com.cts.admin.service.AuditLogService;
import com.cts.admin.service.AuditLogServiceImpl;
import com.cts.admin.service.SessionService;
import com.cts.admin.service.SessionServiceImpl;
import com.cts.admin.service.UserService;
import com.cts.admin.service.UserServiceImpl;
import com.cts.inward.config.ConnectionPool;

public class LoginComposer extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    // =====================================================
    // TIME ZONE
    // =====================================================

    private static final java.util.TimeZone IST =
            java.util.TimeZone.getTimeZone("Asia/Kolkata");


    // =====================================================
    // LOGIN COMPONENTS
    // =====================================================

    private Textbox username;
    private Textbox password;
    private Label loginMessage;
    private A togglePasswordBtn;


    // =====================================================
    // SERVICES
    // =====================================================

    private UserService userService;
    private AuditLogService auditLogService;
    private SessionService sessionService;


    // =====================================================
    // PASSWORD VISIBILITY
    // =====================================================

    private boolean isPasswordVisible = false;


    // =====================================================
    // INITIALIZATION
    // =====================================================

    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

        userService =
                new UserServiceImpl();

        auditLogService =
                new AuditLogServiceImpl();

        sessionService =
                new SessionServiceImpl();
    }


    // =====================================================
    // LOGIN
    // =====================================================

    public void onClick$loginButton(Event event) {

        loginMessage.setValue("");

        String usernameValue =
                username.getValue();

        String passwordValue =
                password.getValue();


        // =================================================
        // VALIDATE INPUT
        // =================================================

        if (usernameValue == null
                || usernameValue.trim().isEmpty()
                || passwordValue == null
                || passwordValue.isEmpty()) {

            loginMessage.setValue(
                    "Please enter both User ID and Password."
            );

            return;
        }


        // =================================================
        // AUTHENTICATE USER
        // =================================================

        User user =
                userService.authenticate(
                        usernameValue,
                        passwordValue
                );


        if (user == null) {

            loginMessage.setValue(
                    "Invalid username or password."
            );

            return;
        }


        // =================================================
        // CREATE AUDIT SESSION
        // =================================================

        String auditSessionId =
                auditLogService.createAuditLog(
                        user.getUserId()
                );


        // =================================================
        // UPDATE LAST LOGIN
        // =================================================

        java.sql.Timestamp nowIST =
                new java.sql.Timestamp(
                        java.util.Calendar
                                .getInstance(IST)
                                .getTimeInMillis()
                );


        updateLastLogin(
                user.getUserId(),
                nowIST
        );


        user.setLastLogin(nowIST);


        // =================================================
        // CREATE USER SESSION
        // =================================================

        Session session =
                Executions
                        .getCurrent()
                        .getSession();


        session.setAttribute(
                "loggedInUser",
                user
        );


        session.setAttribute(
                "userId",
                user.getUserId()
        );


        session.setAttribute(
                "username",
                user.getUsername()
        );


        session.setAttribute(
                "roleId",
                user.getRoleId()
        );


        session.setAttribute(
                "roleName",
                user.getRoleName()
        );


        session.setAttribute(
                "auditSessionId",
                auditSessionId
        );


        // =================================================
        // REDIRECT USER BASED ON ROLE
        // =================================================

        redirectUser(user);
    }


    // =====================================================
    // TOGGLE PASSWORD VISIBILITY
    // =====================================================

    public void onClick$togglePasswordBtn(Event event) {

        isPasswordVisible =
                !isPasswordVisible;


        if (isPasswordVisible) {

            password.setType("text");

            togglePasswordBtn.setIconSclass(
                    "z-icon-eye-slash"
            );

        } else {

            password.setType("password");

            togglePasswordBtn.setIconSclass(
                    "z-icon-eye"
            );
        }
    }


    // =====================================================
    // UPDATE LAST LOGIN
    // =====================================================

    private void updateLastLogin(
            Long userId,
            java.sql.Timestamp nowIST) {

        String sql =
                "UPDATE \"user\" "
                + "SET last_login = ? "
                + "WHERE user_id = ?";


        try (
                Connection conn =
                        ConnectionPool
                                .getDataSource()
                                .getConnection();

                PreparedStatement stmt =
                        conn.prepareStatement(sql)
        ) {

            stmt.setTimestamp(
                    1,
                    nowIST
            );

            stmt.setLong(
                    2,
                    userId
            );

            stmt.executeUpdate();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    // =====================================================
    // REDIRECT USER BASED ON ROLE
    // =====================================================

    private void redirectUser(User user) {

        String roleName =
                user.getRoleName();


        // =================================================
        // ROLE VALIDATION
        // =================================================

        if (roleName == null
                || roleName.isBlank()) {

            loginMessage.setValue(
                    "User role is not configured."
            );

            return;
        }


        // =================================================
        // ROLE-BASED REDIRECTION
        // =================================================

        switch (roleName) {


        // =================================================
        // ADMIN
        //
        // NO OUTWARD SESSION CHECK
        // =================================================

        case "Admin":

            Executions.sendRedirect(
                    "/zul/admin/admin-dashboard.zul"
            );

            break;


        // =================================================
        // INWARD MAKER
        //
        // NO OUTWARD SESSION CHECK
        // =================================================

        case "Inward Maker":

            Executions.sendRedirect(
                    "/zul/inward-maker/dashboard.zul"
            );

            break;


        // =================================================
        // INWARD CHECKER
        //
        // NO OUTWARD SESSION CHECK
        // =================================================

        case "Inward Checker":

            Executions.sendRedirect(
                    "/zul/inward-checker/dashboard.zul"
            );

            break;


        // =================================================
        // OUTWARD MAKER
        //
        // OUTWARD SESSION CHECK REQUIRED
        // =================================================

        case "Outward Maker":

            redirectOutwardUser(
                    "/zul/outward/outward-maker/"
                    + "outward-maker-dashboard.zul"
            );

            break;


        // =================================================
        // OUTWARD CHECKER
        //
        // OUTWARD SESSION CHECK REQUIRED
        // =================================================

        case "Outward Checker":

            redirectOutwardUser(
                    "/zul/outward/outward-checker/"
                    + "dashboard.zul"
            );

            break;


        // =================================================
        // CAPTURE OPERATOR
        //
        // BATCH CAPTURE
        //
        // OUTWARD SESSION CHECK REQUIRED
        // =================================================

        case "Capture Operator":

            redirectOutwardUser(
                    "/zul/outward/outward-maker/"
                    + "capture-operator-batch-capture.zul"
            );

            break;


        // =================================================
        // UNKNOWN ROLE
        // =================================================

        default:

            loginMessage.setValue(
                    "User role is not configured."
            );

            break;
        }
    }


    // =====================================================
    // OUTWARD USER REDIRECTION
    //
    // THIS METHOD IS CALLED ONLY FOR:
    //
    // 1. Outward Maker
    // 2. Outward Checker
    // 3. Capture Operator
    // =====================================================

    private void redirectOutwardUser(
            String dashboardPath) {


        // =================================================
        // CHECK OUTWARD CLEARING SESSION
        // =================================================

        if (isOutwardSessionActive()) {


            // =============================================
            // SESSION STARTED
            //
            // GO TO REQUESTED OUTWARD PAGE
            // =============================================

            Executions.sendRedirect(
                    dashboardPath
            );

        } else {


            // =============================================
            // SESSION NOT STARTED
            //
            // GO TO WAITING PAGE
            // =============================================

            Executions.sendRedirect(
                    "/zul/outward/outward-maker/"
                    + "outward-clearing-session-wait.zul"
            );
        }
    }


    // =====================================================
    // CHECK OUTWARD CLEARING SESSION
    // =====================================================

    private boolean isOutwardSessionActive() {

        try {


            // =================================================
            // GET ACTIVE SESSION
            // =================================================

            com.cts.admin.model.Session activeSession =
                    sessionService.getActiveSession();


            // =================================================
            // NO ACTIVE SESSION
            // =================================================

            if (activeSession == null) {

                return false;
            }


            // =================================================
            // GET SESSION STATUS
            // =================================================

            String status =
                    activeSession.getStatus();


            // =================================================
            // SESSION IS ACTIVE ONLY WHEN STATUS = STARTED
            // =================================================

            return status != null
                    && "STARTED".equalsIgnoreCase(
                            status.trim()
                    );


        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }
}