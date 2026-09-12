package com.iispl.cts.model.outward;

import java.io.Serializable;

public class ChequeProcessing implements Serializable {

    private static final long serialVersionUID = 1L;

    private String batchNumber;

    private String chequeNumber;

    // =========================
    // MAKER
    // =========================

    private Integer makerId;

    private String makerAction;

    private String makerReasonCode;

    // =========================
    // CHECKER
    // =========================

    private Integer checkerId;

    private String checkerAction;

    private String checkerReasonCode;

    // =========================
    // CONSTRUCTOR
    // =========================

    public ChequeProcessing() {

    }

    // =========================
    // BATCH NUMBER
    // =========================

    public String getBatchNumber() {

        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {

        this.batchNumber = batchNumber;
    }

    // =========================
    // CHEQUE NUMBER
    // =========================

    public String getChequeNumber() {

        return chequeNumber;
    }

    public void setChequeNumber(String chequeNumber) {

        this.chequeNumber = chequeNumber;
    }

    // =========================
    // MAKER ID
    // =========================

    public Integer getMakerId() {

        return makerId;
    }

    public void setMakerId(Integer makerId) {

        this.makerId = makerId;
    }

    // =========================
    // MAKER ACTION
    // =========================

    public String getMakerAction() {

        return makerAction;
    }

    public void setMakerAction(String makerAction) {

        this.makerAction = makerAction;
    }

    // =========================
    // MAKER REASON CODE
    // =========================

    public String getMakerReasonCode() {

        return makerReasonCode;
    }

    public void setMakerReasonCode(String makerReasonCode) {

        this.makerReasonCode = makerReasonCode;
    }

    // =========================
    // CHECKER ID
    // =========================

    public Integer getCheckerId() {

        return checkerId;
    }

    public void setCheckerId(Integer checkerId) {

        this.checkerId = checkerId;
    }

    // =========================
    // CHECKER ACTION
    // =========================

    public String getCheckerAction() {

        return checkerAction;
    }

    public void setCheckerAction(String checkerAction) {

        this.checkerAction = checkerAction;
    }

    // =========================
    // CHECKER REASON CODE
    // =========================

    public String getCheckerReasonCode() {

        return checkerReasonCode;
    }

    public void setCheckerReasonCode(String checkerReasonCode) {

        this.checkerReasonCode = checkerReasonCode;
    }
}