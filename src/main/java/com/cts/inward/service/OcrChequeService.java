package com.cts.inward.service;

import com.cts.inward.model.OcrChequeData;

public interface OcrChequeService {

	void saveCheque(OcrChequeData chequeData);

	void linkInwardChequeIds(long ocrBatchId);
	
}
