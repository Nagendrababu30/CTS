package com.iispl.cts.controller.outward;

import java.util.List;

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

import com.iispl.cts.dao.outward.OutwardMakerDataEntryDAO;
import com.iispl.cts.model.outward.OutwardBatch;

public class OutwardMakerDataEntryController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    @Wire
    private Listbox batchListbox;

    private final OutwardMakerDataEntryDAO dataEntryDAO =
            new OutwardMakerDataEntryDAO();

    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

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

        System.out.println(
                "======================================"
        );

        System.out.println(
                "OUTWARD MAKER DATA ENTRY"
        );

        System.out.println(
                "doAfterCompose() START"
        );

        System.out.println(
                "Current Maker User : "
                + userId
        );

        // =====================================================
        // LOAD USER-SPECIFIC BATCHES
        // =====================================================

        loadBatches(userId);
    }

    private void loadBatches(long userId) {

        if (batchListbox == null) {
            return;
        }

        batchListbox.getItems().clear();

        // =====================================================
        // FETCH ONLY BATCHES ASSIGNED TO THIS MAKER
        // =====================================================

        List<OutwardBatch> batches =
                dataEntryDAO.getBatchesForMaker(
                        userId);

        if (batches != null) {

            for (OutwardBatch batch : batches) {

                Listitem item =
                        new Listitem();

                // =================================================
                // 1. BATCH ID
                // =================================================

                String batchNumber =
                        batch.getBatchNumber();

                Listcell cellBatchId =
                        new Listcell(
                                batchNumber);

                cellBatchId.setStyle(
                        "font-weight: 600; color: #1E293B;"
                );

                item.appendChild(
                        cellBatchId);

                // =================================================
                // 2. TOTAL CHEQUES
                // =================================================

                int totalCheques =
                        batch.getNumberOfCheques() != null
                                ? batch.getNumberOfCheques()
                                : 0;

                item.appendChild(
                        new Listcell(
                                String.valueOf(
                                        totalCheques)));

                // =================================================
                // 3. BATCH STATUS
                // =================================================

                Listcell cellStatus =
                        new Listcell();

                String status =
                        batch.getBatchStatus() != null
                                ? batch.getBatchStatus()
                                : "UNKNOWN";

                Label lblStatus =
                        new Label(status);

                lblStatus.setSclass(
                        "status-badge "
                        + ("COMPLETED".equalsIgnoreCase(status)
                                ? "badge-completed"
                                : "badge-assigned")
                );

                cellStatus.appendChild(
                        lblStatus);

                item.appendChild(
                        cellStatus);

                // =================================================
                // 4. ACTION BUTTON
                // =================================================

                Listcell cellAction =
                        new Listcell();

                Button btn =
                        new Button("Process");

                btn.setSclass(
                        "action-btn");

                btn.addEventListener(
                        "onClick",
                        e -> {

                            Executions.sendRedirect(
                                    "outward-maker-data-entry-detail.zul"
                                    + "?batchId="
                                    + batchNumber);
                        });

                cellAction.appendChild(
                        btn);

                item.appendChild(
                        cellAction);

                batchListbox.appendChild(
                        item);
            }
        }
    }
}