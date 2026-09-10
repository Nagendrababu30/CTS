package com.iispl.cts.model.outward;

import java.io.Serializable;

import java.time.LocalDateTime;

public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    private long notificationId;
    private int userId;

    private String batchNumber;
    private String chequeNumber;

    private String notificationType;
    private String message;

    private boolean read;

    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public Notification() {
    }

    public Notification(
            long notificationId,
            int userId,
            String batchNumber,
            String chequeNumber,
            String notificationType,
            String message,
            boolean read,
            LocalDateTime createdAt,
            LocalDateTime readAt) {

        this.notificationId = notificationId;
        this.userId = userId;
        this.batchNumber = batchNumber;
        this.chequeNumber = chequeNumber;
        this.notificationType = notificationType;
        this.message = message;
        this.read = read;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }

    public long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(long notificationId) {
        this.notificationId = notificationId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getChequeNumber() {
        return chequeNumber;
    }

    public void setChequeNumber(String chequeNumber) {
        this.chequeNumber = chequeNumber;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "notificationId=" + notificationId +
                ", userId=" + userId +
                ", batchNumber='" + batchNumber + '\'' +
                ", chequeNumber='" + chequeNumber + '\'' +
                ", notificationType='" + notificationType + '\'' +
                ", message='" + message + '\'' +
                ", read=" + read +
                ", createdAt=" + createdAt +
                ", readAt=" + readAt +
                '}';
    }
}