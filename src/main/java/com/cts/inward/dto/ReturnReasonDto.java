package com.cts.inward.dto;

public class ReturnReasonDto {

    private String returnReasonCode;
    private String description;

    public ReturnReasonDto() {
    }

    public ReturnReasonDto(
            String returnReasonCode,
            String description) {

        this.returnReasonCode = returnReasonCode;
        this.description = description;
    }

    public String getReturnReasonCode() {
        return returnReasonCode;
    }

    public void setReturnReasonCode(
            String returnReasonCode) {

        this.returnReasonCode = returnReasonCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(
            String description) {

        this.description = description;
    }

    @Override
    public String toString() {
        return "ReturnReasonDto ["
                + "returnReasonCode="
                + returnReasonCode
                + ", description="
                + description
                + "]";
    }
}