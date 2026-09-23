package com.iispl.cts.dao.outward.checker;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.cts.inward.config.ConnectionPool;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;

public class CheckerReportsDAO {

    private final DataSource dataSource =
            ConnectionPool.getDataSource();

    // ============================================================
    // GET BATCHES AVAILABLE FOR REPORTS
    // ============================================================

    public List<OutwardBatch> getCheckerCompletedBatches() {

        List<OutwardBatch> batches =
                new ArrayList<OutwardBatch>();

        String sql =
                "SELECT batch_number, "
                        + "branch_code, "
                        + "cheque_count, "
                        + "batch_folder_path, "
                        + "created_by, "
                        + "created_at, "
                        + "batch_status "
                        + "FROM public.outward_batch "
                        + "WHERE UPPER(batch_status) = 'CHECKER_VERIFIED' "
                        + "ORDER BY batch_number DESC";

        try (Connection connection =
                     dataSource.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql);

             ResultSet rs =
                     statement.executeQuery()) {

            while (rs.next()) {

                OutwardBatch batch =
                        new OutwardBatch();

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

                batch.setCreatedBy(
                        String.valueOf(
                                rs.getInt("created_by"))
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
                    "Error while fetching Checker Reports batches",
                    e
            );
        }

        return batches;
    }

    // ============================================================
    // GET TOTAL CHEQUE COUNT
    // ============================================================

    public int getTotalChequeCount(
            String batchNumber) {

        String sql =
                "SELECT COUNT(*) "
                        + "FROM public.outward_cheque "
                        + "WHERE batch_number = ?";

        try (Connection connection =
                     dataSource.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber
            );

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {

                    return rs.getInt(1);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while getting total cheque count for batch: "
                            + batchNumber,
                    e
            );
        }

