package com.cts.inward.dao;

import com.cts.inward.model.OcrBatchData;

public interface OcrBatchDao {

	long saveBatch(OcrBatchData batchData);

	// Check if an OCR batch already exists in ocr_batch table
	boolean isBatchExists(long batchId);
}
