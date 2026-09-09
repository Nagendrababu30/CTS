package com.cts.admin.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Window;

import com.cts.admin.model.User;
import com.cts.admin.service.SessionService;
import com.cts.admin.service.SessionServiceImpl;

public class SessionManagementController
        extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    // =========================================================
    // PAGE COMPONENTS
    // =========================================================

    private Vlayout currentSessionCard;
    private Label   sessionStatusBadge;
    private Vlayout closedSessionContent;
    private Vlayout activeSessionContent;
    private Label   activeSessionName;
    private Button  beginSessionButton;
    private Button  endSessionButton;
    private Listbox sessionHistoryListbox;
    private Paging  sessionHistoryPaging;

    private static final int PAGE_SIZE = 5;

    // =========================================================
    // END SESSION MODAL
    // =========================================================

    private Window endSessionModal;
    private Button modalCloseButton;
    private Button modalCancelButton;
    private Button modalConfirmButton;

    // =========================================================
    // SERVICE
    // =========================================================

    private SessionService sessionService;

    // =========================================================
    // COMPOSE
    // =========================================================

    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

        sessionService = new SessionServiceImpl();

        currentSessionCard    = (Vlayout) comp.getFellow("currentSessionCard");
        sessionStatusBadge    = (Label)   comp.getFellow("sessionStatusBadge");
        closedSessionContent  = (Vlayout) comp.getFellow("closedSessionContent");
        activeSessionContent  = (Vlayout) comp.getFellow("activeSessionContent");
        activeSessionName     = (Label)   comp.getFellow("activeSessionName");
        beginSessionButton    = (Button)  comp.getFellow("beginSessionButton");
        endSessionButton      = (Button)  comp.getFellow("endSessionButton");
        sessionHistoryListbox = (Listbox) comp.getFellow("sessionHistoryListbox");
        sessionHistoryPaging  = (Paging)  comp.getFellow("sessionHistoryPaging");
        sessionHistoryPaging.setPageSize(PAGE_SIZE);

        endSessionModal   = (Window) comp.getFellow("endSessionModal");
        modalCloseButton  = (Button) endSessionModal.getFellow("modalCloseButton");
        modalCancelButton = (Button) endSessionModal.getFellow("modalCancelButton");
        modalConfirmButton = (Button) endSessionModal.getFellow("modalConfirmButton");

        registerEvents();
        loadSessionState();
        loadSessionHistory(0);
    }

    // =========================================================
    // GET CURRENT USER ID FROM SESSION
    // =========================================================

    private Long getCurrentUserId() {

        org.zkoss.zk.ui.Session zkSession =
                org.zkoss.zk.ui.Executions.getCurrent().getSession();

        User loggedInUser = (User) zkSession.getAttribute("loggedInUser");

        if (loggedInUser != null) {
            return loggedInUser.getUserId();
        }

        return 1L;
    }

    // =========================================================
    // REGISTER EVENTS
    // =========================================================

    private void registerEvents() {

        beginSessionButton.addEventListener(Events.ON_CLICK,
                new EventListener<Event>() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        startSession();
                    }
                });

        endSessionButton.addEventListener(Events.ON_CLICK,
                new EventListener<Event>() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        openEndSessionModal();
                    }
                });

        modalCloseButton.addEventListener(Events.ON_CLICK,
                new EventListener<Event>() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        closeEndSessionModal();
                    }
                });

        modalCancelButton.addEventListener(Events.ON_CLICK,
                new EventListener<Event>() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        closeEndSessionModal();
                    }
                });

        modalConfirmButton.addEventListener(Events.ON_CLICK,
                new EventListener<Event>() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        endSession();
                    }
                });

        sessionHistoryPaging.addEventListener("onPaging",
                new EventListener<Event>() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        int activePage = sessionHistoryPaging.getActivePage();
                        loadSessionHistory(activePage * PAGE_SIZE);
                    }
                });
    }

    // =========================================================
    // START SESSION
    // =========================================================

    private void startSession() {

        try {

            sessionService.startSession(getCurrentUserId());

            Messagebox.show(
                    "Internal processing session started successfully.",
                    "Session Started",
                    Messagebox.OK,
                    Messagebox.INFORMATION);

            loadSessionState();
            loadSessionHistory(0);

        } catch (IllegalStateException e) {

            Messagebox.show(
                    e.getMessage(),
                    "Session Already Active",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            loadSessionState();

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to start the internal processing session.",
                    "Error",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    // =========================================================
    // OPEN END SESSION MODAL
    // =========================================================

    private void openEndSessionModal() {

        com.cts.admin.model.Session activeSession =
                sessionService.getActiveSession();

        if (activeSession == null) {

            Messagebox.show(
                    "There is no active internal processing session.",
                    "No Active Session",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            loadSessionState();
            return;
        }

        endSessionModal.setVisible(true);
        endSessionModal.doModal();
    }

    // =========================================================
    // CLOSE END SESSION MODAL
    // =========================================================

    private void closeEndSessionModal() {

        if (endSessionModal != null) {
            endSessionModal.setVisible(false);
        }
    }

    // =========================================================
    // END SESSION
    // =========================================================

    private void endSession() {

        try {

            com.cts.admin.model.Session activeSession =
                    sessionService.getActiveSession();

            if (activeSession == null) {

                closeEndSessionModal();

                Messagebox.show(
                        "There is no active internal processing session.",
                        "No Active Session",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION);

                loadSessionState();
                loadSessionHistory(0);
                return;
            }

            boolean ended = sessionService.endSession(
                    activeSession.getSessionId(),
                    getCurrentUserId());

            closeEndSessionModal();

            if (ended) {

                Messagebox.show(
                        "Internal processing session ended successfully.",
                        "Session Ended",
                        Messagebox.OK,
                        Messagebox.INFORMATION);

            } else {

                Messagebox.show(
                        "Unable to end the internal processing session.",
                        "Error",
                        Messagebox.OK,
                        Messagebox.ERROR);
            }

            loadSessionState();
            loadSessionHistory(0);

        } catch (Exception e) {

            e.printStackTrace();
            closeEndSessionModal();

            Messagebox.show(
                    "Unable to end the internal processing session.",
                    "Error",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    // =========================================================
    // LOAD CURRENT SESSION STATE
    // =========================================================

    private void loadSessionState() {

        com.cts.admin.model.Session activeSession =
                sessionService.getActiveSession();

        if (activeSession == null) {

            closedSessionContent.setVisible(true);
            activeSessionContent.setVisible(false);
            sessionStatusBadge.setValue("NO ACTIVE SESSION");
            sessionStatusBadge.setSclass("status-badge status-inactive");
            return;
        }

        closedSessionContent.setVisible(false);
        activeSessionContent.setVisible(true);
        sessionStatusBadge.setValue("ACTIVE");
        sessionStatusBadge.setSclass("status-badge status-active");

        activeSessionName.setValue(
                activeSession.getSessionName() != null
                        ? activeSession.getSessionName()
                        : "Clearing Session");
    }

    // =========================================================
    // LOAD SESSION HISTORY
    // =========================================================

    private void loadSessionHistory(int offset) {

        sessionHistoryListbox.getItems().clear();

        int total = sessionService.getSessionCount();
        sessionHistoryPaging.setTotalSize(total);

        List<com.cts.admin.model.Session> sessions =
                sessionService.getAllSessions(PAGE_SIZE, offset);

        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        for (com.cts.admin.model.Session session : sessions) {

            Listitem item = new Listitem();

            item.appendChild(createCell(
                    session.getSessionId() != null
                            ? String.valueOf(session.getSessionId()) : "-"));

            item.appendChild(createCell(
                    session.getStartedAt() != null
                            ? formatDate(session.getStartedAt()) : "-"));

            item.appendChild(createCell(
                    session.getStartedAt() != null
                            ? formatTime(session.getStartedAt()) : "-"));

            item.appendChild(createCell(
                    session.getEndedAt() != null
                            ? formatDate(session.getEndedAt()) : "-"));

            item.appendChild(createCell(
                    session.getEndedAt() != null
                            ? formatTime(session.getEndedAt()) : "-"));

            item.appendChild(createCell(
                    session.getStatus() != null
                            ? session.getStatus() : "-"));

            sessionHistoryListbox.appendChild(item);
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private Listcell createCell(String value) {
        Listcell cell = new Listcell();
        Label label = new Label();
        label.setValue(value);
        cell.appendChild(label);
        return cell;
    }

    private static final java.util.TimeZone IST =
            java.util.TimeZone.getTimeZone("Asia/Kolkata");

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
        sdf.setTimeZone(IST);
        return sdf.format(date);
    }

    private String formatTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
        sdf.setTimeZone(IST);
        return sdf.format(date);
    }

    private String formatDateTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm a");
        sdf.setTimeZone(IST);
        return sdf.format(date);
    }
}
