package com.cts.inward.dao;

import java.util.List;
import java.util.Map;

import com.cts.inward.dto.ReturnReasonDto;
import com.cts.inward.model.NpciChequeData;
import com.cts.inward.model.OcrChequeData;

// DAO interface for inward MICR repair and verification operations
public interface MicrRepairDao {

    // Retrieves NPCI cheque records for a batch
    List<NpciChequeData> getNpciCheques(long batchId);

    // Retrieves OCR cheque records for a batch
    List<OcrChequeData> getOcrCheques(long batchId);

    // Bulk retrieves completed repaired MICR codes for a list of cheques
    Map<String, String> getCompletedRepairedMicrs(List<String> chequeNumbers);

    // Bulk retrieves latest cheque statuses for a list of cheques
    Map<String, String> getLatestChequeStatuses(List<String> chequeNumbers);

    // Bulk retrieves latest cheque return reasons for a list of cheques
    Map<String, String> getLatestChequeReturnReasons(List<String> chequeNumbers);

    // Checks if the batch was returned to maker by checker
    boolean isBatchReturnedToMaker(long batchId);

    // Checks if a specific cheque needs MICR repair
    boolean chequeNeedsMicrRepair(String chequeNumber);

    // Resolves batch ID for a given cheque number
    long getBatchIdByChequeNumber(String chequeNumber);

    // Retrieves front image file path for a cheque
    String getFrontImagePath(String chequeNumber);

    // Retrieves back image file path for a cheque
    String getBackImagePath(String chequeNumber);

    // Retrieves active return reason codes for Maker returns
    List<ReturnReasonDto> getMakerReturnReasons();

    // Saves a Maker return decision for a cheque
    boolean saveMakerReturn(
            String chequeNumber,
            String returnReasonCode,
            String makerRemarks,
            long userId);

    // Saves repaired MICR values and audit remarks for a cheque
    boolean saveMicrRepair(
            String chequeNumber,
            String originalMicr,
            String repairedMicr,
            String remarks,
            long userId);

    // Updates batch status to DATA_ENTRY when MICR repair is complete and amount/date needs review
    boolean markBatchReadyForDataEntry(
            long batchId,
            long userId);

    // Updates batch status to SEND_TO_CHECKER when all cheques are repaired/clean
    boolean markBatchReadyForChecker(
            long batchId,
            long userId);

    // Checks if any cheque in the batch requires Data Entry review
    boolean hasChequesNeedingDataEntry(
            long batchId);
}
