package com.iispl.cts.dao.outward;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.cts.inward.config.ConnectionPool;
import com.iispl.cts.data.CTSStaticData;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;

public class CaptureOperatorBatchDAO {

    private final javax.sql.DataSource dataSource =
            ConnectionPool.getDataSource();

    // =========================================================
    // GET ACTIVE BRANCHES
    // =========================================================

    public List<String[]> getActiveBranches() {

        List<String[]> branches =
                new ArrayList<>();

        String sql =
                "SELECT branch_code, branch_name " +
                "FROM branch " +
                "WHERE status = 'ACTIVE' " +
                "ORDER BY branch_code";

        try (Connection connection =
                     dataSource.getConnection();
             PreparedStatement ps =
                     connection.prepareStatement(sql);
             ResultSet rs =
                     ps.executeQuery()) {

            while (rs.next()) {

                branches.add(
                        new String[] {
                                rs.getString("branch_code"),
                                rs.getString("branch_name")
                        });
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to load branches from database.",
                    e);
        }

        return branches;
    }

    // =========================================================
    // GET BRANCH NAME
    // =========================================================

    public String getBranchName(
            String branchCode) {

        String sql =
                "SELECT branch_name " +
                "FROM branch " +
                "WHERE branch_code = ?";

        try (Connection connection =
                     dataSource.getConnection();
             PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ps.setString(
                    1,
                    branchCode);

            try (ResultSet rs =
                         ps.executeQuery()) {

                if (rs.next()) {

                    return rs.getString(
                            "branch_name");
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to get branch name.",
                    e);
        }

        return "";
    }

    // =========================================================
    // FIND DUPLICATE CHEQUES
    // =========================================================
    //
    // IMPORTANT:
    //
    // BRANCH CODE IS NOT USED.
    //
    // Therefore:
    //
    // Branch A + Cheque X
    // Branch B + Same Cheque X
    //
    // = DUPLICATE
    //
    // The duplicate check is based on the cheque's actual
    // business data.
    //
    // =========================================================

    public List<String> findDuplicateChequeDetails(
            List<OutwardCheque> cheques) {

        List<String> duplicates =
                new ArrayList<>();

        if (cheques == null ||
                cheques.isEmpty()) {

            return duplicates;
        }

        try (Connection connection =
                     dataSource.getConnection()) {

            return findDuplicateChequeDetailsUsingConnection(
                    connection,
                    cheques);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to check duplicate cheques.",
                    e);
        }
    }

    // =========================================================
    // FIND DUPLICATES USING EXISTING CONNECTION
    // =========================================================
    //
    // This method is also called from saveBatchWithCheques()
    // using the SAME transaction connection.
    //
    // This protects against two capture requests reaching
    // the database at nearly the same time.
    //
    // =========================================================

