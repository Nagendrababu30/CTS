package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.cts.inward.dto.ReturnReasonDto;
import com.cts.inward.model.NpciChequeData;
import com.cts.inward.model.OcrChequeData;
import com.cts.inward.config.ConnectionPool;

public class MicrRepairDaoImpl implements MicrRepairDao {

    /*
     * ---------------------------------------------------------------------
     * NPCI CHEQUES
     * ---------------------------------------------------------------------
     */
    @Override
    public List<NpciChequeData> getNpciCheques(long batchId) {

        List<NpciChequeData> cheques = new ArrayList<>();

        String sql =
                "SELECT "
                + "cheque_number, "
                + "batch_id, "
                + "account_number, "
                + "cheque_date, "
                + "drawer_name, "
                + "amount, "
                + "micr_code, "
                + "city_code, "
                + "bank_code, "
                + "branch_code "
                + "FROM public.inward_cheque "
                + "WHERE batch_id = ? "
                + "ORDER BY cheque_number";

        try (Connection connection = ConnectionPool.getDataSource().getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, batchId);

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {

                    NpciChequeData cheque =
                            NpciChequeData.of(
                                    rs.getString("cheque_number"),
                                    rs.getLong("batch_id"),
                                    rs.getString("account_number"),
                                    rs.getDate("cheque_date") != null
                                            ? rs.getDate("cheque_date").toLocalDate()
                                            : null,
                                    rs.getString("drawer_name"),
                                    rs.getBigDecimal("amount"),
                                    rs.getString("micr_code"),
                                    rs.getString("city_code"),
                                    rs.getString("bank_code"),
                                    rs.getString("branch_code"));

                    cheques.add(cheque);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to fetch NPCI cheques for batch " + batchId,
                    e);
        }

