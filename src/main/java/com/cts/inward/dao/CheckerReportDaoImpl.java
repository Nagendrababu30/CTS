package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.cts.inward.config.ConnectionPool;

public class CheckerReportDaoImpl implements CheckerReportDao {

    private final DataSource dataSource;

    private CheckerReportDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static CheckerReportDao of() {
        return new CheckerReportDaoImpl(
                ConnectionPool.getDataSource());
    }

    @Override
    public List<Map<String, Object>> getRrfReportData() {

        String sql = """
                WITH latest_batch_status AS (
                    SELECT
                        batch_id,
                        batch_status
                    FROM (
                        SELECT
                            batch_id,
                            batch_status,
                            ROW_NUMBER() OVER (
                                PARTITION BY batch_id
                                ORDER BY changed_on DESC,
                                         batch_history_id DESC
                            ) AS rn
                        FROM inward_batch_history
                    ) x
                    WHERE rn = 1
                ),

                latest_cheque_status AS (
                    SELECT
                        cheque_number,
                        status_history_id,
                        status,
                        remarks,
                        report_generated
                    FROM (
                        SELECT
                            h.*,
                            ROW_NUMBER() OVER (
                                PARTITION BY cheque_number
                                ORDER BY status_history_id DESC
                            ) AS rn
                        FROM inward_cheque_status_history h
                    ) x
                    WHERE rn = 1
                ),

                rejection_reasons AS (
                    SELECT
                        h.cheque_number,

                        STRING_AGG(
                            DISTINCT r.description,
                            ', '
                            ORDER BY r.description
                        ) AS return_reason

                    FROM inward_cheque_status_history h

                    CROSS JOIN LATERAL regexp_split_to_table(
                        COALESCE(h.rejection_reason_code, ''),
                        '\\s*,\\s*'
                    ) AS reason_code

                    JOIN inward_cheque_rejection_reason r
                        ON r.rejection_reason_code =
                           TRIM(reason_code)

                    WHERE UPPER(TRIM(h.status))
                            IN ('REJECT', 'REJECTED')

                      AND h.report_generated = 'N'

                    GROUP BY h.cheque_number
                ),

                rejection_history AS (
                    SELECT
                        cheque_number,

                        STRING_AGG(
                            status_history_id::text,
                            ','
                            ORDER BY status_history_id
                        ) AS status_history_ids

                    FROM inward_cheque_status_history

                    WHERE UPPER(TRIM(status))
                            IN ('REJECT', 'REJECTED')

                      AND report_generated = 'N'

                    GROUP BY cheque_number
                )

                SELECT
                    c.cheque_number AS cheque_no,
                    c.batch_id,
                    c.amount,
                    c.account_number,
                    c.payee_account_number,
                    c.payee_name,
                    c.drawer_name,
                    c.cheque_date,
                    b.presenting_bank_name,

                    lcs.status_history_id,

                    rr.return_reason,

                    lcs.remarks,

                    rh.status_history_ids

                FROM inward_cheque c

                JOIN inward_batch b
                    ON c.batch_id = b.batch_id

                JOIN latest_batch_status lbs
                    ON lbs.batch_id = c.batch_id

                JOIN latest_cheque_status lcs
                    ON lcs.cheque_number = c.cheque_number

                JOIN rejection_reasons rr
                    ON rr.cheque_number = c.cheque_number

                JOIN rejection_history rh
                    ON rh.cheque_number = c.cheque_number

                WHERE UPPER(TRIM(lbs.batch_status))
                        = 'COMPLETED'

                  AND UPPER(TRIM(lcs.status))
                        IN ('REJECT', 'REJECTED')

                  AND lcs.report_generated = 'N'

                ORDER BY
                    c.batch_id,
                    c.cheque_number
                """;

        List<Map<String, Object>> rrfList =
                new ArrayList<>();

        try (Connection connection =
                     dataSource.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql);

             ResultSet rs =
                     statement.executeQuery()) {

            while (rs.next()) {

                Map<String, Object> row =
                        new HashMap<>();

                row.put(
                        "statusHistoryId",
                        rs.getLong("status_history_id"));

                row.put(
                        "statusHistoryIds",
                        getStatusHistoryIds(
                                rs.getString(
                                        "status_history_ids")));

                row.put(
                        "batchId",
                        rs.getLong("batch_id"));

                row.put(
                        "chequeNo",
                        rs.getString("cheque_no"));

                row.put(
                        "amount",
                        rs.getBigDecimal("amount"));

                row.put(
                        "drawerAccountNo",
                        rs.getString("account_number"));

                row.put(
                        "payeeAccountNo",
                        rs.getString(
                                "payee_account_number"));

                row.put(
                        "payeeName",
                        rs.getString("payee_name"));

                row.put(
                        "drawerName",
                        rs.getString("drawer_name"));

                row.put(
                        "bankName",
                        rs.getString(
                                "presenting_bank_name"));

                row.put(
                        "chequeDate",
                        rs.getDate("cheque_date"));

                row.put(
                        "returnReason",
                        rs.getString("return_reason"));

                row.put(
                        "remark",
                        rs.getString("remarks"));

                rrfList.add(row);
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "DB Error fetching RRF data: "
                            + e.getMessage(),
                    e);
        }

