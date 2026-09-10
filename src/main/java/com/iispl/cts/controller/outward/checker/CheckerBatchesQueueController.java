package com.iispl.cts.controller.outward.checker;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;

import org.zkoss.zk.ui.util.Clients;

import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.service.outward.checker.CheckerBatchService;

public class CheckerBatchesQueueController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    // ============================================
    // ZUL COMPONENT
    // ============================================

    @Wire
    private Listbox queueListbox;

    // ============================================
    // SERVICE
    // ============================================

    private CheckerBatchService service;

    // ============================================
    // CURRENT CHECKER USER ID
    // ============================================

    private long currentCheckerUser;

    // ============================================
    // PAGE LOAD
    // ============================================

    @Override
    public void doAfterCompose(Component comp)
            throws Exception {

        super.doAfterCompose(comp);

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

            currentCheckerUser =
                    ((Number) sessionUserId).longValue();

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

        // ========================================
        // LOG CURRENT CHECKER
        // ========================================

        System.out.println(
                "Checker Batches Queue - User ID = "
                        + currentCheckerUser
        );

        // ========================================
        // CREATE SERVICE
        // ========================================

        service =
                new CheckerBatchService();

        // ========================================
        // LOAD BATCHES
        // ========================================

        loadBatches();
    }

    // ============================================
    // LOAD BATCHES
    // ============================================

    private void loadBatches() {

        try {

            List<OutwardBatch> batches =
                    service.getCheckerQueueBatches(
                            String.valueOf(
                                    currentCheckerUser
                            )
                    );

            // ========================================
            // CLEAR OLD ROWS
            // ========================================

            queueListbox.getItems().clear();

            // ========================================
            // NO BATCHES
            // ========================================

            if (batches == null
                    || batches.isEmpty()) {

                return;
            }

            // ========================================
            // CREATE ROWS
            // ========================================

            for (OutwardBatch batch : batches) {

                createBatchRow(batch);
            }

        } catch (Exception e) {

            e.printStackTrace();

            Clients.showNotification(
                    "Unable to load Batches Queue.",
                    Clients.NOTIFICATION_TYPE_ERROR,
                    null,
                    "top_center",
                    4000
            );
        }
    }

    // ============================================
    // CREATE BATCH ROW
    // ============================================

    private void createBatchRow(
            final OutwardBatch batch) {

        // ========================================
        // ROW
        // ========================================

        Listitem item =
                new Listitem();

        // ========================================
        // BATCH NUMBER
        // ========================================

        Listcell batchNumberCell =
                new Listcell();

        batchNumberCell.setLabel(
                batch.getBatchNumber()
        );

        item.appendChild(
                batchNumberCell
        );

        // ========================================
        // TOTAL CHEQUES
        // ========================================

        Listcell chequeCell =
                new Listcell();

        chequeCell.setLabel(
                String.valueOf(
                        batch.getNumberOfCheques()
                )
        );

        item.appendChild(
                chequeCell
        );

        // ========================================
        // STATUS
        // ========================================

        Listcell statusCell =
                new Listcell();

        statusCell.setLabel(
                "Locked by Checker"
        );

        item.appendChild(
                statusCell
        );

        // ========================================
        // ACTION
        // ========================================

        Listcell actionCell =
                new Listcell();

        Button openButton =
                new Button("Open");

        openButton.setSclass(
                "btn btn-primary"
        );

        // ========================================
        // OPEN BUTTON CLICK
        // ========================================

        openButton.addEventListener(
                Events.ON_CLICK,
                new EventListener<Event>() {

                    @Override
                    public void onEvent(Event event)
                            throws Exception {

                        openBatch(
                                batch.getBatchNumber()
                        );
                    }
                }
        );

        actionCell.appendChild(
                openButton
        );

        item.appendChild(
                actionCell
        );

        // ========================================
        // ADD TO LISTBOX
        // ========================================

        queueListbox.appendChild(
                item
        );
    }

    // ============================================
    // OPEN BATCH
    // ============================================

    private void openBatch(
            String batchNumber) {

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            return;
        }

        System.out.println(
                "OPENING BATCH = "
                        + batchNumber
                        + " | CHECKER USER ID = "
                        + currentCheckerUser
        );

        // ========================================
        // OPEN CHEQUE VERIFICATION
        // ========================================
        //
        // batchNumber is passed to
        // CheckerChequeVerificationController
        //
        // ========================================

        Executions.sendRedirect(
                "/outward/checker/chequeVerification.zul"
                        + "?batchNumber="
                        + Executions.encodeURL(
                                batchNumber
                        )
        );
    }
}