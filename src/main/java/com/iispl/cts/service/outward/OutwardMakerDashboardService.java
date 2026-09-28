package com.iispl.cts.service.outward;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.iispl.cts.dao.outward.OutwardMakerDashboardDAO;
import com.iispl.cts.model.outward.ChequeProcessing;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.model.outward.OutwardValidationResult;

public class OutwardMakerDashboardService {

    private final OutwardMakerDashboardDAO dao;
    private final OutwardValidationService validationService;

    // initialize dashboard service
    public OutwardMakerDashboardService() {
        this.dao = new OutwardMakerDashboardDAO();
        this.validationService = new OutwardValidationService();
    }

    // get all batches
    public List<OutwardBatch> getBatches() throws SQLException {
        return dao.getBatches();
    }

    // find batch by batch number
    public OutwardBatch findBatch(String batchNumber) throws SQLException {
        if (isEmpty(batchNumber)) {
            return null;
        }

        List<OutwardBatch> batches = dao.getBatches();

        if (batches == null) {
            return null;
        }

        String cleanBatchNumber = batchNumber.trim();

        for (OutwardBatch batch : batches) {
            if (batch == null) {
                continue;
            }

            String currentBatchNumber = batch.getBatchNumber();

            if (currentBatchNumber != null
                    && cleanBatchNumber.equalsIgnoreCase(currentBatchNumber.trim())) {
                return batch;
            }
        }

        return null;
    }

    // check whether batch is on hold
    public boolean isHoldBatch(String batchNumber) throws SQLException {
        if (isEmpty(batchNumber)) {
            return false;
        }

        OutwardBatch batch = findBatch(batchNumber.trim());

        if (batch == null) {
            return false;
        }

        String batchStatus = batch.getBatchStatus();

        return batchStatus != null
                && "HOLD".equalsIgnoreCase(batchStatus.trim());
    }

    // get cheques returned to maker
    public List<OutwardCheque> getReturnedCheques(String batchNumber) throws SQLException {
        List<OutwardCheque> returnedCheques = new ArrayList<>();

        if (isEmpty(batchNumber)) {
            return returnedCheques;
        }

        List<OutwardCheque> cheques = dao.getCheques(batchNumber.trim());

        if (cheques == null || cheques.isEmpty()) {
            return returnedCheques;
        }

        for (OutwardCheque cheque : cheques) {
            if (cheque == null) {
                continue;
            }

            String chequeStatus = cheque.getChequeStatus();

            if (chequeStatus != null
                    && "SENT_BACK_TO_MAKER".equalsIgnoreCase(chequeStatus.trim())) {
                returnedCheques.add(cheque);
            }
        }

        return returnedCheques;
    }

    // release batch lock
    public boolean releaseBatchLock(String batchNumber, String userId) throws SQLException {
        if (isEmpty(batchNumber) || isEmpty(userId)) {
            return false;
        }

        return dao.releaseBatchLock(batchNumber.trim(), userId.trim());
    }

    // get cheque processing details
    public ChequeProcessing getChequeProcessing(
            String batchNumber,
            String chequeNumber) throws SQLException {

        if (isEmpty(batchNumber) || isEmpty(chequeNumber)) {
            return null;
        }

        return dao.getChequeProcessing(
                batchNumber.trim(),
                chequeNumber.trim());
    }

    // assign batch and validate cheques
    public OutwardValidationResult assignAndValidate(
            String batchNumber,
            String userId) throws SQLException {

        if (isEmpty(batchNumber) || isEmpty(userId)) {
            return null;
        }

        String cleanBatchNumber = batchNumber.trim();
        String cleanUserId = userId.trim();

        boolean assigned = dao.assignBatch(
                cleanBatchNumber,
                cleanUserId);

        if (!assigned) {
            return null;
        }

        List<OutwardCheque> cheques = dao.getCheques(cleanBatchNumber);

        OutwardValidationResult result = validationService.validate(cheques);

        if (cheques != null) {
            for (OutwardCheque cheque : cheques) {
                if (cheque == null) {
                    continue;
                }

                String errorType = validationService.getMicrErrorType(cheque);

                if (errorType != null) {
                    dao.updateChequeStatus(
                            cleanBatchNumber,
                            cheque.getChequeNumber(),
                            "MICR_ERROR");
                } else {
                    dao.updateChequeStatus(
                            cleanBatchNumber,
                            cheque.getChequeNumber(),
                            "MICR_VERIFIED");
                }
            }
        }

        if (result.getMicrErrors() > 0) {
            dao.updateBatchStatus(
                    cleanBatchNumber,
                    "MICR_REPAIR");
        } else {
            dao.updateBatchStatus(
                    cleanBatchNumber,
                    "MICR_VERIFIED");
        }

        return result;
    }

    // get cheques for a batch
    public List<OutwardCheque> getCheques(String batchNumber) throws SQLException {
        if (isEmpty(batchNumber)) {
            return null;
        }

        return dao.getCheques(batchNumber.trim());
    }

    // check whether batch is valid
    public boolean isBatchValid(String batchNumber) throws SQLException {
        if (isEmpty(batchNumber)) {
            return false;
        }

        return dao.isBatchValid(batchNumber.trim());
    }

    // complete batch processing
    public void completeBatch(String batchNumber) throws SQLException {
        if (isEmpty(batchNumber)) {
            return;
        }

        dao.updateBatchIfCompleted(batchNumber.trim());
    }

    // check whether value is empty
    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    // get dashboard counts
    public Map<String, Integer> getDashboardCounts() throws SQLException {
        return dao.getDashboardCounts();
    }
}