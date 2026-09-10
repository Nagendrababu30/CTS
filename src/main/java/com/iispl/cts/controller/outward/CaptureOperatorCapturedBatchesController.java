package com.iispl.cts.controller.outward;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Messagebox;

import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.service.outward.CaptureOperatorBatchService;

public class CaptureOperatorCapturedBatchesController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    // =========================================================
    // ZUL COMPONENT
    // =========================================================

    @Wire
    private Listbox capturedBatchesList;

    // =========================================================
    // SERVICE
    // =========================================================

    private CaptureOperatorBatchService service;

    // =========================================================
    // AFTER COMPOSE
    // =========================================================

    @Override
    public void doAfterCompose(Component comp)
            throws Exception {

        super.doAfterCompose(comp);

        System.out.println(
                "======================================");

        System.out.println(
                "CAPTURED BATCHES SCREEN");

        System.out.println(
                "doAfterCompose() START");

        System.out.println(
                "======================================");


        // =====================================================
        // CURRENT LOGGED-IN USER
        // =====================================================

        Session session =
                Executions.getCurrent().getSession();

        if (session == null) {

            System.out.println(
                    "ERROR: ZK session is NULL.");

            Executions.sendRedirect(
                    "/zul/login.zul");

            return;
        }


        Object sessionUserId =
                session.getAttribute("userId");

        if (sessionUserId == null) {

            System.out.println(
                    "ERROR: userId not found in session.");

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

                System.out.println(
                        "ERROR: Invalid userId in session: "
                        + sessionUserId);

                Executions.sendRedirect(
                        "/zul/login.zul");

                return;
            }
        }


        System.out.println(
                "CAPTURE OPERATOR SESSION: "
                + "userId=" + userId);


        // =====================================================
        // SERVICE
        // =====================================================

        service =
                new CaptureOperatorBatchService();


        // =====================================================
        // LOAD CAPTURED BATCHES
        // =====================================================

        loadCapturedBatches();


        System.out.println(
                "======================================");

        System.out.println(
                "CAPTURED BATCHES SCREEN");

        System.out.println(
                "doAfterCompose() END");

        System.out.println(
                "======================================");
    }


    // =========================================================
    // LOAD CAPTURED BATCHES
    // =========================================================

    private void loadCapturedBatches() {

        if (capturedBatchesList == null) {

            System.out.println(
                    "ERROR: capturedBatchesList is NULL.");

            return;
        }


        if (service == null) {

            System.out.println(
                    "ERROR: CaptureOperatorBatchService is NULL.");

            return;
        }


        try {

            System.out.println(
                    "Calling service.getCapturedBatches()...");


            List<OutwardBatch> batches =
                    service.getCapturedBatches();


            if (batches == null) {

                batches =
                        new ArrayList<>();
            }


            System.out.println(
                    "Captured batches returned = "
                    + batches.size());


            // =================================================
            // DEBUG
            // =================================================

            for (OutwardBatch batch : batches) {

                if (batch == null) {
                    continue;
                }


                System.out.println(
                        "--------------------------------------");


                System.out.println(
                        "Batch Number      : "
                        + batch.getBatchNumber());


                System.out.println(
                        "Number Of Cheques : "
                        + batch.getNumberOfCheques());


                System.out.println(
                        "Batch Status      : "
                        + batch.getBatchStatus());


                System.out.println(
                        "Branch Code       : "
                        + batch.getBranchCode());


                System.out.println(
                        "Created By        : "
                        + batch.getCreatedBy());


                System.out.println(
                        "Created At        : "
                        + batch.getCreatedAt());
            }


            // =================================================
            // CREATE LIST MODEL
            // =================================================

            ListModelList<OutwardBatch> model =
                    new ListModelList<>();


            model.addAll(batches);


            // =================================================
            // RENDER LIST
            // =================================================

            capturedBatchesList.setItemRenderer(
                    new ListitemRenderer<OutwardBatch>() {

                        @Override
                        public void render(
                                Listitem item,
                                OutwardBatch batch,
                                int index) {

                            if (batch == null) {
                                return;
                            }


                            // ---------------------------------
                            // BATCH NUMBER
                            // ---------------------------------

                            Listcell batchNumberCell =
                                    new Listcell(
                                            safe(
                                                    batch.getBatchNumber()
                                            )
                                    );

                            item.appendChild(
                                    batchNumberCell
                            );


                            // ---------------------------------
                            // TOTAL CHEQUES
                            // ---------------------------------

                            Listcell chequeCountCell =
                                    new Listcell(
                                            String.valueOf(
                                                    batch.getNumberOfCheques()
                                            )
                                    );

                            item.appendChild(
                                    chequeCountCell
                            );


                            // ---------------------------------
                            // STATUS
                            // ---------------------------------

                            Listcell statusCell =
                                    new Listcell(
                                            safe(
                                                    batch.getBatchStatus()
                                            )
                                    );

                            item.appendChild(
                                    statusCell
                            );
                        }
                    }
            );


            capturedBatchesList.setModel(
                    model
            );


            System.out.println(
                    "Captured batches successfully loaded into ZUL."
            );


        } catch (Exception e) {

            e.printStackTrace();


            System.out.println(
                    "ERROR: Unable to load captured batches."
            );


            Messagebox.show(
                    "Unable to load captured batches from database.\n\n"
                    + "Error: "
                    + e.getMessage(),
                    "Captured Batches",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }


    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }
}