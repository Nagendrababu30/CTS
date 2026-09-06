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

    private final BatchDao batchDao;
    private final MicrRepairDao micrRepairDao;
    private final MicrMasterDao micrMasterDao;

    public MicrRepairServiceImpl() {

        this.batchDao =
                BatchDaoImpl.of();

        this.micrRepairDao =
                new MicrRepairDaoImpl();

        this.micrMasterDao =
                MicrMasterDaoImpl.of();
    }

    // -------------------------------------------------------------------------
    // Get batches requiring MICR repair
    // -------------------------------------------------------------------------

    @Override
    public List<MicrRepairBatchDto> getRepairBatches() {

        List<MicrRepairBatchDto> result =
                new ArrayList<>();

        List<NpciBatchData> batches =
                batchDao.getAllBatches();

        if (batches == null) {
            return result;
        }

        for (NpciBatchData batch : batches) {

            List<MicrComparisonDto> comparisons =
                    compareBatch(
                            batch.getBatchId());

            int micrErrorCount = 0;

            if (comparisons != null) {

                for (MicrComparisonDto comparison :
                        comparisons) {

                    if (comparison.isNeedsMicrRepair()) {

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

    // -------------------------------------------------------------------------
    // Check whether a batch needs MICR repair
    // -------------------------------------------------------------------------

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

            if (comparison.isNeedsMicrRepair()) {

                return true;
            }
        }

        return false;
    }

    // -------------------------------------------------------------------------
    // Find first unfinished MICR repair
    // -------------------------------------------------------------------------

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

            if (comparisons.get(i)
                    .isNeedsMicrRepair()) {

                return i;
            }
        }

        return -1;
    }

    // -------------------------------------------------------------------------
    // Compare NPCI and OCR
    // -------------------------------------------------------------------------

    @Override
    public List<MicrComparisonDto> compareBatch(
            long batchId) {

        List<NpciChequeData> npciCheques =
                micrRepairDao.getNpciCheques(
                        batchId);

        List<OcrChequeData> ocrCheques =
                micrRepairDao.getOcrCheques(
                        batchId);

        List<MicrComparisonDto> result =
                new ArrayList<>();

        if (npciCheques == null
                || npciCheques.isEmpty()) {

            return result;
        }

        if (ocrCheques == null) {

            return result;
        }


        /*
         * Match OCR data using cheque number.
         */
        Map<String, OcrChequeData> ocrByChequeNumber =
                new HashMap<>();

        for (OcrChequeData ocr :
                ocrCheques) {

            if (ocr == null
                    || ocr.getChequeNumber() == null) {

                continue;
            }

            ocrByChequeNumber.put(
                    ocr.getChequeNumber(),
                    ocr);
        }


        for (NpciChequeData npci :
                npciCheques) {

            if (npci == null
                    || npci.getChequeNumber() == null) {

                continue;
            }


            OcrChequeData ocr =
                    ocrByChequeNumber.get(
                            npci.getChequeNumber());


            /*
             * There is no OCR record for this cheque.
             */
            if (ocr == null) {

                continue;
            }


            MicrComparisonDto comparison =
                    new MicrComparisonDto();


            // -----------------------------------------------------------------
            // Cheque number
            // -----------------------------------------------------------------

            comparison.setChequeNumber(
                    npci.getChequeNumber());


            // -----------------------------------------------------------------
            // NPCI MICR
            // -----------------------------------------------------------------

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


            // -----------------------------------------------------------------
            // IMPORTANT
            //
            // Derive City / Bank / Branch from the actual MICR code.
            //
            // Do NOT use the separate city_code / bank_code /
            // branch_code columns here for MICR comparison.
            // -----------------------------------------------------------------

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


            // -----------------------------------------------------------------
            // Component mismatch
            // -----------------------------------------------------------------

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


            /*
             * Complete MICR mismatch is retained as information,
             * but the UI will highlight the individual mismatching
             * component instead of highlighting the whole MICR field.
             */
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


            // -----------------------------------------------------------------
            // MICR master validation
            // -----------------------------------------------------------------

            boolean npciMicrFound =
                    npciMicr != null
                    && !npciMicr.isEmpty()
                    && micrMasterDao.exists(
                            npciMicr);


            boolean ocrMicrFound =
                    ocrMicr != null
                    && !ocrMicr.isEmpty()
                    && micrMasterDao.exists(
                            ocrMicr);


            comparison.setNpciMicrFoundInMaster(
                    npciMicrFound);

            comparison.setOcrMicrFoundInMaster(
                    ocrMicrFound);


            // -----------------------------------------------------------------
            // Determine whether repair is required
            // -----------------------------------------------------------------

            boolean needsRepair =
                    cityMismatch
                    || bankMismatch
                    || branchMismatch
                    || micrMismatch
                    || !npciMicrFound;


            /*
             * If a previous valid repair was completed,
             * this cheque no longer needs repair.
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

    // -------------------------------------------------------------------------
    // Front image
    // -------------------------------------------------------------------------

    @Override
    public String getFrontImagePath(
            String chequeNumber) {

        return micrRepairDao
                .getFrontImagePath(
                        chequeNumber);
    }

    // -------------------------------------------------------------------------
    // Back image
    // -------------------------------------------------------------------------

    @Override
    public String getBackImagePath(
            String chequeNumber) {

        return micrRepairDao
                .getBackImagePath(
                        chequeNumber);
    }

    // -------------------------------------------------------------------------
    // Maker return reasons
    // -------------------------------------------------------------------------

    @Override
    public List<ReturnReasonDto> getMakerReturnReasons() {

        return micrRepairDao
                .getMakerReturnReasons();
    }

    // -------------------------------------------------------------------------
    // Save Maker return
    // -------------------------------------------------------------------------

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


        return micrRepairDao.saveMakerReturn(
                chequeNumber,
                returnReasonCode,
                makerRemarks,
                userId);
    }

    // -------------------------------------------------------------------------
    // Save MICR repair
    // -------------------------------------------------------------------------

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
         * Corrected MICR must exist in MICR master.
         */
        if (!micrMasterDao.exists(
                repairedMicr)) {

            throw new IllegalArgumentException(
                    "Corrected MICR code "
                            + repairedMicr
                            + " was not found in MICR master");
        }


        return micrRepairDao.saveMicrRepair(
                chequeNumber,
                originalMicr,
                repairedMicr,
                remarks,
                userId);
    }

    // -------------------------------------------------------------------------
    // Normalize MICR
    // -------------------------------------------------------------------------

    private String normalizeMicr(
            String micr) {

        if (micr == null) {

            return "";
        }

        return micr.trim();
    }

    // -------------------------------------------------------------------------
    // City component
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Bank component
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Branch component
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Compare values
    // -------------------------------------------------------------------------

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