package com.cts.inward.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class MicrRepairServiceImpl
        implements MicrRepairService {

    private static final String STATUS_RETURN_BY_MAKER =
            "RETURN_BY_MAKER";

    private final BatchDao batchDao;

    private final MicrRepairDao micrRepairDao;

    private final MicrMasterDao micrMasterDao;


    public MicrRepairServiceImpl() {

        batchDao =
                BatchDaoImpl.of();

        micrRepairDao =
                new MicrRepairDaoImpl();

        micrMasterDao =
                MicrMasterDaoImpl.of();
    }


    @Override
    public List<MicrRepairBatchDto> getRepairBatches() {

        List<MicrRepairBatchDto> result =
                new ArrayList<>();

        List<NpciBatchData> batches =
                batchDao.getAllBatches();

        if (batches == null) {

            return result;
        }


        for (NpciBatchData batch :
                batches) {

            List<MicrComparisonDto> comparisons =
                    compareBatch(
                            batch.getBatchId());

            int micrErrorCount = 0;


            if (comparisons != null) {

                for (MicrComparisonDto comparison :
                        comparisons) {

                    if (comparison != null
                            && comparison.isNeedsMicrRepair()) {

                        micrErrorCount++;
                    }
                }
            }


            if (micrErrorCount > 0) {

                result.add(
                        new MicrRepairBatchDto(
                                batch.getBatchId(),
                                batch.getTotalCheques(),
                                micrErrorCount));
            }
        }


        return result;
    }


    @Override
    public boolean needsMicrRepair(
            long batchId) {

        List<MicrComparisonDto> comparisons =
                compareBatch(batchId);

        if (comparisons == null) {

            return false;
        }


        for (MicrComparisonDto comparison :
                comparisons) {

            if (comparison != null
                    && comparison.isNeedsMicrRepair()) {

                return true;
            }
        }


        return false;
    }


    @Override
    public int getNextRepairIndex(
            long batchId) {

        List<MicrComparisonDto> comparisons =
                compareBatch(batchId);

        if (comparisons == null) {

            return -1;
        }


        for (int i = 0;
                i < comparisons.size();
                i++) {

            MicrComparisonDto comparison =
                    comparisons.get(i);

            if (comparison != null
                    && comparison.isNeedsMicrRepair()) {

                return i;
            }
        }


        return -1;
    }


    @Override
    public List<MicrComparisonDto> compareBatch(
            long batchId) {

        List<NpciChequeData> npciCheques =
                micrRepairDao
                        .getNpciCheques(
                                batchId);

        List<OcrChequeData> ocrCheques =
                micrRepairDao
                        .getOcrCheques(
                                batchId);

        List<MicrComparisonDto> result =
                new ArrayList<>();


        if (npciCheques == null
                || npciCheques.isEmpty()
                || ocrCheques == null
                || ocrCheques.isEmpty()) {

            return result;
        }


        Map<Long, OcrChequeData>
                ocrByInwardChequeId =
                new HashMap<>();


        for (OcrChequeData ocr :
                ocrCheques) {

            if (ocr == null) {

                continue;
            }


            ocrByInwardChequeId.put(
                    ocr.getInwardChequeId(),
                    ocr);
        }


        for (NpciChequeData npci :
                npciCheques) {

            if (npci == null) {

                continue;
            }


            String latestStatus =
                    micrRepairDao
                            .getLatestChequeStatus(
                                    npci.getChequeNumber());


            /*
             * A returned cheque has completed its Maker MICR stage.
             *
             * Never show it again in MICR Repair.
             */
            if (STATUS_RETURN_BY_MAKER
                    .equalsIgnoreCase(
                            latestStatus)) {

                continue;
            }


            OcrChequeData ocr =
                    ocrByInwardChequeId.get(
                            npci.getInwardChequeId());


            if (ocr == null) {

                continue;
            }


            MicrComparisonDto comparison =
                    new MicrComparisonDto();


            comparison.setInwardChequeId(
                    npci.getInwardChequeId());

            comparison.setChequeNumber(
                    npci.getChequeNumber());


            String npciMicr =
                    normalizeMicr(
                            npci.getMicrCode());

            String ocrMicr =
                    normalizeMicr(
                            ocr.getMicrCode());


            comparison.setNpciMicrCode(
                    npciMicr);

            comparison.setOcrMicrCode(
                    ocrMicr);


            String npciCity =
                    getCityCode(
                            npciMicr);

            String npciBank =
                    getBankCode(
                            npciMicr);

            String npciBranch =
                    getBranchCode(
                            npciMicr);


            String ocrCity =
                    getCityCode(
                            ocrMicr);

            String ocrBank =
                    getBankCode(
                            ocrMicr);

            String ocrBranch =
                    getBranchCode(
                            ocrMicr);


            comparison.setNpciCityCode(
                    npciCity);

            comparison.setNpciBankCode(
                    npciBank);

            comparison.setNpciBranchCode(
                    npciBranch);


            comparison.setOcrCityCode(
                    ocrCity);

            comparison.setOcrBankCode(
                    ocrBank);

            comparison.setOcrBranchCode(
                    ocrBranch);


            boolean cityMismatch =
                    isDifferent(
                            npciCity,
                            ocrCity);

            boolean bankMismatch =
                    isDifferent(
                            npciBank,
                            ocrBank);

            boolean branchMismatch =
                    isDifferent(
                            npciBranch,
                            ocrBranch);

            boolean micrMismatch =
                    isDifferent(
                            npciMicr,
                            ocrMicr);


            comparison.setCityCodeMismatch(
                    cityMismatch);

            comparison.setBankCodeMismatch(
                    bankMismatch);

            comparison.setBranchCodeMismatch(
                    branchMismatch);

            comparison.setMicrMismatch(
                    micrMismatch);


            boolean npciMicrFound =
                    !npciMicr.isEmpty()
                            && micrMasterDao.exists(
                                    npciMicr);


            boolean ocrMicrFound =
                    !ocrMicr.isEmpty()
                            && micrMasterDao.exists(
                                    ocrMicr);


            comparison.setNpciMicrFoundInMaster(
                    npciMicrFound);

            comparison.setOcrMicrFoundInMaster(
                    ocrMicrFound);


            boolean needsRepair =
                    cityMismatch
                            || bankMismatch
                            || branchMismatch
                            || micrMismatch
                            || !npciMicrFound;


            /*
             * A completed valid repair overrides the original
             * mismatch for workflow purposes.
             */
            if (needsRepair) {

                String completedMicr =
                        micrRepairDao
                                .getCompletedRepairedMicr(
                                        npci.getChequeNumber());


                if (completedMicr != null
                        && !completedMicr.trim().isEmpty()
                        && micrMasterDao.exists(
                                completedMicr.trim())) {

                    needsRepair = false;
                }
            }


            comparison.setNeedsMicrRepair(
                    needsRepair);


            result.add(comparison);
        }


        return result;
    }


    @Override
    public String getFrontImagePath(
            String chequeNumber) {

        return micrRepairDao
                .getFrontImagePath(
                        chequeNumber);
    }


    @Override
    public String getBackImagePath(
            String chequeNumber) {

        return micrRepairDao
                .getBackImagePath(
                        chequeNumber);
    }


    @Override
    public List<ReturnReasonDto>
            getMakerReturnReasons() {

        return micrRepairDao
                .getMakerReturnReasons();
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
                    "Cheque number is required");
        }


        if (returnReasonCode == null
                || returnReasonCode.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Return reason is required");
        }


        return micrRepairDao
                .saveMakerReturn(
                        chequeNumber.trim(),
                        returnReasonCode.trim(),
                        makerRemarks,
                        userId);
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
                    "Cheque number is required");
        }


        if (originalMicr == null
                || originalMicr.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Original MICR code is required");
        }


        if (repairedMicr == null
                || repairedMicr.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Corrected MICR code is required");
        }


        originalMicr =
                originalMicr.trim();

        repairedMicr =
                repairedMicr.trim();


        if (!repairedMicr.matches(
                "\\d{9}")) {

            throw new IllegalArgumentException(
                    "Corrected MICR code must contain exactly 9 digits");
        }


        /*
         * Corrected MICR MUST exist in master.
         */
        if (!micrMasterDao.exists(
                repairedMicr)) {

            throw new IllegalArgumentException(
                    "Corrected MICR code "
                            + repairedMicr
                            + " was not found in MICR master");
        }


        boolean saved =
                micrRepairDao.saveMicrRepair(
                        chequeNumber.trim(),
                        originalMicr,
                        repairedMicr,
                        remarks,
                        userId);


        if (saved) {

            updateBatchStatusIfMicrStageComplete(
                    chequeNumber.trim(),
                    userId);
        }


        return saved;
    }


    private void updateBatchStatusIfMicrStageComplete(
            String chequeNumber,
            long userId) {

        long batchId =
                micrRepairDao
                        .getBatchIdByChequeNumber(
                                chequeNumber);


        if (batchId <= 0L) {

            return;
        }


        /*
         * compareBatch() ignores RETURN_BY_MAKER cheques.
         *
         * Therefore when no cheque needs repair,
         * the MICR stage is complete.
         */
        if (!needsMicrRepair(
                batchId)) {

            micrRepairDao
                    .markBatchReadyForDataEntry(
                            batchId,
                            userId);
        }
    }


    @Override
    public boolean markBatchDataEntry(
            long batchId,
            long userId) {

        if (batchId <= 0L
                || userId <= 0L) {

            return false;
        }


        return micrRepairDao
                .markBatchReadyForDataEntry(
                        batchId,
                        userId);
    }


    private String normalizeMicr(
            String micr) {

        if (micr == null) {

            return "";
        }

        return micr.trim();
    }


    private String getCityCode(
            String micr) {

        if (micr == null
                || micr.length() != 9) {

            return "";
        }

        return micr.substring(
                0,
                3);
    }


    private String getBankCode(
            String micr) {

        if (micr == null
                || micr.length() != 9) {

            return "";
        }

        return micr.substring(
                3,
                6);
    }


    private String getBranchCode(
            String micr) {

        if (micr == null
                || micr.length() != 9) {

            return "";
        }

        return micr.substring(
                6,
                9);
    }


    private boolean isDifferent(
            String first,
            String second) {

        String firstValue =
                first == null
                        ? ""
                        : first.trim();

        String secondValue =
                second == null
                        ? ""
                        : second.trim();

        return !firstValue.equals(
                secondValue);
    }
}