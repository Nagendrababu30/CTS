package com.iispl.cts.controller.outward.checker;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;

import com.iispl.cts.controller.outward.LoginController;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.UserSession;
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
    // CURRENT CHECKER
    // ============================================

    private String currentCheckerUser;


    // ============================================
    // PAGE LOAD
    // ============================================

    @Override
    public void doAfterCompose(Component comp)
            throws Exception {

        super.doAfterCompose(comp);


        // ========================================
        // GET CURRENT USER
        // ========================================

        UserSession sessionUser =
                LoginController.getCurrentUserSession();


        // ========================================
        // NO SESSION
        // ========================================

        if (sessionUser == null) {

            Executions.sendRedirect(
                    "/login.zul"
            );

            return;
        }


        // ========================================
        // CHECK ROLE
        // ROLE ID 4 = OUTWARD CHECKER
        // ========================================

        if (sessionUser.getRoleId() != 4) {

            Messagebox.show(

                    "Access denied. "
                    + "Outward Checker access is required.",

                    "Access Denied",

                    Messagebox.OK,

                    Messagebox.ERROR

            );

            Executions.sendRedirect(
                    "/login.zul"
            );

            return;
        }


        // ========================================
        // GET CHECKER USER ID
        // ========================================

        currentCheckerUser =

                String.valueOf(
                        sessionUser.getUserId()
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
                            currentCheckerUser
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

        );


        // ========================================
        // IMPORTANT
        //
        // PARAMETER NAME = batchNumber
        //
        // SAME NAME USED IN
        // CheckerChequeVerificationController
        // ========================================

        Executions.sendRedirect(

                "/outward/checker/chequeVerification.zul"

                + "?batchNumber="

                + Executions.encodeURL(batchNumber)

        );

    }

}


