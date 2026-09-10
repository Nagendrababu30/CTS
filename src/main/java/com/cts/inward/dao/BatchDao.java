package com.cts.inward.dao;

import java.util.List;
import java.util.Map;

import com.cts.inward.model.NpciBatchData;

public interface BatchDao {

    void saveBatch(NpciBatchData batchData);

	 List<NpciBatchData> getAllBatches();
	 
	 int getDataEntryPendingCount(long batchId);
	 
	 boolean completeDataEntry(long batchId, long userId);

	List<NpciBatchData> getBatchesByStatus(String batchStatus);

    
}
