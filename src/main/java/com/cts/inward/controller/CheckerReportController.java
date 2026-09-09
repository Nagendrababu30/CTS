package com.cts.inward.controller;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import com.cts.inward.dao.CheckerReportDao;
import com.cts.inward.dao.CheckerReportDaoImpl;
import com.cts.inward.model.NpciBatchData;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

public class CheckerReportController extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    
    private Button generateRrfBtn;
    private Button generateApprovedBtn;
    private CheckerReportDao reportDao = CheckerReportDaoImpl.of();

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        initRrfEvent();
        initApprovedEvent();
    }

    
    private void initRrfEvent() {
        if (generateRrfBtn != null) {
            generateRrfBtn.addEventListener(Events.ON_CLICK, e -> {
                try {
                    // 1. Fetch expanded RRF data from database
                    List<Map<String, Object>> rrfData = reportDao.getRrfReportData();

                    if (rrfData.isEmpty()) {
                        Messagebox.show("No returned cheques found for RRF generation.", "Information", Messagebox.OK, Messagebox.EXCLAMATION);
                        return;
                    }

                    // 2. Build the detailed XML structure dynamically
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
                        xmlBuilder.append("            <ReturnReason>").append(item.get("returnReason") != null ? item.get("returnReason") : "CBS_FAILURE").append("</ReturnReason>\n");
                        xmlBuilder.append("            <Remark>").append(item.get("remark") != null ? item.get("remark") : "").append("</Remark>\n");
                        xmlBuilder.append("        </Cheque>\n");
                    }

                    xmlBuilder.append("    </ReturnedCheques>\n");
                    xmlBuilder.append("</RRFDocument>");

                    // 3. Trigger browser download as .xml file
                    byte[] xmlBytes = xmlBuilder.toString().getBytes(StandardCharsets.UTF_8);
                    String fileName = "RRF_Return_Report_" + System.currentTimeMillis() + ".xml";
                    Filedownload.save(xmlBytes, "application/xml", fileName);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    Messagebox.show("Error generating RRF XML: " + ex.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR);
                }
            });
        }
    }
    
    private void initApprovedEvent() {
        if (generateApprovedBtn != null) {
            generateApprovedBtn.addEventListener(Events.ON_CLICK, e -> {
                try {
                    List<Map<String, Object>> approvedData = reportDao.getApprovedReportData();

                    if (approvedData.isEmpty()) {
                        Messagebox.show("No approved cheques found for report generation.", "Information", Messagebox.OK, Messagebox.EXCLAMATION);
                        return;
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

                    byte[] xmlBytes = xmlBuilder.toString().getBytes(StandardCharsets.UTF_8);
                    String fileName = "Approved_Cheques_Report_" + System.currentTimeMillis() + ".xml";
                    Filedownload.save(xmlBytes, "application/xml", fileName);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    Messagebox.show("Error generating Approved XML: " + ex.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR);
                }
            });
        }
    }
}