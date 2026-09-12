package com.iispl.cts.controller.outward;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;

import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.service.outward.CaptureOperatorBatchService;

public class CaptureOperatorBatchCaptureController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    // =========================================================
    // ZK COMPONENTS
    // =========================================================

    @Wire
    private Combobox branchCodeCombo;

    @Wire
    private Textbox branchNameTextbox;

    @Wire
    private Intbox numberOfCheques;

    @Wire
    private Fileupload batchFilesUpload;

    @Wire
    private Label selectedFilesLabel;

    // =========================================================
    // SERVICE
    // =========================================================

    private CaptureOperatorBatchService service;

    // =========================================================
    // UPLOADED FILES
    // =========================================================

    private final List<Media> uploadedFiles =
            new ArrayList<>();

    // =========================================================
    // INIT
    // =========================================================

    @Override
    public void doAfterCompose(
            Component component)
            throws Exception {

        super.doAfterCompose(
                component);

        Session session =
                Executions.getCurrent()
                        .getSession();

        if (session == null) {

            Executions.sendRedirect(
                    "/zul/login.zul");

            return;
        }

        Object sessionUserId =
                session.getAttribute(
                        "userId");

        if (sessionUserId == null) {

            Executions.sendRedirect(
                    "/zul/login.zul");

            return;
        }

        long userId;

        if (sessionUserId instanceof Number) {

            userId =
                    ((Number) sessionUserId)
                            .longValue();

        } else {

            try {

                userId =
                        Long.parseLong(
                                sessionUserId.toString());

            } catch (NumberFormatException e) {

                Executions.sendRedirect(
                        "/zul/login.zul");

                return;
            }
        }

        System.out.println(
                "CAPTURE OPERATOR SESSION: "
                + "userId="
                + userId);

        service =
                new CaptureOperatorBatchService();

        loadBranches();
    }

    // =========================================================
    // LOAD BRANCHES
    // =========================================================

    private void loadBranches() {

        branchCodeCombo
                .getItems()
                .clear();

        try {

            List<String[]> branches =
                    service.getActiveBranches();

            if (branches == null ||
                    branches.isEmpty()) {

                System.out.println(
                        "No active branches found.");

                return;
            }

            for (String[] branch :
                    branches) {

                if (branch == null ||
                        branch.length < 2) {

                    continue;
                }

                Comboitem item =
                        new Comboitem();

                item.setLabel(
                        branch[0]);

                item.setValue(
                        branch[0]);

                item.setAttribute(
                        "branchName",
                        branch[1]);

                branchCodeCombo
                        .appendChild(item);
            }

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to load branches from database.\n\n"
                    + e.getMessage(),
                    "Database Error",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    // =========================================================
    // BRANCH SELECT
    // =========================================================

    @Listen("onSelect = #branchCodeCombo")
    public void onBranchSelected() {

        Comboitem selectedItem =
                branchCodeCombo
                        .getSelectedItem();

        if (selectedItem == null) {

            branchNameTextbox
                    .setValue("");

            return;
        }

        String branchCode =
                selectedItem.getValue();

        String branchName =
                (String) selectedItem
                        .getAttribute(
                                "branchName");

        if (branchName == null ||
                branchName.trim().isEmpty()) {

            branchName =
                    service.getBranchName(
                            branchCode);
        }

        branchNameTextbox.setValue(
                branchName == null
                        ? ""
                        : branchName);
    }

    // =========================================================
    // FILE UPLOAD
    // =========================================================

    @Listen("onUpload = #batchFilesUpload")
    public void onBatchFilesUpload(
            UploadEvent event) {

        try {

            Media media =
                    event.getMedia();

            if (media == null) {

                selectedFilesLabel.setValue(
                        "No file selected.");

                return;
            }

            String fileName =
                    media.getName();

            if (fileName == null ||
                    fileName.trim().isEmpty()) {

                Messagebox.show(
                        "Invalid file selected.",
                        "Upload Error",
                        Messagebox.OK,
                        Messagebox.ERROR);

                return;
            }

            // =================================================
            // DUPLICATE FILE NAME
            // =================================================

            for (Media existing :
                    uploadedFiles) {

                if (existing != null &&
                        existing.getName() != null &&
                        existing.getName()
                                .equalsIgnoreCase(
                                        fileName)) {

                    Messagebox.show(
                            "File already selected:\n\n"
                            + fileName,
                            "Duplicate File",
                            Messagebox.OK,
                            Messagebox.EXCLAMATION);

                    return;
                }
            }

            // =================================================
            // ONLY ONE XML
            // =================================================

            if (fileName
                    .toLowerCase()
                    .endsWith(".xml")) {

                for (Media existing :
                        uploadedFiles) {

                    if (existing != null &&
                            existing.getName() != null &&
                            existing.getName()
                                    .toLowerCase()
                                    .endsWith(".xml")) {

                        Messagebox.show(
                                "Only one XML file is allowed.",
                                "XML Validation",
                                Messagebox.OK,
                                Messagebox.EXCLAMATION);

                        return;
                    }
                }
            }

            // =================================================
            // ADD FILE
            // =================================================

            uploadedFiles.add(
                    media);

            // =================================================
            // COUNT FILES
            // =================================================

            int xmlCount = 0;

            int imageCount = 0;

            for (Media selectedMedia :
                    uploadedFiles) {

                if (selectedMedia == null ||
                        selectedMedia.getName() == null) {

                    continue;
                }

                String lowerName =
                        selectedMedia
                                .getName()
                                .toLowerCase();

                if (lowerName.endsWith(".xml")) {

                    xmlCount++;

                } else if (
                        lowerName.endsWith(".jpg")
                        || lowerName.endsWith(".jpeg")
                        || lowerName.endsWith(".png")
                        || lowerName.endsWith(".tif")
                        || lowerName.endsWith(".tiff")
                        || lowerName.endsWith(".bmp")) {

                    imageCount++;
                }
            }

            selectedFilesLabel.setValue(
                    "Files selected: "
                    + uploadedFiles.size()
                    + " | XML: "
                    + xmlCount
                    + " | Images: "
                    + imageCount);

            System.out.println(
                    "FILE ADDED: "
                    + fileName);

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to process selected file.\n\n"
                    + e.getMessage(),
                    "Upload Error",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    // =========================================================
    // CAPTURE BATCH
    // =========================================================

    @Listen("onClick = #capturedBatchButton")
    public void captureBatch() {

        // =====================================================
        // VALIDATE BASIC UI INPUT
        // =====================================================

        Comboitem selectedItem =
                branchCodeCombo
                        .getSelectedItem();

        if (selectedItem == null) {

            Messagebox.show(
                    "Please select branch code.",
                    "Validation",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }

        final String branchCode =
                selectedItem.getValue();

        final Integer chequeCount =
                numberOfCheques.getValue();

        if (chequeCount == null ||
                chequeCount <= 0) {

            Messagebox.show(
                    "Please enter a valid number of cheques.",
                    "Validation",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }

        if (uploadedFiles.isEmpty()) {

            Messagebox.show(
                    "Please select the XML file and cheque images.",
                    "Validation",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }

        int xmlCount = 0;

        for (Media media :
                uploadedFiles) {

            if (media != null &&
                    media.getName() != null &&
                    media.getName()
                            .toLowerCase()
                            .endsWith(".xml")) {

                xmlCount++;
            }
        }

        if (xmlCount == 0) {

            Messagebox.show(
                    "No XML file was selected.",
                    "Validation",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }

        if (xmlCount > 1) {

            Messagebox.show(
                    "Please select only one XML file.",
                    "Validation",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }

        // =====================================================
        // SESSION
        // =====================================================

        Session session =
                Executions.getCurrent()
                        .getSession();

        if (session == null) {

            Messagebox.show(
                    "Your session has expired. Please login again.",
                    "Session Expired",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            Executions.sendRedirect(
                    "/zul/login.zul");

            return;
        }

        Object sessionUserId =
                session.getAttribute(
                        "userId");

        if (sessionUserId == null) {

            Messagebox.show(
                    "Your session has expired. Please login again.",
                    "Session Expired",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            Executions.sendRedirect(
                    "/zul/login.zul");

            return;
        }

        final int createdBy;

        try {

            long userId;

            if (sessionUserId instanceof Number) {

                userId =
                        ((Number) sessionUserId)
                                .longValue();

            } else {

                userId =
                        Long.parseLong(
                                sessionUserId.toString());
            }

            createdBy =
                    Math.toIntExact(
                            userId);

        } catch (Exception e) {

            Messagebox.show(
                    "Invalid user session. Please login again.",
                    "Session Error",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            Executions.sendRedirect(
                    "/zul/login.zul");

            return;
        }

        // =====================================================
        // FIRST CAPTURE ATTEMPT
        // =====================================================

        try {

            OutwardBatch batch =
                    service.captureBatch(
                            branchCode,
                            chequeCount,
                            uploadedFiles,
                            createdBy,
                            false);

            // =================================================
            // SUCCESS
            // =================================================

            showCaptureSuccess(
                    batch);

        } catch (
                CaptureOperatorBatchService
                        .ChequeCountMismatchException mismatch) {

            // =================================================
            // COUNT MISMATCH
            // =================================================
            //
            // DO NOT create the batch yet.
            //
            // Ask operator whether to continue.
            //
            // =================================================

            String message =
                    "The entered cheque count does not "
                    + "match the number of cheque objects "
                    + "parsed from the XML."
                    + "\n\n"
                    + "Entered Cheques : "
                    + mismatch.getEnteredCount()
                    + "\n"
                    + "XML Cheques     : "
                    + mismatch.getXmlCount()
                    + "\n\n"
                    + "If you continue, the batch cheque "
                    + "count will be changed to "
                    + mismatch.getXmlCount()
                    + " based on the XML."
                    + "\n\n"
                    + "Do you want to continue?";

            Messagebox.show(
                    message,
                    "Batch Count Mismatch",
                    Messagebox.YES | Messagebox.NO,
                    Messagebox.QUESTION,
                    new EventListener<Event>() {

                        @Override
                        public void onEvent(
                                Event event)
                                throws Exception {

                            // =================================
                            // CONTINUE
                            // =================================

                            if ("onYes".equals(
                                    event.getName())) {

                                continueAfterMismatch(
                                        branchCode,
                                        chequeCount,
                                        createdBy);
                            }

                            // =================================
                            // REJECT
                            // =================================

                            else {

                                Messagebox.show(
                                        "Batch rejected.\n\n"
                                        + "No batch was created.",
                                        "Batch Rejected",
                                        Messagebox.OK,
                                        Messagebox.EXCLAMATION);
                            }
                        }
                    });

        } catch (IllegalArgumentException e) {

            // =================================================
            // DUPLICATE / OTHER VALIDATION ERROR
            // =================================================

            String message =
                    e.getMessage();

            if (message == null ||
                    message.trim().isEmpty()) {

                message =
                        "Validation failed.\n\n"
                        + "Batch cannot be created.";
            }

            Messagebox.show(
                    message,
                    "Duplicate Cheque / Validation",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to capture batch.\n\n"
                    + "Error: "
                    + e.getMessage(),
                    "Capture Error",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    // =========================================================
    // CONTINUE AFTER COUNT MISMATCH
    // =========================================================

    private void continueAfterMismatch(
            String branchCode,
            Integer enteredChequeCount,
            int createdBy) {

        try {

            System.out.println(
                    "=================================");

            System.out.println(
                    "COUNT MISMATCH OVERRIDE");

            System.out.println(
                    "Operator selected CONTINUE");

            System.out.println(
                    "Entered Count: "
                    + enteredChequeCount);

            System.out.println(
                    "=================================");

            // =================================================
            // IMPORTANT
            // =================================================
            //
            // Service parses XML again.
            //
            // allowCountMismatch = TRUE
            //
            // Therefore:
            //
            // actual batch count = XML parsed count
            //
            // =================================================

            OutwardBatch batch =
                    service.captureBatch(
                            branchCode,
                            enteredChequeCount,
                            uploadedFiles,
                            createdBy,
                            true);

            showCaptureSuccess(
                    batch);

        } catch (
                CaptureOperatorBatchService
                        .ChequeCountMismatchException mismatch) {

            // This should normally not happen because
            // allowCountMismatch=true.

            Messagebox.show(
                    "Unable to continue because the XML "
                    + "cheque count changed.\n\n"
                    + "Entered Count: "
                    + mismatch.getEnteredCount()
                    + "\n"
                    + "XML Count: "
                    + mismatch.getXmlCount()
                    + "\n\n"
                    + "Batch was not created.",
                    "Count Validation",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

        } catch (IllegalArgumentException e) {

            // =================================================
            // DUPLICATE CHEQUE
            // =================================================

            String message =
                    e.getMessage();

            if (message == null ||
                    message.trim().isEmpty()) {

                message =
                        "Validation failed.\n\n"
                        + "Batch cannot be created.";
            }

            Messagebox.show(
                    message,
                    "Duplicate Cheque / Validation",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to capture batch.\n\n"
                    + "Error: "
                    + e.getMessage(),
                    "Capture Error",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    // =========================================================
    // SUCCESS MESSAGE
    // =========================================================

    private void showCaptureSuccess(
            OutwardBatch batch) {

        Messagebox.show(
                "Batch captured successfully.\n\n"
                + "Batch Number: "
                + batch.getBatchNumber()
                + "\n\n"
                + "Branch Code: "
                + batch.getBranchCode()
                + "\n\n"
                + "Total Cheques: "
                + batch.getNumberOfCheques()
                + "\n\n"
                + "Status: "
                + batch.getBatchStatus(),
                "Batch Captured",
                Messagebox.OK,
                Messagebox.INFORMATION);

        clearForm();
    }

    // =========================================================
    // CLEAR FORM
    // =========================================================

    private void clearForm() {

        branchCodeCombo
                .setSelectedItem(null);

        branchNameTextbox
                .setValue("");

        numberOfCheques
                .setValue(null);

        uploadedFiles.clear();

        selectedFilesLabel
                .setValue(
                        "No files selected.");
    }
}