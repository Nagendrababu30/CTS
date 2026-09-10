package com.iispl.cts.model.outward;

import java.util.ArrayList;
import java.util.List;

public class OutwardValidationResult {

    private int totalCheques;

    private int dataEntryErrors;

    private int micrErrors;

    private int amountAccountErrors;

    /*
     * Cheque numbers having MICR errors.
     *
     * This is used by the MICR module to know
     * exactly which cheques need MICR repair.
     */
    private List<String> micrErrorChequeNumbers;


    public OutwardValidationResult() {

        micrErrorChequeNumbers =
                new ArrayList<>();
    }


    public int getTotalCheques() {

        return totalCheques;
    }


    public void setTotalCheques(
            int totalCheques) {

        this.totalCheques =
                totalCheques;
    }


    public int getDataEntryErrors() {

        return dataEntryErrors;
    }


    public void setDataEntryErrors(
            int dataEntryErrors) {

        this.dataEntryErrors =
                dataEntryErrors;
    }


    public int getMicrErrors() {

        return micrErrors;
    }


    public void setMicrErrors(
            int micrErrors) {

        this.micrErrors =
                micrErrors;
    }


    public int getAmountAccountErrors() {

        return amountAccountErrors;
    }


    public void setAmountAccountErrors(
            int amountAccountErrors) {

        this.amountAccountErrors =
                amountAccountErrors;
    }


    public List<String> getMicrErrorChequeNumbers() {

        return micrErrorChequeNumbers;
    }


    public void setMicrErrorChequeNumbers(
            List<String> micrErrorChequeNumbers) {

        this.micrErrorChequeNumbers =
                micrErrorChequeNumbers;
    }


    public int getTotalErrors() {

        return dataEntryErrors
                + micrErrors
                + amountAccountErrors;
    }
}