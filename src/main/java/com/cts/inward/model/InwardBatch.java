package com.cts.inward.model;

public class InwardBatch {

	private long batchId;
	private long fileId;
	private String presentingBankName;
	private int totalCheques;

	public InwardBatch() {
	}

	public InwardBatch(long batchId, long fileId, String presentingBankName, int totalCheques) {

		this.batchId = batchId;
		this.fileId = fileId;
		this.presentingBankName = presentingBankName;
		this.totalCheques = totalCheques;
	}

	public long getBatchId() {
		return batchId;
	}

	public void setBatchId(long batchId) {
		this.batchId = batchId;
	}

	public long getFileId() {
		return fileId;
	}

	public void setFileId(long fileId) {
		this.fileId = fileId;
	}

	public String getPresentingBankName() {
		return presentingBankName;
	}

	public void setPresentingBankName(String presentingBankName) {

		this.presentingBankName = presentingBankName;
	}

	public int getTotalCheques() {
		return totalCheques;
	}

	public void setTotalCheques(int totalCheques) {

		this.totalCheques = totalCheques;
	}
}