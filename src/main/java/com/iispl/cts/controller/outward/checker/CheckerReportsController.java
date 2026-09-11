package com.iispl.cts.controller.outward.checker;

import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.service.outward.checker.CheckerReportsService;
import com.iispl.cts.service.outward.CheckerFileGenerationService;
import com.iispl.cts.service.outward.checker.CheckerCXFGenerationService;
import com.iispl.cts.service.outward.checker.CheckerCIBFGenerationService;
import com.iispl.cts.service.outward.RRFXmlWriter;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;

import org.zkoss.zul.Button;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * CHECKER REPORTS CONTROLLER
 * ============================================================
 *
 * Flow:
 *
 * 1. Load available batches
 * 2. Select Batch Number
 * 3. Click Load Batch
 * 4. Display batch details
 * 5. Determine rejected cheques from cheque_processing
 * 6. RRF:
 *      - Available when rejected cheque exists
 *      - Not available otherwise
 *      - Never generate empty RRF
 * 7. CFX:
 *      - Available for every batch
 * 8. CIBF:
 *      - Available for every batch
 * 9. Send generated files to NPCI
 *
 * ============================================================
 */
public class CheckerReportsController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    // ============================================================
    // ZUL COMPONENTS
    // ============================================================

    @Wire
    private Combobox batchCombo;

    @Wire
    private Button loadBatchBtn;

    @Wire
    private Label selectedBatchNumber;

    @Wire
    private Label selectedBatchChequeCount;

    @Wire
    private Label selectedBatchRejectedCount;

    @Wire
    private Label batchReportStatus;

    @Wire
    private Listbox rejectedChequeListbox;

    @Wire
    private Label rrfStatusLabel;

    @Wire
    private Button generateRrfXmlBtn;

    @Wire
    private Button generateRrfPdfBtn;

    @Wire
    private Label cfxStatusLabel;

    @Wire
    private Button generateCfxXmlBtn;

    @Wire
    private Button generateCfxPdfBtn;

    @Wire
    private Label cibfStatusLabel;

    @Wire
    private Button generateCibfBtn;

    @Wire
    private Button sendToNpciBtn;

    // ============================================================
    // SERVICES
    // ============================================================

    private CheckerReportsService service;

    private CheckerFileGenerationService fileGenerationService;

    private CheckerCXFGenerationService cxfGenerationService;

    private CheckerCIBFGenerationService cibfGenerationService;

    private RRFXmlWriter rrfXmlWriter;

    // ============================================================
    // CURRENT CHECKER USER
    // ============================================================

    private long currentCheckerUser;

    // ============================================================
    // CURRENT SELECTED BATCH
    // ============================================================

    private OutwardBatch selectedBatch;

    private List<OutwardCheque> selectedBatchCheques =
            new ArrayList<OutwardCheque>();

    private List<OutwardCheque> rejectedCheques =
            new ArrayList<OutwardCheque>();

    // ============================================================
    // GENERATED FILE PATHS
    // ============================================================

    private String generatedRrfXmlPath;

    private String generatedCfxXmlPath;

    private String generatedCibfPath;

    // ============================================================
    // PAGE INITIALIZATION
    // ============================================================

    @Override
    public void doAfterCompose(Component comp)
            throws Exception {

        super.doAfterCompose(comp);

        // ========================================================
        // GET ZK SESSION
        // ========================================================

        Session session =
                Executions.getCurrent().getSession();

        // ========================================================
        // NO SESSION
        // ========================================================

        if (session == null) {

            Executions.sendRedirect(
                    "/zul/login.zul"
            );

            return;
        }

        // ========================================================
        // GET USER ID FROM SESSION
        // ========================================================

        Object sessionUserId =
                session.getAttribute("userId");

        // ========================================================
        // USER ID NOT FOUND
        // ========================================================

        if (sessionUserId == null) {

            Executions.sendRedirect(
                    "/zul/login.zul"
            );

            return;
        }

        // ========================================================
        // CONVERT USER ID
        // ========================================================

        if (sessionUserId instanceof Number) {

            currentCheckerUser =
                    ((Number) sessionUserId)
                            .longValue();

        } else {

            try {

                currentCheckerUser =
                        Long.parseLong(
                                sessionUserId.toString()
                        );

            } catch (NumberFormatException e) {

                Executions.sendRedirect(
                        "/zul/login.zul"
                );

                return;
            }
        }

        // ========================================================
        // LOG CURRENT USER
        // ========================================================

        System.out.println(
                "CHECKER REPORTS SESSION: "
                        + "userId="
                        + currentCheckerUser
        );

        // ========================================================
        // CREATE SERVICES
        // ========================================================

        service =
                new CheckerReportsService();

        fileGenerationService =
                new CheckerFileGenerationService();

        cxfGenerationService =
                new CheckerCXFGenerationService();

        cibfGenerationService =
                new CheckerCIBFGenerationService();

        rrfXmlWriter =
                new RRFXmlWriter();

        // ========================================================
        // INITIAL UI STATE
        // ========================================================

        clearSelectedBatch();

        // ========================================================
        // LOAD BATCHES
        // ========================================================

        loadCompletedBatches();

        // ========================================================
        // BUTTON EVENTS
        // ========================================================

        if (loadBatchBtn != null) {

            loadBatchBtn.addEventListener(
                    Events.ON_CLICK,
                    new EventListener<Event>() {

                        @Override
                        public void onEvent(Event event)
                                throws Exception {

                            loadSelectedBatch();
                        }
                    }
            );
        }

        if (generateRrfXmlBtn != null) {

            generateRrfXmlBtn.addEventListener(
                    Events.ON_CLICK,
                    new EventListener<Event>() {

                        @Override
                        public void onEvent(Event event)
                                throws Exception {

                            generateRrfXml();
                        }
                    }
            );
        }

        if (generateRrfPdfBtn != null) {

            generateRrfPdfBtn.addEventListener(
                    Events.ON_CLICK,
                    new EventListener<Event>() {

                        @Override
                        public void onEvent(Event event)
                                throws Exception {

                            generateRrfPdf();
                        }
                    }
            );
        }

        if (generateCfxXmlBtn != null) {

            generateCfxXmlBtn.addEventListener(
                    Events.ON_CLICK,
                    new EventListener<Event>() {

                        @Override
                        public void onEvent(Event event)
                                throws Exception {

                            generateCfxXml();
                        }
                    }
            );
        }

        if (generateCfxPdfBtn != null) {

            generateCfxPdfBtn.addEventListener(
                    Events.ON_CLICK,
                    new EventListener<Event>() {

                        @Override
                        public void onEvent(Event event)
                                throws Exception {

                            generateCfxPdf();
                        }
                    }
            );
        }

        if (generateCibfBtn != null) {

            generateCibfBtn.addEventListener(
                    Events.ON_CLICK,
                    new EventListener<Event>() {

                        @Override
                        public void onEvent(Event event)
                                throws Exception {

                            generateCibf();
                        }
                    }
            );
        }

        if (sendToNpciBtn != null) {

            sendToNpciBtn.addEventListener(
                    Events.ON_CLICK,
                    new EventListener<Event>() {

                        @Override
                        public void onEvent(Event event)
                                throws Exception {

                            sendToNpci();
                        }
                    }
            );
        }
    }

    // ============================================================
    // LOAD AVAILABLE BATCHES
    // ============================================================

    private void loadCompletedBatches() {

        if (batchCombo == null) {
            return;
        }

        batchCombo.getItems().clear();

        try {

            List<OutwardBatch> batches =
                    service.getCheckerCompletedBatches();

            if (batches == null ||
                    batches.isEmpty()) {

                batchCombo.setPlaceholder(
                        "No batches available"
                );

                return;
            }

            for (OutwardBatch batch : batches) {

                if (batch == null) {
                    continue;
                }

                String batchNumber =
                        batch.getBatchNumber();

                if (batchNumber == null ||
                        batchNumber.trim().isEmpty()) {

                    continue;
                }

                Comboitem item =
                        new Comboitem();

                item.setLabel(
                        batchNumber
                );

                item.setValue(
                        batch
                );

                batchCombo.appendChild(
                        item
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to load batches.\n\n"
                            + safe(e.getMessage()),
                    "Reports Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }

    // ============================================================
    // LOAD SELECTED BATCH
    // ============================================================

    private void loadSelectedBatch() {

        if (batchCombo == null ||
                batchCombo.getSelectedItem() == null) {

            Messagebox.show(
                    "Please select a Batch Number.",
                    "Batch Required",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION
            );

            return;
        }

        try {

            Comboitem selectedItem =
                    batchCombo.getSelectedItem();

            Object value =
                    selectedItem.getValue();

            String batchNumber =
                    selectedItem.getLabel();

            // ====================================================
            // CLEAR PREVIOUS GENERATED FILES
            // ====================================================

            generatedRrfXmlPath = null;
            generatedCfxXmlPath = null;
            generatedCibfPath = null;

            // ====================================================
            // GET BATCH
            // ====================================================

            if (value instanceof OutwardBatch) {

                selectedBatch =
                        (OutwardBatch) value;

            } else {

                selectedBatch =
                        service.getBatchByNumber(
                                batchNumber
                        );
            }

            if (selectedBatch == null) {

                Messagebox.show(
                        "Batch not found:\n\n"
                                + batchNumber,
                        "Batch Error",
                        Messagebox.OK,
                        Messagebox.ERROR
                );

                clearSelectedBatch();

                return;
            }

            // ====================================================
            // LOAD ALL CHEQUES
            // ====================================================

            selectedBatchCheques =
                    fileGenerationService
                            .getBatchCheques(
                                    batchNumber
                            );

            if (selectedBatchCheques == null) {

                selectedBatchCheques =
                        new ArrayList<OutwardCheque>();
            }

            // ====================================================
            // FIND REJECTED CHEQUES
            // ====================================================
            //
            // IMPORTANT:
            //
            // RRF availability is determined from
            // cheque_processing.checker_action = 'REJECT'.
            //
            // Do NOT use outward_cheque.return_reason_id
            // alone to decide whether RRF exists.
            //
            // ====================================================

            rejectedCheques =
                    service.getRejectedCheques(
                            batchNumber
                    );

            if (rejectedCheques == null) {

                rejectedCheques =
                        new ArrayList<OutwardCheque>();
            }

            // ====================================================
            // UPDATE UI
            // ====================================================

            updateBatchInformation();

            loadRejectedChequeList();

            updateReportAvailability();

        } catch (Exception e) {

            e.printStackTrace();

            String selectedBatchNumber =
                    "";

            if (batchCombo.getSelectedItem() != null) {

                selectedBatchNumber =
                        safe(
                                batchCombo
                                        .getSelectedItem()
                                        .getLabel()
                        );
            }

            Messagebox.show(
                    "Unable to load batch details.\n\n"
                            + "Batch: "
                            + selectedBatchNumber
                            + "\n\n"
                            + safe(e.getMessage()),
                    "Reports Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );

            clearSelectedBatch();
        }
    }

    // ============================================================
    // UPDATE BATCH INFORMATION
    // ============================================================

    private void updateBatchInformation() {

        if (selectedBatch == null) {
            return;
        }

        String batchNumber =
                selectedBatch.getBatchNumber();

        Integer chequeCount =
                selectedBatch.getNumberOfCheques();

        int actualChequeCount =
                selectedBatchCheques == null
                        ? 0
                        : selectedBatchCheques.size();

        int rejectedCount =
                rejectedCheques == null
                        ? 0
                        : rejectedCheques.size();

        if (selectedBatchNumber != null) {

            selectedBatchNumber.setValue(
                    safe(batchNumber)
            );
        }

        if (selectedBatchChequeCount != null) {

            if (chequeCount != null) {

                selectedBatchChequeCount.setValue(
                        String.valueOf(
                                chequeCount
                        )
                );

            } else {

                selectedBatchChequeCount.setValue(
                        String.valueOf(
                                actualChequeCount
                        )
                );
            }
        }

        if (selectedBatchRejectedCount != null) {

            selectedBatchRejectedCount.setValue(
                    String.valueOf(
                            rejectedCount
                    )
            );
        }

        if (batchReportStatus != null) {

            batchReportStatus.setValue(
                    "Batch "
                            + safe(batchNumber)
                            + " loaded successfully."
            );
        }
    }

    // ============================================================
    // LOAD REJECTED CHEQUE LIST
    // ============================================================

    private void loadRejectedChequeList() {

        if (rejectedChequeListbox == null) {
            return;
        }

        rejectedChequeListbox
                .getItems()
                .clear();

        if (rejectedCheques == null ||
                rejectedCheques.isEmpty()) {

            return;
        }

        for (OutwardCheque cheque :
                rejectedCheques) {

            if (cheque == null) {
                continue;
            }

            Listitem item =
                    new Listitem();

            // ====================================================
            // CHEQUE NUMBER
            // ====================================================

            Listcell chequeNumberCell =
                    new Listcell();

            chequeNumberCell.appendChild(
                    new Label(
                            safe(
                                    cheque.getChequeNumber()
                            )
                    )
            );

            item.appendChild(
                    chequeNumberCell
            );

            // ====================================================
            // CITY CODE
            // ====================================================

            Listcell cityCell =
                    new Listcell();

            cityCell.appendChild(
                    new Label(
                            safe(
                                    cheque.getCityCode()
                            )
                    )
            );

            item.appendChild(
                    cityCell
            );

            // ====================================================
            // BANK CODE
            // ====================================================

            Listcell bankCell =
                    new Listcell();

            bankCell.appendChild(
                    new Label(
                            safe(
                                    cheque.getBankCode()
                            )
                    )
            );

            item.appendChild(
                    bankCell
            );

            // ====================================================
            // BRANCH CODE
            // ====================================================

            Listcell branchCell =
                    new Listcell();

            branchCell.appendChild(
                    new Label(
                            safe(
                                    cheque.getBranchCode()
                            )
                    )
            );

            item.appendChild(
                    branchCell
            );

            // ====================================================
            // STATUS
            // ====================================================

            Listcell statusCell =
                    new Listcell();

            Label statusLabel =
                    new Label(
                            "REJECTED"
                    );

            statusLabel.setSclass(
                    "status-rejected"
            );

            statusCell.appendChild(
                    statusLabel
            );

            item.appendChild(
                    statusCell
            );

            rejectedChequeListbox
                    .appendChild(
                            item
                    );
        }
    }

    // ============================================================
    // UPDATE REPORT AVAILABILITY
    // ============================================================

    private void updateReportAvailability() {

        boolean hasBatch =
                selectedBatch != null;

        boolean hasRejectedCheques =
                rejectedCheques != null &&
                        !rejectedCheques.isEmpty();

        // ========================================================
        // RRF
        // ========================================================
        //
        // RRF is conditional.
        //
        // Rejected cheque exists:
        //      RRF available
        //
        // No rejected cheque:
        //      RRF not available for this batch
        //
        // ========================================================

        if (rrfStatusLabel != null) {

            if (hasBatch &&
                    hasRejectedCheques) {

                rrfStatusLabel.setValue(
                        "RRF available for this batch."
                );

                rrfStatusLabel.setSclass(
                        "report-status-success"
                );

            } else {

                rrfStatusLabel.setValue(
                        "RRF not available for this batch"
                );

                rrfStatusLabel.setSclass(
                        "report-status-warning"
                );
            }
        }

        if (generateRrfXmlBtn != null) {

            generateRrfXmlBtn.setDisabled(
                    !hasBatch ||
                            !hasRejectedCheques
            );
        }

        if (generateRrfPdfBtn != null) {

            generateRrfPdfBtn.setDisabled(
                    !hasBatch ||
                            !hasRejectedCheques
            );
        }

        // ========================================================
        // CFX
        // ========================================================
        //
        // EVERY BATCH HAS CFX.
        //
        // ========================================================

        if (cfxStatusLabel != null) {

            if (hasBatch) {

                cfxStatusLabel.setValue(
                        "CFX available for this batch."
                );

                cfxStatusLabel.setSclass(
                        "report-status-success"
                );

            } else {

                cfxStatusLabel.setValue(
                        "Select a batch to generate CFX."
                );
            }
        }

        if (generateCfxXmlBtn != null) {

            generateCfxXmlBtn.setDisabled(
                    !hasBatch
            );
        }

        if (generateCfxPdfBtn != null) {

            generateCfxPdfBtn.setDisabled(
                    !hasBatch
            );
        }

        // ========================================================
        // CIBF
        // ========================================================
        //
        // EVERY BATCH HAS CIBF.
        //
        // ========================================================

        if (cibfStatusLabel != null) {

            if (hasBatch) {

                cibfStatusLabel.setValue(
                        "CIBF available for this batch."
                );

                cibfStatusLabel.setSclass(
                        "report-status-success"
                );

            } else {

                cibfStatusLabel.setValue(
                        "Select a batch to generate CIBF."
                );
            }
        }

        if (generateCibfBtn != null) {

            generateCibfBtn.setDisabled(
                    !hasBatch
            );
        }

        // ========================================================
        // NPCI
        // ========================================================

        if (sendToNpciBtn != null) {

            sendToNpciBtn.setDisabled(
                    !hasBatch
            );
        }
    }

    // ============================================================
    // GENERATE RRF XML
    // ============================================================

    private void generateRrfXml() {

        if (!isBatchLoaded()) {

            showBatchRequired();

            return;
        }

        // ========================================================
        // NEVER GENERATE EMPTY RRF
        // ========================================================

        if (rejectedCheques == null ||
                rejectedCheques.isEmpty()) {

            Messagebox.show(
                    "RRF not available for this batch.\n\n"
                            + "Batch: "
                            + selectedBatch.getBatchNumber(),
                    "RRF Not Available",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION
            );

            return;
        }

        String batchNumber =
                selectedBatch.getBatchNumber();

        try {

            String outputDirectory =
                    "C:/CTS/OUTWARD/"
                            + batchNumber;

            File file =
                    rrfXmlWriter.generateRRF(
                            batchNumber,
                            rejectedCheques,
                            outputDirectory
                    );

            if (file == null) {

                throw new Exception(
                        "RRF writer returned no file."
                );
            }

            generatedRrfXmlPath =
                    file.getAbsolutePath();

            Messagebox.show(
                    "RRF XML generated successfully.\n\n"
                            + "Batch: "
                            + batchNumber
                            + "\n\n"
                            + "Rejected Cheques: "
                            + rejectedCheques.size()
                            + "\n\n"
                            + "File:\n"
                            + generatedRrfXmlPath,
                    "RRF Generation",
                    Messagebox.OK,
                    Messagebox.INFORMATION
            );

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "RRF XML generation failed.\n\n"
                            + "Batch: "
                            + batchNumber
                            + "\n\n"
                            + safe(e.getMessage()),
                    "RRF Generation Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }

    // ============================================================
    // GENERATE RRF PDF
    // ============================================================

    private void generateRrfPdf() {

        if (!isBatchLoaded()) {

            showBatchRequired();

            return;
        }

        if (rejectedCheques == null ||
                rejectedCheques.isEmpty()) {

            Messagebox.show(
                    "RRF not available for this batch.",
                    "RRF Not Available",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION
            );

            return;
        }

        /*
         * No RRF PDF writer was supplied in the existing project.
         *
         * Do not create a fake PDF here.
         *
         * Once an RRF PDF generation service is added,
         * this method can call that service.
         */

        Messagebox.show(
                "RRF PDF generation service is not configured yet.\n\n"
                        + "RRF XML is available for this batch.",
                "RRF PDF",
                Messagebox.OK,
                Messagebox.INFORMATION
        );
    }

    // ============================================================
    // GENERATE CFX XML
    // ============================================================

    private void generateCfxXml() {

        if (!isBatchLoaded()) {

            showBatchRequired();

            return;
        }

        String batchNumber =
                selectedBatch.getBatchNumber();

        try {

            if (selectedBatchCheques == null ||
                    selectedBatchCheques.isEmpty()) {

                Messagebox.show(
                        "No cheque records found for batch:\n\n"
                                + batchNumber,
                        "CFX Generation",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION
                );

                return;
            }

            generatedCfxXmlPath =
                    cxfGenerationService.generateCXF(
                            batchNumber,
                            selectedBatchCheques
                    );

            Messagebox.show(
                    "CFX XML generated successfully.\n\n"
                            + "Batch: "
                            + batchNumber
                            + "\n\n"
                            + "Total Cheques: "
                            + selectedBatchCheques.size()
                            + "\n\n"
                            + "File:\n"
                            + generatedCfxXmlPath,
                    "CFX Generation",
                    Messagebox.OK,
                    Messagebox.INFORMATION
            );

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "CFX XML generation failed.\n\n"
                            + "Batch: "
                            + batchNumber
                            + "\n\n"
                            + safe(e.getMessage()),
                    "CFX Generation Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }

    // ============================================================
    // GENERATE CFX PDF
    // ============================================================

    private void generateCfxPdf() {

        if (!isBatchLoaded()) {

            showBatchRequired();

            return;
        }

        /*
         * No CFX PDF writer was supplied in the existing project.
         */

        Messagebox.show(
                "CFX PDF generation service is not configured yet.\n\n"
                        + "CFX XML is available for this batch.",
                "CFX PDF",
                Messagebox.OK,
                Messagebox.INFORMATION
        );
    }

    // ============================================================
    // GENERATE CIBF
    // ============================================================

    private void generateCibf() {

        if (!isBatchLoaded()) {

            showBatchRequired();

            return;
        }

        String batchNumber =
                selectedBatch.getBatchNumber();

        try {

            if (selectedBatchCheques == null ||
                    selectedBatchCheques.isEmpty()) {

                Messagebox.show(
                        "No cheque records found for batch:\n\n"
                                + batchNumber,
                        "CIBF Generation",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION
                );

                return;
            }

            generatedCibfPath =
                    cibfGenerationService.generateCIBF(
                            batchNumber,
                            selectedBatchCheques
                    );

            Messagebox.show(
                    "CIBF generated successfully.\n\n"
                            + "Batch: "
                            + batchNumber
                            + "\n\n"
                            + "Total Cheques: "
                            + selectedBatchCheques.size()
                            + "\n\n"
                            + "File:\n"
                            + generatedCibfPath,
                    "CIBF Generation",
                    Messagebox.OK,
                    Messagebox.INFORMATION
            );

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "CIBF generation failed.\n\n"
                            + "Batch: "
                            + batchNumber
                            + "\n\n"
                            + safe(e.getMessage()),
                    "CIBF Generation Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }

    // ============================================================
    // SEND TO NPCI
    // ============================================================

    private void sendToNpci() {

        if (!isBatchLoaded()) {

            showBatchRequired();

            return;
        }

        String batchNumber =
                selectedBatch.getBatchNumber();

        // ========================================================
        // REQUIRED FILES
        // ========================================================

        boolean cfxGenerated =
                generatedCfxXmlPath != null &&
                        !generatedCfxXmlPath.trim().isEmpty();

        boolean cibfGenerated =
                generatedCibfPath != null &&
                        !generatedCibfPath.trim().isEmpty();

        boolean rrfRequired =
                rejectedCheques != null &&
                        !rejectedCheques.isEmpty();

        boolean rrfGenerated =
                generatedRrfXmlPath != null &&
                        !generatedRrfXmlPath.trim().isEmpty();

        // ========================================================
        // CFX REQUIRED
        // ========================================================

        if (!cfxGenerated) {

            Messagebox.show(
                    "Please generate CFX before sending to NPCI.",
                    "NPCI Submission",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION
            );

            return;
        }

        // ========================================================
        // CIBF REQUIRED
        // ========================================================

        if (!cibfGenerated) {

            Messagebox.show(
                    "Please generate CIBF before sending to NPCI.",
                    "NPCI Submission",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION
            );

            return;
        }

        // ========================================================
        // RRF REQUIRED ONLY IF REJECTED CHEQUES EXIST
        // ========================================================

        if (rrfRequired &&
                !rrfGenerated) {

            Messagebox.show(
                    "This batch contains rejected cheque(s).\n\n"
                            + "Please generate RRF before sending to NPCI.",
                    "NPCI Submission",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION
            );

            return;
        }

        // ========================================================
        // CONFIRM
        // ========================================================

        final boolean finalRrfRequired =
                rrfRequired;

        Messagebox.show(
                "Ready to send batch to NPCI.\n\n"
                        + "Batch: "
                        + batchNumber
                        + "\n\n"
                        + "CFX: Generated\n"
                        + "CIBF: Generated\n"
                        + "RRF: "
                        + (
                        finalRrfRequired
                                ? "Generated"
                                : "Not required"
                )
                        + "\n\n"
                        + "Do you want to continue?",

                "Send to NPCI",

                Messagebox.YES |
                        Messagebox.NO,

                Messagebox.QUESTION,

                new EventListener<Event>() {

                    @Override
                    public void onEvent(
                            Event event)
                            throws Exception {

                        if (Messagebox.ON_YES.equals(
                                event.getName())) {

                            performNpciSubmission();
                        }
                    }
                }
        );
    }

    // ============================================================
    // PERFORM NPCI SUBMISSION
    // ============================================================

    private void performNpciSubmission() {

        String batchNumber =
                selectedBatch.getBatchNumber();

        /*
         * Actual NPCI submission API/file-transfer logic
         * is not present in the supplied project.
         *
         * Therefore this method only confirms that the
         * required files are ready.
         *
         * Actual NPCI integration can be connected here later.
         */

        Messagebox.show(
                "Batch is ready for NPCI submission.\n\n"
                        + "Batch: "
                        + batchNumber
                        + "\n\n"
                        + "CFX: "
                        + safe(generatedCfxXmlPath)
                        + "\n\n"
                        + "CIBF: "
                        + safe(generatedCibfPath)
                        + "\n\n"
                        + "RRF: "
                        + (
                        generatedRrfXmlPath == null
                                ? "Not required"
                                : generatedRrfXmlPath
                ),
                "NPCI Submission",
                Messagebox.OK,
                Messagebox.INFORMATION
        );
    }

    // ============================================================
    // CHECK BATCH LOADED
    // ============================================================

    private boolean isBatchLoaded() {

        return selectedBatch != null &&
                selectedBatch.getBatchNumber() != null &&
                !selectedBatch
                        .getBatchNumber()
                        .trim()
                        .isEmpty();
    }

    // ============================================================
    // SHOW BATCH REQUIRED
    // ============================================================

    private void showBatchRequired() {

        Messagebox.show(
                "Please select and load a Batch Number first.",
                "Batch Required",
                Messagebox.OK,
                Messagebox.EXCLAMATION
        );
    }

    // ============================================================
    // CLEAR SELECTED BATCH
    // ============================================================

    private void clearSelectedBatch() {

        selectedBatch = null;

        selectedBatchCheques =
                new ArrayList<OutwardCheque>();

        rejectedCheques =
                new ArrayList<OutwardCheque>();

        generatedRrfXmlPath = null;

        generatedCfxXmlPath = null;

        generatedCibfPath = null;

        if (selectedBatchNumber != null) {

            selectedBatchNumber.setValue(
                    "—"
            );
        }

        if (selectedBatchChequeCount != null) {

            selectedBatchChequeCount.setValue(
                    "—"
            );
        }

        if (selectedBatchRejectedCount != null) {

            selectedBatchRejectedCount.setValue(
                    "—"
            );
        }

        if (batchReportStatus != null) {

            batchReportStatus.setValue(
                    "Please select a batch."
            );
        }

        if (rejectedChequeListbox != null) {

            rejectedChequeListbox
                    .getItems()
                    .clear();
        }

        if (rrfStatusLabel != null) {

            rrfStatusLabel.setValue(
                    "RRF not available for this batch"
            );

            rrfStatusLabel.setSclass(
                    "report-status-warning"
            );
        }

        if (cfxStatusLabel != null) {

            cfxStatusLabel.setValue(
                    "Select a batch to generate CFX."
            );
        }

        if (cibfStatusLabel != null) {

            cibfStatusLabel.setValue(
                    "Select a batch to generate CIBF."
            );
        }

        if (generateRrfXmlBtn != null) {

            generateRrfXmlBtn.setDisabled(true);
        }

        if (generateRrfPdfBtn != null) {

            generateRrfPdfBtn.setDisabled(true);
        }

        if (generateCfxXmlBtn != null) {

            generateCfxXmlBtn.setDisabled(true);
        }

        if (generateCfxPdfBtn != null) {

            generateCfxPdfBtn.setDisabled(true);
        }

        if (generateCibfBtn != null) {

            generateCibfBtn.setDisabled(true);
        }

        if (sendToNpciBtn != null) {

            sendToNpciBtn.setDisabled(true);
        }
    }

    // ============================================================
    // SAFE STRING
    // ============================================================

    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }
}