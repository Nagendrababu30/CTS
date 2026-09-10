package com.iispl.cts.service.outward;

import java.util.List;

import com.iispl.cts.dao.outward.OutwardMakerMicrRepairDAO;
import com.iispl.cts.model.outward.OutwardBatch;

public class OutwardMakerMicrRepairService {

    private OutwardMakerMicrRepairDAO dao;

    public OutwardMakerMicrRepairService() {

        dao = new OutwardMakerMicrRepairDAO();
    }

    public List<OutwardBatch> getMicrErrorBatches(
            long userId) {

        return dao.getMicrErrorBatches(userId);
    }

    public int getMicrErrorCount(
            String batchNumber) {

        return dao.getMicrErrorCount(
                batchNumber);
    }
}

