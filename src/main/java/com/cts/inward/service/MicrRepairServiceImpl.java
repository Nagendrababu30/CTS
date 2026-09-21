package com.cts.inward.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cts.inward.dao.BatchDao;
import com.cts.inward.dao.BatchDaoImpl;
import com.cts.inward.dao.MicrMasterDao;
import com.cts.inward.dao.MicrMasterDaoImpl;
import com.cts.inward.dao.MicrRepairDao;
import com.cts.inward.dao.MicrRepairDaoImpl;
import com.cts.inward.dto.MicrComparisonDto;
import com.cts.inward.dto.MicrRepairBatchDto;
import com.cts.inward.dto.ReturnReasonDto;
import com.cts.inward.model.NpciBatchData;
import com.cts.inward.model.NpciChequeData;
import com.cts.inward.model.OcrChequeData;

public class MicrRepairServiceImpl implements MicrRepairService {

    private static final String STATUS_RETURN_BY_MAKER = "RETURN_BY_MAKER";
    private static final String STATUS_MICR_REPAIRED = "MICR_REPAIRED";

    private final BatchDao batchDao;
    private final MicrRepairDao micrRepairDao;
    private final MicrMasterDao micrMasterDao;

    public MicrRepairServiceImpl() {
        this.batchDao = BatchDaoImpl.of();
        this.micrRepairDao = new MicrRepairDaoImpl();
        this.micrMasterDao = MicrMasterDaoImpl.of();
    }

    @Override
    public List<MicrRepairBatchDto> getRepairBatches() {
        return getRepairBatches(null);
    }

    @Override
    public List<MicrRepairBatchDto> getRepairBatches(Long userId) {

        List<MicrRepairBatchDto> result =
                new ArrayList<>();

        if (userId == null) {
            return result;
        }

        List<NpciBatchData> batches =
                batchDao.getBatchesForMaker(userId);

        if (batches == null
                || batches.isEmpty()) {

            return result;
        }

        for (NpciBatchData batch : batches) {

            if (batch == null) {
                continue;
            }

            long batchId =
                    batch.getBatchId();

            List<MicrComparisonDto> comparisons =
                    compareBatch(batchId);

            if (comparisons == null
                    || comparisons.isEmpty()) {

                continue;
            }

            int pendingMicrRepairCount = 0;

            for (MicrComparisonDto comparison :
                    comparisons) {

                if (comparison == null) {
                    continue;
                }

                if (comparison.isNeedsMicrRepair()) {

                    pendingMicrRepairCount++;
                }
            }

            /*
             * Keep the batch in the MICR Repair queue
             * while at least one cheque still needs repair.
             *
             * Example:
             *
             * Cheque 1 -> MICR_REPAIRED
             * Cheque 2 -> MICR_REPAIR
             * Cheque 3 -> MICR_REPAIR
             *
             * Batch remains in MICR Repair queue.
             *
             * When:
             *
             * Cheque 1 -> MICR_REPAIRED
             * Cheque 2 -> MICR_REPAIRED
             * Cheque 3 -> MICR_REPAIRED
             *
             * pendingMicrRepairCount becomes 0
             * and the batch is removed from this queue.
             */

            if (pendingMicrRepairCount > 0) {

                result.add(
                        new MicrRepairBatchDto(
                                batchId,
                                batch.getTotalCheques(),
                                pendingMicrRepairCount));
            }
        }

        return result;
    }

