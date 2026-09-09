package com.cts.inward.dto;

public class MicrComparisonDto {

    private long inwardChequeId;

    private String chequeNumber;

    // -------------------------------------------------------------------------
    // NPCI data
    // -------------------------------------------------------------------------

    private String npciAccountNumber;
    private String npciDrawerName;
    private java.math.BigDecimal npciAmount;
    private java.time.LocalDate npciChequeDate;

    private String npciCityCode;
    private String npciBankCode;
    private String npciBranchCode;
    private String npciMicrCode;

    // -------------------------------------------------------------------------
    // OCR data
    // -------------------------------------------------------------------------

    private String ocrAccountNumber;
    private String ocrDrawerName;
    private java.math.BigDecimal ocrAmount;
    private java.time.LocalDate ocrChequeDate;

    private String ocrCityCode;
    private String ocrBankCode;
    private String ocrBranchCode;
    private String ocrMicrCode;

    // -------------------------------------------------------------------------
    // MICR mismatch flags
    // -------------------------------------------------------------------------

    private boolean cityCodeMismatch;
    private boolean bankCodeMismatch;
    private boolean branchCodeMismatch;
    private boolean micrMismatch;

    // -------------------------------------------------------------------------
    // Other mismatch flags
    // -------------------------------------------------------------------------

    private boolean accountNumberMismatch;
    private boolean drawerNameMismatch;
    private boolean amountMismatch;
    private boolean chequeDateMismatch;

    // -------------------------------------------------------------------------
    // MICR master validation
    // -------------------------------------------------------------------------

    private boolean npciMicrFoundInMaster;
    private boolean ocrMicrFoundInMaster;

    // -------------------------------------------------------------------------
    // Overall MICR repair flag
    // -------------------------------------------------------------------------

    private boolean needsMicrRepair;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public MicrComparisonDto() {
    }

    // -------------------------------------------------------------------------
    // Inward cheque ID
    // -------------------------------------------------------------------------

    public long getInwardChequeId() {
        return inwardChequeId;
    }

    public void setInwardChequeId(long inwardChequeId) {
        this.inwardChequeId = inwardChequeId;
    }

    // -------------------------------------------------------------------------
    // Cheque number
    // -------------------------------------------------------------------------

    public String getChequeNumber() {
        return chequeNumber;
    }

    public void setChequeNumber(String chequeNumber) {
        this.chequeNumber = chequeNumber;
    }

    // -------------------------------------------------------------------------
    // NPCI account number
    // -------------------------------------------------------------------------

    public String getNpciAccountNumber() {
        return npciAccountNumber;
    }

    public void setNpciAccountNumber(String npciAccountNumber) {
        this.npciAccountNumber = npciAccountNumber;
    }

    // -------------------------------------------------------------------------
    // NPCI drawer name
    // -------------------------------------------------------------------------

    public String getNpciDrawerName() {
        return npciDrawerName;
    }

    public void setNpciDrawerName(String npciDrawerName) {
        this.npciDrawerName = npciDrawerName;
    }

    // -------------------------------------------------------------------------
    // NPCI amount
    // -------------------------------------------------------------------------

    public java.math.BigDecimal getNpciAmount() {
        return npciAmount;
    }

    public void setNpciAmount(java.math.BigDecimal npciAmount) {
        this.npciAmount = npciAmount;
    }

    // -------------------------------------------------------------------------
    // NPCI cheque date
    // -------------------------------------------------------------------------

    public java.time.LocalDate getNpciChequeDate() {
        return npciChequeDate;
    }

    public void setNpciChequeDate(
            java.time.LocalDate npciChequeDate) {

        this.npciChequeDate = npciChequeDate;
    }

    // -------------------------------------------------------------------------
    // NPCI city code
    // -------------------------------------------------------------------------

    public String getNpciCityCode() {
        return npciCityCode;
    }

    public void setNpciCityCode(String npciCityCode) {
        this.npciCityCode = npciCityCode;
    }

    // -------------------------------------------------------------------------
    // NPCI bank code
    // -------------------------------------------------------------------------

    public String getNpciBankCode() {
        return npciBankCode;
    }

    public void setNpciBankCode(String npciBankCode) {
        this.npciBankCode = npciBankCode;
    }

    // -------------------------------------------------------------------------
    // NPCI branch code
    // -------------------------------------------------------------------------

    public String getNpciBranchCode() {
        return npciBranchCode;
    }

