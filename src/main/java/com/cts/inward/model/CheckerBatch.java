package com.cts.inward.model;

public class CheckerBatch {

	private long lockId;
	private long batchId;
	private int totalCheques;

	private String maker;
	private Long userId;
	private String lockStatus;

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

	public String getLockStatus() {
		return lockStatus;
	}

	public void setLockStatus(String lockStatus) {
		this.lockStatus = lockStatus;
	}
}