    @Override
    public boolean needsMicrRepair(long batchId) {
        List<MicrComparisonDto> comparisons = compareBatch(batchId);

        if (comparisons == null) {
            return false;
        }

        for (MicrComparisonDto comparison : comparisons) {
            if (comparison != null && comparison.isNeedsMicrRepair()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int getNextRepairIndex(long batchId) {
        List<MicrComparisonDto> comparisons = compareBatch(batchId);

        if (comparisons == null) {
            return -1;
        }

        for (int i = 0; i < comparisons.size(); i++) {
            MicrComparisonDto comparison = comparisons.get(i);

            if (comparison != null && comparison.isNeedsMicrRepair()) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public List<MicrComparisonDto> compareBatch(long batchId) {

        List<NpciChequeData> npciCheques =
                micrRepairDao.getNpciCheques(batchId);

        List<OcrChequeData> ocrCheques =
                micrRepairDao.getOcrCheques(batchId);

        List<MicrComparisonDto> result =
                new ArrayList<>();

        if (npciCheques == null
                || npciCheques.isEmpty()
                || ocrCheques == null
                || ocrCheques.isEmpty()) {

            return result;
        }

        Map<Long, OcrChequeData> ocrByInwardChequeId =
                new HashMap<>();
        Map<String, OcrChequeData> ocrByChequeNumber =
                new HashMap<>();

        for (OcrChequeData ocr : ocrCheques) {

            if (ocr == null) {
                continue;
            }

            if (ocr.getInwardChequeId() > 0) {
                ocrByInwardChequeId.put(
                        ocr.getInwardChequeId(),
                        ocr
                );
            }
            if (ocr.getChequeNumber() != null && !ocr.getChequeNumber().trim().isEmpty()) {
                ocrByChequeNumber.put(
                        ocr.getChequeNumber().trim(),
                        ocr
                );
            }
        }

        boolean isBatchReturned =
                micrRepairDao.isBatchReturnedToMaker(batchId);

        // =========================================================
        // BATCH QUERY OPTIMIZATION (Option C)
        // Pre-fetch statuses, repaired MICRs, and master validity
        // in bulk instead of 5 individual queries per cheque.
        // =========================================================
        List<String> allChequeNumbers = new ArrayList<>();
        Set<String> allMicrCodes = new HashSet<>();

        for (NpciChequeData npci : npciCheques) {
            if (npci != null && npci.getChequeNumber() != null) {
                allChequeNumbers.add(npci.getChequeNumber().trim());
                String npciMicr = normalizeMicr(npci.getMicrCode());
                if (!npciMicr.isEmpty()) {
                    allMicrCodes.add(npciMicr);
                }
            }
        }
        for (OcrChequeData ocr : ocrCheques) {
            if (ocr != null) {
                String ocrMicr = normalizeMicr(ocr.getMicrCode());
                if (!ocrMicr.isEmpty()) {
                    allMicrCodes.add(ocrMicr);
                }
            }
        }

        Map<String, String> latestStatuses =
                micrRepairDao.getLatestChequeStatuses(allChequeNumbers);

        Map<String, String> completedRepairs =
                micrRepairDao.getCompletedRepairedMicrs(allChequeNumbers);

        for (String rep : completedRepairs.values()) {
            if (rep != null && rep.trim().length() == 9) {
                allMicrCodes.add(rep.trim());
            }
        }

        Set<String> validMasterMicrs =
                micrMasterDao.findExistingMicrCodes(allMicrCodes);

        Map<String, String> returnReasons =
                micrRepairDao.getLatestChequeReturnReasons(allChequeNumbers);

        for (NpciChequeData npci : npciCheques) {

            if (npci == null) {
                continue;
            }

            String chqNo = npci.getChequeNumber() != null ? npci.getChequeNumber().trim() : "";
            String latestStatus = latestStatuses.get(chqNo);

            boolean returnByMaker =
                    STATUS_RETURN_BY_MAKER.equalsIgnoreCase(
                            latestStatus
                    );

            if ("ACCEPT".equalsIgnoreCase(latestStatus)
                    || "REJECT".equalsIgnoreCase(latestStatus)) {

                continue;
            }

            if (isBatchReturned) {
                if (!micrRepairDao.chequeNeedsMicrRepair(chqNo)) {
                    continue;
                }
            }

            boolean micrRepaired =
                    STATUS_MICR_REPAIRED.equalsIgnoreCase(
                            latestStatus
                    );

            OcrChequeData ocr =
                    ocrByInwardChequeId.get(
                            npci.getInwardChequeId()
                    );
            if (ocr == null && chqNo != null && !chqNo.isEmpty()) {
                ocr = ocrByChequeNumber.get(chqNo);
            }

            if (ocr == null) {
                continue;
            }

            MicrComparisonDto comparison =
                    new MicrComparisonDto();

            comparison.setInwardChequeId(
                    npci.getInwardChequeId()
            );

            comparison.setChequeNumber(
                    npci.getChequeNumber()
            );

            String npciMicr =
                    normalizeMicr(
                            npci.getMicrCode()
                    );

            String ocrMicr =
                    normalizeMicr(
                            ocr.getMicrCode()
                    );

            comparison.setNpciMicrCode(
                    npciMicr
            );

            comparison.setOcrMicrCode(
                    ocrMicr
            );

            String npciCity =
                    getCityCode(npciMicr);

            String npciBank =
                    getBankCode(npciMicr);

            String npciBranch =
                    getBranchCode(npciMicr);

            String ocrCity =
                    getCityCode(ocrMicr);

            String ocrBank =
                    getBankCode(ocrMicr);

            String ocrBranch =
                    getBranchCode(ocrMicr);

            comparison.setNpciCityCode(
                    npciCity
            );

            comparison.setNpciBankCode(
                    npciBank
            );

            comparison.setNpciBranchCode(
                    npciBranch
            );

            comparison.setOcrCityCode(
                    ocrCity
            );

            comparison.setOcrBankCode(
                    ocrBank
            );

            comparison.setOcrBranchCode(
                    ocrBranch
            );

            boolean cityMismatch =
                    isDifferent(
                            npciCity,
                            ocrCity
                    );

            boolean bankMismatch =
                    isDifferent(
                            npciBank,
                            ocrBank
                    );

            boolean branchMismatch =
                    isDifferent(
                            npciBranch,
                            ocrBranch
                    );

            boolean micrMismatch =
                    isDifferent(
                            npciMicr,
                            ocrMicr
                    );

            comparison.setCityCodeMismatch(
                    cityMismatch
            );

            comparison.setBankCodeMismatch(
                    bankMismatch
            );

            comparison.setBranchCodeMismatch(
                    branchMismatch
            );

            comparison.setMicrMismatch(
                    micrMismatch
            );

            boolean npciMicrFound =
                    !npciMicr.isEmpty()
                    && validMasterMicrs.contains(npciMicr);

            boolean ocrMicrFound =
                    !ocrMicr.isEmpty()
                    && validMasterMicrs.contains(ocrMicr);

            comparison.setNpciMicrFoundInMaster(
                    npciMicrFound
            );

            comparison.setOcrMicrFoundInMaster(
                    ocrMicrFound
            );

            boolean needsRepair =
                    cityMismatch
                    || bankMismatch
                    || branchMismatch
                    || micrMismatch
                    || !npciMicrFound;

            // Check if this cheque was already repaired
            String completedMicr = completedRepairs.get(chqNo);

            if (completedMicr != null && completedMicr.trim().length() == 9) {
                String rep = completedMicr.trim();
                comparison.setRepairedMicrCode(rep);
                comparison.setRepairedCityCode(rep.substring(0, 3));
                comparison.setRepairedBankCode(rep.substring(3, 6));
                comparison.setRepairedBranchCode(rep.substring(6, 9));

                if (validMasterMicrs.contains(rep)) {
                    needsRepair = false;
                }
            }

            if (returnByMaker) {
                needsRepair = false;
                comparison.setReturnByMaker(true);
                String returnReason = returnReasons.get(chqNo);
                comparison.setReturnReasonCode(returnReason);
            }

            if (isBatchReturned && "RETURN_TO_MAKER".equalsIgnoreCase(latestStatus)) {
                needsRepair = true;
                micrRepaired = false;
            }

            comparison.setNeedsMicrRepair(
                    needsRepair
            );

            comparison.setMicrRepaired(
                    micrRepaired
            );

            result.add(comparison);
        }

        return result;
    }

    @Override
    public int getBatchChequePosition(
            long batchId,
            String chequeNumber) {

        if (batchId <= 0L
                || chequeNumber == null
                || chequeNumber.trim().isEmpty()) {
            return 0;
        }

        return micrRepairDao.getBatchChequePosition(
            batchId,
            chequeNumber.trim()
        );
    }

    @Override
    public int getBatchTotalChequeCount(long batchId) {
        if (batchId <= 0L) {
            return 0;
        }

        return micrRepairDao.getBatchTotalChequeCount(batchId);
    }

    @Override
    public String getFrontImagePath(String chequeNumber) {
        return micrRepairDao.getFrontImagePath(chequeNumber);
    }

    @Override
    public String getBackImagePath(String chequeNumber) {
        return micrRepairDao.getBackImagePath(chequeNumber);
    }

    @Override
    public List<ReturnReasonDto> getMakerReturnReasons() {
        return micrRepairDao.getMakerReturnReasons();
    }

    @Override
    public boolean saveMakerReturn(
            String chequeNumber,
            String returnReasonCode,
            String makerRemarks,
            long userId) {

        if (chequeNumber == null
                || chequeNumber.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Cheque number is required"
            );
        }

        if (returnReasonCode == null
                || returnReasonCode.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Return reason is required"
            );
        }

        boolean saved = micrRepairDao.saveMakerReturn(
            chequeNumber.trim(),
            returnReasonCode.trim(),
            makerRemarks,
            userId
        );

        if (saved) {
            updateBatchStatusIfMicrStageComplete(
                chequeNumber.trim(),
                userId
            );
        }

        return saved;
    }

    @Override
    public boolean saveMicrRepair(
            String chequeNumber,
            String originalMicr,
            String repairedMicr,
            String remarks,
            long userId) {

        if (chequeNumber == null
                || chequeNumber.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Cheque number is required"
            );
        }

        if (originalMicr == null
                || originalMicr.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Original MICR code is required"
            );
        }

        if (repairedMicr == null
                || repairedMicr.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Corrected MICR code is required"
            );
        }

        originalMicr = originalMicr.trim();
        repairedMicr = repairedMicr.trim();

        if (!repairedMicr.matches("\\d{9}")) {
            throw new IllegalArgumentException(
                "Corrected MICR code must contain exactly 9 digits"
            );
        }

        if (!micrMasterDao.exists(repairedMicr)) {
            throw new IllegalArgumentException(
                "Corrected MICR code "
                + repairedMicr
                + " was not found in MICR master"
            );
        }

        boolean saved =
            micrRepairDao.saveMicrRepair(
                chequeNumber.trim(),
                originalMicr,
                repairedMicr,
                remarks,
                userId
            );

        if (saved) {
            updateBatchStatusIfMicrStageComplete(
                chequeNumber.trim(),
                userId
            );
        }

        return saved;
    }

