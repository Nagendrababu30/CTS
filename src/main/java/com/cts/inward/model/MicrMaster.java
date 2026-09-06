package com.cts.inward.model;

public class MicrMaster {

    private final String micrCode;
    private final String bankName;
    private final String branchName;
    private final String cityCode;
    private final String bankCode;
    private final String branchCode;
    private final String cityName;


    private MicrMaster(
            String micrCode,
            String bankName,
            String branchName,
            String cityCode,
            String bankCode,
            String branchCode,
            String cityName) {

        this.micrCode = micrCode;
        this.bankName = bankName;
        this.branchName = branchName;
        this.cityCode = cityCode;
        this.bankCode = bankCode;
        this.branchCode = branchCode;
        this.cityName = cityName;
    }


    public static MicrMaster of(
            String micrCode,
            String bankName,
            String branchName,
            String cityCode,
            String bankCode,
            String branchCode,
            String cityName) {

        return new MicrMaster(
                micrCode,
                bankName,
                branchName,
                cityCode,
                bankCode,
                branchCode,
                cityName);
    }


    public String getMicrCode() {
        return micrCode;
    }


    public String getBankName() {
        return bankName;
    }


    public String getBranchName() {
        return branchName;
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


    public String getCityName() {
        return cityName;
    }
}