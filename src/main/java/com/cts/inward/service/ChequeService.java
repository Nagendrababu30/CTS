package com.cts.inward.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.cts.inward.model.InwardCheque;
import com.cts.inward.model.NpciChequeData;

public interface ChequeService {

	List<InwardCheque> getChequesForBatch(String batchId);

	List<InwardCheque> getMicrMismatchCheques(String batchId);

	InwardCheque getCheque(String chequeId);

	InwardCheque getNextCheque(String batchId, String chequeId);

	InwardCheque getPreviousCheque(String batchId, String chequeId);
	
	void saveCheque(NpciChequeData cheque);

	void updateCheque(String chequeNumber,long batchId,String accountNumber,BigDecimal amount,LocalDate chequeDate);
	
	void updateDataEntryCheque(String chequeNumber, long batchId, String accountNumber, BigDecimal amount,
			LocalDate chequeDate, long userId);
}
