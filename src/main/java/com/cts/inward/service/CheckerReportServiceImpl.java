
package com.cts.inward.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cts.inward.dao.CheckerReportDao;
import com.cts.inward.dao.CheckerReportDaoImpl;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.export.JRXmlExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleXmlExporterOutput;


public class CheckerReportServiceImpl implements CheckerReportService {

    private final CheckerReportDao reportDao;

    private CheckerReportServiceImpl(CheckerReportDao reportDao) {
        this.reportDao = reportDao;
    }

    public static CheckerReportService of() {
        return new CheckerReportServiceImpl(CheckerReportDaoImpl.of());
    }

    @Override
    public byte[] generateRrfXml() {
        List<Map<String, Object>> rrfData = reportDao.getRrfReportData();

        if (rrfData == null || rrfData.isEmpty()) {
            return null;
        }

        try {
            InputStream inputStream = getClass().getResourceAsStream("/reports/rrf_report.jrxml");

            if (inputStream == null) {
                throw new RuntimeException("RRF JRXML file not found: /reports/rrf_report.jrxml");
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(inputStream);
            JRDataSource dataSource =new JRMapCollectionDataSource( new ArrayList<Map<String, ?>>(rrfData));
            Map<String, Object> parameters = new HashMap<>();

            parameters.put("FILE_DESCRIPTION", "Return Reason File (RRF) - CBS Failures");
            parameters.put("TOTAL_RECORDS", rrfData.size());

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            byte[] xmlBytes = exportToXml(jasperPrint);

            List<Long> statusHistoryIds = extractStatusHistoryIds(rrfData);
            reportDao.updateRrfReportGenerated(statusHistoryIds);

            return xmlBytes;

        } catch (Exception e) {
            throw new RuntimeException("Error generating RRF Jasper XML", e);
        }
    }

    @Override
    public byte[] generateApprovedXml() {
        List<Map<String, Object>> approvedData = reportDao.getApprovedReportData();

        if (approvedData == null || approvedData.isEmpty()) {
            return null;
        }

        try {
            InputStream inputStream = getClass().getResourceAsStream("/reports/approved_report.jrxml");

            if (inputStream == null) {
                throw new RuntimeException("Approved JRXML file not found: /reports/approved_report.jrxml");
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(inputStream);
            JRDataSource dataSource = new JRMapCollectionDataSource( new ArrayList<Map<String, ?>>(approvedData));
            Map<String, Object> parameters = new HashMap<>();

            parameters.put("FILE_DESCRIPTION", "Approved Cheques Report");
            parameters.put("TOTAL_RECORDS", approvedData.size());

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            byte[] xmlBytes = exportToXml(jasperPrint);

            List<Long> statusHistoryIds = extractStatusHistoryIds(approvedData);
            reportDao.updateApprovedReportGenerated(statusHistoryIds);

            return xmlBytes;

        } catch (Exception e) {
            throw new RuntimeException("Error generating Approved Jasper XML", e);
        }
    }

    private byte[] exportToXml(JasperPrint jasperPrint) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JRXmlExporter exporter = new JRXmlExporter();

        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleXmlExporterOutput(outputStream));
        exporter.exportReport();

        return outputStream.toByteArray();
    }

    private List<Long> extractStatusHistoryIds(List<Map<String, Object>> data) {
        List<Long> statusHistoryIds = new ArrayList<>();

        for (Map<String, Object> item : data) {
            Object value = item.get("statusHistoryId");

            if (value instanceof Number) {
                statusHistoryIds.add(((Number) value).longValue());
            }
        }

        return statusHistoryIds;
    }
}

