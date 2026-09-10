package com.iispl.cts.service.outward;

import java.util.List;

import com.iispl.cts.dao.outward.OutwardMakerAmountAccountDAO;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;

public class OutwardMakerAmountAccountService {

    private final OutwardMakerAmountAccountDAO dao;

    public OutwardMakerAmountAccountService() {
        this.dao = new OutwardMakerAmountAccountDAO();
    }

    // ============================================================
    // GET AMOUNT & ACCOUNT BATCHES
    // ============================================================

    public List<OutwardBatch> getAmountAccountBatches() {

        return dao.getAmountAccountBatches();
    }

    // ============================================================
    // GET CHEQUES FOR SELECTED BATCH
    // ============================================================

    public List<OutwardCheque> getChequesForBatch(
            String batchNumber) {

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Batch number is required."
            );
        }

        return dao.getChequesForBatch(
                batchNumber.trim()
        );
    }

    // ============================================================
    // VERIFY BATCH
    // ============================================================

    public boolean verifyBatch(
            String batchNumber) {

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Batch number is required."
            );
        }

        return dao.verifyBatch(
                batchNumber.trim()
        );
    }
}