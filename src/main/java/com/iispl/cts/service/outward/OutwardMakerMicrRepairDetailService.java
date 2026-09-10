package com.iispl.cts.service.outward;

import java.util.List;

import com.iispl.cts.dao.outward.OutwardMakerMicrRepairDetailDAO;
import com.iispl.cts.model.outward.OutwardCheque;

public class OutwardMakerMicrRepairDetailService {


private OutwardMakerMicrRepairDetailDAO dao;

public OutwardMakerMicrRepairDetailService() {
    dao = new OutwardMakerMicrRepairDetailDAO();
}


public List<OutwardCheque> getMicrErrorCheques(
        String batchNumber) {

    return dao.getMicrErrorCheques(batchNumber);
}


public boolean updateCorrectedMicr(
        String batchNumber,
        String chequeNumber,
        String cityCode,
        String bankCode,
        String branchCode) {

    return dao.updateCorrectedMicr(
            batchNumber,
            chequeNumber,
            cityCode,
            bankCode,
            branchCode);
}


public boolean hasRemainingMicrErrors(
        String batchNumber) {

    return dao.hasRemainingMicrErrors(batchNumber);
}


public boolean updateBatchStatus(
        String batchNumber) {

    return dao.updateBatchStatus(batchNumber);
}


}