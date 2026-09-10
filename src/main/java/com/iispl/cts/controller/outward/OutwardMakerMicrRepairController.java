package com.iispl.cts.controller.outward;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Messagebox;

import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.service.outward.OutwardMakerMicrRepairService;

public class OutwardMakerMicrRepairController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    @Wire
    private Listbox batchListbox;

    private OutwardMakerMicrRepairService service;

    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

        service = new OutwardMakerMicrRepairService();

        setListItemRenderer();

        loadMicrErrorBatches();
    }

    private void setListItemRenderer() {

        batchListbox.setItemRenderer(
                new ListitemRenderer<OutwardBatch>() {

                    @Override
                    public void render(
                            Listitem item,
                            OutwardBatch batch,
                            int index) {

                        item.setValue(batch);

                        // Batch Number
                        item.appendChild(
                                new Listcell(
                                        batch.getBatchNumber()));

                        // Total Cheques
                        item.appendChild(
                                new Listcell(
                                        String.valueOf(
                                                batch.getNumberOfCheques())));

                        // MICR Error Count
                        int micrErrorCount =
                                service.getMicrErrorCount(
                                        batch.getBatchNumber());

                        item.appendChild(
                                new Listcell(
                                        String.valueOf(
                                                micrErrorCount)));

                        // Action
                        Listcell actionCell =
                                new Listcell();

                        Button openButton =
                                new Button("OPEN");

                        openButton.addEventListener(
                                "onClick",
                                event -> openBatch(batch));

                        actionCell.appendChild(openButton);

                        item.appendChild(actionCell);
                    }
                });
    }

    private void loadMicrErrorBatches() {

        // =====================================================
        // GET LOGGED-IN USER FROM SESSION
        // =====================================================

        Session sessionUser =
                Executions.getCurrent()
                        .getSession();

        if (sessionUser == null) {

            System.out.println(
                    "No logged-in user session found."
            );

            Executions.sendRedirect(
                    "/zul/login.zul");

            return;
        }

        // =====================================================
        // GET LOGGED-IN USER ID FROM SESSION
        // =====================================================
        //
        // Existing session design:
        // session attribute = "userId"
        //

        Object sessionUserId =
                sessionUser.getAttribute("userId");

        if (sessionUserId == null) {

            System.out.println(
                    "No logged-in user ID found in session."
            );

            Executions.sendRedirect(
                    "/zul/login.zul");

            return;
        }

        // =====================================================
        // CONVERT SESSION USER ID TO LONG
        // =====================================================

        long userId;

        if (sessionUserId instanceof Number) {

            userId =
                    ((Number) sessionUserId)
                            .longValue();

        } else {

            try {

                userId =
                        Long.parseLong(
                                sessionUserId.toString()
                        );

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid userId in session: "
                        + sessionUserId
                );

                Executions.sendRedirect(
                        "/zul/login.zul");

                return;
            }
        }

        // =====================================================
        // DYNAMIC LOGGED-IN USER ID
        // =====================================================

        String currentUserId =
                String.valueOf(userId);

        System.out.println(
                "======================================"
        );

        System.out.println(
                "OUTWARD MAKER MICR REPAIR"
        );

        System.out.println(
                "doAfterCompose() START"
        );

        System.out.println(
                "Current Maker User : "
                + currentUserId
        );

        // =====================================================
        // LOAD MICR ERROR BATCHES FOR LOGGED-IN USER
        // =====================================================

        List<OutwardBatch> batches =
                service.getMicrErrorBatches(
                        userId);

        System.out.println(
                "MICR REPAIR - BATCHES FOUND = "
                + (batches == null ? 0 : batches.size()));

        if (batches != null) {

            for (OutwardBatch batch : batches) {

                System.out.println(
                        "MICR REPAIR - BATCH = "
                        + batch.getBatchNumber());
            }
        }

        ListModelList<OutwardBatch> model =
                new ListModelList<>(
                        batches);

        batchListbox.setModel(model);
    }

    private void openBatch(OutwardBatch batch) {

        if (batch == null ||
            batch.getBatchNumber() == null) {

            return;
        }

        String batchNumber =
                batch.getBatchNumber();

        Executions.sendRedirect(
                "outward-maker-micr-repair-detail.zul"
                + "?batchNumber="
                + batchNumber);
    }
}