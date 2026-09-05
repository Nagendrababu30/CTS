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
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import com.cts.admin.model.User;
import com.cts.inward.dto.DashboardBatchDto;
import com.cts.inward.service.DashboardService;
import com.cts.inward.service.DashboardServiceImpl;

public class DashboardController
        extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    private Grid batchesGrid;

    private Button allBtn;
    private Button availableBtn;
    private Button lockedBtn;

    private Label receivedCountLabel;
    private Label pendingCountLabel;
    private Label submittedCountLabel;

    private DashboardService dashboardService;

    private String selectedStatus = "All";

    private Long loggedInUserId;

    @Override
    public void doAfterCompose(
            Component comp) throws Exception {

        super.doAfterCompose(comp);

        dashboardService =
                new DashboardServiceImpl();

        loadLoggedInUser();

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

        lockedBtn.addEventListener(
                Events.ON_CLICK,
                event -> {

                    selectedStatus = "Locked";

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

    private void updateFilterButtons() {

        allBtn.setSclass("filter-btn");

        availableBtn.setSclass("filter-btn");

        lockedBtn.setSclass("filter-btn");

        if ("All".equals(selectedStatus)) {

            allBtn.setSclass(
                    "filter-btn active-filter");
        }

        if ("Available".equals(selectedStatus)) {

            availableBtn.setSclass(
                    "filter-btn active-filter");
        }

        if ("Locked".equals(selectedStatus)) {

            lockedBtn.setSclass(
                    "filter-btn active-filter");
        }
    }

    private void loadBatches() {

        Rows rows =
                batchesGrid.getRows();

        rows.getChildren().clear();

        List<DashboardBatchDto> batches =
                dashboardService
                        .getDashboardBatches();

        int receivedCount =
                batches.size();

        int availableCount = 0;

        int lockedCount = 0;

        for (DashboardBatchDto batch : batches) {

            boolean locked =
                    "LOCKED".equalsIgnoreCase(
                            batch.getLockStatus());

            if (locked) {
                lockedCount++;
            } else {
                availableCount++;
            }
        }

        receivedCountLabel.setValue(
                String.valueOf(receivedCount));

        pendingCountLabel.setValue(
                String.valueOf(availableCount));

        /*
         * This should later count SENT_TO_CHECKER
         * from inward_batch_history.
         */
        submittedCountLabel.setValue("0");

        for (DashboardBatchDto batch : batches) {

            boolean locked =
                    "LOCKED".equalsIgnoreCase(
                            batch.getLockStatus());

            String status =
                    locked
                    ? "Locked"
                    : "Available";

            if (!"All".equals(selectedStatus)
                    && !selectedStatus.equals(status)) {

                continue;
            }

            Row row = new Row();

            /*
             * Batch ID
             */
            row.appendChild(
                    new Label(
                            String.valueOf(
                                    batch.getBatchId())));

            /*
             * Total cheque count
             */
            row.appendChild(
                    new Label(
                            String.valueOf(
                                    batch.getTotalCheques())));

            /*
             * Status
             */
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

            /*
             * Lock owner
             */
            Label userIdLabel =
                    new Label("-");

            if (locked
                    && batch.getLockUserId() != null) {

                userIdLabel.setValue(
                        String.valueOf(
                                batch.getLockUserId()));
            }

            row.appendChild(userIdLabel);

            /*
             * Action
             */
            Button actionButton =
                    new Button();

            if (!locked) {

                /*
                 * Available batch
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
                                lockBatch(batchId));

            } else {

                /*
                 * Batch is locked.
                 */
                boolean ownedByCurrentUser =
                        loggedInUserId != null
                        && batch.getLockUserId() != null
                        && loggedInUserId.equals(
                                batch.getLockUserId());

                if (ownedByCurrentUser) {

                    /*
                     * Current maker owns the lock.
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
                                    openBatch(batchId));

                } else {

                    /*
                     * Another maker owns the lock.
                     */
                    actionButton.setLabel(
                            "Locked");

                    actionButton.setIconSclass(
                            "z-icon-lock");

                    actionButton.setSclass(
                            "btn btn-locked");

                    actionButton.setDisabled(true);
                }
            }

            row.appendChild(actionButton);

            rows.appendChild(row);
        }

        if (batchesGrid.getPaginal() != null) {

            batchesGrid.getPaginal()
                    .setTotalSize(
                            rows.getChildren().size());
        }

        batchesGrid.setActivePage(0);
    }

    private void lockBatch(long batchId) {

        if (loggedInUserId == null) {
            return;
        }

        boolean success =
                dashboardService.lockBatch(
                        batchId,
                        loggedInUserId);

        if (success) {

            /*
             * Reload from DB.
             *
             * This is important because the UI
             * should reflect the persisted lock.
             */
            loadBatches();

            /*
             * At this point the batch is locked
             * and owned by the current user.
             *
             * The Open button will now appear.
             */
        }
    }

    private void openBatch(long batchId) {

        /*
         * Navigation to the actual batch
         * processing/validation page will go here.
         */
    }
}