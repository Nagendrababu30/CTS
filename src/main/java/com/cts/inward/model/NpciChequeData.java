package com.cts.inward.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NpciChequeData {

    private final String chequeNumber;
    private final long batchId;
    private final String accountNumber;
    private final LocalDate chequeDate;
    private final String drawerName;
    private final BigDecimal chequeAmount;
    private final String micrCode;
    private String cityCode;
    private String bankCode;
    private String branchCode;

    private NpciChequeData(
            String chequeNumber,
            long batchId,
            String accountNumber,
            LocalDate chequeDate,
            String drawerName,
            BigDecimal chequeAmount,
            String micrCode,
            String cityCode,
            String bankCode,
            String branchCode) {

        this.chequeNumber = chequeNumber;
        this.batchId = batchId;
        this.accountNumber = accountNumber;
        this.chequeDate = chequeDate;
        this.drawerName = drawerName;
        this.chequeAmount = chequeAmount;
        this.micrCode = micrCode;
        this.cityCode = cityCode;
        this.bankCode = bankCode;
        this.branchCode = branchCode;
    }

    public static NpciChequeData of(
            String chequeNumber,
            long batchId,
            String accountNumber,
            LocalDate chequeDate,
            String drawerName,
            BigDecimal chequeAmount,
            String micrCode,
            String cityCode,
            String bankCode,
            String branchCode) {

        return new NpciChequeData(
                chequeNumber,
                batchId,
                accountNumber,
                chequeDate,
                drawerName,
                chequeAmount,
                micrCode,
                cityCode,
                bankCode,
                branchCode);
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

    public String getBranchCode() {
        return branchCode;
    }
}