package com.iispl.cts.controller.outward;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.UserSession;
import com.iispl.cts.service.outward.CaptureOperatorBatchService;

public class CaptureOperatorBatchCaptureController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    @Wire
    private Combobox branchCodeCombo;

    @Wire
    private Textbox branchNameTextbox;

    @Wire
    private Intbox numberOfCheques;

    @Wire
    private Textbox batchFolderPath;

    private CaptureOperatorBatchService service;

    // =========================================================
    // INIT
    // =========================================================

    @Override
    public void doAfterCompose(Component component)
            throws Exception {

        super.doAfterCompose(component);

        // -------------------------------------------------
        // CURRENT LOGGED-IN USER
        // -------------------------------------------------

        UserSession sessionUser =
                LoginController.getCurrentUserSession();

        if (sessionUser == null) {

            Executions.sendRedirect("/login.zul");
            return;
        }

        // Capture Operator role = 5
        if (sessionUser.getRoleId() != 5) {

            Messagebox.show(
                    "Access denied. Capture Operator access is required.",
                    "Access Denied",
                    Messagebox.OK,
                    Messagebox.ERROR);

            Executions.sendRedirect("/login.zul");
            return;
        }

        System.out.println(
                "CAPTURE OPERATOR SESSION: "
                + "userId=" + sessionUser.getUserId()
                + ", username=" + sessionUser.getUsername()
                + ", roleId=" + sessionUser.getRoleId());

        service =
                new CaptureOperatorBatchService();

        loadBranches();

        // Temporary default folder path
        batchFolderPath.setValue(
        	    "/home/iispl/Desktop"
        	);
    }

    // =========================================================
    // LOAD BRANCHES
    // =========================================================

    private void loadBranches() {

        branchCodeCombo.getItems().clear();

        try {

            List<String[]> branches =
                    service.getActiveBranches();

            if (branches == null ||
                    branches.isEmpty()) {

                System.out.println(
                        "No active branches found.");
                return;
            }

            for (String[] branch : branches) {

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

                branchCodeCombo.appendChild(
                        item);
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
                branchCodeCombo.getSelectedItem();

        if (selectedItem == null) {

            branchNameTextbox.setValue("");

            return;
        }

        String branchCode =
                selectedItem.getValue();

        String branchName =
                (String) selectedItem
                        .getAttribute("branchName");

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
                    branchCodeCombo.getSelectedItem();

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
            // FOLDER PATH
            // -------------------------------------------------

            String folderPath =
                    batchFolderPath.getValue();

            if (folderPath == null ||
                    folderPath.trim().isEmpty()) {

                Messagebox.show(
                        "Please enter the batch folder path.",
                        "Validation",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION);

                return;
            }

            folderPath =
                    folderPath.trim();

            System.out.println(
                    "Folder Path: "
                    + folderPath);

            // -------------------------------------------------
            // CURRENT LOGGED-IN USER
            // -------------------------------------------------

            UserSession sessionUser =
                    LoginController.getCurrentUserSession();

            if (sessionUser == null) {

                Messagebox.show(
                        "Your session has expired. Please login again.",
                        "Session Expired",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION);

                Executions.sendRedirect("/login.zul");
                return;
            }

            int createdBy =
                    sessionUser.getUserId();

            System.out.println(
                    "Created By (Logged-in User ID): "
                    + createdBy);

            // -------------------------------------------------
            // SERVICE
            // -------------------------------------------------

            OutwardBatch batch =
                    service.captureBatch(
                            branchCode,
                            chequeCount,
                            folderPath,
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

        branchCodeCombo.setSelectedItem(
                null);

        branchNameTextbox.setValue("");

        numberOfCheques.setValue(
                null);

        // Keep temporary default path
     // Temporary default folder path
        batchFolderPath.setValue(
                "/home/iispl/Desktop");
    }
}