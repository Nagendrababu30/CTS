package com.cts.inward.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
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

	void updateCheque(String chequeNumber, long batchId, String accountNumber,BigDecimal amount,
		LocalDate chequeDate);

	void updateDataEntryCheque(String chequeNumber, long batchId, String accountNumber, BigDecimal amount,
			LocalDate chequeDate, long userId);
}