        return 0;
    }

    // ============================================================
    // GET VALID CHEQUE COUNT
    // ============================================================

    public int getValidChequeCount(
            String batchNumber) {

        String sql =
                "SELECT COUNT(*) "
                        + "FROM public.outward_cheque "
                        + "WHERE batch_number = ? "
                        + "AND UPPER(cheque_status) = "
                        + "'CHECKER_ACCEPTED'";

        try (Connection connection =
                     dataSource.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber
            );

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {

                    return rs.getInt(1);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while getting valid cheque count for batch: "
                            + batchNumber,
                    e
            );
        }

        return 0;
    }

    // ============================================================
    // GET SINGLE BATCH
    // ============================================================

    public OutwardBatch getBatchByNumber(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return null;
        }

        String sql =
                "SELECT batch_number, "
                        + "branch_code, "
                        + "cheque_count, "
                        + "batch_folder_path, "
                        + "created_by, "
                        + "created_at, "
                        + "batch_status "
                        + "FROM public.outward_batch "
                        + "WHERE batch_number = ?";

        try (Connection connection =
                     dataSource.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber.trim()
            );

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {

                    OutwardBatch batch =
                            new OutwardBatch();

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

                    batch.setCreatedBy(
                            String.valueOf(
                                    rs.getInt("created_by"))
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

                    return batch;
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching batch: "
                            + batchNumber,
                    e
            );
        }

        return null;
    }

    // ============================================================
    // GET ALL CHEQUES
    // Used by CFX / CIBF
    // ============================================================

    public List<OutwardCheque> getBatchCheques(
            String batchNumber) {

        List<OutwardCheque> cheques =
                new ArrayList<OutwardCheque>();

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return cheques;
        }

        String sql =
                "SELECT batch_number, "
                        + "cheque_number, "
                        + "city_code, "
                        + "bank_code, "
                        + "branch_code, "
                        + "drawer_account_number, "
                        + "drawer_name, "
                        + "payee_account_number, "
                        + "payee_name, "
                        + "amount, "
                        + "amount_in_words, "
                        + "cheque_date, "
                        + "front_image_path, "
                        + "back_image_path, "
                        + "cheque_status, "
                        + "return_reason_id, "
                        + "checker_remarks "
                        + "FROM public.outward_cheque "
                        + "WHERE batch_number = ? "
                        + "ORDER BY cheque_number";

        try (Connection connection =
                     dataSource.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber.trim()
            );

            try (ResultSet rs =
                         statement.executeQuery()) {

                while (rs.next()) {

                    cheques.add(
                            mapCheque(rs)
                    );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching cheques for batch: "
                            + batchNumber,
                    e
            );
        }

        return cheques;
    }

    // ============================================================
    // GET REJECTED CHEQUES
    // RRF
    //
    // Rejection is taken from cheque_processing.
    // ============================================================

    public List<OutwardCheque> getRejectedCheques(
            String batchNumber) {

        List<OutwardCheque> rejectedCheques =
                new ArrayList<OutwardCheque>();

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return rejectedCheques;
        }

        String sql =
                "SELECT oc.batch_number, "
                        + "oc.cheque_number, "
                        + "oc.city_code, "
                        + "oc.bank_code, "
                        + "oc.branch_code, "
                        + "oc.drawer_account_number, "
                        + "oc.drawer_name, "
                        + "oc.payee_account_number, "
                        + "oc.payee_name, "
                        + "oc.amount, "
                        + "oc.amount_in_words, "
                        + "oc.cheque_date, "
                        + "oc.front_image_path, "
                        + "oc.back_image_path, "
                        + "oc.cheque_status, "
                        + "oc.return_reason_id, "
                        + "oc.checker_remarks "
                        + "FROM public.outward_cheque oc "
                        + "INNER JOIN public.cheque_processing cp "
                        + "ON cp.batch_number = oc.batch_number "
                        + "AND cp.cheque_number = oc.cheque_number "
                        + "WHERE oc.batch_number = ? "
                        + "AND UPPER(cp.checker_action) = 'REJECT' "
                        + "ORDER BY oc.cheque_number";

        try (Connection connection =
                     dataSource.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber.trim()
            );

            try (ResultSet rs =
                         statement.executeQuery()) {

                while (rs.next()) {

                    rejectedCheques.add(
                            mapCheque(rs)
                    );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while fetching rejected cheques for batch: "
                            + batchNumber,
                    e
            );
        }

        return rejectedCheques;
    }

    // ============================================================
    // CHECK RRF AVAILABILITY
    // ============================================================

    public boolean hasRejectedCheques(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return false;
        }

        String sql =
                "SELECT EXISTS ("
                        + "SELECT 1 "
                        + "FROM public.cheque_processing "
                        + "WHERE batch_number = ? "
                        + "AND UPPER(checker_action) = 'REJECT'"
                        + ")";

        try (Connection connection =
                     dataSource.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber.trim()
            );

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {

                    return rs.getBoolean(1);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while checking RRF availability for batch: "
                            + batchNumber,
                    e
            );
        }

        return false;
    }

    // ============================================================
    // GET REJECTED CHEQUE COUNT
    // ============================================================

    public int getRejectedChequeCount(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return 0;
        }

        String sql =
                "SELECT COUNT(*) "
                        + "FROM public.cheque_processing "
                        + "WHERE batch_number = ? "
                        + "AND UPPER(checker_action) = 'REJECT'";

        try (Connection connection =
                     dataSource.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber.trim()
            );

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {

                    return rs.getInt(1);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while counting rejected cheques for batch: "
                            + batchNumber,
                    e
            );
        }

        return 0;
    }

    // ============================================================
    // CHECK WHETHER BATCH IS READY FOR NPCI
    // ============================================================

    public boolean isBatchReadyForNPCI(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return false;
        }

        String sql =
                "SELECT EXISTS ("
                        + "SELECT 1 "
                        + "FROM public.outward_batch "
                        + "WHERE batch_number = ? "
                        + "AND UPPER(batch_status) = "
                        + "'CHECKER_VERIFIED'"
                        + ")";

        try (Connection connection =
                     dataSource.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber.trim()
            );

            try (ResultSet rs =
                         statement.executeQuery()) {

                if (rs.next()) {

                    return rs.getBoolean(1);
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while checking NPCI readiness for batch: "
                            + batchNumber,
                    e
            );
        }

        return false;
    }

    // ============================================================
    // SAVE NPCI SUBMISSION
    // outward_npci_submission
    // ============================================================

    public boolean saveNPCISubmission(
            String batchNumber,
            int validChequeCount,
            int invalidChequeCount,
            String validXmlPath) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return false;
        }

        if (validChequeCount <= 0) {

            return false;
        }

        if (validXmlPath == null ||
                validXmlPath.trim().isEmpty()) {

            return false;
        }

        String sql =
                "INSERT INTO public.outward_npci_submission "
                        + "(batch_number, "
                        + "valid_cheque_count, "
                        + "invalid_cheque_count, "
                        + "valid_xml_path) "
                        + "VALUES (?, ?, ?, ?)";

        try (Connection connection =
                     dataSource.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber.trim()
            );

            statement.setInt(
                    2,
                    validChequeCount
            );

            statement.setInt(
                    3,
                    invalidChequeCount
            );

            statement.setString(
                    4,
                    validXmlPath.trim()
            );

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while saving NPCI submission for batch: "
                            + batchNumber,
                    e
            );
        }
    }

    // ============================================================
    // MARK BATCH AS NPCI SENT
    // ============================================================

    public boolean markBatchAsNPCISent(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return false;
        }

        String sql =
                "UPDATE public.outward_batch "
                        + "SET batch_status = 'NPCI_SENT' "
                        + "WHERE batch_number = ? "
                        + "AND UPPER(batch_status) = "
                        + "'CHECKER_VERIFIED'";

        try (Connection connection =
                     dataSource.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    batchNumber.trim()
            );

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Error while marking batch as NPCI_SENT: "
                            + batchNumber,
                    e
            );
        }
    }

    // ============================================================
    // MAP CHEQUE
    // ============================================================

    private OutwardCheque mapCheque(
            ResultSet rs)
            throws Exception {

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
                rs.getString("drawer_account_number")
        );

        cheque.setDrawerName(
                rs.getString("drawer_name")
        );

        cheque.setPayeeAccountNumber(
                rs.getString("payee_account_number")
        );

        cheque.setPayeeName(
                rs.getString("payee_name")
        );

        BigDecimal amount =
                rs.getBigDecimal("amount");

        cheque.setAmount(amount);

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
                rs.getString("front_image_path")
        );

        cheque.setBackImagePath(
                rs.getString("back_image_path")
        );

        cheque.setChequeStatus(
                rs.getString("cheque_status")
        );

        Object reasonObject =
                rs.getObject("return_reason_id");

        if (reasonObject != null) {

            cheque.setReturnReasonId(
                    ((Number) reasonObject).intValue()
            );
        }

        cheque.setCheckerRemarks(
                rs.getString("checker_remarks")
        );

        return cheque;
    }
}