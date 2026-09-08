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
         * ---------------------------------------------------------
         * ALL FILTER
         * ---------------------------------------------------------
         */
        allBtn.addEventListener(
                Events.ON_CLICK,
                event -> {

                    selectedStatus =
                            "All";

                    updateFilterButtons();

                    loadBatches();
                });

        /*
         * ---------------------------------------------------------
         * AVAILABLE FILTER
         * ---------------------------------------------------------
         */
        availableBtn.addEventListener(
                Events.ON_CLICK,
                event -> {

                    selectedStatus =
                            "Available";

                    updateFilterButtons();

                    loadBatches();
                });

        /*
         * ---------------------------------------------------------
         * MY BATCHES FILTER
         * ---------------------------------------------------------
         */
        myBatchesBtn.addEventListener(
                Events.ON_CLICK,
                event -> {

                    selectedStatus =
                            "My Batches";

                    updateFilterButtons();

                    loadBatches();
                });

        loadBatches();
    }

    /*
     * ---------------------------------------------------------
     * Load logged-in user
     * ---------------------------------------------------------
     */
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
     * ---------------------------------------------------------
     * Update filter button styling
     * ---------------------------------------------------------
     */
    private void updateFilterButtons() {

        allBtn.setSclass(
                "filter-btn");

        availableBtn.setSclass(
                "filter-btn");

        myBatchesBtn.setSclass(
                "filter-btn");

        if ("All".equals(
                selectedStatus)) {

            allBtn.setSclass(
                    "filter-btn active-filter");
        }

        if ("Available".equals(
                selectedStatus)) {

            availableBtn.setSclass(
                    "filter-btn active-filter");
        }

        if ("My Batches".equals(
                selectedStatus)) {

            myBatchesBtn.setSclass(
                    "filter-btn active-filter");
        }
    }

    /*
     * ---------------------------------------------------------
     * Load dashboard batches
     * ---------------------------------------------------------
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

        /*
         * ---------------------------------------------------------
         * Summary counts
         * ---------------------------------------------------------
         */
        int availableCount = 0;

        int myBatchesCount = 0;

        for (DashboardBatchDto batch :
                batches) {

            String batchStatus =
                    safe(
                            batch.getBatchStatus());

            boolean locked =
                    "LOCKED".equalsIgnoreCase(
                            batch.getLockStatus());

            /*
             * Available means an unlocked RECEIVED
             * batch.
             */
            if (!locked
                    && ("RECEIVED".equalsIgnoreCase(
                            batchStatus)
                        || batchStatus.isEmpty())) {

                availableCount++;
            }

            /*
             * My Batches means the current Maker
             * owns the active lock.
             */
            if (isOwnedByCurrentUser(
                    batch)) {

                myBatchesCount++;
            }
        }

        /*
         * Received card.
         *
         * This is the number of batches currently
         * visible on the Maker Dashboard.
         */
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
         * ---------------------------------------------------------
         * Build table
         * ---------------------------------------------------------
         */
        for (DashboardBatchDto batch :
                batches) {

            String batchStatus =
                    safe(
                            batch.getBatchStatus());

            boolean locked =
                    "LOCKED".equalsIgnoreCase(
                            batch.getLockStatus());

            String displayStatus =
                    getDisplayStatus(
                            batch);

            /*
             * Apply selected filter.
             */
            if (!matchesFilter(
                    selectedStatus,
                    batch,
                    displayStatus)) {

                continue;
            }

            Row row =
                    new Row();

            /*
             * -----------------------------------------------------
             * Batch ID
             * -----------------------------------------------------
             */
            row.appendChild(
                    new Label(
                            String.valueOf(
                                    batch.getBatchId())));

            /*
             * -----------------------------------------------------
             * Total cheque count
             * -----------------------------------------------------
             */
            row.appendChild(
                    new Label(
                            String.valueOf(
                                    batch.getTotalCheques())));

            /*
             * -----------------------------------------------------
             * Status
             * -----------------------------------------------------
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

                Label lockIcon =
                        new Label();

                lockIcon.setSclass(
                        "z-icon-lock");

                statusLayout.appendChild(
                        lockIcon);

                statusLayout.appendChild(
                        statusLabel);

            } else if ("MICR Repair".equalsIgnoreCase(
                    displayStatus)) {

                statusLayout.setSclass(
                        "status-badge badge-locked");

                Label icon =
                        new Label();

                icon.setSclass(
                        "z-icon-wrench");

                statusLayout.appendChild(
                        icon);

                statusLayout.appendChild(
                        statusLabel);

            } else if ("Data Entry".equalsIgnoreCase(
                    displayStatus)) {

                statusLayout.setSclass(
                        "status-badge badge-locked");

                Label icon =
                        new Label();

                icon.setSclass(
                        "z-icon-edit");

                statusLayout.appendChild(
                        icon);

                statusLayout.appendChild(
                        statusLabel);

            } else {

                statusLayout.setSclass(
                        "status-badge badge-available");

                statusLayout.appendChild(
                        statusLabel);
            }

            row.appendChild(
                    statusLayout);

            /*
             * -----------------------------------------------------
             * User ID / lock owner
             * -----------------------------------------------------
             */
            Label userIdLabel =
                    new Label("-");

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
             * -----------------------------------------------------
             * Action
             * -----------------------------------------------------
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

            rows.appendChild(
                    row);
        }

        /*
         * ---------------------------------------------------------
         * Reset pagination
         * ---------------------------------------------------------
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
     * ---------------------------------------------------------
     * Determine display status
     * ---------------------------------------------------------
     */
    private String getDisplayStatus(
            DashboardBatchDto batch) {

        String batchStatus =
                safe(
                        batch.getBatchStatus());

        boolean locked =
                "LOCKED".equalsIgnoreCase(
                        batch.getLockStatus());

        /*
         * MICR_REPAIR
         */
        if ("MICR_REPAIR".equalsIgnoreCase(
                batchStatus)) {

            return "MICR Repair";
        }

        /*
         * DATA_ENTRY
         */
        if ("DATA_ENTRY".equalsIgnoreCase(
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
         * Any unlocked RECEIVED batch.
         */
        return "Available";
    }

    /*
     * ---------------------------------------------------------
     * Filter matching
     * ---------------------------------------------------------
     */
    private boolean matchesFilter(
            String filter,
            DashboardBatchDto batch,
            String displayStatus) {

        /*
         * ALL
         */
        if ("All".equalsIgnoreCase(
                filter)) {

            return true;
        }

        /*
         * AVAILABLE
         */
        if ("Available".equalsIgnoreCase(
                filter)) {

            return "Available".equalsIgnoreCase(
                    displayStatus);
        }

        /*
         * MY BATCHES
         *
         * Only batches locked by the
         * currently logged-in Maker.
         */
        if ("My Batches".equalsIgnoreCase(
                filter)) {

            return isOwnedByCurrentUser(
                    batch);
        }

        return false;
    }

    /*
     * ---------------------------------------------------------
     * Configure action button
     * ---------------------------------------------------------
     */
    private void configureActionButton(
            Button actionButton,
            DashboardBatchDto batch,
            String batchStatus,
            boolean locked) {

        long batchId =
                batch.getBatchId();

        /*
         * =====================================================
         * AVAILABLE
         * =====================================================
         */
        if (!locked
                && ("RECEIVED".equalsIgnoreCase(
                        batchStatus)
                    || batchStatus.isEmpty())) {

            actionButton.setLabel(
                    "Lock & Validate");

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
         * =====================================================
         * MICR_REPAIR
         * =====================================================
         */
        if ("MICR_REPAIR".equalsIgnoreCase(
                batchStatus)) {

            boolean ownedByCurrentUser =
                    isOwnedByCurrentUser(
                            batch);

            if (!ownedByCurrentUser) {

                setLockedButton(
                        actionButton);

                return;
            }

            actionButton.setLabel(
                    "Open");

            actionButton.setIconSclass(
                    "z-icon-folder-open");

            actionButton.setSclass(
                    "btn btn-action");

            actionButton.setDisabled(
                    false);

            /*
             * MICR repair page requires
             * batchId + chequeIndex.
             */
            int nextRepairIndex =
                    micrRepairService
                            .getNextRepairIndex(
                                    batchId);

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
         * =====================================================
         * DATA_ENTRY
         * =====================================================
         */
        if ("DATA_ENTRY".equalsIgnoreCase(
                batchStatus)) {

            boolean ownedByCurrentUser =
                    isOwnedByCurrentUser(
                            batch);

            if (!ownedByCurrentUser) {

                setLockedButton(
                        actionButton);

                return;
            }

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
                            openDataEntry(
                                    batchId));

            return;
        }

        /*
         * =====================================================
         * LOCKED
         * =====================================================
         */
        if (locked) {

            boolean ownedByCurrentUser =
                    isOwnedByCurrentUser(
                            batch);

            if (ownedByCurrentUser) {

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
         * =====================================================
         * FALLBACK
         * =====================================================
         */
        actionButton.setLabel("-");

        actionButton.setSclass(
                "btn btn-locked");

        actionButton.setDisabled(
                true);
    }

    /*
     * ---------------------------------------------------------
     * Check whether current user owns lock
     * ---------------------------------------------------------
     */
    private boolean isOwnedByCurrentUser(
            DashboardBatchDto batch) {

        return loggedInUserId != null
                && batch.getLockUserId() != null
                && loggedInUserId.equals(
                        batch.getLockUserId());
    }

    /*
     * ---------------------------------------------------------
     * Locked button
     * ---------------------------------------------------------
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
     * ---------------------------------------------------------
     * Lock & Validate
     * ---------------------------------------------------------
     */
    private void lockAndValidate(
            long batchId) {

        if (loggedInUserId == null) {

            showError(
                    "Unable to identify the logged-in user.");

            return;
        }

        /*
         * 1. Lock the batch.
         */
        boolean success =
                dashboardService.lockBatch(
                        batchId,
                        loggedInUserId);

        if (!success) {

            showError(
                    "Unable to lock Batch ID "
                            + batchId
                            + ". It may already be locked by another Maker.");

            loadBatches();

            return;
        }

        /*
         * 2. Compare NPCI and OCR.
         */
        boolean needsMicrRepair =
                micrRepairService
                        .needsMicrRepair(
                                batchId);

        /*
         * 3. No MICR repair.
         *
         * Go directly to Data Entry.
         */
        if (!needsMicrRepair) {

            openDataEntry(
                    batchId);

            return;
        }

        /*
         * 4. MICR repair required.
         */
        int nextRepairIndex =
                micrRepairService
                        .getNextRepairIndex(
                                batchId);

        if (nextRepairIndex < 0) {

            openDataEntry(
                    batchId);

            return;
        }

        openMicrRepair(
                batchId,
                nextRepairIndex);
    }

    /*
     * ---------------------------------------------------------
     * Open locked batch
     * ---------------------------------------------------------
     */
    private void openBatch(
            long batchId) {

        boolean needsMicrRepair =
                micrRepairService
                        .needsMicrRepair(
                                batchId);

        /*
         * No remaining MICR repair.
         */
        if (!needsMicrRepair) {

            openDataEntry(
                    batchId);

            return;
        }

        /*
         * Find next MICR repair.
         */
        int nextRepairIndex =
                micrRepairService
                        .getNextRepairIndex(
                                batchId);

        if (nextRepairIndex < 0) {

            openDataEntry(
                    batchId);

            return;
        }

        openMicrRepair(
                batchId,
                nextRepairIndex);
    }

    /*
     * ---------------------------------------------------------
     * Open MICR Repair
     * ---------------------------------------------------------
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

        Executions.sendRedirect(
                url);
    }

    /*
     * ---------------------------------------------------------
     * Open Data Entry
     * ---------------------------------------------------------
     */
    private void openDataEntry(
            long batchId) {

        String url =
                "/zul/inward-maker/"
                        + "data-entry.zul"
                        + "?batchId="
                        + batchId
                        + "&source=dashboard";

        Executions.sendRedirect(
                url);
    }

    /*
     * ---------------------------------------------------------
     * Error message
     * ---------------------------------------------------------
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
     * ---------------------------------------------------------
     * Safe string
     * ---------------------------------------------------------
     */
    private String safe(
            String value) {

        return value == null
                ? ""
                : value.trim();
    }
}