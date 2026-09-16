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

    // ==================================================a==========
    @Wire
    private Button reVerifyBatchesBtn;

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
                    ((Number) sessionUserId).longValue();

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

        System.out.println("CHECKER SESSION: "
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

        // ========================================================
        // RE-VERIFY BATCHES
        // ========================================================

        if (reVerifyBatchesBtn != null) {

            reVerifyBatchesBtn.addEventListener(
                    Events.ON_CLICK,
                    event -> {

                        System.out.println(
                                "CHECKER RE-VERIFY BATCHES BUTTON CLICKED"
                        );

                        currentFilter =
                                "RE_VERIFY_BATCHES";

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

        if (reVerifyBatchesBtn != null) {

            reVerifyBatchesBtn.setSclass(
                    "filter-btn"
                            +
                            (
                                    "RE_VERIFY_BATCHES".equals(
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
                    getDashboardBatches();

            loadCounts(batches);

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
    // GET DASHBOARD BATCHES
    // ============================================================

    private List<OutwardBatch> getDashboardBatches() {

        List<OutwardBatch> batches =
                service.getBatches(
                        String.valueOf(
                                currentCheckerUser
                        )
                );

        if (batches == null) {

            batches =
                    new ArrayList<>();
        }

        /*
         * Re-Verify eligibility is based ONLY on:
         *
         * 1. cheque_processing.checker_id
         * 2. cheque_processing.checker_action = SEND_BACK
         * 3. outward_cheque.cheque_status = RE_VERIFIED
         *
         * outward_batch.batch_status is NOT used.
         *
         * RE_VERIFY is only an in-memory UI marker.
         */

        for (OutwardBatch batch : batches) {

            if (batch == null ||
                    batch.getBatchNumber() == null) {

                continue;
            }

            boolean reVerified =
                    service.hasReVerifiedCheques(
                            batch.getBatchNumber(),
                            String.valueOf(
                                    currentCheckerUser
                            )
                    );

            if (reVerified) {

                /*
                 * UI-only marker.
                 *
                 * This does NOT update
                 * outward_batch.batch_status.
                 */

                batch.setLockStatus(
                        "RE_VERIFY"
                );
            }
        }

        return batches;
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

        if (pendingVerificationCount != null) {

            pendingVerificationCount.setValue(
                    String.valueOf(pending)
            );
        }

        if (cbsValidationCount != null) {

            cbsValidationCount.setValue(
                    String.valueOf(cbsValidation)
            );
        }

        if (readyToSendCount != null) {

            readyToSendCount.setValue(
                    String.valueOf(readyToSend)
            );
        }
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
                        + (
                                batches == null
                                        ? 0
                                        : batches.size()
                        )
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

        // ========================================================
        // RE-VERIFY BATCHES
        // ========================================================

        if ("RE_VERIFY_BATCHES".equals(currentFilter)) {

            return isReVerifyBatch(batch);
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
    // CHECK RE-VERIFY BATCH
    // ============================================================

    private boolean isReVerifyBatch(
            OutwardBatch batch) {

        if (batch == null) {

            return false;
        }

        /*
         * The DAO/service already verified:
         *
         * 1. cheque_processing.checker_id
         * 2. checker_action = SEND_BACK
         * 3. outward_cheque.cheque_status = RE_VERIFIED
         *
         * This method only checks the UI marker.
         *
         * outward_batch.batch_status is NOT checked.
         */

        return "RE_VERIFY".equalsIgnoreCase(
                safe(batch.getLockStatus())
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
                    getDashboardBatches();

            if (batches == null) {

                return 0;
            }

            int filteredCount = 0;

            for (OutwardBatch batch : batches) {

                if (batch != null
                        && matchesCurrentFilter(batch)) {

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

 // ============================================================
 // UPDATE PAGINATION
 // ============================================================

 private void updatePagination(
         int totalRecords,
         int totalPages) {

     // ========================================================
     // NO BATCHES
     // ========================================================

     if (totalRecords == 0) {

         // Hide Previous
         if (previousPageButton != null) {
             previousPageButton.setVisible(false);
         }

         // Hide Next
         if (nextPageButton != null) {
             nextPageButton.setVisible(false);
         }

         // Hide page buttons
         if (page1Button != null) {
             page1Button.setVisible(false);
         }

         if (page2Button != null) {
             page2Button.setVisible(false);
         }

         if (page3Button != null) {
             page3Button.setVisible(false);
         }

         if (page4Button != null) {
             page4Button.setVisible(false);
         }

         if (page5Button != null) {
             page5Button.setVisible(false);
         }

         // Hide "Showing 0 of 0"
         if (paginationInfo != null) {
             paginationInfo.setVisible(false);
         }

         return;
     }

     // ========================================================
     // BATCHES EXIST
     // ========================================================

     // Show pagination information
     if (paginationInfo != null) {
         paginationInfo.setVisible(true);
     }

  // Previous button
     if (previousPageButton != null) {
         previousPageButton.setVisible(totalPages > 1 && currentPage > 1);
     }

     // Next button
     if (nextPageButton != null) {
         nextPageButton.setVisible(totalPages > 1 && currentPage < totalPages);
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

         int start =
                 ((currentPage - 1)
                         * PAGE_SIZE)
                         + 1;

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

            int chequeCount =
                    batch.getNumberOfCheques();

            /*
             * IMPORTANT:
             *
             * ALL:
             *     Original batch count is displayed.
             *
             * RE_VERIFY_BATCHES:
             *     Only RE_VERIFIED cheque count is displayed.
             */

            if ("RE_VERIFY_BATCHES".equals(currentFilter)
                    && "RE_VERIFY".equalsIgnoreCase(
                            batch.getLockStatus()
                    )) {

                chequeCount =
                        service.getReVerifiedChequeCount(
                                batch.getBatchNumber(),
                                String.valueOf(
                                        currentCheckerUser
                                )
                        );
            }

            chequeCell.setLabel(
                    String.valueOf(
                            chequeCount
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

            boolean reVerifyStatus =
                    "RE_VERIFY_BATCHES".equals(currentFilter)
                    && "RE_VERIFY".equalsIgnoreCase(
                            batch.getLockStatus()
                    );

            if (reVerifyStatus) {

                statusCell.setLabel(
                        "RE-VERIFIED"
                );

            } else {

                statusCell.setLabel(
                        safe(
                                batch.getBatchStatus()
                        )
                );
            }

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

            } else if (
                    "RE_VERIFY".equalsIgnoreCase(
                            lockStatus
                    )
                    && "RE_VERIFY_BATCHES".equals(
                            currentFilter
                    )) {

                assignmentCell.setLabel(
                        "Re-Verify"
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

            boolean reVerify =
                    "RE_VERIFY_BATCHES".equals(
                            currentFilter
                    )
                    && "RE_VERIFY".equalsIgnoreCase(
                            batch.getLockStatus()
                    );

            /*
             * ADDITION:
             *
             * Original Checker must be able to open
             * his own batch again after Maker has
             * re-verified the returned cheques.
             *
             * This is determined from cheque_processing
             * and outward_cheque by the Service.
             *
             * outward_batch.batch_status is NOT used.
             */

            boolean assignedToCurrentChecker =
                    service.isAssignedToChecker(
                            batch.getBatchNumber(),
                            String.valueOf(
                                    currentCheckerUser
                            )
                    );

            boolean originalCheckerCanReVerify =
                    service.hasReVerifiedCheques(
                            batch.getBatchNumber(),
                            String.valueOf(
                                    currentCheckerUser
                            )
                    );

            if (available
                    || reVerify
                    || assignedToCurrentChecker
                    || originalCheckerCanReVerify) {

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
            // CHECK IF ANY SENT-BACK CHEQUE IS STILL WITH MAKER
            // ====================================================

            boolean pendingMakerCheques =
                    service.hasPendingMakerCheques(
                            batchNumber,
                            String.valueOf(
                                    currentCheckerUser
                            )
                    );

            /*
             * IMPORTANT:
             *
             * If even ONE cheque sent back by this
             * Checker is still not RE_VERIFIED,
             * do not open the batch.
             *
             * outward_batch.batch_status is NOT used.
             */

            if (pendingMakerCheques) {

                Clients.showNotification(
                        "Still in process by Maker.",
                        Clients.NOTIFICATION_TYPE_WARNING,
                        null,
                        "top_center",
                        4000
                );

                return;
            }

            // ====================================================
            // CHECK RE-VERIFY ELIGIBILITY
            // ====================================================

            boolean reVerifyAllowed =
                    service.hasReVerifiedCheques(
                            batchNumber,
                            String.valueOf(
                                    currentCheckerUser
                            )
                    );

            // ====================================================
            // RE-VERIFY BATCH
            // ====================================================

            if (reVerifyAllowed) {

                System.out.println(
                        "CHECKER RE-VERIFY OPEN: "
                                + batchNumber
                                + " | checker="
                                + currentCheckerUser
                );

                /*
                 * IMPORTANT:
                 *
                 * Do NOT call assignBatch().
                 *
                 * Re-Verify must not create another
                 * Checker assignment/lock.
                 *
                 * Do NOT update outward_batch.batch_status.
                 */

                Executions.sendRedirect(
                        "/zul/outward/outward-checker/processing.zul"
                                + "?batchNumber="
                                + Executions.encodeURL(
                                        batchNumber
                                )
                                + "&mode=RE_VERIFY"
                );

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
                    "/zul/outward/outward-checker/batchesQueue.zul"
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