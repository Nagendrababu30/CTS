package com.iispl.cts.controller.outward;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Timer;

import com.cts.admin.service.SessionService;
import com.cts.admin.service.SessionServiceImpl;

public class OutwardClearingSessionWaitController extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;
    private static final int WAIT_SECONDS = 4 * 60;

    @Wire
    private Label timerLabel;

    @Wire
    private Button tryLaterButton;

    @Wire
    private Button logoutButton;

    private Timer sessionTimer;
    private SessionService sessionService;
    private int remainingSeconds = WAIT_SECONDS;    
    // intitalize screen 

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        sessionService = new SessionServiceImpl();

        Session session = Executions.getCurrent().getSession();

        if (session == null) {
            redirectToLogin();
            return;
        }

        Object userId = session.getAttribute("userId");

        if (userId == null) {
            redirectToLogin();
            return;
        }

        // check clearing session status
        if (isSessionActive()) {
            redirectToDashboard();
            return;
        }

        createTimer(comp);
        updateTimer();
    }

    // create session timer
    private void createTimer(Component parent) {
        sessionTimer = new Timer();
        sessionTimer.setDelay(1000);
        sessionTimer.setRepeats(true);

        sessionTimer.addEventListener(
                "onTimer",
                new EventListener<Event>() {
                    @Override
                    public void onEvent(Event event) {
                        handleTimer();
                    }
                }
        );

        sessionTimer.setParent(parent);
        sessionTimer.start();
    }

    // handle timer countdown
    private void handleTimer() {
        if (remainingSeconds > 0) {
            remainingSeconds--;
            updateTimer();
            return;
        }

        checkSession();
    }

    // check session from Try Later
    @Listen("onClick = #tryLaterButton")
    public void onTryLater(Event event) {
        checkSession();
    }

    // logout current user
    @Listen("onClick = #logoutButton")
    public void onLogout(Event event) {
        logout();
    }

    // check clearing session
    private void checkSession() {
        if (isSessionActive()) {
            stopTimer();
            redirectToDashboard();
            return;
        }

        remainingSeconds = WAIT_SECONDS;
        updateTimer();
    }

    // check whether clearing session is active
    private boolean isSessionActive() {
        try {
            com.cts.admin.model.Session activeSession =
                    sessionService.getActiveSession();

            if (activeSession == null) {
                return false;
            }

            String status = activeSession.getStatus();

            return status != null
                    && "STARTED".equalsIgnoreCase(status.trim());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // update countdown display
    private void updateTimer() {
        if (timerLabel == null) {
            return;
        }

        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;

        timerLabel.setValue(
                String.format("%02d:%02d", minutes, seconds)
        );
    }

    // stop session timer
    private void stopTimer() {
        if (sessionTimer != null) {
            sessionTimer.stop();
            sessionTimer = null;
        }
    }

    // redirect user based on role
    private void redirectToDashboard() {
        Session session = Executions.getCurrent().getSession();

        if (session == null) {
            redirectToLogin();
            return;
        }

        Object roleName = session.getAttribute("roleName");

        if (roleName == null) {
            redirectToLogin();
            return;
        }

        String role = roleName.toString().trim();

        if ("Outward Maker".equalsIgnoreCase(role)) {
            Executions.sendRedirect(
                    "/zul/outward/outward-maker/outward-maker-dashboard.zul"
            );
            return;
        }

        if ("Outward Checker".equalsIgnoreCase(role)) {
            Executions.sendRedirect(
                    "/zul/outward/outward-checker/dashboard.zul"
            );
            return;
        }

        if ("Capture Operator".equalsIgnoreCase(role)) {
            Executions.sendRedirect(
                    "/zul/outward/outward-maker/capture-operator-batch-capture.zul"
            );
            return;
        }

        redirectToLogin();
    }

    // logout current user
    private void logout() {
        stopTimer();

        Session session = Executions.getCurrent().getSession();

        if (session != null) {
            session.invalidate();
        }

        redirectToLogin();
    }

    // redirect to login
    private void redirectToLogin() {
        Executions.sendRedirect("/login.zul");
    }
}