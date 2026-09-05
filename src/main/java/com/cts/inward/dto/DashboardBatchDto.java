package com.cts.inward.dto;

public class DashboardBatchDto {

    private long batchId;
    private int totalCheques;

    private String batchStatus;

    private Long lockUserId;
    private String lockStatus;

    public DashboardBatchDto() {
    }

    public DashboardBatchDto(
            long batchId,
            int totalCheques,
            String batchStatus,
            Long lockUserId,
            String lockStatus) {

        this.batchId = batchId;
        this.totalCheques = totalCheques;
        this.batchStatus = batchStatus;
        this.lockUserId = lockUserId;
        this.lockStatus = lockStatus;
    }

    public long getBatchId() {
        return batchId;
    }

    public void setBatchId(long batchId) {
        this.batchId = batchId;
    }

    public int getTotalCheques() {
        return totalCheques;
    }

    public void setTotalCheques(int totalCheques) {
        this.totalCheques = totalCheques;
    }

    public String getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(String batchStatus) {
        this.batchStatus = batchStatus;
    }

    public Long getLockUserId() {
        return lockUserId;
    }

    public void setLockUserId(Long lockUserId) {
        this.lockUserId = lockUserId;
    }

    public String getLockStatus() {
        return lockStatus;
    }

    public void setLockStatus(String lockStatus) {
        this.lockStatus = lockStatus;
    }

    @Override
    public String toString() {
        return "DashboardBatchDto [batchId="
                + batchId
                + ", totalCheques="
                + totalCheques
                + ", batchStatus="
                + batchStatus
                + ", lockUserId="
                + lockUserId
                + ", lockStatus="
                + lockStatus
                + "]";
    }
}