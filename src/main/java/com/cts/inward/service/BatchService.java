package com.cts.inward.service;

import java.util.List;
import java.util.Map;

import com.cts.inward.model.InwardBatch;
import com.cts.inward.model.NpciBatchData;

public interface BatchService {

	List<InwardBatch> getAvailableBatchesForMaker(String userId);

	List<InwardBatch> getBatchesForChecker(String userId);

	InwardBatch getBatch(String batchId);

	int getDataEntryPendingCount(long batchId);

	boolean acquireLock(String batchId, String userId);

	void releaseLock(String batchId, String userId);

	void sendToChecker(String batchId, String userId);

	void saveBatch(NpciBatchData batchData);

	boolean completeDataEntry(long batchId, long userId);

	long getBatchIdByFileName(String batchName);

	// Verify Batch
	List<Map<String, Object>> getBatchesForVerification(Integer userId);

	List<Map<String, Object>> searchBatchesForVerification(Long batchId, Integer userId);

}