    private List<String> findDuplicateChequeDetailsUsingConnection(
            Connection connection,
            List<OutwardCheque> cheques)
            throws Exception {

        List<String> duplicates =
                new ArrayList<>();

        if (cheques == null ||
                cheques.isEmpty()) {

            return duplicates;
        }

        // -----------------------------------------------------
        // Prevent duplicate messages for the same cheque
        // -----------------------------------------------------

        Set<String> duplicateMessages =
                new LinkedHashSet<>();

        // =====================================================
        // DUPLICATE SQL
        // =====================================================
        //
        // IMPORTANT:
        //
        // branch_code IS INTENTIONALLY NOT PRESENT.
        //
        // Duplicate matching:
        //
        // cheque_number
        // drawer_account_number
        // drawer_name
        // payee_account_number
        // payee_name
        // amount
        // amount_in_words
        // cheque_date
        // bank_code
        // city_code
        //
        // =====================================================

        String sql =
                "SELECT " +
                "oc.batch_number, " +
                "oc.branch_code, " +
                "oc.cheque_number, " +
                "oc.drawer_account_number, " +
                "oc.drawer_name, " +
                "oc.payee_account_number, " +
                "oc.payee_name, " +
                "oc.amount, " +
                "oc.amount_in_words, " +
                "oc.cheque_date, " +
                "oc.bank_code, " +
                "oc.city_code " +
                "FROM outward_cheque oc " +
                "WHERE " +

                "UPPER(TRIM(COALESCE(oc.cheque_number, ''))) " +
                "= UPPER(TRIM(COALESCE(?, ''))) " +

                "AND UPPER(TRIM(COALESCE(oc.drawer_account_number, ''))) " +
                "= UPPER(TRIM(COALESCE(?, ''))) " +

                "AND UPPER(TRIM(COALESCE(oc.drawer_name, ''))) " +
                "= UPPER(TRIM(COALESCE(?, ''))) " +

                "AND UPPER(TRIM(COALESCE(oc.payee_account_number, ''))) " +
                "= UPPER(TRIM(COALESCE(?, ''))) " +

                "AND UPPER(TRIM(COALESCE(oc.payee_name, ''))) " +
                "= UPPER(TRIM(COALESCE(?, ''))) " +

                "AND oc.amount IS NOT DISTINCT FROM ? " +

                "AND UPPER(TRIM(COALESCE(oc.amount_in_words, ''))) " +
                "= UPPER(TRIM(COALESCE(?, ''))) " +

                "AND oc.cheque_date IS NOT DISTINCT FROM ? " +

                "AND UPPER(TRIM(COALESCE(oc.bank_code, ''))) " +
                "= UPPER(TRIM(COALESCE(?, ''))) " +

                "AND UPPER(TRIM(COALESCE(oc.city_code, ''))) " +
                "= UPPER(TRIM(COALESCE(?, ''))) " +

                "ORDER BY oc.batch_number, oc.cheque_number";

        // =====================================================
        // CHECK EACH INCOMING CHEQUE
        // =====================================================

        for (OutwardCheque cheque :
                cheques) {

            if (cheque == null) {
                continue;
            }

            try (PreparedStatement ps =
                         connection.prepareStatement(sql)) {

                setDuplicateParameters(
                        ps,
                        cheque);

                try (ResultSet rs =
                             ps.executeQuery()) {

                    while (rs.next()) {

                        String message =
                                buildDuplicateMessage(
                                        cheque,
                                        rs);

                        duplicateMessages.add(
                                message);
                    }
                }
            }
        }

        duplicates.addAll(
                duplicateMessages);

        return duplicates;
    }

    // =========================================================
    // SET DUPLICATE QUERY PARAMETERS
    // =========================================================

    private void setDuplicateParameters(
            PreparedStatement ps,
            OutwardCheque cheque)
            throws Exception {

        // -----------------------------------------------------
        // 1. cheque_number
        // -----------------------------------------------------

        ps.setString(
                1,
                safeValue(
                        cheque.getChequeNumber()));

        // -----------------------------------------------------
        // 2. drawer_account_number
        // -----------------------------------------------------

        ps.setString(
                2,
                safeValue(
                        cheque.getDrawerAccountNumber()));

        // -----------------------------------------------------
        // 3. drawer_name
        // -----------------------------------------------------

        ps.setString(
                3,
                safeValue(
                        cheque.getDrawerName()));

        // -----------------------------------------------------
        // 4. payee_account_number
        // -----------------------------------------------------
        //
        // Java:
        // depositorAccountNumber
        //
        // Database:
        // payee_account_number
        //
        // -----------------------------------------------------

        ps.setString(
                4,
                safeValue(
                        cheque.getDepositorAccountNumber()));

        // -----------------------------------------------------
        // 5. payee_name
        // -----------------------------------------------------

        ps.setString(
                5,
                safeValue(
                        cheque.getPayeeName()));

        // -----------------------------------------------------
        // 6. amount
        // -----------------------------------------------------

        if (cheque.getAmount() != null) {

            ps.setBigDecimal(
                    6,
                    cheque.getAmount());

        } else {

            ps.setNull(
                    6,
                    java.sql.Types.NUMERIC);
        }

        // -----------------------------------------------------
        // 7. amount_in_words
        // -----------------------------------------------------

        ps.setString(
                7,
                safeValue(
                        cheque.getAmountInWords()));

        // -----------------------------------------------------
        // 8. cheque_date
        // -----------------------------------------------------

        if (cheque.getChequeDate() != null) {

            ps.setDate(
                    8,
                    java.sql.Date.valueOf(
                            cheque.getChequeDate()));

        } else {

            ps.setNull(
                    8,
                    java.sql.Types.DATE);
        }

        // -----------------------------------------------------
        // 9. bank_code
        // -----------------------------------------------------

        ps.setString(
                9,
                safeValue(
                        cheque.getBankCode()));

        // -----------------------------------------------------
        // 10. city_code
        // -----------------------------------------------------

        ps.setString(
                10,
                safeValue(
                        cheque.getCityCode()));
    }