    private void updateBatchStatusIfMicrStageComplete(
            String chequeNumber,
            long userId) {

        long batchId =
            micrRepairDao.getBatchIdByChequeNumber(
                chequeNumber
            );

        if (batchId <= 0L) {
            return;
        }

        if (!needsMicrRepair(batchId)) {
            if (hasChequesNeedingDataEntry(batchId)) {
                micrRepairDao.markBatchReadyForDataEntry(
                    batchId,
                    userId
                );
            } else {
                micrRepairDao.markBatchReadyForChecker(
                    batchId,
                    userId
                );
            }
        }
    }

    private String normalizeMicr(String micr) {
        if (micr == null) {
            return "";
        }

        return micr.trim();
    }

    private String getCityCode(String micr) {
        if (micr == null || micr.length() != 9) {
            return "";
        }

        return micr.substring(0, 3);
    }

    private String getBankCode(String micr) {
        if (micr == null || micr.length() != 9) {
            return "";
        }

        return micr.substring(3, 6);
    }

    private String getBranchCode(String micr) {
        if (micr == null || micr.length() != 9) {
            return "";
        }

        return micr.substring(6, 9);
    }

    private boolean isDifferent(
            String first,
            String second) {

        String firstValue =
            first == null ? "" : first.trim();

        String secondValue =
            second == null ? "" : second.trim();

        return !firstValue.equals(secondValue);
    }