    public void setNpciBranchCode(String npciBranchCode) {
        this.npciBranchCode = npciBranchCode;
    }

    // -------------------------------------------------------------------------
    // NPCI MICR
    // -------------------------------------------------------------------------

    public String getNpciMicrCode() {
        return npciMicrCode;
    }

    public void setNpciMicrCode(String npciMicrCode) {
        this.npciMicrCode = npciMicrCode;
    }

    // -------------------------------------------------------------------------
    // OCR account number
    // -------------------------------------------------------------------------

    public String getOcrAccountNumber() {
        return ocrAccountNumber;
    }

    public void setOcrAccountNumber(String ocrAccountNumber) {
        this.ocrAccountNumber = ocrAccountNumber;
    }

    // -------------------------------------------------------------------------
    // OCR drawer name
    // -------------------------------------------------------------------------

    public String getOcrDrawerName() {
        return ocrDrawerName;
    }

    public void setOcrDrawerName(String ocrDrawerName) {
        this.ocrDrawerName = ocrDrawerName;
    }

    // -------------------------------------------------------------------------
    // OCR amount
    // -------------------------------------------------------------------------

    public java.math.BigDecimal getOcrAmount() {
        return ocrAmount;
    }

    public void setOcrAmount(java.math.BigDecimal ocrAmount) {
        this.ocrAmount = ocrAmount;
    }

    // -------------------------------------------------------------------------
    // OCR cheque date
    // -------------------------------------------------------------------------

    public java.time.LocalDate getOcrChequeDate() {
        return ocrChequeDate;
    }

    public void setOcrChequeDate(
            java.time.LocalDate ocrChequeDate) {

        this.ocrChequeDate = ocrChequeDate;
    }

    // -------------------------------------------------------------------------
    // OCR city code
    // -------------------------------------------------------------------------

    public String getOcrCityCode() {
        return ocrCityCode;
    }

    public void setOcrCityCode(String ocrCityCode) {
        this.ocrCityCode = ocrCityCode;
    }

    // -------------------------------------------------------------------------
    // OCR bank code
    // -------------------------------------------------------------------------

    public String getOcrBankCode() {
        return ocrBankCode;
    }

    public void setOcrBankCode(String ocrBankCode) {
        this.ocrBankCode = ocrBankCode;
    }

    // -------------------------------------------------------------------------
    // OCR branch code
    // -------------------------------------------------------------------------

    public String getOcrBranchCode() {
        return ocrBranchCode;
    }

    public void setOcrBranchCode(String ocrBranchCode) {
        this.ocrBranchCode = ocrBranchCode;
    }

    // -------------------------------------------------------------------------
    // OCR MICR
    // -------------------------------------------------------------------------

    public String getOcrMicrCode() {
        return ocrMicrCode;
    }

    public void setOcrMicrCode(String ocrMicrCode) {
        this.ocrMicrCode = ocrMicrCode;
    }

    // -------------------------------------------------------------------------
    // Account number mismatch
    // -------------------------------------------------------------------------

    public boolean isAccountNumberMismatch() {
        return accountNumberMismatch;
    }

    public void setAccountNumberMismatch(
            boolean accountNumberMismatch) {

        this.accountNumberMismatch =
                accountNumberMismatch;
    }

    // -------------------------------------------------------------------------
    // Drawer name mismatch
    // -------------------------------------------------------------------------

    public boolean isDrawerNameMismatch() {
        return drawerNameMismatch;
    }

    public void setDrawerNameMismatch(
            boolean drawerNameMismatch) {

        this.drawerNameMismatch =
                drawerNameMismatch;
    }

    // -------------------------------------------------------------------------
    // Amount mismatch
    // -------------------------------------------------------------------------

    public boolean isAmountMismatch() {
        return amountMismatch;
    }

    public void setAmountMismatch(
            boolean amountMismatch) {

        this.amountMismatch =
                amountMismatch;
    }

    // -------------------------------------------------------------------------
    // Cheque date mismatch
    // -------------------------------------------------------------------------

    public boolean isChequeDateMismatch() {
        return chequeDateMismatch;
    }

    public void setChequeDateMismatch(
            boolean chequeDateMismatch) {

        this.chequeDateMismatch =
                chequeDateMismatch;
    }

    // -------------------------------------------------------------------------
    // City code mismatch
    // -------------------------------------------------------------------------

    public boolean isCityCodeMismatch() {
        return cityCodeMismatch;
    }

