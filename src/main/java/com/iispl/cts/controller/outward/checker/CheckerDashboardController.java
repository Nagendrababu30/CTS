package com.iispl.cts.controller.outward.checker;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
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

import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.service.outward.checker.CheckerDashboardService;

public class CheckerDashboardController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    // ============================================================
    // ZUL COMPONENTS
    // ============================================================

    @Wire
    private Listbox batchListbox;

    @Wire
    private Label pendingVerificationCount;

    @Wire
    private Label cbsValidationCount;

    @Wire
    private Label readyToSendCount;

    // ============================================================
    // SERVICE
    // ============================================================

    private CheckerDashboardService service;

    // ============================================================
    // CURRENT LOGGED-IN CHECKER USER ID
    // ============================================================

    private long currentCheckerUser;

    // ============================================================
    // PAGE INITIALIZATION
    // ============================================================

    @Override
    public void doAfterCompose(Component comp)
            throws Exception {

        super.doAfterCompose(comp);

        // ========================================================
        // GET ZK SESSION
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
        // GET USER ID FROM SESSION
        // ========================================================

        Object sessionUserId =
                session.getAttribute("userId");

        // ========================================================
        // USER ID NOT FOUND
        // ========================================================

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

            currentCheckerUser =
                    ((Number) sessionUserId)
                            .longValue();

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

        // ========================================================
        // LOG CURRENT CHECKER
        // ========================================================

        System.out.println(
                "CHECKER SESSION: "
                        + "userId="
                        + currentCheckerUser
        );

        // ========================================================
        // CREATE SERVICE
        // ========================================================

        service =
                new CheckerDashboardService();

        // ========================================================
        // LOAD DASHBOARD
        // ========================================================

        loadDashboard();
    }

    // ============================================================
    // LOAD DASHBOARD
    // ============================================================

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

    // ============================================================
    // SUMMARY COUNTS
    // ============================================================

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

                status =
                        status.toUpperCase();

                // =================================================
                // PENDING VERIFICATION
                // =================================================

                if ("READY_FOR_CHECKER".equals(status)
                        || "SUBMITTED".equals(status)
                        || "CHECKER_PENDING".equals(status)
                        || "PENDING_CHECKER".equals(status)) {

                    pending++;
                }

                // =================================================
                // CBS VALIDATION
                // =================================================

                if ("CBS_VALIDATION".equals(status)
                        || "PENDING_CBS_VALIDATION".equals(status)) {

                    cbsValidation++;
                }

                // =================================================
                // READY TO SEND
                // =================================================

                if ("READY_TO_SEND".equals(status)
                        || "READY_FOR_NPCI".equals(status)) {

                    readyToSend++;
                }
            }
        }

        // ========================================================
        // DISPLAY COUNTS
        // ========================================================

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

    // ============================================================
    // LOAD BATCH TABLE
    // ============================================================

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

    // ============================================================
    // BATCH RENDERER
    // ============================================================

    private class CheckerBatchRenderer
            implements ListitemRenderer<OutwardBatch> {

        @Override
        public void render(
                Listitem item,
                OutwardBatch batch,
                int index)
                throws Exception {

            // ====================================================
            // BATCH NUMBER
            // ====================================================

            Listcell batchCell =
                    new Listcell();

            batchCell.setLabel(
                    safe(
                            batch.getBatchNumber()
                    )
            );

            item.appendChild(
                    batchCell
            );

            // ====================================================
            // CHEQUE COUNT
            // ====================================================

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

            // ====================================================
            // STATUS
            // ====================================================

            Listcell statusCell =
                    new Listcell();

            statusCell.setLabel(
                    safe(
                            batch.getBatchStatus()
                    )
            );

            item.appendChild(
                    statusCell
            );

            // ====================================================
            // ASSIGNMENT
            // ====================================================

            Listcell assignmentCell =
                    new Listcell();

            String lockStatus =
                    batch.getLockStatus();

            if (lockStatus == null) {

                assignmentCell.setLabel(
                        "AVAILABLE"
                );

            } else if (
                    "AVAILABLE".equalsIgnoreCase(
                            lockStatus
                    )) {

                assignmentCell.setLabel(
                        "Available"
                );

            } else {

                String checker =
                        batch.getCheckerUserNumber();

                if (checker != null
                        && !checker.trim().isEmpty()) {

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

            item.appendChild(
                    assignmentCell
            );

            // ====================================================
            // ACTION
            // ====================================================

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

            item.appendChild(
                    actionCell
            );
        }
    }

    // ============================================================
    // OPEN BATCH
    // ============================================================

    private void openBatch(
            OutwardBatch batch) {

        if (batch == null
                || batch.getBatchNumber() == null) {

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

            // ====================================================
            // RE-CHECK DATABASE BEFORE LOCKING
            // ====================================================

            OutwardBatch latest =
                    service.findBatch(
                            batchNumber
                    );

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

            // ====================================================
            // ALREADY LOCKED
            // ====================================================

            if (!"AVAILABLE".equalsIgnoreCase(
                    latest.getLockStatus()
            )) {

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

            // ====================================================
            // ATTEMPT ATOMIC ASSIGNMENT
            // ====================================================

            boolean assigned =
                    service.assignBatch(
                            batchNumber,
                            String.valueOf(
                                    currentCheckerUser
                            )
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

            // ====================================================
            // SUCCESSFUL LOCK
            // ====================================================

            Clients.showNotification(
                    "Batch assigned successfully.",
                    Clients.NOTIFICATION_TYPE_INFO,
                    null,
                    "top_center",
                    2000
            );

            // ====================================================
            // MOVE TO BATCHES QUEUE
            // ====================================================

            Executions.sendRedirect(
                    "/outward/checker/batchesQueue.zul"
                            + "?batchNumber="
                            + Executions.encodeURL(
                                    batchNumber
                            )
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

    // ============================================================
    // SAFE STRING
    // ============================================================

    private String safe(
            String value) {

        return value == null
                ? ""
                : value;
    }
}