package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.dto.ReturnReasonDto;
import com.cts.inward.model.NpciChequeData;
import com.cts.inward.model.OcrChequeData;

public class MicrRepairDaoImpl implements MicrRepairDao {

    private static final String STATUS_RETURN_BY_MAKER =
            "RETURN_BY_MAKER";

    private static final String STATUS_DATA_ENTRY =
            "DATA_ENTRY";

    @Override
    public List<NpciChequeData> getNpciCheques(
            long batchId) {

        String sql = """
            SELECT
                inward_cheque_id,
                cheque_number,
                batch_id,
                account_number,
                cheque_date,
                drawer_name,
                amount,
                micr_code,
                city_code,
                bank_code,
                branch_code
            FROM public.inward_cheque
            WHERE batch_id = ?
            ORDER BY inward_cheque_id
            """;

        List<NpciChequeData> cheques =
                new ArrayList<>();

        try (
                Connection connection =
                        ConnectionPool
                                .getDataSource()
                                .getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setLong(
                    1,
                    batchId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {

                    NpciChequeData cheque =
                            NpciChequeData.of(
                                    resultSet.getString(
                                            "cheque_number"),

                                    resultSet.getLong(
                                            "batch_id"),

                                    resultSet.getString(
                                            "account_number"),

                                    resultSet.getDate(
                                            "cheque_date") != null
                                            ? resultSet.getDate(
                                                    "cheque_date")
                                                    .toLocalDate()
                                            : null,

                                    resultSet.getString(
                                            "drawer_name"),

                                    resultSet.getBigDecimal(
                                            "amount"),

                                    resultSet.getString(
                                            "micr_code"),

                                    resultSet.getString(
                                            "city_code"),

                                    resultSet.getString(
                                            "bank_code"),

                                    resultSet.getString(
                                            "branch_code")
                            );

                    cheque.setInwardChequeId(
                            resultSet.getLong(
                                    "inward_cheque_id"));

                    cheques.add(
                            cheque);
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error retrieving NPCI cheques for batch "
                            + batchId,
                    e);
        }

        return cheques;
    }

    @Override
    public List<OcrChequeData> getOcrCheques(
            long batchId) {

        String sql = """
            SELECT
                o.inward_cheque_id,
                o.cheque_number,
                o.ocr_batch_id,
                o.account_number,
                o.cheque_date,
                o.drawer_name,
                o.amount,
                o.micr_code,
                o.branch_code,
                o.city_code,
                o.bank_code
            FROM public.ocr_cheque_data o
            INNER JOIN public.ocr_batch ob
                ON o.ocr_batch_id = ob.ocr_batch_id
            WHERE ob.batch_id = ?
            ORDER BY o.inward_cheque_id
            """;

        List<OcrChequeData> cheques =
                new ArrayList<>();

        try (
                Connection connection =
                        ConnectionPool
                                .getDataSource()
                                .getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setLong(
                    1,
                    batchId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {

                    OcrChequeData cheque =
                            OcrChequeData.of();

                    cheque.setInwardChequeId(
                            resultSet.getLong(
                                    "inward_cheque_id"));

                    cheque.setChequeNumber(
                            resultSet.getString(
                                    "cheque_number"));

                    cheque.setBatchId(
                            batchId);

                    cheque.setAccountNumber(
                            resultSet.getString(
                                    "account_number"));

                    cheque.setChequeDate(
                            resultSet.getDate(
                                    "cheque_date") != null
                                    ? resultSet.getDate(
                                            "cheque_date")
                                            .toLocalDate()
                                    : null);

                    cheque.setDrawerName(
                            resultSet.getString(
                                    "drawer_name"));

                    cheque.setChequeAmount(
                            resultSet.getBigDecimal(
                                    "amount"));

                    cheque.setMicrCode(
                            resultSet.getString(
                                    "micr_code"));

                    cheque.setBranchSpecificCode(
                            resultSet.getString(
                                    "branch_code"));

                    cheque.setCityCode(
                            resultSet.getString(
                                    "city_code"));

                    cheque.setBankCode(
                            resultSet.getString(
                                    "bank_code"));

                    cheques.add(
                            cheque);
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error retrieving OCR cheques for batch "
                            + batchId,
                    e);
        }

        return cheques;
    }

    @Override
    public String getCompletedRepairedMicr(
            String chequeNumber) {

        String sql =
                "SELECT new_value "
              + "FROM public.inward_micr_repair_history "
              + "WHERE cheque_no = ? "
              + "ORDER BY micr_repair_id DESC "
              + "LIMIT 1";

        try (
                Connection connection =
                        ConnectionPool
                                .getDataSource()
                                .getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    chequeNumber);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {

                    return resultSet.getString(
                            "new_value");
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Failed to fetch completed MICR repair for cheque "
                            + chequeNumber,
                    e);
        }

        return null;
    }

    @Override
    public String getLatestChequeStatus(
            String chequeNumber) {

        String sql =
                "SELECT status "
              + "FROM public.inward_cheque_status_history "
              + "WHERE cheque_number = ? "
              + "ORDER BY status_history_id DESC "
              + "LIMIT 1";

        try (
                Connection connection =
                        ConnectionPool
                                .getDataSource()
                                .getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    chequeNumber);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {

                    return resultSet.getString(
                            "status");
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Failed to fetch latest cheque status for "
                            + chequeNumber,
                    e);
        }

        return null;
    }