        return cheques;
    }


    /*
     * ---------------------------------------------------------------------
     * OCR CHEQUES
     * ---------------------------------------------------------------------
     *
     * IMPORTANT:
     *
     * Do NOT join:
     *
     *      ob.file_id = ib.file_id
     *
     * because the OCR file has its own inward_file record.
     *
     * Example:
     *
     * inward_batch:
     *     batch_id = 8
     *     file_id  = 11
     *
     * OCR:
     *     batch_id = 8
     *     file_id  = 13
     *
     * Therefore the correct relationship is:
     *
     *      ob.batch_id = ?
     *
     * ---------------------------------------------------------------------
     */
    @Override
    public List<OcrChequeData> getOcrCheques(long batchId) {

        List<OcrChequeData> cheques = new ArrayList<>();

        String sql =
                "SELECT "
                + "o.cheque_number, "
                + "o.ocr_batch_id, "
                + "o.account_number, "
                + "o.cheque_date, "
                + "o.drawer_name, "
                + "o.amount, "
                + "o.micr_code, "
                + "o.branch_code, "
                + "o.city_code, "
                + "o.bank_code "
                + "FROM public.ocr_cheque_data o "
                + "INNER JOIN public.ocr_batch ob "
                + "    ON o.ocr_batch_id = ob.ocr_batch_id "
                + "WHERE ob.batch_id = ? "
                + "ORDER BY o.cheque_number";

        try (Connection connection = ConnectionPool.getDataSource().getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, batchId);

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {

                    OcrChequeData cheque =
                            OcrChequeData.of(
                                    rs.getString("cheque_number"),
                                    rs.getLong("ocr_batch_id"),
                                    rs.getString("account_number"),
                                    rs.getDate("cheque_date") != null
                                            ? rs.getDate("cheque_date").toLocalDate()
                                            : null,
                                    rs.getString("drawer_name"),
                                    rs.getBigDecimal("amount"),
                                    rs.getString("micr_code"),
                                    rs.getString("city_code"),
                                    rs.getString("bank_code"),
                                    rs.getString("branch_code"));

                    cheques.add(cheque);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to fetch OCR cheques for batch " + batchId,
                    e);
        }

        return cheques;
    }


    /*
     * ---------------------------------------------------------------------
     * COMPLETED MICR REPAIR
     * ---------------------------------------------------------------------
     */
    @Override
    public String getCompletedRepairedMicr(String chequeNumber) {

        String sql =
                "SELECT repaired_micr "
                + "FROM public.inward_micr_repair "
                + "WHERE cheque_number = ? "
                + "AND repair_status = 'COMPLETED' "
                + "ORDER BY repaired_on DESC "
                + "LIMIT 1";

        try (Connection connection = ConnectionPool.getDataSource().getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, chequeNumber);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    return rs.getString("repaired_micr");
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


    /*
     * ---------------------------------------------------------------------
     * FRONT IMAGE
     * ---------------------------------------------------------------------
     */
    @Override
    public String getFrontImagePath(String chequeNumber) {

        String sql =
                "SELECT front_image "
                + "FROM public.inward_cheque_image "
                + "WHERE cheque_number = ?";

        try (Connection connection = ConnectionPool.getDataSource().getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, chequeNumber);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    return rs.getString("front_image");
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to fetch front image for cheque "
                            + chequeNumber,
                    e);
        }

        return null;
    }


    /*
     * ---------------------------------------------------------------------
     * BACK IMAGE
     * ---------------------------------------------------------------------
     */
    @Override
    public String getBackImagePath(String chequeNumber) {

        String sql =
                "SELECT back_image "
                + "FROM public.inward_cheque_image "
                + "WHERE cheque_number = ?";

        try (Connection connection = ConnectionPool.getDataSource().getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, chequeNumber);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    return rs.getString("back_image");
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to fetch back image for cheque "
                            + chequeNumber,
                    e);
        }

        return null;
    }


    /*
     * ---------------------------------------------------------------------
     * MAKER RETURN REASONS
     * ---------------------------------------------------------------------
     */
    @Override
    public List<ReturnReasonDto> getMakerReturnReasons() {

        List<ReturnReasonDto> reasons = new ArrayList<>();

        String sql =
                "SELECT return_reason_code, description "
                + "FROM public.inward_cheque_return_reason "
                + "WHERE applicable_role IN ('MAKER', 'BOTH') "
                + "AND status = 'ACTIVE' "
                + "ORDER BY description";

        try (Connection connection = ConnectionPool.getDataSource().getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {

                reasons.add(
                        new ReturnReasonDto(
                                rs.getString("return_reason_code"),
                                rs.getString("description")));
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to fetch maker return reasons",
                    e);
        }

        return reasons;
    }


    /*
     * ---------------------------------------------------------------------
     * MAKER RETURN
     * ---------------------------------------------------------------------
     */
    @Override
    public boolean saveMakerReturn(
            String chequeNumber,
            String returnReasonCode,
            String makerRemarks,
            long userId) {

        Connection connection = null;

        try {

            connection = ConnectionPool.getDataSource().getConnection();
            connection.setAutoCommit(false);

            /*
             * 1. Save return request.
             */
            String returnSql =
                    "INSERT INTO public.inward_cheque_return "
                    + "(cheque_number, "
                    + "return_reason_code, "
                    + "maker_remarks, "
                    + "requested_by, "
                    + "return_status) "
                    + "VALUES (?, ?, ?, ?, 'RETURN_REQUESTED')";

            try (PreparedStatement statement =
                         connection.prepareStatement(returnSql)) {

                statement.setString(1, chequeNumber);
                statement.setString(2, returnReasonCode);
                statement.setString(3, makerRemarks);
                statement.setLong(4, userId);

                statement.executeUpdate();
            }


            /*
             * 2. Save status history.
             */
            String historySql =
                    "INSERT INTO public.inward_cheque_status_history "
                    + "(cheque_number, "
                    + "status, "
                    + "return_reason_code, "
                    + "maker_id, "
                    + "maker_action, "
                    + "maker_action_on, "
                    + "remarks) "
                    + "VALUES (?, 'RETURN_REQUESTED', ?, ?, "
                    + "'RETURN_REQUESTED', CURRENT_TIMESTAMP, ?)";

            try (PreparedStatement statement =
                         connection.prepareStatement(historySql)) {

                statement.setString(1, chequeNumber);
                statement.setString(2, returnReasonCode);
                statement.setLong(3, userId);
                statement.setString(4, makerRemarks);

                statement.executeUpdate();
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


    /*
     * ---------------------------------------------------------------------
     * MICR REPAIR
     * ---------------------------------------------------------------------
     */
    @Override
    public boolean saveMicrRepair(
            String chequeNumber,
            String originalMicr,
            String repairedMicr,
            String remarks,
            long userId) {

        Connection connection = null;

        try {

            connection = ConnectionPool.getDataSource().getConnection();
            connection.setAutoCommit(false);

            long repairId = findLatestRepairId(
                    connection,
                    chequeNumber);

            /*
             * 1. Update existing repair or create a new repair.
             */
            if (repairId > 0) {

                String updateSql =
                        "UPDATE public.inward_micr_repair "
                        + "SET repaired_micr = ?, "
                        + "repair_status = 'COMPLETED', "
                        + "maker_id = ?, "
                        + "repaired_on = CURRENT_TIMESTAMP "
                        + "WHERE repair_id = ?";

                try (PreparedStatement statement =
                             connection.prepareStatement(updateSql)) {

                    statement.setString(1, repairedMicr);
                    statement.setLong(2, userId);
                    statement.setLong(3, repairId);

                    statement.executeUpdate();
                }

            } else {

                String insertSql =
                        "INSERT INTO public.inward_micr_repair "
                        + "(cheque_number, "
                        + "original_micr, "
                        + "repaired_micr, "
                        + "repair_status, "
                        + "maker_id, "
                        + "repaired_on) "
                        + "VALUES (?, ?, ?, 'COMPLETED', ?, CURRENT_TIMESTAMP)";

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     insertSql,
                                     java.sql.Statement.RETURN_GENERATED_KEYS)) {

                    statement.setString(1, chequeNumber);
                    statement.setString(2, originalMicr);
                    statement.setString(3, repairedMicr);
                    statement.setLong(4, userId);

                    statement.executeUpdate();

                    try (ResultSet keys =
                                 statement.getGeneratedKeys()) {

                        if (keys.next()) {
                            repairId = keys.getLong(1);
                        }
                    }
                }
            }


            /*
             * 2. Save MICR change history.
             */
            String changeHistorySql =
                    "INSERT INTO public.inward_micr_change_history "
                    + "(micr_repair_id, "
                    + "old_micr, "
                    + "is_checker_reviewed, "
                    + "checker_user_id, "
                    + "checker_reviewed_on) "
                    + "VALUES (?, ?, 'N', NULL, NULL)";

            try (PreparedStatement statement =
                         connection.prepareStatement(changeHistorySql)) {

                statement.setLong(1, repairId);
                statement.setString(2, originalMicr);

                statement.executeUpdate();
            }


            /*
             * 3. Save cheque status history.
             */
            String statusHistorySql =
                    "INSERT INTO public.inward_cheque_status_history "
                    + "(cheque_number, "
                    + "status, "
                    + "maker_id, "
                    + "maker_action, "
                    + "maker_action_on, "
                    + "remarks) "
                    + "VALUES (?, 'MICR_REPAIRED', ?, "
                    + "'MICR_REPAIRED', CURRENT_TIMESTAMP, ?)";

            try (PreparedStatement statement =
                         connection.prepareStatement(statusHistorySql)) {

                statement.setString(1, chequeNumber);
                statement.setLong(2, userId);
                statement.setString(3, remarks);

                statement.executeUpdate();
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


    /*
     * ---------------------------------------------------------------------
     * FIND LATEST REPAIR
     * ---------------------------------------------------------------------
     */
    private long findLatestRepairId(
            Connection connection,
            String chequeNumber)
            throws SQLException {

        String sql =
                "SELECT repair_id "
                + "FROM public.inward_micr_repair "
                + "WHERE cheque_number = ? "
                + "ORDER BY repaired_on DESC "
                + "LIMIT 1";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, chequeNumber);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    return rs.getLong("repair_id");
                }
            }
        }

        return 0;
    }
}