package com.iispl.cts.controller.outward.checker;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.ListModelList;

import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.service.outward.checker.CheckerDashboardService;

public class CheckerDashboardController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    // ============================================================
    // ZUL COMPONENTS
    // ============================================================

    @Wire
    private Listbox batchListbox;

    @Wire
    private Label pendingVerificationCount;

    @Wire
    private Label cbsValidationCount;

    @Wire
    private Label readyToSendCount;

    // ============================================================
    // FILTER COMPONENTS
    // ============================================================

    @Wire
    private Button allBtn;

    @Wire
    private Button availableBtn;

    @Wire
    private Button myBatchesBtn;

    // ============================================================
    // PAGINATION COMPONENTS
    // ============================================================

    @Wire
    private Button previousPageButton;

    @Wire
    private Button nextPageButton;

    @Wire
    private Button page1Button;

    @Wire
    private Button page2Button;

    @Wire
    private Button page3Button;

    @Wire
    private Button page4Button;

    @Wire
    private Button page5Button;

    @Wire
    private Label paginationInfo;

    // ============================================================
    // SERVICE
    // ============================================================

    private CheckerDashboardService service;

    // ============================================================
    // CURRENT LOGGED-IN CHECKER USER ID
    // ============================================================

    private long currentCheckerUser;

    // ============================================================
    // FILTER
    // ============================================================

    private String currentFilter = "ALL";

    // ============================================================
    // PAGINATION
    // ============================================================

    private int currentPage = 1;

    private static final int PAGE_SIZE = 5;

    // ============================================================
    // PAGE INITIALIZATION
    // ============================================================

    @Override
    public void doAfterCompose(Component comp)
            throws Exception {

        super.doAfterCompose(comp);

        // ========================================================
        // GET ZK SESSION
        // ========================================================

        Session session =
                Executions.getCurrent().getSession();

        // ========================================================
        // NO SESSION
        // ========================================================

        if (session == null) {

            Executions.sendRedirect(
                    "/zul/login.zul"
            );

            return;
        }

        // ========================================================
        // GET USER ID FROM SESSION
        // ========================================================

        Object sessionUserId =
                session.getAttribute("userId");

        // ========================================================
        // USER ID NOT FOUND
        // ========================================================

        if (sessionUserId == null) {

            Executions.sendRedirect(
                    "/zul/login.zul"
            );

            return;
        }

        // ========================================================
        // CONVERT USER ID
        // ========================================================

        if (sessionUserId instanceof Number) {

            currentCheckerUser =
                    ((Number) sessionUserId)
                            .longValue();

        } else {

            try {

                currentCheckerUser =
                        Long.parseLong(
                                sessionUserId.toString()
                        );

            } catch (NumberFormatException e) {

                Executions.sendRedirect(
                        "/zul/login.zul"
                );

                return;
            }
        }

        // ========================================================
        // LOG CURRENT CHECKER
        // ========================================================

        System.out.println(
                "CHECKER SESSION: "
                        + "userId="
                        + currentCheckerUser
        );

        // ========================================================
        // CREATE SERVICE
        // ========================================================

        service =
                new CheckerDashboardService();

        // ========================================================
        // INITIAL FILTER
        // ========================================================

        currentFilter = "ALL";

        // ========================================================
        // INITIAL PAGE
        // ========================================================

        currentPage = 1;

        // ========================================================
        // FILTER BUTTON STYLE
        // ========================================================

        updateFilterButtonStyles();

        // ========================================================
        // REGISTER FILTER EVENTS
        // ========================================================

        registerFilterEvents();

        // ========================================================
        // REGISTER PAGINATION EVENTS
        // ========================================================

        registerPaginationEvents();

        // ========================================================
        // LOAD DASHBOARD
        // ========================================================

        loadDashboard();
    }

    // ============================================================
    // REGISTER FILTER EVENTS
    // ============================================================

    private void registerFilterEvents() {

        System.out.println(
                "Registering Checker dashboard filters..."
        );

        System.out.println(
                "allBtn = " + allBtn
        );

        System.out.println(
                "availableBtn = " + availableBtn
        );

        System.out.println(
                "myBatchesBtn = " + myBatchesBtn
        );

        // ========================================================
        // ALL
        // ========================================================

        if (allBtn != null) {

            allBtn.addEventListener(
                    Events.ON_CLICK,
                    event -> {

                        System.out.println(
                                "CHECKER ALL BUTTON CLICKED"
                        );

                        currentFilter = "ALL";

                        currentPage = 1;

                        updateFilterButtonStyles();

                        loadDashboard();
                    }
            );
        }

        // ========================================================
        // AVAILABLE
        // ========================================================

        if (availableBtn != null) {

            availableBtn.addEventListener(
                    Events.ON_CLICK,
                    event -> {

                        System.out.println(
                                "CHECKER AVAILABLE BUTTON CLICKED"
                        );

                        currentFilter = "AVAILABLE";

                        currentPage = 1;

                        updateFilterButtonStyles();

                        loadDashboard();
                    }
            );
        }

        // ========================================================
        // MY BATCHES
        // ========================================================

        if (myBatchesBtn != null) {

            myBatchesBtn.addEventListener(
                    Events.ON_CLICK,
                    event -> {

                        System.out.println(
                                "CHECKER MY BATCHES BUTTON CLICKED"
                        );

                        currentFilter = "MY_BATCHES";

                        currentPage = 1;

                        updateFilterButtonStyles();

                        loadDashboard();
                    }
            );
        }
    }

    // ============================================================
    // REGISTER PAGINATION EVENTS
    // ============================================================

    private void registerPaginationEvents() {

        // ========================================================
        // PREVIOUS
        // ========================================================

        if (previousPageButton != null) {

            previousPageButton.addEventListener(
                    Events.ON_CLICK,
                    event -> {

                        if (currentPage > 1) {

                            currentPage--;

                            loadDashboard();
                        }
                    }
            );
        }

        // ========================================================
        // NEXT
        // ========================================================

        if (nextPageButton != null) {

            nextPageButton.addEventListener(
                    Events.ON_CLICK,
                    event -> {

                        int totalPages =
                                getTotalPages();

                        if (currentPage < totalPages) {

                            currentPage++;

                            loadDashboard();
                        }
                    }
            );
        }

        // ========================================================
        // PAGE BUTTONS
        // ========================================================

        registerPageButton(
                page1Button,
                1
        );

        registerPageButton(
                page2Button,
                2
        );

        registerPageButton(
                page3Button,
                3
        );

        registerPageButton(
                page4Button,
                4
        );

        registerPageButton(
                page5Button,
                5
        );
    }

    // ============================================================
    // REGISTER PAGE BUTTON
    // ============================================================

    private void registerPageButton(
            Button button,
            int pageNumber) {

        if (button == null) {
            return;
        }

        button.addEventListener(
                Events.ON_CLICK,
                event -> {

                    int totalPages =
                            getTotalPages();

                    if (pageNumber <= totalPages) {

                        currentPage =
                                pageNumber;

                        loadDashboard();
                    }
                }
        );
    }

    // ============================================================
    // UPDATE FILTER BUTTON STYLES
    // ============================================================

    private void updateFilterButtonStyles() {

        if (allBtn != null) {

            allBtn.setSclass(
                    "filter-btn"
                            +
                            (
                                    "ALL".equals(
                                            currentFilter
                                    )
                                            ? " active-filter"
                                            : ""
                            )
            );
        }

        if (availableBtn != null) {

            availableBtn.setSclass(
                    "filter-btn"
                            +
                            (
                                    "AVAILABLE".equals(
                                            currentFilter
                                    )
                                            ? " active-filter"
                                            : ""
                            )
            );
        }

        if (myBatchesBtn != null) {

            myBatchesBtn.setSclass(
                    "filter-btn"
                            +
                            (
                                    "MY_BATCHES".equals(
                                            currentFilter
                                    )
                                            ? " active-filter"
                                            : ""
                            )
            );
        }
    }

    // ============================================================
    // LOAD DASHBOARD
    // ============================================================

    private void loadDashboard() {

        try {

            List<OutwardBatch> batches =
                    service.getBatches();

            // ----------------------------------------------------
            // KPI COUNTS
            // ----------------------------------------------------

            loadCounts(batches);

            // ----------------------------------------------------
            // BATCH TABLE
            // ----------------------------------------------------

            loadBatchList(batches);

        } catch (Exception e) {

            e.printStackTrace();

            Clients.showNotification(
                    "Unable to load Checker Dashboard.",
                    Clients.NOTIFICATION_TYPE_ERROR,
                    null,
                    "top_center",
                    4000
            );
        }
    }

    // ============================================================
    // SUMMARY COUNTS
    // ============================================================

    private void loadCounts(
            List<OutwardBatch> batches) {

        int pending = 0;

        int cbsValidation = 0;

        int readyToSend = 0;

        if (batches != null) {

            for (OutwardBatch batch : batches) {

                if (batch == null) {
                    continue;
                }

                String status =
                        batch.getBatchStatus();

                if (status == null) {
                    continue;
                }

                status =
                        status.toUpperCase();

                // =================================================
                // PENDING VERIFICATION
                // =================================================

                if ("READY_FOR_CHECKER".equals(status)
                        || "SUBMITTED".equals(status)
                        || "CHECKER_PENDING".equals(status)
                        || "PENDING_CHECKER".equals(status)) {

                    pending++;
                }

                // =================================================
                // CBS VALIDATION
                // =================================================

                if ("CBS_VALIDATION".equals(status)
                        || "PENDING_CBS_VALIDATION".equals(status)) {

                    cbsValidation++;
                }

                // =================================================
                // READY TO SEND
                // =================================================

                if ("READY_TO_SEND".equals(status)
                        || "READY_FOR_NPCI".equals(status)) {

                    readyToSend++;
                }
            }
        }

        // ========================================================
        // DISPLAY COUNTS
        // ========================================================

        pendingVerificationCount.setValue(
                String.valueOf(pending)
        );

        cbsValidationCount.setValue(
                String.valueOf(cbsValidation)
        );

        readyToSendCount.setValue(
                String.valueOf(readyToSend)
        );
    }

    // ============================================================
    // LOAD BATCH TABLE
    // ============================================================

    private void loadBatchList(
            List<OutwardBatch> batches) {

        // ========================================================
        // FILTER DATA
        // ========================================================

        List<OutwardBatch> filteredBatches =
                new ArrayList<>();

        if (batches != null) {

            for (OutwardBatch batch : batches) {

                if (batch == null) {
                    continue;
                }

                if (matchesCurrentFilter(batch)) {

                    filteredBatches.add(batch);
                }
            }
        }

        System.out.println(
                "======================================"
        );

        System.out.println(
                "CHECKER BATCH LIST"
        );

        System.out.println(
                "Filter        : "
                        + currentFilter
        );

        System.out.println(
                "Total Batches : "
                        + (batches == null
                                ? 0
                                : batches.size())
        );

        System.out.println(
                "Filtered      : "
                        + filteredBatches.size()
        );

        // ========================================================
        // TOTAL PAGES
        // ========================================================

        int totalPages =
                calculateTotalPages(
                        filteredBatches.size()
                );

        // ========================================================
        // SAFETY CHECK
        // ========================================================

        if (totalPages == 0) {

            currentPage = 1;

        } else if (currentPage > totalPages) {

            currentPage = totalPages;
        }

        // ========================================================
        // GET CURRENT PAGE DATA
        // ========================================================

        List<OutwardBatch> pageData =
                getPageData(
                        filteredBatches
                );

        System.out.println(
                "Current Page  : "
                        + currentPage
        );

        System.out.println(
                "Total Pages   : "
                        + totalPages
        );

        System.out.println(
                "Page Records  : "
                        + pageData.size()
        );

        // ========================================================
        // CREATE MODEL
        // ========================================================

        ListModelList<OutwardBatch> model =
                new ListModelList<>();

        model.addAll(pageData);

        // ========================================================
        // SET RENDERER
        // ========================================================

        batchListbox.setItemRenderer(
                new CheckerBatchRenderer()
        );

        // ========================================================
        // SET MODEL
        // ========================================================

        batchListbox.setModel(model);

        // ========================================================
        // UPDATE PAGINATION
        // ========================================================

        updatePagination(
                filteredBatches.size(),
                totalPages
        );
    }

    // ============================================================
    // CHECK CURRENT FILTER
    // ============================================================

    private boolean matchesCurrentFilter(
            OutwardBatch batch) {

        if (batch == null) {
            return false;
        }

        // ========================================================
        // ALL
        // ========================================================

        if ("ALL".equals(currentFilter)) {

            return true;
        }

        // ========================================================
        // AVAILABLE
        //
        // Checker available logic is based on lock status.
        // ========================================================

        if ("AVAILABLE".equals(currentFilter)) {

            return isBatchAvailable(batch);
        }

        // ========================================================
        // MY BATCHES
        // ========================================================

        if ("MY_BATCHES".equals(currentFilter)) {

            return isMyBatch(batch);
        }

        return true;
    }

    // ============================================================
    // CHECK AVAILABLE BATCH
    // ============================================================

    private boolean isBatchAvailable(
            OutwardBatch batch) {

        if (batch == null) {
            return false;
        }

        String lockStatus =
                batch.getLockStatus();

        return "AVAILABLE".equalsIgnoreCase(
                safe(lockStatus)
        );
    }

    // ============================================================
    // CHECK MY BATCH
    // ============================================================

    private boolean isMyBatch(
            OutwardBatch batch) {

        if (batch == null) {
            return false;
        }

        String checkerUser =
                batch.getCheckerUserNumber();

        if (checkerUser == null
                || checkerUser.trim().isEmpty()) {

            return false;
        }

        return String.valueOf(
                currentCheckerUser
        ).equalsIgnoreCase(
                checkerUser.trim()
        );
    }

    // ============================================================
    // GET CURRENT PAGE DATA
    // ============================================================

    private List<OutwardBatch> getPageData(
            List<OutwardBatch> filteredBatches) {

        List<OutwardBatch> pageData =
                new ArrayList<>();

        if (filteredBatches == null
                || filteredBatches.isEmpty()) {

            return pageData;
        }

        int startIndex =
                (currentPage - 1)
                        * PAGE_SIZE;

        if (startIndex >= filteredBatches.size()) {

            return pageData;
        }

        int endIndex =
                Math.min(
                        startIndex + PAGE_SIZE,
                        filteredBatches.size()
                );

        pageData.addAll(
                filteredBatches.subList(
                        startIndex,
                        endIndex
                )
        );

        return pageData;
    }

    // ============================================================
    // CALCULATE TOTAL PAGES
    // ============================================================

    private int calculateTotalPages(
            int totalRecords) {

        if (totalRecords <= 0) {

            return 0;
        }

        return (
                totalRecords
                        + PAGE_SIZE
                        - 1
        ) / PAGE_SIZE;
    }

    // ============================================================
    // GET TOTAL PAGES
    // ============================================================

    private int getTotalPages() {

        if (service == null) {
            return 0;
        }

        try {

            List<OutwardBatch> batches =
                    service.getBatches();

            if (batches == null) {
                return 0;
            }

            int filteredCount = 0;

            for (OutwardBatch batch : batches) {

                if (batch != null
                        &&
                        matchesCurrentFilter(batch)) {

                    filteredCount++;
                }
            }

            return calculateTotalPages(
                    filteredCount
            );

        } catch (Exception e) {

            e.printStackTrace();

            return 0;
        }
    }

    // ============================================================
    // UPDATE PAGINATION
    // ============================================================

    private void updatePagination(
            int totalRecords,
            int totalPages) {

        // ========================================================
        // PREVIOUS
        // ========================================================

        if (previousPageButton != null) {

            boolean disabled =
                    currentPage <= 1
                            || totalPages <= 1;

            previousPageButton.setDisabled(
                    disabled
            );

            previousPageButton.setSclass(
                    disabled
                            ? "pagination-btn pagination-disabled"
                            : "pagination-btn"
            );
        }

        // ========================================================
        // NEXT
        // ========================================================

        if (nextPageButton != null) {

            boolean disabled =
                    totalPages == 0
                            || currentPage >= totalPages;

            nextPageButton.setDisabled(
                    disabled
            );

            nextPageButton.setSclass(
                    disabled
                            ? "pagination-btn pagination-disabled"
                            : "pagination-btn"
            );
        }

        // ========================================================
        // PAGE BUTTONS
        // ========================================================

        updatePageButton(
                page1Button,
                1,
                totalPages
        );

        updatePageButton(
                page2Button,
                2,
                totalPages
        );

        updatePageButton(
                page3Button,
                3,
                totalPages
        );

        updatePageButton(
                page4Button,
                4,
                totalPages
        );

        updatePageButton(
                page5Button,
                5,
                totalPages
        );

        // ========================================================
        // PAGINATION INFORMATION
        // ========================================================

        if (paginationInfo != null) {

            if (totalRecords == 0) {

                paginationInfo.setValue(
                        "Showing 0 of 0"
                );

            } else {

                int start =
                        (
                                (currentPage - 1)
                                        * PAGE_SIZE
                        ) + 1;

                int end =
                        Math.min(
                                currentPage * PAGE_SIZE,
                                totalRecords
                        );

                paginationInfo.setValue(
                        "Showing "
                                + start
                                + "-"
                                + end
                                + " of "
                                + totalRecords
                );
            }
        }
    }

    // ============================================================
    // UPDATE PAGE BUTTON
    // ============================================================

    private void updatePageButton(
            Button button,
            int pageNumber,
            int totalPages) {

        if (button == null) {
            return;
        }

        boolean visible =
                pageNumber <= totalPages;

        button.setVisible(
                visible
        );

        if (!visible) {
            return;
        }

        button.setDisabled(false);

        if (currentPage == pageNumber) {

            button.setSclass(
                    "pagination-btn pagination-active"
            );

        } else {

            button.setSclass(
                    "pagination-btn"
            );
        }
    }

    // ============================================================
    // BATCH RENDERER
    // ============================================================

    private class CheckerBatchRenderer
            implements ListitemRenderer<OutwardBatch> {

        @Override
        public void render(
                Listitem item,
                OutwardBatch batch,
                int index)
                throws Exception {

            // ====================================================
            // BATCH NUMBER
            // ====================================================

            Listcell batchCell =
                    new Listcell();

            batchCell.setLabel(
                    safe(
                            batch.getBatchNumber()
                    )
            );

            item.appendChild(
                    batchCell
            );

            // ====================================================
            // CHEQUE COUNT
            // ====================================================

            Listcell chequeCell =
                    new Listcell();

            chequeCell.setLabel(
                    String.valueOf(
                            batch.getNumberOfCheques()
                    )
            );

            item.appendChild(
                    chequeCell
            );

            // ====================================================
            // STATUS
            // ====================================================

            Listcell statusCell =
                    new Listcell();

            statusCell.setLabel(
                    safe(
                            batch.getBatchStatus()
                    )
            );

            item.appendChild(
                    statusCell
            );

            // ====================================================
            // ASSIGNMENT
            // ====================================================

            Listcell assignmentCell =
                    new Listcell();

            String lockStatus =
                    batch.getLockStatus();

            if (lockStatus == null) {

                assignmentCell.setLabel(
                        "AVAILABLE"
                );

            } else if (
                    "AVAILABLE".equalsIgnoreCase(
                            lockStatus
                    )) {

                assignmentCell.setLabel(
                        "Available"
                );

            } else {

                String checker =
                        batch.getCheckerUserNumber();

                if (checker != null
                        && !checker.trim().isEmpty()) {

                    assignmentCell.setLabel(
                            "Locked by Checker "
                                    + checker
                    );

                } else {

                    assignmentCell.setLabel(
                            "Locked"
                    );
                }
            }

            item.appendChild(
                    assignmentCell
            );

            // ====================================================
            // ACTION
            // ====================================================

            Listcell actionCell =
                    new Listcell();

            boolean available =
                    "AVAILABLE".equalsIgnoreCase(
                            batch.getLockStatus()
                    );

            if (available) {

                Button openButton =
                        new Button("Open");

                openButton.setSclass(
                        "btn btn-primary"
                );

                openButton.addEventListener(
                        Events.ON_CLICK,
                        event ->
                                openBatch(batch)
                );

                actionCell.appendChild(
                        openButton
                );

            } else {

                Button lockedButton =
                        new Button("🔒 Locked");

                lockedButton.setDisabled(
                        true
                );

                actionCell.appendChild(
                        lockedButton
                );
            }

            item.appendChild(
                    actionCell
            );
        }
    }

    // ============================================================
    // OPEN BATCH
    // ============================================================

    private void openBatch(
            OutwardBatch batch) {

        if (batch == null
                || batch.getBatchNumber() == null) {

            Clients.showNotification(
                    "Invalid batch.",
                    Clients.NOTIFICATION_TYPE_ERROR,
                    null,
                    "top_center",
                    3000
            );

            return;
        }

        String batchNumber =
                batch.getBatchNumber();

        try {

            // ====================================================
            // RE-CHECK DATABASE BEFORE LOCKING
            // ====================================================

            OutwardBatch latest =
                    service.findBatch(
                            batchNumber
                    );

            if (latest == null) {

                Clients.showNotification(
                        "Batch no longer exists.",
                        Clients.NOTIFICATION_TYPE_ERROR,
                        null,
                        "top_center",
                        3000
                );

                loadDashboard();

                return;
            }

            // ====================================================
            // ALREADY LOCKED
            // ====================================================

            if (!"AVAILABLE".equalsIgnoreCase(
                    latest.getLockStatus()
            )) {

                Clients.showNotification(
                        "Batch is already locked.",
                        Clients.NOTIFICATION_TYPE_WARNING,
                        null,
                        "top_center",
                        3000
                );

                loadDashboard();

                return;
            }

            // ====================================================
            // ATTEMPT ATOMIC ASSIGNMENT
            // ====================================================

            boolean assigned =
                    service.assignBatch(
                            batchNumber,
                            String.valueOf(
                                    currentCheckerUser
                            )
                    );

            if (!assigned) {

                Clients.showNotification(
                        "Batch was already assigned to another Checker.",
                        Clients.NOTIFICATION_TYPE_WARNING,
                        null,
                        "top_center",
                        4000
                );

                loadDashboard();

                return;
            }

            // ====================================================
            // SUCCESSFUL LOCK
            // ====================================================

            Clients.showNotification(
                    "Batch assigned successfully.",
                    Clients.NOTIFICATION_TYPE_INFO,
                    null,
                    "top_center",
                    2000
            );

            // ====================================================
            // MOVE TO BATCHES QUEUE
            // ====================================================

            Executions.sendRedirect(
                    "/outward/checker/batchesQueue.zul"
                            + "?batchNumber="
                            + Executions.encodeURL(
                                    batchNumber
                            )
            );

        } catch (Exception e) {

            e.printStackTrace();

            Clients.showNotification(
                    "Unable to open batch.",
                    Clients.NOTIFICATION_TYPE_ERROR,
                    null,
                    "top_center",
                    4000
            );
        }
    }

    // ============================================================
    // SAFE STRING
    // ============================================================

    private String safe(
            String value) {

        return value == null
                ? ""
                : value;
    }
}