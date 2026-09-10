package com.iispl.cts.service.outward;

import java.util.List;

import com.iispl.cts.dao.outward.NotificationDAO;
import com.iispl.cts.model.outward.Notification;

public class NotificationService {

    // =========================================================
    // NOTIFICATION TYPES
    // =========================================================

    public static final String NEW_BATCH =
            "NEW_BATCH";

    public static final String BATCH_ASSIGNED =
            "BATCH_ASSIGNED";

    public static final String CHEQUE_REJECTED =
            "CHEQUE_REJECTED";

    public static final String BATCH_PASSED =
            "BATCH_PASSED";

    public static final String BATCH_READY_FOR_CHECKER =
            "BATCH_READY_FOR_CHECKER";

    public static final String BATCH_SENT_TO_NPCI =
            "BATCH_SENT_TO_NPCI";

    public static final String DATA_ENTRY_ERROR =
            "DATA_ENTRY_ERROR";

    public static final String AMOUNT_ACCOUNT_ERROR =
            "AMOUNT_ACCOUNT_ERROR";


    // =========================================================
    // DAO
    // =========================================================

    private final NotificationDAO dao;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public NotificationService() {

        dao = new NotificationDAO();
    }


    // =========================================================
    // CREATE NOTIFICATION
    // =========================================================

    public void notifyUser(
            int userId,
            String batchNumber,
            String chequeNumber,
            String notificationType,
            String message) throws Exception {

        if (userId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid notification user.");
        }

        if (notificationType == null ||
                notificationType.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Notification type is required.");
        }

        if (message == null ||
                message.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Notification message is required.");
        }

        dao.createNotification(
                userId,
                batchNumber,
                chequeNumber,
                notificationType,
                message);
    }


    // =========================================================
    // GET USER NOTIFICATIONS
    // =========================================================

    public List<Notification> getNotifications(
            int userId) throws Exception {

        if (userId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid user.");
        }

        return dao.getNotifications(userId);
    }


    // =========================================================
    // GET UNREAD COUNT
    // =========================================================

    public int getUnreadCount(
            int userId) throws Exception {

        if (userId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid user.");
        }

        return dao.getUnreadCount(userId);
    }


    // =========================================================
    // MARK ONE NOTIFICATION AS READ
    // =========================================================

    public void markAsRead(
            long notificationId,
            int userId) throws Exception {

        if (notificationId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid notification.");
        }

        if (userId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid user.");
        }

        dao.markAsRead(
                notificationId,
                userId);
    }


    // =========================================================
    // MARK ALL NOTIFICATIONS AS READ
    // =========================================================

    public void markAllAsRead(
            int userId) throws Exception {

        if (userId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid user.");
        }

        dao.markAllAsRead(userId);
    }


    // =========================================================
    // FIND ACTIVE USERS DYNAMICALLY
    // =========================================================

    public List<Integer> getActiveUserIdsByRole(
            int roleId) throws Exception {

        if (roleId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid role.");
        }

        return dao.getActiveUserIdsByRole(
                roleId);
    }
}