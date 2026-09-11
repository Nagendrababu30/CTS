package com.cts.inward.service;

import java.util.List;

import com.cts.inward.dto.MicrComparisonDto;
import com.cts.inward.dto.MicrRepairBatchDto;
import com.cts.inward.dto.ReturnReasonDto;

public interface MicrRepairService {

    List<MicrRepairBatchDto> getRepairBatches();

    boolean needsMicrRepair(long batchId);

    int getNextRepairIndex(long batchId);

    List<MicrComparisonDto> compareBatch(long batchId);

    String getFrontImagePath(String chequeNumber);

    String getBackImagePath(String chequeNumber);

    List<ReturnReasonDto> getMakerReturnReasons();

    boolean saveMakerReturn(
            String chequeNumber,
            String returnReasonCode,
            String makerRemarks,
            long userId);

    boolean saveMicrRepair(
            String chequeNumber,
            String originalMicr,
            String repairedMicr,
            String remarks,
            long userId);

    boolean markBatchDataEntry(
            long batchId,
            long userId);

    int getBatchChequePosition(
            long batchId,
            String chequeNumber);

    int getBatchTotalChequeCount(
            long batchId);
}
