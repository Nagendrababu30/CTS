package com.cts.inward.dao;

import com.cts.inward.model.OcrBatchData;

public interface OcrBatchDao {

	long saveBatch(OcrBatchData batchData);
	
}
