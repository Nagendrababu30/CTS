 package com.cts.inward.controller;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import com.cts.inward.model.CheckerBatch;
import com.cts.inward.service.CheckerDashboardService;
import com.cts.inward.service.CheckerDashboardServiceImpl;

public class VerifyBatchController
        extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    /*
     * =========================================================
     * ZUL COMPONENTS
     * =========================================================
     */

    private Grid batchTable;

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
     * CURRENT USER
     * =========================================================
     */

    private long userId;


    /*
     * =========================================================
     * BATCH DATA
     *
     * Keep the complete list in memory.
     * Pagination only changes what is displayed.
     * =========================================================
     */

    private List<CheckerBatch> batches =
            new ArrayList<>();


    /*
     * =========================================================
     * PAGINATION
     * =========================================================
     */

    private static final int PAGE_SIZE = 10;

    private int currentPageNumber = 1;


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
         * GET SESSION
         * =====================================================
         */

        Session session =
                Executions.getCurrent()
                        .getSession();

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
         * CONVERT USER ID
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
         * INITIALIZE CHECKER DASHBOARD SERVICE
         *
         * Use the SAME service used by Checker Dashboard.
         * =====================================================
         */

        service =
                new CheckerDashboardServiceImpl();


        /*
         * =====================================================
         * LOAD VERIFY BATCHES
         * =====================================================
         */

        loadBatches();
    }


    /*
     * =========================================================
     * LOAD BATCHES
     * =========================================================
     *
     * Get batches from the same source as Checker Dashboard.
     *
     * Verify Batch shows batches currently locked by
     * the logged-in checker.
     *
     * =========================================================
     */

    private void loadBatches() {

        try {

            List<CheckerBatch> allBatches =
                    service.getBatches();

            batches =
                    new ArrayList<>();


            if (allBatches != null) {

                for (CheckerBatch batch :
                        allBatches) {

                    if (batch == null) {
                        continue;
                    }


                    /*
                     * =================================================
                     * ONLY CURRENT CHECKER'S BATCHES THAT ARE NOT ON HOLD
                     * Batches on hold (returned to maker) should not be displayed.
                     * =================================================
                     */

                    if (!isOnHold(batch)
                            && "LOCKED".equals(
                            batch.getLockStatus())

                            && batch.getUserId() != null

                            && batch.getUserId()
                                    .longValue()
                                    == userId) {

                        batches.add(batch);
                    }
                }
            }


            currentPageNumber = 1;

            renderCurrentPage();

        } catch (Exception e) {

            e.printStackTrace();

            batches =
                    new ArrayList<>();

            currentPageNumber = 1;

            clearRows();

            updatePagination();
        }
    }


    /*
     * =========================================================
     * IS ON HOLD
     * =========================================================
     */
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


    /*
     * =========================================================
     * RENDER CURRENT PAGE
     * =========================================================
     */

    private void renderCurrentPage() {

        clearRows();


        /*
         * =====================================================
         * NO BATCHES
         * =====================================================
         */

        if (batches == null
                || batches.isEmpty()) {

            currentPageNumber = 1;

            updatePagination();

            return;
        }


        /*
         * =====================================================
         * TOTAL PAGES
         * =====================================================
         */

        int totalPages =
                getTotalPages();


        /*
         * =====================================================
         * MAKE SURE PAGE IS VALID
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
         * INDEXES
         * =====================================================
         */

        int startIndex =
                (currentPageNumber - 1)
                        * PAGE_SIZE;

        int endIndex =
                Math.min(
                        startIndex + PAGE_SIZE,
                        batches.size());


        /*
         * =====================================================
         * CREATE ROWS
         * =====================================================
         */

        for (int i = startIndex;
                i < endIndex;
                i++) {

            CheckerBatch batch =
                    batches.get(i);

            createBatchRow(batch);
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
     * CLEAR GRID ROWS
     * =========================================================
     */

    private void clearRows() {

        if (batchTable == null) {
            return;
        }

        if (batchTable.getRows() == null) {
            return;
        }

        batchTable
                .getRows()
                .getChildren()
                .clear();
    }


    /*
     * =========================================================
     * CREATE BATCH ROW
     * =========================================================
     *
     * Values come directly from CheckerBatch,
     * exactly like Checker Dashboard.
     *
     * batch.getBatchId()
     * batch.getTotalCheques()
     * batch.getMaker()
     *
     * =========================================================
     */

    private void createBatchRow(
            CheckerBatch batch) {


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


        /*
         * =====================================================
         * TOTAL CHEQUES
         * =====================================================
         */

        Label totalChequesLabel =
                new Label(
                        String.valueOf(
                                batch.getTotalCheques()));


        /*
         * =====================================================
         * MAKER
         * =====================================================
         */

        String makerName =
                "Not Assigned";

        if (batch.getMaker() != null
                && !batch.getMaker()
                        .trim()
                        .isEmpty()) {

            makerName =
                    batch.getMaker();
        }

        Label makerLabel =
                new Label(
                        makerName);

        makerLabel.setSclass(
                "maker-name");


        /*
         * =====================================================
         * OPEN BUTTON
         * =====================================================
         */

        Button openButton =
                new Button(
                        "Open");

        openButton.setSclass(
                "btn btn-action");


        /*
         * Store batch ID.
         */

        openButton.setAttribute(
                "batchId",
                batch.getBatchId());


        /*
         * Open event.
         */

        openButton.addEventListener(
                "onClick",
                event -> {

                    long selectedBatchId =
                            ((Number)
                                    openButton
                                            .getAttribute(
                                                    "batchId"))
                                    .longValue();

                    openBatch(
                            selectedBatchId);
                });


        /*
         * =====================================================
         * CREATE GRID ROW
         * =====================================================
         */

        Row row =
                new Row();


        /*
         * Add cells.
         */

        row.appendChild(
                batchIdLabel);

        row.appendChild(
                totalChequesLabel);

        row.appendChild(
                makerLabel);

        row.appendChild(
                openButton);


        /*
         * Add row to grid.
         */

        row.setParent(
                batchTable.getRows());
    }


    /*
     * =========================================================
     * TOTAL PAGES
     * =========================================================
     */

    private int getTotalPages() {

        if (batches == null
                || batches.isEmpty()) {

            return 1;
        }

        return (int) Math.ceil(
                (double) batches.size()
                        / PAGE_SIZE);
    }


    /*
     * =========================================================
     * UPDATE PAGINATION
     * =========================================================
     *
     * Required display:
     *
     * ← Previous    1/1    Next →
     *
     * =========================================================
     */

    private void updatePagination() {

        int totalPages =
                getTotalPages();


        /*
         * Current page:
         *
         * 1/1
         * 1/5
         * 2/5
         * etc.
         */

        currentPage.setLabel(
                currentPageNumber
                        + "/"
                        + totalPages);


        /*
         * Previous disabled on first page.
         */

        previousPage.setDisabled(
                currentPageNumber <= 1);


        /*
         * Next disabled on last page.
         */

        nextPage.setDisabled(
                currentPageNumber
                        >= totalPages);
    }


    /*
     * =========================================================
     * PREVIOUS PAGE
     * =========================================================
     */

    public void onClick$previousPage() {

        if (currentPageNumber <= 1) {
            return;
        }

        currentPageNumber--;

        renderCurrentPage();
    }


    /*
     * =========================================================
     * NEXT PAGE
     * =========================================================
     */

    public void onClick$nextPage() {

        int totalPages =
                getTotalPages();

        if (currentPageNumber
                >= totalPages) {

            return;
        }

        currentPageNumber++;

        renderCurrentPage();
    }


    /*
     * =========================================================
     * OPEN BATCH
     * =========================================================
     */

    private void openBatch(
            long batchId) {

        System.out.println(
                "Opening Batch Details for Batch ID = "
                        + batchId);

        Executions.sendRedirect(
                "/zul/inward-checker/"
                        + "batch-details.zul"
                        + "?batchId="
                        + batchId);
    }
}