package com.cts.inward.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.zkoss.zk.ui.Executions;

import com.cts.inward.dao.CheckerReportDao;
import com.cts.inward.dao.CheckerReportDaoImpl;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

public class CheckerReportServiceImpl implements CheckerReportService {

    private final CheckerReportDao reportDao;

    private CheckerReportServiceImpl(CheckerReportDao reportDao) {
        this.reportDao = reportDao;
    }

    public static CheckerReportService of() {
        return new CheckerReportServiceImpl(
                CheckerReportDaoImpl.of());
    }

    @Override
    public byte[] generateRrfXml() {

        List<Map<String, Object>> rrfData =
                reportDao.getRrfReportData();

        if (rrfData == null || rrfData.isEmpty()) {
            return null;
        }

        try {
            InputStream inputStream =
                    Executions.getCurrent()
                            .getDesktop()
                            .getWebApp()
                            .getResourceAsStream(
                                    "/WEB-INF/reports/rrf_report.jrxml");

            if (inputStream == null) {
                throw new RuntimeException(
                        "RRF JRXML file not found: "
                                + "/WEB-INF/reports/rrf_report.jrxml");
            }

            JasperReport jasperReport =
                    JasperCompileManager.compileReport(inputStream);

            JRDataSource dataSource =
                    new JRMapCollectionDataSource(
                            new ArrayList<Map<String, ?>>(rrfData));

            Map<String, Object> parameters =
                    new HashMap<>();

            parameters.put(
                    "FILE_DESCRIPTION",
                    "Return Reason File (RRF) - CBS Failures");

            parameters.put(
                    "TOTAL_RECORDS",
                    rrfData.size());

            /*
             * Jasper is used to compile and fill the report.
             */
            JasperPrint jasperPrint =
                    JasperFillManager.fillReport(
                            jasperReport,
                            parameters,
                            dataSource);

            /*
             * Build the required business XML.
             */
            byte[] xmlBytes =
                    buildRrfXml(rrfData);

            /*
             * Update only after successful XML generation.
             */
            List<Long> statusHistoryIds =
                    extractStatusHistoryIds(rrfData);

            reportDao.updateRrfReportGenerated(
                    statusHistoryIds);

            return xmlBytes;

        } catch (Exception e) {
            e.printStackTrace();

            throw new RuntimeException(
                    "Error generating RRF Jasper XML: "
                            + e.getMessage(),
                    e);
        }
    }

    @Override
    public byte[] generateApprovedXml() {

        List<Map<String, Object>> approvedData =
                reportDao.getApprovedReportData();

        if (approvedData == null || approvedData.isEmpty()) {
            return null;
        }

        try {
            InputStream inputStream =
                    Executions.getCurrent()
                            .getDesktop()
                            .getWebApp()
                            .getResourceAsStream(
                                    "/WEB-INF/reports/approved_report.jrxml");

            if (inputStream == null) {
                throw new RuntimeException(
                        "Approved JRXML file not found: "
                                + "/WEB-INF/reports/approved_report.jrxml");
            }

            JasperReport jasperReport =
                    JasperCompileManager.compileReport(inputStream);

            JRDataSource dataSource =
                    new JRMapCollectionDataSource(
                            new ArrayList<Map<String, ?>>(approvedData));

            Map<String, Object> parameters =
                    new HashMap<>();

            parameters.put(
                    "FILE_DESCRIPTION",
                    "Approved Cheques Report");

            parameters.put(
                    "TOTAL_RECORDS",
                    approvedData.size());

            /*
             * Jasper is used to compile and fill the report.
             */
            JasperPrint jasperPrint =
                    JasperFillManager.fillReport(
                            jasperReport,
                            parameters,
                            dataSource);

            /*
             * Build the required business XML.
             */
            byte[] xmlBytes =
                    buildApprovedXml(approvedData);

            /*
             * Update only after successful XML generation.
             */
            List<Long> statusHistoryIds =
                    extractStatusHistoryIds(approvedData);

            reportDao.updateApprovedReportGenerated(
                    statusHistoryIds);

            return xmlBytes;

        } catch (Exception e) {
            e.printStackTrace();

            throw new RuntimeException(
                    "Error generating Approved Jasper XML: "
                            + e.getMessage(),
                    e);
        }
    }

