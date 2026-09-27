package com.cts.inward.service;

import java.util.List;

import com.cts.inward.dto.MicrComparisonDto;
import com.cts.inward.dto.MicrRepairBatchDto;
import com.cts.inward.dto.ReturnReasonDto;

// Service interface for inward Maker MICR repair and verification
public interface MicrRepairService {

    // Retrieves batches needing MICR repair assigned to the logged-in maker
    List<MicrRepairBatchDto> getRepairBatches(Long userId);

    // Checks whether a batch still contains cheques requiring MICR repair
    boolean needsMicrRepair(long batchId);

    // Resolves the index of the next cheque in the batch that requires MICR repair
    int getNextRepairIndex(long batchId);

    // Compares NPCI vs OCR MICR data for all cheques in the batch
    List<MicrComparisonDto> compareBatch(long batchId);

    // Retrieves front image path for a cheque
    String getFrontImagePath(String chequeNumber);

    // Retrieves back image path for a cheque
    String getBackImagePath(String chequeNumber);

    // Retrieves active return reasons available to the Maker
    List<ReturnReasonDto> getMakerReturnReasons();

    // Saves a Maker return decision for a cheque and advances batch workflow if stage is complete
    boolean saveMakerReturn(
            String chequeNumber,
            String returnReasonCode,
            String makerRemarks,
            long userId);

    // Saves repaired MICR values, verifies 9-digit master validity, and advances batch workflow
    boolean saveMicrRepair(
            String chequeNumber,
            String originalMicr,
            String repairedMicr,
            String remarks,
            long userId);

    // Moves batch to DATA_ENTRY stage when MICR repairs are complete but amount/date requires review
    boolean markBatchDataEntry(
            long batchId,
            long userId);

    // Checks whether any cheques in the batch require Data Entry verification
    boolean hasChequesNeedingDataEntry(long batchId);
}
