package com.cts.inward.dao;

import java.util.List;

import com.cts.inward.dto.ReturnReasonDto;
import com.cts.inward.model.NpciChequeData;
import com.cts.inward.model.OcrChequeData;

public interface MicrRepairDao {

    List<NpciChequeData> getNpciCheques(long batchId);

    List<OcrChequeData> getOcrCheques(long batchId);

    String getCompletedRepairedMicr(String chequeNumber);

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
}