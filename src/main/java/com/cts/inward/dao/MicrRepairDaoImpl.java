package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cts.inward.config.ConnectionPool;
import com.cts.inward.dto.ReturnReasonDto;
import com.cts.inward.model.NpciChequeData;
import com.cts.inward.model.OcrChequeData;

public class MicrRepairDaoImpl implements MicrRepairDao {

    private static final String STATUS_RETURN_BY_MAKER =
            "RETURN_BY_MAKER";

    private static final String STATUS_MICR_REPAIRED =
            "MICR_REPAIRED";

    private static final String STATUS_DATA_ENTRY =
            "DATA_ENTRY";

    // =========================================================
    // Get NPCI Cheques
    // =========================================================

    @Override
    public List<NpciChequeData> getNpciCheques(
            long batchId) {

        String sql =
                "SELECT "
                        + "inward_cheque_id, "
                        + "cheque_number, "
                        + "batch_id, "
                        + "account_number, "
                        + "cheque_date, "
                        + "presenting_date, "
                        + "amount, "
                        + "amount_in_words, "
                        + "micr_code, "
                        + "city_code, "
                        + "bank_code, "
                        + "branch_code, "
                        + "drawer_name, "
                        + "payee_name, "
                        + "payee_account_number "
                        + "FROM public.inward_cheque "
                        + "WHERE batch_id = ? "
                        + "ORDER BY inward_cheque_id";

        List<NpciChequeData> cheques =
                new ArrayList<>();

        try (
                Connection connection =
                        ConnectionPool
                                .getDataSource()
                                .getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    batchId);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

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
                                            ? resultSet
                                                    .getDate(
                                                            "cheque_date")
                                                    .toLocalDate()
                                            : null,

                                    resultSet.getDate(
                                            "presenting_date") != null
                                            ? resultSet
                                                    .getDate(
                                                            "presenting_date")
                                                    .toLocalDate()
                                            : null,

                                    resultSet.getBigDecimal(
                                            "amount"),

                                    resultSet.getString(
                                            "amount_in_words"),

                                    resultSet.getString(
                                            "micr_code"),

                                    resultSet.getString(
                                            "city_code"),

                                    resultSet.getString(
                                            "bank_code"),

                                    resultSet.getString(
                                            "branch_code"),

                                    resultSet.getString(
                                            "drawer_name"),

                                    resultSet.getString(
                                            "payee_name"),

                                    resultSet.getString(
                                            "payee_account_number"));

                    cheque.setInwardChequeId(
                            resultSet.getLong(
                                    "inward_cheque_id"));

