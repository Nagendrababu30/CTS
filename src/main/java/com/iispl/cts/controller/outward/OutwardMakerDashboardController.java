package com.iispl.cts.controller.outward;

import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;

import java.util.List;

import org.zkoss.zk.ui.Component;

import org.zkoss.zk.ui.Executions;


import org.zkoss.zk.ui.Session;

import org.zkoss.zk.ui.event.Events;

import org.zkoss.zk.ui.select.SelectorComposer;

import org.zkoss.zk.ui.select.annotation.Wire;

import org.zkoss.zul.Button;

import org.zkoss.zul.Label;

import org.zkoss.zul.ListModelList;

import org.zkoss.zul.Listbox;

import org.zkoss.zul.Listcell;

import org.zkoss.zul.Listitem;

import org.zkoss.zul.ListitemRenderer;

import org.zkoss.zul.Messagebox;

import com.iispl.cts.model.outward.ChequeProcessing;

import com.iispl.cts.model.outward.OutwardBatch;

import com.iispl.cts.model.outward.OutwardCheque;

import com.iispl.cts.model.outward.OutwardValidationResult;

import com.iispl.cts.service.outward.OutwardMakerDashboardService;

public class OutwardMakerDashboardController

        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    // =========================================================

    // ZUL COMPONENTS

    // =========================================================

    @Wire

    private Listbox batchListbox;

    @Wire

    private Label pendingDataEntryCount;

    @Wire

    private Label micrRepairCount;
    

