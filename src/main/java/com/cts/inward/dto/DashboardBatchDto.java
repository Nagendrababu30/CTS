 package com.cts.inward.dto;

public class DashboardBatchDto {

    private long batchId;

    private int totalCheques;

    private String batchStatus;

    /*
     * User ID is still required internally
     * for ownership checking.
     */
    private Long lockUserId;

    /*
     * User name is used only for displaying
     * the maker name on the dashboard.
     */
    private String lockUserName;

    private String lockStatus;

    public DashboardBatchDto() {

    }

    public DashboardBatchDto(
            long batchId,
            int totalCheques,
            String batchStatus,
            Long lockUserId,
            String lockUserName,
            String lockStatus) {

        this.batchId = batchId;
        this.totalCheques = totalCheques;
        this.batchStatus = batchStatus;
        this.lockUserId = lockUserId;
        this.lockUserName = lockUserName;
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

    public String getLockUserName() {

        return lockUserName;
    }

    public void setLockUserName(String lockUserName) {

        this.lockUserName = lockUserName;
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
                + ", lockUserName="
                + lockUserName
                + ", lockStatus="
                + lockStatus
                + "]";
    }
}