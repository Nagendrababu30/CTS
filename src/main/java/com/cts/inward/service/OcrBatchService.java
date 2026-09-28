package com.cts.inward.service;

import com.cts.inward.model.OcrBatchData;

public interface OcrBatchService {

	long saveBatch(OcrBatchData batchData);

	// Check if an OCR batch already exists in database
	boolean isBatchExists(long batchId);
}
