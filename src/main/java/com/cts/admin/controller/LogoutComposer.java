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

public class LogoutComposer extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    private static final java.util.TimeZone IST =
            java.util.TimeZone.getTimeZone("Asia/Kolkata");

    private Div   userWrapper;
    private Popup logoutPopup;
    private Label headerDate;
    private Label headerLastLogin;

    private AuditLogService auditLogService;

    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

        auditLogService = new AuditLogServiceImpl();

        /* Today's date */
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy");
        dateFmt.setTimeZone(IST);
        headerDate.setValue(dateFmt.format(new Date()));

        /* Last login from session */
        Session zkSession = Executions.getCurrent().getSession();
        User loggedInUser = (User) zkSession.getAttribute("loggedInUser");

        if (loggedInUser != null && loggedInUser.getLastLogin() != null) {
            SimpleDateFormat loginFmt = new SimpleDateFormat("dd/MM/yyyy hh:mm a");
            loginFmt.setTimeZone(IST);
            headerLastLogin.setValue(loginFmt.format(loggedInUser.getLastLogin()));
        } else {
            headerLastLogin.setValue("-");
        }
    }

    public void onClick$userWrapper() {

        if (logoutPopup != null && !logoutPopup.isVisible()) {
            logoutPopup.open(userWrapper, "after_end");
        } else if (logoutPopup != null) {
            logoutPopup.close();
        }
    }

    public void onClick$logoutButton() {

        Session session = Executions.getCurrent().getSession();

        if (session != null) {
            /* End audit log entry via AuditLogService */
            String auditSessionId = (String) session.getAttribute("auditSessionId");
            if (auditSessionId != null) {
                try {
                    auditLogService.endAuditLog(auditSessionId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            session.invalidate();
        }

        Executions.sendRedirect("/login.zul");
    }
}