package com.iispl.cts.controller.outward;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.util.media.Media;

import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Fileupload;

import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.service.outward.CaptureOperatorBatchService;

public class CaptureOperatorBatchCaptureController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    // =========================================================
    // ZUL COMPONENTS
    // =========================================================

    @Wire
    private Combobox branchCodeCombo;

    @Wire
    private org.zkoss.zul.Textbox branchNameTextbox;

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

    /*
     * Important:
     *
     * Your ZK Fileupload supports only ONE file per upload.
     *
     * Therefore every time the user selects a file,
     * we ADD it to this list.
     *
     * We DO NOT clear the list during upload.
     */

    private final List<Media> uploadedFiles =
            new ArrayList<>();


    // =========================================================
    // INIT
    // =========================================================

    @Override
    public void doAfterCompose(Component component)
            throws Exception {

        super.doAfterCompose(component);

        // -----------------------------------------------------
        // CURRENT SESSION
        // -----------------------------------------------------

        Session session =
                Executions.getCurrent().getSession();

        if (session == null) {

            Executions.sendRedirect(
                    "/zul/login.zul");

            return;
        }


        Object sessionUserId =
                session.getAttribute("userId");

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


        // -----------------------------------------------------
        // SERVICE
        // -----------------------------------------------------

        service =
                new CaptureOperatorBatchService();


        // -----------------------------------------------------
        // LOAD BRANCHES
        // -----------------------------------------------------

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

            // -------------------------------------------------
            // GET ONE FILE
            // -------------------------------------------------

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


            // -------------------------------------------------
            // CHECK DUPLICATE FILE
            // -------------------------------------------------

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


            // -------------------------------------------------
            // CHECK XML COUNT
            // -------------------------------------------------

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


            // -------------------------------------------------
            // ADD FILE
            // -------------------------------------------------

            uploadedFiles.add(
                    media);


            // -------------------------------------------------
            // COUNT FILES
            // -------------------------------------------------

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


            // -------------------------------------------------
            // UPDATE LABEL
            // -------------------------------------------------

            selectedFilesLabel.setValue(
                    "Files selected: "
                    + uploadedFiles.size()
                    + " | XML: "
                    + xmlCount
                    + " | Images: "
                    + imageCount);


            // -------------------------------------------------
            // LOG
            // -------------------------------------------------

            System.out.println(
                    "=================================");

            System.out.println(
                    "FILE ADDED");

            System.out.println(
                    "File: "
                    + fileName);

            System.out.println(
                    "Total files: "
                    + uploadedFiles.size());

            System.out.println(
                    "XML files: "
                    + xmlCount);

            System.out.println(
                    "Image files: "
                    + imageCount);

            System.out.println(
                    "=================================");


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

        try {

            System.out.println(
                    "=================================");

            System.out.println(
                    "CAPTURE BATCH BUTTON CLICKED");

            System.out.println(
                    "=================================");


            // -------------------------------------------------
            // BRANCH
            // -------------------------------------------------

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


            String branchCode =
                    selectedItem.getValue();


            // -------------------------------------------------
            // CHEQUE COUNT
            // -------------------------------------------------

            Integer chequeCount =
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


            // -------------------------------------------------
            // FILE VALIDATION
            // -------------------------------------------------

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


            // -------------------------------------------------
            // SESSION
            // -------------------------------------------------

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


            long userId;


            if (sessionUserId instanceof Number) {

                userId =
                        ((Number) sessionUserId)
                                .longValue();

            } else {

                try {

                    userId =
                            Long.parseLong(
                                    sessionUserId
                                            .toString());

                } catch (
                        NumberFormatException e) {

                    Messagebox.show(
                            "Invalid user session. Please login again.",
                            "Session Error",
                            Messagebox.OK,
                            Messagebox.EXCLAMATION);

                    Executions.sendRedirect(
                            "/zul/login.zul");

                    return;
                }
            }


            int createdBy =
                    Math.toIntExact(
                            userId);


            // -------------------------------------------------
            // SERVICE
            // -------------------------------------------------

            OutwardBatch batch =
                    service.captureBatch(
                            branchCode,
                            chequeCount,
                            uploadedFiles,
                            createdBy);


            // -------------------------------------------------
            // SUCCESS
            // -------------------------------------------------

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


            // -------------------------------------------------
            // CLEAR
            // -------------------------------------------------

            clearForm();


        } catch (IllegalArgumentException e) {

            Messagebox.show(
                    e.getMessage(),
                    "Validation",
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