                    cheques.add(cheque);
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Error retrieving NPCI cheques for batch "
                            + batchId,
                    e);
        }

        return cheques;
    }

    // =========================================================
    // Get OCR Cheques
    // =========================================================

    @Override
    public List<OcrChequeData> getOcrCheques(
            long batchId) {

        String sql =
                "SELECT "
                        + "o.inward_cheque_id, "
                        + "o.cheque_number, "
                        + "o.account_number, "
                        + "o.amount, "
                        + "o.micr_code, "
                        + "o.cheque_date, "
                        + "o.branch_code, "
                        + "o.city_code, "
                        + "o.bank_code, "
                        + "o.payee_name, "
                        + "o.payee_account_number "
                        + "FROM public.ocr_cheque_data o "
                        + "INNER JOIN public.ocr_batch ob "
                        + "ON o.ocr_batch_id = ob.ocr_batch_id "
                        + "WHERE ob.batch_id = ? "
                        + "ORDER BY o.inward_cheque_id";

        List<OcrChequeData> cheques =
                new ArrayList<>();

        try (
                Connection connection =
                        ConnectionPool
                                .getDataSource()
                                .getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    batchId);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

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
                                    ? resultSet
                                            .getDate(
                                                    "cheque_date")
                                            .toLocalDate()
                                    : null);

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

                    cheque.setPayeeName(
                            resultSet.getString(
                                    "payee_name"));

                    cheque.setPayeeAccountNumber(
                            resultSet.getString(
                                    "payee_account_number"));

                    cheques.add(cheque);
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Error retrieving OCR cheques for batch "
                            + batchId,
                    e);
        }

        return cheques;
    }

    // =========================================================
    // Get Completed Repaired MICR
    // =========================================================

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
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    chequeNumber);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

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
    public Map<String, String> getCompletedRepairedMicrs(
            List<String> chequeNumbers) {

        Map<String, String> map = new HashMap<>();
        if (chequeNumbers == null || chequeNumbers.isEmpty()) {
            return map;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT ON (cheque_no) cheque_no, new_value "
                        + "FROM public.inward_micr_repair_history "
                        + "WHERE cheque_no IN (");
        int count = 0;
        for (String cn : chequeNumbers) {
            if (cn != null && !cn.trim().isEmpty()) {
                if (count > 0) sql.append(",");
                sql.append("?");
                count++;
            }
        }
        sql.append(") ORDER BY cheque_no, micr_repair_id DESC");

        if (count == 0) return map;

        try (Connection conn = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            for (String cn : chequeNumbers) {
                if (cn != null && !cn.trim().isEmpty()) {
                    ps.setString(idx++, cn.trim());
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String chq = rs.getString("cheque_no");
                    String val = rs.getString("new_value");
                    if (chq != null && val != null) {
                        map.put(chq.trim(), val.trim());
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to batch fetch completed repaired MICRs", e);
        }
        return map;
    }

    // =========================================================
    // Get Latest Cheque Status
    // =========================================================

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
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    chequeNumber);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

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
    public Map<String, String> getLatestChequeStatuses(
            List<String> chequeNumbers) {

        Map<String, String> map = new HashMap<>();
        if (chequeNumbers == null || chequeNumbers.isEmpty()) {
            return map;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT ON (cheque_number) cheque_number, status "
                        + "FROM public.inward_cheque_status_history "
                        + "WHERE cheque_number IN (");
        int count = 0;
        for (String cn : chequeNumbers) {
            if (cn != null && !cn.trim().isEmpty()) {
                if (count > 0) sql.append(",");
                sql.append("?");
                count++;
            }
        }
        sql.append(") ORDER BY cheque_number, status_history_id DESC");

        if (count == 0) return map;

        try (Connection conn = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            for (String cn : chequeNumbers) {
                if (cn != null && !cn.trim().isEmpty()) {
                    ps.setString(idx++, cn.trim());
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String chq = rs.getString("cheque_number");
                    String st = rs.getString("status");
                    if (chq != null && st != null) {
                        map.put(chq.trim(), st.trim());
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to batch fetch latest cheque statuses", e);
        }
        return map;
    }

    // =========================================================
    // Get Latest Cheque Return Reason
    // =========================================================

    @Override
    public String getLatestChequeReturnReason(
            String chequeNumber) {

        String sql =
                "SELECT return_reason_code "
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
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    chequeNumber);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (resultSet.next()) {

                    return resultSet.getString(
                            "return_reason_code");
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Failed to fetch latest cheque return reason for "
                            + chequeNumber,
                    e);
        }

        return null;
    }

    @Override
    public Map<String, String> getLatestChequeReturnReasons(
            List<String> chequeNumbers) {

        Map<String, String> map = new HashMap<>();
        if (chequeNumbers == null || chequeNumbers.isEmpty()) {
            return map;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT ON (cheque_number) cheque_number, return_reason_code "
                        + "FROM public.inward_cheque_status_history "
                        + "WHERE cheque_number IN (");
        int count = 0;
        for (String cn : chequeNumbers) {
            if (cn != null && !cn.trim().isEmpty()) {
                if (count > 0) sql.append(",");
                sql.append("?");
                count++;
            }
        }
        sql.append(") ORDER BY cheque_number, status_history_id DESC");

        if (count == 0) return map;

        try (Connection conn = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            for (String cn : chequeNumbers) {
                if (cn != null && !cn.trim().isEmpty()) {
                    ps.setString(idx++, cn.trim());
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String chq = rs.getString("cheque_number");
                    String reason = rs.getString("return_reason_code");
                    if (chq != null && reason != null) {
                        map.put(chq.trim(), reason.trim());
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to batch fetch latest cheque return reasons", e);
        }
        return map;
    }

    // =========================================================
    // Is Batch Returned To Maker
    // =========================================================

    @Override
    public boolean isBatchReturnedToMaker(
            long batchId) {

        String sql =
                "SELECT batch_status "
                        + "FROM public.inward_batch_history "
                        + "WHERE batch_id = ? "
                        + "ORDER BY batch_history_id DESC "
                        + "LIMIT 1";

        try (
                Connection connection =
                        ConnectionPool
                                .getDataSource()
                                .getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    batchId);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (resultSet.next()) {

                    String status =
                            resultSet.getString(
                                    "batch_status");

                    return "RETURN_TO_MAKER"
                            .equalsIgnoreCase(
                                    status);
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Failed to check if batch is returned to maker: "
                            + batchId,
                    e);
        }

        return false;
    }

    @Override
    public boolean chequeNeedsMicrRepair(String chequeNumber) {
        if (chequeNumber == null || chequeNumber.trim().isEmpty()) {
            return false;
        }

        String checkerReasonsSql = """
                SELECT sh.return_reason_code
                FROM public.inward_cheque_status_history sh
                WHERE sh.cheque_number = ?
                  AND sh.status = 'RETURN_TO_MAKER'
                ORDER BY sh.status_history_id DESC
                """;

        List<String> checkerCodes = new ArrayList<>();
        try (Connection conn = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(checkerReasonsSql)) {
            ps.setString(1, chequeNumber.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("return_reason_code");
                    if (code != null && !code.trim().isEmpty()) {
                        checkerCodes.add(code.trim().toUpperCase());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (checkerCodes.isEmpty()) {
            return false;
        }

        for (String code : checkerCodes) {
            if (code.startsWith("CR-MICR-") || code.startsWith("MR-MICR-") || code.startsWith("MICR_")) {
                return true;
            }
            if (code.startsWith("CR-IMG-")) {
                String makerSql = """
                        SELECT return_reason_code
                        FROM (
                            SELECT return_reason_code, status_history_id AS ord
                            FROM public.inward_cheque_status_history
                            WHERE cheque_number = ?
                              AND (status = 'RETURN_BY_MAKER' OR maker_action = 'RETURN_BY_MAKER')
                            UNION ALL
                            SELECT return_reason_code, return_id AS ord
                            FROM public.inward_cheque_return
                            WHERE cheque_number = ?
                        ) sub
                        ORDER BY ord DESC
                        """;
                try (Connection conn = ConnectionPool.getDataSource().getConnection();
                     PreparedStatement mPs = conn.prepareStatement(makerSql)) {
                    mPs.setString(1, chequeNumber.trim());
                    mPs.setString(2, chequeNumber.trim());
                    try (ResultSet mRs = mPs.executeQuery()) {
                        while (mRs.next()) {
                            String mCode = mRs.getString("return_reason_code");
                            if (mCode != null && mCode.trim().toUpperCase().startsWith("MR-MICR-")) {
                                return true;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    // =========================================================
    // Get Batch ID By Cheque Number
    // =========================================================

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
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    chequeNumber);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

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

    // =========================================================
    // Get Batch Cheque Position
    // =========================================================

    @Override
    public int getBatchChequePosition(
            long batchId,
            String chequeNumber) {

        String sql =
                "SELECT position_no "
                        + "FROM ("
                        + "SELECT cheque_number, "
                        + "ROW_NUMBER() OVER ("
                        + "PARTITION BY batch_id "
                        + "ORDER BY inward_cheque_id"
                        + ") AS position_no "
                        + "FROM public.inward_cheque "
                        + "WHERE batch_id = ?"
                        + ") x "
                        + "WHERE cheque_number = ?";

        try (
                Connection connection =
                        ConnectionPool
                                .getDataSource()
                                .getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    batchId);

            statement.setString(
                    2,
                    chequeNumber);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (resultSet.next()) {

                    return resultSet.getInt(
                            "position_no");
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Failed to find batch position for cheque "
                            + chequeNumber
                            + " in batch "
                            + batchId,
                    e);
        }

        return 0;
    }

    // =========================================================
    // Get Batch Total Cheque Count
    // =========================================================

    @Override
    public int getBatchTotalChequeCount(
            long batchId) {

        String sql =
                "SELECT COUNT(*) "
                        + "FROM public.inward_cheque "
                        + "WHERE batch_id = ?";

        try (
                Connection connection =
                        ConnectionPool
                                .getDataSource()
                                .getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(
                    1,
                    batchId);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (resultSet.next()) {

                    return resultSet.getInt(1);
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Failed to find total cheque count for batch "
                            + batchId,
                    e);
        }

        return 0;
    }

    // =========================================================
    // Front Image
    // =========================================================

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

    // =========================================================
    // Back Image
    // =========================================================

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

    // =========================================================
    // Get Image Path
    // =========================================================

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
                        connection.prepareStatement(sql)
        ) {

            statement.setString(
                    1,
                    chequeNumber);

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (resultSet.next()) {

                    return resultSet.getString(
                            columnName);
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to load cheque image for "
                            + chequeNumber,
                    e);
        }

        return null;
    }

    // =========================================================
    // Get Maker Return Reasons
    // =========================================================

    @Override
    public List<ReturnReasonDto> getMakerReturnReasons() {

        List<ReturnReasonDto> reasons =
                new ArrayList<>();

        String sql =
                "SELECT "
                        + "return_reason_code, "
                        + "description "
                        + "FROM public.inward_cheque_return_reason "
                        + "WHERE applicable_role = 'MAKER' "
                        + "  AND return_reason_code LIKE 'MR-MICR-%' "
                        + "ORDER BY return_reason_code";

        try (
                Connection connection =
                        ConnectionPool
                                .getDataSource()
                                .getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            while (resultSet.next()) {

                reasons.add(
                        new ReturnReasonDto(
                                resultSet.getString(
                                        "return_reason_code"),

                                resultSet.getString(
                                        "description")));
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Failed to fetch maker return reasons",
                    e);
        }

        return reasons;
    }

    // =========================================================
    // Save Maker Return
    // =========================================================

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

            // -------------------------------------------------
            // Return record
            // -------------------------------------------------

            String returnSql =
                    "INSERT INTO public.inward_cheque_return "
                            + "("
                            + "cheque_number, "
                            + "return_reason_code, "
                            + "maker_remarks, "
                            + "requested_by, "
                            + "return_status"
                            + ") "
                            + "VALUES (?, ?, ?, ?, ?)";

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    returnSql)
            ) {

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

            // -------------------------------------------------
            // Cheque status history (update existing row, fallback to insert)
            // -------------------------------------------------

            String updateHistorySql =
                    "UPDATE public.inward_cheque_status_history "
                            + "SET status = ?, return_reason_code = ?, maker_id = ?, "
                            + "maker_action = ?, maker_action_on = CURRENT_TIMESTAMP, remarks = ? "
                            + "WHERE cheque_number = ?";

            int updatedHist = 0;
            try (PreparedStatement statement = connection.prepareStatement(updateHistorySql)) {
                statement.setString(1, STATUS_RETURN_BY_MAKER);
                statement.setString(2, returnReasonCode);
                statement.setLong(3, userId);
                statement.setString(4, STATUS_RETURN_BY_MAKER);
                if (makerRemarks == null || makerRemarks.trim().isEmpty()) {
                    statement.setNull(5, Types.VARCHAR);
                } else {
                    statement.setString(5, makerRemarks.trim());
                }
                statement.setString(6, chequeNumber);
                updatedHist = statement.executeUpdate();
            }

            if (updatedHist == 0) {
                String historySql =
                        "INSERT INTO public.inward_cheque_status_history "
                                + "("
                                + "cheque_number, "
                                + "status, "
                                + "rejection_reason_code, "
                                + "return_reason_code, "
                                + "maker_id, "
                                + "maker_action, "
                                + "maker_action_on, "
                                + "checker_id, "
                                + "checker_action, "
                                + "checker_action_on, "
                                + "remarks"
                                + ") "
                                + "VALUES "
                                + "(?, ?, NULL, ?, ?, ?, "
                                + "CURRENT_TIMESTAMP, "
                                + "NULL, NULL, NULL, ?)";

                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        historySql)
                ) {

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

                    statement.executeUpdate();
                }
            }

            // -------------------------------------------------
            // Find batch
            // -------------------------------------------------

            long batchId = 0L;

            String batchIdSql =
                    "SELECT batch_id "
                            + "FROM public.inward_cheque "
                            + "WHERE cheque_number = ? "
                            + "LIMIT 1";

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    batchIdSql)
            ) {

                statement.setString(
                        1,
                        chequeNumber);

                try (
                        ResultSet resultSet =
                                statement.executeQuery()
                ) {

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

            // -------------------------------------------------
            // Check remaining MICR repair cheques
            // -------------------------------------------------

            int pendingMicrCount = 0;

            String pendingMicrSql = """
                    SELECT COUNT(*)
                    FROM public.inward_cheque c
                    INNER JOIN LATERAL
                    (
                        SELECT h.status
                        FROM public.inward_cheque_status_history h
                        WHERE h.cheque_number = c.cheque_number
                        ORDER BY h.status_history_id DESC
                        LIMIT 1
                    ) latest
                    ON TRUE
                    WHERE c.batch_id = ?
                    AND latest.status = 'MICR_REPAIR'
                    """;

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    pendingMicrSql)
            ) {

                statement.setLong(
                        1,
                        batchId);

                try (
                        ResultSet resultSet =
                                statement.executeQuery()
                ) {

                    if (resultSet.next()) {

                        pendingMicrCount =
                                resultSet.getInt(1);
                    }
                }
            }

            /*
             * If no MICR_REPAIR cheque remains,
             * the batch can move to DATA_ENTRY.
             */
            if (pendingMicrCount == 0) {

                String latestBatchStatusSql =
                        "SELECT batch_status "
                                + "FROM public.inward_batch_history "
                                + "WHERE batch_id = ? "
                                + "ORDER BY batch_history_id DESC "
                                + "LIMIT 1";

                String latestBatchStatus = null;

                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        latestBatchStatusSql)
                ) {

                    statement.setLong(
                            1,
                            batchId);

                    try (
                            ResultSet resultSet =
                                    statement.executeQuery()
                    ) {

                        if (resultSet.next()) {

                            latestBatchStatus =
                                    resultSet.getString(
                                            "batch_status");
                        }
                    }
                }

                if (!STATUS_DATA_ENTRY.equalsIgnoreCase(
                        latestBatchStatus)) {

                    String batchHistorySql =
                            "INSERT INTO public.inward_batch_history "
                                    + "("
                                    + "batch_id, "
                                    + "batch_status, "
                                    + "changed_on, "
                                    + "changed_by, "
                                    + "reason, "
                                    + "remarks"
                                    + ") "
                                    + "VALUES "
                                    + "(?, ?, CURRENT_TIMESTAMP, ?, ?, ?)";

                    try (
                            PreparedStatement statement =
                                    connection.prepareStatement(
                                            batchHistorySql)
                    ) {

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
                                "All cheques completed MICR stage; "
                                        + "batch ready for Data Entry");

                        int inserted =
                                statement.executeUpdate();

                        if (inserted != 1) {

                            connection.rollback();

                            return false;
                        }
                    }
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

    // =========================================================
    // Save MICR Repair
    // =========================================================

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

            // -------------------------------------------------
            // MICR repair history
            // -------------------------------------------------

            // -------------------------------------------------
            // MICR repair history (UPDATE if exists, else INSERT)
            // -------------------------------------------------

            Long existingRepairId = null;
            String checkExistingSql =
                    "SELECT micr_repair_id "
                            + "FROM public.inward_micr_repair_history "
                            + "WHERE cheque_no = ? "
                            + "ORDER BY micr_repair_id DESC "
                            + "LIMIT 1";

            try (PreparedStatement checkStmt = connection.prepareStatement(checkExistingSql)) {
                checkStmt.setString(1, chequeNumber);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        existingRepairId = rs.getLong("micr_repair_id");
                    }
                }
            }

            if (existingRepairId != null) {
                // Update the previously saved row with the latest corrected MICR
                String updateHistorySql =
                        "UPDATE public.inward_micr_repair_history "
                                + "SET new_value = ?, changed_by = ? "
                                + "WHERE micr_repair_id = ?";

                try (PreparedStatement updateStmt = connection.prepareStatement(updateHistorySql)) {
                    updateStmt.setString(1, repairedMicr);
                    updateStmt.setLong(2, userId);
                    updateStmt.setLong(3, existingRepairId);

                    int updated = updateStmt.executeUpdate();
                    if (updated != 1) {
                        connection.rollback();
                        return false;
                    }
                }
            } else {
                // First time save: insert new row
                String insertHistorySql =
                        "INSERT INTO public.inward_micr_repair_history "
                                + "(cheque_no, old_value, new_value, changed_by, varified_by) "
                                + "VALUES (?, ?, ?, ?, NULL)";

                try (PreparedStatement insertStmt = connection.prepareStatement(insertHistorySql)) {
                    insertStmt.setString(1, chequeNumber);
                    insertStmt.setString(2, originalMicr);
                    insertStmt.setString(3, repairedMicr);
                    insertStmt.setLong(4, userId);

                    int inserted = insertStmt.executeUpdate();
                    if (inserted != 1) {
                        connection.rollback();
                        return false;
                    }
                }
            }


            // -------------------------------------------------
            // IMPORTANT:
            // Individual repaired cheque = MICR_REPAIRED
            //
            // Do NOT use DATA_ENTRY here.
            // -------------------------------------------------

            String statusHistorySql =
                    "INSERT INTO public.inward_cheque_status_history "
                            + "("
                            + "cheque_number, "
                            + "status, "
                            + "rejection_reason_code, "
                            + "return_reason_code, "
                            + "maker_id, "
                            + "maker_action, "
                            + "maker_action_on, "
                            + "checker_id, "
                            + "checker_action, "
                            + "checker_action_on, "
                            + "remarks"
                            + ") "
                            + "VALUES "
                            + "(?, ?, NULL, NULL, ?, ?, "
                            + "CURRENT_TIMESTAMP, "
                            + "NULL, NULL, NULL, ?)";

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    statusHistorySql)
            ) {

                statement.setString(
                        1,
                        chequeNumber);

                /*
                 * CORRECT:
                 * MICR_REPAIRED
                 */
                statement.setString(
                        2,
                        STATUS_MICR_REPAIRED);

                statement.setLong(
                        3,
                        userId);

                statement.setString(
                        4,
                        STATUS_MICR_REPAIRED);

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

    // =========================================================
    // Mark Batch Ready For Data Entry
    // =========================================================

    @Override
    public boolean markBatchReadyForDataEntry(
            long batchId,
            long userId) {

        if (batchId <= 0L
                || userId <= 0L) {

            return false;
        }

        Connection connection = null;

        try {

            connection =
                    ConnectionPool
                            .getDataSource()
                            .getConnection();

            connection.setAutoCommit(false);

            /*
             * At this point the service has already checked
             * that no cheque still needs MICR repair.
             *
             * Move applicable cheques to DATA_ENTRY.
             * Update existing status history rows (RETURN_BY_MAKER remains untouched).
             */
            String updateChequeStatusSql = """
                    UPDATE public.inward_cheque_status_history
                    SET status = 'DATA_ENTRY',
                        maker_id = ?,
                        maker_action = 'MICR_STAGE_COMPLETED',
                        maker_action_on = CURRENT_TIMESTAMP,
                        remarks = 'Cheque ready for Data Entry'
                    WHERE cheque_number IN (
                        SELECT c.cheque_number
                        FROM public.inward_cheque c
                        WHERE c.batch_id = ?
                    )
                    AND status NOT IN (
                        'RETURN_BY_MAKER',
                        'DATA_ENTRY',
                        'DATA_ENTRY_COMPLETED',
                        'SENT_TO_CHECKER'
                    )
                    """;

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    updateChequeStatusSql)
            ) {
                statement.setLong(1, userId);
                statement.setLong(2, batchId);
                statement.executeUpdate();
            }

            // Fallback insert for any cheque in batch that has no row in status history yet
            String insertMissingChequeSql = """
                    INSERT INTO public.inward_cheque_status_history
                    (
                        cheque_number,
                        status,
                        rejection_reason_code,
                        return_reason_code,
                        maker_id,
                        maker_action,
                        maker_action_on,
                        checker_id,
                        checker_action,
                        checker_action_on,
                        remarks
                    )
                    SELECT
                        c.cheque_number,
                        'DATA_ENTRY',
                        NULL,
                        NULL,
                        ?,
                        'MICR_STAGE_COMPLETED',
                        CURRENT_TIMESTAMP,
                        NULL,
                        NULL,
                        NULL,
                        'Cheque ready for Data Entry'
                    FROM public.inward_cheque c
                    WHERE c.batch_id = ?
                      AND NOT EXISTS (
                          SELECT 1 FROM public.inward_cheque_status_history h
                          WHERE h.cheque_number = c.cheque_number
                      )
                    """;

            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    insertMissingChequeSql)
            ) {
                statement.setLong(1, userId);
                statement.setLong(2, batchId);
                statement.executeUpdate();
            }

            // -------------------------------------------------
            // Update batch history status (update existing row, fallback to insert)
            // -------------------------------------------------

            String updateBatchHistorySql = """
                    UPDATE public.inward_batch_history
                    SET batch_status = 'DATA_ENTRY',
                        changed_on = CURRENT_TIMESTAMP,
                        changed_by = ?,
                        reason = 'MICR validation completed',
                        remarks = 'Batch ready for Data Entry'
                    WHERE batch_id = ?
                    """;

            int updatedBatch = 0;
            try (
                    PreparedStatement statement =
                            connection.prepareStatement(
                                    updateBatchHistorySql)
            ) {
                statement.setLong(1, userId);
                statement.setLong(2, batchId);
                updatedBatch = statement.executeUpdate();
            }

            if (updatedBatch == 0) {
                String batchHistorySql = """
                        INSERT INTO public.inward_batch_history
                        (
                            batch_id,
                            batch_status,
                            changed_on,
                            changed_by,
                            reason,
                            remarks
                        )
                        VALUES
                        (
                            ?,
                            'DATA_ENTRY',
                            CURRENT_TIMESTAMP,
                            ?,
                            'MICR validation completed',
                            'Batch ready for Data Entry'
                        )
                        """;

                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        batchHistorySql)
                ) {
                    statement.setLong(1, batchId);
                    statement.setLong(2, userId);
                    statement.executeUpdate();
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
                    "Failed to move batch "
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