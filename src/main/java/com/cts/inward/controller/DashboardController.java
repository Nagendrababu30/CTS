package com.cts.inward.controller;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import com.cts.admin.model.User;
import com.cts.inward.dto.DashboardBatchDto;
import com.cts.inward.service.DashboardService;
import com.cts.inward.service.DashboardServiceImpl;
import com.cts.inward.service.MicrRepairService;
import com.cts.inward.service.MicrRepairServiceImpl;

public class DashboardController
        extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    private static final String STATUS_RECEIVED =
            "RECEIVED";

    private static final String STATUS_LOCKED =
            "LOCKED";

    private static final String STATUS_MICR_REPAIR =
            "MICR_REPAIR";

    private static final String STATUS_DATA_ENTRY =
            "DATA_ENTRY";

    private static final String STATUS_DATA_ENTRY_COMPLETED =
            "DATA_ENTRY_COMPLETED";

    private static final String STATUS_SENT_TO_CHECKER =
            "SENT_TO_CHECKER";

    private Grid batchesGrid;

    private Button allBtn;
    private Button availableBtn;
    private Button myBatchesBtn;

    private Label receivedCountLabel;
    private Label pendingCountLabel;
    private Label myBatchesCountLabel;

    private DashboardService dashboardService;
    private MicrRepairService micrRepairService;

    private String selectedStatus = "All";

    private Long loggedInUserId;

    @Override
    public void doAfterCompose(
            Component comp) throws Exception {

        super.doAfterCompose(comp);

        dashboardService =
                new DashboardServiceImpl();

        micrRepairService =
                new MicrRepairServiceImpl();

        loadLoggedInUser();

        /*
         * -----------------------------------------------------------------
         * ALL
         * -----------------------------------------------------------------
         */
        allBtn.addEventListener(
                Events.ON_CLICK,
                event -> {
                    selectedStatus = "All";
                    updateFilterButtons();
                    loadBatches();
                });

        /*
         * -----------------------------------------------------------------
         * AVAILABLE
         * -----------------------------------------------------------------
         */
        availableBtn.addEventListener(
                Events.ON_CLICK,
                event -> {
                    selectedStatus = "Available";
                    updateFilterButtons();
                    loadBatches();
                });

        /*
         * -----------------------------------------------------------------
         * MY BATCHES
         * -----------------------------------------------------------------
         */
        myBatchesBtn.addEventListener(
                Events.ON_CLICK,
                event -> {
                    selectedStatus = "My Batches";
                    updateFilterButtons();
                    loadBatches();
                });

        loadBatches();
    }

    private void loadLoggedInUser() {

        Session session =
                Executions.getCurrent()
                        .getSession();

        User user =
                (User) session.getAttribute(
                        "loggedInUser");

        if (user != null) {

            loggedInUserId =
                    user.getUserId();
        }
    }

    /*
     * -------------------------------------------------------------------------
     * Filter button styling
     * -------------------------------------------------------------------------
     */
    private void updateFilterButtons() {

        allBtn.setSclass(
                "filter-btn");

        availableBtn.setSclass(
                "filter-btn");

        myBatchesBtn.setSclass(
                "filter-btn");

        if ("All".equals(selectedStatus)) {

            allBtn.setSclass(
                    "filter-btn active-filter");
        }

        if ("Available".equals(selectedStatus)) {

            availableBtn.setSclass(
                    "filter-btn active-filter");
        }

        if ("My Batches".equals(selectedStatus)) {

            myBatchesBtn.setSclass(
                    "filter-btn active-filter");
        }
    }

    /*
     * -------------------------------------------------------------------------
     * Load dashboard batches
     * -------------------------------------------------------------------------
     */
    private void loadBatches() {

        Rows rows =
                batchesGrid.getRows();

        rows.getChildren().clear();

        List<DashboardBatchDto> batches =
                dashboardService
                        .getDashboardBatches();

        if (batches == null) {

            batches =
                    java.util.Collections.emptyList();
        }

        int availableCount = 0;
        int myBatchesCount = 0;

        /*
         * -----------------------------------------------------------------
         * KPI counts
         * -----------------------------------------------------------------
         */
        for (DashboardBatchDto batch :
                batches) {

            String batchStatus =
                    safe(
                            batch.getBatchStatus());

            boolean locked =
                    STATUS_LOCKED.equalsIgnoreCase(
                            batch.getLockStatus());

            if (!locked
                    && (STATUS_RECEIVED.equalsIgnoreCase(
                            batchStatus)
                        || batchStatus.isEmpty())) {

                availableCount++;
            }

            if (isOwnedByCurrentUser(batch)) {

                myBatchesCount++;
            }
        }

        receivedCountLabel.setValue(
                String.valueOf(
                        batches.size()));

        pendingCountLabel.setValue(
                String.valueOf(
                        availableCount));

        myBatchesCountLabel.setValue(
                String.valueOf(
                        myBatchesCount));

        /*
         * -----------------------------------------------------------------
         * Build rows
         * -----------------------------------------------------------------
         */
        for (DashboardBatchDto batch :
                batches) {

            String batchStatus =
                    safe(
                            batch.getBatchStatus());

            boolean locked =
                    STATUS_LOCKED.equalsIgnoreCase(
                            batch.getLockStatus());

            String displayStatus =
                    getDisplayStatus(
                            batch);

            if (!matchesFilter(
                    selectedStatus,
                    batch,
                    displayStatus)) {

                continue;
            }

            Row row =
                    new Row();

            /*
             * -------------------------------------------------------------
             * Batch ID
             * -------------------------------------------------------------
             */
            row.appendChild(
                    new Label(
                            String.valueOf(
                                    batch.getBatchId())));

            /*
             * -------------------------------------------------------------
             * Total cheque count
             * -------------------------------------------------------------
             */
            row.appendChild(
                    new Label(
                            String.valueOf(
                                    batch.getTotalCheques())));

            /*
             * -------------------------------------------------------------
             * Status
             * -------------------------------------------------------------
             */
            Hlayout statusLayout =
                    new Hlayout();

            Label statusLabel =
                    new Label(
                            displayStatus);

            if ("Locked".equalsIgnoreCase(
                    displayStatus)) {

                statusLayout.setSclass(
                        "status-badge badge-locked");

                Label icon =
                        new Label();

                icon.setSclass(
                        "z-icon-lock");

                statusLayout.appendChild(icon);
                statusLayout.appendChild(statusLabel);

            } else if ("MICR Repair".equalsIgnoreCase(
                    displayStatus)) {

                statusLayout.setSclass(
                        "status-badge badge-locked");

                Label icon =
                        new Label();

                icon.setSclass(
                        "z-icon-wrench");

                statusLayout.appendChild(icon);
                statusLayout.appendChild(statusLabel);

            } else if ("Data Entry".equalsIgnoreCase(
                    displayStatus)) {

                statusLayout.setSclass(
                        "status-badge badge-locked");

                Label icon =
                        new Label();

                icon.setSclass(
                        "z-icon-edit");

                statusLayout.appendChild(icon);
                statusLayout.appendChild(statusLabel);

            } else if ("Data Entry Completed"
                    .equalsIgnoreCase(
                            displayStatus)) {

                statusLayout.setSclass(
                        "status-badge badge-locked");

                Label icon =
                        new Label();

                icon.setSclass(
                        "z-icon-check");

                statusLayout.appendChild(icon);
                statusLayout.appendChild(statusLabel);

            } else {

                statusLayout.setSclass(
                        "status-badge badge-available");

                statusLayout.appendChild(
                        statusLabel);
            }

            row.appendChild(
                    statusLayout);

            /*
             * -------------------------------------------------------------
             * User ID / lock owner
             * -------------------------------------------------------------
             */
            Label userIdLabel =
                    new Label("Not Assigned");

            if (locked
                    && batch.getLockUserId()
                            != null) {

                userIdLabel.setValue(
                        String.valueOf(
                                batch.getLockUserId()));
            }

            row.appendChild(
                    userIdLabel);

            /*
             * -------------------------------------------------------------
             * Action
             * -------------------------------------------------------------
             */
            Button actionButton =
                    new Button();

            configureActionButton(
                    actionButton,
                    batch,
                    batchStatus,
                    locked);

            row.appendChild(
                    actionButton);

            rows.appendChild(row);
        }

        /*
         * -----------------------------------------------------------------
         * Pagination
         * -----------------------------------------------------------------
         */
        if (batchesGrid.getPaginal()
                != null) {

            batchesGrid.getPaginal()
                    .setTotalSize(
                            rows.getChildren()
                                    .size());
        }

        batchesGrid.setActivePage(0);
    }

    /*
     * -------------------------------------------------------------------------
     * Determine display status
     * -------------------------------------------------------------------------
     */
    private String getDisplayStatus(
            DashboardBatchDto batch) {

        String batchStatus =
                safe(
                        batch.getBatchStatus());

        boolean locked =
                STATUS_LOCKED.equalsIgnoreCase(
                        batch.getLockStatus());

        /*
         * SENT TO CHECKER
         */
        if (STATUS_SENT_TO_CHECKER.equalsIgnoreCase(
                batchStatus)) {

            return "Sent to Checker";
        }

        /*
         * DATA ENTRY COMPLETED
         */
        if (STATUS_DATA_ENTRY_COMPLETED.equalsIgnoreCase(
                batchStatus)) {

            return "Data Entry Completed";
        }

        /*
         * MICR REPAIR
         */
        if (STATUS_MICR_REPAIR.equalsIgnoreCase(
                batchStatus)) {

            return "MICR Repair";
        }

        /*
         * DATA ENTRY
         */
        if (STATUS_DATA_ENTRY.equalsIgnoreCase(
                batchStatus)) {

            return "Data Entry";
        }

        /*
         * LOCKED
         */
        if (locked) {

            return "Locked";
        }

        /*
         * AVAILABLE
         */
        return "Available";
    }

    /*
     * -------------------------------------------------------------------------
     * Filter matching
     * -------------------------------------------------------------------------
     */
    private boolean matchesFilter(
            String filter,
            DashboardBatchDto batch,
            String displayStatus) {

        if ("All".equalsIgnoreCase(
                filter)) {

            return true;
        }

        if ("Available".equalsIgnoreCase(
                filter)) {

            return "Available".equalsIgnoreCase(
                    displayStatus);
        }

        if ("My Batches".equalsIgnoreCase(
                filter)) {

            return isOwnedByCurrentUser(
                    batch);
        }

        return false;
    }

    /*
     * -------------------------------------------------------------------------
     * Configure action button
     * -------------------------------------------------------------------------
     */
    private void configureActionButton(
            Button actionButton,
            DashboardBatchDto batch,
            String batchStatus,
            boolean locked) {

        long batchId =
                batch.getBatchId();

        /*
         * -----------------------------------------------------------------
         * AVAILABLE
         * -----------------------------------------------------------------
         */
        if (!locked
                && (STATUS_RECEIVED.equalsIgnoreCase(
                        batchStatus)
                    || batchStatus.isEmpty())) {

            actionButton.setLabel(
                    "Lock & Open");

            actionButton.setIconSclass(
                    "z-icon-lock");

            actionButton.setSclass(
                    "btn btn-action");

            actionButton.setDisabled(
                    false);

            actionButton.addEventListener(
                    Events.ON_CLICK,
                    event ->
                            lockAndValidate(
                                    batchId));

            return;
        }

        /*
         * -----------------------------------------------------------------
         * MICR REPAIR
         * -----------------------------------------------------------------
         */
        if (STATUS_MICR_REPAIR.equalsIgnoreCase(
                batchStatus)) {

            if (!isOwnedByCurrentUser(batch)) {

                setLockedButton(
                        actionButton);

                return;
            }

            int nextRepairIndex =
                    micrRepairService
                            .getNextRepairIndex(
                                    batchId);

            actionButton.setLabel(
                    "MICR Repair");

            actionButton.setIconSclass(
                    "z-icon-wrench");

            actionButton.setSclass(
                    "btn btn-action");

            actionButton.setDisabled(
                    nextRepairIndex < 0);

            if (nextRepairIndex >= 0) {

                actionButton.addEventListener(
                        Events.ON_CLICK,
                        event ->
                                openMicrRepair(
                                        batchId,
                                        nextRepairIndex));
            }

            return;
        }

        /*
         * -----------------------------------------------------------------
         * DATA ENTRY
         * -----------------------------------------------------------------
         */
        if (STATUS_DATA_ENTRY.equalsIgnoreCase(
                batchStatus)) {

            if (!isOwnedByCurrentUser(batch)) {

                setLockedButton(
                        actionButton);

                return;
            }

            actionButton.setLabel(
                    "Data Entry");

            actionButton.setIconSclass(
                    "z-icon-edit");

            actionButton.setSclass(
                    "btn btn-action");

            actionButton.setDisabled(
                    false);

            actionButton.addEventListener(
                    Events.ON_CLICK,
                    event ->
                            openDataEntry(
                                    batchId));

            return;
        }

        /*
         * -----------------------------------------------------------------
         * DATA ENTRY COMPLETED
         * -----------------------------------------------------------------
         *
         * IMPORTANT:
         *
         * Once a batch reaches DATA_ENTRY_COMPLETED,
         * the action must be "Send to Checker".
         *
         * Do NOT check lock ownership here.
         *
         * The dashboard screenshot showed:
         *
         * Status = Data Entry Completed
         * Action = Locked
         *
         * because the previous code required the batch to still be
         * owned by the current user.
         *
         * DATA_ENTRY_COMPLETED is the workflow state that allows
         * the Maker to move the batch to Send to Checker.
         * -----------------------------------------------------------------
         */
        if (STATUS_DATA_ENTRY_COMPLETED.equalsIgnoreCase(
                batchStatus)) {

            actionButton.setLabel(
                    "Send to Checker");

            actionButton.setIconSclass(
                    "z-icon-send");

            actionButton.setSclass(
                    "btn btn-action");

            actionButton.setDisabled(
                    false);

            actionButton.addEventListener(
                    Events.ON_CLICK,
                    event ->
                            openSendToChecker(
                                    batchId));

            return;
        }

        /*
         * -----------------------------------------------------------------
         * LOCKED
         * -----------------------------------------------------------------
         */
        if (locked) {

            if (isOwnedByCurrentUser(batch)) {

                actionButton.setLabel(
                        "Open");

                actionButton.setIconSclass(
                        "z-icon-folder-open");

                actionButton.setSclass(
                        "btn btn-action");

                actionButton.setDisabled(
                        false);

                actionButton.addEventListener(
                        Events.ON_CLICK,
                        event ->
                                openBatch(
                                        batchId));

            } else {

                setLockedButton(
                        actionButton);
            }

            return;
        }

        /*
         * -----------------------------------------------------------------
         * FALLBACK
         * -----------------------------------------------------------------
         */
        setLockedButton(
                actionButton);
    }

    /*
     * -------------------------------------------------------------------------
     * Check current user's ownership
     * -------------------------------------------------------------------------
     */
    private boolean isOwnedByCurrentUser(
            DashboardBatchDto batch) {

        return loggedInUserId != null
                && batch.getLockUserId() != null
                && loggedInUserId.equals(
                        batch.getLockUserId());
    }

    /*
     * -------------------------------------------------------------------------
     * Locked button
     * -------------------------------------------------------------------------
     */
    private void setLockedButton(
            Button actionButton) {

        actionButton.setLabel(
                "Locked");

        actionButton.setIconSclass(
                "z-icon-lock");

        actionButton.setSclass(
                "btn btn-locked");

        actionButton.setDisabled(
                true);
    }

    /*
     * -------------------------------------------------------------------------
     * Lock & Validate
     * -------------------------------------------------------------------------
     */
    private void lockAndValidate(
            long batchId) {

        if (loggedInUserId == null) {

            showError(
                    "Unable to identify the logged-in user.");

            return;
        }

        /*
         * -----------------------------------------------------------------
         * 1. Lock
         * -----------------------------------------------------------------
         */
        boolean locked =
                dashboardService.lockBatch(
                        batchId,
                        loggedInUserId);

        if (!locked) {

            showError(
                    "Unable to lock Batch ID "
                            + batchId
                            + ". It may already be locked.");

            loadBatches();

            return;
        }

        /*
         * -----------------------------------------------------------------
         * 2. Run NPCI vs OCR MICR validation
         * -----------------------------------------------------------------
         */
        boolean needsMicrRepair =
                micrRepairService
                        .needsMicrRepair(
                                batchId);

        /*
         * -----------------------------------------------------------------
         * 3. MICR REPAIR required
         * -----------------------------------------------------------------
         */
        if (needsMicrRepair) {

            boolean updated =
                    dashboardService
                            .updateBatchStatus(
                                    batchId,
                                    STATUS_MICR_REPAIR,
                                    loggedInUserId);

            if (!updated) {

                showError(
                        "Unable to move Batch ID "
                                + batchId
                                + " to MICR Repair.");

                return;
            }

            int nextRepairIndex =
                    micrRepairService
                            .getNextRepairIndex(
                                    batchId);

            if (nextRepairIndex < 0) {

                showError(
                        "MICR validation indicates repair is needed, "
                                + "but no repair cheque is available.");

                return;
            }

            openMicrRepair(
                    batchId,
                    nextRepairIndex);

            return;
        }

        /*
         * -----------------------------------------------------------------
         * 4. No MICR repair
         *
         * Move batch and applicable cheques to DATA_ENTRY.
         * -----------------------------------------------------------------
         */
        boolean moved =
                micrRepairService
                        .markBatchDataEntry(
                                batchId,
                                loggedInUserId);

        if (!moved) {

            showError(
                    "Unable to move Batch ID "
                            + batchId
                            + " to Data Entry.");

            return;
        }

        openDataEntry(
                batchId);
    }

    /*
     * -------------------------------------------------------------------------
     * Open locked batch
     * -------------------------------------------------------------------------
     */
    private void openBatch(
            long batchId) {

        boolean needsMicrRepair =
                micrRepairService
                        .needsMicrRepair(
                                batchId);

        if (needsMicrRepair) {

            int nextRepairIndex =
                    micrRepairService
                            .getNextRepairIndex(
                                    batchId);

            if (nextRepairIndex >= 0) {

                openMicrRepair(
                        batchId,
                        nextRepairIndex);

                return;
            }
        }

        openDataEntry(
                batchId);
    }

    /*
     * -------------------------------------------------------------------------
     * Open MICR Repair
     * -------------------------------------------------------------------------
     */
    private void openMicrRepair(
            long batchId,
            int chequeIndex) {

        String url =
                "/zul/inward-maker/"
                        + "micr-repair.zul"
                        + "?batchId="
                        + batchId
                        + "&chequeIndex="
                        + chequeIndex
                        + "&source=dashboard";

        Executions.sendRedirect(url);
    }

    /*
     * -------------------------------------------------------------------------
     * Open Data Entry
     * -------------------------------------------------------------------------
     */
    private void openDataEntry(
            long batchId) {

        String url =
                "/zul/inward-maker/"
                        + "data-entry.zul"
                        + "?batchId="
                        + batchId
                        + "&source=dashboard";

        Executions.sendRedirect(url);
    }

    /*
     * -------------------------------------------------------------------------
     * Open Send to Checker
     * -------------------------------------------------------------------------
     */
    private void openSendToChecker(
            long batchId) {

        String url =
                "/zul/inward-maker/"
                        + "send-to-checker.zul"
                        + "?batchId="
                        + batchId
                        + "&source=dashboard";

        Executions.sendRedirect(url);
    }

    /*
     * -------------------------------------------------------------------------
     * Show error
     * -------------------------------------------------------------------------
     */
    private void showError(
            String message) {

        Messagebox.show(
                message,
                "Dashboard",
                Messagebox.OK,
                Messagebox.ERROR);
    }

    /*
     * -------------------------------------------------------------------------
     * Safe string
     * -------------------------------------------------------------------------
     */
    private String safe(
            String value) {

        return value == null
                ? ""
                : value.trim();
    }
}