package com.cts.inward.service;

import java.util.List;

import com.cts.inward.dto.MicrComparisonDto;
import com.cts.inward.dto.MicrRepairBatchDto;
import com.cts.inward.dto.ReturnReasonDto;

public interface MicrRepairService {

    List<MicrComparisonDto> compareBatch(
            long batchId);

    boolean needsMicrRepair(
            long batchId);

    List<MicrRepairBatchDto> getRepairBatches();

    int getNextRepairIndex(
            long batchId);

    String getFrontImagePath(
            String chequeNumber);

    String getBackImagePath(
            String chequeNumber);

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
}