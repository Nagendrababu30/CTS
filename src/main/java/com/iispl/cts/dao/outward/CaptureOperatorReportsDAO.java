package com.iispl.cts.dao.outward;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.cts.inward.config.ConnectionPool;
import com.iispl.cts.model.outward.OutwardBatch;

public class CaptureOperatorReportsDAO {

    private final javax.sql.DataSource dataSource =
            ConnectionPool.getDataSource();

    // =========================================================
    // GET REPORT DATA
    // =========================================================

    public List<OutwardBatch> getReportData(
            long operatorId,
            Date fromDate,
            Date toDate) {

        List<OutwardBatch> batches =
                new ArrayList<>();

        StringBuilder sql =
                new StringBuilder();

        sql.append(
                "SELECT "
                + "batch_number, "
                + "branch_code, "
                + "cheque_count, "
                + "batch_folder_path, "
                + "created_by, "
                + "created_at, "
                + "batch_status "
                + "FROM outward_batch "
                + "WHERE created_by = ? "
        );

        // -----------------------------------------------------
        // FROM DATE
        // -----------------------------------------------------

        if (fromDate != null) {

            sql.append(
                    "AND created_at >= ? "
            );
        }

        // -----------------------------------------------------
        // TO DATE
        // -----------------------------------------------------

        if (toDate != null) {

            /*
             * Use the beginning of the next day.
             *
             * Example:
             * To Date = 20/09/2026
             *
             * Condition:
             * created_at < 21/09/2026 00:00:00
             *
             * Therefore the complete 20/09/2026 is included.
             */

            sql.append(
                    "AND created_at < ? "
            );
        }

        sql.append(
                "ORDER BY created_at DESC, batch_number"
        );

        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(
                                sql.toString()
                        )
        ) {

            int parameterIndex = 1;

            // -------------------------------------------------
            // CURRENT CAPTURE OPERATOR
            // -------------------------------------------------

            ps.setLong(
                    parameterIndex++,
                    operatorId
            );

            // -------------------------------------------------
            // FROM DATE
            // -------------------------------------------------

            if (fromDate != null) {

                ps.setTimestamp(
                        parameterIndex++,
                        getStartOfDay(fromDate)
                );
            }

            // -------------------------------------------------
            // TO DATE
            // -------------------------------------------------

            if (toDate != null) {

                ps.setTimestamp(
                        parameterIndex++,
                        getStartOfNextDay(toDate)
                );
            }

            try (
                    ResultSet rs =
                            ps.executeQuery()
            ) {

                while (rs.next()) {

                    OutwardBatch batch =
                            new OutwardBatch();

                    // -----------------------------------------
                    // BATCH NUMBER
                    // -----------------------------------------

                    batch.setBatchNumber(
                            rs.getString(
                                    "batch_number"
                            )
                    );

                    // -----------------------------------------
                    // BRANCH CODE
                    // -----------------------------------------

                    batch.setBranchCode(
                            rs.getString(
                                    "branch_code"
                            )
                    );

                    // -----------------------------------------
                    // CHEQUE COUNT
                    // -----------------------------------------

                    batch.setNumberOfCheques(
                            rs.getInt(
                                    "cheque_count"
                            )
                    );

                    // -----------------------------------------
                    // BATCH FOLDER
                    // -----------------------------------------

                    batch.setBatchFolderPath(
                            rs.getString(
                                    "batch_folder_path"
                            )
                    );

                    // -----------------------------------------
                    // CREATED BY
                    // -----------------------------------------

                    batch.setCreatedBy(
                            String.valueOf(
                                    rs.getInt(
                                            "created_by"
                                    )
                            )
                    );

                    // -----------------------------------------
                    // CREATED AT
                    // -----------------------------------------

                    Timestamp timestamp =
                            rs.getTimestamp(
                                    "created_at"
                            );

                    if (timestamp != null) {

                        batch.setCreatedAt(
                                timestamp.toLocalDateTime()
                        );
                    }

                    // -----------------------------------------
                    // BATCH STATUS
                    // -----------------------------------------

                    batch.setBatchStatus(
                            rs.getString(
                                    "batch_status"
                            )
                    );

                    batches.add(batch);
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to load capture operator report data.",
                    e
            );
        }

        return batches;
    }


