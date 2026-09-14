package com.iispl.cts.dao.outward;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.cts.inward.config.ConnectionPool;
import com.iispl.cts.model.outward.OutwardBatch;

public class OutwardMakerDataEntryDAO {

    private final javax.sql.DataSource dataSource =
            ConnectionPool.getDataSource();


    // =========================================================
    // GET USER ID BY USERNAME
    // =========================================================

    private int getUserIdByUsername(
            Connection con,
            String username) throws SQLException {

        String sql =
                "SELECT user_id "
              + "FROM public.\"user\" "
              + "WHERE username = ?";

        try (PreparedStatement ps =
                     con.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs =
                         ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        }

        return 0;
    }


    // =========================================================
    // GET ALL BATCHES
    // =========================================================

    public List<OutwardBatch> getAllBatches() {

        List<OutwardBatch> batches =
                new ArrayList<>();

        String sql =
                "SELECT batch_number, "
              + "       cheque_count, "
              + "       batch_status "
              + "FROM public.outward_batch "
              + "ORDER BY batch_number ASC";

        try (Connection con =
                     dataSource.getConnection();

             PreparedStatement ps =
                     con.prepareStatement(sql);

             ResultSet rs =
                     ps.executeQuery()) {

            while (rs.next()) {

                OutwardBatch batch =
                        new OutwardBatch();

                batch.setBatchNumber(
                        rs.getString("batch_number")
                );

                batch.setNumberOfCheques(
                        rs.getInt("cheque_count")
                );

                batch.setBatchStatus(
                        rs.getString("batch_status")
                );

                batches.add(batch);
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return batches;
    }


    // =========================================================
    // GET BATCHES FOR MAKER
    //
    // NORMAL DATA ENTRY:
    //
    //     Existing functionality remains.
    //
    //     Batch is loaded from:
    //
    //         outward_batch
    //         outward_batch_assignment
    //
    //
    // RETURNED DATA ENTRY:
    //
    //     Checker returned cheque:
    //
    //         outward_cheque.cheque_status
    //                 = SENT_BACK_TO_MAKER
    //
    //     AND:
    //
    //         cheque_processing.checker_action
    //                 = SEND_BACK
    //
    //     AND:
    //
    //         checker reason is NOT MICR.
    //
    //
    // DISPLAY:
    //
    //     Batch ID       = batch number
    //
    //     Total Cheques  = number of returned cheques
    //
    //     Status         = SENT_TO_MAKER
    //
    //     Action         = Open
    //
    //
    // Example:
    //
    //     Original batch = 10 cheques
    //     Returned       = 2 cheques
    //
    //     UI:
    //
    //     B006 | 2 | SENT_TO_MAKER | Open
    //
    //
    // IMPORTANT:
    //
    // Actual database status remains:
    //
    //     SENT_BACK_TO_MAKER
    //
    // SENT_TO_MAKER is the UI display status only.
    // =========================================================

    public List<OutwardBatch> getBatchesForMaker(
            long userId) {

        List<OutwardBatch> batches =
                new ArrayList<>();


        String sql =
                "SELECT "
              + "    b.batch_number, "
              + "    b.branch_code, "
              + "    b.cheque_count, "
              + "    b.batch_folder_path, "
              + "    b.batch_status, "
              + "    ba.assignment_status, "

              // =====================================================
              // RETURNED DATA ENTRY CHEQUE COUNT
              // =====================================================

              + "    COALESCE(rc.returned_cheque_count, 0) "
              + "        AS returned_cheque_count "

              + "FROM public.outward_batch b "

              // =====================================================
              // MAKER ASSIGNMENT
              // =====================================================

              + "INNER JOIN public.outward_batch_assignment ba "
              + "    ON b.batch_number = ba.batch_number "

              // =====================================================
              // RETURNED DATA ENTRY CHEQUES
              //
              // SAME BASE STATUS USED BY WORKING
              // OUTWARD MAKER DASHBOARD:
              //
              //     SENT_BACK_TO_MAKER
              //
              // Additional Checker validation:
              //
              //     SEND_BACK
              //
              //     NOT MICR
              // =====================================================

              + "LEFT JOIN ( "
              + "    SELECT "
              + "        oc.batch_number, "
              + "        COUNT(*) AS returned_cheque_count "
              + "    FROM public.outward_cheque oc "
              + "    WHERE UPPER(TRIM(oc.cheque_status)) "
              + "              = 'SENT_BACK_TO_MAKER' "

              + "      AND EXISTS ( "
              + "          SELECT 1 "
              + "          FROM public.cheque_processing cp "
              + "          WHERE cp.batch_number = oc.batch_number "
              + "            AND cp.cheque_number = oc.cheque_number "
              + "            AND UPPER(TRIM(cp.checker_action)) "
              + "                    = 'SEND_BACK' "

              // -----------------------------------------------------
              // Exclude MICR returns.
              //
              // Therefore Data Entry gets the other Checker
              // return reasons.
              // -----------------------------------------------------

              + "            AND ( "
              + "                 cp.checker_reason_code IS NULL "
              + "                 OR UPPER(TRIM(cp.checker_reason_code)) "
              + "                       NOT IN ( "
              + "                           'MICR', "
              + "                           'MICR_CORRECTION', "
              + "                           'MICR_MISMATCH' "
              + "                       ) "
              + "                ) "
              + "      ) "

              + "    GROUP BY oc.batch_number "

              + ") rc "

              + "    ON rc.batch_number = b.batch_number "

              // =====================================================
              // MAKER ASSIGNMENT FILTER
              // =====================================================

              + "WHERE ba.user_id = ? "

              + "  AND UPPER(TRIM(ba.assignment_role)) "
              + "          = 'MAKER' "

              // =====================================================
              // NORMAL DATA ENTRY BATCH
              //
              // OR
              //
              // RETURNED DATA ENTRY BATCH
              // =====================================================

              + "  AND ( "
              + "        b.batch_status IN ( "
              + "            'MICR_VERIFIED', "
              + "            'MICR_REPAIR_COMPLETED' "
              + "        ) "

              + "        OR COALESCE("
              + "               rc.returned_cheque_count, 0"
              + "           ) > 0 "
              + "      ) "

              + "ORDER BY b.batch_number ASC";


        // =========================================================
        // EXECUTE QUERY
        // =========================================================

        try (Connection con =
                     dataSource.getConnection();

             PreparedStatement ps =
                     con.prepareStatement(sql)) {


            // =====================================================
            // LOGGED-IN MAKER
            // =====================================================

            ps.setLong(1, userId);


            try (ResultSet rs =
                         ps.executeQuery()) {


                while (rs.next()) {

                    OutwardBatch batch =
                            new OutwardBatch();


                    // =================================================
                    // BATCH NUMBER
                    // =================================================

                    batch.setBatchNumber(
                            rs.getString("batch_number")
                    );


                    // =================================================
                    // BRANCH CODE
                    // =================================================

                    batch.setBranchCode(
                            rs.getString("branch_code")
                    );


                    // =================================================
                    // BATCH FOLDER PATH
                    // =================================================

                    batch.setBatchFolderPath(
                            rs.getString(
                                    "batch_folder_path"
                            )
                    );


                    // =================================================
                    // ORIGINAL BATCH STATUS
                    // =================================================

                    String originalBatchStatus =
                            rs.getString("batch_status");


                    // =================================================
                    // MAKER ASSIGNMENT STATUS
                    // =================================================

                    batch.setMakerAssignmentStatus(
                            rs.getString(
                                    "assignment_status"
                            )
                    );


                    // =================================================
                    // RETURNED CHEQUE COUNT
                    // =================================================

                    int returnedCount =
                            rs.getInt(
                                    "returned_cheque_count"
                            );


                    // =================================================
                    // RETURNED DATA ENTRY BATCH
                    // =================================================

                    if (returnedCount > 0) {

                        // ---------------------------------------------
                        // IMPORTANT:
                        //
                        // Display returned cheque count.
                        //
                        // NOT original batch cheque_count.
                        // ---------------------------------------------

                        batch.setNumberOfCheques(
                                returnedCount
                        );


                        // ---------------------------------------------
                        // DISPLAY STATUS
                        //
                        // Actual DB status:
                        //
                        //     SENT_BACK_TO_MAKER
                        //
                        // UI:
                        //
                        //     SENT_TO_MAKER
                        // ---------------------------------------------

                        batch.setBatchStatus(
                                "SENT_TO_MAKER"
                        );


                        // ---------------------------------------------
                        // Debug
                        // ---------------------------------------------

                        System.out.println(
                                "======================================"
                        );

                        System.out.println(
                                "DATA ENTRY RETURNED BATCH"
                        );

                        System.out.println(
                                "Maker User ID : "
                                + userId
                        );

                        System.out.println(
                                "Batch Number  : "
                                + batch.getBatchNumber()
                        );

                        System.out.println(
                                "Returned Count: "
                                + returnedCount
                        );

                        System.out.println(
                                "DB Status     : SENT_BACK_TO_MAKER"
                        );

                        System.out.println(
                                "UI Status     : SENT_TO_MAKER"
                        );

                        System.out.println(
                                "Action        : Open"
                        );

                        System.out.println(
                                "======================================"
                        );


                    } else {

                        // =================================================
                        // NORMAL DATA ENTRY FUNCTIONALITY
                        // =================================================

                        batch.setNumberOfCheques(
                                rs.getInt("cheque_count")
                        );


                        batch.setBatchStatus(
                                originalBatchStatus
                        );
                    }


                    // =================================================
                    // ADD BATCH
                    // =================================================

                    batches.add(batch);
                }
            }


        } catch (SQLException e) {

            System.err.println(
                    "Error loading Data Entry batches "
                    + "for Maker: "
                    + userId
            );

            e.printStackTrace();
        }


        return batches;
    }
}