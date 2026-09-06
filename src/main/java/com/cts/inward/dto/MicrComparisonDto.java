package com.cts.inward.dto;

public class MicrComparisonDto {

    private String chequeNumber;

    /*
     * NPCI values
     */
    private String npciCityCode;
    private String npciBankCode;
    private String npciBranchCode;
    private String npciMicrCode;

    /*
     * OCR values
     */
    private String ocrCityCode;
    private String ocrBankCode;
    private String ocrBranchCode;
    private String ocrMicrCode;

    /*
     * Comparison flags
     */
    private boolean cityCodeMismatch;
    private boolean bankCodeMismatch;
    private boolean branchCodeMismatch;
    private boolean micrMismatch;

    /*
     * MICR master validation
     */
    private boolean npciMicrFoundInMaster;
    private boolean ocrMicrFoundInMaster;

    /*
     * Final business decision
     */
    private boolean needsMicrRepair;

    public MicrComparisonDto() {
    }

    public String getChequeNumber() {
        return chequeNumber;
    }

    public void setChequeNumber(String chequeNumber) {
        this.chequeNumber = chequeNumber;
    }

    public String getNpciCityCode() {
        return npciCityCode;
    }

    public void setNpciCityCode(String npciCityCode) {
        this.npciCityCode = npciCityCode;
    }

    public String getNpciBankCode() {
        return npciBankCode;
    }

    public void setNpciBankCode(String npciBankCode) {
        this.npciBankCode = npciBankCode;
    }

    public String getNpciBranchCode() {
        return npciBranchCode;
    }

    public void setNpciBranchCode(String npciBranchCode) {
        this.npciBranchCode = npciBranchCode;
    }

    public String getNpciMicrCode() {
        return npciMicrCode;
    }

    public void setNpciMicrCode(String npciMicrCode) {
        this.npciMicrCode = npciMicrCode;
    }

    public String getOcrCityCode() {
        return ocrCityCode;
    }

    public void setOcrCityCode(String ocrCityCode) {
        this.ocrCityCode = ocrCityCode;
    }

    public String getOcrBankCode() {
        return ocrBankCode;
    }

    public void setOcrBankCode(String ocrBankCode) {
        this.ocrBankCode = ocrBankCode;
    }

    public String getOcrBranchCode() {
        return ocrBranchCode;
    }

    public void setOcrBranchCode(String ocrBranchCode) {
        this.ocrBranchCode = ocrBranchCode;
    }

    public String getOcrMicrCode() {
        return ocrMicrCode;
    }

    public void setOcrMicrCode(String ocrMicrCode) {
        this.ocrMicrCode = ocrMicrCode;
    }

    public boolean isCityCodeMismatch() {
        return cityCodeMismatch;
    }

    public void setCityCodeMismatch(boolean cityCodeMismatch) {
        this.cityCodeMismatch = cityCodeMismatch;
    }

    public boolean isBankCodeMismatch() {
        return bankCodeMismatch;
    }

    public void setBankCodeMismatch(boolean bankCodeMismatch) {
        this.bankCodeMismatch = bankCodeMismatch;
    }

    public boolean isBranchCodeMismatch() {
        return branchCodeMismatch;
    }

    public void setBranchCodeMismatch(boolean branchCodeMismatch) {
        this.branchCodeMismatch = branchCodeMismatch;
    }

    public boolean isMicrMismatch() {
        return micrMismatch;
    }

    public void setMicrMismatch(boolean micrMismatch) {
        this.micrMismatch = micrMismatch;
    }

    public boolean isNpciMicrFoundInMaster() {
        return npciMicrFoundInMaster;
    }

    public void setNpciMicrFoundInMaster(boolean npciMicrFoundInMaster) {
        this.npciMicrFoundInMaster = npciMicrFoundInMaster;
    }

    public boolean isOcrMicrFoundInMaster() {
        return ocrMicrFoundInMaster;
    }

    public void setOcrMicrFoundInMaster(boolean ocrMicrFoundInMaster) {
        this.ocrMicrFoundInMaster = ocrMicrFoundInMaster;
    }

    public boolean isNeedsMicrRepair() {
        return needsMicrRepair;
    }

    public void setNeedsMicrRepair(boolean needsMicrRepair) {
        this.needsMicrRepair = needsMicrRepair;
    }

    /*
     * Only MICR-related differences matter here.
     *
     * Cheque number is NOT a mismatch.
     * It is only used to match NPCI cheque with OCR cheque.
     */
    public boolean hasMismatch() {
        return cityCodeMismatch
                || bankCodeMismatch
                || branchCodeMismatch
                || micrMismatch;
    }

    @Override
    public String toString() {
        return "MicrComparisonDto ["
                + "chequeNumber=" + chequeNumber
                + ", npciCityCode=" + npciCityCode
                + ", npciBankCode=" + npciBankCode
                + ", npciBranchCode=" + npciBranchCode
                + ", npciMicrCode=" + npciMicrCode
                + ", ocrCityCode=" + ocrCityCode
                + ", ocrBankCode=" + ocrBankCode
                + ", ocrBranchCode=" + ocrBranchCode
                + ", ocrMicrCode=" + ocrMicrCode
                + ", cityCodeMismatch=" + cityCodeMismatch
                + ", bankCodeMismatch=" + bankCodeMismatch
                + ", branchCodeMismatch=" + branchCodeMismatch
                + ", micrMismatch=" + micrMismatch
                + ", npciMicrFoundInMaster=" + npciMicrFoundInMaster
                + ", ocrMicrFoundInMaster=" + ocrMicrFoundInMaster
                + ", needsMicrRepair=" + needsMicrRepair
                + "]";
    }
}