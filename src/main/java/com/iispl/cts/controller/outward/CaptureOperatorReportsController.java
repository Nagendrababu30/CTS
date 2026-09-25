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

public class CaptureOperatorReportsController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    /*
     * =========================================================
     * DATE FORMAT
     * =========================================================
     */

    private static final String DATE_FORMAT = "dd/MM/yyyy";

    private static final String DATE_TIME_FORMAT =
            "dd/MM/yyyy HH:mm:ss";

    /*
     * =========================================================
     * WIRED COMPONENTS
     * =========================================================
     */

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

    /*
     * =========================================================
     * USER / SERVICE
     * =========================================================
     */

    private long currentUserId;

    private CaptureOperatorReportsService service;

    /*
     * =========================================================
     * PAGE INITIALIZATION
     * =========================================================
     */

    @Override
    public void doAfterCompose(Component comp)
            throws Exception {

        super.doAfterCompose(comp);

        Session session =
                Executions.getCurrent().getSession();

        /*
         * -----------------------------------------------------
         * CHECK LOGIN SESSION
         * -----------------------------------------------------
         */

        if (session == null
                || session.getAttribute("userId") == null) {

            Executions.sendRedirect("/zul/login.zul");

            return;
        }

        /*
         * -----------------------------------------------------
         * GET CURRENT USER ID
         * -----------------------------------------------------
         */

        Object sessionUserId =
                session.getAttribute("userId");

        try {

            if (sessionUserId instanceof Number) {

                currentUserId =
                        ((Number) sessionUserId).longValue();

            } else {

                currentUserId =
                        Long.parseLong(
                                sessionUserId
                                        .toString()
                                        .trim()
                        );
            }

        } catch (NumberFormatException e) {

            Executions.sendRedirect("/zul/login.zul");

            return;
        }

        /*
         * -----------------------------------------------------
         * SERVICE
         * -----------------------------------------------------
         */

        service =
                new CaptureOperatorReportsService();

        /*
         * -----------------------------------------------------
         * REGISTER EVENTS
         * -----------------------------------------------------
         */

        registerEvents();

        /*
         * -----------------------------------------------------
         * LOAD DOWNLOAD HISTORY
         * -----------------------------------------------------
         */

        loadDownloadHistory();
    }

    /*
     * =========================================================
     * REGISTER EVENTS
     * =========================================================
     */

    private void registerEvents() {

        /*
         * -----------------------------------------------------
         * EXPORT BUTTON
         * -----------------------------------------------------
         */

        if (exportButton != null) {

            exportButton.addEventListener(
                    Events.ON_CLICK,
                    event -> exportReport()
            );
        }
    }

    /*
     * =========================================================
     * EXPORT REPORT
     * =========================================================
     */

    private void exportReport() {

        /*
         * -----------------------------------------------------
         * GET MANUALLY ENTERED DATES
         * -----------------------------------------------------
         */

        Date from = getFromDate();

        Date to = getToDate();

        /*
         * -----------------------------------------------------
         * VALIDATE DATE RANGE
         * -----------------------------------------------------
         */

        if (!isValidDateRange(from, to)) {

            return;
        }

        /*
         * -----------------------------------------------------
         * GET FORMAT
         * -----------------------------------------------------
         */

        String format = getSelectedFormat();

        if (format == null
                || format.trim().isEmpty()) {

            Messagebox.show(
                    "Please select XML or CSV format.",
                    "Export Report",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION
            );

            return;
        }

        format =
                format.trim().toUpperCase();

        /*
         * -----------------------------------------------------
         * VALID FORMAT
         * -----------------------------------------------------
         */

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

        /*
         * -----------------------------------------------------
         * GET REPORT DATA
         * -----------------------------------------------------
         */

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

        /*
         * -----------------------------------------------------
         * NO DATA
         * -----------------------------------------------------
         */

        if (batches == null
                || batches.isEmpty()) {

            Messagebox.show(
                    "No data available for export.",
                    "Export Report",
                    Messagebox.OK,
                    Messagebox.INFORMATION
            );

            return;
        }

        /*
         * -----------------------------------------------------
         * GENERATE FILE
         * -----------------------------------------------------
         */

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

    /*
     * =========================================================
     * DOWNLOAD CSV
     * =========================================================
     */

    private void downloadCSV(
            List<OutwardBatch> batches,
            Date from,
            Date to)
            throws Exception {

        StringBuilder csv =
                new StringBuilder();

        /*
         * -----------------------------------------------------
         * CSV HEADER
         * -----------------------------------------------------
         */

        csv.append(
                "Batch Number,Date,Total Cheques,Batch Status\r\n"
        );

        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        DATE_TIME_FORMAT
                );

        /*
         * -----------------------------------------------------
         * CSV DATA
         * -----------------------------------------------------
         */

        for (OutwardBatch batch : batches) {

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
                            batch.getBatchNumber()
                    )
            )
            .append(",")

            .append(
                    csvValue(createdDate)
            )
            .append(",")

            .append(
                    batch.getNumberOfCheques()
            )
            .append(",")

            .append(
                    csvValue(
                            batch.getBatchStatus()
                    )
            )
            .append("\r\n");
        }

        /*
         * -----------------------------------------------------
         * FILE DATA
         * -----------------------------------------------------
         */

        byte[] data =
                csv.toString()
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        /*
         * -----------------------------------------------------
         * FILE NAME
         * -----------------------------------------------------
         */

        String fileName =
                createFileName(
                        "capture_operator_report",
                        "csv"
                );

        /*
         * -----------------------------------------------------
         * DOWNLOAD
         * -----------------------------------------------------
         */

        downloadFile(
                data,
                fileName,
                "text/csv"
        );

        /*
         * -----------------------------------------------------
         * SAVE DOWNLOAD HISTORY
         *
         * IMPORTANT:
         * This stores the manually entered dates.
         * -----------------------------------------------------
         */

        service.saveDownloadHistory(
                currentUserId,
                from,
                to,
                "CSV"
        );

        /*
         * -----------------------------------------------------
         * REFRESH HISTORY
         * -----------------------------------------------------
         */

        loadDownloadHistory();
    }

    /*
     * =========================================================
     * DOWNLOAD XML
     * =========================================================
     */

    private void downloadXML(
            List<OutwardBatch> batches,
            Date from,
            Date to)
            throws Exception {

        StringBuilder xml =
                new StringBuilder();

        /*
         * -----------------------------------------------------
         * XML HEADER
         * -----------------------------------------------------
         */

        xml.append(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
        )
        .append(
                "<captureOperatorReport>\r\n"
        )
        .append(
                "    <operatorId>"
        )
        .append(
                escapeXml(
                        String.valueOf(
                                currentUserId
                        )
                )
        )
        .append(
                "</operatorId>\r\n"
        );

        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        DATE_TIME_FORMAT
                );

        /*
         * -----------------------------------------------------
         * XML DATA
         * -----------------------------------------------------
         */

        for (OutwardBatch batch : batches) {

            xml.append(
                    "    <batch>\r\n"
            )
            .append(
                    "        <batchNumber>"
            )
            .append(
                    escapeXml(
                            batch.getBatchNumber()
                    )
            )
            .append(
                    "</batchNumber>\r\n"
            )
            .append(
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
                    "</date>\r\n"
            )
            .append(
                    "        <totalCheques>"
            )
            .append(
                    batch.getNumberOfCheques()
            )
            .append(
                    "</totalCheques>\r\n"
            )
            .append(
                    "        <batchStatus>"
            )
            .append(
                    escapeXml(
                            batch.getBatchStatus()
                    )
            )
            .append(
                    "</batchStatus>\r\n"
            )
            .append(
                    "    </batch>\r\n"
            );
        }

        /*
         * -----------------------------------------------------
         * XML END
         * -----------------------------------------------------
         */

        xml.append(
                "</captureOperatorReport>\r\n"
        );

        /*
         * -----------------------------------------------------
         * FILE DATA
         * -----------------------------------------------------
         */

        byte[] data =
                xml.toString()
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        /*
         * -----------------------------------------------------
         * FILE NAME
         * -----------------------------------------------------
         */

        String fileName =
                createFileName(
                        "capture_operator_report",
                        "xml"
                );

        /*
         * -----------------------------------------------------
         * DOWNLOAD
         * -----------------------------------------------------
         */

        downloadFile(
                data,
                fileName,
                "application/xml"
        );

        /*
         * -----------------------------------------------------
         * SAVE DOWNLOAD HISTORY
         *
         * IMPORTANT:
         * This stores the manually entered dates.
         * -----------------------------------------------------
         */

        service.saveDownloadHistory(
                currentUserId,
                from,
                to,
                "XML"
        );

        /*
         * -----------------------------------------------------
         * REFRESH HISTORY
         * -----------------------------------------------------
         */

        loadDownloadHistory();
    }

    /*
     * =========================================================
     * FILE DOWNLOAD
     * =========================================================
     */

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

        Filedownload.save(media);
    }

    /*
     * =========================================================
     * CSV VALUE
     * =========================================================
     */

    private String csvValue(String value) {

        if (value == null) {

            return "";
        }

        return "\""
                + value.replace(
                        "\"",
                        "\"\""
                )
                + "\"";
    }

    /*
     * =========================================================
     * XML ESCAPE
     * =========================================================
     */

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

    /*
     * =========================================================
     * CREATE FILE NAME
     * =========================================================
     */

    private String createFileName(
            String prefix,
            String extension) {

        String timestamp =
                new SimpleDateFormat(
                        "yyyyMMdd_HHmmss"
                )
                .format(new Date());

        return prefix
                + "_"
                + timestamp
                + "."
                + extension;
    }

    /*
     * =========================================================
     * GET FROM DATE
     *
     * Manual textbox:
     *
     * DD/MM/YYYY
     *
     * Example:
     * 02/08/2026
     * =========================================================
     */

    private Date getFromDate() {

        if (fromDate == null) {

            return null;
        }

        String value =
                fromDate.getValue();

        return parseDate(
                value,
                "From Date"
        );
    }

    /*
     * =========================================================
     * GET TO DATE
     *
     * Manual textbox:
     *
     * DD/MM/YYYY
     *
     * Example:
     * 24/09/2026
     * =========================================================
     */

    private Date getToDate() {

        if (toDate == null) {

            return null;
        }

        String value =
                toDate.getValue();

        return parseDate(
                value,
                "To Date"
        );
    }

    /*
     * =========================================================
     * PARSE MANUAL DATE
     * =========================================================
     */

    private Date parseDate(
            String value,
            String fieldName) {

        /*
         * -----------------------------------------------------
         * EMPTY DATE IS ALLOWED
         * -----------------------------------------------------
         */

        if (value == null
                || value.trim().isEmpty()) {

            return null;
        }

        value = value.trim();

        /*
         * -----------------------------------------------------
         * STRICT DATE FORMAT
         * -----------------------------------------------------
         */

        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        DATE_FORMAT
                );

        dateFormat.setLenient(false);

        try {

            return dateFormat.parse(value);

        } catch (ParseException e) {

            Messagebox.show(
                    fieldName
                            + " must be in DD/MM/YYYY format.",
                    "Invalid Date",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION
            );

            return null;
        }
    }

    /*
     * =========================================================
     * SELECTED FORMAT
     * =========================================================
     */

    private String getSelectedFormat() {

        if (formatCombo == null
                || formatCombo.getSelectedItem() == null) {

            return null;
        }

        return formatCombo
                .getSelectedItem()
                .getValue();
    }

    /*
     * =========================================================
     * DATE RANGE VALIDATION
     * =========================================================
     */

    private boolean isValidDateRange(
            Date from,
            Date to) {

        /*
         * -----------------------------------------------------
         * BOTH EMPTY
         * -----------------------------------------------------
         */

        if (from == null && to == null) {

            return true;
        }

        /*
         * -----------------------------------------------------
         * ONLY FROM DATE
         * -----------------------------------------------------
         */

        if (from != null && to == null) {

            return true;
        }

        /*
         * -----------------------------------------------------
         * ONLY TO DATE
         * -----------------------------------------------------
         */

        if (from == null && to != null) {

            return true;
        }

        /*
         * -----------------------------------------------------
         * CHECK DATE ORDER
         * -----------------------------------------------------
         */

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

    /*
     * =========================================================
     * LOAD DOWNLOAD HISTORY
     * =========================================================
     */

    private void loadDownloadHistory() {

        if (downloadHistoryList == null) {

            return;
        }

        try {

            /*
             * -------------------------------------------------
             * CLEAR OLD ROWS
             * -------------------------------------------------
             */

            downloadHistoryList
                    .getItems()
                    .clear();

            /*
             * -------------------------------------------------
             * LOAD HISTORY
             * -------------------------------------------------
             */

            List<Object[]> history =
                    service.getDownloadHistory(
                            currentUserId
                    );

            /*
             * -------------------------------------------------
             * CONFIGURE PAGINATION
             * -------------------------------------------------
             */

            if (historyPaging != null) {

                historyPaging.setPageSize(5);

                historyPaging.setTotalSize(
                        history != null
                                ? history.size()
                                : 0
                );

                historyPaging.setDetailed(false);

                /*
                 * Reset to first page after a new
                 * download is added.
                 */
                historyPaging.setActivePage(0);

                downloadHistoryList
                        .setPaginal(historyPaging);
            }

            /*
             * -------------------------------------------------
             * NO HISTORY
             * -------------------------------------------------
             */

            if (history == null
                    || history.isEmpty()) {

                if (historyCountLabel != null) {

                    historyCountLabel.setValue(
                            "Showing 0 records"
                    );
                }

                return;
            }

            /*
             * -------------------------------------------------
             * DATE FORMATS
             * -------------------------------------------------
             */

            SimpleDateFormat dateTimeFormat =
                    new SimpleDateFormat(
                            DATE_TIME_FORMAT
                    );

            SimpleDateFormat dateFormat =
                    new SimpleDateFormat(
                            DATE_FORMAT
                    );

            /*
             * -------------------------------------------------
             * ADD HISTORY ROWS
             *
             * ZK paging handles which rows are visible
             * because the ZUL listbox uses mold="paging".
             * -------------------------------------------------
             */

            for (Object[] row : history) {

                Listitem item =
                        new Listitem();

                /*
                 * ---------------------------------------------
                 * DOWNLOAD DATE
                 * ---------------------------------------------
                 */

                Listcell downloadDateCell =
                        new Listcell();

                if (row.length > 0
                        && row[0] != null) {

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

                /*
                 * ---------------------------------------------
                 * FROM DATE
                 * ---------------------------------------------
                 */

                Listcell fromDateCell =
                        new Listcell();

                if (row.length > 1
                        && row[1] != null) {

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

                /*
                 * ---------------------------------------------
                 * TO DATE
                 * ---------------------------------------------
                 */

                Listcell toDateCell =
                        new Listcell();

                if (row.length > 2
                        && row[2] != null) {

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

                /*
                 * ---------------------------------------------
                 * FORMAT
                 * ---------------------------------------------
                 */

                Listcell formatCell =
                        new Listcell();

                if (row.length > 3
                        && row[3] != null) {

                    formatCell.setLabel(
                            row[3].toString()
                    );

                } else {

                    formatCell.setLabel("-");
                }

                item.appendChild(
                        formatCell
                );

                /*
                 * ---------------------------------------------
                 * ADD ROW
                 * ---------------------------------------------
                 */

                downloadHistoryList
                        .appendChild(item);
            }

            /*
             * -------------------------------------------------
             * RECORD COUNT
             * -------------------------------------------------
             */

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

    /*
     * =========================================================
     * FORMAT DATE
     * =========================================================
     */

    private String formatDate(Date date) {

        if (date == null) {

            return "-";
        }

        return new SimpleDateFormat(
                DATE_FORMAT
        ).format(date);
    }

    /*
     * =========================================================
     * SAFE VALUE
     * =========================================================
     */

    private String safeValue(String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return "-";
        }

        return value;
    }
}