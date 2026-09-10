package com.iispl.cts.dao.outward;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.iispl.cts.data.CTSStaticData;
import com.iispl.cts.model.outward.Notification;

public class NotificationDAO {

    // =========================================================
    // CREATE NOTIFICATION
    // =========================================================

    public void createNotification(
            int userId,
            String batchNumber,
            String chequeNumber,
            String notificationType,
            String message) throws Exception {

        String sql =
                "INSERT INTO public.notification " +
                "(user_id, batch_number, cheque_number, " +
                "notification_type, message) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (
                Connection con = CTSStaticData.getConnection();
                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setInt(1, userId);
            ps.setString(2, batchNumber);
            ps.setString(3, chequeNumber);
            ps.setString(4, notificationType);
            ps.setString(5, message);

            ps.executeUpdate();
        }
    }


    // =========================================================
    // GET USER NOTIFICATIONS
    // =========================================================

    public List<Notification> getNotifications(
            int userId) throws Exception {

        List<Notification> notifications =
                new ArrayList<>();

        String sql =
                "SELECT notification_id, " +
                "user_id, " +
                "batch_number, " +
                "cheque_number, " +
                "notification_type, " +
                "message, " +
                "is_read, " +
                "created_at, " +
                "read_at " +
                "FROM public.notification " +
                "WHERE user_id = ? " +
                "ORDER BY created_at DESC";

        try (
                Connection con = CTSStaticData.getConnection();
                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    Notification notification =
                            new Notification();

                    notification.setNotificationId(
                            rs.getLong("notification_id"));

                    notification.setUserId(
                            rs.getInt("user_id"));

                    notification.setBatchNumber(
                            rs.getString("batch_number"));

                    notification.setChequeNumber(
                            rs.getString("cheque_number"));

                    notification.setNotificationType(
                            rs.getString("notification_type"));

                    notification.setMessage(
                            rs.getString("message"));

                    notification.setRead(
                            rs.getBoolean("is_read"));

                    Timestamp createdAt =
                            rs.getTimestamp("created_at");

                    if (createdAt != null) {

                        notification.setCreatedAt(
                                createdAt.toLocalDateTime());
                    }

                    Timestamp readAt =
                            rs.getTimestamp("read_at");

                    if (readAt != null) {

                        notification.setReadAt(
                                readAt.toLocalDateTime());
                    }

                    notifications.add(
                            notification);
                }
            }
        }

        return notifications;
    }


    // =========================================================
    // GET UNREAD COUNT
    // =========================================================

    public int getUnreadCount(
            int userId) throws Exception {

        String sql =
                "SELECT COUNT(*) " +
                "FROM public.notification " +
                "WHERE user_id = ? " +
                "AND is_read = FALSE";

        try (
                Connection con = CTSStaticData.getConnection();
                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }


    // =========================================================
    // MARK ONE AS READ
    // =========================================================

    public void markAsRead(
            long notificationId,
            int userId) throws Exception {

        String sql =
                "UPDATE public.notification " +
                "SET is_read = TRUE, " +
                "read_at = CURRENT_TIMESTAMP " +
                "WHERE notification_id = ? " +
                "AND user_id = ?";

        try (
                Connection con = CTSStaticData.getConnection();
                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setLong(1, notificationId);
            ps.setInt(2, userId);

            ps.executeUpdate();
        }
    }


    // =========================================================
    // MARK ALL AS READ
    // =========================================================

    public void markAllAsRead(
            int userId) throws Exception {

        String sql =
                "UPDATE public.notification " +
                "SET is_read = TRUE, " +
                "read_at = CURRENT_TIMESTAMP " +
                "WHERE user_id = ? " +
                "AND is_read = FALSE";

        try (
                Connection con = CTSStaticData.getConnection();
                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setInt(1, userId);

            ps.executeUpdate();
        }
    }


    // =========================================================
    // FIND ACTIVE USERS DYNAMICALLY
    // =========================================================

    public List<Integer> getActiveUserIdsByRole(
            int roleId) throws Exception {

        List<Integer> userIds =
                new ArrayList<>();

        String sql =
                "SELECT user_id " +
                "FROM public.\"user\" " +
                "WHERE role_id = ? " +
                "AND status = 'ACTIVE' " +
                "ORDER BY user_id";

        try (
                Connection con = CTSStaticData.getConnection();
                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setInt(1, roleId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    userIds.add(
                            rs.getInt("user_id"));
                }
            }
        }

        return userIds;
    }
}