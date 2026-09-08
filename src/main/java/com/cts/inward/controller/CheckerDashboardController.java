 package com.cts.inward.controller;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;

import com.cts.inward.model.CheckerBatch;
import com.cts.inward.service.CheckerDashboardService;
import com.cts.inward.service.CheckerDashboardServiceImpl;

public class CheckerDashboardController extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    /*
     * =========================================================
     * ZUL COMPONENTS
     * =========================================================
     */

    @Wire("#receivedCount")
    private Label receivedCount;

    @Wire("#availableCount")
    private Label availableCount;

    @Wire("#myBatchCount")
    private Label myBatchCount;

    @Wire("#showingText")
    private Label showingText;

    @Wire("#batchList")
    private Listbox batchList;

    /*
     * Filter buttons
     */
    @Wire("#allFilter")
    private Button allFilter;

    @Wire("#availableFilter")
    private Button availableFilter;

    @Wire("#myBatchesFilter")
    private Button myBatchesFilter;


    /*
     * =========================================================
     * SERVICE
     * =========================================================
     */

    private CheckerDashboardService service;


    /*
     * =========================================================
     * CURRENT LOGGED-IN CHECKER
     * =========================================================
     */

    private long userId;


    /*
     * =========================================================
     * CURRENT FILTER
     * =========================================================
     *
     * ALL
     * AVAILABLE
     * MY_BATCHES
     */

    private String selectedFilter = "ALL";


    /*
     * =========================================================
     * AFTER COMPOSE
     * =========================================================
     */

    @Override
    public void doAfterCompose(Component component) throws Exception {

        super.doAfterCompose(component);

        /*
         * Wire ZUL components.
         */
        Selectors.wireComponents(component, this, false);


        /*
         * Get current session.
         */
        Session session = Executions.getCurrent().getSession();


        /*
         * Get logged-in user ID.
         */
        Object sessionUserId =
                session.getAttribute("userId");


        /*
         * If user is not logged in,
         * redirect to login page.
         */
        if (sessionUserId == null) {

            Executions.sendRedirect("/zul/login.zul");

            return;
        }


        /*
         * Convert session user ID to long.
         */
        userId =
                ((Number) sessionUserId).longValue();


        /*
         * Initialize service.
         */
        service =
                new CheckerDashboardServiceImpl();


        /*
         * =====================================================
         * ALL FILTER
         * =====================================================
         */

        allFilter.addEventListener("onClick", event -> {

            selectedFilter = "ALL";

            updateFilterButtons();

            loadBatches();
        });


        /*
         * =====================================================
         * AVAILABLE FILTER
         * =====================================================
         */

        availableFilter.addEventListener("onClick", event -> {

            selectedFilter = "AVAILABLE";

            updateFilterButtons();

            loadBatches();
        });


        /*
         * =====================================================
         * MY BATCHES FILTER
         * =====================================================
         */

        myBatchesFilter.addEventListener("onClick", event -> {

            selectedFilter = "MY_BATCHES";

            updateFilterButtons();

            loadBatches();
        });


        /*
         * Set ALL as the default filter.
         */
        updateFilterButtons();


        /*
         * Load dashboard.
         */
        loadDashboard();
    }


    /*
     * =========================================================
     * LOAD COMPLETE DASHBOARD
     * =========================================================
     */

    private void loadDashboard() {

        loadCounts();

        updateFilterCounts();

        loadBatches();
    }


    /*
     * =========================================================
     * LOAD SUMMARY COUNTS
     * =========================================================
     */

    private void loadCounts() {

        /*
         * Received Batches
         */
        receivedCount.setValue(
                String.valueOf(
                        service.getReceivedBatchCount()));


        /*
         * Available Batches
         */
        availableCount.setValue(
                String.valueOf(
                        service.getAvailableBatchCount()));


        /*
         * My Batches
         */
        myBatchCount.setValue(
                String.valueOf(
                        service.getMyBatchCount(userId)));
    }


    /*
     * =========================================================
     * UPDATE FILTER COUNTS
     * =========================================================
     *
     * Example:
     *
     * All 3
     * Available 1
     * My Batches 1
     */

    private void updateFilterCounts() {

        List<CheckerBatch> batches =
                service.getBatches();

        int allCount =
                batches.size();

        int availableCountValue = 0;

        int myBatchesCount = 0;


        for (CheckerBatch batch : batches) {

            /*
             * AVAILABLE
             *
             * lock_status = AVAILABLE
             * user_id = NULL
             */
            if ("AVAILABLE".equals(
                        batch.getLockStatus())
                    && batch.getUserId() == null) {

                availableCountValue++;
            }


            /*
             * MY BATCHES
             *
             * lock_status = LOCKED
             * user_id = current checker
             */
            if ("LOCKED".equals(
                        batch.getLockStatus())
                    && batch.getUserId() != null
                    && batch.getUserId()
                            .longValue() == userId) {

                myBatchesCount++;
            }
        }


        /*
         * Update button labels.
         */
        allFilter.setLabel(
                "All " + allCount);

        availableFilter.setLabel(
                "Available "
                + availableCountValue);

        myBatchesFilter.setLabel(
                "My Batches "
                + myBatchesCount);
    }


    /*
     * =========================================================
     * LOAD BATCH TABLE
     * =========================================================
     */

    private void loadBatches() {

        /*
         * Remove existing rows.
         */
        batchList.getItems().clear();


        /*
         * Get all batches.
         */
        List<CheckerBatch> batches =
                service.getBatches();


        /*
         * List after applying selected filter.
         */
        List<CheckerBatch> filteredBatches =
                new ArrayList<>();


        /*
         * =====================================================
         * APPLY FILTER
         * =====================================================
         */

        for (CheckerBatch batch : batches) {

            /*
             * ALL
             *
             * Show every batch.
             */
            if ("ALL".equals(selectedFilter)) {

                filteredBatches.add(batch);
            }


            /*
             * AVAILABLE
             *
             * Show only:
             *
             * lock_status = AVAILABLE
             * user_id = NULL
             */
            else if ("AVAILABLE".equals(
                    selectedFilter)) {

                if ("AVAILABLE".equals(
                            batch.getLockStatus())
                        && batch.getUserId() == null) {

                    filteredBatches.add(batch);
                }
            }


            /*
             * MY BATCHES
             *
             * Show only batches locked
             * by current checker.
             */
            else if ("MY_BATCHES".equals(
                    selectedFilter)) {

                if ("LOCKED".equals(
                            batch.getLockStatus())
                        && batch.getUserId() != null
                        && batch.getUserId()
                                .longValue() == userId) {

                    filteredBatches.add(batch);
                }
            }
        }


        /*
         * =====================================================
         * CREATE TABLE ROWS
         * =====================================================
         */

        for (CheckerBatch batch :
                filteredBatches) {

            Listitem item =
                    new Listitem();


            /*
             * =================================================
             * BATCH ID
             * =================================================
             */

            Listcell batchIdCell =
                    new Listcell(
                            String.valueOf(
                                    batch.getBatchId()));

            batchIdCell.setSclass(
                    "batch-id");

            item.appendChild(
                    batchIdCell);


            /*
             * =================================================
             * TOTAL CHECKS
             * =================================================
             */

            Listcell totalCell =
                    new Listcell(
                            String.valueOf(
                                    batch.getTotalCheques()));

            item.appendChild(
                    totalCell);


            /*
             * =================================================
             * MAKER
             * =================================================
             */

            String makerId =
                    "Not Assigned";

            if (batch.getMaker() != null) {

                makerId =
                        batch.getMaker();
            }

            Listcell makerCell =
                    new Listcell(makerId);

            makerCell.setSclass(
                    "maker-name");

            item.appendChild(
                    makerCell);


            /*
             * =================================================
             * STATUS
             * =================================================
             */

            Listcell statusCell =
                    new Listcell(
                            batch.getLockStatus());

            item.appendChild(
                    statusCell);


            /*
             * =================================================
             * CHECKER USER ID
             * =================================================
             *
             * AVAILABLE:
             *
             * Not Assigned
             *
             *
             * LOCKED:
             *
             * Display owner checker ID.
             */

            String checkerId =
                    "Not Assigned";

            if ("LOCKED".equals(
                        batch.getLockStatus())
                    && batch.getUserId() != null) {

                checkerId =
                        String.valueOf(
                                batch.getUserId());
            }

            Listcell checkerCell =
                    new Listcell(checkerId);

            item.appendChild(
                    checkerCell);


            /*
             * =================================================
             * ACTION
             * =================================================
             */

            Listcell actionCell =
                    new Listcell();


            /*
             * =================================================
             * CASE 1
             *
             * AVAILABLE BATCH
             * =================================================
             *
             * user_id = NULL
             *
             * Display:
             *
             * Not Assigned
             * Open Verification
             */

            if ("AVAILABLE".equals(
                        batch.getLockStatus())
                    && batch.getUserId() == null) {

                Button openButton =
                        new Button(
                                "Open Verification");

                openButton.setSclass(
                        "verify-button");


                openButton.addEventListener(
                        "onClick",
                        event -> {

                            long batchId =
                                    batch.getBatchId();


                            /*
                             * Try to lock batch.
                             */
                            boolean locked =
                                    service.lockBatch(
                                            batchId,
                                            userId);


                            /*
                             * Lock successful.
                             */
                            if (locked) {

                                Executions.sendRedirect(
                                        "/zul/inward-checker/"
                                        + "batch-details.zul"
                                        + "?batchId="
                                        + batchId);
                            }


                            /*
                             * Lock failed.
                             *
                             * Another checker may
                             * have taken the batch.
                             */
                            else {

                                loadDashboard();
                            }
                        });


                actionCell.appendChild(
                        openButton);
            }


            /*
             * =================================================
             * CASE 2
             *
             * LOCKED BY CURRENT CHECKER
             * =================================================
             */

            else if ("LOCKED".equals(
                            batch.getLockStatus())
                    && batch.getUserId() != null
                    && batch.getUserId()
                            .longValue() == userId) {

                Button openButton =
                        new Button(
                                "Open Verification");

                openButton.setSclass(
                        "verify-button");


                openButton.addEventListener(
                        "onClick",
                        event -> {

                            long batchId =
                                    batch.getBatchId();


                            /*
                             * Already owned by
                             * current checker.
                             *
                             * No lock operation.
                             */
                            Executions.sendRedirect(
                                    "/zul/inward-checker/"
                                    + "batch-details.zul"
                                    + "?batchId="
                                    + batchId);
                        });


                actionCell.appendChild(
                        openButton);
            }


            /*
             * =================================================
             * CASE 3
             *
             * LOCKED BY ANOTHER CHECKER
             * =================================================
             */

            else {

                Label lockedLabel =
                        new Label("Locked");

                lockedLabel.setSclass(
                        "status-locked");

                actionCell.appendChild(
                        lockedLabel);
            }


            /*
             * Add action cell.
             */
            item.appendChild(
                    actionCell);


            /*
             * Add row to table.
             */
            batchList.appendChild(
                    item);
        }


        /*
         * Update footer.
         */
        updateShowingText(
                filteredBatches.size());
    }


    /*
     * =========================================================
     * UPDATE ACTIVE FILTER
     * =========================================================
     */

    private void updateFilterButtons() {

        /*
         * Reset all buttons.
         */
        allFilter.setSclass(
                "filter-btn");

        availableFilter.setSclass(
                "filter-btn");

        myBatchesFilter.setSclass(
                "filter-btn");


        /*
         * Set active button.
         */
        if ("ALL".equals(selectedFilter)) {

            allFilter.setSclass(
                    "filter-btn active-filter");
        }

        else if ("AVAILABLE".equals(
                selectedFilter)) {

            availableFilter.setSclass(
                    "filter-btn active-filter");
        }

        else if ("MY_BATCHES".equals(
                selectedFilter)) {

            myBatchesFilter.setSclass(
                    "filter-btn active-filter");
        }
    }


    /*
     * =========================================================
     * UPDATE FOOTER
     * =========================================================
     */

    private void updateShowingText(
            int totalBatches) {

        if (totalBatches == 0) {

            showingText.setValue(
                    "Showing 0 batches");
        }

        else {

            showingText.setValue(
                    "Showing 1 to "
                    + totalBatches
                    + " of "
                    + totalBatches
                    + " batches");
        }
    }
}