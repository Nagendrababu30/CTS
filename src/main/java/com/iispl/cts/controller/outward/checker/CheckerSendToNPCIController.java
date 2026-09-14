package com.iispl.cts.controller.outward.checker;

import com.iispl.cts.dao.outward.checker.CheckerReportsDAO;
import com.iispl.cts.model.outward.OutwardBatch;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CheckerSendToNPCIController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;


    // ============================================================
    // ZUL COMPONENT
    // ============================================================

    @Wire
    private Listbox npciBatchListbox;


    // ============================================================
    // DAO
    // ============================================================

    private CheckerReportsDAO reportsDAO;


    // ============================================================
    // CURRENT USER
    // ============================================================

    private long currentUserId;


    // ============================================================
    // VALIDATION STATE
    //
    // batchNumber -> true/false
    //
    // This keeps track of which batch has been verified.
    // ============================================================

    private final Map<String, Boolean> validationStatus =
            new HashMap<String, Boolean>();


    // ============================================================
    // SEND BUTTONS
    //
    // batchNumber -> Send Button
    // ============================================================

    private final Map<String, Button> sendButtons =
            new HashMap<String, Button>();


    // ============================================================
    // STATUS LABELS
    //
    // batchNumber -> Status Label
    // ============================================================

    private final Map<String, Label> statusLabels =
            new HashMap<String, Label>();


    // ============================================================
    // INIT
    // ============================================================

    @Override
    public void doAfterCompose(
            Component component) throws Exception {

        super.doAfterCompose(component);

        System.out.println();
        System.out.println(
                "======================================"
        );
        System.out.println(
                "SEND TO NPCI CONTROLLER STARTED"
        );
        System.out.println(
                "======================================"
        );


        // ========================================================
        // GET SESSION
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
        // GET USER ID
        // ========================================================

        Object sessionUserId =
                session.getAttribute("userId");


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

            currentUserId =
                    ((Number) sessionUserId)
                            .longValue();

        } else {

            try {

                currentUserId =
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


        System.out.println(
                "CURRENT CHECKER USER ID = "
                        + currentUserId
        );


        // ========================================================
        // INITIALIZE REPORTS DAO
        // ========================================================

        reportsDAO =
                new CheckerReportsDAO();


        // ========================================================
        // LOAD BATCHES
        // ========================================================

        loadBatches();
    }


    // ============================================================
    // LOAD BATCHES
    // ============================================================

    private void loadBatches() {

        try {

            System.out.println(
                    "Loading batches ready for NPCI..."
            );


            // ====================================================
            // CLEAR UI
            // ====================================================

            if (npciBatchListbox != null) {

                npciBatchListbox
                        .getItems()
                        .clear();
            }


            // ====================================================
            // CLEAR OLD STATE
            // ====================================================

            validationStatus.clear();
            sendButtons.clear();
            statusLabels.clear();


            // ====================================================
            // GET CHECKER COMPLETED BATCHES
            // ====================================================

            List<OutwardBatch> batches =
                    reportsDAO.getCheckerCompletedBatches();


            // ====================================================
            // NO BATCHES
            // ====================================================

            if (batches == null ||
                    batches.isEmpty()) {

                System.out.println(
                        "No batches available for NPCI."
                );

                return;
            }


            System.out.println(
                    "NPCI batches found = "
                            + batches.size()
            );


            // ====================================================
            // ADD BATCH ROWS
            // ====================================================

            for (OutwardBatch batch : batches) {

                addBatchRow(batch);
            }


        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to load batches ready for NPCI.\n\n"
                            + e.getMessage(),
                    "Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }


    // ============================================================
    // ADD BATCH ROW
    // ============================================================

    private void addBatchRow(
            OutwardBatch batch) {

        if (batch == null) {

            return;
        }


        String batchNumber =
                batch.getBatchNumber();


        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return;
        }


        batchNumber =
                batchNumber.trim();


        // ========================================================
        // INITIAL VALIDATION STATE
        // ========================================================

        validationStatus.put(
                batchNumber,
                false
        );


        // ========================================================
        // GET TOTAL CHEQUE COUNT
        // ========================================================

        int totalCount =
                reportsDAO.getTotalChequeCount(
                        batchNumber
                );


        // ========================================================
        // GET VALID CHEQUE COUNT
        //
        // CHECKER_ACCEPTED = VALID
        // ========================================================

        int validCount =
                reportsDAO.getValidChequeCount(
                        batchNumber
                );


        // ========================================================
        // CREATE LIST ITEM
        // ========================================================

        Listitem item =
                new Listitem();


        // ========================================================
        // BATCH NUMBER
        // ========================================================

        Listcell batchCell =
                new Listcell();


        Label batchLabel =
                new Label(
                        batchNumber
                );


        batchLabel.setStyle(
                "font-weight:bold;"
                        + "color:#172B4D;"
        );


        batchCell.appendChild(
                batchLabel
        );


        item.appendChild(
                batchCell
        );


        // ========================================================
        // TOTAL CHEQUES
        // ========================================================

        Listcell totalCell =
                new Listcell(
                        String.valueOf(
                                totalCount
                        )
                );


        item.appendChild(
                totalCell
        );


        // ========================================================
        // VALID XML COUNT
        // ========================================================

        Listcell validXmlCell =
                new Listcell(
                        String.valueOf(
                                validCount
                        )
                );


        item.appendChild(
                validXmlCell
        );


        // ========================================================
        // VALIDATION STATUS
        // ========================================================

        Listcell validationCell =
                new Listcell();


        Label validationLabel =
                new Label(
                        "NOT VERIFIED"
                );


        validationLabel.setStyle(
                "font-weight:bold;"
                        + "color:#B42318;"
        );


        validationCell.appendChild(
                validationLabel
        );


        item.appendChild(
                validationCell
        );


        // ========================================================
        // STORE STATUS LABEL
        // ========================================================

        statusLabels.put(
                batchNumber,
                validationLabel
        );


        // ========================================================
        // ACTION CELL
        // ========================================================

        Listcell actionCell =
                new Listcell();


        Hbox actionBox =
                new Hbox();


        actionBox.setSpacing(
                "8px"
        );


        // ========================================================
        // VALIDATE BUTTON
        // ========================================================

        Button validateButton =
                new Button(
                        "Validate"
                );


        validateButton.setStyle(
                "background:#1769AA;"
                        + "color:white;"
                        + "border:none;"
                        + "border-radius:5px;"
                        + "padding:7px 14px;"
                        + "font-weight:bold;"
                        + "cursor:pointer;"
        );


        final String validateBatchNumber =
                batchNumber;


        validateButton.addEventListener(
                "onClick",
                event -> {

                    validateBatch(
                            validateBatchNumber,
                            validateButton
                    );
                }
        );


        actionBox.appendChild(
                validateButton
        );


        // ========================================================
        // SEND TO NPCI BUTTON
        // ========================================================

        Button sendButton =
                new Button(
                        "Send to NPCI"
                );


        sendButton.setStyle(
                "background:#172B4D;"
                        + "color:white;"
                        + "border:none;"
                        + "border-radius:5px;"
                        + "padding:7px 14px;"
                        + "font-weight:bold;"
                        + "cursor:pointer;"
        );


        // ========================================================
        // IMPORTANT
        //
        // SEND IS DISABLED UNTIL VALIDATION SUCCEEDS
        // ========================================================

        sendButton.setDisabled(
                true
        );


        final String selectedBatch =
                batchNumber;


        sendButton.addEventListener(
                "onClick",
                event -> {

                    sendToNPCI(
                            selectedBatch
                    );
                }
        );


        actionBox.appendChild(
                sendButton
        );


        // ========================================================
        // STORE SEND BUTTON
        // ========================================================

        sendButtons.put(
                batchNumber,
                sendButton
        );


        // ========================================================
        // ADD ACTION BOX
        // ========================================================

        actionCell.appendChild(
                actionBox
        );


        item.appendChild(
                actionCell
        );


        // ========================================================
        // ADD ROW
        // ========================================================

        npciBatchListbox.appendChild(
                item
        );
    }


    // ============================================================
    // VALIDATE BATCH
    // ============================================================

    private void validateBatch(
            String batchNumber,
            Button validateButton) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return;
        }


        batchNumber =
                batchNumber.trim();


        try {

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "VALIDATING BATCH"
            );

            System.out.println(
                    "BATCH = "
                            + batchNumber
            );

            System.out.println(
                    "USER = "
                            + currentUserId
            );

            System.out.println(
                    "======================================"
            );


            // ====================================================
            // CHECK BATCH STATUS
            // ====================================================

            boolean ready =
                    reportsDAO.isBatchReadyForNPCI(
                            batchNumber
                    );


            if (!ready) {

                validationStatus.put(
                        batchNumber,
                        false
                );


                updateValidationStatus(
                        batchNumber,
                        false
                );


                Messagebox.show(
                        "Batch "
                                + batchNumber
                                + " is not available "
                                + "for NPCI validation.\n\n"
                                + "The batch must be in "
                                + "CHECKER_COMPLETED status.",
                        "Validation Failed",
                        Messagebox.OK,
                        Messagebox.ERROR
                );


                loadBatches();

                return;
            }


            // ====================================================
            // GET COUNTS
            // ====================================================

            int totalCount =
                    reportsDAO.getTotalChequeCount(
                            batchNumber
                    );


            int validCount =
                    reportsDAO.getValidChequeCount(
                            batchNumber
                    );


            System.out.println(
                    "TOTAL CHEQUES = "
                            + totalCount
            );


            System.out.println(
                    "VALID CHEQUES = "
                            + validCount
            );


            // ====================================================
            // VALIDATION RULE
            //
            // Every cheque in the batch must be
            // CHECKER_ACCEPTED.
            //
            // If total = valid and total > 0,
            // validation succeeds.
            // ====================================================

            boolean verified =
                    validCount > 0;


            // ====================================================
            // VALIDATION SUCCESS
            // ====================================================

            if (verified) {

                validationStatus.put(
                        batchNumber,
                        true
                );


                updateValidationStatus(
                        batchNumber,
                        true
                );


                System.out.println(
                        "BATCH VALIDATION = VERIFIED"
                );


                Messagebox.show(
                        "Batch "
                                + batchNumber
                                + " has been validated successfully.\n\n"
                                + "Validation Status: VERIFIED",
                        "Validation Successful",
                        Messagebox.OK,
                        Messagebox.INFORMATION
                );


                return;
            }


            // ====================================================
            // VALIDATION FAILED
            // ====================================================

            validationStatus.put(
                    batchNumber,
                    false
            );


            updateValidationStatus(
                    batchNumber,
                    false
            );


            System.out.println(
                    "BATCH VALIDATION = NOT VERIFIED"
            );


            Messagebox.show(
                    "Batch "
                            + batchNumber
                            + " could not be verified.\n\n"
                            + "Total Cheques: "
                            + totalCount
                            + "\n"
                            + "Valid Cheques: "
                            + validCount
                            + "\n\n"
                            + "Send to NPCI remains disabled.",
                    "Validation Failed",
                    Messagebox.OK,
                    Messagebox.ERROR
            );


        } catch (Exception e) {

            e.printStackTrace();


            validationStatus.put(
                    batchNumber,
                    false
            );


            updateValidationStatus(
                    batchNumber,
                    false
            );


            Messagebox.show(
                    "Error while validating batch "
                            + batchNumber
                            + ".\n\n"
                            + e.getMessage(),
                    "Validation Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }


    // ============================================================
    // UPDATE VALIDATION STATUS IN UI
    // ============================================================

    private void updateValidationStatus(
            String batchNumber,
            boolean verified) {


        Label statusLabel =
                statusLabels.get(
                        batchNumber
                );


        Button sendButton =
                sendButtons.get(
                        batchNumber
                );


        if (verified) {

            // ====================================================
            // VERIFIED
            // ====================================================

            if (statusLabel != null) {

                statusLabel.setValue(
                        "VERIFIED"
                );


                statusLabel.setStyle(
                        "font-weight:bold;"
                                + "color:#16803A;"
                );
            }


            // ====================================================
            // ENABLE SEND BUTTON
            // ====================================================

            if (sendButton != null) {

                sendButton.setDisabled(
                        false
                );
            }

        } else {

            // ====================================================
            // NOT VERIFIED
            // ====================================================

            if (statusLabel != null) {

                statusLabel.setValue(
                        "NOT VERIFIED"
                );


                statusLabel.setStyle(
                        "font-weight:bold;"
                                + "color:#B42318;"
                );
            }


            // ====================================================
            // DISABLE SEND BUTTON
            // ====================================================

            if (sendButton != null) {

                sendButton.setDisabled(
                        true
                );
            }
        }
    }


    // ============================================================
    // SEND TO NPCI
    // ============================================================

    private void sendToNPCI(
            String batchNumber) {


        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return;
        }


        batchNumber =
                batchNumber.trim();


        // ========================================================
        // DOUBLE CHECK VALIDATION
        //
        // Even if somebody tries to trigger the button manually,
        // do not allow an unverified batch to be sent.
        // ========================================================

        Boolean verified =
                validationStatus.get(
                        batchNumber
                );


        if (verified == null ||
                !verified) {

            Messagebox.show(
                    "Batch "
                            + batchNumber
                            + " is not verified.\n\n"
                            + "Please click Validate first.",
                    "Batch Not Verified",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION
            );

            return;
        }


        System.out.println(
                "======================================"
        );


        System.out.println(
                "NPCI SEND REQUEST"
        );


        System.out.println(
                "BATCH = "
                        + batchNumber
        );


        System.out.println(
                "USER = "
                        + currentUserId
        );


        System.out.println(
                "======================================"
        );


        try {

            // ====================================================
            // CHECK CURRENT DB STATUS AGAIN
            // ====================================================

            boolean ready =
                    reportsDAO.isBatchReadyForNPCI(
                            batchNumber
                    );


            if (!ready) {

                Messagebox.show(
                        "This batch is no longer available "
                                + "for NPCI submission.\n\n"
                                + "The batch status may have changed.",
                        "Batch Not Available",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION
                );


                loadBatches();

                return;
            }


            // ====================================================
            // CONFIRMATION
            // ====================================================

            final String confirmedBatchNumber =
                    batchNumber;


            Messagebox.show(
                    "Batch "
                            + confirmedBatchNumber
                            + " has been VERIFIED.\n\n"
                            + "Are you sure you want to send "
                            + "this batch to NPCI?",
                    "Confirm NPCI Submission",
                    Messagebox.YES | Messagebox.NO,
                    Messagebox.QUESTION,
                    event -> {

                        if (Messagebox.ON_YES.equals(
                                event.getName())) {

                            submitBatch(
                                    confirmedBatchNumber
                            );
                        }
                    }
            );


        } catch (Exception e) {

            e.printStackTrace();


            Messagebox.show(
                    "Error while preparing batch "
                            + batchNumber
                            + " for NPCI.\n\n"
                            + e.getMessage(),
                    "NPCI Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }


    // ============================================================
    // SUBMIT BATCH
    // ============================================================

    private void submitBatch(
            String batchNumber) {


        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return;
        }


        batchNumber =
                batchNumber.trim();


        try {

            System.out.println(
                    "Submitting batch "
                            + batchNumber
                            + " to NPCI..."
            );


            System.out.println(
                    "SUBMISSION USER ID = "
                            + currentUserId
            );


            // ====================================================
            // FINAL VALIDATION CHECK
            // ====================================================

            Boolean verified =
                    validationStatus.get(
                            batchNumber
                    );


            if (verified == null ||
                    !verified) {

                Messagebox.show(
                        "Batch "
                                + batchNumber
                                + " is not verified.\n\n"
                                + "The batch cannot be sent to NPCI.",
                        "Submission Blocked",
                        Messagebox.OK,
                        Messagebox.ERROR
                );

                return;
            }


            // ====================================================
            // FINAL DATABASE STATUS CHECK
            // ====================================================

            boolean ready =
                    reportsDAO.isBatchReadyForNPCI(
                            batchNumber
                    );


            if (!ready) {

                Messagebox.show(
                        "Batch "
                                + batchNumber
                                + " is no longer in "
                                + "CHECKER_COMPLETED status.\n\n"
                                + "Submission cancelled.",
                        "Submission Blocked",
                        Messagebox.OK,
                        Messagebox.ERROR
                );


                loadBatches();

                return;
            }


            // ====================================================
            // CURRENT PROJECT BEHAVIOUR
            //
            // There is no external NPCI API integration currently
            // connected to this screen.
            //
            // Therefore the existing application behaviour is to
            // update the batch status to NPCI_SENT.
            // ====================================================

            boolean success =
                    reportsDAO.markBatchAsNPCISent(
                            batchNumber
                    );


            // ====================================================
            // UPDATE FAILED
            // ====================================================

            if (!success) {

                Messagebox.show(
                        "Unable to update batch status.\n\n"
                                + "Batch "
                                + batchNumber
                                + " was NOT marked as "
                                + "NPCI_SENT.",
                        "NPCI Submission Failed",
                        Messagebox.OK,
                        Messagebox.ERROR
                );


                return;
            }


            // ====================================================
            // SUCCESS
            // ====================================================

            System.out.println(
                    "======================================"
            );


            System.out.println(
                    "NPCI SUBMISSION SUCCESS"
            );


            System.out.println(
                    "BATCH NUMBER = "
                            + batchNumber
            );


            System.out.println(
                    "USER ID = "
                            + currentUserId
            );


            System.out.println(
                    "BATCH STATUS = NPCI_SENT"
            );


            System.out.println(
                    "======================================"
            );


            // ====================================================
            // REMOVE VALIDATION STATE
            // ====================================================

            validationStatus.remove(
                    batchNumber
            );


            sendButtons.remove(
                    batchNumber
            );


            statusLabels.remove(
                    batchNumber
            );


            // ====================================================
            // SUCCESS MESSAGE
            // ====================================================

            Messagebox.show(
                    "Batch "
                            + batchNumber
                            + " sent to NPCI successfully.",
                    "NPCI Submission Successful",
                    Messagebox.OK,
                    Messagebox.INFORMATION
            );


            // ====================================================
            // REFRESH SCREEN
            // ====================================================

            loadBatches();


        } catch (Exception e) {

            e.printStackTrace();


            Messagebox.show(
                    "Error while sending batch "
                            + batchNumber
                            + " to NPCI.\n\n"
                            + e.getMessage(),
                    "NPCI Submission Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }
}