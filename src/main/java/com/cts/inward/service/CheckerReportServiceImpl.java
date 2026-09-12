package com.cts.inward.service;

import java.util.List;
import java.util.Map;

import com.cts.inward.dao.CheckerReportDao;
import com.cts.inward.dao.CheckerReportDaoImpl;

public class CheckerReportServiceImpl implements CheckerReportService {

    private final CheckerReportDao reportDao;

    private CheckerReportServiceImpl(CheckerReportDao reportDao) {
        this.reportDao = reportDao;
    }

    public static CheckerReportService of() {
        return new CheckerReportServiceImpl(CheckerReportDaoImpl.of());
    }

    @Override
    public List<Map<String, Object>> getRrfReportData() {
        return reportDao.getRrfReportData();
    }

    @Override
    public List<Map<String, Object>> getApprovedReportData() {
        return reportDao.getApprovedReportData();
    }

    @Override
    public String generateRrfXml() {
        List<Map<String, Object>> rrfData = reportDao.getRrfReportData();

        if (rrfData.isEmpty()) {
            return null;
        }

        StringBuilder xmlBuilder = new StringBuilder();

        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xmlBuilder.append("<RRFDocument>\n");
        xmlBuilder.append("    <Header>\n");
        xmlBuilder.append("        <FileDescription>Return Reason File (RRF) - CBS Failures</FileDescription>\n");
        xmlBuilder.append("        <TotalRecords>").append(rrfData.size()).append("</TotalRecords>\n");
        xmlBuilder.append("    </Header>\n");
        xmlBuilder.append("    <ReturnedCheques>\n");

        for (Map<String, Object> item : rrfData) {
            xmlBuilder.append("        <Cheque>\n");
            xmlBuilder.append("            <BatchID>").append(item.get("batchId")).append("</BatchID>\n");
            xmlBuilder.append("            <ChequeNumber>").append(item.get("chequeNo") != null ? item.get("chequeNo") : "").append("</ChequeNumber>\n");
            xmlBuilder.append("            <ChequeAmount>").append(item.get("amount") != null ? item.get("amount") : "").append("</ChequeAmount>\n");
            xmlBuilder.append("            <DrawerAccountNumber>").append(item.get("drawerAccountNo") != null ? item.get("drawerAccountNo") : "").append("</DrawerAccountNumber>\n");
            xmlBuilder.append("            <PayeeAccountNumber>").append(item.get("payeeAccountNo") != null ? item.get("payeeAccountNo") : "").append("</PayeeAccountNumber>\n");
            xmlBuilder.append("            <PayeeName>").append(item.get("payeeName") != null ? item.get("payeeName") : "").append("</PayeeName>\n");
            xmlBuilder.append("            <DrawerName>").append(item.get("drawerName") != null ? item.get("drawerName") : "").append("</DrawerName>\n");
            xmlBuilder.append("            <PresentingBank>").append(item.get("bankName") != null ? item.get("bankName") : "").append("</PresentingBank>\n");
            xmlBuilder.append("            <ChequeDate>").append(item.get("chequeDate") != null ? item.get("chequeDate") : "").append("</ChequeDate>\n");
            xmlBuilder.append("            <ReturnReason>").append(item.get("returnReason") != null ? item.get("returnReason") : "").append("</ReturnReason>\n");
            xmlBuilder.append("            <Remark>").append(item.get("remark") != null ? item.get("remark") : "").append("</Remark>\n");
            xmlBuilder.append("        </Cheque>\n");
        }

        xmlBuilder.append("    </ReturnedCheques>\n");
        xmlBuilder.append("</RRFDocument>");

        return xmlBuilder.toString();
    }

    @Override
    public String generateApprovedXml() {
        List<Map<String, Object>> approvedData = reportDao.getApprovedReportData();

        if (approvedData.isEmpty()) {
            return null;
        }

        StringBuilder xmlBuilder = new StringBuilder();

        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xmlBuilder.append("<ApprovedChequesDocument>\n");
        xmlBuilder.append("    <Header>\n");
        xmlBuilder.append("        <FileDescription>Approved Cheques Report</FileDescription>\n");
        xmlBuilder.append("        <TotalRecords>").append(approvedData.size()).append("</TotalRecords>\n");
        xmlBuilder.append("    </Header>\n");
        xmlBuilder.append("    <Cheques>\n");

        for (Map<String, Object> item : approvedData) {
            xmlBuilder.append("        <Cheque>\n");
            xmlBuilder.append("            <BatchID>").append(item.get("batchId")).append("</BatchID>\n");
            xmlBuilder.append("            <ChequeNumber>").append(item.get("chequeNo") != null ? item.get("chequeNo") : "").append("</ChequeNumber>\n");
            xmlBuilder.append("            <ChequeAmount>").append(item.get("amount") != null ? item.get("amount") : "").append("</ChequeAmount>\n");
            xmlBuilder.append("            <AccountNumber>").append(item.get("accountNumber") != null ? item.get("accountNumber") : "").append("</AccountNumber>\n");
            xmlBuilder.append("            <DrawerName>").append(item.get("drawerName") != null ? item.get("drawerName") : "").append("</DrawerName>\n");
            xmlBuilder.append("            <PayeeAccountNumber>").append(item.get("payeeAccountNo") != null ? item.get("payeeAccountNo") : "").append("</PayeeAccountNumber>\n");
            xmlBuilder.append("            <PayeeName>").append(item.get("payeeName") != null ? item.get("payeeName") : "").append("</PayeeName>\n");
            xmlBuilder.append("            <PresentingBank>").append(item.get("bankName") != null ? item.get("bankName") : "").append("</PresentingBank>\n");
            xmlBuilder.append("            <ChequeDate>").append(item.get("chequeDate") != null ? item.get("chequeDate") : "").append("</ChequeDate>\n");
            xmlBuilder.append("        </Cheque>\n");
        }

        xmlBuilder.append("    </Cheques>\n");
        xmlBuilder.append("</ApprovedChequesDocument>");

        return xmlBuilder.toString();
    }
}