        return rrfList;
    }

    @Override
    public List<Map<String, Object>> getApprovedReportData() {

        String sql = """
                WITH latest_batch_status AS (
                    SELECT
                        batch_id,
                        batch_status
                    FROM (
                        SELECT
                            batch_id,
                            batch_status,
                            ROW_NUMBER() OVER (
                                PARTITION BY batch_id
                                ORDER BY changed_on DESC,
                                         batch_history_id DESC
                            ) AS rn
                        FROM inward_batch_history
                    ) x
                    WHERE rn = 1
                ),

                latest_cheque_status AS (
                    SELECT
                        cheque_number,
                        status_history_id,
                        status,
                        report_generated
                    FROM (
                        SELECT
                            h.*,
                            ROW_NUMBER() OVER (
                                PARTITION BY cheque_number
                                ORDER BY status_history_id DESC
                            ) AS rn
                        FROM inward_cheque_status_history h
                    ) x
                    WHERE rn = 1
                )

                SELECT
                    c.cheque_number AS cheque_no,
                    c.batch_id,
                    c.amount,
                    c.account_number,
                    c.payee_account_number,
                    c.payee_name,
                    c.drawer_name,
                    c.cheque_date,
                    b.presenting_bank_name,

                    lcs.status_history_id

                FROM inward_cheque c

                JOIN inward_batch b
                    ON c.batch_id = b.batch_id

                JOIN latest_batch_status lbs
                    ON lbs.batch_id = c.batch_id

                JOIN latest_cheque_status lcs
                    ON lcs.cheque_number = c.cheque_number

                WHERE UPPER(TRIM(lbs.batch_status))
                        = 'COMPLETED'

                  AND UPPER(TRIM(lcs.status))
                        IN ('ACCEPT', 'ACCEPTED', 'APPROVED')

                  AND lcs.report_generated = 'N'

                ORDER BY
                    c.batch_id,
                    c.cheque_number
                """;

        List<Map<String, Object>> approvedList =
                new ArrayList<>();

        try (Connection connection =
                     dataSource.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql);

             ResultSet rs =
                     statement.executeQuery()) {

            while (rs.next()) {

                Map<String, Object> row =
                        new HashMap<>();

                row.put(
                        "statusHistoryId",
                        rs.getLong("status_history_id"));

                row.put(
                        "batchId",
                        rs.getLong("batch_id"));

                row.put(
                        "chequeNo",
                        rs.getString("cheque_no"));

                row.put(
                        "amount",
                        rs.getBigDecimal("amount"));

                row.put(
                        "accountNumber",
                        rs.getString("account_number"));

                row.put(
                        "payeeAccountNo",
                        rs.getString(
                                "payee_account_number"));

                row.put(
                        "payeeName",
                        rs.getString("payee_name"));

                row.put(
                        "drawerName",
                        rs.getString("drawer_name"));

                row.put(
                        "bankName",
                        rs.getString(
                                "presenting_bank_name"));

                row.put(
                        "chequeDate",
                        rs.getDate("cheque_date"));

                approvedList.add(row);
            }

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "DB Error fetching Approved data: "
                            + e.getMessage(),
                    e);
        }

        return approvedList;
    }

    @Override
    public void updateRrfReportGenerated(
            List<Long> statusHistoryIds) {

        updateReportGenerated(statusHistoryIds);
    }

    @Override
    public void updateApprovedReportGenerated(
            List<Long> statusHistoryIds) {

        updateReportGenerated(statusHistoryIds);
    }

    private void updateReportGenerated(
            List<Long> statusHistoryIds) {

        if (statusHistoryIds == null
                || statusHistoryIds.isEmpty()) {
            return;
        }

        String sql = """
                UPDATE inward_cheque_status_history
                SET report_generated = 'Y'
                WHERE status_history_id = ?
                  AND report_generated = 'N'
                """;

        try (Connection connection =
                     dataSource.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            for (Long statusHistoryId :
                    statusHistoryIds) {

                statement.setLong(
                        1,
                        statusHistoryId);

                statement.addBatch();
            }

            statement.executeBatch();

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "DB Error updating report status: "
                            + e.getMessage(),
                    e);
        }
    }

    private List<Long> getStatusHistoryIds(
            String statusHistoryIds) {

        List<Long> ids =
                new ArrayList<>();

        if (statusHistoryIds == null
                || statusHistoryIds.trim().isEmpty()) {

            return ids;
        }

        String[] values =
                statusHistoryIds.split(",");

        for (String value : values) {

            if (value != null
                    && !value.trim().isEmpty()) {

                ids.add(
                        Long.parseLong(
                                value.trim()));
            }
        }

        return ids;
    }
}