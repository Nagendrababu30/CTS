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
import com.cts.inward.model.NpciBatchData;

public class CheckerReportDaoImpl implements CheckerReportDao {

    private final DataSource dataSource;

    private CheckerReportDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static CheckerReportDao of() {
        return new CheckerReportDaoImpl(ConnectionPool.getDataSource());
    }

   
    @Override
    public List<Map<String, Object>> getRrfReportData() {
        String sql = """
                SELECT 
                    c.cheque_number as cheque_no,
                    c.batch_id,
                    c.amount,
                    c.account_number,
                    c.payee_account_number,
                    c.payee_name,
                    c.drawer_name,
                    c.cheque_date,
                    b.presenting_bank_name,
                    (
                        SELECT h.return_reason_code 
                        FROM inward_cheque_status_history h 
                        WHERE h.cheque_number = c.cheque_number 
                        ORDER BY h.status_history_id DESC 
                        LIMIT 1
                    ) as return_reason,
                    (
                        SELECT h.remarks 
                        FROM inward_cheque_status_history h 
                        WHERE h.cheque_number = c.cheque_number 
                        ORDER BY h.status_history_id DESC 
                        LIMIT 1
                    ) as remark
                FROM inward_cheque c
                JOIN inward_batch b ON c.batch_id = b.batch_id
                WHERE (
                    SELECT UPPER(TRIM(h.status))
                    FROM inward_cheque_status_history h 
                    WHERE h.cheque_number = c.cheque_number 
                    ORDER BY h.status_history_id DESC 
                    LIMIT 1
                ) = 'REJECT'
                ORDER BY c.batch_id, c.cheque_number
                """;

        List<Map<String, Object>> rrfList = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
             
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("batchId", String.format("BATCH%03d", rs.getLong("batch_id")));
                row.put("chequeNo", rs.getString("cheque_no"));
                row.put("amount", rs.getBigDecimal("amount"));
                row.put("drawerAccountNo", rs.getString("account_number"));
                row.put("payeeAccountNo", rs.getString("payee_account_number"));
                row.put("payeeName", rs.getString("payee_name"));         
                row.put("drawerName", rs.getString("drawer_name"));
                row.put("bankName", rs.getString("presenting_bank_name"));
                row.put("chequeDate", rs.getDate("cheque_date")); 
                row.put("returnReason", rs.getString("return_reason"));
                row.put("remark", rs.getString("remark"));
                
                rrfList.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("DB Error fetching RRF data: " + e.getMessage(), e);
        }
        return rrfList;
    }
    
    @Override
    public List<Map<String, Object>> getApprovedReportData() {
        String sql = """
                SELECT 
                    c.cheque_number as cheque_no,
                    c.batch_id,
                    c.amount,
                    c.account_number,
                    c.payee_account_number,
                    c.payee_name,
                    c.drawer_name,
                    c.cheque_date,
                    b.presenting_bank_name
                FROM inward_cheque c
                JOIN inward_batch b ON c.batch_id = b.batch_id
                WHERE (
                    SELECT UPPER(TRIM(h.status))
                    FROM inward_cheque_status_history h 
                    WHERE h.cheque_number = c.cheque_number 
                    ORDER BY h.status_history_id DESC 
                    LIMIT 1
                ) = 'ACCEPT'
                ORDER BY c.batch_id, c.cheque_number
                """;

        List<Map<String, Object>> approvedList = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
             
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("batchId", String.format("BATCH%03d", rs.getLong("batch_id")));
                row.put("chequeNo", rs.getString("cheque_no"));
                row.put("amount", rs.getBigDecimal("amount"));
                row.put("accountNumber", rs.getString("account_number"));
                row.put("payeeAccountNo", rs.getString("payee_account_number"));
                row.put("payeeName", rs.getString("payee_name"));        
                row.put("drawerName", rs.getString("drawer_name"));
                row.put("bankName", rs.getString("presenting_bank_name"));
                row.put("chequeDate", rs.getDate("cheque_date"));
                approvedList.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("DB Error fetching Approved data: " + e.getMessage(), e);
        }
        return approvedList;
    }
}