    private byte[] buildRrfXml(
            List<Map<String, Object>> data) throws Exception {

        Document document =
                DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .newDocument();

        Element root =
                document.createElement("cheques");

        document.appendChild(root);

        for (Map<String, Object> row : data) {

            Element cheque =
                    document.createElement("cheque");

            root.appendChild(cheque);

            addElement(
                    document,
                    cheque,
                    "batch_id",
                    row.get("batchId"));

            addElement(
                    document,
                    cheque,
                    "cheque_number",
                    row.get("chequeNo"));

            addElement(
                    document,
                    cheque,
                    "cheque_amount",
                    row.get("amount"));

            addElement(
                    document,
                    cheque,
                    "drawer_account_number",
                    row.get("drawerAccountNo"));

            addElement(
                    document,
                    cheque,
                    "payee_account_number",
                    row.get("payeeAccountNo"));

            addElement(
                    document,
                    cheque,
                    "payee_name",
                    row.get("payeeName"));

            addElement(
                    document,
                    cheque,
                    "drawer_name",
                    row.get("drawerName"));

            addElement(
                    document,
                    cheque,
                    "presenting_bank_name",
                    row.get("bankName"));

            addElement(
                    document,
                    cheque,
                    "cheque_date",
                    row.get("chequeDate"));

            addElement(
                    document,
                    cheque,
                    "rejection_reason_code",
                    row.get("returnReason"));

            addElement(
                    document,
                    cheque,
                    "remarks",
                    row.get("remarks"));
        }

        return convertDocumentToBytes(document);
    }

    private byte[] buildApprovedXml(
            List<Map<String, Object>> data) throws Exception {

        Document document =
                DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .newDocument();

        Element root =
                document.createElement("cheques");

        document.appendChild(root);

        for (Map<String, Object> row : data) {

            Element cheque =
                    document.createElement("cheque");

            root.appendChild(cheque);

            addElement(
                    document,
                    cheque,
                    "batch_id",
                    row.get("batchId"));

            addElement(
                    document,
                    cheque,
                    "cheque_number",
                    row.get("chequeNo"));

            addElement(
                    document,
                    cheque,
                    "cheque_amount",
                    row.get("amount"));

            addElement(
                    document,
                    cheque,
                    "account_number",
                    row.get("accountNumber"));

            addElement(
                    document,
                    cheque,
                    "payee_account_number",
                    row.get("payeeAccountNo"));

            addElement(
                    document,
                    cheque,
                    "payee_name",
                    row.get("payeeName"));

            addElement(
                    document,
                    cheque,
                    "drawer_name",
                    row.get("drawerName"));

            addElement(
                    document,
                    cheque,
                    "presenting_bank_name",
                    row.get("bankName"));

            addElement(
                    document,
                    cheque,
                    "cheque_date",
                    row.get("chequeDate"));
        }

        return convertDocumentToBytes(document);
    }

    private void addElement(
            Document document,
            Element parent,
            String tagName,
            Object value) {

        Element element =
                document.createElement(tagName);

        if (value != null) {
            element.setTextContent(
                    String.valueOf(value));
        }

        parent.appendChild(element);
    }

    private byte[] convertDocumentToBytes(
            Document document) throws Exception {

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        Transformer transformer =
                TransformerFactory
                        .newInstance()
                        .newTransformer();

        transformer.setOutputProperty(
                OutputKeys.INDENT,
                "yes");

        transformer.setOutputProperty(
                OutputKeys.ENCODING,
                "UTF-8");

        transformer.setOutputProperty(
                "{http://xml.apache.org/xslt}indent-amount",
                "4");

        transformer.transform(
                new DOMSource(document),
                new StreamResult(outputStream));

        return outputStream.toByteArray();
    }

    private List<Long> extractStatusHistoryIds(
            List<Map<String, Object>> data) {

        List<Long> statusHistoryIds =
                new ArrayList<>();

        for (Map<String, Object> item : data) {

            Object value =
                    item.get("statusHistoryId");

            if (value instanceof Number) {

                statusHistoryIds.add(
                        ((Number) value).longValue());
            }
        }

        return statusHistoryIds;
    }
}