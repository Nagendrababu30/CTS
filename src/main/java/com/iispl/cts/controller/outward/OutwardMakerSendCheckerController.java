package com.iispl.cts.controller.outward;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;

import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.service.outward.OutwardMakerSendCheckerService;

public class OutwardMakerSendCheckerController extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    @Wire
    private Listbox batchListbox;

    private OutwardMakerSendCheckerService service;

    private int currentUserId;

    // =========================================================
    // PAGE LOAD
    // =========================================================

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        // 1. Validate active session
        if (!LoginController.isLoggedIn()) {
            Executions.sendRedirect("/login.zul");
            return;
        }

        // 2. Fetch logged-in user integer ID
        this.currentUserId = LoginController.getCurrentUserId();
        System.out.println("user"+currentUserId);

        // 3. Initialize service
        this.service = new OutwardMakerSendCheckerService();

        // 4. Load batches
        loadReadyBatches();
    }

    // =========================================================
    // LOAD READY FOR CHECKER BATCHES
    // =========================================================

    private void loadReadyBatches() {
        try {
            if (batchListbox == null) {
                return;
            }

            // Retrieve batches in status 'READY_TO_SUBMIT' assigned to this maker
            List<OutwardBatch> batches = service.getReadyBatches(currentUserId);

            ListModelList<OutwardBatch> model = new ListModelList<>(batches);

            batchListbox.setItemRenderer((Listitem item, OutwardBatch batch, int index) -> {
                renderBatchRow(item, batch);
            });

            batchListbox.setModel(model);

        } catch (Exception e) {
            e.printStackTrace();
            Messagebox.show(
                "Unable to load batches ready for Checker.\n" + e.getMessage(),
                "Error",
                Messagebox.OK,
                Messagebox.ERROR
            );
        }
    }

    // =========================================================
    // RENDER ONE BATCH ROW
    // =========================================================

    private void renderBatchRow(Listitem item, OutwardBatch batch) {
        // 1. Batch Number
        Listcell batchIdCell = new Listcell();
        Label batchIdLabel = new Label(safeValue(batch.getBatchNumber()));
        batchIdLabel.setStyle("font-weight:bold; color:#122B49;");
        batchIdCell.appendChild(batchIdLabel);
        item.appendChild(batchIdCell);

        // 2. Total Cheques
        Listcell chequeCell = new Listcell();
        int cheques = batch.getNumberOfCheques() != null ? batch.getNumberOfCheques() : 0;
        Label chequeLabel = new Label(String.valueOf(cheques));
        chequeCell.appendChild(chequeLabel);
        item.appendChild(chequeCell);

        // 3. Status Badge
        Listcell statusCell = new Listcell();
        Label statusLabel = new Label("✓ Ready to Submit");
        statusLabel.setStyle(
            "background:#D1FADF;"
            + "color:#039855;"
            + "padding:6px 12px;"
            + "border-radius:4px;"
            + "font-weight:bold;"
            + "display:inline-block;"
        );
        statusCell.appendChild(statusLabel);
        item.appendChild(statusCell);

        // 4. Action Button
        Listcell actionCell = new Listcell();
        Button sendButton = new Button("Send to Checker");
        sendButton.setWidth("150px");
        sendButton.setStyle(
            "background:#2457D6;"
            + "color:white;"
            + "border:none;"
            + "padding:6px 12px;"
            + "cursor:pointer;"
            + "font-weight:bold;"
        );

        sendButton.addEventListener(Events.ON_CLICK, event -> {
            sendToChecker(batch.getBatchNumber());
        });

        actionCell.appendChild(sendButton);
        item.appendChild(actionCell);
    }

    // =========================================================
    // SEND TO CHECKER - CONFIRMATION
    // =========================================================

    private void sendToChecker(String batchNumber) {
        if (batchNumber == null || batchNumber.trim().isEmpty()) {
            Messagebox.show("Invalid batch number.", "Error", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        Messagebox.show(
            "Are you sure you want to send batch " + batchNumber + " to Checker?",
            "Confirm Submission",
            Messagebox.YES | Messagebox.NO,
            Messagebox.QUESTION,
            event -> {
                if (Messagebox.ON_YES.equals(event.getName())) {
                    processSend(batchNumber);
                }
            }
        );
    }

    // =========================================================
    // ACTUAL SEND OPERATION
    // =========================================================

    private void processSend(String batchNumber) {
        try {
            boolean success = service.sendToChecker(batchNumber, currentUserId);

            if (success) {
                Messagebox.show(
                    "Batch " + batchNumber + " has been successfully sent to Checker.",
                    "Success",
                    Messagebox.OK,
                    Messagebox.INFORMATION
                );
            } else {
                Messagebox.show(
                    "Batch " + batchNumber + " could not be sent.\nIt may already be dispatched or not assigned to you.",
                    "Send Failed",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION
                );
            }

            // Refresh table
            loadReadyBatches();

        } catch (Exception e) {
            e.printStackTrace();
            Messagebox.show(
                "Error while sending batch to Checker.\n" + e.getMessage(),
                "Error",
                Messagebox.OK,
                Messagebox.ERROR
            );
        }
    }

    private String safeValue(String value) {
        return (value == null || value.trim().isEmpty()) ? "-" : value;
    }
}