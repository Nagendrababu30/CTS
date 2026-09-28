package com.iispl.cts.service.outward;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.iispl.cts.dao.outward.OutwardMakerDataEntryDetailDAO;
import com.iispl.cts.model.outward.OutwardCheque;

public class OutwardMakerDataEntryDetailService {

	private final OutwardMakerDataEntryDetailDAO dao;

	public OutwardMakerDataEntryDetailService() {

		dao = new OutwardMakerDataEntryDetailDAO();
	}

	public List<OutwardCheque> getCheques(String batchId) {

		return dao.getCheques(batchId);
	}

	public List<OutwardCheque> getReturnedCheques(String batchId) {

		return dao.getReturnedCheques(batchId);
	}

	public void saveCheque(OutwardCheque cheque) {

		dao.saveCheque(cheque);
	}

	public void rejectCheque(OutwardCheque cheque, String reason) {

		dao.rejectCheque(cheque, reason);
	}

	public boolean completeBatchDataEntry(String batchId, int currentUserId) {

		return dao.completeBatchDataEntry(batchId, currentUserId);
	}

	public boolean saveAndVerifyCheque(OutwardCheque cheque, int makerId) {

		try {

			dao.saveCheque(cheque);

			String status = cheque.getChequeStatus();

			if (status == null || status.trim().isEmpty()) {

				status = "VERIFIED";
			}

			dao.updateChequeStatus(cheque.getBatchNumber(), cheque.getChequeNumber(), status);

			return dao.saveMakerVerify(cheque.getBatchNumber(), cheque.getChequeNumber(), makerId);

		} catch (Exception e) {

			e.printStackTrace();

			return false;
		}
	}

	public boolean recordMakerReject(String batchNumber, String chequeNumber, int makerId, String reasonId) {

		try {

			dao.updateChequeStatus(batchNumber, chequeNumber, "REJECT_REQUESTED");

			return dao.saveMakerReject(batchNumber, chequeNumber, makerId, reasonId);

		} catch (Exception e) {

			e.printStackTrace();

			return false;
		}
	}

	public Map<String, String> getReturnReasons() {

		return dao.getReturnReasons();
	}
}