package com.iispl.cts.controller.outward.checker;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Messagebox;

import com.iispl.cts.controller.outward.LoginController;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.UserSession;
import com.iispl.cts.service.outward.checker.CheckerDashboardService;

public class CheckerDashboardController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    @Wire
    private Listbox batchListbox;

    @Wire
    private Label pendingVerificationCount;

    @Wire
    private Label cbsValidationCount;

    @Wire
    private Label readyToSendCount;

    private CheckerDashboardService service;

    /*
     * ============================================================
     * CURRENT LOGGED-IN CHECKER
     * ============================================================
     */

    private String currentCheckerUser;


    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

        /*
         * ---------------------------------------------------------
         * CURRENT LOGIN SESSION
         * ---------------------------------------------------------
         */

        UserSession sessionUser =
                LoginController.getCurrentUserSession();

        if (sessionUser == null) {

            Executions.sendRedirect("/login.zul");
            return;
        }

        /*
         * Outward Checker role = 4
         */

        if (sessionUser.getRoleId() != 4) {

            Messagebox.show(
                    "Access denied. Outward Checker access is required.",
                    "Access Denied",
                    Messagebox.OK,
                    Messagebox.ERROR);

            Executions.sendRedirect("/login.zul");
            return;
        }

        /*
         * Use logged-in user's actual database user ID.
         */

        currentCheckerUser =
                String.valueOf(sessionUser.getUserId());

        System.out.println(
                "CHECKER SESSION: "
                + "userId=" + sessionUser.getUserId()
                + ", username=" + sessionUser.getUsername()
                + ", roleId=" + sessionUser.getRoleId());

        service = new CheckerDashboardService();

        loadDashboard();
    }


    /*
     * ============================================================
     * LOAD DASHBOARD
     * ============================================================
     */
    private void loadDashboard() {

        try {

            List<OutwardBatch> batches =
                    service.getBatches();

            loadCounts(batches);

            loadBatchList(batches);

        } catch (Exception e) {

            e.printStackTrace();

            Clients.showNotification(
                    "Unable to load Checker Dashboard.",
                    Clients.NOTIFICATION_TYPE_ERROR,
                    null,
                    "top_center",
                    4000
            );
        }
    }


    /*
     * ============================================================
     * SUMMARY COUNTS
     * ============================================================
     */
    private void loadCounts(
            List<OutwardBatch> batches) {

        int pending = 0;
        int cbsValidation = 0;
        int readyToSend = 0;

        if (batches != null) {

            for (OutwardBatch batch : batches) {

                if (batch == null) {
                    continue;
                }

                String status =
                        batch.getBatchStatus();

                if (status == null) {
                    continue;
                }

                status = status.toUpperCase();


                /*
                 * Pending verification
                 */
                if ("READY_FOR_CHECKER".equals(status)
                        || "SUBMITTED".equals(status)
                        || "CHECKER_PENDING".equals(status)
                        || "PENDING_CHECKER".equals(status)) {

                    pending++;
                }


                /*
                 * CBS validation
                 */
                if ("CBS_VALIDATION".equals(status)
                        || "PENDING_CBS_VALIDATION".equals(status)) {

                    cbsValidation++;
                }


                /*
                 * Ready to send
                 */
                if ("READY_TO_SEND".equals(status)
                        || "READY_FOR_NPCI".equals(status)) {

                    readyToSend++;
                }
            }
        }


        pendingVerificationCount.setValue(
                String.valueOf(pending)
        );

        cbsValidationCount.setValue(
                String.valueOf(cbsValidation)
        );

        readyToSendCount.setValue(
                String.valueOf(readyToSend)
        );
    }


    /*
     * ============================================================
     * LOAD BATCH TABLE
     * ============================================================
     */
    private void loadBatchList(
            List<OutwardBatch> batches) {

        ListModelList<OutwardBatch> model =
                new ListModelList<>();

        if (batches != null) {
            model.addAll(batches);
        }

        batchListbox.setModel(model);

        batchListbox.setItemRenderer(
                new CheckerBatchRenderer()
        );
    }


    /*
     * ============================================================
     * BATCH RENDERER
     * ============================================================
     */
    private class CheckerBatchRenderer
            implements ListitemRenderer<OutwardBatch> {

        @Override
        public void render(
                Listitem item,
                OutwardBatch batch,
                int index)
                throws Exception {


            /*
             * ----------------------------------------------------
             * BATCH NUMBER
             * ----------------------------------------------------
             */
            Listcell batchCell =
                    new Listcell();

            batchCell.setLabel(
                    safe(batch.getBatchNumber())
            );

            item.appendChild(batchCell);


            /*
             * ----------------------------------------------------
             * CHEQUE COUNT
             * ----------------------------------------------------
             */
            Listcell chequeCell =
                    new Listcell();

            chequeCell.setLabel(
                    String.valueOf(
                            batch.getNumberOfCheques()
                    )
            );

            item.appendChild(chequeCell);


            /*
             * ----------------------------------------------------
             * STATUS
             * ----------------------------------------------------
             */
            Listcell statusCell =
                    new Listcell();

            statusCell.setLabel(
                    safe(batch.getBatchStatus())
            );

            item.appendChild(statusCell);


            /*
             * ----------------------------------------------------
             * ASSIGNMENT
             * ----------------------------------------------------
             */
            Listcell assignmentCell =
                    new Listcell();

            String lockStatus =
                    batch.getLockStatus();


            if (lockStatus == null) {

                assignmentCell.setLabel(
                        "AVAILABLE"
                );

            } else if ("AVAILABLE".equalsIgnoreCase(
                    lockStatus)) {

                assignmentCell.setLabel(
                        "Available"
                );

            } else {

                String checker =
                        batch.getCheckerUserNumber();

                if (checker != null &&
                    !checker.trim().isEmpty()) {

                    assignmentCell.setLabel(
                            "Locked by Checker "
                            + checker
                    );

                } else {

                    assignmentCell.setLabel(
                            "Locked"
                    );
                }
            }

            item.appendChild(assignmentCell);


            /*
             * ----------------------------------------------------
             * ACTION
             * ----------------------------------------------------
             */
            Listcell actionCell =
                    new Listcell();


            boolean available =
                    "AVAILABLE".equalsIgnoreCase(
                            batch.getLockStatus()
                    );


            if (available) {

                Button openButton =
                        new Button("Open");

                openButton.setSclass(
                        "btn btn-primary"
                );


                openButton.addEventListener(
                        "onClick",
                        event -> openBatch(batch)
                );


                actionCell.appendChild(
                        openButton
                );

            } else {

                Button lockedButton =
                        new Button("🔒 Locked");

                lockedButton.setDisabled(
                        true
                );

                actionCell.appendChild(
                        lockedButton
                );
            }


            item.appendChild(actionCell);
        }
    }


    /*
     * ============================================================
     * OPEN BATCH
     * ============================================================
     *
     * 1. User clicks Open
     * 2. Backend tries to acquire lock
     * 3. If successful -> Batches Queue
     * 4. If another Checker already acquired it -> error
     */
    private void openBatch(
            OutwardBatch batch) {

        if (batch == null ||
            batch.getBatchNumber() == null) {

            Clients.showNotification(
                    "Invalid batch.",
                    Clients.NOTIFICATION_TYPE_ERROR,
                    null,
                    "top_center",
                    3000
            );

            return;
        }


        String batchNumber =
                batch.getBatchNumber();


        try {

            /*
             * Re-check database before locking.
             */
            OutwardBatch latest =
                    service.findBatch(batchNumber);


            if (latest == null) {

                Clients.showNotification(
                        "Batch no longer exists.",
                        Clients.NOTIFICATION_TYPE_ERROR,
                        null,
                        "top_center",
                        3000
                );

                loadDashboard();

                return;
            }


            /*
             * Already locked?
             */
            if (!"AVAILABLE".equalsIgnoreCase(
                    latest.getLockStatus())) {

                Clients.showNotification(
                        "Batch is already locked.",
                        Clients.NOTIFICATION_TYPE_WARNING,
                        null,
                        "top_center",
                        3000
                );

                loadDashboard();

                return;
            }


            /*
             * Attempt atomic assignment.
             */
            boolean assigned =
                    service.assignBatch(
                            batchNumber,
                            currentCheckerUser
                    );


            if (!assigned) {

                Clients.showNotification(
                        "Batch was already assigned to another Checker.",
                        Clients.NOTIFICATION_TYPE_WARNING,
                        null,
                        "top_center",
                        4000
                );

                loadDashboard();

                return;
            }


            /*
             * Successful lock.
             */
            Clients.showNotification(
                    "Batch assigned successfully.",
                    Clients.NOTIFICATION_TYPE_INFO,
                    null,
                    "top_center",
                    2000
            );


            /*
             * Move to Batches Queue.
             */
            Executions.sendRedirect(
                    "/outward/checker/batchesQueue.zul"
                    + "?batchNumber="
                    + Executions.encodeURL(batchNumber)
            );


        } catch (Exception e) {

            e.printStackTrace();

            Clients.showNotification(
                    "Unable to open batch.",
                    Clients.NOTIFICATION_TYPE_ERROR,
                    null,
                    "top_center",
                    4000
            );
        }
    }


    /*
     * ============================================================
     * SAFE STRING
     * ============================================================
     */
    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }
}