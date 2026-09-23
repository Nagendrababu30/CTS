 package com.cts.inward.model;

public class CheckerBatch {

    private long lockId;

    private long batchId;

    private int totalCheques;

    // Maker username
    private String maker;

    // Checker ID - used internally
    private Long userId;

    // Checker username - displayed in UI
    private String checkerName;

    private String lockStatus;

    private String batchStatus;

    private boolean reVerify;


    public long getLockId() {
        return lockId;
    }

    public void setLockId(long lockId) {
        this.lockId = lockId;
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


    public String getMaker() {
        return maker;
    }

    public void setMaker(String maker) {
        this.maker = maker;
    }


    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }


    public String getCheckerName() {
        return checkerName;
    }

    public void setCheckerName(String checkerName) {
        this.checkerName = checkerName;
    }


    public String getLockStatus() {
        return lockStatus;
    }

    public void setLockStatus(String lockStatus) {
        this.lockStatus = lockStatus;
    }


    public String getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(String batchStatus) {
        this.batchStatus = batchStatus;
    }


    public boolean isReVerify() {
        return reVerify;
    }

    public void setReVerify(boolean reVerify) {
        this.reVerify = reVerify;
    }
}