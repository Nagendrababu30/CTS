package com.cts.inward.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.cts.inward.config.ConnectionPool;

public class BatchDetailsDaoImpl implements BatchDetailsDao {

    private final javax.sql.DataSource dataSource;

    private BatchDetailsDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static BatchDetailsDao of() {
        return new BatchDetailsDaoImpl(
                ConnectionPool.getDataSource()
        );
    }

    @Override
    public Map<String, Object> getMicrDetails(String chequeNumber) {

        String sql = """
                SELECT
                    c.cheque_number,
                    c.micr_code AS current_micr,
                    r.old_value AS old_micr,
                    r.new_value AS corrected_micr
                FROM inward_cheque c
                LEFT JOIN inward_micr_repair_history r
                    ON c.cheque_number = r.cheque_no
                WHERE c.cheque_number = ?
                """;

        Map<String, Object> micrDetails = new HashMap<>();

        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, chequeNumber);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {

                    micrDetails.put(
                            "chequeNumber",
                            resultSet.getString("cheque_number")
                    );

                    micrDetails.put(
                            "currentMicr",
                            resultSet.getString("current_micr")
                    );

                    micrDetails.put(
                            "oldMicr",
                            resultSet.getString("old_micr")
                    );

                    micrDetails.put(
                            "correctedMicr",
                            resultSet.getString("corrected_micr")
                    );
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error retrieving MICR details for cheque: "
                    + chequeNumber,
                    e
            );
        }

        return micrDetails;
    }
    
    @Override
    public List<Map<String, Object>> getChequesByBatchId(String batchId) {

        String sql = """
                SELECT
                    cheque_number,
                    batch_id,
                    account_number,
                    drawer_name,
                    amount,
                    micr_code,
                    cheque_date
                FROM inward_cheque
                WHERE batch_id = ?
                ORDER BY cheque_number
                """;

        List<Map<String, Object>> cheques = new java.util.ArrayList<>();

        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setLong(1, Long.parseLong(batchId));

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {

                    Map<String, Object> cheque =
                            new HashMap<>();

                    cheque.put(
                            "chequeNumber",
                            resultSet.getString("cheque_number")
                    );

                    cheque.put(
                            "batchId",
                            resultSet.getLong("batch_id")
                    );

                    cheque.put(
                            "accountNumber",
                            resultSet.getString("account_number")
                    );

                    cheque.put(
                            "drawerName",
                            resultSet.getString("drawer_name")
                    );

                    cheque.put(
                            "amount",
                            resultSet.getBigDecimal("amount")
                    );

                    cheque.put(
                            "micrCode",
                            resultSet.getString("micr_code")
                    );

                    cheque.put(
                            "chequeDate",
                            resultSet.getDate("cheque_date")
                    );

                    cheques.add(cheque);
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error retrieving cheques for batch: "
                    + batchId,
                    e
            );
        }

        return cheques;
    }
    
    
    @Override
    public Map<String, Object> getDataEntryDetails(String chequeNumber) {

        Map<String, Object> details = new HashMap<>();

        String chequeSql = """
            SELECT
                cheque_number,
                account_number,
                amount,
                cheque_date,
                drawer_name
            FROM inward_cheque
            WHERE cheque_number = ?
            """;

        String historySql = """
            SELECT
                field_name,
                new_value
            FROM inward_cheque_dataentry_history
            WHERE cheque_no = ?
            ORDER BY history_id
            """;

        try (
            Connection connection = dataSource.getConnection()
        ) {

            // =====================================================
            // 1. GET OLD VALUES FROM inward_cheque
            // =====================================================

            try (
                PreparedStatement statement =
                        connection.prepareStatement(chequeSql)
            ) {

                statement.setString(1, chequeNumber);

                try (
                    ResultSet rs = statement.executeQuery()
                ) {

                    if (rs.next()) {

                        details.put(
                                "chequeNumber",
                                rs.getString("cheque_number")
                        );

                        details.put(
                                "oldAccountNumber",
                                rs.getString("account_number")
                        );

                        details.put(
                                "oldAmount",
                                rs.getString("amount")
                        );

                        details.put(
                                "oldChequeDate",
                                rs.getString("cheque_date")
                        );

                        details.put(
                                "oldDrawerName",
                                rs.getString("drawer_name")
                        );
                    }
                }
            }


            // =====================================================
            // 2. GET NEW VALUES FROM DATA ENTRY HISTORY
            // =====================================================

            try (
                PreparedStatement statement =
                        connection.prepareStatement(historySql)
            ) {

                statement.setString(1, chequeNumber);

                try (
                    ResultSet rs = statement.executeQuery()
                ) {

                    while (rs.next()) {

                        String fieldName =
                                rs.getString("field_name");

                        String newValue =
                                rs.getString("new_value");


                        if ("ACCOUNT_NUMBER".equals(fieldName)) {

                            details.put(
                                    "newAccountNumber",
                                    newValue
                            );

                        } else if ("AMOUNT".equals(fieldName)) {

                            details.put(
                                    "newAmount",
                                    newValue
                            );

                        } else if ("CHEQUE_DATE".equals(fieldName)) {

                            details.put(
                                    "newChequeDate",
                                    newValue
                            );

                        } else if ("DRAWER_NAME".equals(fieldName)) {

                            details.put(
                                    "newDrawerName",
                                    newValue
                            );
                        }
                    }
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error retrieving Data Entry details for cheque: "
                            + chequeNumber,
                    e
            );
        }

        return details;
    }
    
    @Override
    public Map<String, Object> getCbsValidation(String chequeNumber) {

        String sql = """
            SELECT
                c.cheque_number,
                c.account_number,
                c.cheque_date,

                COALESCE(
                    (
                        SELECT h.new_value
                        FROM inward_cheque_dataentry_history h
                        WHERE h.cheque_no = c.cheque_number
                          AND h.field_name = 'AMOUNT'
                        ORDER BY h.history_id DESC
                        LIMIT 1
                    ),
                    CAST(c.amount AS VARCHAR)
                ) AS cheque_amount,

                a.account_number AS master_account_number,
                a.account_status,
                a.available_balance

            FROM inward_cheque c

            LEFT JOIN account_master a
                ON a.account_number = c.account_number

            WHERE c.cheque_number = ?
            """;

        Map<String, Object> cbsDetails = new HashMap<>();

        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {

            statement.setString(1, chequeNumber);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {

                    cbsDetails.put(
                            "chequeNumber",
                            resultSet.getString("cheque_number")
                    );

                    cbsDetails.put(
                            "accountNumber",
                            resultSet.getString("account_number")
                    );

                    cbsDetails.put(
                            "chequeDate",
                            resultSet.getDate("cheque_date")
                    );

                    cbsDetails.put(
                            "chequeAmount",
                            resultSet.getBigDecimal("cheque_amount")
                    );

                    cbsDetails.put(
                            "accountStatus",
                            resultSet.getString("account_status")
                    );

                    cbsDetails.put(
                            "availableBalance",
                            resultSet.getBigDecimal("available_balance")
                    );
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error retrieving CBS validation details for cheque: "
                            + chequeNumber,
                    e
            );
        }

        return cbsDetails;
    }
    
}