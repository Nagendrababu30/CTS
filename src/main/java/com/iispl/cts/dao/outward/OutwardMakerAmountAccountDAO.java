package com.iispl.cts.dao.outward;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.iispl.cts.data.CTSStaticData;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;

public class OutwardMakerAmountAccountDAO {

    /*
     * ============================================================
     * 1. GET BATCHES FOR AMOUNT & ACCOUNT MODULE
     * ============================================================
     *
     * Batches entering this module must have passed
     * Data Entry Verification.
     *
     * ACCOUNT_ERROR is also included so that a failed batch
     * remains visible and can be corrected/reprocessed.
     *
     * Before returning the list, automatic validation is performed.
     */
	public List<OutwardBatch> getAmountAccountBatches() {

	    List<OutwardBatch> batches = new ArrayList<>();

	    String sql =
	            "SELECT " +
	            "    ob.batch_number, " +
	            "    ob.branch_code, " +
	            "    ob.cheque_count, " +
	            "    ob.batch_folder_path, " +
	            "    ob.created_by, " +
	            "    ob.created_at, " +
	            "    ob.batch_status " +
	            "FROM public.outward_batch ob " +
	            "WHERE UPPER(TRIM(ob.batch_status)) " +
	            "      IN ('ASSIGNED', 'ACCOUNT_ERROR') " +
	            "ORDER BY ob.batch_number";

	    try (
	            Connection con = CTSStaticData.getConnection();
	            PreparedStatement ps = con.prepareStatement(sql);
	            ResultSet rs = ps.executeQuery()
	    ) {

	        while (rs.next()) {

	            OutwardBatch batch = new OutwardBatch();

	            batch.setBatchNumber(
	                    rs.getString("batch_number")
	            );

	            batch.setBranchCode(
	                    rs.getString("branch_code")
	            );

	            batch.setNumberOfCheques(
	                    rs.getInt("cheque_count")
	            );

	            batch.setBatchFolderPath(
	                    rs.getString("batch_folder_path")
	            );

	            if (rs.getTimestamp("created_at") != null) {

	                batch.setCreatedAt(
	                        rs.getTimestamp("created_at")
	                                .toLocalDateTime()
	                );
	            }

	            batch.setBatchStatus(
	                    rs.getString("batch_status")
	            );

	            batches.add(batch);
	        }

	    } catch (Exception e) {

	        e.printStackTrace();

	        throw new RuntimeException(
	                "Error loading Account & Amount batches: "
	                        + e.getMessage(),
	                e
	        );
	    }

	    return batches;
	}


    /*
     * ============================================================
     * 2. AUTOMATIC BATCH VALIDATION
     * ============================================================
     *
     * Every cheque in the batch is checked.
     *
     * Account validation:
     *      outward_cheque.payee_account_number
     *              VS
     *      account_master.account_number
     *
     * Other validations:
     *      - account number must not be empty
     *      - account number must be numeric
     *      - account must exist in account_master
     *      - amount must be present
     *      - amount must be greater than zero
     *      - cheque date must be present
     */
    private void validateBatchAutomatically(
            Connection con,
            String batchNumber) throws SQLException {

        List<OutwardCheque> cheques =
                getChequesForValidation(
                        con,
                        batchNumber
                );

        if (cheques.isEmpty()) {

            /*
             * A batch with no cheques cannot be considered
             * successfully account verified.
             */
            updateBatchStatus(
                    con,
                    batchNumber,
                    "ACCOUNT_ERROR"
            );

            return;
        }

        boolean batchHasError = false;

        for (OutwardCheque cheque : cheques) {

            String errorMessage =
                    validateCheque(
                            con,
                            cheque
                    );

            if (errorMessage != null) {

                batchHasError = true;

                updateChequeStatus(
                        con,
                        cheque.getBatchNumber(),
                        cheque.getChequeNumber(),
                        "ACCOUNT_ERROR"
                );

                System.out.println(
                        "Account & Amount validation failed. "
                        + "Batch: "
                        + cheque.getBatchNumber()
                        + ", Cheque: "
                        + cheque.getChequeNumber()
                        + ", Reason: "
                        + errorMessage
                );

            } else {

                updateChequeStatus(
                        con,
                        cheque.getBatchNumber(),
                        cheque.getChequeNumber(),
                        "ACCOUNT_VERIFIED"
                );
            }
        }

        /*
         * Batch status is decided from ALL cheque results.
         */
        if (batchHasError) {

            updateBatchStatus(
                    con,
                    batchNumber,
                    "ACCOUNT_ERROR"
            );

        } else {

            updateBatchStatus(
                    con,
                    batchNumber,
                    "ACCOUNT_VERIFICATION_COMPLETED"
            );
        }
    }


