package com.cts.inward.dao;

import java.util.List;

import com.cts.inward.model.NpciBatchData;

/**
 * DAO placeholder.
 * Exact methods will be aligned with the finalized database design later.
 */
public interface BatchDao {

	 void saveBatch(NpciBatchData batchData);

	 List<NpciBatchData> getAllBatches();
	
	 boolean completeDataEntry(long batchId, long userId);

	List<NpciBatchData> getBatchesByStatus(String batchStatus);

}
