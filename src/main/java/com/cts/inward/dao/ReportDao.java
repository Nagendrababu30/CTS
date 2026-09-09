package com.cts.inward.dao;

import java.util.List;
import com.cts.inward.model.NpciBatchData;

public interface ReportDao {
    
    // Fetch all batches that have been SENT_TO_CHECKER
    List<NpciBatchData> getSentToCheckerBatches();
    
 // Fetch report details for a specific batch
    java.util.Map<String, Object> getBatchReportDetails(Long batchId);
}