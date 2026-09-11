package com.cts.inward.dao;

import java.util.List;

import com.cts.inward.dto.ReturnReasonDto;
import com.cts.inward.model.NpciChequeData;
import com.cts.inward.model.OcrChequeData;

public interface MicrRepairDao {

    /*
     * -------------------------------------------------------------------------
     * NPCI / OCR DATA
     * -------------------------------------------------------------------------
     */

    List<NpciChequeData> getNpciCheques(
            long batchId);

    List<OcrChequeData> getOcrCheques(
            long batchId);


    /*
     * -------------------------------------------------------------------------
     * MICR REPAIR
     * -------------------------------------------------------------------------
     */

    String getCompletedRepairedMicr(
            String chequeNumber);

    String getLatestChequeStatus(
            String chequeNumber);


    /*
     * -------------------------------------------------------------------------
     * BATCH / CHEQUE INFORMATION
     * -------------------------------------------------------------------------
     */

    long getBatchIdByChequeNumber(
            String chequeNumber);

    int getBatchChequePosition(
            long batchId,
            String chequeNumber);

    int getBatchTotalChequeCount(
            long batchId);


    /*
     * -------------------------------------------------------------------------
     * IMAGES
     * -------------------------------------------------------------------------
     */

    String getFrontImagePath(
            String chequeNumber);

    String getBackImagePath(
            String chequeNumber);


    /*
     * -------------------------------------------------------------------------
     * MAKER RETURN
     * -------------------------------------------------------------------------
     */

    List<ReturnReasonDto> getMakerReturnReasons();

    boolean saveMakerReturn(
            String chequeNumber,
            String returnReasonCode,
            String makerRemarks,
            long userId);


    /*
     * -------------------------------------------------------------------------
     * MICR REPAIR SAVE
     * -------------------------------------------------------------------------
     */

    boolean saveMicrRepair(
            String chequeNumber,
            String originalMicr,
            String repairedMicr,
            String remarks,
            long userId);


    /*
     * -------------------------------------------------------------------------
     * MOVE BATCH TO DATA ENTRY
     * -------------------------------------------------------------------------
     */

    boolean markBatchReadyForDataEntry(
            long batchId,
            long userId);
}
