package com.cts.admin.controller;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Popup;

import com.cts.admin.model.User;
import com.cts.admin.service.AuditLogService;
import com.cts.admin.service.AuditLogServiceImpl;
import com.cts.admin.service.SessionService;
import com.cts.admin.service.SessionServiceImpl;

public class LogoutComposer extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    private static final java.util.TimeZone IST =
            java.util.TimeZone.getTimeZone("Asia/Kolkata");

    private Div userWrapper;
    private Popup logoutPopup;
    private Label headerDate;
    private Label headerLastLogin;
    private Label headerUserName;
    private Label headerSessionStatus;

    private AuditLogService auditLogService;
    private SessionService sessionService;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        auditLogService = new AuditLogServiceImpl();
        sessionService = new SessionServiceImpl();

        SimpleDateFormat dateFmt =
                new SimpleDateFormat("dd/MM/yyyy");

        dateFmt.setTimeZone(IST);
        headerDate.setValue(dateFmt.format(new Date()));

        Session zkSession =
                Executions.getCurrent().getSession();

        User loggedInUser =
                (User) zkSession.getAttribute("loggedInUser");

        if (loggedInUser != null) {

            if (loggedInUser.getLastLogin() != null) {

                SimpleDateFormat loginFmt =
                        new SimpleDateFormat(
                                "dd/MM/yyyy hh:mm a");

                loginFmt.setTimeZone(IST);

                headerLastLogin.setValue(
                        loginFmt.format(
                                loggedInUser.getLastLogin()));

            } else {
                headerLastLogin.setValue("-");
            }

            String userName =
                    loggedInUser.getUsername();

            if (userName != null
                    && !userName.trim().isEmpty()) {

                headerUserName.setValue(
                        userName.trim());

            } else {
                headerUserName.setValue("-");
            }

        } else {

            headerLastLogin.setValue("-");
            headerUserName.setValue("-");
        }

        com.cts.admin.model.Session activeSession =
                sessionService.getActiveSession();

        if (activeSession != null
                && activeSession.getStatus() != null
                && !activeSession.getStatus()
                        .trim()
                        .isEmpty()) {

        	headerSessionStatus.setValue("● Active");
        	headerSessionStatus.setStyle(
        	    "display:inline-flex !important;"
        	  + "align-items:center !important;"
        	  + "gap:6px !important;"
        	  + "padding:4px 12px !important;"
        	  + "border-radius:9999px !important;"
        	  + "background:#dcfce7 !important;"
        	  + "color:#15803d !important;"
        	  + "font-size:12px !important;"
        	  + "font-weight:600 !important;"
        	  + "letter-spacing:0.3px !important;"
        	  + "line-height:1.4 !important;"
        	  + "box-shadow:0 0 0 1px rgba(22,163,74,0.15) inset !important;"
        	);


        } else {

        	headerSessionStatus.setValue("● Inactive");
        	headerSessionStatus.setStyle(
        	    "display:inline-flex !important;"
        	  + "align-items:center !important;"
        	  + "gap:6px !important;"
        	  + "padding:4px 12px !important;"
        	  + "border-radius:9999px !important;"
        	  + "background:#fee2e2 !important;"
        	  + "color:#b91c1c !important;"
        	  + "font-size:12px !important;"
        	  + "font-weight:600 !important;"
        	  + "letter-spacing:0.3px !important;"
        	  + "line-height:1.4 !important;"
        	  + "box-shadow:0 0 0 1px rgba(220,38,38,0.15) inset !important;"
        	);
        }
    }

    public void onClick$userWrapper() {

        if (logoutPopup != null
                && !logoutPopup.isVisible()) {

            logoutPopup.open(
                    userWrapper,
                    "after_end");

        } else if (logoutPopup != null) {

            logoutPopup.close();
        }
    }

    public void onClick$logoutButton() {

        Session session =
                Executions.getCurrent().getSession();

        if (session != null) {

            String auditSessionId =
                    (String) session.getAttribute(
                            "auditSessionId");

            if (auditSessionId != null) {

                try {

                    auditLogService.endAuditLog(
                            auditSessionId);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            session.invalidate();
        }

        Executions.sendRedirect(
                "/login.zul");
    }
}