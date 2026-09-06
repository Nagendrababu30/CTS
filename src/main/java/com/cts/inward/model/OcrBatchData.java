package com.cts.inward.model;

import java.util.List;

public class OcrBatchData {

    private final long batchId;
    private final String presentingBankName;
    private final int totalCheque;
    private final String fileId;
    private final List<OcrChequeData> cheques;

    private OcrBatchData(
            long batchId,
            String presentingBankName,
            int totalCheque,
            String fileId,
            List<OcrChequeData> cheques) {

        this.batchId = batchId;
        this.presentingBankName = presentingBankName;
        this.totalCheque = totalCheque;
        this.fileId = fileId;
        this.cheques = cheques;
    }

    public static OcrBatchData of(
            long batchId,
            String presentingBankName,
            int totalCheque,
            String fileId,
            List<OcrChequeData> cheques) {

        return new OcrBatchData(
                batchId,
                presentingBankName,
                totalCheque,
                fileId,
                cheques);
    }

    public long getBatchId() {
        return batchId;
    }

    public String getPresentingBankName() {
        return presentingBankName;
    }

    public int getTotalCheque() {
        return totalCheque;
    }

    public String getFileId() {
        return fileId;
    }

    public List<OcrChequeData> getCheques() {
        return cheques;
    }
}