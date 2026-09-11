package com.iispl.cts.service.outward;

import java.util.List;

import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.model.outward.OutwardValidationResult;

public class OutwardValidationService {

    // =========================================================
    // VALIDATE CHEQUES
    // =========================================================
    public OutwardValidationResult validate(
            List<OutwardCheque> cheques) {

        OutwardValidationResult result =
                new OutwardValidationResult();

        if (cheques == null) {
            return result;
        }

        result.setTotalCheques(cheques.size());

        int micrErrors = 0;

        for (OutwardCheque cheque : cheques) {

            if (cheque == null) {
                continue;
            }

            boolean micrError =
                    !isValidMicrCode(cheque.getCityCode())
                    || !isValidMicrCode(cheque.getBankCode())
                    || !isValidMicrCode(cheque.getBranchCode());

            if (micrError) {
                micrErrors++;
            }
        }

        result.setMicrErrors(micrErrors);

        result.setDataEntryErrors(0);
        result.setAmountAccountErrors(0);

        return result;
    }

    // =========================================================
    // GET MICR ERROR TYPE
    // =========================================================
    public String getMicrErrorType(
            OutwardCheque cheque) {

        if (cheque == null) {
            return null;
        }

        if (!isValidMicrCode(
                cheque.getCityCode())) {
            return "CITY";
        }

        if (!isValidMicrCode(
                cheque.getBankCode())) {
            return "BANK";
        }

        if (!isValidMicrCode(
                cheque.getBranchCode())) {
            return "BRANCH";
        }

        return null;
    }

    // =========================================================
    // MICR CODE VALIDATION
    // =========================================================
    public boolean isValidMicrCode(
            String value) {

        if (value == null
                || value.trim().isEmpty()) {
            return false;
        }

        String code = value.trim();

        if (!code.matches("^[0-9]{3}$")) {
            return false;
        }

        if ("000".equals(code)) {
            return false;
        }

        return true;
    }
}