package com.iispl.cts.controller.outward;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
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

public class OutwardMakerSendCheckerController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    @Wire
    private Listbox batchListbox;

    private OutwardMakerSendCheckerService service;

    private long currentUserId;

    // =========================================================
    // PAGE LOAD
    // =========================================================

    @Override
    public void doAfterCompose(Component comp)
            throws Exception {

        super.doAfterCompose(comp);

        // =====================================================
        // GET ZK SESSION
        // =====================================================

        Session session =
                Executions.getCurrent().getSession();

        // =====================================================
        // NO SESSION
        // =====================================================

        if (session == null) {

            Executions.sendRedirect(
                    "/zul/login.zul"
            );

            return;
        }

        // =====================================================
        // GET USER ID FROM SESSION
        // =====================================================

        Object sessionUserId =
                session.getAttribute("userId");

        // =====================================================
        // USER ID NOT FOUND
        // =====================================================

        if (sessionUserId == null) {

            Executions.sendRedirect(
                    "/zul/login.zul"
            );

            return;
        }

        // =====================================================
        // CONVERT USER ID
        // =====================================================

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

        // =====================================================
        // LOG CURRENT USER
        // =====================================================

        System.out.println(
                "OUTWARD MAKER SEND TO CHECKER: "
                        + "userId="
                        + currentUserId
        );

        // =====================================================
        // CREATE SERVICE
        // =====================================================

        this.service =
                new OutwardMakerSendCheckerService();

        // =====================================================
        // LOAD BATCHES
        // =====================================================

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

            // =================================================
            // RETRIEVE BATCHES ASSIGNED TO CURRENT MAKER
            // =================================================

            List<OutwardBatch> batches =
                    service.getReadyBatches(
                            Math.toIntExact(
                                    currentUserId
                            )
                    );

            // =================================================
            // SAFETY
            // =================================================

            if (batches == null) {

                batches =
                        new java.util.ArrayList<>();
            }

            ListModelList<OutwardBatch> model =
                    new ListModelList<>(
                            batches
                    );

            // =================================================
            // RENDERER
            // =================================================

            batchListbox.setItemRenderer(
                    (Listitem item,
                     OutwardBatch batch,
                     int index) -> {

                        renderBatchRow(
                                item,
                                batch
                        );
                    }
            );

            batchListbox.setModel(
                    model
            );

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to load batches ready for Checker.\n"
                            + e.getMessage(),
                    "Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }

    // =========================================================
    // RENDER ONE BATCH ROW
    // =========================================================

    private void renderBatchRow(
            Listitem item,
            OutwardBatch batch) {

        // =====================================================
        // BATCH NUMBER
        // =====================================================

        Listcell batchIdCell =
                new Listcell();

        Label batchIdLabel =
                new Label(
                        safeValue(
                                batch.getBatchNumber()
                        )
                );

        batchIdLabel.setStyle(
                "font-weight:bold;"
                        + "color:#122B49;"
        );

        batchIdCell.appendChild(
                batchIdLabel
        );

        item.appendChild(
                batchIdCell
        );

        // =====================================================
        // TOTAL CHEQUES
        // =====================================================

        Listcell chequeCell =
                new Listcell();

        int cheques =
                batch.getNumberOfCheques() != null
                        ? batch.getNumberOfCheques()
                        : 0;

        Label chequeLabel =
                new Label(
                        String.valueOf(
                                cheques
                        )
                );

        chequeCell.appendChild(
                chequeLabel
        );

        item.appendChild(
                chequeCell
        );

        // =====================================================
        // STATUS BADGE
        // =====================================================

        Listcell statusCell =
                new Listcell();

        Label statusLabel =
                new Label(
                        "✓ Ready to Submit"
                );

        statusLabel.setStyle(
                "background:#D1FADF;"
                        + "color:#039855;"
                        + "padding:6px 12px;"
                        + "border-radius:4px;"
                        + "font-weight:bold;"
                        + "display:inline-block;"
        );

        statusCell.appendChild(
                statusLabel
        );

        item.appendChild(
                statusCell
        );

        // =====================================================
        // ACTION BUTTON
        // =====================================================

        Listcell actionCell =
                new Listcell();

        Button sendButton =
                new Button(
                        "Send to Checker"
                );

        sendButton.setWidth(
                "150px"
        );

        sendButton.setStyle(
                "background:#2457D6;"
                        + "color:white;"
                        + "border:none;"
                        + "padding:6px 12px;"
                        + "cursor:pointer;"
                        + "font-weight:bold;"
        );

        sendButton.addEventListener(
                Events.ON_CLICK,
                event ->
                        sendToChecker(
                                batch.getBatchNumber()
                        )
        );

        actionCell.appendChild(
                sendButton
        );

        item.appendChild(
                actionCell
        );
    }

    // =========================================================
    // SEND TO CHECKER - CONFIRMATION
    // =========================================================

    private void sendToChecker(
            String batchNumber) {

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            Messagebox.show(
                    "Invalid batch number.",
                    "Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );

            return;
        }

        Messagebox.show(
                "Are you sure you want to send batch "
                        + batchNumber
                        + " to Checker?",
                "Confirm Submission",
                Messagebox.YES | Messagebox.NO,
                Messagebox.QUESTION,
                event -> {

                    if (Messagebox.ON_YES.equals(
                            event.getName()
                    )) {

                        processSend(
                                batchNumber
                        );
                    }
                }
        );
    }

    // =========================================================
    // ACTUAL SEND OPERATION
    // =========================================================

    private void processSend(
            String batchNumber) {

        try {

            // =================================================
            // SEND USING CURRENT LOGGED-IN USER
            // =================================================

            boolean success =
                    service.sendToChecker(
                            batchNumber,
                            Math.toIntExact(
                                    currentUserId
                            )
                    );

            if (success) {

                Messagebox.show(
                        "Batch "
                                + batchNumber
                                + " has been successfully sent to Checker.",
                        "Success",
                        Messagebox.OK,
                        Messagebox.INFORMATION
                );

            } else {

                Messagebox.show(
                        "Batch "
                                + batchNumber
                                + " could not be sent.\n"
                                + "It may already be dispatched "
                                + "or not assigned to you.",
                        "Send Failed",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION
                );
            }

            // =================================================
            // REFRESH TABLE
            // =================================================

            loadReadyBatches();

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Error while sending batch to Checker.\n"
                            + e.getMessage(),
                    "Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }

    // =========================================================
    // SAFE VALUE
    // =========================================================

    private String safeValue(
            String value) {

        return (value == null
                || value.trim().isEmpty())
                ? "-"
                : value;
    }
}