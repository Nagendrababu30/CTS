package com.iispl.cts.service.outward;

import java.util.List;

import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.model.outward.OutwardValidationResult;

public class OutwardValidationService {

    public OutwardValidationResult validate(
            List<OutwardCheque> cheques) {

        OutwardValidationResult result =
                new OutwardValidationResult();

        if (cheques == null) {
            return result;
        }

        result.setTotalCheques(
                cheques.size()
        );

        int micrErrors = 0;

        for (OutwardCheque cheque : cheques) {

            if (cheque == null) {
                continue;
            }

            String cityCode =
                    cheque.getCityCode();

            String bankCode =
                    cheque.getBankCode();

            String branchCode =
                    cheque.getBranchCode();

            boolean micrError = false;

            /*
             * CITY CODE
             * Must contain exactly 3 numeric digits.
             */
            if (isBlank(cityCode)
                    || !cityCode.trim()
                            .matches("^[0-9]{3}$")) {

                micrError = true;
            }

            /*
             * BANK CODE
             * Must contain exactly 3 numeric digits.
             */
            if (isBlank(bankCode)
                    || !bankCode.trim()
                            .matches("^[0-9]{3}$")) {

                micrError = true;
            }

            /*
             * BRANCH CODE
             * Must contain exactly 3 numeric digits.
             */
            if (isBlank(branchCode)
                    || !branchCode.trim()
                            .matches("^[0-9]{3}$")) {

                micrError = true;
            }

            if (micrError) {
                micrErrors++;
            }
        }

        result.setMicrErrors(
                micrErrors
        );

        /*
         * Currently only MICR validation
         * is implemented.
         */
        result.setDataEntryErrors(0);

        result.setAmountAccountErrors(0);

        return result;
    }

    /*
     * Returns the first MICR component
     * which contains an error.
     *
     * Used by Controller/UI for
     * highlighting the incorrect field.
     */
    public String getMicrErrorType(
            OutwardCheque cheque) {

        if (cheque == null) {
            return null;
        }

        String cityCode =
                cheque.getCityCode();

        if (isBlank(cityCode)
                || !cityCode.trim()
                        .matches("^[0-9]{3}$")) {

            return "CITY";
        }

        String bankCode =
                cheque.getBankCode();

        if (isBlank(bankCode)
                || !bankCode.trim()
                        .matches("^[0-9]{3}$")) {

            return "BANK";
        }

        String branchCode =
                cheque.getBranchCode();

        if (isBlank(branchCode)
                || !branchCode.trim()
                        .matches("^[0-9]{3}$")) {

            return "BRANCH";
        }

        return null;
    }

    private boolean isBlank(
            String value) {

        return value == null
                || value.trim().isEmpty();
    }
}
