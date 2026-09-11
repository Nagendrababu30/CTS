package com.iispl.cts.model.outward;

import java.io.Serializable;

public class ChequeProcessing implements Serializable {

    private static final long serialVersionUID = 1L;

    private String batchNumber;
    private String chequeNumber;

    // Maker
    private Integer makerId;
    private String makerAction;
    private Integer makerReasonId;

    // Checker
    private Integer checkerId;
    private String checkerAction;
    private Integer checkerReasonId;

    public ChequeProcessing() {
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getChequeNumber() {
        return chequeNumber;
    }

    public void setChequeNumber(String chequeNumber) {
        this.chequeNumber = chequeNumber;
    }

    public Integer getMakerId() {
        return makerId;
    }

    public void setMakerId(Integer makerId) {
        this.makerId = makerId;
    }

    public String getMakerAction() {
        return makerAction;
    }

    public void setMakerAction(String makerAction) {
        this.makerAction = makerAction;
    }

    public Integer getMakerReasonId() {
        return makerReasonId;
    }

    public void setMakerReasonId(Integer makerReasonId) {
        this.makerReasonId = makerReasonId;
    }

    public Integer getCheckerId() {
        return checkerId;
    }

    public void setCheckerId(Integer checkerId) {
        this.checkerId = checkerId;
    }

    public String getCheckerAction() {
        return checkerAction;
    }

    public void setCheckerAction(String checkerAction) {
        this.checkerAction = checkerAction;
    }

    public Integer getCheckerReasonId() {
        return checkerReasonId;
    }

    public void setCheckerReasonId(Integer checkerReasonId) {
        this.checkerReasonId = checkerReasonId;
    }
}