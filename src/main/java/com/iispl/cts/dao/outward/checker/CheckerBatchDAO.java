

package com.iispl.cts.dao.outward.checker;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.iispl.cts.data.CTSStaticData;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;

public class CheckerBatchDAO {

    // ============================================================
    // GET BATCHES LOCKED BY CURRENT CHECKER
    // ============================================================

    public List<OutwardBatch> getCheckerBatches(
            String checkerUserId) {

        List<OutwardBatch> batches =
                new ArrayList<>();

        String sql =
                "SELECT "
                + "ob.batch_number, "
                + "ob.cheque_count, "
                + "cba.user_id "
                + "FROM public.outward_batch ob "
                + "INNER JOIN public.outward_batch_assignment cba "
                + "ON ob.batch_number = cba.batch_number "
                + "WHERE cba.user_id = ? "
                + "AND UPPER(cba.assignment_role) = 'CHECKER' "
                + "AND UPPER(cba.assignment_status) "
                + "IN ('ASSIGNED', 'IN_PROGRESS') "
                + "ORDER BY cba.assigned_at DESC";

        try (
                Connection con =
                        CTSStaticData.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setInt(
                    1,
                    Integer.parseInt(checkerUserId)
            );

            try (
                    ResultSet rs =
                            ps.executeQuery()
            ) {

                while (rs.next()) {

                    OutwardBatch batch =
                            new OutwardBatch();

                    // Batch Number
                    batch.setBatchNumber(
                            rs.getString("batch_number")
                    );

                    // Total Cheques
                    batch.setNumberOfCheques(
                            rs.getInt("cheque_count")
                    );

                    // Checker ID
                    int checkerId =
                            rs.getInt("user_id");

                    batch.setCheckerUserNumber(
                            String.valueOf(checkerId)
                    );

                    // Display Status
                    batch.setBatchStatus(
                            "Locked by Checker"
                    );

                    // Lock Information
                    batch.setLockedBy(
                            String.valueOf(checkerId)
                    );

                    batch.setLockStatus(
                            "LOCKED"
                    );

                    batches.add(batch);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to load batches locked by Checker.",
                    e
            );
        }

        return batches;
    }


    // ============================================================
    // GET CHEQUES BY BATCH NUMBER
    // ============================================================

    public List<OutwardCheque> getChequesByBatchNumber(
            String batchNumber) {

        List<OutwardCheque> cheques =
                new ArrayList<>();

        String sql =
                "SELECT "
                + "batch_number, "
                + "cheque_number, "
                + "drawer_account_number, "
                + "drawer_name, "
                + "payee_name, "
                + "amount, "
                + "amount_in_words, "
                + "cheque_date, "
                + "front_image_path, "
                + "back_image_path, "
                + "cheque_status, "
                + "bank_code, "
                + "branch_code, "
                + "city_code "
                + "FROM public.outward_cheque "
                + "WHERE batch_number = ? "
                + "ORDER BY cheque_number";

        try (
                Connection con =
                        CTSStaticData.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            // Set batch number
            ps.setString(
                    1,
                    batchNumber
            );

            try (
                    ResultSet rs =
                            ps.executeQuery()
            ) {

                while (rs.next()) {

                    OutwardCheque cheque =
                            new OutwardCheque();

                    // =========================================
                    // BATCH NUMBER
                    // =========================================

                    cheque.setBatchNumber(
                            rs.getString("batch_number")
                    );


                    // =========================================
                    // CHEQUE NUMBER
                    // =========================================

                    cheque.setChequeNumber(
                            rs.getString("cheque_number")
                    );


                    // =========================================
                    // DRAWER ACCOUNT NUMBER
                    // =========================================

                    cheque.setDrawerAccountNumber(
                            rs.getString(
                                    "drawer_account_number"
                            )
                    );


                    // =========================================
                    // DRAWER NAME
                    // =========================================

                    cheque.setDrawerName(
                            rs.getString("drawer_name")
                    );


                    // =========================================
                    // PAYEE NAME
                    // =========================================

                    cheque.setPayeeName(
                            rs.getString("payee_name")
                    );


                    // =========================================
                    // AMOUNT
                    // =========================================

                    BigDecimal amount =
                            rs.getBigDecimal("amount");

                    cheque.setAmount(amount);


                    // =========================================
                    // AMOUNT IN WORDS
                    // =========================================

                    cheque.setAmountInWords(
                            rs.getString(
                                    "amount_in_words"
                            )
                    );


                    // =========================================
                    // CHEQUE DATE
                    // =========================================

                    LocalDate chequeDate =
                            rs.getObject(
                                    "cheque_date",
                                    LocalDate.class
                            );

                    cheque.setChequeDate(
                            chequeDate
                    );


                    // =========================================
                    // FRONT IMAGE PATH
                    // =========================================

                    cheque.setFrontImagePath(
                            rs.getString(
                                    "front_image_path"
                            )
                    );


                    // =========================================
                    // BACK IMAGE PATH
                    // =========================================

                    cheque.setBackImagePath(
                            rs.getString(
                                    "back_image_path"
                            )
                    );


                    // =========================================
                    // CHEQUE STATUS
                    // =========================================

                    cheque.setChequeStatus(
                            rs.getString(
                                    "cheque_status"
                            )
                    );


                    // =========================================
                    // BANK CODE
                    // =========================================

                    cheque.setBankCode(
                            rs.getString(
                                    "bank_code"
                            )
                    );


                    // =========================================
                    // BRANCH CODE
                    // =========================================

                    cheque.setBranchCode(
                            rs.getString(
                                    "branch_code"
                            )
                    );


                    // =========================================
                    // CITY CODE
                    // =========================================

                    cheque.setCityCode(
                            rs.getString(
                                    "city_code"
                            )
                    );


                    // =========================================
                    // ADD CHEQUE TO LIST
                    // =========================================

                    cheques.add(cheque);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Unable to load cheques for batch: "
                    + batchNumber,
                    e
            );
        }

        return cheques;
    }


    // ============================================================
    // GET CHEQUES BY BATCH ID
    // ============================================================
    //
    // Your controller currently calls:
    //
    // batchService.getChequesByBatchId(batchId)
    //
    // But your database actually uses batch_number.
    //
    // Therefore this method simply calls the
    // getChequesByBatchNumber() method.
    // ============================================================

    public List<OutwardCheque> getChequesByBatchId(
            String batchId) {

        return getChequesByBatchNumber(
                batchId
        );
    }
 // ============================================================
 // CHECK WHETHER ACCOUNT EXISTS
 // ============================================================

 public boolean accountExists(String accountNumber) {

     String sql =
             "SELECT 1 "
             + "FROM public.account_master "
             + "WHERE account_number = ?";

     try (
             Connection con =
                     CTSStaticData.getConnection();

             PreparedStatement ps =
                     con.prepareStatement(sql)
     ) {

         ps.setString(
                 1,
                 accountNumber
         );

         try (
                 ResultSet rs =
                         ps.executeQuery()
         ) {

             // If account is found → true
             return rs.next();
         }

     } catch (Exception e) {

         e.printStackTrace();

         throw new RuntimeException(
                 "Unable to verify account: "
                 + accountNumber,
                 e
         );
     }
 }

}