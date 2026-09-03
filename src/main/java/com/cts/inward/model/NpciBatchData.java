package com.cts.inward.model;

import java.util.List;
import java.util.Objects;

/**
 * Domain model placeholder. Exact fields are intentionally deferred to the
 * finalized database/domain design.
 */
public class NpciBatchData {

	private String batchId;
	private String presentingBankName;
	private int totalCheque;
	private String fileId;
	private List<NpciChequeData> cheques;

	private NpciBatchData(String batchId, String presentingBankName, int totalCheque, String fileId,
			List<NpciChequeData> cheques) {
		this.batchId = batchId;
		this.presentingBankName = presentingBankName;
		this.totalCheque = totalCheque;
		this.fileId = fileId;
		this.cheques = cheques;
	}

	public static NpciBatchData of(String batchId, String presentingBankName, int totalCheque, String fileId,
			List<NpciChequeData> cheques) {
		// TODO Auto-generated method stub
		return new NpciBatchData(batchId, presentingBankName, totalCheque, fileId, cheques);
	}

	public String getBatchId() {
		return batchId;
	}

	public void setBatchId(String batchId) {
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

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public List<NpciChequeData> getCheques() {
		return cheques;
	}

	public void setCheques(List<NpciChequeData> cheques) {
		this.cheques = cheques;
	}

	@Override
	public String toString() {
		return "NpciBatchData [batchId=" + batchId + ", presentingBankName=" + presentingBankName + ", totalCheque="
				+ totalCheque + ", fileId=" + fileId + ", cheques=" + cheques + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(batchId, cheques, fileId, presentingBankName, Integer.valueOf(totalCheque));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NpciBatchData other = (NpciBatchData) obj;
		return Objects.equals(batchId, other.batchId) && Objects.equals(cheques, other.cheques)
				&& Objects.equals(fileId, other.fileId) && Objects.equals(presentingBankName, other.presentingBankName)
				&& totalCheque == other.totalCheque;
	}
	
	

}
