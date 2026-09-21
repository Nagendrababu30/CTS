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
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;

import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.service.outward.CaptureOperatorReportsService;

public class CaptureOperatorReportsController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    // =========================================================
    // WIRED COMPONENTS
    // =========================================================

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

    // ONLY DOWNLOAD HISTORY LIST
    @Wire
    private Listbox downloadHistoryList;

    @Wire
    private Label historyCountLabel;


    // =========================================================
    // VARIABLES
    // =========================================================

    private long currentUserId;

    private CaptureOperatorReportsService service;


    // =========================================================
    // AFTER COMPOSE
    // =========================================================

    @Override
    public void doAfterCompose(Component comp)
            throws Exception {

        super.doAfterCompose(comp);

        // -----------------------------------------------------
        // GET SESSION
        // -----------------------------------------------------

        Session session =
                Executions.getCurrent().getSession();

        if (session == null) {

            Executions.sendRedirect(
                    "/zul/login.zul"
            );

            return;
        }


        // -----------------------------------------------------
        // GET LOGGED-IN USER ID
        // -----------------------------------------------------

        Object sessionUserId =
                session.getAttribute("userId");

        if (sessionUserId == null) {

            Executions.sendRedirect(
                    "/zul/login.zul"
            );

            return;
        }


        // -----------------------------------------------------
        // CONVERT USER ID
        // -----------------------------------------------------

        if (sessionUserId instanceof Number) {

            currentUserId =
                    ((Number) sessionUserId).longValue();

        } else {

            try {

                currentUserId =
                        Long.parseLong(
                                sessionUserId
                                        .toString()
                                        .trim()
                        );

            } catch (NumberFormatException e) {

                Executions.sendRedirect(
                        "/zul/login.zul"
                );

                return;
            }
        }


        // -----------------------------------------------------
        // DEBUG
        // -----------------------------------------------------

        System.out.println(
                "========================================"
        );

        System.out.println(
                "CAPTURE OPERATOR REPORTS"
        );

        System.out.println(
                "CURRENT USER ID = "
                + currentUserId
        );

        System.out.println(
                "========================================"
        );


        // -----------------------------------------------------
        // CREATE SERVICE
        // -----------------------------------------------------

        service =
                new CaptureOperatorReportsService();


        // -----------------------------------------------------
        // REGISTER EVENTS
        // -----------------------------------------------------

        registerEvents();


        // -----------------------------------------------------
        // LOAD DOWNLOAD HISTORY
        // -----------------------------------------------------

        loadDownloadHistory();
    }


    // =========================================================
    // REGISTER EVENTS
    // =========================================================

    private void registerEvents() {

        if (viewDataButton != null) {

            viewDataButton.addEventListener(
                    Events.ON_CLICK,
                    event -> viewReportData()
            );
        }


        if (exportButton != null) {

            exportButton.addEventListener(
                    Events.ON_CLICK,
                    event -> exportReport()
            );
        }
    }


    // =========================================================
    // VIEW REPORT DATA
    // =========================================================

    private void viewReportData() {

        Date from =
                getFromDate();

        Date to =
                getToDate();


        // -----------------------------------------------------
        // VALIDATE DATE RANGE
        // -----------------------------------------------------

        if (!isValidDateRange(
                from,
                to)) {

            return;
        }


        // -----------------------------------------------------
        // GET DATA FOR CURRENT OPERATOR
        // -----------------------------------------------------

        List<OutwardBatch> batches;

        try {

            batches =
                    service.getReportData(
                            currentUserId,
                            from,
                            to
                    );

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to load report data.",
                    "Report Data",
                    Messagebox.OK,
                    Messagebox.ERROR
            );

            return;
        }


        // -----------------------------------------------------
        // NO DATA
        // -----------------------------------------------------

        if (batches == null ||
                batches.isEmpty()) {

            Messagebox.show(
                    "No batches found for the current "
                    + "Capture Operator.",
                    "Report Data",
                    Messagebox.OK,
                    Messagebox.INFORMATION
            );

            return;
        }


        // -----------------------------------------------------
        // BUILD REPORT TEXT
        // -----------------------------------------------------

        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        "dd/MM/yyyy HH:mm:ss"
                );

        StringBuilder report =
                new StringBuilder();


        report.append(
                "Capture Operator Report"
        );

        report.append("\n\n");


        report.append(
                "Operator ID : "
        );

        report.append(
                currentUserId
        );

        report.append("\n");


        report.append(
                "From Date : "
        );

        report.append(
                formatDate(from)
        );

        report.append("\n");


        report.append(
                "To Date : "
        );

        report.append(
                formatDate(to)
        );

        report.append("\n\n");


        report.append(
                "Total Batches : "
        );

        report.append(
                batches.size()
        );

        report.append("\n\n");


        report.append(
                "------------------------------------------------------------"
        );

        report.append("\n");


        report.append(
                "Batch Number | Date | Total Cheques | Batch Status"
        );

        report.append("\n");


        report.append(
                "------------------------------------------------------------"
        );

        report.append("\n");


        // -----------------------------------------------------
        // ADD BATCHES
        // -----------------------------------------------------

        for (OutwardBatch batch :
                batches) {

            report.append(
                    safeValue(
                            batch.getBatchNumber()
                    )
            );

            report.append(
                    " | "
            );


            // DATE

            if (batch.getCreatedAt() != null) {

                report.append(
                        dateFormat.format(
                                java.sql.Timestamp.valueOf(
                                        batch.getCreatedAt()
                                )
                        )
                );

            } else {

                report.append("-");
            }


            report.append(
                    " | "
            );


            // TOTAL CHEQUES

            report.append(
                    batch.getNumberOfCheques()
            );


            report.append(
                    " | "
            );


            // STATUS

            report.append(
                    safeValue(
                            batch.getBatchStatus()
                    )
            );


            report.append("\n");
        }


        // -----------------------------------------------------
        // SHOW DATA
        // -----------------------------------------------------

        Messagebox.show(
                report.toString(),
                "Report Data",
                Messagebox.OK,
                Messagebox.INFORMATION
        );
    }


    // =========================================================
    // EXPORT REPORT
    // =========================================================

    private void exportReport() {

        Date from =
                getFromDate();

        Date to =
                getToDate();


        // -----------------------------------------------------
        // VALIDATE DATE RANGE
        // -----------------------------------------------------

        if (!isValidDateRange(
                from,
                to)) {

            return;
        }


        // -----------------------------------------------------
        // GET FORMAT
        // -----------------------------------------------------

        String format =
                getSelectedFormat();


        if (format == null ||
                format.trim().isEmpty()) {

            Messagebox.show(
                    "Please select XML or CSV format.",
                    "Export Report",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION
            );

            return;
        }


        format =
                format.trim()
                        .toUpperCase();


        // -----------------------------------------------------
        // ONLY XML / CSV
        // -----------------------------------------------------

        if (!"XML".equals(format)
                && !"CSV".equals(format)) {

            Messagebox.show(
                    "Only XML and CSV formats are supported.",
                    "Export Report",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION
            );

            return;
        }


        // -----------------------------------------------------
        // GET REPORT DATA
        // -----------------------------------------------------

        List<OutwardBatch> batches;

        try {

            batches =
                    service.getReportData(
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


        // -----------------------------------------------------
        // NO DATA
        // -----------------------------------------------------

        if (batches == null ||
                batches.isEmpty()) {

            Messagebox.show(
                    "No data available for export.",
                    "Export Report",
                    Messagebox.OK,
                    Messagebox.INFORMATION
            );

            return;
        }


        // -----------------------------------------------------
        // GENERATE FILE
        // -----------------------------------------------------

        try {

            if ("CSV".equals(format)) {

                downloadCSV(
                        batches,
                        from,
                        to
                );

            } else {

                downloadXML(
                        batches,
                        from,
                        to
                );
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


    // =========================================================
    // DOWNLOAD CSV
    // =========================================================

    private void downloadCSV(
            List<OutwardBatch> batches,
            Date from,
            Date to)
            throws Exception {

        StringBuilder csv =
                new StringBuilder();


        // -----------------------------------------------------
        // HEADER
        // -----------------------------------------------------

        csv.append(
                "Batch Number,Date,Total Cheques,Batch Status"
        );

        csv.append("\r\n");


        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        "dd/MM/yyyy HH:mm:ss"
                );


        // -----------------------------------------------------
        // DATA
        // -----------------------------------------------------

        for (OutwardBatch batch :
                batches) {

            csv.append(
                    csvValue(
                            batch.getBatchNumber()
                    )
            );

            csv.append(",");


            String createdDate = "";

            if (batch.getCreatedAt() != null) {

                createdDate =
                        dateFormat.format(
                                java.sql.Timestamp.valueOf(
                                        batch.getCreatedAt()
                                )
                        );
            }


            csv.append(
                    csvValue(
                            createdDate
                    )
            );

            csv.append(",");


            csv.append(
                    batch.getNumberOfCheques()
            );

            csv.append(",");


            csv.append(
                    csvValue(
                            batch.getBatchStatus()
                    )
            );

            csv.append("\r\n");
        }


        // -----------------------------------------------------
        // BYTES
        // -----------------------------------------------------

        byte[] data =
                csv.toString()
                        .getBytes(
                                StandardCharsets.UTF_8
                        );


        // -----------------------------------------------------
        // FILE NAME
        // -----------------------------------------------------

        String fileName =
                createFileName(
                        "capture_operator_report",
                        "csv"
                );


        // -----------------------------------------------------
        // DOWNLOAD
        // -----------------------------------------------------

        downloadFile(
                data,
                fileName,
                "text/csv"
        );


        // -----------------------------------------------------
        // SAVE DOWNLOAD HISTORY
        // -----------------------------------------------------

        service.saveDownloadHistory(
                currentUserId,
                from,
                to,
                "CSV"
        );


        // -----------------------------------------------------
        // REFRESH DOWNLOAD HISTORY
        // -----------------------------------------------------

        loadDownloadHistory();
    }


    // =========================================================
    // DOWNLOAD XML
    // =========================================================

    private void downloadXML(
            List<OutwardBatch> batches,
            Date from,
            Date to)
            throws Exception {

        StringBuilder xml =
                new StringBuilder();


        // -----------------------------------------------------
        // XML HEADER
        // -----------------------------------------------------

        xml.append(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        );

        xml.append("\r\n");


        // -----------------------------------------------------
        // ROOT
        // -----------------------------------------------------

        xml.append(
                "<captureOperatorReport>"
        );

        xml.append("\r\n");


        // -----------------------------------------------------
        // OPERATOR ID
        // -----------------------------------------------------

        xml.append(
                "    <operatorId>"
        );

        xml.append(
                escapeXml(
                        String.valueOf(
                                currentUserId
                        )
                )
        );

        xml.append(
                "</operatorId>"
        );

        xml.append("\r\n");


        // -----------------------------------------------------
        // DATE FORMAT
        // -----------------------------------------------------

        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        "dd/MM/yyyy HH:mm:ss"
                );


        // -----------------------------------------------------
        // BATCHES
        // -----------------------------------------------------

        for (OutwardBatch batch :
                batches) {

            xml.append(
                    "    <batch>"
            );

            xml.append("\r\n");


            // -------------------------------------------------
            // BATCH NUMBER
            // -------------------------------------------------

            xml.append(
                    "        <batchNumber>"
            );

            xml.append(
                    escapeXml(
                            batch.getBatchNumber()
                    )
            );

            xml.append(
                    "</batchNumber>"
            );

            xml.append("\r\n");


            // -------------------------------------------------
            // DATE
            // -------------------------------------------------

            xml.append(
                    "        <date>"
            );

            if (batch.getCreatedAt() != null) {

                xml.append(
                        dateFormat.format(
                                java.sql.Timestamp.valueOf(
                                        batch.getCreatedAt()
                                )
                        )
                );
            }

            xml.append(
                    "</date>"
            );

            xml.append("\r\n");


            // -------------------------------------------------
            // TOTAL CHEQUES
            // -------------------------------------------------

            xml.append(
                    "        <totalCheques>"
            );

            xml.append(
                    batch.getNumberOfCheques()
            );

            xml.append(
                    "</totalCheques>"
            );

            xml.append("\r\n");


            // -------------------------------------------------
            // BATCH STATUS
            // -------------------------------------------------

            xml.append(
                    "        <batchStatus>"
            );

            xml.append(
                    escapeXml(
                            batch.getBatchStatus()
                    )
            );

            xml.append(
                    "</batchStatus>"
            );

            xml.append("\r\n");


            // -------------------------------------------------
            // CLOSE BATCH
            // -------------------------------------------------

            xml.append(
                    "    </batch>"
            );

            xml.append("\r\n");
        }


        // -----------------------------------------------------
        // CLOSE ROOT
        // -----------------------------------------------------

        xml.append(
                "</captureOperatorReport>"
        );

        xml.append("\r\n");


        // -----------------------------------------------------
        // BYTES
        // -----------------------------------------------------

        byte[] data =
                xml.toString()
                        .getBytes(
                                StandardCharsets.UTF_8
                        );


        // -----------------------------------------------------
        // FILE NAME
        // -----------------------------------------------------

        String fileName =
                createFileName(
                        "capture_operator_report",
                        "xml"
                );


        // -----------------------------------------------------
        // DOWNLOAD
        // -----------------------------------------------------

        downloadFile(
                data,
                fileName,
                "application/xml"
        );


        // -----------------------------------------------------
        // SAVE DOWNLOAD HISTORY
        // -----------------------------------------------------

        service.saveDownloadHistory(
                currentUserId,
                from,
                to,
                "XML"
        );


        // -----------------------------------------------------
        // REFRESH DOWNLOAD HISTORY
        // -----------------------------------------------------

        loadDownloadHistory();
    }


    // =========================================================
    // DOWNLOAD FILE
    // =========================================================

    private void downloadFile(
            byte[] data,
            String fileName,
            String contentType)
            throws Exception {

        AMedia media =
                new AMedia(
                        fileName,
                        null,
                        contentType,
                        data
                );

        Filedownload.save(
                media
        );
    }


    // =========================================================
    // CSV VALUE
    // =========================================================

    private String csvValue(
            String value) {

        if (value == null) {

            return "";
        }

        String escaped =
                value.replace(
                        "\"",
                        "\"\""
                );

        return "\""
                + escaped
                + "\"";
    }


    // =========================================================
    // XML ESCAPE
    // =========================================================

    private String escapeXml(
            String value) {

        if (value == null) {

            return "";
        }

        return value
                .replace(
                        "&",
                        "&amp;"
                )
                .replace(
                        "<",
                        "&lt;"
                )
                .replace(
                        ">",
                        "&gt;"
                )
                .replace(
                        "\"",
                        "&quot;"
                )
                .replace(
                        "'",
                        "&apos;"
                );
    }


    // =========================================================
    // CREATE FILE NAME
    // =========================================================

    private String createFileName(
            String prefix,
            String extension) {

        String timestamp =
                new SimpleDateFormat(
                        "yyyyMMdd_HHmmss"
                ).format(
                        new Date()
                );

        return prefix
                + "_"
                + timestamp
                + "."
                + extension;
    }


    // =========================================================
    // GET FROM DATE
    // =========================================================

    private Date getFromDate() {

        if (fromDate == null) {

            return null;
        }

        return fromDate.getValue();
    }


    // =========================================================
    // GET TO DATE
    // =========================================================

    private Date getToDate() {

        if (toDate == null) {

            return null;
        }

        return toDate.getValue();
    }


    // =========================================================
    // GET SELECTED FORMAT
    // =========================================================

    private String getSelectedFormat() {

        if (formatCombo == null ||
                formatCombo.getSelectedItem() == null) {

            return null;
        }

        return formatCombo
                .getSelectedItem()
                .getValue();
    }


    // =========================================================
    // VALIDATE DATE RANGE
    // =========================================================

    private boolean isValidDateRange(
            Date from,
            Date to) {

        // -----------------------------------------------------
        // BOTH EMPTY
        // -----------------------------------------------------

        if (from == null &&
                to == null) {

            return true;
        }


        // -----------------------------------------------------
        // ONLY FROM
        // -----------------------------------------------------

        if (from != null &&
                to == null) {

            return true;
        }


        // -----------------------------------------------------
        // ONLY TO
        // -----------------------------------------------------

        if (from == null &&
                to != null) {

            return true;
        }


        // -----------------------------------------------------
        // BOTH PRESENT
        // -----------------------------------------------------

        if (from.after(to)) {

            Messagebox.show(
                    "From Date cannot be later than To Date.",
                    "Invalid Date Range",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION
            );

            return false;
        }

        return true;
    }


    // =========================================================
    // DOWNLOAD HISTORY
    // =========================================================

    private void loadDownloadHistory() {

        if (downloadHistoryList == null) {

            return;
        }


        try {

            // -------------------------------------------------
            // CLEAR OLD DATA
            // -------------------------------------------------

            downloadHistoryList.getItems().clear();


            // -------------------------------------------------
            // GET HISTORY FOR CURRENT OPERATOR
            // -------------------------------------------------

            List<Object[]> history =
                    service.getDownloadHistory(
                            currentUserId
                    );


            // -------------------------------------------------
            // NO HISTORY
            // -------------------------------------------------

            if (history == null ||
                    history.isEmpty()) {

                if (historyCountLabel != null) {

                    historyCountLabel.setValue(
                            "Showing 0 records"
                    );
                }

                return;
            }


            // -------------------------------------------------
            // DATE FORMAT
            // -------------------------------------------------

            SimpleDateFormat dateTimeFormat =
                    new SimpleDateFormat(
                            "dd/MM/yyyy HH:mm:ss"
                    );

            SimpleDateFormat dateFormat =
                    new SimpleDateFormat(
                            "dd/MM/yyyy"
                    );


            // -------------------------------------------------
            // CREATE LIST ITEMS
            // -------------------------------------------------

            for (Object[] row :
                    history) {

                Listitem item =
                        new Listitem();


                // ---------------------------------------------
                // DOWNLOAD DATE
                // ---------------------------------------------

                Listcell downloadDateCell =
                        new Listcell();

                if (row[0] != null) {

                    downloadDateCell.setLabel(
                            dateTimeFormat.format(
                                    (Date) row[0]
                            )
                    );

                } else {

                    downloadDateCell.setLabel("-");
                }

                item.appendChild(
                        downloadDateCell
                );


                // ---------------------------------------------
                // FROM DATE
                // ---------------------------------------------

                Listcell fromDateCell =
                        new Listcell();

                if (row[1] != null) {

                    fromDateCell.setLabel(
                            dateFormat.format(
                                    (Date) row[1]
                            )
                    );

                } else {

                    fromDateCell.setLabel("-");
                }

                item.appendChild(
                        fromDateCell
                );


                // ---------------------------------------------
                // TO DATE
                // ---------------------------------------------

                Listcell toDateCell =
                        new Listcell();

                if (row[2] != null) {

                    toDateCell.setLabel(
                            dateFormat.format(
                                    (Date) row[2]
                            )
                    );

                } else {

                    toDateCell.setLabel("-");
                }

                item.appendChild(
                        toDateCell
                );


                // ---------------------------------------------
                // FORMAT
                // ---------------------------------------------

                Listcell formatCell =
                        new Listcell();

                formatCell.setLabel(
                        row[3] != null
                                ? row[3].toString()
                                : "-"
                );

                item.appendChild(
                        formatCell
                );


                // ---------------------------------------------
                // ACTION
                // ---------------------------------------------

                Listcell actionCell =
                        new Listcell();

                Button viewButton =
                        new Button(
                                "View"
                        );

                viewButton.setWidth(
                        "60px"
                );

                viewButton.setHeight(
                        "28px"
                );

                viewButton.setSclass(
                        "reports-view-button"
                );


                // Save dates for this history row

                Date historyFrom = null;

                Date historyTo = null;


                if (row[1] != null) {

                    historyFrom =
                            (Date) row[1];
                }


                if (row[2] != null) {

                    historyTo =
                            (Date) row[2];
                }


                final Date selectedFrom =
                        historyFrom;

                final Date selectedTo =
                        historyTo;


                viewButton.addEventListener(
                        Events.ON_CLICK,
                        event -> {

                            showHistoryData(
                                    selectedFrom,
                                    selectedTo
                            );
                        }
                );


                actionCell.appendChild(
                        viewButton
                );

                item.appendChild(
                        actionCell
                );


                // ---------------------------------------------
                // ADD ROW
                // ---------------------------------------------

                downloadHistoryList.appendChild(
                        item
                );
            }


            // -------------------------------------------------
            // UPDATE COUNT
            // -------------------------------------------------

            if (historyCountLabel != null) {

                historyCountLabel.setValue(
                        "Showing "
                        + history.size()
                        + " records"
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


    // =========================================================
    // SHOW HISTORY DATA
    // =========================================================

    private void showHistoryData(
            Date from,
            Date to) {

        try {

            List<OutwardBatch> batches =
                    service.getReportData(
                            currentUserId,
                            from,
                            to
                    );


            // -------------------------------------------------
            // NO DATA
            // -------------------------------------------------

            if (batches == null ||
                    batches.isEmpty()) {

                Messagebox.show(
                        "No batch data found for this download range.",
                        "Download History",
                        Messagebox.OK,
                        Messagebox.INFORMATION
                );

                return;
            }


            // -------------------------------------------------
            // BUILD DATA
            // -------------------------------------------------

            SimpleDateFormat dateFormat =
                    new SimpleDateFormat(
                            "dd/MM/yyyy HH:mm:ss"
                    );

            StringBuilder report =
                    new StringBuilder();


            report.append(
                    "Capture Operator Report"
            );

            report.append("\n\n");


            report.append(
                    "Operator ID : "
            );

            report.append(
                    currentUserId
            );

            report.append("\n");


            report.append(
                    "From Date : "
            );

            report.append(
                    formatDate(from)
            );

            report.append("\n");


            report.append(
                    "To Date : "
            );

            report.append(
                    formatDate(to)
            );

            report.append("\n\n");


            report.append(
                    "Total Batches : "
            );

            report.append(
                    batches.size()
            );

            report.append("\n\n");


            report.append(
                    "------------------------------------------------------------"
            );

            report.append("\n");


            report.append(
                    "Batch Number | Date | Total Cheques | Batch Status"
            );

            report.append("\n");


            report.append(
                    "------------------------------------------------------------"
            );

            report.append("\n");


            for (OutwardBatch batch :
                    batches) {

                report.append(
                        safeValue(
                                batch.getBatchNumber()
                        )
                );

                report.append(
                        " | "
                );


                if (batch.getCreatedAt() != null) {

                    report.append(
                            dateFormat.format(
                                    java.sql.Timestamp.valueOf(
                                            batch.getCreatedAt()
                                    )
                            )
                    );

                } else {

                    report.append("-");
                }


                report.append(
                        " | "
                );


                report.append(
                        batch.getNumberOfCheques()
                );


                report.append(
                        " | "
                );


                report.append(
                        safeValue(
                                batch.getBatchStatus()
                        )
                );

                report.append("\n");
            }


            // -------------------------------------------------
            // SHOW
            // -------------------------------------------------

            Messagebox.show(
                    report.toString(),
                    "Download History - Report Data",
                    Messagebox.OK,
                    Messagebox.INFORMATION
            );


        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to load report data.",
                    "Download History",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }


    // =========================================================
    // FORMAT DATE
    // =========================================================

    private String formatDate(
            Date date) {

        if (date == null) {

            return "-";
        }

        return new SimpleDateFormat(
                "dd/MM/yyyy"
        ).format(date);
    }


    // =========================================================
    // SAFE VALUE
    // =========================================================

    private String safeValue(
            String value) {

        if (value == null ||
                value.trim().isEmpty()) {

            return "-";
        }

        return value;
    }
}