    @Override
    public boolean markBatchDataEntry(
            long batchId,
            long userId) {

        if (batchId <= 0L || userId <= 0L) {
            return false;
        }

        if (needsMicrRepair(batchId)) {
            return false;
        }

        return micrRepairDao.markBatchReadyForDataEntry(
            batchId,
            userId
        );
    }

    @Override
    public int getBatchMicrCompletedCount(long batchId) {
        List<MicrComparisonDto> comparisons =
            compareBatch(batchId);

        if (comparisons == null) {
            return 0;
        }

        int count = 0;

        for (MicrComparisonDto comparison : comparisons) {
            if (comparison != null
                    && !comparison.isNeedsMicrRepair()) {
                count++;
            }
        }

        return count;
    }

    private boolean isMicrReturnReason(String code) {
        if (code == null) {
            return false;
        }
        String upper = code.trim().toUpperCase();
        return upper.startsWith("CR-MICR-")
                || upper.startsWith("CR-IMG-")
                || upper.startsWith("MR-MICR-")
                || upper.startsWith("MICR_");
    }

    @Override
    public int getBatchMicrPendingCount(long batchId) {
        List<MicrComparisonDto> comparisons =
            compareBatch(batchId);

        if (comparisons == null) {
            return 0;
        }

        int count = 0;

        for (MicrComparisonDto comparison : comparisons) {
            if (comparison != null
                    && comparison.isNeedsMicrRepair()) {
                count++;
            }
        }

        return count;
    }

    @Override
    public boolean hasChequesNeedingDataEntry(long batchId) {
        return micrRepairDao.hasChequesNeedingDataEntry(batchId);
    }
}