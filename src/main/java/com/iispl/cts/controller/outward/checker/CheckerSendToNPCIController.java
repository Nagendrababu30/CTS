package com.iispl.cts.controller.outward.checker;

import com.iispl.cts.dao.outward.checker.CheckerReportsDAO;
import com.iispl.cts.dao.outward.checker.CheckerSendToNPCIDAO;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;

import java.util.List;

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

    private CheckerSendToNPCIDAO npciDAO;

    private CheckerReportsDAO reportsDAO;

    // ============================================================
    // CURRENT USER
    // ============================================================

    private long currentUserId;

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

        // ========================================
        // GET ZK SESSION
        // ========================================

        Session session =
                Executions.getCurrent().getSession();

        // ========================================
        // NO SESSION
        // ========================================

        if (session == null) {

            Executions.sendRedirect(
                    "/zul/login.zul"
            );

            return;
        }

        // ========================================
        // GET USER ID FROM SESSION
        // ========================================

        Object sessionUserId =
                session.getAttribute("userId");

        // ========================================
        // USER ID NOT FOUND
        // ========================================

        if (sessionUserId == null) {

            Executions.sendRedirect(
                    "/zul/login.zul"
            );

            return;
        }

        // ========================================
        // CONVERT USER ID
        // ========================================

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

        // ========================================
        // LOG CURRENT USER
        // ========================================

        System.out.println(
                "CURRENT CHECKER USER ID = "
                        + currentUserId
        );

        // ========================================
        // INITIALIZE DAOS
        // ========================================

        npciDAO =
                new CheckerSendToNPCIDAO();

        reportsDAO =
                new CheckerReportsDAO();

        // ========================================
        // LOAD BATCHES
        // ========================================

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

            if (npciBatchListbox != null) {

                npciBatchListbox
                        .getItems()
                        .clear();
            }

            List<OutwardBatch> batches =
                    npciDAO.getBatchesReadyForNPCI();

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
        // GET ALL CHEQUES
        // ========================================================

        List<OutwardCheque> allCheques =
                reportsDAO.getBatchCheques(
                        batchNumber
                );

        // ========================================================
        // GET REJECTED CHEQUES
        // ========================================================

        List<OutwardCheque> rejectedCheques =
                reportsDAO.getRejectedCheques(
                        batchNumber
                );

        // ========================================================
        // COUNTS
        // ========================================================

        int totalCount =
                allCheques == null
                        ? 0
                        : allCheques.size();

        int rejectedCount =
                rejectedCheques == null
                        ? 0
                        : rejectedCheques.size();

        int acceptedCount =
                totalCount - rejectedCount;

        if (acceptedCount < 0) {

            acceptedCount = 0;
        }

        // ========================================================
        // CREATE LIST ITEM
        // ========================================================

        Listitem item =
                new Listitem();

        // ========================================================
        // BATCH ID
        // ========================================================

        Listcell batchCell =
                new Listcell();

        Label batchLabel =
                new Label(batchNumber);

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
        // ACCEPTED
        // ========================================================

        Listcell acceptedCell =
                new Listcell(
                        String.valueOf(
                                acceptedCount
                        )
                );

        item.appendChild(
                acceptedCell
        );

        // ========================================================
        // REJECTED
        // ========================================================

        Listcell rejectedCell =
                new Listcell(
                        String.valueOf(
                                rejectedCount
                        )
                );

        item.appendChild(
                rejectedCell
        );

        // ========================================================
        // STATUS
        // ========================================================

        Listcell statusCell =
                new Listcell();

        Label statusLabel =
                new Label(
                        "READY FOR NPCI"
                );

        statusLabel.setStyle(
                "font-weight:bold;"
                        + "color:#1769AA;"
        );

        statusCell.appendChild(
                statusLabel
        );

        item.appendChild(
                statusCell
        );

        // ========================================================
        // ACTION
        // ========================================================

        Listcell actionCell =
                new Listcell();

        Button sendButton =
                new Button(
                        "Send to NPCI"
                );

        sendButton.setStyle(
                "background:#172B4D;"
                        + "color:white;"
                        + "border:none;"
                        + "border-radius:5px;"
                        + "padding:8px 16px;"
                        + "font-weight:bold;"
                        + "cursor:pointer;"
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

        actionCell.appendChild(
                sendButton
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
    // SEND TO NPCI
    // ============================================================

    private void sendToNPCI(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            Messagebox.show(
                    "Batch number is required.",
                    "Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );

            return;
        }

        batchNumber =
                batchNumber.trim();

        System.out.println(
                "NPCI SEND REQUEST"
                        + " | Batch = "
                        + batchNumber
                        + " | User = "
                        + currentUserId
        );

        // ========================================================
        // CHECK CURRENT BATCH STATUS
        // ========================================================

        boolean ready =
                npciDAO.isBatchReadyForNPCI(
                        batchNumber
                );

        if (!ready) {

            Messagebox.show(
                    "This batch is no longer available "
                            + "for NPCI submission.",
                    "Batch Not Available",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION
            );

            loadBatches();

            return;
        }

        // ========================================================
        // GET CHEQUES
        // ========================================================

        List<OutwardCheque> cheques =
                reportsDAO.getBatchCheques(
                        batchNumber
                );

        if (cheques == null ||
                cheques.isEmpty()) {

            Messagebox.show(
                    "No cheques found for batch "
                            + batchNumber
                            + ".\n\n"
                            + "The batch cannot be sent to NPCI.",
                    "Cannot Send",
                    Messagebox.OK,
                    Messagebox.ERROR
            );

            return;
        }

        // ========================================================
        // CONFIRMATION
        // ========================================================

        final String confirmedBatchNumber =
                batchNumber;

        Messagebox.show(
                "Are you sure you want to send batch "
                        + confirmedBatchNumber
                        + " to NPCI?",
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
    }

    // ============================================================
    // SUBMIT BATCH
    // ============================================================

    private void submitBatch(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            Messagebox.show(
                    "Batch number is required.",
                    "Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );

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
            // ACTUAL NPCI SUBMISSION
            // ====================================================
            //
            // At present, the project does not have an external
            // NPCI integration service supplied for this screen.
            //
            // Once the actual NPCI integration is available,
            // call that service here.
            //
            // IMPORTANT:
            //
            // markBatchAsNPCISent() must execute only after the
            // actual NPCI submission succeeds.
            //
            // ====================================================

            boolean success =
                    npciDAO.markBatchAsNPCISent(
                            batchNumber
                    );

            // ====================================================
            // STATUS UPDATE FAILED
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

            Messagebox.show(
                    "Batch "
                            + batchNumber
                            + " sent to NPCI successfully.",
                    "NPCI Submission Successful",
                    Messagebox.OK,
                    Messagebox.INFORMATION
            );

            // ====================================================
            // REFRESH CURRENT NPCI SCREEN
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