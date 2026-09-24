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
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;

import com.iispl.cts.model.outward.ChequeProcessing;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.model.outward.OutwardValidationResult;
import com.iispl.cts.service.outward.OutwardMakerDashboardService;
import com.cts.admin.service.SessionService;
import com.cts.admin.service.SessionServiceImpl;

public class OutwardMakerDashboardController

        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;
    
    @Wire
    private Listbox batchListbox;
    @Wire
    private Paging batchPaging;
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

private OutwardMakerDashboardService service;

    private SessionService sessionService;

    private String currentUserId;

    private String currentFilter = "ALL";
    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

        Session sessionUser =
                Executions.getCurrent().getSession();

        if (sessionUser == null) {

            Executions.sendRedirect("/zul/login.zul");

            return;
        }

        Object sessionUserId =
                sessionUser.getAttribute("userId");

        if (sessionUserId == null) {

            Executions.sendRedirect("/zul/login.zul");

            return;
        }

        long userId;

        if (sessionUserId instanceof Number) {

            userId = ((Number) sessionUserId).longValue();

        } else {

            try {

                userId =
                        Long.parseLong(
                                sessionUserId.toString()
                        );

            } catch (NumberFormatException e) {

                Executions.sendRedirect("/zul/login.zul");

                return;
            }
        }

        currentUserId =
                String.valueOf(userId);

        service =
                new OutwardMakerDashboardService();

        currentFilter = "ALL";

        updateFilterButtonStyles();

        registerFilterEvents();

        loadDashboard();

        loadSummaryCounts();
    }
    private void registerFilterEvents() {
        if (allBtn != null) {
            allBtn.addEventListener(
                    Events.ON_CLICK,
                    event -> {
                        currentFilter = "ALL";
                        updateFilterButtonStyles();
                        loadBatches();
                    }
            );
        }

        if (availableBtn != null) {
            availableBtn.addEventListener(
                    Events.ON_CLICK,
                    event -> {
                        currentFilter = "AVAILABLE";
                        updateFilterButtonStyles();
                        loadBatches();
                    }
            );
        }

        if (reVerifyBatchesBtn != null) {
            reVerifyBatchesBtn.addEventListener(
                    Events.ON_CLICK,
                    event -> {
                        currentFilter = "RE_VERIFY_BATCHES";
                        updateFilterButtonStyles();
                        loadBatches();
                    }
            );
        }

        if (myBatchesBtn != null) {
            myBatchesBtn.addEventListener(
                    Events.ON_CLICK,
                    event -> {
                        currentFilter = "MY_BATCHES";
                        updateFilterButtonStyles();
                        loadBatches();
                    }
            );
        }
    }
    private void loadSummaryCounts() {

            if (service == null) {
                return;
            }

            try {
                java.util.Map<String, Integer> counts = service.getDashboardCounts();

                if (counts != null) {
                    if (pendingDataEntryCount != null) {
                        pendingDataEntryCount.setValue(
                            String.valueOf(counts.getOrDefault("PENDING_DATA_ENTRY", 0))
                        );
                    }

                    if (micrRepairCount != null) {
                        micrRepairCount.setValue(
                            String.valueOf(counts.getOrDefault("MICR_REPAIR", 0))
                        );
                    }

                    if (readyToSubmitCount != null) {
                        readyToSubmitCount.setValue(
                            String.valueOf(counts.getOrDefault("READY_TO_SUBMIT", 0))
                        );
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch maker dashboard counts: " + safeExceptionMessage(e));
                e.printStackTrace();
            }
        }

    private void updateFilterButtonStyles() {
        if (allBtn != null) {
            allBtn.setSclass("filter-btn" + ("ALL".equals(currentFilter) ? " active-filter" : ""));
        }
        if (availableBtn != null) {
            availableBtn.setSclass("filter-btn" + ("AVAILABLE".equals(currentFilter) ? " active-filter" : ""));
        }
        if (myBatchesBtn != null) {
            myBatchesBtn.setSclass("filter-btn" + ("MY_BATCHES".equals(currentFilter) ? " active-filter" : ""));
        }
        if (reVerifyBatchesBtn != null) {
            reVerifyBatchesBtn.setSclass("filter-btn" + ("RE_VERIFY_BATCHES".equals(currentFilter) ? " active-filter" : ""));
        }
    }

    private void loadDashboard() {
        loadBatches();
    }

    private void loadBatches() {
        if (batchListbox == null || service == null) {
            return;
        }
        try {
            List<OutwardBatch> batches = service.getBatches();
            if (batches == null) {
                batches = new ArrayList<>();
            }

            List<OutwardBatch> filteredBatches = new ArrayList<>();
            for (OutwardBatch batch : batches) {
                if (batch != null && matchesCurrentFilter(batch)) {
                    filteredBatches.add(batch);
                }
            }

            ListModelList<OutwardBatch> model =
                    new ListModelList<>(filteredBatches);

            batchListbox.setItemRenderer(
                    new ListitemRenderer<OutwardBatch>() {
                        @Override
                        public void render(
                                Listitem item,
                                OutwardBatch batch,
                                int index)
                                throws Exception {
                            renderBatchRow(item, batch);
                        }
                    });

            batchListbox.setModel(model);

            if (batchPaging != null) {
                batchPaging.setPageSize(10);
                batchPaging.setDetailed(false);
                batchListbox.setPaginal(batchPaging);
            }

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

    private boolean matchesCurrentFilter(

            OutwardBatch batch) {

        if (batch == null) {

            return false;

        }

        if ("ALL".equals(currentFilter)) {

            return true;

        }
        if ("RE_VERIFY_BATCHES".equals(currentFilter)) {
            return isReVerifyBatch(batch);
        }

        if ("AVAILABLE".equals(currentFilter)) {

            return isBatchAvailable(batch);

        }

        if ("MY_BATCHES".equals(currentFilter)) {

            return isMyBatch(batch);

        }

        return true;

    }
    private boolean isBatchAvailable(OutwardBatch batch) {
        if (batch == null) {
            return false;
        }

        if (hasValue(batch.getBatchStatus())) {
            String status = batch.getBatchStatus().trim();
            if ("HOLD".equalsIgnoreCase(status)
                    || "ON_HOLD".equalsIgnoreCase(status)
                    || "SENT_TO_MAKER".equalsIgnoreCase(status)) {
                return false;
            }
        }

        String makerUserNumber = batch.getMakerUserNumber();
        String lockedBy = batch.getLockedBy();
        String lockStatus = batch.getLockStatus();

        boolean hasMakerAssignment = hasValue(makerUserNumber);
        boolean hasLockedBy = hasValue(lockedBy);
        boolean lockStatusLocked = isLockedStatus(lockStatus);

        return !hasMakerAssignment
                && !hasLockedBy
                && !lockStatusLocked;
    }

    private boolean isMyBatch(OutwardBatch batch) {
        if (batch == null) {
            return false;
        }

        if (!hasValue(currentUserId)) {
            return false;
        }

        String makerUserNumber = batch.getMakerUserNumber();

        if (!hasValue(makerUserNumber)) {
            return false;
        }

        return currentUserId.trim()
                .equalsIgnoreCase(makerUserNumber.trim());
    }

    private void renderBatchRow(Listitem item, OutwardBatch batch) {
        if (batch == null) {
            return;
        }

        Listcell batchCell = new Listcell();
        batchCell.appendChild(
                new Label(safeValue(batch.getBatchNumber()))
        );
        item.appendChild(batchCell);

        Listcell totalCell = new Listcell();

        String chequeCount =
                batch.getNumberOfCheques() == null
                        ? "0"
                        : String.valueOf(batch.getNumberOfCheques());

        totalCell.appendChild(new Label(chequeCount));
        item.appendChild(totalCell);

        Listcell statusCell = new Listcell();
        statusCell.appendChild(
                new Label(safeValue(batch.getBatchStatus()))
        );
        item.appendChild(statusCell);

        String makerUserNumber = batch.getMakerUserNumber();
        String lockStatus = batch.getLockStatus();
        String lockedBy = batch.getLockedBy();

        boolean hasMakerAssignment = hasValue(makerUserNumber);
        boolean hasLockedBy = hasValue(lockedBy);
        boolean lockStatusLocked = isLockedStatus(lockStatus);

        boolean isReturned = false;

        if (hasValue(batch.getBatchStatus())) {
            String status = batch.getBatchStatus().trim();
            isReturned = "SENT_TO_MAKER".equalsIgnoreCase(status);
        }

        boolean isOriginalMaker =
                hasValue(currentUserId)
                        && hasMakerAssignment
                        && currentUserId.trim()
                                .equalsIgnoreCase(makerUserNumber.trim());

        boolean isAssignedToCurrentMaker =
                hasMakerAssignment
                        && hasValue(currentUserId)
                        && currentUserId.trim()
                                .equalsIgnoreCase(makerUserNumber.trim());

        boolean canCurrentMakerOpen =
                isAssignedToCurrentMaker && !isReturned;

        boolean isAvailable =!hasMakerAssignment && !hasLockedBy && !lockStatusLocked && !isReturned;

        Listcell actionCell = new Listcell();

        if (isReturned && isOriginalMaker) {

            appendReturnedRepairButtons(
                    actionCell,
                    batch.getBatchNumber()
            );

        } else if (isAvailable) {

            Button openButton = new Button("Open");

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
                    event -> openAndAssignBatch(
                            batch.getBatchNumber()
                    )
            );

            actionCell.appendChild(openButton);

        } else if (canCurrentMakerOpen) {

            Hlayout actionLayout = new Hlayout();
            actionLayout.setSpacing("5px");

            Button openButton = new Button("Open");

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
                    event -> openAssignedBatch(
                            batch.getBatchNumber()
                    )
            );

            Button releaseButton = new Button("Release Lock");

            releaseButton.setWidth("105px");
            releaseButton.setHeight("32px");

            releaseButton.setStyle(
                    "background:#F04438;"
                            + "color:white;"
                            + "border:none;"
                            + "border-radius:5px;"
                            + "font-weight:bold;"
                            + "cursor:pointer;"
            );

            releaseButton.addEventListener(
                    Events.ON_CLICK,
                    event -> {
                        String batchNumber = batch.getBatchNumber();
                        releaseBatchLock(batchNumber);
                    }
            );

            actionLayout.appendChild(openButton);
            actionLayout.appendChild(releaseButton);
            actionCell.appendChild(actionLayout);

        } else {

            Label lockedLabel = new Label("🔒 Locked");

            lockedLabel.setStyle(
                    "color:#E74C3C;"
                            + "font-weight:bold;"
            );

            actionCell.appendChild(lockedLabel);
        }

        item.appendChild(actionCell);

        Listcell assignmentCell = new Listcell();

        if (hasMakerAssignment) {

            assignmentCell.appendChild(
                    new Label("Maker " + makerUserNumber)
            );

        } else if (hasLockedBy) {

            assignmentCell.appendChild(
                    new Label("Locked by " + lockedBy)
            );

        } else {

            assignmentCell.appendChild(
                    new Label("Available")
            );
        }

        item.appendChild(assignmentCell);
    }

    private void releaseBatchLock(String batchNumber) {

        if (batchNumber == null || batchNumber.trim().isEmpty()) {
            Messagebox.show(
                    "Invalid batch number.",
                    "Release Lock",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
            return;
        }

        final String cleanBatchNumber = batchNumber.trim();

        Messagebox.show(
                "Are you sure you want to release the lock for batch "
                        + cleanBatchNumber + "?\n\n"
                        + "The batch will become available again.",
                "Confirm Release Lock",
                Messagebox.YES | Messagebox.NO,
                Messagebox.QUESTION,
                event -> {

                    if (Messagebox.ON_YES.equals(event.getName())) {

                        try {

                            boolean released =
                                    service.releaseBatchLock(
                                            cleanBatchNumber,
                                            currentUserId
                                    );

                            if (released) {

                                Messagebox.show(
                                        "Batch "
                                                + cleanBatchNumber
                                                + " has been released successfully.",
                                        "Release Lock",
                                        Messagebox.OK,
                                        Messagebox.INFORMATION
                                );

                                loadBatches();

                                loadSummaryCounts();

                            } else {

                                Messagebox.show(
                                        "Unable to release the batch.\n"
                                                + "The batch may no longer be assigned to you.",
                                        "Release Lock",
                                        Messagebox.OK,
                                        Messagebox.ERROR
                                );
                            }

                        } catch (Exception e) {

                            e.printStackTrace();

                            Messagebox.show(
                                    "Error while releasing batch "
                                            + cleanBatchNumber
                                            + ".",
                                    "Release Lock",
                                    Messagebox.OK,
                                    Messagebox.ERROR
                            );
                        }
                    }
                }
        );
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

            OutwardValidationResult result =
                    service.assignAndValidate(
                            cleanBatchNumber,
                            userId
                    );

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

            loadBatches();

            showValidationResult(
                    cleanBatchNumber,
                    result
            );

            int dataEntry =
                    result.getDataEntryErrors();

            int micr =
                    result.getMicrErrors();

            int amountAccount =
                    result.getAmountAccountErrors();

            if (micr > 0) {

                openMicrRepair(
                        cleanBatchNumber
                );

                return;
            }

            else  {

                openDataEntry(
                        cleanBatchNumber
                );

              
            }

        

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

            String chequeNumber =

                    selectedCheque

                            .getChequeNumber()

                            .trim();

            String checkerAction =

                    selectedProcessing

                            .getCheckerAction();

            String checkerReasonCode =

                    selectedProcessing

                            .getCheckerReasonCode();

            String checkerRemarks =

                    selectedCheque

                            .getCheckerRemarks();

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

            if (isMicrReturnReason(

                    checkerReasonCode

            )) {

                openHoldMicrRepair(

                        cleanBatchNumber,

                        chequeNumber

                );

                return;

            }

            if (isDataEntryReturnReason(

                    checkerReasonCode

            )) {

                openHoldDataEntry(

                        cleanBatchNumber,

                        chequeNumber

                );

                return;

            }

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

                        openHoldBatch(batchNumber);
                    }
                }
        );

        return button;
    }

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

        Executions.sendRedirect(url);

    }

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

        Executions.sendRedirect(url);

    }

    private void showValidationResult(
            String batchNumber,
            OutwardValidationResult result) {

        if (result == null) {
            return;
        }

        int dataEntry = result.getDataEntryErrors();
        int micr = result.getMicrErrors();
        int amountAccount = result.getAmountAccountErrors();

        int totalErrors = dataEntry + micr + amountAccount;

        if (totalErrors == 0) {
            Messagebox.show(
                    "Batch " + batchNumber
                            + " has no validation errors.\n\n"
                            + "The batch is ready for Checker.",
                    "Validation Successful",
                    Messagebox.OK,
                    Messagebox.INFORMATION
            );
            return;
        }

        String message =
                "Batch " + batchNumber
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
    private void openDataEntry(

            String batchNumber) {

        Executions.sendRedirect(

                "/zul/outward/outward-maker/"

                        + "outward-maker-data-entry.zul"

                        + "?batchNumber="

                        + encode(batchNumber)

        );

    }

    private void openMicrRepair(

            String batchNumber) {

        Executions.sendRedirect(

                "/zul/outward/outward-maker/"

                        + "outward-maker-micr-repair.zul"

                        + "?batchNumber="

                        + encode(batchNumber)

        );

    }

    private void openAmountAccount(

            String batchNumber) {

        Executions.sendRedirect(

                "/outward-maker-amount-account.zul"

                        + "?batchNumber="

                        + encode(batchNumber)

        );

    }

    private OutwardBatch findBatch(String batchNumber) {
        if (!hasValue(batchNumber)) {
            return null;
        }

        try {
            List<OutwardBatch> batches = service.getBatches();

            if (batches == null) {
                return null;
            }

            for (OutwardBatch batch : batches) {
                if (batch != null
                        && batchNumber.trim().equalsIgnoreCase(
                                safeValue(batch.getBatchNumber()))) {
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

    private boolean isLockedStatus(String status) {
        if (!hasValue(status)) {
            return false;
        }

        String cleanStatus = status.trim();

        return "LOCKED".equalsIgnoreCase(cleanStatus)
                || "IN_PROGRESS".equalsIgnoreCase(cleanStatus)
                || "ASSIGNED".equalsIgnoreCase(cleanStatus);
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safeValue(String value) {
        if (!hasValue(value)) {
            return "-";
        }

        return value.trim();
    }

    private String safeExceptionMessage(Exception e) {
        if (e == null) {
            return "Unknown error";
        }

        String message = e.getMessage();

        if (!hasValue(message)) {
            return e.getClass().getSimpleName();
        }

        return message;
    }

    private String encode(String value) {
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