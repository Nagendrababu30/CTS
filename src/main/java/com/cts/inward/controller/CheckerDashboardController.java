 package com.cts.inward.controller;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

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

    private Grid batchList;

    private Button allFilter;

    private Button availableFilter;

    private Button myBatchesFilter;

    private Hlayout pagination;

    private Button previousPage;

    private Button currentPage;

    private Button nextPage;


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
     * PAGINATION
     * =========================================================
     */

    private static final int PAGE_SIZE = 10;

    private int currentPageNumber = 1;

    private int totalBatches = 0;

    private List<CheckerBatch> filteredBatches =
            new ArrayList<>();


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
         * =====================================================
         * GET CURRENT SESSION
         * =====================================================
         */

        Session session =
                Executions.getCurrent().getSession();

        if (session == null) {

            Executions.sendRedirect(
                    "/zul/login.zul");

            return;
        }


        /*
         * =====================================================
         * GET LOGGED-IN USER ID
         * =====================================================
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
         * =====================================================
         * CONVERT USER ID TO LONG
         * =====================================================
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
         * PREVIOUS PAGE
         * =====================================================
         */

        previousPage.addEventListener(
                "onClick",
                event -> {

                    if (currentPageNumber <= 1) {
                        return;
                    }

                    currentPageNumber--;

                    renderBatches();
                });


        /*
         * =====================================================
         * NEXT PAGE
         * =====================================================
         */

        nextPage.addEventListener(
                "onClick",
                event -> {

                    int totalPages =
                            getTotalPages();

                    if (currentPageNumber >= totalPages) {
                        return;
                    }

                    currentPageNumber++;

                    renderBatches();
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

        receivedCount.setValue(
                String.valueOf(
                        service
                                .getReceivedBatchCount()));


        availableCount.setValue(
                String.valueOf(
                        service
                                .getAvailableBatchCount()));


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
             * AVAILABLE
             */

            if ("AVAILABLE".equals(
                    batch.getLockStatus())
                    || "UNLOCKED".equals(
                            batch.getLockStatus())) {

                availableCountValue++;
            }


            /*
             * MY BATCHES
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
         * Keep the existing filter labels.
         */

        allFilter.setLabel(
                "All");

        availableFilter.setLabel(
                "Available");

        myBatchesFilter.setLabel(
                "My Batches");
    }


    /*
     * =========================================================
     * LOAD BATCHES
     * =========================================================
     */

    private void loadBatches() {

        /*
         * =====================================================
         * GET ALL BATCHES
         * =====================================================
         */

        List<CheckerBatch> batches =
                service.getBatches();

        if (batches == null) {

            batches =
                    new ArrayList<>();
        }


        /*
         * =====================================================
         * CREATE FILTERED LIST
         * =====================================================
         */

        filteredBatches =
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
             * ALL
             */

            if ("ALL".equals(
                    selectedFilter)) {

                filteredBatches.add(
                        batch);
            }


            /*
             * AVAILABLE
             */

            else if ("AVAILABLE".equals(
                    selectedFilter)) {

                if ("AVAILABLE".equals(
                        batch.getLockStatus())
                        || "UNLOCKED".equals(
                                batch.getLockStatus())) {

                    filteredBatches.add(
                            batch);
                }
            }


            /*
             * MY BATCHES
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
         * TOTAL FILTERED BATCHES
         * =====================================================
         */

        totalBatches =
                filteredBatches.size();


        /*
         * =====================================================
         * ALWAYS START FROM PAGE 1
         *
         * Important when filter changes.
         * =====================================================
         */

        currentPageNumber = 1;


        /*
         * =====================================================
         * RENDER
         * =====================================================
         */

        renderBatches();
    }


    /*
     * =========================================================
     * RENDER CURRENT PAGE
     * =========================================================
     */

    private void renderBatches() {

        /*
         * =====================================================
         * CLEAR EXISTING ROWS
         * =====================================================
         */

        batchList
                .getRows()
                .getChildren()
                .clear();


        /*
         * =====================================================
         * TOTAL PAGES
         * =====================================================
         */

        int totalPages =
                getTotalPages();


        /*
         * =====================================================
         * VALIDATE CURRENT PAGE
         * =====================================================
         */

        if (currentPageNumber < 1) {

            currentPageNumber = 1;
        }

        if (currentPageNumber > totalPages) {

            currentPageNumber = totalPages;
        }


        /*
         * =====================================================
         * EMPTY LIST
         * =====================================================
         */

        if (filteredBatches.isEmpty()) {

            updatePagination();

            return;
        }


        /*
         * =====================================================
         * CALCULATE START INDEX
         * =====================================================
         */

        int startIndex =
                (currentPageNumber - 1)
                * PAGE_SIZE;


        /*
         * =====================================================
         * CALCULATE END INDEX
         * =====================================================
         */

        int endIndex =
                Math.min(
                        startIndex + PAGE_SIZE,
                        filteredBatches.size());


        /*
         * =====================================================
         * CREATE ONLY CURRENT PAGE ROWS
         * =====================================================
         */

        for (int i = startIndex;
                i < endIndex;
                i++) {

            createBatchRow(
                    filteredBatches.get(i));
        }


        /*
         * =====================================================
         * UPDATE PAGINATION
         * =====================================================
         */

        updatePagination();
    }


    /*
     * =========================================================
     * CREATE BATCH ROW
     * =========================================================
     */

    private void createBatchRow(
            CheckerBatch batch) {

        Row row =
                new Row();


        /*
         * =====================================================
         * BATCH ID
         * =====================================================
         */

        Label batchIdLabel =
                new Label(
                        String.valueOf(
                                batch.getBatchId()));

        batchIdLabel.setSclass(
                "batch-id");

        row.appendChild(
                batchIdLabel);


        /*
         * =====================================================
         * TOTAL CHEQUES
         * =====================================================
         */

        Label totalLabel =
                new Label(
                        String.valueOf(
                                batch.getTotalCheques()));

        row.appendChild(
                totalLabel);


        /*
         * =====================================================
         * MAKER
         * =====================================================
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


        Label makerLabel =
                new Label(
                        makerId);

        makerLabel.setSclass(
                "maker-name");

        row.appendChild(
                makerLabel);


        /*
         * =====================================================
         * STATUS
         * =====================================================
         */

        String lockStatus =
                batch.getLockStatus();

        if (lockStatus == null
                || lockStatus.trim()
                        .isEmpty()) {

            lockStatus =
                    "AVAILABLE";
        }


        Label statusLabel =
                new Label(
                        lockStatus);


        if ("LOCKED".equals(
                lockStatus)) {

            statusLabel.setSclass(
                    "status-badge badge-locked");

        } else if ("AVAILABLE".equals(
                lockStatus)
                || "UNLOCKED".equals(
                        lockStatus)) {

            statusLabel.setSclass(
                    "status-badge badge-available");

        } else {

            statusLabel.setSclass(
                    "status-badge");
        }


        row.appendChild(
                statusLabel);


        /*
         * =====================================================
         * LOCKED BY
         * =====================================================
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


        Label checkerLabel =
                new Label(
                        checkerId);

        row.appendChild(
                checkerLabel);


        /*
         * =====================================================
         * ACTION
         * =====================================================
         */

        /*
         * AVAILABLE / UNLOCKED
         */

        if ("AVAILABLE".equals(
                lockStatus)
                || "UNLOCKED".equals(
                        lockStatus)) {

            Button openButton =
                    new Button(
                            "Open Verification");

            openButton.setSclass(
                    "btn btn-action");


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


            row.appendChild(
                    openButton);
        }


        /*
         * LOCKED BY CURRENT CHECKER
         */

        else if ("LOCKED".equals(
                lockStatus)
                && batch.getUserId() != null
                && batch.getUserId()
                        .longValue()
                        == userId) {

            Button openButton =
                    new Button(
                            "Open Verification");

            openButton.setSclass(
                    "btn btn-action");


            openButton.addEventListener(
                    "onClick",
                    event -> {

                        long batchId =
                                batch.getBatchId();


                        Executions.sendRedirect(
                                "/zul/inward-checker/"
                                + "batch-details.zul"
                                + "?batchId="
                                + batchId);
                    });


            row.appendChild(
                    openButton);
        }


        /*
         * LOCKED BY ANOTHER CHECKER
         */

        else {

            Label lockedLabel =
                    new Label(
                            "Locked");

            lockedLabel.setSclass(
                    "status-locked");

            row.appendChild(
                    lockedLabel);
        }


        /*
         * =====================================================
         * ADD ROW
         * =====================================================
         */

        row.setParent(
                batchList.getRows());
    }


    /*
     * =========================================================
     * GET TOTAL PAGES
     * =========================================================
     */

    private int getTotalPages() {

        if (totalBatches <= 0) {

            return 1;
        }

        return (int) Math.ceil(
                (double) totalBatches
                / PAGE_SIZE);
    }


    /*
     * =========================================================
     * UPDATE PAGINATION
     * =========================================================
     */

    private void updatePagination() {

        int totalPages =
                getTotalPages();


        /*
         * CURRENT PAGE
         *
         * Example:
         *
         * 1/5
         * 2/5
         * 5/5
         */

        currentPage.setLabel(
                currentPageNumber
                + "/"
                + totalPages);


        /*
         * PREVIOUS
         */

        previousPage.setDisabled(
                currentPageNumber <= 1);


        /*
         * NEXT
         */

        nextPage.setDisabled(
                currentPageNumber >= totalPages);


        /*
         * Always visible.
         */

        pagination.setVisible(
                true);
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

        } else if ("AVAILABLE".equals(
                selectedFilter)) {

            availableFilter.setSclass(
                    "filter-btn active-filter");

        } else if ("MY_BATCHES".equals(
                selectedFilter)) {

            myBatchesFilter.setSclass(
                    "filter-btn active-filter");
        }
    }
}