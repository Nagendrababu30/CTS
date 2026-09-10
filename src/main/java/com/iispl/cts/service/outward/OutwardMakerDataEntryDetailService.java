package com.iispl.cts.service.outward;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.iispl.cts.dao.outward.OutwardMakerDataEntryDetailDAO;
import com.iispl.cts.model.outward.OutwardCheque;

public class OutwardMakerDataEntryDetailService {

    private final OutwardMakerDataEntryDetailDAO dao;

    public OutwardMakerDataEntryDetailService() {

        dao =
                new OutwardMakerDataEntryDetailDAO();
    }

    public List<OutwardCheque> getCheques(
            String batchId) {

        return dao.getCheques(batchId);
    }

    public void saveCheque(
            OutwardCheque cheque) {

        dao.saveCheque(cheque);
    }

    public void rejectCheque(
            OutwardCheque cheque,
            String reason) {

        dao.rejectCheque(
                cheque,
                reason
        );
    }

	public boolean completeBatchDataEntry(String batchId, int currentUserId) {
		// TODO Auto-generated method stub
		return dao.completeBatchDataEntry(batchId, currentUserId);
	}
	public boolean saveAndVerifyCheque(OutwardCheque cheque, int makerId) {
	    try {
	        // 1. Update instrument record & status in public.outward_cheque
	        dao.saveCheque(cheque);
	        dao.updateChequeStatus(cheque.getBatchNumber(), cheque.getChequeNumber(), "VERIFIED");

	        // 2. Track in public.cheque_processing without any maker_reason_id
	        return dao.saveMakerVerify(cheque.getBatchNumber(), cheque.getChequeNumber(), makerId);
	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	    }
	}

	public boolean recordMakerReject(String batchNumber, String chequeNumber, int makerId, int reasonId) {
	    try {
	        dao.updateChequeStatus(batchNumber, chequeNumber, "REJECT_REQUESTED");
	        return dao.saveMakerReject(batchNumber, chequeNumber, makerId, reasonId);
	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	    }
	}

	public Map<Integer, String> getReturnReasons() {
		// TODO Auto-generated method stub
		return dao.getReturnReasons();
	}
}