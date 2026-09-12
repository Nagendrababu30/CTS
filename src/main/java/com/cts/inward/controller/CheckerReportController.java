package com.cts.inward.controller;

import java.nio.charset.StandardCharsets;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Messagebox;
import com.cts.inward.service.CheckerReportService;
import com.cts.inward.service.CheckerReportServiceImpl;

public class CheckerReportController extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;
    private Button generateRrfBtn;
    private Button generateApprovedBtn;
    private CheckerReportService reportService = CheckerReportServiceImpl.of();

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        initRrfEvent();
        initApprovedEvent();
    }

    private void initRrfEvent() {
        if (generateRrfBtn != null) {
            generateRrfBtn.addEventListener(Events.ON_CLICK, event -> {
                try {
                    String xml = reportService.generateRrfXml();
                    if (xml == null) {
                        Messagebox.show("No returned cheques found for RRF generation.", "Information", Messagebox.OK, Messagebox.EXCLAMATION);
                        return;
                    }
                    byte[] xmlBytes = xml.getBytes(StandardCharsets.UTF_8);
                    String fileName = "RRF_Return_Report_" + System.currentTimeMillis() + ".xml";
                    Filedownload.save(xmlBytes, "application/xml", fileName);
                } catch (Exception e) {
                    e.printStackTrace();
                    Messagebox.show("Error generating RRF XML: " + e.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR);
                }
            });
        }
    }

    private void initApprovedEvent() {
        if (generateApprovedBtn != null) {
            generateApprovedBtn.addEventListener(Events.ON_CLICK, event -> {
                try {
                    String xml = reportService.generateApprovedXml();
                    if (xml == null) {
                        Messagebox.show("No approved cheques found for report generation.", "Information", Messagebox.OK, Messagebox.EXCLAMATION);
                        return;
                    }
                    byte[] xmlBytes = xml.getBytes(StandardCharsets.UTF_8);
                    String fileName = "Approved_Cheques_Report_" + System.currentTimeMillis() + ".xml";
                    Filedownload.save(xmlBytes, "application/xml", fileName);
                } catch (Exception e) {
                    e.printStackTrace();
                    Messagebox.show("Error generating Approved XML: " + e.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR);
                }
            });
        }
    }
}