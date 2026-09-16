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

    List<Map<String, String>> getCheckerReturnReasons();

    List<Map<String, String>> getCheckerRejectionReasons();

    List<Map<String, String>> getMakerReturnReasons(String chequeNumber);

    void saveCheckerDecision(
            String chequeNumber,
            String status,
            List<String> rejectionReasonCodes,
            List<String> returnReasonCodes,
            Integer checkerId,
            String checkerAction,
            String remarks);

    default void saveCheckerDecision(
            String chequeNumber,
            String status,
            String rejectionReasonCode,
            String returnReasonCode,
            Integer checkerId,
            String checkerAction,
            String remarks) {
        List<String> rejList = rejectionReasonCode != null && !rejectionReasonCode.trim().isEmpty()
                ? java.util.Collections.singletonList(rejectionReasonCode.trim())
                : java.util.Collections.emptyList();
        List<String> retList = returnReasonCode != null && !returnReasonCode.trim().isEmpty()
                ? java.util.Collections.singletonList(returnReasonCode.trim())
                : java.util.Collections.emptyList();
        saveCheckerDecision(chequeNumber, status, rejList, retList, checkerId, checkerAction, remarks);
    }

    boolean completeVerification(
            Long batchId,
            Integer checkerId);
}