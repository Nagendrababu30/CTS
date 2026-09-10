package com.cts.inward.service;

import java.util.List;
import java.util.Map;

import com.cts.inward.dao.BatchDetailsDao;

public class BatchDetailsServiceImpl implements BatchDetailsService {

    private final BatchDetailsDao batchDetailsDao;

    private BatchDetailsServiceImpl(BatchDetailsDao batchDetailsDao) {
        this.batchDetailsDao = batchDetailsDao;
    }

    public static BatchDetailsServiceImpl of(
            BatchDetailsDao batchDetailsDao) {

        return new BatchDetailsServiceImpl(batchDetailsDao);
    }

    @Override
    public Map<String, Object> getMicrDetails(
            String chequeNumber) {

        return batchDetailsDao.getMicrDetails(chequeNumber);
    }

    @Override
    public List<Map<String, Object>> getChequesByBatchId(
            Long batchId) {

        return batchDetailsDao.getChequesByBatchId(batchId);
    }

    @Override
    public Map<String, Object> getDataEntryDetails(
            String chequeNumber) {

        return batchDetailsDao.getDataEntryDetails(chequeNumber);
    }

    @Override
    public Map<String, Object> getCbsValidation(
            String chequeNumber) {

        return batchDetailsDao.getCbsValidation(chequeNumber);
    }

    @Override
    public void saveCheckerDecision(
            String chequeNumber,
            String status,
            String rejectionReasonCode,
            String returnReasonCode,
            Integer checkerId,
            String checkerAction,
            String remarks) {

        batchDetailsDao.saveCheckerDecision(
                chequeNumber,
                status,
                rejectionReasonCode,
                returnReasonCode,
                checkerId,
                checkerAction,
                remarks);
    }
}