    /*
     * ============================================================
     * 3. GET CHEQUES FOR VALIDATION
     * ============================================================
     */
    private List<OutwardCheque> getChequesForValidation(
            Connection con,
            String batchNumber) throws SQLException {

        List<OutwardCheque> cheques =
                new ArrayList<>();

        String sql =
                "SELECT " +
                "    oc.batch_number, " +
                "    oc.cheque_number, " +
                "    oc.city_code, " +
                "    oc.bank_code, " +
                "    oc.branch_code, " +
                "    oc.drawer_account_number, " +
                "    oc.drawer_name, " +
                "    oc.payee_account_number, " +
                "    oc.payee_name, " +
                "    oc.amount, " +
                "    oc.amount_in_words, " +
                "    oc.cheque_date, " +
                "    oc.front_image_path, " +
                "    oc.back_image_path, " +
                "    oc.cheque_status, " +
                "    oc.created_by, " +
                "    oc.created_at, " +
                "    oc.updated_by, " +
                "    oc.updated_at " +
                "FROM public.outward_cheque oc " +
                "WHERE oc.batch_number = ? " +
                "ORDER BY oc.cheque_number";

        try (PreparedStatement ps =
                     con.prepareStatement(sql)) {

            ps.setString(1, batchNumber);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    OutwardCheque cheque =
                            new OutwardCheque();

                    cheque.setBatchNumber(
                            rs.getString("batch_number")
                    );

                    cheque.setChequeNumber(
                            rs.getString("cheque_number")
                    );

                    cheque.setCityCode(
                            rs.getString("city_code")
                    );

                    cheque.setBankCode(
                            rs.getString("bank_code")
                    );

                    cheque.setBranchCode(
                            rs.getString("branch_code")
                    );

                    cheque.setDrawerAccountNumber(
                            rs.getString(
                                    "drawer_account_number"
                            )
                    );

                    cheque.setDrawerName(
                            rs.getString("drawer_name")
                    );

                    /*
                     * IMPORTANT:
                     *
                     * payee_account_number from DB is mapped
                     * to depositorAccountNumber because that is
                     * the account field available in your
                     * existing OutwardCheque model.
                     */
                    cheque.setDepositorAccountNumber(
                            rs.getString(
                                    "payee_account_number"
                            )
                    );

                    cheque.setPayeeName(
                            rs.getString("payee_name")
                    );

                    if (rs.getBigDecimal("amount") != null) {
                        cheque.setAmount(
                                rs.getBigDecimal("amount")
                        );
                    }

                    cheque.setAmountInWords(
                            rs.getString("amount_in_words")
                    );

                    if (rs.getDate("cheque_date") != null) {
                        cheque.setChequeDate(
                                rs.getDate("cheque_date")
                                        .toLocalDate()
                        );
                    }

                    cheque.setFrontImagePath(
                            rs.getString(
                                    "front_image_path"
                            )
                    );

                    cheque.setBackImagePath(
                            rs.getString(
                                    "back_image_path"
                            )
                    );

                    cheque.setChequeStatus(
                            rs.getString(
                                    "cheque_status"
                            )
                    );

                    cheque.setCreatedBy(
                            rs.getString("created_by")
                    );

                    if (rs.getTimestamp("created_at") != null) {
                        cheque.setCreatedAt(
                                rs.getTimestamp(
                                        "created_at"
                                ).toLocalDateTime()
                        );
                    }

                    cheque.setUpdatedBy(
                            rs.getString("updated_by")
                    );

                    if (rs.getTimestamp("updated_at") != null) {
                        cheque.setUpdatedAt(
                                rs.getTimestamp(
                                        "updated_at"
                                ).toLocalDateTime()
                        );
                    }

                    cheques.add(cheque);
                }
            }
        }

        return cheques;
    }


    /*
     * ============================================================
     * 4. VALIDATE INDIVIDUAL CHEQUE
     * ============================================================
     */
    private String validateCheque(
            Connection con,
            OutwardCheque cheque) throws SQLException {

        /*
         * --------------------------------------------
         * ACCOUNT VALIDATION
         * --------------------------------------------
         */

        String accountNumber =
                cheque.getDepositorAccountNumber();

        if (accountNumber == null
                || accountNumber.trim().isEmpty()) {

            return "Account number is missing.";
        }

        accountNumber = accountNumber.trim();

        /*
         * Account number should contain numeric characters.
         */
        if (!accountNumber.matches("\\d+")) {

            return "Account number must contain only digits.";
        }

        /*
         * Check account against Account Master.
         */
        if (!accountExists(
                con,
                accountNumber)) {

            return "Account number does not exist in Account Master.";
        }


        /*
         * --------------------------------------------
         * AMOUNT VALIDATION
         * --------------------------------------------
         */

        if (cheque.getAmount() == null) {

            return "Cheque amount is missing.";
        }

        if (cheque.getAmount().signum() <= 0) {

            return "Cheque amount must be greater than zero.";
        }


        /*
         * --------------------------------------------
         * CHEQUE DATE VALIDATION
         * --------------------------------------------
         */

        if (cheque.getChequeDate() == null) {

            return "Cheque date is missing.";
        }


        /*
         * All validations passed.
         */
        return null;
    }


    /*
     * ============================================================
     * 5. ACCOUNT MASTER VALIDATION
     * ============================================================
     */
    private boolean accountExists(
            Connection con,
            String accountNumber) throws SQLException {

        String sql =
                "SELECT 1 " +
                "FROM public.account_master " +
                "WHERE account_number = ? " +
                "LIMIT 1";

        try (PreparedStatement ps =
                     con.prepareStatement(sql)) {

            ps.setString(1, accountNumber);

            try (ResultSet rs =
                         ps.executeQuery()) {

                return rs.next();
            }
        }
    }


    /*
     * ============================================================
     * 6. UPDATE CHEQUE STATUS
     * ============================================================
     */
    private void updateChequeStatus(
            Connection con,
            String batchNumber,
            String chequeNumber,
            String status) throws SQLException {

        String sql =
                "UPDATE public.outward_cheque " +
                "SET cheque_status = ?, " +
                "    updated_at = CURRENT_TIMESTAMP " +
                "WHERE batch_number = ? " +
                "AND cheque_number = ?";

        try (PreparedStatement ps =
                     con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setString(2, batchNumber);
            ps.setString(3, chequeNumber);

            ps.executeUpdate();
        }
    }


    /*
     * ============================================================
     * 7. UPDATE BATCH STATUS
     * ============================================================
     */
    private void updateBatchStatus(
            Connection con,
            String batchNumber,
            String status) throws SQLException {

        String sql =
                "UPDATE public.outward_batch " +
                "SET batch_status = ? " +
                "WHERE batch_number = ?";

        try (PreparedStatement ps =
                     con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setString(2, batchNumber);

            ps.executeUpdate();
        }
    }


    /*
     * ============================================================
     * 8. GET CURRENT BATCH STATUS
     * ============================================================
     */
    private String getBatchStatus(
            Connection con,
            String batchNumber) throws SQLException {

        String sql =
                "SELECT batch_status " +
                "FROM public.outward_batch " +
                "WHERE batch_number = ?";

        try (PreparedStatement ps =
                     con.prepareStatement(sql)) {

            ps.setString(1, batchNumber);

            try (ResultSet rs =
                         ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getString(
                            "batch_status"
                    );
                }
            }
        }

        return null;
    }


    /*
     * ============================================================
     * 9. MANUAL / EXPLICIT BATCH VERIFICATION
     * ============================================================
     *
     * This can be called by the Service/Controller after
     * corrections have been made.
     *
     * It re-runs the complete automatic validation.
     */
    public boolean verifyBatch(
            String batchNumber) {

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            return false;
        }

        try (Connection con =
                     CTSStaticData.getConnection()) {

            validateBatchAutomatically(
                    con,
                    batchNumber
            );

            String status =
                    getBatchStatus(
                            con,
                            batchNumber
                    );

            return "ACCOUNT_VERIFICATION_COMPLETED"
                    .equalsIgnoreCase(
                            status == null
                                    ? ""
                                    : status.trim()
                    );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error verifying batch "
                            + batchNumber,
                    e
            );
        }
    }


    public List<OutwardCheque> getChequesForBatch(String batchNumber) {

        List<OutwardCheque> cheques = new ArrayList<>();

        if (batchNumber == null || batchNumber.trim().isEmpty()) {
            return cheques;
        }

        String sql =
                "SELECT " +
                "    oc.batch_number, " +
                "    oc.cheque_number, " +
                "    oc.city_code, " +
                "    oc.bank_code, " +
                "    oc.branch_code, " +
                "    oc.drawer_account_number, " +
                "    oc.drawer_name, " +
                "    oc.payee_account_number, " +
                "    oc.payee_name, " +
                "    oc.amount, " +
                "    oc.amount_in_words, " +
                "    oc.cheque_date, " +
                "    oc.front_image_path, " +
                "    oc.back_image_path, " +
                "    oc.cheque_status " +
                "FROM public.outward_cheque oc " +
                "WHERE oc.batch_number = ? " +
                "ORDER BY oc.cheque_number";

        try (
                Connection con = CTSStaticData.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, batchNumber.trim());

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    OutwardCheque cheque = new OutwardCheque();

                    // --------------------------------------------
                    // BASIC CHEQUE DETAILS
                    // --------------------------------------------

                    cheque.setBatchNumber(
                            rs.getString("batch_number")
                    );

                    cheque.setChequeNumber(
                            rs.getString("cheque_number")
                    );

                    // --------------------------------------------
                    // BANK / BRANCH DETAILS
                    // --------------------------------------------

                    cheque.setCityCode(
                            rs.getString("city_code")
                    );

                    cheque.setBankCode(
                            rs.getString("bank_code")
                    );

                    cheque.setBranchCode(
                            rs.getString("branch_code")
                    );

                    // --------------------------------------------
                    // DRAWER DETAILS
                    // --------------------------------------------

                    cheque.setDrawerAccountNumber(
                            rs.getString("drawer_account_number")
                    );

                    cheque.setDrawerName(
                            rs.getString("drawer_name")
                    );

                    // --------------------------------------------
                    // PAYEE / DEPOSITOR DETAILS
                    // --------------------------------------------

                    /*
                     * Your OutwardCheque model does not contain a
                     * separate payeeAccountNumber field.
                     *
                     * Therefore payee_account_number is mapped
                     * into the existing depositorAccountNumber field.
                     */

                    cheque.setDepositorAccountNumber(
                            rs.getString("payee_account_number")
                    );

                    cheque.setPayeeName(
                            rs.getString("payee_name")
                    );

                    // --------------------------------------------
                    // AMOUNT
                    // --------------------------------------------

                    cheque.setAmount(
                            rs.getBigDecimal("amount")
                    );

                    cheque.setAmountInWords(
                            rs.getString("amount_in_words")
                    );

                    // --------------------------------------------
                    // CHEQUE DATE
                    // --------------------------------------------

                    if (rs.getDate("cheque_date") != null) {

                        cheque.setChequeDate(
                                rs.getDate("cheque_date")
                                        .toLocalDate()
                        );
                    }

                    // --------------------------------------------
                    // IMAGES
                    // --------------------------------------------

                    cheque.setFrontImagePath(
                            rs.getString("front_image_path")
                    );

                    cheque.setBackImagePath(
                            rs.getString("back_image_path")
                    );

                    // --------------------------------------------
                    // STATUS
                    // --------------------------------------------

                    cheque.setChequeStatus(
                            rs.getString("cheque_status")
                    );

                    cheques.add(cheque);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to load cheques for batch "
                            + batchNumber,
                    e
            );
        }

        return cheques;
    }
}