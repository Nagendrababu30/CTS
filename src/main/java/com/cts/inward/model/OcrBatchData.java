package com.cts.inward.model;

import java.util.ArrayList;
import java.util.List;

public class OcrBatchData {

	private long batchId;
	private String presentingBankName;
	private int totalCheque;
	private long fileId;
	private List<OcrChequeData> cheques;

	private OcrBatchData(long batchId, String presentingBankName, int totalCheque, long fileId) {
		this.batchId = batchId;
		this.presentingBankName = presentingBankName;
		this.totalCheque = totalCheque;
		this.fileId = fileId;
		this.cheques = new ArrayList<>();
	}

	public static OcrBatchData of(long batchId, String presentingBankName, int totalCheque, long fileId) {
		return new OcrBatchData(batchId, presentingBankName, totalCheque, fileId);
	}

	public long getBatchId() {
		return batchId;
	}

	public void setBatchId(long batchId) {
		this.batchId = batchId;
	}

	public String getPresentingBankName() {
		return presentingBankName;
	}

	public void setPresentingBankName(String presentingBankName) {
		this.presentingBankName = presentingBankName;
	}

	public int getTotalCheque() {
		return totalCheque;
	}

	public void setTotalCheque(int totalCheque) {
		this.totalCheque = totalCheque;
	}

	public long getFileId() {
		return fileId;
	}

	public void setFileId(long fileId) {
		this.fileId = fileId;
	}

	public List<OcrChequeData> getCheques() {
		return cheques;
	}

	public void setCheques(List<OcrChequeData> cheques) {
		this.cheques = cheques;
	}
}
