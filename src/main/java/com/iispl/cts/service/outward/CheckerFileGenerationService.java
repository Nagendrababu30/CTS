package com.iispl.cts.service.outward;

import com.iispl.cts.dao.outward.checker.CheckerBatchDAO;
import com.iispl.cts.model.outward.OutwardCheque;

import java.util.List;

public class CheckerFileGenerationService {

    private final CheckerBatchDAO checkerBatchDAO;

    public CheckerFileGenerationService() {

        checkerBatchDAO = new CheckerBatchDAO();
    }

    /**
     * Get all cheques belonging to the selected batch.
     */
    public List<OutwardCheque> getBatchCheques(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Batch number is required"
            );
        }

        return checkerBatchDAO.getChequesByBatchId(
                batchNumber
        );
    }
}