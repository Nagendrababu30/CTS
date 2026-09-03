package com.cts.inward.dao;

import java.util.List;

import com.cts.inward.model.InwardCheque;
import com.cts.inward.model.NpciChequeData;

/**
 * DAO placeholder.
 * Exact methods will be aligned with the finalized database design later.
 */
public interface ChequeDao {

	void saveCheque(NpciChequeData cheque);

	List<InwardCheque> getChequesForBatch(String batchId);
	
}
