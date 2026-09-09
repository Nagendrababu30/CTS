package com.cts.inward.dao;

import java.util.List;
import java.util.Map;

public interface BatchDetailsDao {
	
	 Map<String, Object> getMicrDetails(String chequeNumber);

	 List<Map<String, Object>> getChequesByBatchId(String batchId);
	 
	 Map<String, Object> getDataEntryDetails(String chequeNumber);
	 
	 Map<String,Object> getCbsValidation(String chequeNumber);

}
