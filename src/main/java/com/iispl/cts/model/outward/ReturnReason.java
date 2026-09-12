package com.iispl.cts.model.outward;

import java.io.Serializable;

public class ReturnReason implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reasonCode;
    private String reasonName;
    private boolean active;

    public ReturnReason() {

    }

    public String getReasonCode() {

        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {

        this.reasonCode = reasonCode;
    }

    public String getReasonName() {

        return reasonName;
    }

    public void setReasonName(String reasonName) {

        this.reasonName = reasonName;
    }

    public boolean isActive() {

        return active;
    }

    public void setActive(boolean active) {

        this.active = active;
    }
}