    @Override
    public long getBatchIdByChequeNumber(
            String chequeNumber) {

        String sql =
                "SELECT batch_id "
              + "FROM public.inward_cheque "
              + "WHERE cheque_number = ? "
              + "LIMIT 1";

        try (
                Connection connection =
                        ConnectionPool
                                .getDataSource()
                                .getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    chequeNumber);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {

                    return resultSet.getLong(
                            "batch_id");
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Failed to find batch for cheque "
                            + chequeNumber,
                    e);
        }

        return 0L;
    }

    @Override
    public String getFrontImagePath(
            String chequeNumber) {

        String sql =
                "SELECT front_image "
              + "FROM public.inward_cheque_image "
              + "WHERE cheque_number = ? "
              + "LIMIT 1";

        return getImagePath(
                sql,
                chequeNumber,
                "front_image");
    }

    @Override
    public String getBackImagePath(
            String chequeNumber) {

        String sql =
                "SELECT back_image "
              + "FROM public.inward_cheque_image "
              + "WHERE cheque_number = ? "
              + "LIMIT 1";

        return getImagePath(
                sql,
                chequeNumber,
                "back_image");
    }

    private String getImagePath(
            String sql,
            String chequeNumber,
            String columnName) {

        try (
                Connection connection =
                        ConnectionPool
                                .getDataSource()
                                .getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    chequeNumber);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {

                    return resultSet.getString(
                            columnName);
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to load cheque image for "
                            + chequeNumber,
                    e);
        }

        return null;
    }

    @Override
    public List<ReturnReasonDto> getMakerReturnReasons() {

        List<ReturnReasonDto> reasons =
                new ArrayList<>();

        String sql =
                "SELECT return_reason_code, description "
              + "FROM public.inward_cheque_return_reason "
              + "WHERE applicable_role IN ('MAKER', 'BOTH') "
              + "ORDER BY description";

        try (
                Connection connection =
                        ConnectionPool
                                .getDataSource()
                                .getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()) {

            while (resultSet.next()) {

                reasons.add(
                        new ReturnReasonDto(
                                resultSet.getString(
                                        "return_reason_code"),

                                resultSet.getString(
                                        "description")));
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to load Maker return reasons",
                    e);
        }

        return reasons;
    }

    @Override
    public boolean saveMakerReturn(
            String chequeNumber,
            String returnReasonCode,
            String makerRemarks,
            long userId) {

        Connection connection = null;

        try {

            connection =
                    ConnectionPool
                            .getDataSource()
                            .getConnection();

            connection.setAutoCommit(false);

            /*
             * ---------------------------------------------------------
             * 1. Save CHEQUE-level return record.
             *
             * RETURN_BY_MAKER belongs to the cheque only.
             * ---------------------------------------------------------
             */
            String returnSql =
                    "INSERT INTO public.inward_cheque_return "
                  + "(cheque_number, return_reason_code, "
                  + "maker_remarks, requested_by, return_status) "
                  + "VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 returnSql)) {

                statement.setString(
                        1,
                        chequeNumber);

                statement.setString(
                        2,
                        returnReasonCode);

                if (makerRemarks == null
                        || makerRemarks.trim().isEmpty()) {

                    statement.setNull(
                            3,
                            Types.VARCHAR);

                } else {

                    statement.setString(
                            3,
                            makerRemarks.trim());
                }

                statement.setLong(
                        4,
                        userId);

                statement.setString(
                        5,
                        STATUS_RETURN_BY_MAKER);

                int inserted =
                        statement.executeUpdate();

                if (inserted != 1) {

                    connection.rollback();

                    return false;
                }
            }

            /*
             * ---------------------------------------------------------
             * 2. Save CHEQUE-level status history.
             *
             * This cheque is now RETURN_BY_MAKER.
             * ---------------------------------------------------------
             */
            String historySql =
                    "INSERT INTO public.inward_cheque_status_history "
                  + "(cheque_number, status, rejection_reason_code, "
                  + "return_reason_code, maker_id, maker_action, "
                  + "maker_action_on, checker_id, checker_action, "
                  + "checker_action_on, remarks) "
                  + "VALUES (?, ?, NULL, ?, ?, ?, CURRENT_TIMESTAMP, "
                  + "NULL, NULL, NULL, ?)";

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 historySql)) {

                statement.setString(
                        1,
                        chequeNumber);

                statement.setString(
                        2,
                        STATUS_RETURN_BY_MAKER);

                statement.setString(
                        3,
                        returnReasonCode);

                statement.setLong(
                        4,
                        userId);

                statement.setString(
                        5,
                        STATUS_RETURN_BY_MAKER);

                if (makerRemarks == null
                        || makerRemarks.trim().isEmpty()) {

                    statement.setNull(
                            6,
                            Types.VARCHAR);

                } else {

                    statement.setString(
                            6,
                            makerRemarks.trim());
                }

                int inserted =
                        statement.executeUpdate();

                if (inserted != 1) {

                    connection.rollback();

                    return false;
                }
            }

            /*
             * ---------------------------------------------------------
             * 3. Find the batch containing this cheque.
             * ---------------------------------------------------------
             */
            long batchId = 0L;

            String batchIdSql =
                    "SELECT batch_id "
                  + "FROM public.inward_cheque "
                  + "WHERE cheque_number = ? "
                  + "LIMIT 1";

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 batchIdSql)) {

                statement.setString(
                        1,
                        chequeNumber);

                try (ResultSet resultSet =
                             statement.executeQuery()) {

                    if (resultSet.next()) {

                        batchId =
                                resultSet.getLong(
                                        "batch_id");
                    }
                }
            }

            if (batchId <= 0L) {

                connection.rollback();

                return false;
            }

            /*
             * ---------------------------------------------------------
             * 4. Count remaining CHEQUES that are still waiting for
             *    MICR repair.
             *
             * Only the LATEST cheque status is considered.
             *
             * MICR_MISMATCH = still pending
             *
             * DATA_ENTRY = MICR stage completed
             *
             * RETURN_BY_MAKER = MICR stage completed
             *
             * ---------------------------------------------------------
             */
            int pendingMicrCount = 0;

            String pendingMicrSql =
                    "SELECT COUNT(*) "
                  + "FROM public.inward_cheque c "
                  + "INNER JOIN LATERAL ( "
                  + "    SELECT h.status "
                  + "    FROM public.inward_cheque_status_history h "
                  + "    WHERE h.cheque_number = c.cheque_number "
                  + "    ORDER BY h.status_history_id DESC "
                  + "    LIMIT 1 "
                  + ") latest ON TRUE "
                  + "WHERE c.batch_id = ? "
                  + "AND latest.status = 'MICR_MISMATCH'";

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 pendingMicrSql)) {

                statement.setLong(
                        1,
                        batchId);

                try (ResultSet resultSet =
                             statement.executeQuery()) {

                    if (resultSet.next()) {

                        pendingMicrCount =
                                resultSet.getInt(1);
                    }
                }
            }

            /*
             * ---------------------------------------------------------
             * 5. DO NOT change the batch to RETURN_BY_MAKER.
             *
             * Only when ZERO MICR_MISMATCH cheques remain do we move
             * the BATCH to DATA_ENTRY.
             * ---------------------------------------------------------
             */
            if (pendingMicrCount == 0) {

                String latestBatchStatusSql =
                        "SELECT batch_status "
                      + "FROM public.inward_batch_history "
                      + "WHERE batch_id = ? "
                      + "ORDER BY changed_on DESC "
                      + "LIMIT 1";

                String latestBatchStatus = null;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     latestBatchStatusSql)) {

                    statement.setLong(
                            1,
                            batchId);

                    try (ResultSet resultSet =
                                 statement.executeQuery()) {

                        if (resultSet.next()) {

                            latestBatchStatus =
                                    resultSet.getString(
                                            "batch_status");
                        }
                    }
                }

                /*
                 * Do not insert another DATA_ENTRY history row if the
                 * batch is already there.
                 */
                if (!STATUS_DATA_ENTRY.equalsIgnoreCase(
                        latestBatchStatus)) {

                    String batchHistorySql =
                            "INSERT INTO public.inward_batch_history "
                          + "(batch_id, batch_status, changed_on, "
                          + "changed_by, reason, remarks) "
                          + "VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?, ?)";

                    try (PreparedStatement statement =
                                 connection.prepareStatement(
                                         batchHistorySql)) {

                        statement.setLong(
                                1,
                                batchId);

                        statement.setString(
                                2,
                                STATUS_DATA_ENTRY);

                        statement.setLong(
                                3,
                                userId);

                        statement.setString(
                                4,
                                "MICR_REPAIR_COMPLETED");

                        statement.setString(
                                5,
                                "All cheques completed MICR stage; batch ready for Data Entry");

                        int inserted =
                                statement.executeUpdate();

                        if (inserted != 1) {

                            connection.rollback();

                            return false;
                        }
                    }
                }
            }

            /*
             * Everything completed successfully.
             */
            connection.commit();

            return true;

        } catch (SQLException e) {

            if (connection != null) {

                try {
                    connection.rollback();

                } catch (SQLException rollbackException) {

                    rollbackException.printStackTrace();
                }
            }

            throw new RuntimeException(
                    "Failed to save maker return for cheque "
                            + chequeNumber,
                    e);

        } finally {

            if (connection != null) {

                try {

                    connection.setAutoCommit(true);
                    connection.close();

                } catch (SQLException closeException) {

                    closeException.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean saveMicrRepair(
            String chequeNumber,
            String originalMicr,
            String repairedMicr,
            String remarks,
            long userId) {

        Connection connection = null;

        try {

            connection =
                    ConnectionPool
                            .getDataSource()
                            .getConnection();

            connection.setAutoCommit(false);

            /*
             * ---------------------------------------------------------
             * 1. Save MICR repair history.
             *
             * Original OCR/NPCI data is NOT modified.
             * ---------------------------------------------------------
             */
            String repairHistorySql =
                    "INSERT INTO public.inward_micr_repair_history "
                  + "(cheque_no, old_value, new_value, "
                  + "changed_by, varified_by) "
                  + "VALUES (?, ?, ?, ?, NULL)";

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 repairHistorySql)) {

                statement.setString(
                        1,
                        chequeNumber);

                statement.setString(
                        2,
                        originalMicr);

                statement.setString(
                        3,
                        repairedMicr);

                statement.setLong(
                        4,
                        userId);

                int inserted =
                        statement.executeUpdate();

                if (inserted != 1) {

                    connection.rollback();

                    return false;
                }
            }

            /*
             * ---------------------------------------------------------
             * 2. Change CHEQUE status to DATA_ENTRY.
             * ---------------------------------------------------------
             */
            String statusHistorySql =
                    "INSERT INTO public.inward_cheque_status_history "
                  + "(cheque_number, status, rejection_reason_code, "
                  + "return_reason_code, maker_id, maker_action, "
                  + "maker_action_on, checker_id, checker_action, "
                  + "checker_action_on, remarks) "
                  + "VALUES (?, ?, NULL, NULL, ?, ?, CURRENT_TIMESTAMP, "
                  + "NULL, NULL, NULL, ?)";

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 statusHistorySql)) {

                statement.setString(
                        1,
                        chequeNumber);

                statement.setString(
                        2,
                        STATUS_DATA_ENTRY);

                statement.setLong(
                        3,
                        userId);

                statement.setString(
                        4,
                        "MICR_REPAIR_COMPLETED");

                if (remarks == null
                        || remarks.trim().isEmpty()) {

                    statement.setNull(
                            5,
                            Types.VARCHAR);

                } else {

                    statement.setString(
                            5,
                            remarks.trim());
                }

                int inserted =
                        statement.executeUpdate();

                if (inserted != 1) {

                    connection.rollback();

                    return false;
                }
            }

            connection.commit();

            return true;

        } catch (SQLException e) {

            if (connection != null) {

                try {
                    connection.rollback();

                } catch (SQLException rollbackException) {

                    rollbackException.printStackTrace();
                }
            }

            throw new RuntimeException(
                    "Failed to save MICR repair for cheque "
                            + chequeNumber,
                    e);

        } finally {

            if (connection != null) {

                try {

                    connection.setAutoCommit(true);
                    connection.close();

                } catch (SQLException closeException) {

                    closeException.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean markBatchReadyForDataEntry(
            long batchId,
            long userId) {

        Connection connection = null;

        try {

            connection =
                    ConnectionPool
                            .getDataSource()
                            .getConnection();

            connection.setAutoCommit(false);

            /*
             * Find the current/latest batch status.
             */
            String latestStatusSql =
                    "SELECT batch_status "
                  + "FROM public.inward_batch_history "
                  + "WHERE batch_id = ? "
                  + "ORDER BY changed_on DESC "
                  + "LIMIT 1";

            String latestStatus = null;

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 latestStatusSql)) {

                statement.setLong(
                        1,
                        batchId);

                try (ResultSet resultSet =
                             statement.executeQuery()) {

                    if (resultSet.next()) {

                        latestStatus =
                                resultSet.getString(
                                        "batch_status");
                    }
                }
            }

            /*
             * Batch already ready for Data Entry.
             */
            if (STATUS_DATA_ENTRY.equalsIgnoreCase(
                    latestStatus)) {

                connection.commit();

                return true;
            }

            /*
             * Change BATCH status to DATA_ENTRY.
             */
            String historySql =
                    "INSERT INTO public.inward_batch_history "
                  + "(batch_id, batch_status, changed_on, "
                  + "changed_by, reason, remarks) "
                  + "VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?, ?)";

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 historySql)) {

                statement.setLong(
                        1,
                        batchId);

                statement.setString(
                        2,
                        STATUS_DATA_ENTRY);

                statement.setLong(
                        3,
                        userId);

                statement.setString(
                        4,
                        "MICR_REPAIR_COMPLETED");

                statement.setString(
                        5,
                        "Batch ready for Data Entry");

                int inserted =
                        statement.executeUpdate();

                if (inserted != 1) {

                    connection.rollback();

                    return false;
                }
            }

            connection.commit();

            return true;

        } catch (SQLException e) {

            if (connection != null) {

                try {
                    connection.rollback();

                } catch (SQLException rollbackException) {

                    rollbackException.printStackTrace();
                }
            }

            throw new RuntimeException(
                    "Failed to update batch "
                            + batchId
                            + " to DATA_ENTRY",
                    e);

        } finally {

            if (connection != null) {

                try {

                    connection.setAutoCommit(true);
                    connection.close();

                } catch (SQLException closeException) {

                    closeException.printStackTrace();
                }
            }
        }
    }
}