    public void setCityCodeMismatch(
            boolean cityCodeMismatch) {

        this.cityCodeMismatch =
                cityCodeMismatch;
    }

    // -------------------------------------------------------------------------
    // Bank code mismatch
    // -------------------------------------------------------------------------

    public boolean isBankCodeMismatch() {
        return bankCodeMismatch;
    }

    public void setBankCodeMismatch(
            boolean bankCodeMismatch) {

        this.bankCodeMismatch =
                bankCodeMismatch;
    }

    // -------------------------------------------------------------------------
    // Branch code mismatch
    // -------------------------------------------------------------------------

    public boolean isBranchCodeMismatch() {
        return branchCodeMismatch;
    }

    public void setBranchCodeMismatch(
            boolean branchCodeMismatch) {

        this.branchCodeMismatch =
                branchCodeMismatch;
    }

    // -------------------------------------------------------------------------
    // MICR mismatch
    // -------------------------------------------------------------------------

    public boolean isMicrMismatch() {
        return micrMismatch;
    }

    public void setMicrMismatch(
            boolean micrMismatch) {

        this.micrMismatch =
                micrMismatch;
    }

    // -------------------------------------------------------------------------
    // NPCI MICR exists in master
    // -------------------------------------------------------------------------

    public boolean isNpciMicrFoundInMaster() {
        return npciMicrFoundInMaster;
    }

    public void setNpciMicrFoundInMaster(
            boolean npciMicrFoundInMaster) {

        this.npciMicrFoundInMaster =
                npciMicrFoundInMaster;
    }

    // -------------------------------------------------------------------------
    // OCR MICR exists in master
    // -------------------------------------------------------------------------

    public boolean isOcrMicrFoundInMaster() {
        return ocrMicrFoundInMaster;
    }

    public void setOcrMicrFoundInMaster(
            boolean ocrMicrFoundInMaster) {

        this.ocrMicrFoundInMaster =
                ocrMicrFoundInMaster;
    }

    // -------------------------------------------------------------------------
    // Needs MICR repair
    // -------------------------------------------------------------------------

    public boolean isNeedsMicrRepair() {
        return needsMicrRepair;
    }

    public void setNeedsMicrRepair(
            boolean needsMicrRepair) {

        this.needsMicrRepair =
                needsMicrRepair;
    }

    // -------------------------------------------------------------------------
    // MICR mismatch only
    // -------------------------------------------------------------------------

    public boolean hasMismatch() {

        return cityCodeMismatch
                || bankCodeMismatch
                || branchCodeMismatch
                || micrMismatch;
    }

    // -------------------------------------------------------------------------
    // ToString
    // -------------------------------------------------------------------------

    @Override
    public String toString() {

        return "MicrComparisonDto ["
                + "inwardChequeId=" + inwardChequeId
                + ", chequeNumber=" + chequeNumber

                + ", npciAccountNumber=" + npciAccountNumber
                + ", npciDrawerName=" + npciDrawerName
                + ", npciAmount=" + npciAmount
                + ", npciChequeDate=" + npciChequeDate

                + ", npciCityCode=" + npciCityCode
                + ", npciBankCode=" + npciBankCode
                + ", npciBranchCode=" + npciBranchCode
                + ", npciMicrCode=" + npciMicrCode

                + ", ocrAccountNumber=" + ocrAccountNumber
                + ", ocrDrawerName=" + ocrDrawerName
                + ", ocrAmount=" + ocrAmount
                + ", ocrChequeDate=" + ocrChequeDate

                + ", ocrCityCode=" + ocrCityCode
                + ", ocrBankCode=" + ocrBankCode
                + ", ocrBranchCode=" + ocrBranchCode
                + ", ocrMicrCode=" + ocrMicrCode

                + ", accountNumberMismatch="
                + accountNumberMismatch

                + ", drawerNameMismatch="
                + drawerNameMismatch

                + ", amountMismatch="
                + amountMismatch

                + ", chequeDateMismatch="
                + chequeDateMismatch

                + ", cityCodeMismatch="
                + cityCodeMismatch

                + ", bankCodeMismatch="
                + bankCodeMismatch

                + ", branchCodeMismatch="
                + branchCodeMismatch

                + ", micrMismatch="
                + micrMismatch

                + ", npciMicrFoundInMaster="
                + npciMicrFoundInMaster

                + ", ocrMicrFoundInMaster="
                + ocrMicrFoundInMaster

                + ", needsMicrRepair="
                + needsMicrRepair

                + "]";
    }
}