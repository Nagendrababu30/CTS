package com.cts.inward.model;

import java.util.Objects;

/**
 * Domain model placeholder. Exact fields are intentionally deferred to the
 * finalized database/domain design.
 */
public class NpciBatchData {

	private long batchId;
	private long fileId;
	private String presentingBankName;
	private int totalCheques;
	
	private NpciBatchData() {
		
	}
	
	public NpciBatchData(long batchId, long fileId, String presentingBankName, int totalCheques) {
		this.batchId = batchId;
		this.fileId = fileId;
		this.presentingBankName = presentingBankName;
		this.totalCheques = totalCheques;
	}
	
	public static NpciBatchData of() {
		return new NpciBatchData();
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

	@Override
	public String toString() {
		return "NpciBatchData [batchId=" + batchId + ", fileId=" + fileId + ", presentingBankName=" + presentingBankName
				+ ", totalCheques=" + totalCheques + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(batchId, fileId, presentingBankName, totalCheques);
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
		return batchId == other.batchId && fileId == other.fileId
				&& Objects.equals(presentingBankName, other.presentingBankName) && totalCheques == other.totalCheques;
	}
	
}
