
package com.cts.admin.report;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Executions;

import com.cts.admin.model.AuditLog;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

public class AuditLogReportService {

    private static final String REPORT_PATH =
            "/reports/admin_reports/audit_log_report.jrxml";

    public byte[] generateAuditLogPdf(
            List<AuditLog> auditLogs,
            String generatedDate,
            String fromDate,
            String toDate) throws Exception {

        if (auditLogs == null) {
            throw new IllegalArgumentException(
                    "Audit log list cannot be null.");
        }

        InputStream reportStream =
                Executions.getCurrent()
                        .getDesktop()
                        .getWebApp()
                        .getResourceAsStream(REPORT_PATH);

        if (reportStream == null) {
            throw new IllegalArgumentException(
                    "JRXML report file not found: "
                            + REPORT_PATH);
        }

        JasperReport jasperReport;

        try (InputStream inputStream = reportStream) {

            jasperReport =
                    JasperCompileManager.compileReport(inputStream);
        }

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("GENERATED_DATE", generatedDate);
        parameters.put("FROM_DATE", fromDate);
        parameters.put("TO_DATE", toDate);

        JRBeanCollectionDataSource dataSource =
                new JRBeanCollectionDataSource(auditLogs);

        JasperPrint jasperPrint =
                JasperFillManager.fillReport(
                        jasperReport,
                        parameters,
                        dataSource);

        return JasperExportManager.exportReportToPdf(
                jasperPrint);
    }
}