    // =========================================================
    // SAVE DOWNLOAD HISTORY
    // =========================================================

    public void saveDownloadHistory(
            long operatorId,
            Date fromDate,
            Date toDate,
            String format) {

        String sql =
                "INSERT INTO capture_operator_download_history "
                + "(operator_id, download_date, from_date, "
                + "to_date, format) "
                + "VALUES (?, CURRENT_TIMESTAMP, ?, ?, ?)";

        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            // -------------------------------------------------
            // CURRENT CAPTURE OPERATOR
            // -------------------------------------------------

            ps.setLong(
                    1,
                    operatorId
            );

            // -------------------------------------------------
            // FROM DATE
            // -------------------------------------------------

            if (fromDate != null) {

                ps.setDate(
                        2,
                        new java.sql.Date(
                                fromDate.getTime()
                        )
                );

            } else {

                ps.setNull(
                        2,
                        java.sql.Types.DATE
                );
            }

            // -------------------------------------------------
            // TO DATE
            // -------------------------------------------------

            if (toDate != null) {

                ps.setDate(
                        3,
                        new java.sql.Date(
                                toDate.getTime()
                        )
                );

            } else {

                ps.setNull(
                        3,
                        java.sql.Types.DATE
                );
            }

            // -------------------------------------------------
            // FORMAT
            // -------------------------------------------------

            ps.setString(
                    4,
                    format
            );

            ps.executeUpdate();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to save download history.",
                    e
            );
        }
    }


    // =========================================================
    // GET DOWNLOAD HISTORY
    // =========================================================

    public List<Object[]> getDownloadHistory(
            long operatorId) {

        List<Object[]> history =
                new ArrayList<>();

        String sql =
                "SELECT "
                + "download_date, "
                + "from_date, "
                + "to_date, "
                + "format "
                + "FROM capture_operator_download_history "
                + "WHERE operator_id = ? "
                + "ORDER BY download_date DESC";

        try (
                Connection connection =
                        dataSource.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            // -------------------------------------------------
            // CURRENT CAPTURE OPERATOR
            // -------------------------------------------------

            ps.setLong(
                    1,
                    operatorId
            );

            try (
                    ResultSet rs =
                            ps.executeQuery()
            ) {

                while (rs.next()) {

                    Object[] row =
                            new Object[4];

                    // -----------------------------------------
                    // DOWNLOAD DATE
                    // -----------------------------------------

                    row[0] =
                            rs.getTimestamp(
                                    "download_date"
                            );

                    // -----------------------------------------
                    // FROM DATE
                    // -----------------------------------------

                    row[1] =
                            rs.getDate(
                                    "from_date"
                            );

                    // -----------------------------------------
                    // TO DATE
                    // -----------------------------------------

                    row[2] =
                            rs.getDate(
                                    "to_date"
                            );

                    // -----------------------------------------
                    // FORMAT
                    // -----------------------------------------

                    row[3] =
                            rs.getString(
                                    "format"
                            );

                    history.add(row);
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to load download history.",
                    e
            );
        }

        return history;
    }


    // =========================================================
    // START OF DAY
    // =========================================================

    private Timestamp getStartOfDay(
            Date date) {

        Calendar calendar =
                Calendar.getInstance();

        calendar.setTime(date);

        calendar.set(
                Calendar.HOUR_OF_DAY,
                0
        );

        calendar.set(
                Calendar.MINUTE,
                0
        );

        calendar.set(
                Calendar.SECOND,
                0
        );

        calendar.set(
                Calendar.MILLISECOND,
                0
        );

        return new Timestamp(
                calendar.getTimeInMillis()
        );
    }


    // =========================================================
    // START OF NEXT DAY
    // =========================================================

    private Timestamp getStartOfNextDay(
            Date date) {

        Calendar calendar =
                Calendar.getInstance();

        calendar.setTime(date);

        calendar.set(
                Calendar.HOUR_OF_DAY,
                0
        );

        calendar.set(
                Calendar.MINUTE,
                0
        );

        calendar.set(
                Calendar.SECOND,
                0
        );

        calendar.set(
                Calendar.MILLISECOND,
                0
        );

        calendar.add(
                Calendar.DAY_OF_MONTH,
                1
        );

        return new Timestamp(
                calendar.getTimeInMillis()
        );
    }
}