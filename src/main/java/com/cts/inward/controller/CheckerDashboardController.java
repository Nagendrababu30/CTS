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

    private Button reVerifyFilter;

    private Button onHoldFilter;

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
     * RE_VERIFY
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
                session.getAttribute("userId");

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

                    currentPageNumber =
                            1;

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

                    currentPageNumber =
                            1;

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

                    currentPageNumber =
                            1;

                    updateFilterButtons();

                    loadBatches();
                });


        /*
         * =====================================================
         * RE-VERIFY FILTER
         * =====================================================
         */

        if (reVerifyFilter != null) {

            reVerifyFilter.addEventListener(
                    "onClick",
                    event -> {

                        selectedFilter =
                                "RE_VERIFY";

                        currentPageNumber =
                                1;

                        updateFilterButtons();

                        loadBatches();
                    });
        }

        if (onHoldFilter != null) {

            onHoldFilter.addEventListener(
                    "onClick",
                    event -> {

                        selectedFilter =
                                "RE_VERIFY";

                        currentPageNumber =
                                1;

                        updateFilterButtons();

                        loadBatches();
                    });
        }


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
                        service.getReceivedBatchCount()));


        availableCount.setValue(
                String.valueOf(
                        service.getAvailableBatchCount()));


        myBatchCount.setValue(
                String.valueOf(
                        service.getMyBatchCount(
                                userId)));
    }


    /*
     * =========================================================
     * UPDATE FILTER COUNTS
     *
     * Keeps the existing filter labels.
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

        int onHoldCount =
                0;


        for (CheckerBatch batch : batches) {

            if (batch == null) {
                continue;
            }


            /*
             * =================================================
             * ON HOLD / RETURN TO MAKER
             * =================================================
             */

            boolean isOnHold =
                    isOnHold(batch);


            /*
             * =================================================
             * AVAILABLE
             *
             * A returned/on-hold batch must NOT be available.
             * =================================================
             */

            if (!isOnHold
                    && isAvailable(batch)) {

                availableCountValue++;
            }


            /*
             * =================================================
             * MY BATCHES
             * =================================================
             */

            if (!isOnHold
                    && isLockedByCurrentUser(batch)) {

                myBatchesCount++;
            }


            /*
             * =================================================
             * ON HOLD COUNT
             * =================================================
             */

            if (isOnHold) {

                onHoldCount++;
            }
        }


        /*
         * =====================================================
         * KEEP EXISTING FILTER LABELS
         *
         * If you want counts displayed later, these variables
         * are already calculated and can be added to labels.
         * =====================================================
         */

        allFilter.setLabel(
                "All");

        availableFilter.setLabel(
                "Available");

        myBatchesFilter.setLabel(
                "My Batches");

        if (reVerifyFilter != null) {

            reVerifyFilter.setLabel(
                    "Re-Verify Batches");
        }

        if (onHoldFilter != null) {

            onHoldFilter.setLabel(
                    "Re-Verify Batches");
        }
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

        for (CheckerBatch batch : batches) {

            if (batch == null) {
                continue;
            }


            /*
             * =================================================
             * ALL
             *
             * Shows every batch.
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
             *
             * Only batches that are not on hold/returned
             * and are currently available.
             * =================================================
             */

            else if ("AVAILABLE".equals(
                    selectedFilter)) {

                if (!isOnHold(batch)
                        && isAvailable(batch)) {

                    filteredBatches.add(
                            batch);
                }
            }


            /*
             * =================================================
             * MY BATCHES
             *
             * Only batches locked by current checker.
             *
             * RETURN_TO_MAKER / ON_HOLD batches are excluded.
             * =================================================
             */

            else if ("MY_BATCHES".equals(
                    selectedFilter)) {

                if (!isOnHold(batch)
                        && isLockedByCurrentUser(batch)) {

                    filteredBatches.add(
                            batch);
                }
            }


            /*
             * =================================================
             * RE-VERIFY BATCHES
             *
             * Includes:
             * RETURN_TO_MAKER
             * ON_HOLD
             * Resubmitted batches with returned cheque history
             * =================================================
             */

            else if ("RE_VERIFY".equals(
                    selectedFilter)
                    || "ON_HOLD".equals(
                    selectedFilter)) {

                if (isReVerify(batch)) {

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
     * CHECK WHETHER BATCH IS RE-VERIFY BATCH
     * =========================================================
     */

    private boolean isReVerify(
            CheckerBatch batch) {

        if (batch == null) {
            return false;
        }

        return batch.isReVerify() || isOnHold(batch);
    }


    /*
     * =========================================================
     * CHECK WHETHER BATCH IS ON HOLD
     *
     * RETURN_TO_MAKER is treated as ON HOLD.
     * =========================================================
     */

    private boolean isOnHold(
            CheckerBatch batch) {

        if (batch == null) {
            return false;
        }

        String status =
                batch.getBatchStatus();

        return "RETURN_TO_MAKER"
                .equalsIgnoreCase(status)

                || "ON_HOLD"
                .equalsIgnoreCase(status);
    }


    /*
     * =========================================================
     * CHECK WHETHER BATCH IS AVAILABLE
     * =========================================================
     */

    private boolean isAvailable(
            CheckerBatch batch) {

        if (batch == null) {
            return false;
        }

        String lockStatus =
                batch.getLockStatus();

        return "AVAILABLE"
                .equalsIgnoreCase(lockStatus)

                || "UNLOCKED"
                .equalsIgnoreCase(lockStatus)

                || lockStatus == null
                || lockStatus.trim().isEmpty();
    }


    /*
     * =========================================================
     * CHECK WHETHER BATCH IS LOCKED BY CURRENT CHECKER
     * =========================================================
     */

    private boolean isLockedByCurrentUser(
            CheckerBatch batch) {

        if (batch == null) {
            return false;
        }

        return "LOCKED"
                .equalsIgnoreCase(
                        batch.getLockStatus())

                && batch.getUserId() != null

                && batch.getUserId()
                        .longValue()
                        == userId;
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


        /*
         * =====================================================
         * RETURN TO MAKER / ON HOLD STATUS
         * =====================================================
         */

        if (isOnHold(batch)) {

            statusLabel.setValue(
                    "ON HOLD");

            statusLabel.setSclass(
                    "status-badge");
        }


        /*
         * =====================================================
         * LOCKED STATUS
         * =====================================================
         */

        else if ("LOCKED".equalsIgnoreCase(
                lockStatus)) {

            statusLabel.setSclass(
                    "status-badge badge-locked");
        }


        /*
         * =====================================================
         * AVAILABLE STATUS
         * =====================================================
         */

        else if ("AVAILABLE".equalsIgnoreCase(
                lockStatus)
                || "UNLOCKED".equalsIgnoreCase(
                        lockStatus)) {

            statusLabel.setSclass(
                    "status-badge badge-available");
        }


        /*
         * =====================================================
         * OTHER STATUS
         * =====================================================
         */

        else {

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

        if ("LOCKED".equalsIgnoreCase(
                lockStatus)
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
         * =====================================================
         * ON HOLD
         *
         * No Open Verification button.
         *
         * Returned/on-hold batches should go through the
         * appropriate maker flow.
         * =====================================================
         */

        if (isOnHold(batch)) {

            Label onHoldLabel =
                    new Label(
                            "On Hold");

            onHoldLabel.setSclass(
                    "status-locked");

            row.appendChild(
                    onHoldLabel);
        }


        /*
         * =====================================================
         * AVAILABLE / UNLOCKED
         * =====================================================
         */

        else if ("AVAILABLE".equalsIgnoreCase(
                lockStatus)
                || "UNLOCKED".equalsIgnoreCase(
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
                         * =================================================
                         * TRY TO LOCK BATCH
                         * =================================================
                         */

                        boolean locked =
                                service.lockBatch(
                                        batchId,
                                        userId);


                        /*
                         * =================================================
                         * LOCK SUCCESSFUL
                         * =================================================
                         */

                        if (locked) {

                            Executions.sendRedirect(
                                    "/zul/inward-checker/"
                                    + "batch-details.zul"
                                    + "?batchId="
                                    + batchId);
                        }


                        /*
                         * =================================================
                         * LOCK FAILED
                         *
                         * Someone else may have locked it.
                         * =================================================
                         */

                        else {

                            loadDashboard();
                        }
                    });


            row.appendChild(
                    openButton);
        }


        /*
         * =====================================================
         * LOCKED BY CURRENT CHECKER
         * =====================================================
         */

        else if ("LOCKED".equalsIgnoreCase(
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
         * =====================================================
         * LOCKED BY ANOTHER CHECKER
         * =====================================================
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
         * =====================================================
         * CURRENT PAGE
         *
         * Example:
         *
         * 1/5
         * 2/5
         * 5/5
         * =====================================================
         */

        currentPage.setLabel(
                currentPageNumber
                + "/"
                + totalPages);


        /*
         * =====================================================
         * PREVIOUS
         * =====================================================
         */

        previousPage.setDisabled(
                currentPageNumber <= 1);


        /*
         * =====================================================
         * NEXT
         * =====================================================
         */

        nextPage.setDisabled(
                currentPageNumber >= totalPages);


        /*
         * =====================================================
         * ALWAYS VISIBLE
         * =====================================================
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
         * =====================================================
         * RESET ALL BUTTONS
         * =====================================================
         */

        allFilter.setSclass(
                "filter-btn");

        availableFilter.setSclass(
                "filter-btn");

        myBatchesFilter.setSclass(
                "filter-btn");

        if (reVerifyFilter != null) {

            reVerifyFilter.setSclass(
                    "filter-btn");
        }

        if (onHoldFilter != null) {

            onHoldFilter.setSclass(
                    "filter-btn");
        }


        /*
         * =====================================================
         * SET ACTIVE BUTTON
         * =====================================================
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


        else if (("RE_VERIFY".equals(
                selectedFilter) || "ON_HOLD".equals(selectedFilter))) {

            if (reVerifyFilter != null) {
                reVerifyFilter.setSclass(
                        "filter-btn active-filter");
            }
            if (onHoldFilter != null) {
                onHoldFilter.setSclass(
                        "filter-btn active-filter");
            }
        }
    }
}