package com.cts.inward.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NpciChequeData {

	private long inwardChequeId;

	private String chequeNumber;
	private long batchId;
	private String accountNumber;
	private LocalDate chequeDate;
	private LocalDate presentingDate;
	private BigDecimal chequeAmount;
	private String amountInWords;
	private String micrCode;
	private String cityCode;
	private String bankCode;
	private String branchCode;
	private String drawerName;
	private String payeeName;
	private String payeeAccountNumber;

	private NpciChequeData(String chequeNumber, long batchId, String accountNumber, LocalDate chequeDate,
			LocalDate presentingDate, BigDecimal chequeAmount, String amountInWords, String micrCode, String cityCode,
			String bankCode, String branchCode, String drawerName, String payeeName, String payeeAccountNumber) {

		this.chequeNumber = chequeNumber;
		this.batchId = batchId;
		this.accountNumber = accountNumber;
		this.chequeDate = chequeDate;
		this.presentingDate = presentingDate;
		this.chequeAmount = chequeAmount;
		this.amountInWords = amountInWords;
		this.micrCode = micrCode;
		this.cityCode = cityCode;
		this.bankCode = bankCode;
		this.branchCode = branchCode;
		this.drawerName = drawerName;
		this.payeeName = payeeName;
		this.payeeAccountNumber = payeeAccountNumber;
	}

	public static NpciChequeData of(String chequeNumber, long batchId, String accountNumber, LocalDate chequeDate,
			LocalDate presentingDate, BigDecimal chequeAmount, String amountInWords, String micrCode, String cityCode,
			String bankCode, String branchCode, String drawerName, String payeeName, String payeeAccountNumber) {

		return new NpciChequeData(chequeNumber, batchId, accountNumber, chequeDate, presentingDate, chequeAmount,
				amountInWords, micrCode, cityCode, bankCode, branchCode, drawerName, payeeName, payeeAccountNumber);
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

	public LocalDate getPresentingDate() {
		return presentingDate;
	}

	public void setPresentingDate(LocalDate presentingDate) {
		this.presentingDate = presentingDate;
	}

	public String getDrawerName() {
		return drawerName;
	}

	public void setDrawerName(String drawerName) {
		this.drawerName = drawerName;
	}

	public String getAmountInWords() {
		return amountInWords;
	}

	public void setAmountInWords(String amountInWords) {
		this.amountInWords = amountInWords;
	}

}