    // =========================================================
    // BUILD DUPLICATE MESSAGE
    // =========================================================

    private String buildDuplicateMessage(
            OutwardCheque cheque,
            ResultSet rs)
            throws Exception {

        StringBuilder message =
                new StringBuilder();

        message.append(
                "Duplicate Cheque Found");

        message.append(
                "\n\n");

        // -----------------------------------------------------
        // Incoming cheque details
        // -----------------------------------------------------

        message.append(
                "Cheque No: ")
                .append(
                        safeValue(
                                cheque.getChequeNumber()));

        message.append(
                "\nDrawer Account: ")
                .append(
                        safeValue(
                                cheque.getDrawerAccountNumber()));

        message.append(
                "\nDrawer Name: ")
                .append(
                        safeValue(
                                cheque.getDrawerName()));

        message.append(
                "\nPayee Account: ")
                .append(
                        safeValue(
                                cheque.getDepositorAccountNumber()));

        message.append(
                "\nPayee Name: ")
                .append(
                        safeValue(
                                cheque.getPayeeName()));

        message.append(
                "\nAmount: ")
                .append(
                        cheque.getAmount() == null
                                ? ""
                                : cheque.getAmount()
                                        .toPlainString());

        message.append(
                "\nAmount In Words: ")
                .append(
                        safeValue(
                                cheque.getAmountInWords()));

        message.append(
                "\nCheque Date: ")
                .append(
                        cheque.getChequeDate() == null
                                ? ""
                                : cheque.getChequeDate()
                                        .toString());

        message.append(
                "\nBank Code: ")
                .append(
                        safeValue(
                                cheque.getBankCode()));

        message.append(
                "\nCity Code: ")
                .append(
                        safeValue(
                                cheque.getCityCode()));

        // -----------------------------------------------------
        // Existing database details
        // -----------------------------------------------------

        message.append(
                "\n\nExisting Batch: ")
                .append(
                        safeValue(
                                rs.getString(
                                        "batch_number")));

        message.append(
                "\nExisting Branch: ")
                .append(
                        safeValue(
                                rs.getString(
                                        "branch_code")));

        return message.toString();
    }

    // =========================================================
    // SAFE VALUE
    // =========================================================

