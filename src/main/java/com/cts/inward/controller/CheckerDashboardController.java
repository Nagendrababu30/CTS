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


    private Label receivedCount;

    private Label availableCount;

    private Label myBatchCount;

    private Grid batchList;


    private Button allFilter;

    private Button availableFilter;

    private Button myBatchesFilter;

    private Button onHoldFilter;


    private Hlayout pagination;

    private Button previousPage;

    private Button currentPage;

    private Button nextPage;


    private CheckerDashboardService service;


    /*
     * Logged-in Checker ID.
     *
     * Used internally.
     */
    private long userId;


    /*
     * Current selected filter.
     */
    private String selectedFilter =
            "ALL";


    private static final int PAGE_SIZE = 10;


    private int currentPageNumber = 1;


    private int totalBatches = 0;


    private List<CheckerBatch> filteredBatches =
            new ArrayList<>();


    // =========================================================
    // AFTER COMPOSE
    // =========================================================

    @Override
    public void doAfterCompose(
            Component component)
            throws Exception {

        super.doAfterCompose(component);


        Session session =
                Executions.getCurrent()
                        .getSession();


        if (session == null) {

            Executions.sendRedirect(
                    "/zul/login.zul");

            return;
        }


        Object sessionUserId =
                session.getAttribute(
                        "userId");


        if (sessionUserId == null) {

            Executions.sendRedirect(
                    "/zul/login.zul");

            return;
        }


        if (sessionUserId instanceof Number) {

            userId =
                    ((Number) sessionUserId)
                            .longValue();

        } else {

            userId =
                    Long.parseLong(
                            sessionUserId.toString());
        }


        service =
                new CheckerDashboardServiceImpl();


        /*
         * ALL
         */
        allFilter.addEventListener(
                "onClick",
                event -> {

                    selectedFilter =
                            "ALL";

                    currentPageNumber = 1;

                    updateFilterButtons();

                    loadBatches();
                });


        /*
         * AVAILABLE
         */
        availableFilter.addEventListener(
                "onClick",
                event -> {

                    selectedFilter =
                            "AVAILABLE";

                    currentPageNumber = 1;

                    updateFilterButtons();

                    loadBatches();
                });


        /*
         * MY BATCHES
         */
        myBatchesFilter.addEventListener(
                "onClick",
                event -> {

                    selectedFilter =
                            "MY_BATCHES";

                    currentPageNumber = 1;

                    updateFilterButtons();

                    loadBatches();
                });


        /*
         * ON HOLD
         */
        onHoldFilter.addEventListener(
                "onClick",
                event -> {

                    selectedFilter =
                            "ON_HOLD";

                    currentPageNumber = 1;

                    updateFilterButtons();

                    loadBatches();
                });


        /*
         * Previous page.
         */
        previousPage.addEventListener(
                "onClick",
                event -> {

                    if (currentPageNumber > 1) {

                        currentPageNumber--;

                        renderBatches();
                    }
                });


        /*
         * Next page.
         */
        nextPage.addEventListener(
                "onClick",
                event -> {

                    if (currentPageNumber
                            < getTotalPages()) {

                        currentPageNumber++;

                        renderBatches();
                    }
                });


        updateFilterButtons();

        loadDashboard();
    }


    // =========================================================
    // LOAD DASHBOARD
    // =========================================================

    private void loadDashboard() {

        loadCounts();

        loadBatches();
    }


    // =========================================================
    // LOAD COUNTS
    // =========================================================

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


    // =========================================================
    // LOAD BATCHES
    // =========================================================

    private void loadBatches() {

        List<CheckerBatch> batches =
                service.getBatches();


        if (batches == null) {

            batches =
                    new ArrayList<>();
        }


        /*
         * Update Summary Card counts so they always match the loaded batches
         */
        int availCount = 0;
        int myCount = 0;
        for (CheckerBatch b : batches) {
            if (isAvailable(b)) {
                availCount++;
            }
            if (isLockedByCurrentUser(b)) {
                myCount++;
            }
        }
        receivedCount.setValue(String.valueOf(batches.size()));
        availableCount.setValue(String.valueOf(availCount));
        myBatchCount.setValue(String.valueOf(myCount));


        filteredBatches =
                new ArrayList<>();


        for (CheckerBatch batch : batches) {

            if (batch == null) {
                continue;
            }


            /*
             * ALL
             */
            if ("ALL".equals(
                    selectedFilter)) {

                filteredBatches.add(batch);
            }


            /*
             * AVAILABLE
             */
            else if ("AVAILABLE".equals(
                    selectedFilter)) {

                if (isAvailable(batch)) {

                    filteredBatches.add(batch);
                }
            }


            /*
             * MY BATCHES
             */
            else if ("MY_BATCHES".equals(
                    selectedFilter)) {

                if (isLockedByCurrentUser(
                        batch)) {

                    filteredBatches.add(batch);
                }
            }


            /*
             * ON HOLD
             */
            else if ("ON_HOLD".equals(
                    selectedFilter)) {

                if (isOnHold(batch)) {

                    filteredBatches.add(batch);
                }
            }
        }


        totalBatches =
                filteredBatches.size();


        if (currentPageNumber > getTotalPages()) {

            currentPageNumber =
                    getTotalPages();
        }


        renderBatches();
    }


    // =========================================================
    // IS ON HOLD
    // =========================================================

    private boolean isOnHold(
            CheckerBatch batch) {

        if (batch == null) {
            return false;
        }

        return "ON_HOLD".equalsIgnoreCase(
                batch.getBatchStatus())
                || "RETURN_TO_MAKER".equalsIgnoreCase(
                        batch.getBatchStatus());
    }


    // =========================================================
    // IS AVAILABLE
    // =========================================================

    private boolean isAvailable(
            CheckerBatch batch) {

        if (batch == null || isOnHold(batch)) {
            return false;
        }


        String lockStatus =
                batch.getLockStatus();


        return "AVAILABLE".equalsIgnoreCase(
                lockStatus)

                || "UNLOCKED".equalsIgnoreCase(
                        lockStatus)

                || lockStatus == null

                || lockStatus.trim().isEmpty();
    }


    // =========================================================
    // CURRENT CHECKER
    // =========================================================

    private boolean isLockedByCurrentUser(
            CheckerBatch batch) {

        if (batch == null || isOnHold(batch)) {
            return false;
        }


        return "LOCKED".equalsIgnoreCase(
                batch.getLockStatus())

                && batch.getUserId() != null

                && batch.getUserId()
                        .longValue()
                        == userId;
    }


    // =========================================================
    // RENDER BATCHES
    // =========================================================

    private void renderBatches() {

        batchList
                .getRows()
                .getChildren()
                .clear();


        int totalPages =
                getTotalPages();


        if (currentPageNumber > totalPages) {

            currentPageNumber =
                    totalPages;
        }


        if (filteredBatches.isEmpty()) {

            updatePagination();

            return;
        }


        int startIndex =
                (currentPageNumber - 1)
                * PAGE_SIZE;


        int endIndex =
                Math.min(
                        startIndex + PAGE_SIZE,
                        filteredBatches.size());


        for (int i = startIndex;
                i < endIndex;
                i++) {

            createBatchRow(
                    filteredBatches.get(i));
        }


        updatePagination();
    }


    // =========================================================
    // CREATE ROW
    // =========================================================

    private void createBatchRow(
            CheckerBatch batch) {

        Row row =
                new Row();


        /*
         * Batch ID
         */
        row.appendChild(
                new Label(
                        String.valueOf(
                                batch.getBatchId())));


        /*
         * Total cheques
         */
        row.appendChild(
                new Label(
                        String.valueOf(
                                batch.getTotalCheques())));


        /*
         * Maker name
         */
        String makerName =
                batch.getMaker();


        if (makerName == null
                || makerName.trim().isEmpty()) {

            makerName =
                    "Not Assigned";
        }


        row.appendChild(
                new Label(
                        makerName));


        /*
         * Status
         */
        String lockStatus =
                batch.getLockStatus();


        if (lockStatus == null
                || lockStatus.trim().isEmpty()) {

            lockStatus =
                    "UNLOCKED";
        }


        Label statusLabel =
                new Label();


        /*
         * ON HOLD
         *
         * Display "On Hold"
         * instead of "ON HOLD".
         */
        if (isOnHold(batch)) {

            statusLabel.setValue(
                    "On Hold");

            statusLabel.setSclass(
                    "status-badge badge-micr-repair");
        }


        /*
         * LOCKED
         */
        else if ("LOCKED".equalsIgnoreCase(
                lockStatus)) {

            statusLabel.setValue(
                    "LOCKED");

            statusLabel.setSclass(
                    "status-badge badge-locked");
        }


        /*
         * AVAILABLE
         */
        else {

            statusLabel.setValue(
                    "AVAILABLE");

            statusLabel.setSclass(
                    "status-badge badge-available");
        }


        row.appendChild(
                statusLabel);


        /*
         * Locked By
         */
        String checkerName =
                "Not Assigned";


        if ("LOCKED".equalsIgnoreCase(
                lockStatus)
                && batch.getCheckerName() != null
                && !batch.getCheckerName()
                        .trim()
                        .isEmpty()) {

            checkerName =
                    batch.getCheckerName();
        }


        row.appendChild(
                new Label(
                        checkerName));


        /*
         * ACTION
         */

        /*
         * ON HOLD
         *
         * Batch is on hold (returned to maker). Checker cannot open the batch.
         */
        if (isOnHold(batch)) {

            Button onHoldButton =
                    new Button();

            onHoldButton.setLabel(
                    "On Hold");

            onHoldButton.setIconSclass(
                    "z-icon-lock");

            onHoldButton.setSclass(
                    "btn btn-locked");

            onHoldButton.setDisabled(
                    true);

            onHoldButton.setTooltiptext(
                    "Batch is on hold (returned to maker) and cannot be opened.");

            row.appendChild(
                    onHoldButton);
        }


        /*
         * AVAILABLE
         *
         * Show Open Verification button.
         */
        else if (isAvailable(batch)) {

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


                        boolean locked =
                                service.lockBatch(
                                        batchId,
                                        userId);


                        if (locked) {

                            Executions.sendRedirect(
                                    "/zul/inward-checker/"
                                    + "batch-details.zul"
                                    + "?batchId="
                                    + batchId);

                        } else {

                            loadDashboard();
                        }
                    });


            row.appendChild(
                    openButton);
        }


        /*
         * LOCKED BY CURRENT CHECKER
         */
        else if (isLockedByCurrentUser(
                batch)) {

            Button openButton =
                    new Button(
                            "Open Verification");


            openButton.setSclass(
                    "btn btn-action");


            openButton.addEventListener(
                    "onClick",
                    event -> {

                        Executions.sendRedirect(
                                "/zul/inward-checker/"
                                + "batch-details.zul"
                                + "?batchId="
                                + batch.getBatchId());
                    });


            row.appendChild(
                    openButton);
        }


        /*
         * LOCKED BY ANOTHER CHECKER
         */
        else {

            Button lockedButton =
                    new Button();

            lockedButton.setLabel(
                    "Locked");

            lockedButton.setIconSclass(
                    "z-icon-lock");

            lockedButton.setSclass(
                    "btn btn-locked");

            lockedButton.setDisabled(
                    true);

            row.appendChild(
                    lockedButton);
        }


        row.setParent(
                batchList.getRows());
    }


    // =========================================================
    // TOTAL PAGES
    // =========================================================

    private int getTotalPages() {

        if (totalBatches <= 0) {
            return 1;
        }


        return (int) Math.ceil(
                (double) totalBatches
                / PAGE_SIZE);
    }


    // =========================================================
    // PAGINATION
    // =========================================================

    private void updatePagination() {

        int totalPages =
                getTotalPages();


        currentPage.setLabel(
                currentPageNumber
                + "/"
                + totalPages);


        previousPage.setDisabled(
                currentPageNumber <= 1);


        nextPage.setDisabled(
                currentPageNumber >= totalPages);


        pagination.setVisible(
                true);
    }


    // =========================================================
    // FILTER BUTTONS
    // =========================================================

    private void updateFilterButtons() {

        allFilter.setSclass(
                "filter-btn");

        availableFilter.setSclass(
                "filter-btn");

        myBatchesFilter.setSclass(
                "filter-btn");

        onHoldFilter.setSclass(
                "filter-btn");


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


        else if ("ON_HOLD".equals(
                selectedFilter)) {

            onHoldFilter.setSclass(
                    "filter-btn active-filter");
        }
    }
}