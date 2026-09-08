package com.cts.inward.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NpciChequeData {

    private long inwardChequeId;

    private String chequeNumber;
    private long batchId;
    private String accountNumber;
    private LocalDate chequeDate;
    private String drawerName;
    private BigDecimal chequeAmount;
    private String micrCode;
    private String cityCode;
    private String bankCode;
    private String branchCode;

    private NpciChequeData(
            String chequeNumber,
            long batchId,
            String accountNumber,
            LocalDate chequeDate,
            String drawerName,
            BigDecimal chequeAmount,
            String micrCode,
            String cityCode,
            String bankCode,
            String branchCode) {

        this.chequeNumber = chequeNumber;
        this.batchId = batchId;
        this.accountNumber = accountNumber;
        this.chequeDate = chequeDate;
        this.drawerName = drawerName;
        this.chequeAmount = chequeAmount;
        this.micrCode = micrCode;
        this.cityCode = cityCode;
        this.bankCode = bankCode;
        this.branchCode = branchCode;
    }

    public static NpciChequeData of(
            String chequeNumber,
            long batchId,
            String accountNumber,
            LocalDate chequeDate,
            String drawerName,
            BigDecimal chequeAmount,
            String micrCode,
            String cityCode,
            String bankCode,
            String branchCode) {

        return new NpciChequeData(
                chequeNumber,
                batchId,
                accountNumber,
                chequeDate,
                drawerName,
                chequeAmount,
                micrCode,
                cityCode,
                bankCode,
                branchCode);
    }

	public long getInwardChequeId() {
		return inwardChequeId;
	}

	public void setInwardChequeId(long inwardChequeId) {
		this.inwardChequeId = inwardChequeId;
	}

	public String getChequeNumber() {
		return chequeNumber;
	}

	public void setChequeNumber(String chequeNumber) {
		this.chequeNumber = chequeNumber;
	}

	public long getBatchId() {
		return batchId;
	}

	public void setBatchId(long batchId) {
		this.batchId = batchId;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public LocalDate getChequeDate() {
		return chequeDate;
	}

	public void setChequeDate(LocalDate chequeDate) {
		this.chequeDate = chequeDate;
	}

	public String getDrawerName() {
		return drawerName;
	}

	public void setDrawerName(String drawerName) {
		this.drawerName = drawerName;
	}

	public BigDecimal getChequeAmount() {
		return chequeAmount;
	}

	public void setChequeAmount(BigDecimal chequeAmount) {
		this.chequeAmount = chequeAmount;
	}

	public String getMicrCode() {
		return micrCode;
	}

	public void setMicrCode(String micrCode) {
		this.micrCode = micrCode;
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

    
}