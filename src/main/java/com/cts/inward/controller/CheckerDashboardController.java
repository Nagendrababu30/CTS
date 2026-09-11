package com.cts.inward.controller;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;

import com.cts.inward.model.CheckerBatch;
import com.cts.inward.service.CheckerDashboardService;
import com.cts.inward.service.CheckerDashboardServiceImpl;

public class CheckerDashboardController
        extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    /*
     * =========================================================
     * ZUL COMPONENTS
     * =========================================================
     */

    private Label receivedCount;

    private Label availableCount;

    private Label myBatchCount;

    private Label showingText;

    private Listbox batchList;

    private Button allFilter;

    private Button availableFilter;

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
     *
     * ALL
     * AVAILABLE
     * MY_BATCHES
     * =========================================================
     */

    private String selectedFilter = "ALL";


    /*
     * =========================================================
     * AFTER COMPOSE
     * =========================================================
     */

    @Override
    public void doAfterCompose(
            Component component)
            throws Exception {

        super.doAfterCompose(component);

        /*
         * GenericForwardComposer automatically wires
         * ZUL components to fields having the same IDs.
         *
         * Therefore:
         *
         * id="allFilter"
         *        -> private Button allFilter;
         *
         * id="availableFilter"
         *        -> private Button availableFilter;
         *
         * id="myBatchesFilter"
         *        -> private Button myBatchesFilter;
         */

        /*
         * =====================================================
         * GET CURRENT SESSION
         * =====================================================
         */

        

        if (session == null) {

            Executions.sendRedirect(
                    "/zul/login.zul");

            return;
        }


        /*
         * =====================================================
         * GET LOGGED-IN USER ID
         * =====================================================
         *
         * This keeps your existing session design:
         *
         * session attribute = "userId"
         */

        Object sessionUserId =
                session.getAttribute(
                        "userId");

        if (sessionUserId == null) {

            Executions.sendRedirect(
                    "/zul/login.zul");

            return;
        }


        /*
         * Convert session user ID to long.
         */

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

                Executions.sendRedirect(
                        "/zul/login.zul");

                return;
            }
        }


        /*
         * =====================================================
         * INITIALIZE SERVICE
         * =====================================================
         */

        service =
                new CheckerDashboardServiceImpl();


        /*
         * =====================================================
         * ALL FILTER
         * =====================================================
         */

        allFilter.addEventListener(
                "onClick",
                event -> {

                    selectedFilter =
                            "ALL";

                    updateFilterButtons();

                    loadBatches();
                });


        /*
         * =====================================================
         * AVAILABLE FILTER
         * =====================================================
         */

        availableFilter.addEventListener(
                "onClick",
                event -> {

                    selectedFilter =
                            "AVAILABLE";

                    updateFilterButtons();

                    loadBatches();
                });


        /*
         * =====================================================
         * MY BATCHES FILTER
         * =====================================================
         */

        myBatchesFilter.addEventListener(
                "onClick",
                event -> {

                    selectedFilter =
                            "MY_BATCHES";

                    updateFilterButtons();

                    loadBatches();
                });


        /*
         * =====================================================
         * DEFAULT FILTER
         * =====================================================
         */

        updateFilterButtons();


        /*
         * =====================================================
         * LOAD DASHBOARD
         * =====================================================
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
                        service
                                .getReceivedBatchCount()));


        /*
         * Available Batches
         */

        availableCount.setValue(
                String.valueOf(
                        service
                                .getAvailableBatchCount()));


        /*
         * My Batches
         */

        myBatchCount.setValue(
                String.valueOf(
                        service
                                .getMyBatchCount(
                                        userId)));
    }


    /*
     * =========================================================
     * UPDATE FILTER COUNTS
     * =========================================================
     */

    private void updateFilterCounts() {

        List<CheckerBatch> batches =
                service.getBatches();

        if (batches == null) {

            batches =
                    new ArrayList<>();
        }

        int allCount =
                batches.size();

        int availableCountValue =
                0;

        int myBatchesCount =
                0;


        for (CheckerBatch batch :
                batches) {

            if (batch == null) {

                continue;
            }


            /*
             * =====================================================
             * AVAILABLE
             *
             * lock_status = AVAILABLE
             * user_id = NULL
             * =====================================================
             */

            if (("AVAILABLE".equals(batch.getLockStatus())
                    || "UNLOCKED".equals(batch.getLockStatus()))) {

                availableCountValue++;
            }


            /*
             * =====================================================
             * MY BATCHES
             *
             * lock_status = LOCKED
             * user_id = current checker
             * =====================================================
             */

            if ("LOCKED".equals(
                    batch.getLockStatus())
                    && batch.getUserId() != null
                    && batch.getUserId()
                            .longValue()
                            == userId) {

                myBatchesCount++;
            }
        }


        /*
         * =====================================================
         * UPDATE BUTTON LABELS
         * =====================================================
         */

        allFilter.setLabel(
                "All "
                + allCount);

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

        batchList
                .getItems()
                .clear();


        /*
         * Get all batches.
         */

        List<CheckerBatch> batches =
                service.getBatches();

        if (batches == null) {

            batches =
                    new ArrayList<>();
        }


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

        for (CheckerBatch batch :
                batches) {

            if (batch == null) {

                continue;
            }


            /*
             * =================================================
             * ALL
             * =================================================
             */

            if ("ALL".equals(
                    selectedFilter)) {

                filteredBatches.add(
                        batch);
            }


            /*
             * =================================================
             * AVAILABLE
             * =================================================
             */

            else if ("AVAILABLE".equals(
                    selectedFilter)) {

                if ("AVAILABLE".equals(batch.getLockStatus())
                        || "UNLOCKED".equals(batch.getLockStatus())) {

                    filteredBatches.add(batch);
                }
            }


            /*
             * =================================================
             * MY BATCHES
             * =================================================
             */

            else if ("MY_BATCHES".equals(
                    selectedFilter)) {

                if ("LOCKED".equals(
                        batch.getLockStatus())
                        && batch.getUserId() != null
                        && batch.getUserId()
                                .longValue()
                                == userId) {

                    filteredBatches.add(
                            batch);
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

            if (batch.getMaker() != null
                    && !batch.getMaker()
                            .trim()
                            .isEmpty()) {

                makerId =
                        batch.getMaker();
            }

            Listcell makerCell =
                    new Listcell(
                            makerId);

            makerCell.setSclass(
                    "maker-name");

            item.appendChild(
                    makerCell);


            /*
             * =================================================
             * STATUS
             * =================================================
             */

            String lockStatus =
                    batch.getLockStatus();

            if (lockStatus == null
                    || lockStatus.trim()
                            .isEmpty()) {

                lockStatus =
                        "AVAILABLE";
            }

            Listcell statusCell =
                    new Listcell(
                            lockStatus);

            item.appendChild(
                    statusCell);


            /*
             * =================================================
             * CHECKER USER ID
             * =================================================
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
                    new Listcell(
                            checkerId);

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
             */

            if ("AVAILABLE".equals(batch.getLockStatus())
                    || "UNLOCKED".equals(batch.getLockStatus())) {

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
                            .longValue()
                            == userId) {

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
                             * Already owned by current checker.
                             *
                             * Do not lock again.
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
                        new Label(
                                "Locked");

                lockedLabel.setSclass(
                        "status-locked");

                actionCell.appendChild(
                        lockedLabel);
            }


            /*
             * =================================================
             * ADD ACTION CELL
             * =================================================
             */

            item.appendChild(
                    actionCell);


            /*
             * =================================================
             * ADD ROW
             * =================================================
             */

            batchList.appendChild(
                    item);
        }


        /*
         * =====================================================
         * UPDATE FOOTER
         * =====================================================
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

        if ("ALL".equals(
                selectedFilter)) {

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