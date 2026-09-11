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
	
	List<Map<String, Object>> getBatchesForVerification(
	        Integer userId);

	List<Map<String, Object>> searchBatchesForVerification(
	        Long batchId,
	        Integer userId);

	/**
	 * Looks up the numeric batch_id by matching the batch name
	 * extracted from a PIBF filename against the PXF file name
	 * in inward_file.
	 *
	 * e.g. batchName "BATCH001" matches file_name "BATCH001.xml"
	 * where file_type = 'PXF'.
	 *
	 * Returns -1 if not found.
	 */
	long getBatchIdByFileName(String batchName);

   
}