    private String safeValue(
            String value) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }

    // =========================================================
    // SAVE BATCH + CHEQUES
    // =========================================================

    public void saveBatchWithCheques(
            OutwardBatch batch,
            List<OutwardCheque> cheques,
            int createdBy)
            throws Exception {

        Connection connection = null;

        try {

            // =================================================
            // VALIDATION
            // =================================================

            if (batch == null) {

                throw new IllegalArgumentException(
                        "Batch cannot be null.");
            }

            if (cheques == null ||
                    cheques.isEmpty()) {

                throw new IllegalArgumentException(
                        "No cheques found in batch.");
            }

            // =================================================
            // GET CONNECTION
            // =================================================

            connection =
                    dataSource.getConnection();

            // One transaction for batch + all cheques
            connection.setAutoCommit(false);

            // =================================================
            // DUPLICATE CHECK
            // =================================================
            //
            // THIS IS BEFORE outward_batch INSERT.
            //
            // Therefore a duplicate submission will NOT create
            // an outward_batch record.
            //
            // Branch code is NOT considered.
            //
            // =================================================

            List<String> duplicateCheques =
                    findDuplicateChequeDetailsUsingConnection(
                            connection,
                            cheques);

            if (duplicateCheques != null &&
                    !duplicateCheques.isEmpty()) {

                StringBuilder error =
                        new StringBuilder();

                error.append(
                        "DUPLICATE CHEQUE(S) FOUND");

                error.append(
                        "\n\n");

                error.append(
                        "The following cheque(s) already exist "
                        + "in the system:");

                error.append(
                        "\n\n");

                int count = 1;

                for (String duplicate :
                        duplicateCheques) {

                    error.append(
                            "----------------------------------------");

                    error.append(
                            "\nDuplicate #")
                            .append(count++)
                            .append("\n");

                    error.append(
                            duplicate);

                    error.append(
                            "\n");
                }

                error.append(
                        "\n----------------------------------------");

                error.append(
                        "\n\nBATCH NOT CREATED.");

                error.append(
                        "\nDuplicate cheque(s) must be removed "
                        + "before creating the batch.");

                throw new IllegalArgumentException(
                        error.toString());
            }

            // =================================================
            // 1. INSERT BATCH
            // =================================================

            String batchSql =
                    "INSERT INTO outward_batch " +
                    "(batch_number, branch_code, cheque_count, " +
                    "batch_folder_path, created_by, created_at, " +
                    "batch_status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps =
                         connection.prepareStatement(
                                 batchSql)) {

                // -------------------------------------------------
                // 1. batch_number
                // -------------------------------------------------

                ps.setString(
                        1,
                        batch.getBatchNumber());

                // -------------------------------------------------
                // 2. branch_code
                // -------------------------------------------------

                ps.setString(
                        2,
                        batch.getBranchCode());

                // -------------------------------------------------
                // 3. cheque_count
                // -------------------------------------------------

                ps.setInt(
                        3,
                        batch.getNumberOfCheques());

                // -------------------------------------------------
                // 4. batch_folder_path
                // -------------------------------------------------

                ps.setString(
                        4,
                        batch.getBatchFolderPath());

                // -------------------------------------------------
                // 5. created_by
                // -------------------------------------------------

                ps.setInt(
                        5,
                        createdBy);

                // -------------------------------------------------
                // 6. created_at
                // -------------------------------------------------

                if (batch.getCreatedAt() != null) {

                    ps.setTimestamp(
                            6,
                            Timestamp.valueOf(
                                    batch.getCreatedAt()));

                } else {

                    ps.setTimestamp(
                            6,
                            new Timestamp(
                                    System.currentTimeMillis()));
                }

                // -------------------------------------------------
                // 7. batch_status
                // -------------------------------------------------

                ps.setString(
                        7,
                        batch.getBatchStatus());

                ps.executeUpdate();
            }

            // =================================================
            // 2. INSERT CHEQUES
            // =================================================

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
                         connection.prepareStatement(
                                 chequeSql)) {

                for (OutwardCheque cheque :
                        cheques) {

                    if (cheque == null) {
                        continue;
                    }

                    // =================================================
                    // EVERY CHEQUE GETS SAME BATCH NUMBER
                    // =================================================

                    cheque.setBatchNumber(
                            batch.getBatchNumber());

                    // =================================================
                    // 1. batch_number
                    // =================================================

                    ps.setString(
                            1,
                            cheque.getBatchNumber());

                    // =================================================
                    // 2. cheque_number
                    // =================================================

                    ps.setString(
                            2,
                            cheque.getChequeNumber());

                    // =================================================
                    // 3. drawer_account_number
                    // =================================================

                    ps.setString(
                            3,
                            cheque.getDrawerAccountNumber());

                    // =================================================
                    // 4. drawer_name
                    // =================================================

                    ps.setString(
                            4,
                            cheque.getDrawerName());

                    // =================================================
                    // 5. payee_account_number
                    // =================================================
                    //
                    // Java:
                    // depositorAccountNumber
                    //
                    // Database:
                    // payee_account_number
                    //
                    // =================================================

                    ps.setString(
                            5,
                            cheque.getDepositorAccountNumber());

                    // =================================================
                    // 6. payee_name
                    // =================================================

                    ps.setString(
                            6,
                            cheque.getPayeeName());

                    // =================================================
                    // 7. amount
                    // =================================================

                    ps.setBigDecimal(
                            7,
                            cheque.getAmount());

                    // =================================================
                    // 8. amount_in_words
                    // =================================================

                    ps.setString(
                            8,
                            cheque.getAmountInWords());

                    // =================================================
                    // 9. cheque_date
                    // =================================================

                    if (cheque.getChequeDate() != null) {

                        ps.setDate(
                                9,
                                java.sql.Date.valueOf(
                                        cheque.getChequeDate()));

                    } else {

                        ps.setDate(
                                9,
                                null);
                    }

                    // =================================================
                    // 10. front_image_path
                    // =================================================

                    ps.setString(
                            10,
                            cheque.getFrontImagePath());

                    // =================================================
                    // 11. back_image_path
                    // =================================================

                    ps.setString(
                            11,
                            cheque.getBackImagePath());

                    // =================================================
                    // 12. cheque_status
                    // =================================================

                    ps.setString(
                            12,
                            cheque.getChequeStatus());

                    // =================================================
                    // 13. bank_code
                    // =================================================

                    ps.setString(
                            13,
                            cheque.getBankCode());

                    // =================================================
                    // 14. branch_code
                    // =================================================

                    ps.setString(
                            14,
                            cheque.getBranchCode());

                    // =================================================
                    // 15. city_code
                    // =================================================

                    ps.setString(
                            15,
                            cheque.getCityCode());

                    ps.addBatch();
                }

                // =================================================
                // EXECUTE CHEQUE INSERTS
                // =================================================

                ps.executeBatch();
            }

            // =================================================
            // 3. COMMIT
            // =================================================

            connection.commit();

        } catch (Exception e) {

            // =================================================
            // ROLLBACK
            // =================================================

            if (connection != null) {

                try {

                    connection.rollback();

                } catch (Exception rollbackException) {

                    rollbackException.printStackTrace();
                }
            }

            throw e;

        } finally {

            // =================================================
            // RESTORE AUTOCOMMIT + CLOSE CONNECTION
            // =================================================

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

        try (Connection connection =
                     dataSource.getConnection();
             PreparedStatement ps =
                     connection.prepareStatement(sql);
             ResultSet rs =
                     ps.executeQuery()) {

            while (rs.next()) {

                OutwardBatch batch =
                        new OutwardBatch();

                // =================================================
                // batch_number
                // =================================================

                batch.setBatchNumber(
                        rs.getString(
                                "batch_number"));

                // =================================================
                // branch_code
                // =================================================

                batch.setBranchCode(
                        rs.getString(
                                "branch_code"));

                // =================================================
                // cheque_count
                // =================================================

                batch.setNumberOfCheques(
                        rs.getInt(
                                "cheque_count"));

                // =================================================
                // batch_folder_path
                // =================================================

                batch.setBatchFolderPath(
                        rs.getString(
                                "batch_folder_path"));

                // =================================================
                // created_by
                // =================================================

                batch.setCreatedBy(
                        String.valueOf(
                                rs.getInt(
                                        "created_by")));

                // =================================================
                // created_at
                // =================================================

                Timestamp timestamp =
                        rs.getTimestamp(
                                "created_at");

                if (timestamp != null) {

                    batch.setCreatedAt(
                            timestamp.toLocalDateTime());
                }

                // =================================================
                // batch_status
                // =================================================

                batch.setBatchStatus(
                        rs.getString(
                                "batch_status"));

                batches.add(
                        batch);
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to load captured batches from database.",
                    e);
        }

        return batches;
    }
}