package com.cts.inward.dto;

public class MicrRepairBatchDto {

    private long batchId;

    private int totalCheques;

    private int micrErrorCount;

    public MicrRepairBatchDto() {
    }

    public MicrRepairBatchDto(
            long batchId,
            int totalCheques,
            int micrErrorCount) {

        this.batchId = batchId;
        this.totalCheques = totalCheques;
        this.micrErrorCount = micrErrorCount;
    }

    public long getBatchId() {
        return batchId;
    }

    public void setBatchId(long batchId) {
        this.batchId = batchId;
    }

    public int getTotalCheques() {
        return totalCheques;
    }

    public void setTotalCheques(int totalCheques) {
        this.totalCheques = totalCheques;
    }

    public int getMicrErrorCount() {
        return micrErrorCount;
    }

    public void setMicrErrorCount(int micrErrorCount) {
        this.micrErrorCount = micrErrorCount;
    }

    @Override
    public String toString() {
        return "MicrRepairBatchDto ["
                + "batchId=" + batchId
                + ", totalCheques=" + totalCheques
                + ", micrErrorCount=" + micrErrorCount
                + "]";
    }
}