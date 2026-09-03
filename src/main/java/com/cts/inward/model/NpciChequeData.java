package com.cts.inward.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NpciChequeData {

    private final String chequeNumber;
    private final String batchId;
    private final String accountNumber;
    private final LocalDate chequeDate;
    private final String drawerName;
    private final BigDecimal chequeAmount;
    private final String micrCode;

    private NpciChequeData(
            String chequeNumber,
            String batchId,
            String accountNumber,
            LocalDate chequeDate,
            String drawerName,
            BigDecimal chequeAmount,
            String micrCode) {

        this.chequeNumber = chequeNumber;
        this.batchId = batchId;
        this.accountNumber = accountNumber;
        this.chequeDate = chequeDate;
        this.drawerName = drawerName;
        this.chequeAmount = chequeAmount;
        this.micrCode = micrCode;
    }

    public static NpciChequeData of(
            String chequeNumber,
            String batchId,
            String accountNumber,
            LocalDate chequeDate,
            String drawerName,
            BigDecimal chequeAmount,
            String micrCode) {

        return new NpciChequeData(
                chequeNumber,
                batchId,
                accountNumber,
                chequeDate,
                drawerName,
                chequeAmount,
                micrCode);
    }

    public String getChequeNumber() {
        return chequeNumber;
    }

    public String getBatchId() {
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
}