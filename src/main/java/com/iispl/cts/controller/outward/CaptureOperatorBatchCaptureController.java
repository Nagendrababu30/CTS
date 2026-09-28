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
import com.cts.admin.service.SessionService;
import com.cts.admin.service.SessionServiceImpl;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.service.outward.CaptureOperatorBatchService;

public class CaptureOperatorBatchCaptureController extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

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

    private CaptureOperatorBatchService service;
    private SessionService sessionService;
    private final List<Media> uploadedFiles = new ArrayList<>();

    @Override
    public void doAfterCompose(Component component) throws Exception {
        super.doAfterCompose(component);

        Session session = Executions.getCurrent().getSession();
        if (session == null) {
            Executions.sendRedirect("/login.zul");
            return;
        }

        Object sessionUserId = session.getAttribute("userId");
        if (sessionUserId == null) {
            Executions.sendRedirect("/login.zul");
            return;
        }

        long userId;
        if (sessionUserId instanceof Number) {
            userId = ((Number) sessionUserId).longValue();
        } else {
            try {
                userId = Long.parseLong(sessionUserId.toString());
            } catch (NumberFormatException e) {
                Executions.sendRedirect("/login.zul");
                return;
            }
        }

        System.out.println("CAPTURE OPERATOR SESSION: userId=" + userId);

        sessionService = new SessionServiceImpl();
        com.cts.admin.model.Session clearingSession = sessionService.getActiveSession();

        if (clearingSession == null ||
            clearingSession.getStatus() == null ||
            !"STARTED".equalsIgnoreCase(clearingSession.getStatus().trim())) {

            Messagebox.show(
                "Clearing session is not started.\n\nCapture Operator operations are currently unavailable.",
                "Session Not Started",
                Messagebox.OK,
                Messagebox.EXCLAMATION,
                event -> {
                    if (Messagebox.ON_OK.equals(event.getName())) {
                        Executions.sendRedirect("/login.zul");
                    }
                }
            );
            return;
        }

        System.out.println(
            "CAPTURE OPERATOR CLEARING SESSION: sessionName=" +
            clearingSession.getSessionName() +
            ", status=" +
            clearingSession.getStatus()
        );

        service = new CaptureOperatorBatchService();
        loadBranches();
    }

    private void loadBranches() {
        branchCodeCombo.getItems().clear();

        try {
            List<String[]> branches = service.getActiveBranches();

            if (branches == null || branches.isEmpty()) {
                System.out.println("No active branches found.");
                return;
            }

            for (String[] branch : branches) {
                if (branch == null || branch.length < 2) {
                    continue;
                }

                Comboitem item = new Comboitem();
                item.setLabel(branch[0]);
                item.setValue(branch[0]);
                item.setAttribute("branchName", branch[1]);
                branchCodeCombo.appendChild(item);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Messagebox.show(
                "Unable to load branches from database.\n\n" + e.getMessage(),
                "Database Error",
                Messagebox.OK,
                Messagebox.ERROR
            );
        }
    }

    @Listen("onSelect = #branchCodeCombo")
    public void onBranchSelected() {
        Comboitem selectedItem = branchCodeCombo.getSelectedItem();

        if (selectedItem == null) {
            branchNameTextbox.setValue("");
            return;
        }

        String branchCode = selectedItem.getValue();
        String branchName = (String) selectedItem.getAttribute("branchName");

        if (branchName == null || branchName.trim().isEmpty()) {
            branchName = service.getBranchName(branchCode);
        }

        branchNameTextbox.setValue(branchName == null ? "" : branchName);
    }

    @Listen("onUpload = #batchFilesUpload")
    public void onBatchFilesUpload(UploadEvent event) {
        try {
            Media media = event.getMedia();

            if (media == null) {
                selectedFilesLabel.setValue("No file selected.");
                return;
            }

            String fileName = media.getName();

            if (fileName == null || fileName.trim().isEmpty()) {
                Messagebox.show(
                    "Invalid file selected.",
                    "Upload Error",
                    Messagebox.OK,
                    Messagebox.ERROR
                );
                return;
            }

            for (Media existing : uploadedFiles) {
                if (existing != null &&
                    existing.getName() != null &&
                    existing.getName().equalsIgnoreCase(fileName)) {

                    Messagebox.show(
                        "File already selected:\n\n" + fileName,
                        "Duplicate File",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION
                    );
                    return;
                }
            }

            if (fileName.toLowerCase().endsWith(".xml")) {
                for (Media existing : uploadedFiles) {
                    if (existing != null &&
                        existing.getName() != null &&
                        existing.getName().toLowerCase().endsWith(".xml")) {

                        Messagebox.show(
                            "Only one XML file is allowed.",
                            "XML Validation",
                            Messagebox.OK,
                            Messagebox.EXCLAMATION
                        );
                        return;
                    }
                }
            }

            uploadedFiles.add(media);

            int xmlCount = 0;
            int imageCount = 0;

            for (Media selectedMedia : uploadedFiles) {
                if (selectedMedia == null || selectedMedia.getName() == null) {
                    continue;
                }

                String lowerName = selectedMedia.getName().toLowerCase();

                if (lowerName.endsWith(".xml")) {
                    xmlCount++;
                } else if (
                    lowerName.endsWith(".jpg") ||
                    lowerName.endsWith(".jpeg") ||
                    lowerName.endsWith(".png") ||
                    lowerName.endsWith(".tif") ||
                    lowerName.endsWith(".tiff") ||
                    lowerName.endsWith(".bmp")) {
                    imageCount++;
                }
            }

            selectedFilesLabel.setValue(
                "Files selected: " + uploadedFiles.size() +
                " | XML: " + xmlCount +
                " | Images: " + imageCount
            );

            System.out.println("FILE ADDED: " + fileName);

        } catch (Exception e) {
            e.printStackTrace();
            Messagebox.show(
                "Unable to process selected file.\n\n" + e.getMessage(),
                "Upload Error",
                Messagebox.OK,
                Messagebox.ERROR
            );
        }
    }

    @Listen("onClick = #capturedBatchButton")
    public void captureBatch() {
        Comboitem selectedItem = branchCodeCombo.getSelectedItem();

        if (selectedItem == null) {
            Messagebox.show(
                "Please select branch code.",
                "Validation",
                Messagebox.OK,
                Messagebox.EXCLAMATION
            );
            return;
        }

        final String branchCode = selectedItem.getValue();
        final Integer chequeCount = numberOfCheques.getValue();

        if (chequeCount == null || chequeCount <= 0) {
            Messagebox.show(
                "Please enter a valid number of cheques.",
                "Validation",
                Messagebox.OK,
                Messagebox.EXCLAMATION
            );
            return;
        }

        if (uploadedFiles.isEmpty()) {
            Messagebox.show(
                "Please select the XML file and cheque images.",
                "Validation",
                Messagebox.OK,
                Messagebox.EXCLAMATION
            );
            return;
        }

        int xmlCount = 0;

        for (Media media : uploadedFiles) {
            if (media != null &&
                media.getName() != null &&
                media.getName().toLowerCase().endsWith(".xml")) {
                xmlCount++;
            }
        }

        if (xmlCount == 0) {
            Messagebox.show(
                "No XML file was selected.",
                "Validation",
                Messagebox.OK,
                Messagebox.EXCLAMATION
            );
            return;
        }

        if (xmlCount > 1) {
            Messagebox.show(
                "Please select only one XML file.",
                "Validation",
                Messagebox.OK,
                Messagebox.EXCLAMATION
            );
            return;
        }

        Session session = Executions.getCurrent().getSession();

        if (session == null) {
            Messagebox.show(
                "Your session has expired. Please login again.",
                "Session Expired",
                Messagebox.OK,
                Messagebox.EXCLAMATION
            );
            Executions.sendRedirect("/login.zul");
            return;
        }

        Object sessionUserId = session.getAttribute("userId");

        if (sessionUserId == null) {
            Messagebox.show(
                "Your session has expired. Please login again.",
                "Session Expired",
                Messagebox.OK,
                Messagebox.EXCLAMATION
            );
            Executions.sendRedirect("/login.zul");
            return;
        }

        final int createdBy;

        try {
            long userId;

            if (sessionUserId instanceof Number) {
                userId = ((Number) sessionUserId).longValue();
            } else {
                userId = Long.parseLong(sessionUserId.toString());
            }

            createdBy = Math.toIntExact(userId);

        } catch (Exception e) {
            Messagebox.show(
                "Invalid user session. Please login again.",
                "Session Error",
                Messagebox.OK,
                Messagebox.EXCLAMATION
            );
            Executions.sendRedirect("/login.zul");
            return;
        }

        try {
            OutwardBatch batch = service.captureBatch(
                branchCode,
                chequeCount,
                uploadedFiles,
                createdBy,
                false
            );

            showCaptureSuccess(batch);

        } catch (CaptureOperatorBatchService.ChequeCountMismatchException mismatch) {

            String message =
                "The entered cheque count does not match the number of cheque objects parsed from the XML." +
                "\n\nEntered Cheques : " + mismatch.getEnteredCount() +
                "\nXML Cheques     : " + mismatch.getXmlCount() +
                "\n\nIf you continue, the batch cheque count will be changed to " +
                mismatch.getXmlCount() +
                " based on the XML." +
                "\n\nDo you want to continue?";

            Messagebox.show(
                message,
                "Batch Count Mismatch",
                Messagebox.YES | Messagebox.NO,
                Messagebox.QUESTION,
                new EventListener<Event>() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        if ("onYes".equals(event.getName())) {
                            continueAfterMismatch(
                                branchCode,
                                chequeCount,
                                createdBy
                            );
                        } else {
                            Messagebox.show(
                                "Batch rejected.\n\nNo batch was created.",
                                "Batch Rejected",
                                Messagebox.OK,
                                Messagebox.EXCLAMATION
                            );
                        }
                    }
                }
            );

        } catch (IllegalArgumentException e) {

            Messagebox.show(
                "Duplicate cheque found.\n\nBatch cannot be created.",
                "Duplicate Cheque",
                Messagebox.OK,
                Messagebox.EXCLAMATION
            );

        } catch (Exception e) {
            e.printStackTrace();

            Messagebox.show(
                "Unable to capture batch.\n\nError: " + e.getMessage(),
                "Capture Error",
                Messagebox.OK,
                Messagebox.ERROR
            );
        }
    }

    private void continueAfterMismatch(
        String branchCode,
        Integer enteredChequeCount,
        int createdBy) {

        try {
            System.out.println("COUNT MISMATCH OVERRIDE");
            System.out.println("Operator selected CONTINUE");
            System.out.println("Entered Count: " + enteredChequeCount);

            OutwardBatch batch = service.captureBatch(
                branchCode,
                enteredChequeCount,
                uploadedFiles,
                createdBy,
                true
            );

            showCaptureSuccess(batch);

        } catch (CaptureOperatorBatchService.ChequeCountMismatchException mismatch) {

            Messagebox.show(
                "Unable to continue because the XML cheque count changed.\n\n" +
                "Entered Count: " + mismatch.getEnteredCount() +
                "\nXML Count: " + mismatch.getXmlCount() +
                "\n\nBatch was not created.",
                "Count Validation",
                Messagebox.OK,
                Messagebox.EXCLAMATION
            );

        } catch (IllegalArgumentException e) {

            Messagebox.show(
                "Duplicate cheque found.\n\nBatch cannot be created.",
                "Duplicate Cheque",
                Messagebox.OK,
                Messagebox.EXCLAMATION
            );

        } catch (Exception e) {
            e.printStackTrace();

            Messagebox.show(
                "Unable to capture batch.\n\nError: " + e.getMessage(),
                "Capture Error",
                Messagebox.OK,
                Messagebox.ERROR
            );
        }
    }

    private void showCaptureSuccess(OutwardBatch batch) {
        Messagebox.show(
            "Batch captured successfully.\n\n" +
            "Batch Number: " + batch.getBatchNumber() +
            "\n\nBranch Code: " + batch.getBranchCode() +
            "\n\nTotal Cheques: " + batch.getNumberOfCheques() +
            "\n\nStatus: " + batch.getBatchStatus(),
            "Batch Captured",
            Messagebox.OK,
            Messagebox.INFORMATION,
            event -> {
                if (Messagebox.ON_OK.equals(event.getName())) {
                    Executions.sendRedirect(
                        "/zul/outward/outward-maker/capture-operator-captured-batches.zul"
                    );
                }
            }
        );
    }
}