package com.iispl.cts.controller.outward;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
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
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Textbox;

import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.service.outward.CaptureOperatorReportsService;

public class CaptureOperatorReportsController extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;
    private static final String DATE_FORMAT = "dd/MM/yyyy";
    private static final String DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm:ss";

    @Wire
    private Textbox fromDate;

    @Wire
    private Textbox toDate;

    @Wire
    private Combobox formatCombo;

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

    // initialize controller
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        Session session = Executions.getCurrent().getSession();

        if (session == null || session.getAttribute("userId") == null) {
            Executions.sendRedirect("/zul/login.zul");
            return;
        }

        Object sessionUserId = session.getAttribute("userId");

        try {
            if (sessionUserId instanceof Number) {
                currentUserId = ((Number) sessionUserId).longValue();
            } else {
                currentUserId = Long.parseLong(
                    sessionUserId.toString().trim()
                );
            }
        } catch (NumberFormatException e) {
            Executions.sendRedirect("/zul/login.zul");
            return;
        }

        service = new CaptureOperatorReportsService();

        registerEvents();
        loadDownloadHistory();
    }

    // register export events
    private void registerEvents() {
        if (exportButton != null) {
            exportButton.addEventListener(
                Events.ON_CLICK,
                event -> exportReport()
            );
        }
    }

    // validate and export report
    private void exportReport() {
        String fromValue =
            fromDate != null ? fromDate.getValue() : null;

        String toValue =
            toDate != null ? toDate.getValue() : null;

        if (fromValue == null ||
            fromValue.trim().isEmpty()) {

            Messagebox.show(
                "Please enter From Date.",
                "Date Required",
                Messagebox.OK,
                Messagebox.EXCLAMATION
            );
            return;
        }

        if (toValue == null ||
            toValue.trim().isEmpty()) {

            Messagebox.show(
                "Please enter To Date.",
                "Date Required",
                Messagebox.OK,
                Messagebox.EXCLAMATION
            );
            return;
        }

        if (!isValidDateInput(fromValue) ||
            !isValidDateInput(toValue)) {

            Messagebox.show(
                "Please enter a proper date in DD/MM/YYYY format.",
                "Invalid Date",
                Messagebox.OK,
                Messagebox.EXCLAMATION
            );
            return;
        }

        Date from = parseDate(fromValue);
        Date to = parseDate(toValue);

        if (from == null || to == null) {
            Messagebox.show(
                "Please enter a proper date in DD/MM/YYYY format.",
                "Invalid Date",
                Messagebox.OK,
                Messagebox.EXCLAMATION
            );
            return;
        }

        if (from.after(to)) {
            Messagebox.show(
                "From Date cannot be later than To Date.",
                "Invalid Date Range",
                Messagebox.OK,
                Messagebox.EXCLAMATION
            );
            return;
        }

        String format = getSelectedFormat();

        if (format == null || format.trim().isEmpty()) {
            Messagebox.show(
                "Please select XML or CSV format.",
                "Export Report",
                Messagebox.OK,
                Messagebox.EXCLAMATION
            );
            return;
        }

        format = format.trim().toUpperCase();

        if (!"XML".equals(format) && !"CSV".equals(format)) {
            Messagebox.show(
                "Only XML and CSV formats are supported.",
                "Export Report",
                Messagebox.OK,
                Messagebox.EXCLAMATION
            );
            return;
        }

        List<OutwardBatch> batches;

        try {
            batches = service.getReportData(
                currentUserId,
                from,
                to
            );
        } catch (Exception e) {
            e.printStackTrace();

            Messagebox.show(
                "Unable to load report data for export.",
                "Export Report",
                Messagebox.OK,
                Messagebox.ERROR
            );
            return;
        }

        if (batches == null || batches.isEmpty()) {
            Messagebox.show(
                "No data available for the selected date range.",
                "Export Report",
                Messagebox.OK,
                Messagebox.INFORMATION
            );
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
                Messagebox.ERROR
            );
        }
    }

    // validate date format
    private boolean isValidDateInput(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        String cleanValue = value.trim();

        SimpleDateFormat dateFormat =
            new SimpleDateFormat(DATE_FORMAT);

        dateFormat.setLenient(false);

        try {
            Date parsedDate = dateFormat.parse(cleanValue);

            return cleanValue.equals(
                dateFormat.format(parsedDate)
            );
        } catch (ParseException e) {
            return false;
        }
    }

    // parse valid date
    private Date parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        SimpleDateFormat dateFormat =
            new SimpleDateFormat(DATE_FORMAT);

        dateFormat.setLenient(false);

        try {
            return dateFormat.parse(value.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    // generate CSV report
    private void downloadCSV(
            List<OutwardBatch> batches,
            Date from,
            Date to) throws Exception {

        StringBuilder csv = new StringBuilder();

        csv.append(
            "Batch Number,Date,Total Cheques,Batch Status\r\n"
        );

        SimpleDateFormat dateFormat =
            new SimpleDateFormat(DATE_TIME_FORMAT);

        for (OutwardBatch batch : batches) {
            String createdDate = "";

            if (batch.getCreatedAt() != null) {
                createdDate = dateFormat.format(
                    java.sql.Timestamp.valueOf(
                        batch.getCreatedAt()
                    )
                );
            }

            csv.append(
                csvValue(batch.getBatchNumber())
            )
            .append(",")
            .append(csvValue(createdDate))
            .append(",")
            .append(batch.getNumberOfCheques())
            .append(",")
            .append(csvValue(batch.getBatchStatus()))
            .append("\r\n");
        }

        byte[] data =
            csv.toString().getBytes(StandardCharsets.UTF_8);

        String fileName =
            createFileName(
                "capture_operator_report",
                "csv"
            );

        downloadFile(
            data,
            fileName,
            "text/csv"
        );

        service.saveDownloadHistory(
            currentUserId,
            from,
            to,
            "CSV"
        );

        loadDownloadHistory();
    }

    // generate XML report
    private void downloadXML(
            List<OutwardBatch> batches,
            Date from,
            Date to) throws Exception {

        StringBuilder xml = new StringBuilder();

        xml.append(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
        )
        .append("<captureOperatorReport>\r\n")
        .append("    <operatorId>")
        .append(
            escapeXml(
                String.valueOf(currentUserId)
            )
        )
        .append("</operatorId>\r\n");

        SimpleDateFormat dateFormat =
            new SimpleDateFormat(DATE_TIME_FORMAT);

        for (OutwardBatch batch : batches) {
            xml.append("    <batch>\r\n")
               .append("        <batchNumber>")
               .append(
                   escapeXml(
                       batch.getBatchNumber()
                   )
               )
               .append("</batchNumber>\r\n")
               .append("        <date>");

            if (batch.getCreatedAt() != null) {
                xml.append(
                    dateFormat.format(
                        java.sql.Timestamp.valueOf(
                            batch.getCreatedAt()
                        )
                    )
                );
            }

            xml.append("</date>\r\n")
               .append("        <totalCheques>")
               .append(batch.getNumberOfCheques())
               .append("</totalCheques>\r\n")
               .append("        <batchStatus>")
               .append(
                   escapeXml(
                       batch.getBatchStatus()
                   )
               )
               .append("</batchStatus>\r\n")
               .append("    </batch>\r\n");
        }

        xml.append("</captureOperatorReport>\r\n");

        byte[] data =
            xml.toString().getBytes(StandardCharsets.UTF_8);

        String fileName =
            createFileName(
                "capture_operator_report",
                "xml"
            );

        downloadFile(
            data,
            fileName,
            "application/xml"
        );

        service.saveDownloadHistory(
            currentUserId,
            from,
            to,
            "XML"
        );

        loadDownloadHistory();
    }

    // download generated file
    private void downloadFile(
            byte[] data,
            String fileName,
            String contentType) throws Exception {

        AMedia media = new AMedia(
            fileName,
            null,
            contentType,
            data
        );

        Filedownload.save(media);
    }

    // format CSV value
    private String csvValue(String value) {
        if (value == null) {
            return "";
        }

        return "\"" +
            value.replace("\"", "\"\"") +
            "\"";
    }

    // escape XML value
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

    // create report file name
    private String createFileName(
            String prefix,
            String extension) {

        String timestamp =
            new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());

        return prefix + "_" +
               timestamp + "." +
               extension;
    }

    // get selected export format
    private String getSelectedFormat() {
        if (formatCombo == null ||
            formatCombo.getSelectedItem() == null) {
            return null;
        }

        return formatCombo
            .getSelectedItem()
            .getValue();
    }

    // load download history
    private void loadDownloadHistory() {
        if (downloadHistoryList == null) {
            return;
        }

        try {
            downloadHistoryList.getItems().clear();

            List<Object[]> history =
                service.getDownloadHistory(
                    currentUserId
                );

            if (historyPaging != null) {
                historyPaging.setPageSize(5);

                historyPaging.setTotalSize(
                    history != null
                        ? history.size()
                        : 0
                );

                historyPaging.setDetailed(false);
                historyPaging.setActivePage(0);

                downloadHistoryList.setPaginal(
                    historyPaging
                );
            }

            if (history == null ||
                history.isEmpty()) {

                if (historyCountLabel != null) {
                    historyCountLabel.setValue(
                        "Showing 0 records"
                    );
                }

                return;
            }

            SimpleDateFormat dateTimeFormat =
                new SimpleDateFormat(
                    DATE_TIME_FORMAT
                );

            SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                    DATE_FORMAT
                );

            for (Object[] row : history) {
                Listitem item = new Listitem();

                Listcell downloadDateCell =
                    new Listcell();

                if (row.length > 0 &&
                    row[0] != null) {

                    downloadDateCell.setLabel(
                        dateTimeFormat.format(
                            (Date) row[0]
                        )
                    );
                } else {
                    downloadDateCell.setLabel("-");
                }

                item.appendChild(downloadDateCell);

                Listcell fromDateCell =
                    new Listcell();

                if (row.length > 1 &&
                    row[1] != null) {

                    fromDateCell.setLabel(
                        dateFormat.format(
                            (Date) row[1]
                        )
                    );
                } else {
                    fromDateCell.setLabel("-");
                }

                item.appendChild(fromDateCell);

                Listcell toDateCell =
                    new Listcell();

                if (row.length > 2 &&
                    row[2] != null) {

                    toDateCell.setLabel(
                        dateFormat.format(
                            (Date) row[2]
                        )
                    );
                } else {
                    toDateCell.setLabel("-");
                }

                item.appendChild(toDateCell);

                Listcell formatCell =
                    new Listcell();

                if (row.length > 3 &&
                    row[3] != null) {

                    formatCell.setLabel(
                        row[3].toString()
                    );
                } else {
                    formatCell.setLabel("-");
                }

                item.appendChild(formatCell);

                downloadHistoryList.appendChild(item);
            }

            if (historyCountLabel != null) {
                historyCountLabel.setValue(
                    "Showing " +
                    history.size() +
                    " records"
                );
            }

        } catch (Exception e) {
            e.printStackTrace();

            if (historyCountLabel != null) {
                historyCountLabel.setValue(
                    "Unable to load history"
                );
            }

            Messagebox.show(
                "Unable to load download history.",
                "Download History",
                Messagebox.OK,
                Messagebox.ERROR
            );
        }
    }
}