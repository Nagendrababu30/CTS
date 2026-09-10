package com.cts.inward.controller;

import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

import com.cts.inward.dao.BatchDaoImpl;
import com.cts.inward.service.BatchService;
import com.cts.inward.service.BatchServiceImpl;

public class VerifyBatchController extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    /*
     * Logged-in user ID.
     *
     * Database type:
     * user_id -> INTEGER
     */
    private Integer userId;

    /*
     * ZUL components
     */
    private Textbox searchBatch;

    private Vlayout batchTable;

    private Label showingText;

    private Button firstPage;
    private Button previousPage;
    private Button currentPage;
    private Button nextPage;
    private Button lastPage;

    /*
     * Service
     */
    private BatchService batchService;

    /*
     * Pagination
     */
    private int currentPageNumber = 1;

    private static final int PAGE_SIZE = 10;

    private int totalBatches = 0;

    @Override
    public void doAfterCompose(Component component) throws Exception {

        super.doAfterCompose(component);

        // Create service
        batchService = BatchServiceImpl.of(BatchDaoImpl.of());

        /*
         * Get logged-in user ID.
         *
         * userId is INTEGER in database,
         * therefore Integer is used in Java.
         */
        userId = (Integer) Executions.getCurrent()
                .getAttribute("userId");

        // Print current user ID
        System.out.println(
                "VERIFY BATCH CURRENT USER ID = " + userId
        );

        // Make sure user ID exists
        if (userId == null) {

            throw new IllegalStateException(
                    "Logged-in user ID not found"
            );
        }

        // Load batches locked by current user
        loadBatches();
    }

    /**
     * Load batches locked by the currently logged-in user.
     */
    private void loadBatches() {

        try {

            List<Map<String, Object>> batches =
                    batchService.getBatchesForVerification(userId);

            totalBatches = batches.size();

            currentPageNumber = 1;

            renderBatches(batches);

        } catch (Exception e) {

            e.printStackTrace();

            clearRows();

            showingText.setValue(
                    "Unable to load batches"
            );
        }
    }

    /**
     * Search batches.
     *
     * Search is also restricted to the currently logged-in user.
     */
    public void onChange$searchBatch() {

        String batchIdText = searchBatch.getValue();

        try {

            List<Map<String, Object>> batches;

            /*
             * Empty search:
             * load all batches belonging to current checker.
             */
            if (batchIdText == null
                    || batchIdText.trim().isEmpty()) {

                batches =
                        batchService.getBatchesForVerification(
                                userId
                        );

            } else {

                /*
                 * Batch ID is BIGINT in database,
                 * therefore convert the search text to Long.
                 */
                Long batchId =
                        Long.valueOf(batchIdText.trim());

                batches =
                        batchService.searchBatchesForVerification(
                                batchId,
                                userId
                        );
            }

            totalBatches = batches.size();

            currentPageNumber = 1;

            renderBatches(batches);

        } catch (NumberFormatException e) {

            /*
             * User entered something other than a number.
             */
            clearRows();

            totalBatches = 0;

            currentPageNumber = 1;

            showingText.setValue(
                    "Invalid batch ID"
            );

            updatePagination();

        } catch (Exception e) {

            e.printStackTrace();

            clearRows();

            showingText.setValue(
                    "Unable to search batches"
            );
        }
    }

    /**
     * Reload batches while preserving the current search value.
     */
    private void reloadBatches() {

        String batchIdText = searchBatch.getValue();

        List<Map<String, Object>> batches;

        try {

            /*
             * No search value.
             */
            if (batchIdText == null
                    || batchIdText.trim().isEmpty()) {

                batches =
                        batchService.getBatchesForVerification(
                                userId
                        );

            } else {

                /*
                 * Batch ID is Long.
                 */
                Long batchId =
                        Long.valueOf(batchIdText.trim());

                batches =
                        batchService.searchBatchesForVerification(
                                batchId,
                                userId
                        );
            }

            totalBatches = batches.size();

            renderBatches(batches);

        } catch (NumberFormatException e) {

            clearRows();

            totalBatches = 0;

            updateShowingText(0);

            updatePagination();

        } catch (Exception e) {

            e.printStackTrace();

            clearRows();

            showingText.setValue(
                    "Unable to reload batches"
            );
        }
    }

    /**
     * Render the batches for the current page.
     */
    private void renderBatches(
            List<Map<String, Object>> batches) {

        /*
         * Remove previous dynamic rows.
         * Header remains.
         */
        clearRows();

        /*
         * No batches.
         */
        if (batches == null || batches.isEmpty()) {

            updateShowingText(0);

            updatePagination();

            return;
        }

        /*
         * Calculate total pages.
         */
        int totalPages =
                (int) Math.ceil(
                        (double) batches.size() / PAGE_SIZE
                );

        /*
         * Make sure current page is within valid range.
         */
        if (currentPageNumber > totalPages) {

            currentPageNumber = totalPages;
        }

        /*
         * Calculate start and end indexes.
         */
        int startIndex =
                (currentPageNumber - 1) * PAGE_SIZE;

        int endIndex =
                Math.min(
                        startIndex + PAGE_SIZE,
                        batches.size()
                );

        /*
         * Create rows.
         */
        for (int i = startIndex; i < endIndex; i++) {

            createBatchRow(batches.get(i));
        }

        /*
         * Update footer text.
         */
        updateShowingText(batches.size());

        /*
         * Update pagination buttons.
         */
        updatePagination();
    }

    /**
     * Remove dynamically created rows.
     *
     * The first child of batchTable is the table header.
     */
    private void clearRows() {

        while (batchTable.getChildren().size() > 1) {

            Component child =
                    batchTable.getChildren().get(1);

            child.detach();
        }
    }

    /**
     * Create one batch table row.
     */
    private void createBatchRow(
            Map<String, Object> batch) {

        /*
         * Batch ID comes from BIGINT database column.
         *
         * DAO Map value should normally be Long.
         */
        Long batchId =
                (Long) batch.get("batchId");

        /*
         * Convert values to String only for displaying
         * them in the UI.
         */
        String batchIdText =
                String.valueOf(batchId);

        String chequeCount =
                String.valueOf(batch.get("chequeCount"));

        String makerId =
                String.valueOf(batch.get("makerId"));

        /*
         * Main row.
         */
        Hlayout row = new Hlayout();

        row.setWidth("100%");
        row.setHeight("41px");
        row.setSpacing("0");
        row.setSclass("table-row");

        /*
         * Batch ID
         */
        Hlayout batchIdCell =
                createCell(
                        batchIdText,
                        "table-cell"
                );

        batchIdCell.setHflex("26");

        /*
         * Cheque count
         */
        Hlayout chequeCell =
                createCell(
                        chequeCount,
                        "table-cell"
                );

        chequeCell.setHflex("24");

        /*
         * Maker ID
         */
        Hlayout makerCell =
                createCell(
                        makerId,
                        "table-cell maker-cell"
                );

        makerCell.setHflex("24");

        /*
         * Action cell
         */
        Hlayout actionCell = new Hlayout();

        actionCell.setHflex("26");
        actionCell.setHeight("41px");
        actionCell.setSpacing("0");
        actionCell.setValign("middle");
        actionCell.setSclass("table-cell");

        /*
         * Open button
         */
        Button openButton = new Button();

        openButton.setLabel("Open");

        openButton.setSclass("open-button");

        /*
         * Store Long batch ID in button.
         */
        openButton.setAttribute(
                "batchId",
                batchId
        );

        /*
         * Open button event.
         */
        openButton.addEventListener(
                "onClick",
                new EventListener<Event>() {

                    @Override
                    public void onEvent(Event event)
                            throws Exception {

                        Long selectedBatchId =
                                (Long) openButton
                                        .getAttribute(
                                                "batchId"
                                        );

                        openBatch(selectedBatchId);
                    }
                }
        );

        actionCell.appendChild(openButton);

        /*
         * Add cells to row.
         */
        row.appendChild(batchIdCell);

        row.appendChild(chequeCell);

        row.appendChild(makerCell);

        row.appendChild(actionCell);

        /*
         * Add row to table.
         */
        batchTable.appendChild(row);
    }

    /**
     * Create table cell.
     */
    private Hlayout createCell(
            String value,
            String cssClass) {

        Hlayout cell = new Hlayout();

        cell.setHeight("41px");
        cell.setSpacing("0");
        cell.setValign("middle");

        cell.setSclass(cssClass);

        Label label = new Label(value);

        cell.appendChild(label);

        return cell;
    }

    /**
     * Update footer text.
     *
     * Example:
     *
     * Showing 1 to 10 of 25 batches
     */
    private void updateShowingText(int total) {

        if (total == 0) {

            showingText.setValue(
                    "Showing 0 to 0 of 0 batches"
            );

            return;
        }

        int start =
                ((currentPageNumber - 1) * PAGE_SIZE) + 1;

        int end =
                Math.min(
                        currentPageNumber * PAGE_SIZE,
                        total
                );

        showingText.setValue(
                "Showing "
                        + start
                        + " to "
                        + end
                        + " of "
                        + total
                        + " batches"
        );
    }

    /**
     * Update pagination buttons.
     */
    private void updatePagination() {

        int totalPages;

        if (totalBatches == 0) {

            totalPages = 1;

        } else {

            totalPages =
                    (int) Math.ceil(
                            (double) totalBatches / PAGE_SIZE
                    );
        }

        /*
         * Current page number.
         */
        currentPage.setLabel(
                String.valueOf(currentPageNumber)
        );

        /*
         * First / Previous buttons.
         */
        boolean firstDisabled =
                currentPageNumber <= 1;

        /*
         * Next / Last buttons.
         */
        boolean lastDisabled =
                currentPageNumber >= totalPages;

        firstPage.setDisabled(firstDisabled);

        previousPage.setDisabled(firstDisabled);

        nextPage.setDisabled(lastDisabled);

        lastPage.setDisabled(lastDisabled);
    }

    /**
     * First page.
     */
    public void onClick$firstPage() {

        if (currentPageNumber <= 1) {
            return;
        }

        currentPageNumber = 1;

        reloadBatches();
    }

    /**
     * Previous page.
     */
    public void onClick$previousPage() {

        if (currentPageNumber <= 1) {
            return;
        }

        currentPageNumber--;

        reloadBatches();
    }

    /**
     * Next page.
     */
    public void onClick$nextPage() {

        int totalPages =
                (int) Math.ceil(
                        (double) totalBatches / PAGE_SIZE
                );

        if (currentPageNumber >= totalPages) {
            return;
        }

        currentPageNumber++;

        reloadBatches();
    }

    /**
     * Last page.
     */
    public void onClick$lastPage() {

        int totalPages =
                (int) Math.ceil(
                        (double) totalBatches / PAGE_SIZE
                );

        currentPageNumber =
                Math.max(totalPages, 1);

        reloadBatches();
    }

    /**
     * Open selected batch.
     *
     * Batch ID is Long.
     */
    private void openBatch(Long batchId) {

        System.out.println(
                "Opening Batch Details for Batch ID = "
                        + batchId
        );

        Executions.sendRedirect(
                "/zul/inward-checker/batch-details.zul?batchId="
                        + batchId
        );
    }
}