package com.cts.inward.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OcrChequeData {

	private long inwardChequeId;

	private String chequeNumber;
	private long batchId;
	private String accountNumber;
	private LocalDate chequeDate;
	private LocalDate presentingDate;
	private BigDecimal chequeAmount;
	private String micrCode;
	private String cityCode;
	private String bankCode;
	private String branchSpecificCode;
	private String drawerName;
	private String payeeName;
	private String payeeAccountNumber;
	private String amountInWords;

	private OcrChequeData() {

	}

	private OcrChequeData(String chequeNumber, long batchId, String accountNumber, LocalDate chequeDate,
			LocalDate presentingDate, BigDecimal chequeAmount, String micrCode, String cityCode, String bankCode,
			String branchSpecificCode, String drawerName, String payeeName, String payeeAccountNumber,
			String amountInWords) {

		this.chequeNumber = chequeNumber;
		this.batchId = batchId;
		this.accountNumber = accountNumber;
		this.chequeDate = chequeDate;
		this.presentingDate = presentingDate;
		this.chequeAmount = chequeAmount;
		this.micrCode = micrCode;
		this.cityCode = cityCode;
		this.bankCode = bankCode;
		this.branchSpecificCode = branchSpecificCode;
		this.drawerName = drawerName;
		this.payeeName = payeeName;
		this.payeeAccountNumber = payeeAccountNumber;
		this.amountInWords = amountInWords;
	}

	public static OcrChequeData of() {
		return new OcrChequeData();
	}

	public static OcrChequeData of(String chequeNumber, long batchId, String accountNumber, LocalDate chequeDate,
			LocalDate presentingDate, BigDecimal chequeAmount, String micrCode, String cityCode, String bankCode,
			String branchSpecificCode, String drawerName, String payeeName, String payeeAccountNumber,
			String amountInWords) {

		return new OcrChequeData(chequeNumber, batchId, accountNumber, chequeDate, presentingDate, chequeAmount,
				micrCode, cityCode, bankCode, branchSpecificCode, drawerName, payeeName, payeeAccountNumber,
				amountInWords);
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

	public String getBranchSpecificCode() {
		return branchSpecificCode;
	}

	public void setBranchSpecificCode(String branchSpecificCode) {
		this.branchSpecificCode = branchSpecificCode;
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