@Wire
private Button reVerifyBatchesBtn;
    @Wire

    private Label readyToSubmitCount;

    @Wire

    private Button allBtn;

    @Wire

    private Button availableBtn;

    @Wire

    private Button myBatchesBtn;

    // =========================================================

    // PAGINATION COMPONENTS

    // =========================================================

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

    // =========================================================

    // PAGINATION

    // =========================================================

    private int currentPage = 1;

    private static final int PAGE_SIZE = 5;
    private OutwardMakerDashboardService service;
    private String currentUserId;

    // =========================================================

    // CURRENT FILTER

    // =========================================================

    private String currentFilter = "ALL";

    // =========================================================

    // AFTER COMPOSE

    // =========================================================

    @Override

    public void doAfterCompose(Component comp)

            throws Exception {

        super.doAfterCompose(comp);

        // =====================================================

        // GET LOGGED-IN USER FROM SESSION

        // =====================================================

        Session sessionUser =

                Executions.getCurrent()

                        .getSession();

        if (sessionUser == null) {

            System.out.println(

                    "No logged-in user session found."

            );

            Executions.sendRedirect(

                    "/zul/login.zul"

            );

            return;

        }

        // =====================================================

        // GET LOGGED-IN USER ID FROM SESSION

        // =====================================================

        Object sessionUserId =

                sessionUser.getAttribute(

                        "userId"

                );

        if (sessionUserId == null) {

            System.out.println(

                    "No logged-in user ID found in session."

            );

            Executions.sendRedirect(

                    "/zul/login.zul"

            );

            return;

        }

        // =====================================================

        // CONVERT SESSION USER ID TO LONG

        // =====================================================

        long userId;

        if (sessionUserId instanceof Number) {

            userId =

                    ((Number) sessionUserId)

                            .longValue();

        } else {

            try {

                userId =

                        Long.parseLong(

                                sessionUserId.toString()

                        );

            } catch (NumberFormatException e) {

                System.out.println(

                        "Invalid userId in session: "

                                + sessionUserId

                );

                Executions.sendRedirect(

                        "/zul/login.zul"

                );

                return;

            }

        }

        // =====================================================

        // DYNAMIC LOGGED-IN USER ID

        // =====================================================

        currentUserId =

                String.valueOf(userId);

        System.out.println(

                "======================================"

        );

        System.out.println(

                "OUTWARD MAKER DASHBOARD"

        );

        System.out.println(

                "Current Maker User : "

                        + currentUserId

        );

        // =====================================================

        // INITIALIZE SERVICE

        // =====================================================

        service =

                new OutwardMakerDashboardService();

        // =====================================================

        // INITIAL FILTER

        // =====================================================

        currentFilter = "ALL";

        // =====================================================

        // INITIAL PAGE

        // =====================================================

        currentPage = 1;

        // =====================================================

        // UPDATE FILTER BUTTON STYLE

        // =====================================================

        updateFilterButtonStyles();

        // =====================================================

        // REGISTER FILTER EVENTS

        // =====================================================

        registerFilterEvents();

        // =====================================================

        // REGISTER PAGINATION EVENTS

        // =====================================================

        registerPaginationEvents();

        // =====================================================

        // LOAD DASHBOARD

        // =====================================================

        loadDashboard();

        System.out.println(

                "======================================"

        );

        System.out.println(

                "doAfterCompose() END"

        );

    }

    // =========================================================

    // REGISTER FILTER EVENTS

    // =========================================================

    private void registerFilterEvents() {

        System.out.println(

                "Registering dashboard filter events..."

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

        // -----------------------------------------------------

        // ALL

        // -----------------------------------------------------

        if (allBtn != null) {

            allBtn.addEventListener(

                    Events.ON_CLICK,

                    event -> {

                        System.out.println(

                                "ALL BUTTON CLICKED"

                        );

                        currentFilter = "ALL";

                        currentPage = 1;

                        updateFilterButtonStyles();

                        loadBatches();

                    }

            );

        }

        // -----------------------------------------------------

        // AVAILABLE

        // -----------------------------------------------------

        if (availableBtn != null) {

            availableBtn.addEventListener(

                    Events.ON_CLICK,

                    event -> {

                        System.out.println(

                                "AVAILABLE BUTTON CLICKED"

                        );

                        currentFilter = "AVAILABLE";

                        currentPage = 1;

                        updateFilterButtonStyles();

                        loadBatches();

                    }

            );

        }
     // -----------------------------------------------------
     // RE-VERIFY BATCHES
     // -----------------------------------------------------
     if (reVerifyBatchesBtn != null) {
         reVerifyBatchesBtn.addEventListener(
                 Events.ON_CLICK,
                 event -> {
                     System.out.println(
                             "RE-VERIFY BATCHES BUTTON CLICKED"
                     );

                     currentFilter = "RE_VERIFY_BATCHES";
                     currentPage = 1;

                     updateFilterButtonStyles();
                     loadBatches();
                 }
         );
     }
        // -----------------------------------------------------

        // MY BATCHES

        // -----------------------------------------------------

        if (myBatchesBtn != null) {

            myBatchesBtn.addEventListener(

                    Events.ON_CLICK,

                    event -> {

                        System.out.println(

                                "MY BATCHES BUTTON CLICKED"

                        );

                        currentFilter = "MY_BATCHES";

                        currentPage = 1;

                        updateFilterButtonStyles();

                        loadBatches();

                    }

            );

        }

    }

    // =========================================================

    // REGISTER PAGINATION EVENTS

    // =========================================================

    private void registerPaginationEvents() {

        // -----------------------------------------------------

        // PREVIOUS

        // -----------------------------------------------------

        if (previousPageButton != null) {

            previousPageButton.addEventListener(

                    Events.ON_CLICK,

                    event -> {

                        if (currentPage > 1) {

                            currentPage--;

                            loadBatches();

                        }

                    }

            );

        }

        // -----------------------------------------------------

        // NEXT

        // -----------------------------------------------------

        if (nextPageButton != null) {

            nextPageButton.addEventListener(

                    Events.ON_CLICK,

                    event -> {

                        int totalPages =

                                getTotalPages();

                        if (currentPage < totalPages) {

                            currentPage++;

                            loadBatches();

                        }

                    }

            );

        }

        // -----------------------------------------------------

        // PAGE 1

        // -----------------------------------------------------

        registerPageButton(

                page1Button,

                1

        );

        // -----------------------------------------------------

        // PAGE 2

        // -----------------------------------------------------

        registerPageButton(

                page2Button,

                2

        );

        // -----------------------------------------------------

        // PAGE 3

        // -----------------------------------------------------

        registerPageButton(

                page3Button,

                3

        );

        // -----------------------------------------------------

        // PAGE 4

        // -----------------------------------------------------

        registerPageButton(

                page4Button,

                4

        );

        // -----------------------------------------------------

        // PAGE 5

        // -----------------------------------------------------

        registerPageButton(

                page5Button,

                5

        );

    }

    // =========================================================

    // REGISTER INDIVIDUAL PAGE BUTTON

    // =========================================================

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

                        loadBatches();

                    }

                }

        );

    }

    // =========================================================

    // UPDATE FILTER BUTTON STYLES

    // =========================================================

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

    // =========================================================

    // LOAD DASHBOARD

    // =========================================================

    private void loadDashboard() {

        loadBatches();

    }

  
    private void loadBatches() {

        if (batchListbox == null) {

            System.out.println(

                    "ERROR: batchListbox is NULL."

            );

            return;

        }

        if (service == null) {

            System.out.println(

                    "ERROR: service is NULL."

            );

            return;

        }

        try {

            System.out.println(

                    "======================================"

            );

            System.out.println(

                    "LOAD BATCHES"

            );

            System.out.println(

                    "Current Filter : "

                            + currentFilter

            );

            System.out.println(

                    "Current Page   : "

                            + currentPage

            );

            System.out.println(

                    "Current User   : "

                            + currentUserId

            );

            // -------------------------------------------------

            // GET ALL BATCHES

            // -------------------------------------------------

            List<OutwardBatch> batches =

                    service.getBatches();

            if (batches == null) {

                batches =

                        new ArrayList<>();

            }

            System.out.println(

                    "Total DB Batches : "

                            + batches.size()

            );

         
            List<OutwardBatch> filteredBatches =

                    new ArrayList<>();

            for (OutwardBatch batch : batches) {

                if (batch == null) {

                    continue;

                }

                if (matchesCurrentFilter(batch)) {

                    filteredBatches.add(batch);

                }

            }

            System.out.println(

                    "Filtered Batches : "

                            + filteredBatches.size()

            );

            // -------------------------------------------------

            // CALCULATE TOTAL PAGES

            // -------------------------------------------------

            int totalPages =

                    calculateTotalPages(

                            filteredBatches.size()

                    );

         
            if (totalPages == 0) {

                currentPage = 1;

            } else if (currentPage > totalPages) {

                currentPage = totalPages;

            }

            List<OutwardBatch> pageBatches =

                    getPageData(

                            filteredBatches

                    );

            System.out.println(

                    "Total Pages : "

                            + totalPages

            );

            System.out.println(

                    "Current Page : "

                            + currentPage

            );

            System.out.println(

                    "Page Batches : "

                            + pageBatches.size()

            );

            for (OutwardBatch batch : pageBatches) {

                System.out.println(

                        "--------------------------------------"

                );

                System.out.println(

                        "Batch Number : "

                                + batch.getBatchNumber()

                );

                System.out.println(

                        "Number Of Cheques : "

                                + batch.getNumberOfCheques()

                );

                System.out.println(

                        "Batch Status : "

                                + batch.getBatchStatus()

                );

                System.out.println(

                        "Maker User : "

                                + batch.getMakerUserNumber()

                );

                System.out.println(

                        "Lock Status : "

                                + batch.getLockStatus()

                );

                System.out.println(

                        "Locked By : "

                                + batch.getLockedBy()

                );

                System.out.println(

                        "Locked At : "

                                + batch.getLockedAt()

                );

            }

            // -------------------------------------------------

            // CREATE MODEL

            // -------------------------------------------------

            ListModelList<OutwardBatch> model =

                    new ListModelList<>();

            model.addAll(pageBatches);

            // -------------------------------------------------

            // RENDERER

            // -------------------------------------------------

            batchListbox.setItemRenderer(

                    new ListitemRenderer<OutwardBatch>() {

                        @Override

                        public void render(

                                Listitem item,

                                OutwardBatch batch,

                                int index)

                                throws Exception {

                            renderBatchRow(

                                    item,

                                    batch

                            );

                        }

                    }

            );

            // -------------------------------------------------

            // SET MODEL

            // -------------------------------------------------

            batchListbox.setModel(model);

            // -------------------------------------------------

            // UPDATE PAGINATION

            // -------------------------------------------------

            updatePagination(

                    filteredBatches.size(),

                    totalPages

            );

            System.out.println(

                    "Dashboard model loaded successfully."

            );

            System.out.println(

                    "======================================"

            );

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(

                    "Unable to load batches from database.\n\n"

                            + "Error: "

                            + safeExceptionMessage(e),

                    "Dashboard Error",

                    Messagebox.OK,

                    Messagebox.ERROR

            );

        }

    }

    // =========================================================

    // GET PAGE DATA

    // =========================================================

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

    // =========================================================

    // CALCULATE TOTAL PAGES

    // =========================================================

    private int calculateTotalPages(

            int totalRecords) {

        if (totalRecords <= 0) {

            return 0;

        }

        return (

                totalRecords + PAGE_SIZE - 1

        ) / PAGE_SIZE;

    }

    // =========================================================

    // GET TOTAL PAGES

    // =========================================================

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

    // =========================================================

    // UPDATE PAGINATION

    // =========================================================

    private void updatePagination(

            int totalRecords,

            int totalPages) {

        // -----------------------------------------------------

        // PREVIOUS

        // -----------------------------------------------------

        if (previousPageButton != null) {

            previousPageButton.setDisabled(

                    currentPage <= 1

                            || totalPages <= 1

            );

            previousPageButton.setSclass(

                    currentPage <= 1

                            || totalPages <= 1

                            ? "pagination-btn pagination-disabled"

                            : "pagination-btn"

            );

        }

        // -----------------------------------------------------

        // NEXT

        // -----------------------------------------------------

        if (nextPageButton != null) {

            nextPageButton.setDisabled(

                    totalPages == 0

                            || currentPage >= totalPages

            );

            nextPageButton.setSclass(

                    totalPages == 0

                            || currentPage >= totalPages

                            ? "pagination-btn pagination-disabled"

                            : "pagination-btn"

            );

        }

        // -----------------------------------------------------

        // PAGE BUTTONS

        // -----------------------------------------------------

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

        // -----------------------------------------------------

        // PAGINATION INFO

        // -----------------------------------------------------

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

    // =========================================================

    // UPDATE PAGE BUTTON

    // =========================================================
    
    
    private boolean isReVerifyBatch(OutwardBatch batch) {

        if (batch == null) {
            return false;
        }

        if (!hasValue(batch.getBatchStatus())) {
            return false;
        }

        return "SENT_TO_MAKER".equalsIgnoreCase(
                batch.getBatchStatus().trim()
        );
    }

    private void updatePageButton(

            Button button,

            int pageNumber,

            int totalPages) {

        if (button == null) {

            return;

        }

        boolean visible =

                pageNumber <= totalPages;

        button.setVisible(visible);

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

    // =========================================================

    // CHECK CURRENT FILTER

    // =========================================================

    private boolean matchesCurrentFilter(

            OutwardBatch batch) {

        if (batch == null) {

            return false;

        }

        // -----------------------------------------------------

        // ALL

        // -----------------------------------------------------

        if ("ALL".equals(currentFilter)) {

            return true;

        }
        if ("RE_VERIFY_BATCHES".equals(currentFilter)) {
            return isReVerifyBatch(batch);
        }

        // -----------------------------------------------------

        // AVAILABLE

        // -----------------------------------------------------

        if ("AVAILABLE".equals(currentFilter)) {

            return isBatchAvailable(batch);

        }

        // -----------------------------------------------------

        // MY BATCHES

        // -----------------------------------------------------

        if ("MY_BATCHES".equals(currentFilter)) {

            return isMyBatch(batch);

        }

        return true;

    }

    // =========================================================

    // CHECK AVAILABLE BATCH

    // =========================================================

    private boolean isBatchAvailable(

            OutwardBatch batch) {

        if (batch == null) {

            return false;

        }

        // -----------------------------------------------------

        // RETURNED BATCH MUST NOT APPEAR AS AVAILABLE

        // -----------------------------------------------------

        if (hasValue(batch.getBatchStatus())) {

            String status =

                    batch.getBatchStatus().trim();

            if ("HOLD".equalsIgnoreCase(status)

                    || "ON_HOLD".equalsIgnoreCase(status)

                    || "SENT_TO_MAKER".equalsIgnoreCase(status)) {

                return false;

            }

        }

        String makerUserNumber =

                batch.getMakerUserNumber();

        String lockedBy =

                batch.getLockedBy();

        String lockStatus =

                batch.getLockStatus();

        boolean hasMakerAssignment =

                hasValue(

                        makerUserNumber

                );

        boolean hasLockedBy =

                hasValue(

                        lockedBy

                );

        boolean lockStatusLocked =

                isLockedStatus(

                        lockStatus

                );

        return !hasMakerAssignment

                &&

                !hasLockedBy

                &&

                !lockStatusLocked;

    }

    // =========================================================

    // CHECK MY BATCH

    // =========================================================

    private boolean isMyBatch(

            OutwardBatch batch) {

        if (batch == null) {

            return false;

        }

        if (!hasValue(currentUserId)) {

            return false;

        }

        String makerUserNumber =

                batch.getMakerUserNumber();

        if (!hasValue(makerUserNumber)) {

            return false;

        }

        return currentUserId.trim()

                .equalsIgnoreCase(

                        makerUserNumber.trim()

                );

    }

    // =========================================================

    // RENDER ONE BATCH ROW

    // =========================================================
    private void renderBatchRow(
            Listitem item,
            OutwardBatch batch) {

        if (batch == null) {
            return;
        }

        // =====================================================
        // 1. BATCH NUMBER
        // =====================================================

        Listcell batchCell = new Listcell();

        batchCell.appendChild(
                new Label(
                        safeValue(
                                batch.getBatchNumber()
                        )
                )
        );

        item.appendChild(batchCell);

        // =====================================================
        // 2. NUMBER OF CHEQUES
        // =====================================================

        Listcell totalCell = new Listcell();

        String chequeCount =
                batch.getNumberOfCheques() == null
                        ? "0"
                        : String.valueOf(
                                batch.getNumberOfCheques()
                        );

        totalCell.appendChild(
                new Label(chequeCount)
        );

        item.appendChild(totalCell);

        // =====================================================
        // 3. STATUS
        // =====================================================

        Listcell statusCell = new Listcell();

        statusCell.appendChild(
                new Label(
                        safeValue(
                                batch.getBatchStatus()
                        )
                )
        );

        item.appendChild(statusCell);

        // =====================================================
        // ASSIGNMENT / LOCK
        // =====================================================

        String makerUserNumber =
                batch.getMakerUserNumber();

        String lockStatus =
                batch.getLockStatus();

        String lockedBy =
                batch.getLockedBy();

        boolean hasMakerAssignment =
                hasValue(
                        makerUserNumber
                );

        boolean hasLockedBy =
                hasValue(
                        lockedBy
                );

        boolean lockStatusLocked =
                isLockedStatus(
                        lockStatus
                );

        // =====================================================
        // RETURNED BATCH
        // ONLY SENT_TO_MAKER IS A RETURNED BATCH
        // =====================================================

        boolean isReturned = false;

        if (hasValue(batch.getBatchStatus())) {

            String status =
                    batch.getBatchStatus().trim();

            isReturned =
                    "SENT_TO_MAKER".equalsIgnoreCase(
                            status
                    );
        }

        // =====================================================
        // ORIGINAL MAKER
        // =====================================================

        boolean isOriginalMaker =
                hasValue(currentUserId)
                        &&
                        hasMakerAssignment
                        &&
                        currentUserId.trim()
                                .equalsIgnoreCase(
                                        makerUserNumber.trim()
                                );

        // =====================================================
        // CURRENT MAKER ALREADY OWNS NORMAL BATCH
        // =====================================================

        boolean isAssignedToCurrentMaker =
                hasMakerAssignment
                        &&
                        hasValue(currentUserId)
                        &&
                        currentUserId.trim()
                                .equalsIgnoreCase(
                                        makerUserNumber.trim()
                                );

        boolean canCurrentMakerOpen =
                isAssignedToCurrentMaker
                        &&
                        !isReturned;

        // =====================================================
        // AVAILABLE FOR NEW MAKER
        //
        // EXISTING LOCKING IS PRESERVED
        // =====================================================

        boolean isAvailable =
                !hasMakerAssignment
                        &&
                        !hasLockedBy
                        &&
                        !lockStatusLocked
                        &&
                        !isReturned;

        // =====================================================
        // 4. ACTION
        // =====================================================

        Listcell actionCell = new Listcell();

        // =====================================================
        // RETURNED BATCH + ORIGINAL MAKER
        // =====================================================

        if (isReturned
                && isOriginalMaker) {

            // Returned batches can contain different repair types.
            // Show one action per actual repair type instead of
            // opening all returned cheques together.
            appendReturnedRepairButtons(
                    actionCell,
                    batch.getBatchNumber()
            );
        }

        // =====================================================
        // NORMAL AVAILABLE CAPTURED BATCH
        // =====================================================

        else if (isAvailable) {

            Button openButton =
                    new Button("Open");

            openButton.setWidth("75px");

            openButton.setHeight("32px");

            openButton.setStyle(
                    "background:#12B76A;"
                            + "color:white;"
                            + "border:none;"
                            + "border-radius:5px;"
                            + "font-weight:bold;"
                            + "cursor:pointer;"
            );

            openButton.addEventListener(
                    Events.ON_CLICK,
                    event ->
                            openAndAssignBatch(
                                    batch.getBatchNumber()
                            )
            );

            actionCell.appendChild(
                    openButton
            );
        }

        // =====================================================
        // CURRENT MAKER ALREADY ASSIGNED / LOCKED
        //
        // DO NOT ASSIGN AGAIN
        // =====================================================

        else if (canCurrentMakerOpen) {

            Button openButton =
                    new Button("Open");

            openButton.setWidth("75px");

            openButton.setHeight("32px");

            openButton.setStyle(
                    "background:#12B76A;"
                            + "color:white;"
                            + "border:none;"
                            + "border-radius:5px;"
                            + "font-weight:bold;"
                            + "cursor:pointer;"
            );

            openButton.addEventListener(
                    Events.ON_CLICK,
                    event ->
                            openAssignedBatch(
                                    batch.getBatchNumber()
                            )
            );

            actionCell.appendChild(
                    openButton
            );
        }

        // =====================================================
        // LOCKED BY ANOTHER MAKER
        // =====================================================

        else {

            Label lockedLabel =
                    new Label("🔒 Locked");

            lockedLabel.setStyle(
                    "color:#E74C3C;"
                            + "font-weight:bold;"
            );

            actionCell.appendChild(
                    lockedLabel
            );
        }

        item.appendChild(actionCell);

        // =====================================================
        // 5. ASSIGNMENT
        // =====================================================

        Listcell assignmentCell =
                new Listcell();

        if (hasMakerAssignment) {

            assignmentCell.appendChild(
                    new Label(
                            "Maker "
                                    + makerUserNumber
                    )
            );

        } else if (hasLockedBy) {

            assignmentCell.appendChild(
                    new Label(
                            "Locked by "
                                    + lockedBy
                    )
            );

        } else {

            assignmentCell.appendChild(
                    new Label("Available")
            );
        }

        item.appendChild(assignmentCell);
    }
    
    
    private void openAndAssignBatch(
            String batchNumber) {

        if (!hasValue(batchNumber)) {

            Messagebox.show(
                    "Invalid batch number.",
                    "Batch",
                    Messagebox.OK,
                    Messagebox.ERROR
            );

            return;
        }

        String cleanBatchNumber =
                batchNumber.trim();

        String userId =
                currentUserId;

        System.out.println(
                "======================================"
        );

        System.out.println(
                "OPEN BATCH REQUEST"
        );

        System.out.println(
                "Batch : "
                        + cleanBatchNumber
        );

        System.out.println(
                "User  : "
                        + userId
        );

        System.out.println(
                "======================================"
        );

        try {

            // =================================================
            // GET CURRENT BATCH
            // =================================================

            OutwardBatch batch =
                    findBatch(
                            cleanBatchNumber
                    );

            if (batch == null) {

                Messagebox.show(
                        "Batch "
                                + cleanBatchNumber
                                + " was not found.",
                        "Batch Not Found",
                        Messagebox.OK,
                        Messagebox.ERROR
                );

                return;
            }

            // =================================================
            // DO NOT OPEN RETURNED BATCH THROUGH NORMAL FLOW
            // =================================================

            if (hasValue(batch.getBatchStatus())) {

                String batchStatus =
                        batch.getBatchStatus().trim();

                if ("HOLD".equalsIgnoreCase(batchStatus)
                        || "ON_HOLD".equalsIgnoreCase(batchStatus)
                        || "SENT_TO_MAKER".equalsIgnoreCase(batchStatus)) {

                    Messagebox.show(
                            "This batch contains cheque(s) returned by Checker.\n\n"
                                    + "Please open it through the returned-cheque workflow.",
                            "Returned Batch",
                            Messagebox.OK,
                            Messagebox.EXCLAMATION
                    );

                    return;
                }
            }

            // =================================================
            // CHECK EXISTING MAKER ASSIGNMENT
            // =================================================

            if (hasValue(
                    batch.getMakerUserNumber()
            )) {

                Messagebox.show(
                        "Batch "
                                + cleanBatchNumber
                                + " is already assigned to Maker "
                                + batch.getMakerUserNumber()
                                + ".",
                        "Batch Locked",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION
                );

                return;
            }

            // =================================================
            // CHECK LOCK
            // =================================================

            if (hasValue(
                    batch.getLockedBy()
            )) {

                Messagebox.show(
                        "Batch "
                                + cleanBatchNumber
                                + " is already locked by "
                                + batch.getLockedBy()
                                + ".",
                        "Batch Locked",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION
                );

                return;
            }

            // =================================================
            // ASSIGN + VALIDATE
            // =================================================

            OutwardValidationResult result =
                    service.assignAndValidate(
                            cleanBatchNumber,
                            userId
                    );

            // =================================================
            // ASSIGNMENT FAILED
            // =================================================

            if (result == null) {

                Messagebox.show(
                        "Batch "
                                + cleanBatchNumber
                                + " could not be opened.\n\n"
                                + "It may already be assigned "
                                + "or locked by another Maker.",
                        "Batch Locked",
                        Messagebox.OK,
                        Messagebox.ERROR
                );

                loadBatches();

                return;
            }

            // =================================================
            // RELOAD DASHBOARD
            // =================================================

            loadBatches();

            // =================================================
            // SHOW VALIDATION RESULT
            // =================================================

            showValidationResult(
                    cleanBatchNumber,
                    result
            );

            // =================================================
            // VALIDATION COUNTS
            // =================================================

            int dataEntry =
                    result.getDataEntryErrors();

            int micr =
                    result.getMicrErrors();

            int amountAccount =
                    result.getAmountAccountErrors();
            
            // =================================================
            // MICR REPAIR
            // =================================================

            if (micr > 0) {

                openMicrRepair(
                        cleanBatchNumber
                );

                return;
            }


            // =================================================
            // DATA ENTRY
            // =================================================

            if (dataEntry > 0) {

                openDataEntry(
                        cleanBatchNumber
                );

                return;
            }

            // =================================================
            // AMOUNT / ACCOUNT
            // =================================================

            if (amountAccount > 0) {

                openAmountAccount(
                        cleanBatchNumber
                );

                return;
            }

            // =================================================
            // NO ERRORS
            // =================================================

            Messagebox.show(
                    "Batch "
                            + cleanBatchNumber
                            + " is valid and ready for Checker.",
                    "Batch Ready",
                    Messagebox.OK,
                    Messagebox.INFORMATION
            );

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to open batch "
                            + cleanBatchNumber
                            + ".\n\n"
                            + "Error: "
                            + safeExceptionMessage(e),
                    "Open Batch Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }
    

    private void openHoldBatch(

            String batchNumber) {

        if (!hasValue(batchNumber)) {

            Messagebox.show(

                    "Invalid batch number.",

                    "Batch",

                    Messagebox.OK,

                    Messagebox.ERROR

            );

            return;

        }

        String cleanBatchNumber =

                batchNumber.trim();

        try {

            // =================================================

            // FIND BATCH

            // =================================================

            OutwardBatch batch =

                    findBatch(

                            cleanBatchNumber

                    );

            if (batch == null) {

                Messagebox.show(

                        "Batch "

                                + cleanBatchNumber

                                + " was not found.",

                        "Batch Not Found",

                        Messagebox.OK,

                        Messagebox.ERROR

                );

                return;

            }

            // =================================================

            // CHECK RETURNED STATUS

            // =================================================

            if (!hasValue(batch.getBatchStatus())) {

                Messagebox.show(

                        "Batch "

                                + cleanBatchNumber

                                + " has no valid status.",

                        "Invalid Batch State",

                        Messagebox.OK,

                        Messagebox.EXCLAMATION

                );

                return;

            }

            String batchStatus =

                    batch.getBatchStatus().trim();

            boolean returnedBatch =

                    "HOLD".equalsIgnoreCase(batchStatus)

                    || "ON_HOLD".equalsIgnoreCase(batchStatus)

                    || "SENT_TO_MAKER".equalsIgnoreCase(batchStatus);

            if (!returnedBatch) {

                Messagebox.show(

                        "Batch "

                                + cleanBatchNumber

                                + " is not a returned Maker batch."

                                + "\n\nCurrent status: "

                                + batchStatus,

                        "Invalid Batch State",

                        Messagebox.OK,

                        Messagebox.EXCLAMATION

                );

                return;

            }

            // =================================================

            // ONLY ORIGINAL MAKER CAN OPEN

            // =================================================

            String assignedMaker =

                    batch.getMakerUserNumber();

            if (!hasValue(assignedMaker)

                    ||

                    !hasValue(currentUserId)

                    ||

                    !currentUserId.trim()

                            .equalsIgnoreCase(

                                    assignedMaker.trim()

                            )) {

                Messagebox.show(

                        "This returned batch is assigned to Maker "

                                + safeValue(assignedMaker)

                                + ".\n\n"

                                + "Only the original Maker can process it.",

                        "Access Denied",

                        Messagebox.OK,

                        Messagebox.ERROR

                );

                return;

            }

            // =================================================

            // GET ALL RETURNED CHEQUES

            // =================================================

            List<OutwardCheque> returnedCheques =

                    service.getReturnedCheques(

                            cleanBatchNumber

                    );

            if (returnedCheques == null

                    || returnedCheques.isEmpty()) {

                Messagebox.show(

                        "No returned cheques were found for batch "

                                + cleanBatchNumber

                                + ".",

                        "Returned Cheques",

                        Messagebox.OK,

                        Messagebox.EXCLAMATION

                );

                return;

            }

            System.out.println(

                    "Returned Cheques : "

                            + returnedCheques.size()

            );

       
            OutwardCheque selectedCheque =

                    null;

            ChequeProcessing selectedProcessing =

                    null;

            for (OutwardCheque cheque :

                    returnedCheques) {

                if (cheque == null) {

                    continue;

                }

                String chequeNumber =

                        cheque.getChequeNumber();

                if (!hasValue(chequeNumber)) {

                    continue;

                }

                String chequeStatus =

                        cheque.getChequeStatus();

                if (!hasValue(chequeStatus)

                        ||

                        !"SENT_BACK_TO_MAKER".equalsIgnoreCase(

                                chequeStatus.trim()

                        )) {

                    continue;

                }

                ChequeProcessing processing =

                        service.getChequeProcessing(

                                cleanBatchNumber,

                                chequeNumber.trim()

                        );

                if (processing == null) {

                    continue;

                }

                String checkerAction =

                        processing.getCheckerAction();

                if (!hasValue(checkerAction)

                        ||

                        !"SEND_BACK".equalsIgnoreCase(

                                checkerAction.trim()

                        )) {

                    continue;

                }

                selectedCheque =

                        cheque;

                selectedProcessing =

                        processing;

                // -------------------------------------------------

                // Prefer recognized reason

                // -------------------------------------------------

                String reasonCode =

                        processing.getCheckerReasonCode();

                if (isMicrReturnReason(

                        reasonCode

                )

                        ||

                        isDataEntryReturnReason(

                                reasonCode

                        )) {

                    break;

                }

            }

            // =================================================

            // NO VALID RETURN INFORMATION

            // =================================================

            if (selectedCheque == null

                    || selectedProcessing == null) {

                Messagebox.show(

                        "No valid Checker return information was found "

                                + "for the returned cheques in batch "

                                + cleanBatchNumber

                                + ".",

                        "Checker Return",

                        Messagebox.OK,

                        Messagebox.ERROR

                );

                return;

            }

            // =================================================

            // CHEQUE NUMBER

            // =================================================

            String chequeNumber =

                    selectedCheque

                            .getChequeNumber()

                            .trim();

            // =================================================

            // CHECKER INFORMATION

            // =================================================

            String checkerAction =

                    selectedProcessing

                            .getCheckerAction();

            String checkerReasonCode =

                    selectedProcessing

                            .getCheckerReasonCode();

            String checkerRemarks =

                    selectedCheque

                            .getCheckerRemarks();

            System.out.println(

                    "======================================"

            );

            System.out.println(

                    "RETURNED CHEQUE"

            );

            System.out.println(

                    "Batch Number : "

                            + cleanBatchNumber

            );

            System.out.println(

                    "Cheque Number : "

                            + chequeNumber

            );

            System.out.println(

                    "Checker Action : "

                            + checkerAction

            );

            System.out.println(

                    "Checker Reason Code : "

                            + checkerReasonCode

            );

            System.out.println(

                    "Checker Remarks : "

                            + checkerRemarks

            );

            System.out.println(

                    "======================================"

            );

            // =================================================

            // CHECK SEND BACK ACTION

            // =================================================

            if (!hasValue(checkerAction)

                    ||

                    !"SEND_BACK".equalsIgnoreCase(

                            checkerAction.trim()

                    )) {

                Messagebox.show(

                        "Cheque "

                                + chequeNumber

                                + " is not marked as SEND_BACK "

                                + "in cheque_processing.",

                        "Invalid Return",

                        Messagebox.OK,

                        Messagebox.ERROR

                );

                return;

            }

            // =================================================

            // MICR

            // =================================================

            if (isMicrReturnReason(

                    checkerReasonCode

            )) {

                openHoldMicrRepair(

                        cleanBatchNumber,

                        chequeNumber

                );

                return;

            }

            // =================================================

            // DATA ENTRY

            // =================================================

            if (isDataEntryReturnReason(

                    checkerReasonCode

            )) {

                openHoldDataEntry(

                        cleanBatchNumber,

                        chequeNumber

                );

                return;

            }

            // =================================================

            // UNKNOWN REASON

            // =================================================

            Messagebox.show(

                    "Unknown Checker return reason.\n\n"

                            + "Reason Code: "

                            + safeValue(

                                    checkerReasonCode

                            )

                            + "\n"

                            + "Cheque: "

                            + chequeNumber,

                    "Checker Return Reason",

                    Messagebox.OK,

                    Messagebox.ERROR

            );

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(

                    "Unable to open returned cheque.\n\n"

                            + "Batch: "

                            + cleanBatchNumber

                            + "\n\n"

                            + "Error: "

                            + safeExceptionMessage(e),

                    "Returned Cheque Error",

                    Messagebox.OK,

                    Messagebox.ERROR

            );

        }

    }




    private void openAssignedBatch(
            String batchNumber) {

        if (!hasValue(batchNumber)) {
            Messagebox.show(
                    "Invalid batch number.",
                    "Batch",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
            return;
        }

        String cleanBatchNumber =
                batchNumber.trim();

        try {
            OutwardBatch batch =
                    findBatch(
                            cleanBatchNumber
                    );

            if (batch == null) {
                Messagebox.show(
                        "Batch "
                                + cleanBatchNumber
                                + " was not found.",
                        "Batch Not Found",
                        Messagebox.OK,
                        Messagebox.ERROR
                );
                return;
            }

            String makerUserNumber =
                    batch.getMakerUserNumber();

            if (!hasValue(currentUserId)
                    || !hasValue(makerUserNumber)
                    || !currentUserId.trim()
                            .equalsIgnoreCase(
                                    makerUserNumber.trim()
                            )) {

                Messagebox.show(
                        "This batch is assigned to Maker "
                                + safeValue(makerUserNumber)
                                + ".",
                        "Batch Locked",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION
                );
                return;
            }

            if (hasValue(batch.getBatchStatus())
                    && "SENT_TO_MAKER".equalsIgnoreCase(
                            batch.getBatchStatus().trim()
                    )) {

                openHoldBatch(
                        cleanBatchNumber
                );
                return;
            }

            // Already assigned to current Maker.
            // Do not assign again.
            openDataEntry(
                    cleanBatchNumber
            );

        } catch (Exception e) {
            e.printStackTrace();

            Messagebox.show(
                    "Unable to open assigned batch "
                            + cleanBatchNumber
                            + ".\n\n"
                            + "Error: "
                            + safeExceptionMessage(e),
                    "Open Batch Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }
   private void appendReturnedRepairButtons(
            Listcell actionCell,
            String batchNumber) {

        if (actionCell == null
                || !hasValue(batchNumber)) {
            return;
        }

        int micrCount = 0;
        int dataEntryCount = 0;

        try {

            List<OutwardCheque> returnedCheques =
                    service.getReturnedCheques(
                            batchNumber.trim()
                    );

            if (returnedCheques != null) {

                for (OutwardCheque cheque :
                        returnedCheques) {

                    if (cheque == null
                            || !hasValue(
                                    cheque.getChequeNumber()
                            )) {
                        continue;
                    }

                    String chequeStatus =
                            cheque.getChequeStatus();

                    if (!hasValue(chequeStatus)
                            || !"SENT_BACK_TO_MAKER"
                                    .equalsIgnoreCase(
                                            chequeStatus.trim()
                                    )) {
                        continue;
                    }

                    ChequeProcessing processing =
                            service.getChequeProcessing(
                                    batchNumber.trim(),
                                    cheque.getChequeNumber().trim()
                            );

                    if (processing == null) {
                        continue;
                    }

                    String checkerAction =
                            processing.getCheckerAction();

                    if (!hasValue(checkerAction)
                            || !"SEND_BACK"
                                    .equalsIgnoreCase(
                                            checkerAction.trim()
                                    )) {
                        continue;
                    }

                    String reasonCode =
                            processing.getCheckerReasonCode();

                    if (isMicrReturnReason(reasonCode)) {
                        micrCount++;
                    } else if (
                            isDataEntryReturnReason(
                                    reasonCode
                            )) {
                        dataEntryCount++;
                    }
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            // Preserve the previous generic Open behavior if the
            // dashboard cannot determine the repair type.
            Button openButton =
                    createReturnedRepairButton(
                            "Open",
                            "75px",
                            batchNumber,
                            null
                    );

            actionCell.appendChild(openButton);
            return;
        }

        if (micrCount > 0) {

            Button micrButton =
                    createReturnedRepairButton(
                            "MICR (" + micrCount + ")",
                            "105px",
                            batchNumber,
                            "MICR"
                    );

            actionCell.appendChild(micrButton);
        }

        if (dataEntryCount > 0) {

            Button dataEntryButton =
                    createReturnedRepairButton(
                            "Data Entry (" + dataEntryCount + ")",
                            "125px",
                            batchNumber,
                            "DATA_ENTRY"
                    );

            if (micrCount > 0) {
                dataEntryButton.setStyle(
                        "background:#2E90FA;"
                                + "color:white;"
                                + "border:none;"
                                + "border-radius:5px;"
                                + "font-weight:bold;"
                                + "cursor:pointer;"
                                + "margin-top:4px;"
                );
            }

            actionCell.appendChild(dataEntryButton);
        }

        // Unknown reason: retain old generic Open behavior.
        if (micrCount == 0
                && dataEntryCount == 0) {

            Button openButton =
                    createReturnedRepairButton(
                            "Open",
                            "75px",
                            batchNumber,
                            null
                    );

            actionCell.appendChild(openButton);
        }
    }

    // =========================================================
    // CREATE RETURNED REPAIR BUTTON
    // =========================================================

    private Button createReturnedRepairButton(
            String caption,
            String width,
            String batchNumber,
            String repairType) {

        Button button =
                new Button(caption);

        button.setWidth(width);
        button.setHeight("32px");

        button.setStyle(
                "background:#12B76A;"
                        + "color:white;"
                        + "border:none;"
                        + "border-radius:5px;"
                        + "font-weight:bold;"
                        + "cursor:pointer;"
        );

        button.addEventListener(
                Events.ON_CLICK,
                event -> {

                    if (hasValue(repairType)) {

                        openReturnedRepair(
                                batchNumber,
                                repairType
                        );

                    } else {

                        // Existing generic returned-batch behavior.
                        openHoldBatch(batchNumber);
                    }
                }
        );

        return button;
    }

    // =========================================================
    // OPEN ONE RETURNED REPAIR TYPE
    //
    // A real cheque belonging to the selected repair type is
    // selected first. The repairType is passed to the repair page
    // so that page can filter the batch to that category.
    // =========================================================

    private void openReturnedRepair(
            String batchNumber,
            String repairType) {

        if (!hasValue(batchNumber)
                || !hasValue(repairType)) {

            Messagebox.show(
                    "Batch number or repair type is missing.",
                    "Returned Repair",
                    Messagebox.OK,
                    Messagebox.ERROR
            );

            return;
        }

        String cleanBatchNumber =
                batchNumber.trim();

        String cleanRepairType =
                repairType.trim();

        try {

            OutwardBatch batch =
                    findBatch(cleanBatchNumber);

            if (batch == null) {

                Messagebox.show(
                        "Batch "
                                + cleanBatchNumber
                                + " was not found.",
                        "Batch Not Found",
                        Messagebox.OK,
                        Messagebox.ERROR
                );

                return;
            }

            String batchStatus =
                    safeValue(
                            batch.getBatchStatus()
                    ).trim();

            boolean returnedBatch =
                    "HOLD".equalsIgnoreCase(batchStatus)
                            || "ON_HOLD".equalsIgnoreCase(batchStatus)
                            || "SENT_TO_MAKER".equalsIgnoreCase(batchStatus);

            if (!returnedBatch) {

                Messagebox.show(
                        "Batch "
                                + cleanBatchNumber
                                + " is not a returned Maker batch."
                                + "\n\nCurrent status: "
                                + batchStatus,
                        "Invalid Batch State",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION
                );

                return;
            }

            String assignedMaker =
                    batch.getMakerUserNumber();

            if (!hasValue(assignedMaker)
                    || !hasValue(currentUserId)
                    || !currentUserId.trim()
                            .equalsIgnoreCase(
                                    assignedMaker.trim()
                            )) {

                Messagebox.show(
                        "This returned batch is assigned to Maker "
                                + safeValue(assignedMaker)
                                + ".\n\n"
                                + "Only the original Maker can process it.",
                        "Access Denied",
                        Messagebox.OK,
                        Messagebox.ERROR
                );

                return;
            }

            List<OutwardCheque> returnedCheques =
                    service.getReturnedCheques(
                            cleanBatchNumber
                    );

            if (returnedCheques == null
                    || returnedCheques.isEmpty()) {

                Messagebox.show(
                        "No returned cheques were found for batch "
                                + cleanBatchNumber
                                + ".",
                        "Returned Cheques",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION
                );

                return;
            }

            OutwardCheque selectedCheque = null;

            for (OutwardCheque cheque :
                    returnedCheques) {

                if (cheque == null
                        || !hasValue(
                                cheque.getChequeNumber()
                        )) {
                    continue;
                }

                String chequeStatus =
                        cheque.getChequeStatus();

                if (!hasValue(chequeStatus)
                        || !"SENT_BACK_TO_MAKER"
                                .equalsIgnoreCase(
                                        chequeStatus.trim()
                                )) {
                    continue;
                }

                ChequeProcessing processing =
                        service.getChequeProcessing(
                                cleanBatchNumber,
                                cheque.getChequeNumber().trim()
                        );

                if (processing == null) {
                    continue;
                }

                String checkerAction =
                        processing.getCheckerAction();

                if (!hasValue(checkerAction)
                        || !"SEND_BACK"
                                .equalsIgnoreCase(
                                        checkerAction.trim()
                                )) {
                    continue;
                }

                String reasonCode =
                        processing.getCheckerReasonCode();

                boolean matches =
                        "MICR".equalsIgnoreCase(
                                cleanRepairType
                        )
                                ? isMicrReturnReason(reasonCode)
                                : isDataEntryReturnReason(reasonCode);

                if (matches) {
                    selectedCheque = cheque;
                    break;
                }
            }

            if (selectedCheque == null) {

                Messagebox.show(
                        "No "
                                + cleanRepairType
                                + " repair cheques are currently available "
                                + "for batch "
                                + cleanBatchNumber
                                + ".",
                        "Repair Queue",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION
                );

                return;
            }

            String chequeNumber =
                    selectedCheque
                            .getChequeNumber()
                            .trim();

            String targetPage;

            if ("MICR".equalsIgnoreCase(
                    cleanRepairType
            )) {
                targetPage =
                        "outward-maker-micr-repair-detail.zul";
            } else {
                targetPage =
                        "outward-maker-data-entry.zul";
            }

            String url =
                    "/zul/outward/outward-maker/"
                            + targetPage
                            + "?batchNumber="
                            + encode(cleanBatchNumber)
                            + "&returnMode=HOLD"
                            + "&repairType="
                            + encode(cleanRepairType)
                            + "&chequeNumber="
                            + encode(chequeNumber);

            System.out.println(
                    "Opening returned repair : "
                            + url
            );

            Executions.sendRedirect(url);

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to open returned "
                            + cleanRepairType
                            + " repair.\n\n"
                            + "Batch: "
                            + cleanBatchNumber
                            + "\n\n"
                            + "Error: "
                            + safeExceptionMessage(e),
                    "Returned Repair Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }




    // =========================================================

    // CHECK MICR RETURN REASON

    // =========================================================

    private boolean isMicrReturnReason(

            String checkerReasonCode) {

        if (!hasValue(checkerReasonCode)) {

            return false;

        }

        String cleanReasonCode =

                checkerReasonCode.trim();

        return "MICR".equalsIgnoreCase(cleanReasonCode)

                || "MICR_CORRECTION".equalsIgnoreCase(cleanReasonCode)

                || "MICR_MISMATCH".equalsIgnoreCase(cleanReasonCode);

    }

    // =========================================================

    // CHECK DATA ENTRY RETURN REASON

    // =========================================================

    private boolean isDataEntryReturnReason(

            String checkerReasonCode) {

        if (!hasValue(checkerReasonCode)) {

            return false;

        }

        String cleanReasonCode =

                checkerReasonCode.trim();

        return "DATA_ENTRY".equalsIgnoreCase(cleanReasonCode)

                || "DATA ENTRY".equalsIgnoreCase(cleanReasonCode)

                || "DATE_CORRECTION".equalsIgnoreCase(cleanReasonCode)

                || "DATE_MISMATCH".equalsIgnoreCase(cleanReasonCode);

    }

    // =========================================================

    // OPEN HOLD DATA ENTRY

    // =========================================================

    private void openHoldDataEntry(

            String batchNumber,

            String chequeNumber) {

        if (!hasValue(batchNumber)

                || !hasValue(chequeNumber)) {

            Messagebox.show(

                    "Batch number or cheque number is missing.",

                    "Data Entry",

                    Messagebox.OK,

                    Messagebox.ERROR

            );

            return;

        }

        String url =

                "/zul/outward/outward-maker/"

                        + "outward-maker-data-entry.zul"

                        + "?batchNumber="

                        + encode(batchNumber)

                        + "&returnMode=HOLD"

                        + "&chequeNumber="

                        + encode(chequeNumber);

        System.out.println(

                "Opening HOLD Data Entry : "

                        + url

        );

        Executions.sendRedirect(url);

    }

    // =========================================================

    // OPEN HOLD MICR REPAIR

    // =========================================================

    private void openHoldMicrRepair(

            String batchNumber,

            String chequeNumber) {

        if (!hasValue(batchNumber)

                || !hasValue(chequeNumber)) {

            Messagebox.show(

                    "Batch number or cheque number is missing.",

                    "MICR Repair",

                    Messagebox.OK,

                    Messagebox.ERROR

            );

            return;

        }

        String url =

                "/zul/outward/outward-maker/"

                        + "outward-maker-micr-repair-detail.zul"

                        + "?batchNumber="

                        + encode(batchNumber)

                        + "&returnMode=HOLD"

                        + "&chequeNumber="

                        + encode(chequeNumber);

        System.out.println(

                "Opening HOLD MICR Repair : "

                        + url

        );

        Executions.sendRedirect(url);

    }

    // =========================================================

    // SHOW VALIDATION RESULT

    // =========================================================

    private void showValidationResult(

            String batchNumber,

            OutwardValidationResult result) {

        if (result == null) {

            return;

        }

        int dataEntry =

                result.getDataEntryErrors();

        int micr =

                result.getMicrErrors();

        int amountAccount =

                result.getAmountAccountErrors();

        int totalErrors =

                dataEntry

                        + micr

                        + amountAccount;

        if (totalErrors == 0) {

            Messagebox.show(

                    "Batch "

                            + batchNumber

                            + " has no validation errors.\n\n"

                            + "The batch is ready for Checker.",

                    "Validation Successful",

                    Messagebox.OK,

                    Messagebox.INFORMATION

            );

            return;

        }

        String message =

                "Batch "

                        + batchNumber

                        + " validation completed.\n\n"

                        + "Total Cheques: "

                        + result.getTotalCheques()

                        + "\n\n"

                        + "MICR Errors: "

                        + micr;

                       

        Messagebox.show(

                message,

                "Validation Required",

                Messagebox.OK,

                Messagebox.EXCLAMATION

        );

    }

    // =========================================================

    // OPEN DATA ENTRY

    // =========================================================

    private void openDataEntry(

            String batchNumber) {

        Executions.sendRedirect(

                "/zul/outward/outward-maker/"

                        + "outward-maker-data-entry.zul"

                        + "?batchNumber="

                        + encode(batchNumber)

        );

    }

    // =========================================================

    // OPEN MICR REPAIR

    // =========================================================

    private void openMicrRepair(

            String batchNumber) {

        Executions.sendRedirect(

                "/zul/outward/outward-maker/"

                        + "outward-maker-micr-repair.zul"

                        + "?batchNumber="

                        + encode(batchNumber)

        );

    }

    // =========================================================

    // OPEN AMOUNT / ACCOUNT

    // =========================================================

    private void openAmountAccount(

            String batchNumber) {

        Executions.sendRedirect(

                "/outward-maker-amount-account.zul"

                        + "?batchNumber="

                        + encode(batchNumber)

        );

    }

    // =========================================================

    // FIND BATCH

    // =========================================================

    private OutwardBatch findBatch(

            String batchNumber) {

        if (!hasValue(batchNumber)) {

            return null;

        }

        try {

            List<OutwardBatch> batches =

                    service.getBatches();

            if (batches == null) {

                return null;

            }

            for (OutwardBatch batch : batches) {

                if (batch != null

                        &&

                        batchNumber.trim()

                                .equalsIgnoreCase(

                                        safeValue(

                                                batch.getBatchNumber()

                                        )

                                )) {

                    return batch;

                }

            }

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(

                    "Unable to find batch.\n\n"

                            + "Error: "

                            + safeExceptionMessage(e),

                    "Batch Error",

                    Messagebox.OK,

                    Messagebox.ERROR

            );

        }

        return null;

    }

    // =========================================================

    // CHECK LOCK STATUS

    // =========================================================

    private boolean isLockedStatus(

            String status) {

        if (!hasValue(status)) {

            return false;

        }

        String cleanStatus =

                status.trim();

        return "LOCKED".equalsIgnoreCase(

                    cleanStatus

               )

               ||

               "IN_PROGRESS".equalsIgnoreCase(

                    cleanStatus

               )

               ||

               "ASSIGNED".equalsIgnoreCase(

                    cleanStatus

               );

    }

    // =========================================================

    // CHECK VALUE

    // =========================================================

    private boolean hasValue(

            String value) {

        return value != null

                &&

                !value.trim().isEmpty();

    }

    // =========================================================

    // SAFE STRING

    // =========================================================

    private String safeValue(

            String value) {

        if (!hasValue(value)) {

            return "-";

        }

        return value.trim();

    }

    // =========================================================

    // SAFE EXCEPTION MESSAGE

    // =========================================================

    private String safeExceptionMessage(

            Exception e) {

        if (e == null) {

            return "Unknown error";

        }

        String message =

                e.getMessage();

        if (!hasValue(message)) {

            return e.getClass()

                    .getSimpleName();

        }

        return message;

    }

    // =========================================================

    // URL ENCODING

    // =========================================================

    private String encode(

            String value) {

        try {

            return URLEncoder.encode(

                    value,

                    StandardCharsets.UTF_8

            );

        } catch (Exception e) {

            return value;

        }

    }

}