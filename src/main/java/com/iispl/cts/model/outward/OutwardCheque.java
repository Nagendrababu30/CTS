package com.iispl.cts.model.outward;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class OutwardCheque implements Serializable {

    private static final long serialVersionUID = 1L;

    // =========================
    // PRIMARY KEY
    // =========================

    private String batchNumber;
    private String chequeNumber;

    // =========================
    // MICR
    // =========================

    private String cityCode;
    private String bankCode;
    private String branchCode;

    // =========================
    // DRAWER
    // =========================

    private String drawerAccountNumber;
    private String drawerName;

    // =========================
    // DEPOSITOR
    // =========================

    private String depositorAccountNumber;
    private String depositorName;

    // =========================
    // PAYEE
    // =========================

    private String payeeName;
    private String payeeAccountNumber;

    // =========================
    // CHEQUE DETAILS
    // =========================

    private BigDecimal amount;
    private String amountInWords;
    private LocalDate chequeDate;

    // =========================
    // IMAGES
    // =========================

    private String frontImagePath;
    private String backImagePath;

    // =========================
    // STATUS
    // =========================

    private String chequeStatus;

    // =========================
    // AUDIT
    // =========================

    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    // =========================
    // CHECKER
    // =========================

    private Integer returnReasonId;
    private String checkerRemarks;

    // =========================
    // CONSTRUCTORS
    // =========================

    public OutwardCheque() {
    }

    public OutwardCheque(String batchNumber, String chequeNumber) {
        this.batchNumber = batchNumber;
        this.chequeNumber = chequeNumber;
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

    public String getChequeNumber() {
        return chequeNumber;
    }

    public void setChequeNumber(String chequeNumber) {
        this.chequeNumber = chequeNumber;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getDrawerAccountNumber() {
        return drawerAccountNumber;
    }

    public void setDrawerAccountNumber(String drawerAccountNumber) {
        this.drawerAccountNumber = drawerAccountNumber;
    }

    public String getDrawerName() {
        return drawerName;
    }

    public void setDrawerName(String drawerName) {
        this.drawerName = drawerName;
    }

    public String getDepositorAccountNumber() {
        return depositorAccountNumber;
    }

    public void setDepositorAccountNumber(String depositorAccountNumber) {
        this.depositorAccountNumber = depositorAccountNumber;
    }

    public String getDepositorName() {
        return depositorName;
    }

    public void setDepositorName(String depositorName) {
        this.depositorName = depositorName;
    }

    public String getPayeeName() {
        return payeeName;
    }

    public void setPayeeName(String payeeName) {
        this.payeeName = payeeName;
    }

    public String getPayeeAccountNumber() {
        return payeeAccountNumber;
    }

    public void setPayeeAccountNumber(String payeeAccountNumber) {
        this.payeeAccountNumber = payeeAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getAmountInWords() {
        return amountInWords;
    }

    public void setAmountInWords(String amountInWords) {
        this.amountInWords = amountInWords;
    }

    public LocalDate getChequeDate() {
        return chequeDate;
    }

    public void setChequeDate(LocalDate chequeDate) {
        this.chequeDate = chequeDate;
    }

    public String getFrontImagePath() {
        return frontImagePath;
    }

    public void setFrontImagePath(String frontImagePath) {
        this.frontImagePath = frontImagePath;
    }

    public String getBackImagePath() {
        return backImagePath;
    }

    public void setBackImagePath(String backImagePath) {
        this.backImagePath = backImagePath;
    }

    public String getChequeStatus() {
        return chequeStatus;
    }

    public void setChequeStatus(String chequeStatus) {
        this.chequeStatus = chequeStatus;
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

    public Integer getReturnReasonId() {
        return returnReasonId;
    }

    public void setReturnReasonId(Integer returnReasonId) {
        this.returnReasonId = returnReasonId;
    }

    public String getCheckerRemarks() {
        return checkerRemarks;
    }

    public void setCheckerRemarks(String checkerRemarks) {
        this.checkerRemarks = checkerRemarks;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    @Override
    public String toString() {
        return "OutwardCheque [batchNumber=" + batchNumber
                + ", chequeNumber=" + chequeNumber
                + ", cityCode=" + cityCode
                + ", bankCode=" + bankCode
                + ", branchCode=" + branchCode
                + ", drawerAccountNumber=" + drawerAccountNumber
                + ", drawerName=" + drawerName
                + ", depositorAccountNumber=" + depositorAccountNumber
                + ", depositorName=" + depositorName
                + ", payeeName=" + payeeName
                + ", payeeAccountNumber=" + payeeAccountNumber
                + ", amount=" + amount
                + ", amountInWords=" + amountInWords
                + ", chequeDate=" + chequeDate
                + ", frontImagePath=" + frontImagePath
                + ", backImagePath=" + backImagePath
                + ", chequeStatus=" + chequeStatus
                + ", createdBy=" + createdBy
                + ", createdAt=" + createdAt
                + ", updatedBy=" + updatedBy
                + ", updatedAt=" + updatedAt
                + ", returnReasonId=" + returnReasonId
                + ", checkerRemarks=" + checkerRemarks
                + "]";
    }
}