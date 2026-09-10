package com.iispl.cts.dao.outward;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.iispl.cts.data.CTSStaticData;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;

public class CaptureOperatorBatchDAO {

    // =========================================================
    // GET ACTIVE BRANCHES
    // =========================================================

    public List<String[]> getActiveBranches() {

        List<String[]> branches = new ArrayList<>();

        String sql =
                "SELECT branch_code, branch_name " +
                "FROM branch " +
                "WHERE status = 'ACTIVE' " +
                "ORDER BY branch_code";

        try (Connection connection = CTSStaticData.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                branches.add(new String[] {
                        rs.getString("branch_code"),
                        rs.getString("branch_name")
                });
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to load branches from database.",
                    e
            );
        }

        return branches;
    }


    // =========================================================
    // GET BRANCH NAME
    // =========================================================

    public String getBranchName(String branchCode) {

        String sql =
                "SELECT branch_name " +
                "FROM branch " +
                "WHERE branch_code = ?";

        try (Connection connection = CTSStaticData.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, branchCode);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getString("branch_name");
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to get branch name.",
                    e
            );
        }

        return "";
    }


    // =========================================================
    // SAVE BATCH + CHEQUES
    // =========================================================

    public void saveBatchWithCheques(
            OutwardBatch batch,
            List<OutwardCheque> cheques,
            int createdBy) throws Exception {

        Connection connection = null;

        try {

            // =====================================================
            // GET CONNECTION
            // =====================================================

            connection = CTSStaticData.getConnection();

            // One transaction for batch + all cheques
            connection.setAutoCommit(false);


            // =====================================================
            // 1. INSERT BATCH FIRST
            // =====================================================

            String batchSql =
                    "INSERT INTO outward_batch " +
                    "(batch_number, branch_code, cheque_count, " +
                    "batch_folder_path, created_by, created_at, " +
                    "batch_status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps =
                         connection.prepareStatement(batchSql)) {

                ps.setString(
                        1,
                        batch.getBatchNumber()
                );

                ps.setString(
                        2,
                        batch.getBranchCode()
                );

                ps.setInt(
                        3,
                        batch.getNumberOfCheques()
                );

                ps.setString(
                        4,
                        batch.getBatchFolderPath()
                );

                ps.setInt(
                        5,
                        createdBy
                );

                /*
                 * Use batch created time when available.
                 * Otherwise use current database/application time.
                 */
                if (batch.getCreatedAt() != null) {

                    ps.setTimestamp(
                            6,
                            Timestamp.valueOf(
                                    batch.getCreatedAt()
                            )
                    );

                } else {

                    ps.setTimestamp(
                            6,
                            new Timestamp(
                                    System.currentTimeMillis()
                            )
                    );
                }

                ps.setString(
                        7,
                        batch.getBatchStatus()
                );

                ps.executeUpdate();
            }


            // =====================================================
            // 2. INSERT CHEQUES
            // =====================================================
            //
            // ACTUAL DATABASE COLUMNS:
            //
            // batch_number
            // cheque_number
            // drawer_account_number
            // drawer_name
            // payee_account_number
            // payee_name
            // amount
            // amount_in_words
            // cheque_date
            // front_image_path
            // back_image_path
            // cheque_status
            // bank_code
            // branch_code
            // city_code
            //
            // IMPORTANT:
            //
            // Java:
            //     depositorAccountNumber
            //
            // maps to DB:
            //     payee_account_number
            //
            // Java:
            //     depositorName
            //
            // is NOT inserted because the database has
            // NO depositor_name column.
            // =====================================================

            String chequeSql =
                    "INSERT INTO outward_cheque (" +

                    "batch_number, " +
                    "cheque_number, " +
                    "drawer_account_number, " +
                    "drawer_name, " +
                    "payee_account_number, " +
                    "payee_name, " +
                    "amount, " +
                    "amount_in_words, " +
                    "cheque_date, " +
                    "front_image_path, " +
                    "back_image_path, " +
                    "cheque_status, " +
                    "bank_code, " +
                    "branch_code, " +
                    "city_code" +

                    ") VALUES (" +
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
                    ")";


            try (PreparedStatement ps =
                         connection.prepareStatement(chequeSql)) {

                for (OutwardCheque cheque : cheques) {

                    // =================================================
                    // IMPORTANT:
                    // Every cheque gets the SAME generated batch number
                    // =================================================

                    cheque.setBatchNumber(
                            batch.getBatchNumber()
                    );


                    // =================================================
                    // 1. batch_number
                    // =================================================

                    ps.setString(
                            1,
                            cheque.getBatchNumber()
                    );


                    // =================================================
                    // 2. cheque_number
                    // =================================================

                    ps.setString(
                            2,
                            cheque.getChequeNumber()
                    );


                    // =================================================
                    // 3. drawer_account_number
                    // =================================================

                    ps.setString(
                            3,
                            cheque.getDrawerAccountNumber()
                    );


                    // =================================================
                    // 4. drawer_name
                    // =================================================

                    ps.setString(
                            4,
                            cheque.getDrawerName()
                    );


                    // =================================================
                    // 5. payee_account_number
                    //
                    // Java field:
                    // depositorAccountNumber
                    //
                    // Database field:
                    // payee_account_number
                    // =================================================

                    ps.setString(
                            5,
                            cheque.getDepositorAccountNumber()
                    );


                    // =================================================
                    // 6. payee_name
                    // =================================================

                    ps.setString(
                            6,
                            cheque.getPayeeName()
                    );


                    // =================================================
                    // 7. amount
                    // =================================================

                    ps.setBigDecimal(
                            7,
                            cheque.getAmount()
                    );


                    // =================================================
                    // 8. amount_in_words
                    // =================================================

                    ps.setString(
                            8,
                            cheque.getAmountInWords()
                    );


                    // =================================================
                    // 9. cheque_date
                    // =================================================

                    if (cheque.getChequeDate() != null) {

                        ps.setDate(
                                9,
                                java.sql.Date.valueOf(
                                        cheque.getChequeDate()
                                )
                        );

                    } else {

                        ps.setDate(
                                9,
                                null
                        );
                    }


                    // =================================================
                    // 10. front_image_path
                    // =================================================

                    ps.setString(
                            10,
                            cheque.getFrontImagePath()
                    );


                    // =================================================
                    // 11. back_image_path
                    // =================================================

                    ps.setString(
                            11,
                            cheque.getBackImagePath()
                    );


                    // =================================================
                    // 12. cheque_status
                    // =================================================

                    ps.setString(
                            12,
                            cheque.getChequeStatus()
                    );


                    // =================================================
                    // 13. bank_code
                    // =================================================

                    ps.setString(
                            13,
                            cheque.getBankCode()
                    );


                    // =================================================
                    // 14. branch_code
                    // =================================================

                    ps.setString(
                            14,
                            cheque.getBranchCode()
                    );


                    // =================================================
                    // 15. city_code
                    // =================================================

                    ps.setString(
                            15,
                            cheque.getCityCode()
                    );


                    // Add cheque to batch
                    ps.addBatch();
                }

                // Execute all cheque inserts
                ps.executeBatch();
            }


            // =====================================================
            // 3. COMMIT
            // =====================================================

            connection.commit();


        } catch (Exception e) {

            // =====================================================
            // ROLLBACK
            // =====================================================
            //
            // If batch insert succeeds but any cheque fails,
            // EVERYTHING is rolled back.
            // =====================================================

            if (connection != null) {

                try {

                    connection.rollback();

                } catch (Exception rollbackException) {

                    rollbackException.printStackTrace();
                }
            }

            throw e;


        } finally {

            // =====================================================
            // RESTORE AUTOCOMMIT + CLOSE CONNECTION
            // =====================================================

            if (connection != null) {

                try {

                    connection.setAutoCommit(true);

                } catch (Exception ignored) {
                }

                try {

                    connection.close();

                } catch (Exception ignored) {
                }
            }
        }
    }


    // =========================================================
    // GET CAPTURED BATCHES
    // =========================================================

    public List<OutwardBatch> getCapturedBatches() {

        List<OutwardBatch> batches =
                new ArrayList<>();

        String sql =
                "SELECT " +
                "batch_number, " +
                "branch_code, " +
                "cheque_count, " +
                "batch_folder_path, " +
                "created_by, " +
                "created_at, " +
                "batch_status " +
                "FROM outward_batch " +
                "ORDER BY created_at DESC";


        try (Connection connection = CTSStaticData.getConnection();
             PreparedStatement ps =
                     connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {


            while (rs.next()) {

                OutwardBatch batch =
                        new OutwardBatch();


                // =================================================
                // batch_number
                // =================================================

                batch.setBatchNumber(
                        rs.getString("batch_number")
                );


                // =================================================
                // branch_code
                // =================================================

                batch.setBranchCode(
                        rs.getString("branch_code")
                );


                // =================================================
                // cheque_count
                // =================================================

                batch.setNumberOfCheques(
                        rs.getInt("cheque_count")
                );


                // =================================================
                // batch_folder_path
                // =================================================

                batch.setBatchFolderPath(
                        rs.getString("batch_folder_path")
                );


                // =================================================
                // created_by
                // =================================================

                batch.setCreatedBy(
                        String.valueOf(
                                rs.getInt("created_by")
                        )
                );


                // =================================================
                // created_at
                // =================================================

                Timestamp timestamp =
                        rs.getTimestamp("created_at");

                if (timestamp != null) {

                    batch.setCreatedAt(
                            timestamp.toLocalDateTime()
                    );
                }


                // =================================================
                // batch_status
                // =================================================

                batch.setBatchStatus(
                        rs.getString("batch_status")
                );


                batches.add(batch);
            }


        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to load captured batches from database.",
                    e
            );
        }


        return batches;
    }
}