package com.cts.inward.dao;

import com.cts.inward.model.OcrChequeData;

public interface OcrChequeDao {

	void saveCheque(OcrChequeData chequeData);

	void linkInwardChequeIds(long ocrBatchId);
	
}
