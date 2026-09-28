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

    public List<String[]> getActiveBranches() {
        List<String[]> branches = new ArrayList<>();

        String sql =
                "SELECT branch_code, branch_name " +
                "FROM branch " +
                "WHERE status = 'ACTIVE' " +
                "ORDER BY branch_code";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                branches.add(new String[]{
                        rs.getString("branch_code"),
                        rs.getString("branch_name")
                });
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to load branches from database.", e);
        }

        return branches;
    }

    public String getBranchName(String branchCode) {
        String sql =
                "SELECT branch_name " +
                "FROM branch " +
                "WHERE branch_code = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, branchCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("branch_name");
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to get branch name.", e);
        }

        return "";
    }

    public List<String> findDuplicateChequeDetails(
            List<OutwardCheque> cheques) {

        List<String> duplicates = new ArrayList<>();

        if (cheques == null || cheques.isEmpty()) {
            return duplicates;
        }

        try (Connection connection = dataSource.getConnection()) {
            return findDuplicateChequeDetailsUsingConnection(
                    connection, cheques);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to check duplicate cheques.", e);
        }
    }

    private List<String> findDuplicateChequeDetailsUsingConnection(
            Connection connection,
            List<OutwardCheque> cheques) throws Exception {

        List<String> duplicates = new ArrayList<>();

        if (cheques == null || cheques.isEmpty()) {
            return duplicates;
        }

        Set<String> duplicateMessages = new LinkedHashSet<>();

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

        for (OutwardCheque cheque : cheques) {
            if (cheque == null) {
                continue;
            }

            try (PreparedStatement ps =
                         connection.prepareStatement(sql)) {

                setDuplicateParameters(ps, cheque);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String message =
                                buildDuplicateMessage(cheque, rs);

                        duplicateMessages.add(message);
                    }
                }
            }
        }

        duplicates.addAll(duplicateMessages);
        return duplicates;
    }

    private void setDuplicateParameters(
            PreparedStatement ps,
            OutwardCheque cheque) throws Exception {

        ps.setString(
                1,
                safeValue(cheque.getChequeNumber()));

        ps.setString(
                2,
                safeValue(cheque.getDrawerAccountNumber()));

        ps.setString(
                3,
                safeValue(cheque.getDrawerName()));

        ps.setString(
                4,
                safeValue(cheque.getPayeeAccountNumber()));

        ps.setString(
                5,
                safeValue(cheque.getPayeeName()));

        if (cheque.getAmount() != null) {
            ps.setBigDecimal(
                    6,
                    cheque.getAmount());
        } else {
            ps.setNull(
                    6,
                    java.sql.Types.NUMERIC);
        }

        ps.setString(
                7,
                safeValue(cheque.getAmountInWords()));

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

        ps.setString(
                9,
                safeValue(cheque.getBankCode()));

        ps.setString(
                10,
                safeValue(cheque.getCityCode()));
    }

    private String buildDuplicateMessage(
            OutwardCheque cheque,
            ResultSet rs) throws Exception {

        StringBuilder message = new StringBuilder();

        message.append("Duplicate Cheque Found\n\n");

        message.append("Cheque No: ")
                .append(safeValue(cheque.getChequeNumber()));

        message.append("\nDrawer Account: ")
                .append(safeValue(cheque.getDrawerAccountNumber()));

        message.append("\nDrawer Name: ")
                .append(safeValue(cheque.getDrawerName()));

        message.append("\nPayee Account: ")
                .append(safeValue(cheque.getPayeeAccountNumber()));

        message.append("\nPayee Name: ")
                .append(safeValue(cheque.getPayeeName()));

        message.append("\nAmount: ")
                .append(cheque.getAmount() == null
                        ? ""
                        : cheque.getAmount().toPlainString());

        message.append("\nAmount In Words: ")
                .append(safeValue(cheque.getAmountInWords()));

        message.append("\nCheque Date: ")
                .append(cheque.getChequeDate() == null
                        ? ""
                        : cheque.getChequeDate().toString());

        message.append("\nBank Code: ")
                .append(safeValue(cheque.getBankCode()));

        message.append("\nCity Code: ")
                .append(safeValue(cheque.getCityCode()));

        message.append("\n\nExisting Batch: ")
                .append(safeValue(
                        rs.getString("batch_number")));

        message.append("\nExisting Branch: ")
                .append(safeValue(
                        rs.getString("branch_code")));

        return message.toString();
    }

    private String safeValue(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    public void saveBatchWithCheques(
            OutwardBatch batch,
            List<OutwardCheque> cheques,
            int createdBy) throws Exception {

        Connection connection = null;

        try {
            if (batch == null) {
                throw new IllegalArgumentException(
                        "Batch cannot be null.");
            }

            if (cheques == null || cheques.isEmpty()) {
                throw new IllegalArgumentException(
                        "No cheques found in batch.");
            }

            connection = dataSource.getConnection();
            connection.setAutoCommit(false);

            List<String> duplicateCheques =
                    findDuplicateChequeDetailsUsingConnection(
                            connection,
                            cheques);

            if (duplicateCheques != null &&
                !duplicateCheques.isEmpty()) {

                StringBuilder error =
                        new StringBuilder();

                error.append("DUPLICATE CHEQUE(S) FOUND\n\n");
                error.append(
                        "The following cheque(s) already exist " +
                        "in the system:\n\n");

                int count = 1;

                for (String duplicate : duplicateCheques) {
                    error.append(
                            "----------------------------------------");

                    error.append("\nDuplicate #")
                            .append(count++)
                            .append("\n");

                    error.append(duplicate);
                    error.append("\n");
                }

                error.append(
                        "\n----------------------------------------");

                error.append("\n\nBATCH NOT CREATED.");

                error.append(
                        "\nDuplicate cheque(s) must be removed " +
                        "before creating the batch.");

                throw new IllegalArgumentException(
                        error.toString());
            }

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
                        batch.getBatchNumber());

                ps.setString(
                        2,
                        batch.getBranchCode());

                ps.setInt(
                        3,
                        batch.getNumberOfCheques());

                ps.setString(
                        4,
                        batch.getBatchFolderPath());

                ps.setInt(
                        5,
                        createdBy);

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

                ps.setString(
                        7,
                        batch.getBatchStatus());

                ps.executeUpdate();
            }

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
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps =
                         connection.prepareStatement(chequeSql)) {

                for (OutwardCheque cheque : cheques) {
                    if (cheque == null) {
                        continue;
                    }

                    cheque.setBatchNumber(
                            batch.getBatchNumber());

                    ps.setString(
                            1,
                            cheque.getBatchNumber());

                    ps.setString(
                            2,
                            cheque.getChequeNumber());

                    ps.setString(
                            3,
                            cheque.getDrawerAccountNumber());

                    ps.setString(
                            4,
                            cheque.getDrawerName());

                    ps.setString(
                            5,
                            cheque.getPayeeAccountNumber());

                    ps.setString(
                            6,
                            cheque.getPayeeName());

                    ps.setBigDecimal(
                            7,
                            cheque.getAmount());

                    ps.setString(
                            8,
                            cheque.getAmountInWords());

                    if (cheque.getChequeDate() != null) {
                        ps.setDate(
                                9,
                                java.sql.Date.valueOf(
                                        cheque.getChequeDate()));
                    } else {
                        ps.setDate(9, null);
                    }

                    ps.setString(
                            10,
                            cheque.getFrontImagePath());

                    ps.setString(
                            11,
                            cheque.getBackImagePath());

                    ps.setString(
                            12,
                            cheque.getChequeStatus());

                    ps.setString(
                            13,
                            cheque.getBankCode());

                    ps.setString(
                            14,
                            cheque.getBranchCode());

                    ps.setString(
                            15,
                            cheque.getCityCode());

                    ps.addBatch();
                }

                ps.executeBatch();
            }

            connection.commit();

        } catch (Exception e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (Exception rollbackException) {
                    rollbackException.printStackTrace();
                }
            }

            throw e;

        } finally {
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

    public List<OutwardBatch> getCapturedBatches() {
        List<OutwardBatch> batches = new ArrayList<>();

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

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                OutwardBatch batch = new OutwardBatch();

                batch.setBatchNumber(
                        rs.getString("batch_number"));

                batch.setBranchCode(
                        rs.getString("branch_code"));

                batch.setNumberOfCheques(
                        rs.getInt("cheque_count"));

                batch.setBatchFolderPath(
                        rs.getString("batch_folder_path"));

                batch.setCreatedBy(
                        String.valueOf(
                                rs.getInt("created_by")));

                Timestamp timestamp =
                        rs.getTimestamp("created_at");

                if (timestamp != null) {
                    batch.setCreatedAt(
                            timestamp.toLocalDateTime());
                }

                batch.setBatchStatus(
                        rs.getString("batch_status"));

                batches.add(batch);
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to load captured batches from database.",
                    e);
        }

        return batches;
    }
}