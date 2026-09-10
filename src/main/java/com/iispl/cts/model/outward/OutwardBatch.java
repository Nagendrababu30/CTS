package com.iispl.cts.model.outward;

import java.io.Serializable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OutwardBatch implements Serializable {

    private static final long serialVersionUID = 1L;

    // =========================
    // PRIMARY KEY
    // =========================

    private String batchNumber;

    // =========================
    // BRANCH
    // =========================

    private String branchCode;

    // =========================
    // BATCH DETAILS
    // =========================

    private Integer numberOfCheques;

    private BigDecimal totalAmount;

    private String batchFolderPath;

    private String xmlFilePath;

    // =========================
    // AUDIT
    // =========================

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    // =========================
    // BATCH STATUS
    // =========================

    private String batchStatus;

    // =========================
    // MAKER
    // =========================

    private String makerUserNumber;

    private LocalDateTime makerStartedAt;

    private LocalDateTime makerCompletedAt;

    // =========================
    // CHECKER
    // =========================

    private String checkerUserNumber;

    private LocalDateTime checkerStartedAt;

    private LocalDateTime checkerCompletedAt;

    // =========================
    // LOCK
    // =========================

    private String lockedBy;

    private LocalDateTime lockedAt;

    private String lockStatus;


    // =========================
    // CONSTRUCTORS
    // =========================

    public OutwardBatch() {
    }

    public OutwardBatch(String batchNumber) {
        this.batchNumber = batchNumber;
    }


    // =========================
    // GETTERS / SETTERS
    // =========================

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public Integer getNumberOfCheques() {
        return numberOfCheques;
    }

    public void setNumberOfCheques(Integer numberOfCheques) {
        this.numberOfCheques = numberOfCheques;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getBatchFolderPath() {
        return batchFolderPath;
    }

    public void setBatchFolderPath(String batchFolderPath) {
        this.batchFolderPath = batchFolderPath;
    }

    public String getXmlFilePath() {
        return xmlFilePath;
    }

    public void setXmlFilePath(String xmlFilePath) {
        this.xmlFilePath = xmlFilePath;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(String batchStatus) {
        this.batchStatus = batchStatus;
    }

    public String getMakerUserNumber() {
        return makerUserNumber;
    }

    public void setMakerUserNumber(String makerUserNumber) {
        this.makerUserNumber = makerUserNumber;
    }

    public LocalDateTime getMakerStartedAt() {
        return makerStartedAt;
    }

    public void setMakerStartedAt(LocalDateTime makerStartedAt) {
        this.makerStartedAt = makerStartedAt;
    }

    public LocalDateTime getMakerCompletedAt() {
        return makerCompletedAt;
    }

    public void setMakerCompletedAt(LocalDateTime makerCompletedAt) {
        this.makerCompletedAt = makerCompletedAt;
    }

    public String getCheckerUserNumber() {
        return checkerUserNumber;
    }

    public void setCheckerUserNumber(String checkerUserNumber) {
        this.checkerUserNumber = checkerUserNumber;
    }

    public LocalDateTime getCheckerStartedAt() {
        return checkerStartedAt;
    }

    public void setCheckerStartedAt(LocalDateTime checkerStartedAt) {
        this.checkerStartedAt = checkerStartedAt;
    }

    public LocalDateTime getCheckerCompletedAt() {
        return checkerCompletedAt;
    }

    public void setCheckerCompletedAt(LocalDateTime checkerCompletedAt) {
        this.checkerCompletedAt = checkerCompletedAt;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public String getLockStatus() {
        return lockStatus;
    }

    public void setLockStatus(String lockStatus) {
        this.lockStatus = lockStatus;
    }

    @Override
    public String toString() {
        return "OutwardBatch{" +
                "batchNumber='" + batchNumber + '\'' +
                ", branchCode='" + branchCode + '\'' +
                ", numberOfCheques=" + numberOfCheques +
                ", totalAmount=" + totalAmount +
                ", batchStatus='" + batchStatus + '\'' +
                ", makerUserNumber='" + makerUserNumber + '\'' +
                ", checkerUserNumber='" + checkerUserNumber + '\'' +
                ", lockStatus='" + lockStatus + '\'' +
                '}';
    }

	public void setMakerAssignmentStatus(String assignmentStatus) {
		// TODO Auto-generated method stub
		
	}
}