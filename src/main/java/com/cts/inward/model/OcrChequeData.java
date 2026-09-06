package com.cts.inward.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OcrChequeData {

    private final String chequeNumber;
    private final long batchId;
    private final String accountNumber;
    private final LocalDate chequeDate;
    private final String drawerName;
    private final BigDecimal chequeAmount;
    private final String micrCode;
    private final String cityCode;
    private final String bankCode;
    private final String branchSpecificCode;

    private OcrChequeData(
            String chequeNumber,
            long batchId,
            String accountNumber,
            LocalDate chequeDate,
            String drawerName,
            BigDecimal chequeAmount,
            String micrCode,
            String cityCode,
            String bankCode,
            String branchSpecificCode) {

        this.chequeNumber = chequeNumber;
        this.batchId = batchId;
        this.accountNumber = accountNumber;
        this.chequeDate = chequeDate;
        this.drawerName = drawerName;
        this.chequeAmount = chequeAmount;
        this.micrCode = micrCode;
        this.cityCode = cityCode;
        this.bankCode = bankCode;
        this.branchSpecificCode = branchSpecificCode;
    }

    public static OcrChequeData of(
            String chequeNumber,
            long batchId,
            String accountNumber,
            LocalDate chequeDate,
            String drawerName,
            BigDecimal chequeAmount,
            String micrCode,
            String cityCode,
            String bankCode,
            String branchSpecificCode) {

        return new OcrChequeData(
                chequeNumber,
                batchId,
                accountNumber,
                chequeDate,
                drawerName,
                chequeAmount,
                micrCode,
                cityCode,
                bankCode,
                branchSpecificCode);
    }

    public String getChequeNumber() {
        return chequeNumber;
    }

    public long getBatchId() {
        return batchId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public LocalDate getChequeDate() {
        return chequeDate;
    }

    public String getDrawerName() {
        return drawerName;
    }

    public BigDecimal getChequeAmount() {
        return chequeAmount;
    }

    public String getMicrCode() {
        return micrCode;
    }

    public String getCityCode() {
        return cityCode;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getBranchSpecificCode() {
        return branchSpecificCode;
    }
}