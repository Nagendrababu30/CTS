package com.iispl.cts.controller.outward;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Popup;

import com.iispl.cts.model.outward.Notification;
import com.iispl.cts.model.outward.UserSession;
import com.iispl.cts.service.outward.NotificationService;

public class HeaderController
        extends SelectorComposer<org.zkoss.zk.ui.Component> {

    private static final long serialVersionUID = 1L;

    @Wire
    private Label dateLabel;

    @Wire
    private Label lastLoginLabel;

    @Wire
    private Label usernameLabel;

    @Wire
    private Label notificationCountLabel;

    @Wire
    private Listbox notificationListbox;

    @Wire
    private Popup notificationPopup;

    private NotificationService notificationService;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");


    // =========================================================
    // AFTER COMPOSE
    // =========================================================

    @Override
    public void doAfterCompose(
            org.zkoss.zk.ui.Component comp) throws Exception {

        super.doAfterCompose(comp);

        UserSession sessionUser =
                LoginController.getCurrentUserSession();

        if (sessionUser == null) {

            Executions.sendRedirect("/login.zul");

            return;
        }

        notificationService =
                new NotificationService();

        // -----------------------------------------------------
        // CURRENT DATE
        // -----------------------------------------------------

        LocalDateTime now =
                LocalDateTime.now();

        dateLabel.setValue(
                "▣  Date : "
                + now.format(DATE_FORMAT));


        // -----------------------------------------------------
        // LAST LOGIN
        // -----------------------------------------------------

        LocalDateTime lastLogin =
                sessionUser.getLastLogin();

        if (lastLogin != null) {

            lastLoginLabel.setValue(
                    "◷  Last Login : "
                    + lastLogin.format(DATE_TIME_FORMAT));

        } else {

            lastLoginLabel.setValue(
                    "◷  Last Login : Not Available");
        }


        // -----------------------------------------------------
        // DYNAMIC USERNAME
        // -----------------------------------------------------

        usernameLabel.setValue(
                "♙  " + sessionUser.getUsername());


        // -----------------------------------------------------
        // LOAD NOTIFICATIONS
        // -----------------------------------------------------

        loadNotifications(
                sessionUser.getUserId());
    }


    // =========================================================
    // LOAD NOTIFICATIONS
    // =========================================================

    private void loadNotifications(
            int userId) throws Exception {

        List<Notification> notifications =
                notificationService
                        .getNotifications(userId);

        notificationListbox.getItems().clear();

        for (Notification notification : notifications) {

            Listitem item =
                    new Listitem();

            Listcell cell =
                    new Listcell();

            String message =
                    notification.getMessage();

            if (!notification.isRead()) {

                message =
                        "🔵 " + message;
            }

            cell.setLabel(message);

            item.appendChild(cell);

            item.setAttribute(
                    "notificationId",
                    notification.getNotificationId());

            item.setAttribute(
                    "read",
                    notification.isRead());

            notificationListbox.appendChild(item);
        }

        updateUnreadCount(userId);
    }


    // =========================================================
    // UPDATE UNREAD COUNT
    // =========================================================

    private void updateUnreadCount(
            int userId) throws Exception {

        int unreadCount =
                notificationService
                        .getUnreadCount(userId);

        if (unreadCount > 0) {

            notificationCountLabel.setValue(
                    String.valueOf(unreadCount));

            notificationCountLabel.setVisible(
                    true);

        } else {

            notificationCountLabel.setValue("0");

            notificationCountLabel.setVisible(
                    false);
        }
    }


    // =========================================================
    // OPEN NOTIFICATION POPUP
    // =========================================================

    @Listen("onClick=#notificationButton")
    public void openNotifications() {

        try {

            UserSession sessionUser =
                    LoginController
                            .getCurrentUserSession();

            if (sessionUser == null) {

                Executions.sendRedirect(
                        "/login.zul");

                return;
            }

            loadNotifications(
                    sessionUser.getUserId());

            notificationPopup.open(
                    notificationListbox,
                    "after_start");

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    // =========================================================
    // MARK ALL AS READ
    // =========================================================

    @Listen("onClick=#markAllReadButton")
    public void markAllAsRead() {

        try {

            UserSession sessionUser =
                    LoginController
                            .getCurrentUserSession();

            if (sessionUser == null) {

                Executions.sendRedirect(
                        "/login.zul");

                return;
            }

            notificationService.markAllAsRead(
                    sessionUser.getUserId());

            loadNotifications(
                    sessionUser.getUserId());

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    @Listen("onClick=#logoutButton")
    public void logout() {

        LoginController.logout();
    }
}