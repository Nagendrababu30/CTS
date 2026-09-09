package com.cts.inward.controller;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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

    private Rows batchRows; 
    private CheckerReportDao reportDao = CheckerReportDaoImpl.of();

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        loadReportData();
    }

    private void loadReportData() {
        try {
            batchRows.getChildren().clear();

            List<NpciBatchData> completedBatches = reportDao.getCompletedBatches();
            
            for (NpciBatchData batch : completedBatches) {
                Row row = new Row();
                
                String formattedBatchId = String.format("BATCH%03d", batch.getBatchId());
                Label idLbl = new Label(formattedBatchId);
                idLbl.setSclass("report-batch-id"); 
                row.appendChild(idLbl);
               
                row.appendChild(new Label(String.valueOf(batch.getTotalCheques())));

                Button actionBtn = new Button("Generate Report");
                actionBtn.setSclass("btn-generate-report");
                
                actionBtn.addEventListener(Events.ON_CLICK, e -> {
                    try {
                        // 1. Fetch exact metrics (Approved and Rejected counts) from DB
                        Map<String, Object> reportParams = reportDao.getCheckerReportDetails(batch.getBatchId());
                        
                        

                        // 2. Load Jasper template from WEB-INF/reports/checker_batch_summary.jrxml
                        InputStream jrxmlStream = Sessions.getCurrent().getWebApp()
                                                  .getResourceAsStream("/WEB-INF/reports/checker_batch_summary.jrxml");
                        
                        if (jrxmlStream == null) {
                            Messagebox.show("Report template not found in WEB-INF/reports/", "Error", Messagebox.OK, Messagebox.ERROR);
                            return;
                        }

                        // 3. Compile and Fill the Jasper Report
                        JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);
                        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, reportParams, new JREmptyDataSource());

                        // 4. Export to PDF stream
                        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
                        JasperExportManager.exportReportToPdfStream(jasperPrint, pdfOutputStream);

                        // 5. Trigger browser download
                        String fileName = formattedBatchId + "_Checker_Summary.pdf";
                        Filedownload.save(pdfOutputStream.toByteArray(), "application/pdf", fileName);

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Messagebox.show("Error generating report: " + ex.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR);
                    }
                });

                row.appendChild(actionBtn);
                batchRows.appendChild(row);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Messagebox.show("Error loading report data: " + e.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR);
        }
    }
}