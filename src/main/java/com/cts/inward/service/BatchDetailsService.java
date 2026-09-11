package com.cts.inward.service;

import java.util.List;
import java.util.Map;

public interface BatchDetailsService {

    Map<String, Object> getMicrDetails(
            String chequeNumber);

    List<Map<String, Object>> getChequesByBatchId(
            Long batchId);

    Map<String, Object> getDataEntryDetails(
            String chequeNumber);

    Map<String, Object> getCbsValidation(
            String chequeNumber);

    void saveCheckerDecision(
            String chequeNumber,
            String status,
            String rejectionReasonCode,
            String returnReasonCode,
            Integer checkerId,
            String checkerAction,
            String remarks);
}