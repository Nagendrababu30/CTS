package com.cts.inward.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.cts.inward.dao.ChequeDao;
import com.cts.inward.model.InwardCheque;
import com.cts.inward.model.NpciChequeData;

public class ChequeServiceImpl implements ChequeService {

	private final ChequeDao chequeDao;

	private ChequeServiceImpl(ChequeDao chequeDao) {

		this.chequeDao = chequeDao;
	}

	public static ChequeServiceImpl of(ChequeDao chequeDao) {

		return new ChequeServiceImpl(chequeDao);
	}

	@Override
	public void saveCheque(NpciChequeData chequeData) {

		chequeDao.saveCheque(chequeData);
	}

	@Override
	public List<InwardCheque> getChequesForBatch(String batchId) {
		return chequeDao.getChequesForBatch(batchId);
	}

	@Override
	public List<InwardCheque> getAllChequesForBatch(long batchId) {
		return chequeDao.getAllChequesForBatch(batchId);
	}

	@Override
	public List<InwardCheque> getMicrMismatchCheques(String batchId) {

		return null;
	}

	@Override
	public InwardCheque getCheque(String chequeId) {

		return null;
	}

	@Override
	public InwardCheque getNextCheque(String batchId, String chequeId) {

		return null;
	}

	@Override
	public InwardCheque getPreviousCheque(String batchId, String chequeId) {

		return null;
	}

	/*Data Entry corrections are saved into inward_cheque_dataentry_history.*/
	@Override
	public void saveDataEntryCorrections(String chequeNumber, long batchId, String accountNumber, BigDecimal amount,
			LocalDate chequeDate, long userId) {

		chequeDao.saveDataEntryCorrections(chequeNumber, batchId, accountNumber, amount, chequeDate, userId);
	}

	/*
	 * Cheque status is saved into inward_cheque_status_history.
	 */
	@Override
	public void updateChequeStatus(String chequeNumber, String status, long userId) {

		chequeDao.updateChequeStatus(chequeNumber, status, userId);
	}
}