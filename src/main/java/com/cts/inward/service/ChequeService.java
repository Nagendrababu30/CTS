package com.cts.inward.service;

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

	
}
