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

    // -------------------------------------------------------------------------
    // ZUL Components
    // -------------------------------------------------------------------------

    private Grid batchesGrid;

    private Button allBtn;
    private Button availableBtn;
    private Button myBatchesBtn;

    private Label receivedCountLabel;
    private Label pendingCountLabel;
    private Label myBatchesCountLabel;

    // -------------------------------------------------------------------------
    // Services
    // -------------------------------------------------------------------------

    private DashboardService dashboardService;
    private MicrRepairService micrRepairService;

    // -------------------------------------------------------------------------
    // Current filter
    // -------------------------------------------------------------------------

    private String selectedStatus = "All";

    // -------------------------------------------------------------------------
    // Logged-in user
    // -------------------------------------------------------------------------

    private Long loggedInUserId;

    // -------------------------------------------------------------------------
    // Composer lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void doAfterCompose(
            Component comp) throws Exception {

        super.doAfterCompose(comp);

        dashboardService =
                new DashboardServiceImpl();

        micrRepairService =
                new MicrRepairServiceImpl();

        loadLoggedInUser();

        registerEvents();

        loadBatches();
    }

    // -------------------------------------------------------------------------
    // Load logged-in user
    // -------------------------------------------------------------------------

    private void loadLoggedInUser() {

        Session session =
                Executions.getCurrent()
                        .getSession();

        if (session == null) {
            return;
        }

        Object loggedInUserObject =
                session.getAttribute(
                        "loggedInUser");

        if (loggedInUserObject instanceof User) {

            User user =
                    (User) loggedInUserObject;

            loggedInUserId =
                    user.getUserId();
        }
    }

    // -------------------------------------------------------------------------
    // Register button events
    // -------------------------------------------------------------------------

    private void registerEvents() {

        allBtn.addEventListener(
                Events.ON_CLICK,
                event -> {

                    selectedStatus = "All";

                    updateFilterButtons();

                    loadBatches();
                });


        availableBtn.addEventListener(
                Events.ON_CLICK,
                event -> {

                    selectedStatus = "Available";

                    updateFilterButtons();

                    loadBatches();
                });


        myBatchesBtn.addEventListener(
                Events.ON_CLICK,
                event -> {

                    selectedStatus = "My Batches";

                    updateFilterButtons();

                    loadBatches();
                });
    }

    // -------------------------------------------------------------------------
    // Update filter button styles
    // -------------------------------------------------------------------------

    private void updateFilterButtons() {

        allBtn.setSclass("filter-btn");

        availableBtn.setSclass("filter-btn");

        myBatchesBtn.setSclass("filter-btn");


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

    // -------------------------------------------------------------------------
    // Load batches
    // -------------------------------------------------------------------------

    private void loadBatches() {

        Rows rows =
                batchesGrid.getRows();

        /*
         * Your ZUL already contains <rows/>.
         *
         * Keep this fallback so the controller does not fail
         * if the ZUL is changed later.
         */
        if (rows == null) {

            rows = new Rows();

            batchesGrid.appendChild(rows);
        }

        rows.getChildren().clear();


        List<DashboardBatchDto> batches =
                dashboardService
                        .getDashboardBatches();

        if (batches == null) {

            batches =
                    List.of();
        }


        int receivedCount =
                batches.size();

        int availableCount = 0;

        int myBatchCount = 0;


        /*
         * -------------------------------------------------------------
         * Calculate KPI values
         * -------------------------------------------------------------
         */

        for (DashboardBatchDto batch : batches) {

            boolean locked =
                    "LOCKED".equalsIgnoreCase(
                            batch.getLockStatus());

            if (!locked) {

                availableCount++;
            }


            boolean ownedByCurrentUser =
                    loggedInUserId != null
                    && batch.getLockUserId() != null
                    && loggedInUserId.equals(
                            batch.getLockUserId());

            if (ownedByCurrentUser) {

                myBatchCount++;
            }
        }


        /*
         * -------------------------------------------------------------
         * Update KPI cards
         * -------------------------------------------------------------
         */

        receivedCountLabel.setValue(
                String.valueOf(receivedCount));

        pendingCountLabel.setValue(
                String.valueOf(availableCount));

        myBatchesCountLabel.setValue(
                String.valueOf(myBatchCount));


        /*
         * -------------------------------------------------------------
         * Create table rows
         * -------------------------------------------------------------
         */

        for (DashboardBatchDto batch : batches) {

            boolean locked =
                    "LOCKED".equalsIgnoreCase(
                            batch.getLockStatus());

            String status =
                    locked
                    ? "Locked"
                    : "Available";


            /*
             * Apply selected filter.
             */
            if ("Available".equals(selectedStatus)
                    && locked) {

                continue;
            }


            if ("My Batches".equals(selectedStatus)) {

                boolean ownedByCurrentUser =
                        loggedInUserId != null
                        && batch.getLockUserId() != null
                        && loggedInUserId.equals(
                                batch.getLockUserId());

                if (!ownedByCurrentUser) {

                    continue;
                }
            }


            Row row = new Row();


            // -----------------------------------------------------------------
            // Batch ID
            // -----------------------------------------------------------------

            row.appendChild(
                    new Label(
                            String.valueOf(
                                    batch.getBatchId())));


            // -----------------------------------------------------------------
            // Total cheque count
            // -----------------------------------------------------------------

            row.appendChild(
                    new Label(
                            String.valueOf(
                                    batch.getTotalCheques())));


            // -----------------------------------------------------------------
            // Status
            // -----------------------------------------------------------------

            Hlayout statusLayout =
                    new Hlayout();

            Label statusLabel =
                    new Label(status);


            if (locked) {

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

            } else {

                statusLayout.setSclass(
                        "status-badge badge-available");

                statusLayout.appendChild(
                        statusLabel);
            }


            row.appendChild(statusLayout);


            // -----------------------------------------------------------------
            // Lock owner / User ID
            // -----------------------------------------------------------------

            Label userIdLabel =
                    new Label("-");


            if (locked
                    && batch.getLockUserId() != null) {

                userIdLabel.setValue(
                        String.valueOf(
                                batch.getLockUserId()));
            }


            row.appendChild(userIdLabel);


            // -----------------------------------------------------------------
            // Action
            // -----------------------------------------------------------------

            Button actionButton =
                    new Button();


            if (!locked) {

                /*
                 * ---------------------------------------------------------
                 * Available batch
                 * ---------------------------------------------------------
                 */

                actionButton.setLabel(
                        "Lock & Validate");

                actionButton.setSclass(
                        "btn btn-action");


                long batchId =
                        batch.getBatchId();


                actionButton.addEventListener(
                        Events.ON_CLICK,
                        event ->
                                lockAndValidate(
                                        batchId));


            } else {

                /*
                 * ---------------------------------------------------------
                 * Locked batch
                 * ---------------------------------------------------------
                 */

                boolean ownedByCurrentUser =
                        loggedInUserId != null
                        && batch.getLockUserId() != null
                        && loggedInUserId.equals(
                                batch.getLockUserId());


                if (ownedByCurrentUser) {

                    /*
                     * Current Maker owns the batch.
                     */
                    actionButton.setLabel(
                            "Open");

                    actionButton.setIconSclass(
                            "z-icon-folder-open");

                    actionButton.setSclass(
                            "btn btn-action");


                    long batchId =
                            batch.getBatchId();


                    actionButton.addEventListener(
                            Events.ON_CLICK,
                            event ->
                                    openBatch(
                                            batchId));

                } else {

                    /*
                     * Another Maker owns the batch.
                     */
                    actionButton.setLabel(
                            "Locked");

                    actionButton.setIconSclass(
                            "z-icon-lock");

                    actionButton.setSclass(
                            "btn btn-locked");

                    actionButton.setDisabled(
                            true);
                }
            }


            row.appendChild(actionButton);

            rows.appendChild(row);
        }


        /*
         * Reset pagination after every filter operation.
         */
        if (batchesGrid.getPaginal() != null) {

            batchesGrid.getPaginal()
                    .setTotalSize(
                            rows.getChildren().size());

            batchesGrid.setActivePage(0);
        }
    }

    // -------------------------------------------------------------------------
    // Lock and validate
    // -------------------------------------------------------------------------

    private void lockAndValidate(
            long batchId) {

        if (loggedInUserId == null) {

            showError(
                    "Unable to identify the logged-in user.");

            return;
        }


        /*
         * Acquire lock first.
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
         * Determine whether MICR repair is required.
         */
        boolean needsMicrRepair =
                micrRepairService.needsMicrRepair(
                        batchId);


        /*
         * No MICR repair.
         *
         * Go directly to Data Entry.
         */
        if (!needsMicrRepair) {

            openDataEntry(batchId);

            return;
        }


        /*
         * MICR repair required.
         *
         * Find the first unfinished cheque.
         */
        int nextRepairIndex =
                micrRepairService.getNextRepairIndex(
                        batchId);


        if (nextRepairIndex < 0) {

            openDataEntry(batchId);

            return;
        }


        openMicrRepair(
                batchId,
                nextRepairIndex);
    }

    // -------------------------------------------------------------------------
    // Open current user's locked batch
    // -------------------------------------------------------------------------

    private void openBatch(
            long batchId) {

        /*
         * Re-check the batch state.
         */
        boolean needsMicrRepair =
                micrRepairService.needsMicrRepair(
                        batchId);


        /*
         * All MICR repairs completed.
         */
        if (!needsMicrRepair) {

            openDataEntry(batchId);

            return;
        }


        /*
         * Find first unfinished MICR repair.
         */
        int nextRepairIndex =
                micrRepairService.getNextRepairIndex(
                        batchId);


        if (nextRepairIndex < 0) {

            openDataEntry(batchId);

            return;
        }


        openMicrRepair(
                batchId,
                nextRepairIndex);
    }

    // -------------------------------------------------------------------------
    // Open MICR Repair
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Open Data Entry
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Error message
    // -------------------------------------------------------------------------

    private void showError(
            String message) {

        Messagebox.show(
                message,
                "Dashboard",
                Messagebox.OK,
                Messagebox.ERROR);
    }
}