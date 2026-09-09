package com.cts.inward.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.cts.inward.model.InwardCheque;
import com.cts.inward.model.NpciChequeData;

public interface ChequeDao {

	void saveCheque(NpciChequeData cheque);

	List<InwardCheque> getChequesForBatch(String batchId);

	void saveDataEntryCorrections(String chequeNumber, long batchId, String accountNumber, BigDecimal amount,
			LocalDate chequeDate, long userId);

	void updateChequeStatus(String chequeNumber, String status, long userId);
}