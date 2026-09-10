package com.iispl.cts.service.outward;

import java.util.List;

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
}