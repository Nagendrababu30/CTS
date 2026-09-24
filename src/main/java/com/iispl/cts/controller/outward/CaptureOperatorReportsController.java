package com.iispl.cts.controller.outward;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.zkoss.util.media.AMedia;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;

import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.service.outward.CaptureOperatorReportsService;

public class CaptureOperatorReportsController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    @Wire
    private Datebox fromDate;

    @Wire
    private Datebox toDate;

    @Wire
    private Combobox formatCombo;

    @Wire
    private Button viewDataButton;

    @Wire
    private Button exportButton;

    @Wire
    private Listbox downloadHistoryList;

    @Wire
    private Paging historyPaging;

    @Wire
    private Label historyCountLabel;

    private long currentUserId;
    private CaptureOperatorReportsService service;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        Session session = Executions.getCurrent().getSession();

        if (session == null
                || session.getAttribute("userId") == null) {
            Executions.sendRedirect("/zul/login.zul");
            return;
        }

        Object sessionUserId = session.getAttribute("userId");

        try {
            currentUserId = sessionUserId instanceof Number
                    ? ((Number) sessionUserId).longValue()
                    : Long.parseLong(sessionUserId.toString().trim());
        } catch (NumberFormatException e) {
            Executions.sendRedirect("/zul/login.zul");
            return;
        }

        service = new CaptureOperatorReportsService();

        registerEvents();
        registerDateboxEvents();
        loadDownloadHistory();
    }

    private void registerEvents() {
        if (viewDataButton != null) {
            viewDataButton.addEventListener(
                    Events.ON_CLICK,
                    event -> viewReportData());
        }

        if (exportButton != null) {
            exportButton.addEventListener(
                    Events.ON_CLICK,
                    event -> exportReport());
        }
    }

    private void registerDateboxEvents() {
        if (fromDate != null) {
            fromDate.addEventListener(
                    Events.ON_OPEN,
                    event -> repositionDatePopup(fromDate));
        }

        if (toDate != null) {
            toDate.addEventListener(
                    Events.ON_OPEN,
                    event -> repositionDatePopup(toDate));
        }
    }

    private void repositionDatePopup(Datebox datebox) {
        if (datebox == null) {
            return;
        }

        String uuid = datebox.getUuid();

        Clients.evalJavaScript(
                "setTimeout(function(){"
                + "var widget=zk.Widget.$('" + uuid + "');"
                + "var input=widget ? widget.$n() : null;"
                + "var popup=document.querySelector('.z-datebox-popup');"
                + "if(!input || !popup){return;}"
                + "var rect=input.getBoundingClientRect();"
                + "var popupHeight=popup.offsetHeight;"
                + "var popupWidth=popup.offsetWidth;"
                + "var top=rect.bottom+2;"
                + "var left=rect.left;"
                + "if(top+popupHeight>window.innerHeight){"
                + "top=rect.top-popupHeight-2;"
                + "}"
                + "if(left+popupWidth>window.innerWidth){"
                + "left=window.innerWidth-popupWidth-5;"
                + "}"
                + "if(left<5){left=5;}"
                + "if(top<5){top=5;}"
                + "popup.style.position='fixed';"
                + "popup.style.left=left+'px';"
                + "popup.style.top=top+'px';"
                + "popup.style.zIndex='9999999';"
                + "},50);"
        );
    }

    private void viewReportData() {
        Date from = getFromDate();
        Date to = getToDate();

        if (!isValidDateRange(from, to)) {
            return;
        }

        List<OutwardBatch> batches;

        try {
            batches = service.getReportData(
                    currentUserId,
                    from,
                    to);
        } catch (Exception e) {
            e.printStackTrace();

            Messagebox.show(
                    "Unable to load report data.",
                    "Report Data",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        if (batches == null || batches.isEmpty()) {
            Messagebox.show(
                    "No batches found for the current Capture Operator.",
                    "Report Data",
                    Messagebox.OK,
                    Messagebox.INFORMATION);

            return;
        }

        SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        StringBuilder report = new StringBuilder();

        report.append("Capture Operator Report\n\n")
              .append("Operator ID : ")
              .append(currentUserId)
              .append("\n")
              .append("From Date : ")
              .append(formatDate(from))
              .append("\n")
              .append("To Date : ")
              .append(formatDate(to))
              .append("\n\n")
              .append("Total Batches : ")
              .append(batches.size())
              .append("\n\n")
              .append("------------------------------------------------------------\n")
              .append("Batch Number | Date | Total Cheques | Batch Status\n")
              .append("------------------------------------------------------------\n");

        for (OutwardBatch batch : batches) {

            report.append(
                    safeValue(batch.getBatchNumber()))
                  .append(" | ");

            if (batch.getCreatedAt() != null) {
                report.append(
                        dateFormat.format(
                                java.sql.Timestamp.valueOf(
                                        batch.getCreatedAt())));
            } else {
                report.append("-");
            }

            report.append(" | ")
                  .append(batch.getNumberOfCheques())
                  .append(" | ")
                  .append(safeValue(batch.getBatchStatus()))
                  .append("\n");
        }

        Messagebox.show(
                report.toString(),
                "Report Data",
                Messagebox.OK,
                Messagebox.INFORMATION);
    }

    private void exportReport() {
        Date from = getFromDate();
        Date to = getToDate();

        if (!isValidDateRange(from, to)) {
            return;
        }

        String format = getSelectedFormat();

        if (format == null || format.trim().isEmpty()) {
            Messagebox.show(
                    "Please select XML or CSV format.",
                    "Export Report",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }

        format = format.trim().toUpperCase();

        if (!"XML".equals(format) && !"CSV".equals(format)) {
            Messagebox.show(
                    "Only XML and CSV formats are supported.",
                    "Export Report",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }

        List<OutwardBatch> batches;

        try {
            batches = service.getReportData(
                    currentUserId,
                    from,
                    to);
        } catch (Exception e) {
            e.printStackTrace();

            Messagebox.show(
                    "Unable to load report data for export.",
                    "Export Report",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        if (batches == null || batches.isEmpty()) {
            Messagebox.show(
                    "No data available for export.",
                    "Export Report",
                    Messagebox.OK,
                    Messagebox.INFORMATION);

            return;
        }

        try {
            if ("CSV".equals(format)) {
                downloadCSV(batches, from, to);
            } else {
                downloadXML(batches, from, to);
            }
        } catch (Exception e) {
            e.printStackTrace();

            Messagebox.show(
                    "Unable to generate report file.",
                    "Export Report",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    private void downloadCSV(
            List<OutwardBatch> batches,
            Date from,
            Date to) throws Exception {

        StringBuilder csv = new StringBuilder();

        csv.append(
                "Batch Number,Date,Total Cheques,Batch Status\r\n");

        SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        for (OutwardBatch batch : batches) {

            String createdDate =
                    batch.getCreatedAt() != null
                            ? dateFormat.format(
                                    java.sql.Timestamp.valueOf(
                                            batch.getCreatedAt()))
                            : "";

            csv.append(csvValue(batch.getBatchNumber()))
               .append(",")
               .append(csvValue(createdDate))
               .append(",")
               .append(batch.getNumberOfCheques())
               .append(",")
               .append(csvValue(batch.getBatchStatus()))
               .append("\r\n");
        }

        byte[] data = csv.toString()
                .getBytes(StandardCharsets.UTF_8);

        String fileName = createFileName(
                "capture_operator_report",
                "csv");

        downloadFile(
                data,
                fileName,
                "text/csv");

        service.saveDownloadHistory(
                currentUserId,
                from,
                to,
                "CSV");

        loadDownloadHistory();
    }

    private void downloadXML(
            List<OutwardBatch> batches,
            Date from,
            Date to) throws Exception {

        StringBuilder xml = new StringBuilder();

        xml.append(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n")
           .append("<captureOperatorReport>\r\n")
           .append("    <operatorId>")
           .append(
                   escapeXml(
                           String.valueOf(currentUserId)))
           .append("</operatorId>\r\n");

        SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        for (OutwardBatch batch : batches) {

            xml.append("    <batch>\r\n")
               .append("        <batchNumber>")
               .append(
                       escapeXml(
                               batch.getBatchNumber()))
               .append("</batchNumber>\r\n")
               .append("        <date>");

            if (batch.getCreatedAt() != null) {
                xml.append(
                        dateFormat.format(
                                java.sql.Timestamp.valueOf(
                                        batch.getCreatedAt())));
            }

            xml.append("</date>\r\n")
               .append("        <totalCheques>")
               .append(batch.getNumberOfCheques())
               .append("</totalCheques>\r\n")
               .append("        <batchStatus>")
               .append(
                       escapeXml(
                               batch.getBatchStatus()))
               .append("</batchStatus>\r\n")
               .append("    </batch>\r\n");
        }

        xml.append("</captureOperatorReport>\r\n");

        byte[] data = xml.toString()
                .getBytes(StandardCharsets.UTF_8);

        String fileName = createFileName(
                "capture_operator_report",
                "xml");

        downloadFile(
                data,
                fileName,
                "application/xml");

        service.saveDownloadHistory(
                currentUserId,
                from,
                to,
                "XML");

        loadDownloadHistory();
    }

    private void downloadFile(
            byte[] data,
            String fileName,
            String contentType) throws Exception {

        AMedia media = new AMedia(
                fileName,
                null,
                contentType,
                data);

        Filedownload.save(media);
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }

        return "\""
                + value.replace("\"", "\"\"")
                + "\"";
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String createFileName(
            String prefix,
            String extension) {

        String timestamp =
                new SimpleDateFormat(
                        "yyyyMMdd_HHmmss")
                        .format(new Date());

        return prefix
                + "_"
                + timestamp
                + "."
                + extension;
    }

    private Date getFromDate() {
        if (fromDate == null) {
            return null;
        }

        return fromDate.getValue();
    }

    private Date getToDate() {
        if (toDate == null) {
            return null;
        }

        return toDate.getValue();
    }

    private String getSelectedFormat() {
        if (formatCombo == null
                || formatCombo.getSelectedItem() == null) {
            return null;
        }

        return formatCombo
                .getSelectedItem()
                .getValue();
    }

    private boolean isValidDateRange(
            Date from,
            Date to) {

        if (from == null && to == null) {
            return true;
        }

        if (from != null && to == null) {
            return true;
        }

        if (from == null && to != null) {
            return true;
        }

        if (from.after(to)) {
            Messagebox.show(
                    "From Date cannot be later than To Date.",
                    "Invalid Date Range",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return false;
        }

        return true;
    }

    private void loadDownloadHistory() {
        if (downloadHistoryList == null) {
            return;
        }

        try {
            downloadHistoryList.getItems().clear();

            if (historyPaging != null) {
                historyPaging.setPageSize(5);
                historyPaging.setDetailed(false);
                downloadHistoryList.setPaginal(historyPaging);
            }

            List<Object[]> history =
                    service.getDownloadHistory(
                            currentUserId);

            if (history == null || history.isEmpty()) {

                if (historyCountLabel != null) {
                    historyCountLabel.setValue(
                            "Showing 0 records");
                }

                return;
            }

            SimpleDateFormat dateTimeFormat =
                    new SimpleDateFormat(
                            "dd/MM/yyyy HH:mm:ss");

            SimpleDateFormat dateFormat =
                    new SimpleDateFormat(
                            "dd/MM/yyyy");

            for (Object[] row : history) {

                Listitem item = new Listitem();

                Listcell downloadDateCell =
                        new Listcell();

                if (row[0] != null) {
                    downloadDateCell.setLabel(
                            dateTimeFormat.format(
                                    (Date) row[0]));
                } else {
                    downloadDateCell.setLabel("-");
                }

                item.appendChild(downloadDateCell);

                Listcell fromDateCell =
                        new Listcell();

                if (row[1] != null) {
                    fromDateCell.setLabel(
                            dateFormat.format(
                                    (Date) row[1]));
                } else {
                    fromDateCell.setLabel("-");
                }

                item.appendChild(fromDateCell);

                Listcell toDateCell =
                        new Listcell();

                if (row[2] != null) {
                    toDateCell.setLabel(
                            dateFormat.format(
                                    (Date) row[2]));
                } else {
                    toDateCell.setLabel("-");
                }

                item.appendChild(toDateCell);

                Listcell formatCell =
                        new Listcell();

                formatCell.setLabel(
                        row[3] != null
                                ? row[3].toString()
                                : "-");

                item.appendChild(formatCell);

                Listcell actionCell =
                        new Listcell();

                Button viewButton =
                        new Button("View");

                viewButton.setWidth("60px");
                viewButton.setHeight("28px");
                viewButton.setSclass(
                        "reports-view-button");

                Date historyFrom = null;
                Date historyTo = null;

                if (row[1] != null) {
                    historyFrom = (Date) row[1];
                }

                if (row[2] != null) {
                    historyTo = (Date) row[2];
                }

                final Date selectedFrom =
                        historyFrom;

                final Date selectedTo =
                        historyTo;

                viewButton.addEventListener(
                        Events.ON_CLICK,
                        event -> showHistoryData(
                                selectedFrom,
                                selectedTo));

                actionCell.appendChild(
                        viewButton);

                item.appendChild(actionCell);

                downloadHistoryList.appendChild(
                        item);
            }

            if (historyCountLabel != null) {
                historyCountLabel.setValue(
                        "Showing "
                        + history.size()
                        + " records");
            }

        } catch (Exception e) {
            e.printStackTrace();

            if (historyCountLabel != null) {
                historyCountLabel.setValue(
                        "Unable to load history");
            }

            Messagebox.show(
                    "Unable to load download history.",
                    "Download History",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    private void showHistoryData(
            Date from,
            Date to) {

        try {
            List<OutwardBatch> batches =
                    service.getReportData(
                            currentUserId,
                            from,
                            to);

            if (batches == null || batches.isEmpty()) {
                Messagebox.show(
                        "No batch data found for this download range.",
                        "Download History",
                        Messagebox.OK,
                        Messagebox.INFORMATION);

                return;
            }

            SimpleDateFormat dateFormat =
                    new SimpleDateFormat(
                            "dd/MM/yyyy HH:mm:ss");

            StringBuilder report =
                    new StringBuilder();

            report.append(
                    "Capture Operator Report\n\n")
                  .append("Operator ID : ")
                  .append(currentUserId)
                  .append("\n")
                  .append("From Date : ")
                  .append(formatDate(from))
                  .append("\n")
                  .append("To Date : ")
                  .append(formatDate(to))
                  .append("\n\n")
                  .append("Total Batches : ")
                  .append(batches.size())
                  .append("\n\n")
                  .append(
                          "------------------------------------------------------------\n")
                  .append(
                          "Batch Number | Date | Total Cheques | Batch Status\n")
                  .append(
                          "------------------------------------------------------------\n");

            for (OutwardBatch batch : batches) {

                report.append(
                        safeValue(
                                batch.getBatchNumber()))
                      .append(" | ");

                if (batch.getCreatedAt() != null) {
                    report.append(
                            dateFormat.format(
                                    java.sql.Timestamp.valueOf(
                                            batch.getCreatedAt())));
                } else {
                    report.append("-");
                }

                report.append(" | ")
                      .append(batch.getNumberOfCheques())
                      .append(" | ")
                      .append(
                              safeValue(
                                      batch.getBatchStatus()))
                      .append("\n");
            }

            Messagebox.show(
                    report.toString(),
                    "Download History - Report Data",
                    Messagebox.OK,
                    Messagebox.INFORMATION);

        } catch (Exception e) {
            e.printStackTrace();

            Messagebox.show(
                    "Unable to load report data.",
                    "Download History",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "-";
        }

        return new SimpleDateFormat(
                "dd/MM/yyyy").format(date);
    }

    private String safeValue(String value) {
        if (value == null
                || value.trim().isEmpty()) {
            return "-";
        }

        return value;
    }
}