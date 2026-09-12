package com.cts.inward.controller;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import com.cts.admin.model.User;
import com.cts.inward.dto.MicrComparisonDto;
import com.cts.inward.dto.ReturnReasonDto;
import com.cts.inward.service.MicrRepairService;
import com.cts.inward.service.MicrRepairServiceImpl;

public class MicrRepairController
        extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    // =========================================================
    // ZUL components — top bar
    // =========================================================

    private Label  batchLabel;
    private Label  totalCountLabel;
    private Label  completedCountLabel;
    private Label  pendingCountLabel;
    private Label  chequeCounter;
    private Button backToList;

    // =========================================================
    // ZUL components — image panel
    // =========================================================

    private Image  chequeImage;
    private Button toggleImageButton;
    private Button zoomInButton;
    private Button zoomOutButton;

    // =========================================================
    // ZUL components — details panel
    // =========================================================

    private Textbox chequeNumber;
    private Label   currentStatusLabel;

    private Textbox ocrCityCode;
    private Textbox ocrBankCode;
    private Textbox ocrBranchCode;

    private Button previousButton;
    private Button saveNextButton;
    private Button returnButton;

    // =========================================================
    // ZUL components — return window
    // =========================================================

    private Window   returnWindow;
    private Combobox returnReason;
    private Textbox  returnRemarks;
    private Button   cancelReturnButton;
    private Button   confirmReturnButton;

    // =========================================================
    // State
    // =========================================================

    private MicrRepairService micrRepairService;

    private long batchId;
    private int  chequeIndex;
    private String source;

    private List<MicrComparisonDto> comparisons;
    private int totalMicrErrors; // total MICR error cheques — fixed at load time

    private String frontImagePath;
    private String backImagePath;
    private boolean showingFront = true;

    private String correctedMicr;

    // =========================================================
    // Init
    // =========================================================

    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

        micrRepairService = new MicrRepairServiceImpl();

        String batchIdParam = Executions.getCurrent().getParameter("batchId");
        String chequeIndexParam = Executions.getCurrent().getParameter("chequeIndex");

        source = Executions.getCurrent().getParameter("source");

        if (batchIdParam == null || batchIdParam.trim().isEmpty()) {
            Messagebox.show("Batch ID is missing.", "MICR Repair", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        try {
            batchId = Long.parseLong(batchIdParam.trim());
        } catch (NumberFormatException e) {
            Messagebox.show("Invalid Batch ID.", "MICR Repair", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        if (chequeIndexParam != null && !chequeIndexParam.trim().isEmpty()) {
            try {
                chequeIndex = Integer.parseInt(chequeIndexParam.trim());
            } catch (NumberFormatException e) {
                chequeIndex = micrRepairService.getNextRepairIndex(batchId);
            }
        } else {
            chequeIndex = micrRepairService.getNextRepairIndex(batchId);
        }

        initReturnWindow();

        if (chequeIndex < 0) {
            goToDataEntry();
            return;
        }

        loadBatch();
        registerEvents();
    }

    // =========================================================
    // Init return window
    // =========================================================

    private void initReturnWindow() {

        if (returnWindow == null) return;

        returnReason         = (Combobox) returnWindow.getFellow("returnReason");
        returnRemarks        = (Textbox)  returnWindow.getFellow("returnRemarks");
        cancelReturnButton   = (Button)   returnWindow.getFellow("cancelReturnButton");
        confirmReturnButton  = (Button)   returnWindow.getFellow("confirmReturnButton");
    }

    // =========================================================
    // Load batch
    // =========================================================

    private void loadBatch() {

        comparisons = micrRepairService.compareBatch(batchId);

        if (comparisons == null || comparisons.isEmpty()) {
            goToDataEntry();
            return;
        }

        // Calculate total MICR errors at load time — fixed, never changes
        totalMicrErrors = 0;
        for (MicrComparisonDto c : comparisons) {
            if (c != null && c.isNeedsMicrRepair()) totalMicrErrors++;
        }
        // Also count already-repaired ones that were originally errors
        // compareBatch excludes RETURN_BY_MAKER but includes repaired ones with needsMicrRepair=false
        // So total = all that had mismatch originally = those still pending + those already fixed
        // We derive total from comparisons that had ANY mismatch flag or !npciMicrFoundInMaster
        int originalErrors = 0;
        for (MicrComparisonDto c : comparisons) {
            if (c != null && c.hasMismatch()) originalErrors++;
        }
        totalMicrErrors = originalErrors > 0 ? originalErrors : totalMicrErrors;

        if (!isValidRepairIndex(chequeIndex)) {
            chequeIndex = findNextRepairIndex(0);
            if (chequeIndex < 0) {
                goToDataEntry();
                return;
            }
        }

        updateTopBar();
        loadCheque();
    }

    // =========================================================
    // Update top bar counts — derived from already-loaded comparisons
    // Single compareBatch() call avoids multiple DB round-trips
    // =========================================================

    private void updateTopBar() {

        batchLabel.setValue("Batch No :  " + batchId);

        if (comparisons == null) {
            totalCountLabel.setValue("Total: 0");
            completedCountLabel.setValue("Completed: 0");
            pendingCountLabel.setValue("Pending: 0");
            chequeCounter.setValue("Cheque 0 of 0");
            return;
        }

        // Total = only cheques that need/needed MICR repair (3 in your case, not 5)
        // This matches what the list screen shows as "MICR Error" count
        int total = totalMicrErrors;

        // Pending = still need repair
        int pending = 0;
        for (MicrComparisonDto c : comparisons) {
            if (c != null && c.isNeedsMicrRepair()) pending++;
        }

        // Completed = total - pending
        int completed = total - pending;

        totalCountLabel.setValue("Total: " + total);
        completedCountLabel.setValue("Completed: " + completed);
        pendingCountLabel.setValue("Pending: " + pending);

        updateChequeCounter();
    }

    private void updateChequeCounter() {

        if (comparisons == null) {
            chequeCounter.setValue("Cheque 0 of 0");
            return;
        }

        // X = position among repair-needed cheques, Y = totalMicrErrors
        int currentPosition = 0;
        int repairSeen = 0;
        for (int i = 0; i < comparisons.size(); i++) {
            MicrComparisonDto c = comparisons.get(i);
            if (c != null && c.isNeedsMicrRepair()) {
                repairSeen++;
                if (i == chequeIndex) {
                    currentPosition = repairSeen;
                }
            }
        }

        chequeCounter.setValue("Cheque " + currentPosition + " of " + totalMicrErrors);
    }

    // =========================================================
    // Load current cheque
    // =========================================================

    private void loadCheque() {

        if (comparisons == null || chequeIndex < 0 || chequeIndex >= comparisons.size()) return;

        MicrComparisonDto c = comparisons.get(chequeIndex);
        if (c == null) return;

        if (!c.isNeedsMicrRepair()) {
            int next = findNextRepairIndex(chequeIndex + 1);
            if (next < 0) {
                goToDataEntry();
                return;
            }
            chequeIndex = next;
            c = comparisons.get(chequeIndex);
        }

        updateChequeCounter();

        // Cheque number
        chequeNumber.setValue(safe(c.getChequeNumber()));

        // Current status — get actual latest from DB
        String latestStatus = "MICR_REPAIR";
        try {
            String dbStatus = micrRepairService.getFrontImagePath(c.getChequeNumber()) != null
                    ? "MICR_REPAIR" : "MICR_REPAIR";
            // Use compareBatch flag to determine display status
            latestStatus = c.isNeedsMicrRepair() ? "MICR_REPAIR" : "DATA_ENTRY";
        } catch (Exception e) {
            latestStatus = "MICR_REPAIR";
        }
        currentStatusLabel.setValue(latestStatus);
        currentStatusLabel.setSclass(
            "DATA_ENTRY".equals(latestStatus)
                ? "micr-status-value micr-status-ok"
                : "micr-status-value micr-status-error");

        // MICR fields — only show mismatched ones highlighted, rest normal
        populateMicrFields(c);

        // Images
        loadImages(c.getChequeNumber());

        previousButton.setDisabled(true);
    }

    // =========================================================
    // Populate MICR fields — only highlight the mismatched ones red
    // =========================================================

    private void populateMicrFields(MicrComparisonDto c) {

        boolean wholeMicrInvalid = !c.isNpciMicrFoundInMaster();

        // Show NPCI values (the incoming data that may be wrong)
        // Highlight red where NPCI differs from OCR (reference)

        // City Code — show NPCI value, red if mismatch with OCR
        ocrCityCode.setValue(safe(c.getNpciCityCode()));
        if (wholeMicrInvalid || c.isCityCodeMismatch()) {
            ocrCityCode.setSclass("micr-editable-field micr-field-error");
            ocrCityCode.setReadonly(false);
        } else {
            ocrCityCode.setSclass("micr-editable-field");
            ocrCityCode.setReadonly(true);
        }

        // Bank Code — show NPCI value, red if mismatch with OCR
        ocrBankCode.setValue(safe(c.getNpciBankCode()));
        if (wholeMicrInvalid || c.isBankCodeMismatch()) {
            ocrBankCode.setSclass("micr-editable-field micr-field-error");
            ocrBankCode.setReadonly(false);
        } else {
            ocrBankCode.setSclass("micr-editable-field");
            ocrBankCode.setReadonly(true);
        }

        // Branch Code — show NPCI value, red if mismatch with OCR
        ocrBranchCode.setValue(safe(c.getNpciBranchCode()));
        if (wholeMicrInvalid || c.isBranchCodeMismatch()) {
            ocrBranchCode.setSclass("micr-editable-field micr-field-error");
            ocrBranchCode.setReadonly(false);
        } else {
            ocrBranchCode.setSclass("micr-editable-field");
            ocrBranchCode.setReadonly(true);
        }
    }

    // =========================================================
    // Load images
    // =========================================================

    private void loadImages(String chequeNum) {

        frontImagePath = micrRepairService.getFrontImagePath(chequeNum);
        backImagePath  = micrRepairService.getBackImagePath(chequeNum);

        showingFront = true;
        showCurrentImage();
        toggleImageButton.setLabel("View Back");
    }

    private void showCurrentImage() {

        String path = showingFront ? frontImagePath : backImagePath;

        if (path != null && !path.trim().isEmpty()) {
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path));
                chequeImage.setContent(new org.zkoss.image.AImage(
                        showingFront ? "front.jpg" : "back.jpg", bytes));
            } catch (Exception e) {
                e.printStackTrace();
                chequeImage.setContent((org.zkoss.image.AImage) null);
            }
        } else {
            chequeImage.setContent((org.zkoss.image.AImage) null);
        }
    }

    // =========================================================
    // Save & Next
    // =========================================================

    private void saveAndNext() {

        if (comparisons == null || chequeIndex < 0 || chequeIndex >= comparisons.size()) {
            Messagebox.show("No cheque selected.", "MICR Repair", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        MicrComparisonDto c = comparisons.get(chequeIndex);
        if (c == null) return;

        if (!c.isNeedsMicrRepair()) {
            moveAfterSave();
            return;
        }

        // Validate
        String city   = ocrCityCode.getValue().trim();
        String bank   = ocrBankCode.getValue().trim();
        String branch = ocrBranchCode.getValue().trim();

        if (!city.matches("\\d{3}")) {
            Messagebox.show("City Code must be exactly 3 digits.", "MICR Repair", Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }
        if (!bank.matches("\\d{3}")) {
            Messagebox.show("Bank Code must be exactly 3 digits.", "MICR Repair", Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }
        if (!branch.matches("\\d{3}")) {
            Messagebox.show("Branch Code must be exactly 3 digits.", "MICR Repair", Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }

        correctedMicr = city + bank + branch;

        User user = (User) Executions.getCurrent().getSession().getAttribute("loggedInUser");
        if (user == null) {
            Messagebox.show("Session expired. Please login again.", "MICR Repair", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        long userId = user.getUserId();

        try {
            boolean saved = micrRepairService.saveMicrRepair(
                    c.getChequeNumber(),
                    safe(c.getNpciMicrCode()),  // original = NPCI (the wrong one being fixed)
                    correctedMicr,
                    null,
                    userId);

            if (!saved) {
                Messagebox.show("Unable to save MICR repair.", "MICR Repair", Messagebox.OK, Messagebox.ERROR);
                return;
            }

            /*
             * Update cheque status to DATA_ENTRY after repair saved.
             */
            currentStatusLabel.setValue("DATA_ENTRY");
            currentStatusLabel.setSclass("micr-status-value micr-status-ok");

            Messagebox.show(
                    "MICR repair saved.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.INFORMATION,
                    event -> moveAfterSave());

        } catch (IllegalArgumentException e) {
            Messagebox.show(e.getMessage(), "MICR Repair", Messagebox.OK, Messagebox.EXCLAMATION);
        } catch (Exception e) {
            Messagebox.show("Unable to save: " + e.getMessage(), "MICR Repair", Messagebox.OK, Messagebox.ERROR);
        }
    }

    private void moveAfterSave() {

        // Reload comparisons once — all counts derived from this
        comparisons = micrRepairService.compareBatch(batchId);

        int next = findNextRepairIndex(0);

        if (next < 0) {
            goToDataEntry();
            return;
        }

        chequeIndex = next;
        updateTopBar();
        loadCheque();
    }

    // =========================================================
    // Return cheque
    // =========================================================

    private void openReturnWindow() {

        if (returnWindow == null || returnReason == null) {
            Messagebox.show("Return dialog unavailable.", "Return", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        returnReason.getItems().clear();
        returnReason.setSelectedItem(null);
        if (returnRemarks != null) returnRemarks.setValue("");

        List<ReturnReasonDto> reasons = micrRepairService.getMakerReturnReasons();
        if (reasons != null) {
            for (ReturnReasonDto r : reasons) {
                Comboitem item = new Comboitem();
                item.setLabel(r.getDescription());
                item.setValue(r.getReturnReasonCode());
                returnReason.appendChild(item);
            }
        }

        returnWindow.setVisible(true);
    }

    private void cancelReturn() {
        if (returnReason  != null) returnReason.setSelectedItem(null);
        if (returnRemarks != null) returnRemarks.setValue("");
        if (returnWindow  != null) returnWindow.setVisible(false);
    }

    private void confirmReturn() {

        if (returnReason == null || returnReason.getSelectedItem() == null) {
            Messagebox.show("Please select a return reason.", "Return", Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }

        String code = (String) returnReason.getSelectedItem().getValue();
        if (code == null || code.trim().isEmpty()) {
            Messagebox.show("Invalid return reason.", "Return", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        if (comparisons == null || chequeIndex < 0 || chequeIndex >= comparisons.size()) {
            Messagebox.show("No cheque selected.", "Return", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        MicrComparisonDto c = comparisons.get(chequeIndex);
        if (c == null) return;

        User user = (User) Executions.getCurrent().getSession().getAttribute("loggedInUser");
        if (user == null) {
            Messagebox.show("Session expired.", "Return", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        String remarks = returnRemarks != null ? returnRemarks.getValue() : "";

        try {
            boolean saved = micrRepairService.saveMakerReturn(
                    c.getChequeNumber(), code.trim(), remarks, user.getUserId());

            if (!saved) {
                Messagebox.show("Unable to save return.", "Return", Messagebox.OK, Messagebox.ERROR);
                return;
            }

            returnWindow.setVisible(false);

            Messagebox.show(
                    "Cheque " + c.getChequeNumber() + " marked as RETURN_BY_MAKER.",
                    "Return",
                    Messagebox.OK,
                    Messagebox.INFORMATION,
                    event -> moveAfterReturn());

        } catch (Exception e) {
            Messagebox.show("Unable to return: " + e.getMessage(), "Return", Messagebox.OK, Messagebox.ERROR);
        }
    }

    private void moveAfterReturn() {

        comparisons = micrRepairService.compareBatch(batchId);
        int next = findNextRepairIndex(0);

        if (next < 0) {
            goToDataEntry();
            return;
        }

        chequeIndex = next;
        updateTopBar();
        loadCheque();
    }

    // =========================================================
    // Navigation
    // =========================================================

    private void goToDataEntry() {
        Executions.sendRedirect("/zul/inward-maker/data-entry.zul?batchId=" + batchId);
    }

    private void goBack() {
        if ("dashboard".equalsIgnoreCase(source)) {
            Executions.sendRedirect("/zul/inward-maker/dashboard.zul");
        } else {
            Executions.sendRedirect("/zul/inward-maker/micr-repair-list.zul");
        }
    }

    // =========================================================
    // Register events
    // =========================================================

    private void registerEvents() {

        // Toggle front/back image
        toggleImageButton.addEventListener(Events.ON_CLICK, event -> {
            showingFront = !showingFront;
            toggleImageButton.setLabel(showingFront ? "View Back" : "View Front");
            showCurrentImage();
        });

        // Zoom — change image height via style
        zoomInButton.addEventListener(Events.ON_CLICK, event -> {
            chequeImage.setStyle("width:100%; transform:scale(1.15); transform-origin:top left;");
        });

        zoomOutButton.addEventListener(Events.ON_CLICK, event -> {
            chequeImage.setStyle("width:100%; transform:scale(1.0); transform-origin:top left;");
        });

        saveNextButton.addEventListener(Events.ON_CLICK, event -> saveAndNext());

        returnButton.addEventListener(Events.ON_CLICK, event -> openReturnWindow());

        backToList.addEventListener(Events.ON_CLICK, event -> goBack());

        previousButton.addEventListener(Events.ON_CLICK, event ->
            Messagebox.show("Use Save & Next to advance.", "MICR Repair", Messagebox.OK, Messagebox.INFORMATION));

        if (cancelReturnButton  != null) cancelReturnButton.addEventListener(Events.ON_CLICK,  event -> cancelReturn());
        if (confirmReturnButton != null) confirmReturnButton.addEventListener(Events.ON_CLICK, event -> confirmReturn());

        // Recompute corrected MICR when codes change
        ocrCityCode.addEventListener(Events.ON_CHANGE,   event -> correctedMicr = buildMicr());
        ocrBankCode.addEventListener(Events.ON_CHANGE,   event -> correctedMicr = buildMicr());
        ocrBranchCode.addEventListener(Events.ON_CHANGE, event -> correctedMicr = buildMicr());
    }

    // =========================================================
    // Helpers
    // =========================================================

    private String buildMicr() {
        String c = ocrCityCode.getValue().trim();
        String b = ocrBankCode.getValue().trim();
        String r = ocrBranchCode.getValue().trim();
        if (c.matches("\\d{3}") && b.matches("\\d{3}") && r.matches("\\d{3}")) {
            return c + b + r;
        }
        return "";
    }

    private boolean isValidRepairIndex(int index) {
        if (comparisons == null || index < 0 || index >= comparisons.size()) return false;
        MicrComparisonDto c = comparisons.get(index);
        return c != null && c.isNeedsMicrRepair();
    }

    private int findNextRepairIndex(int start) {
        if (comparisons == null) return -1;
        for (int i = start; i < comparisons.size(); i++) {
            if (comparisons.get(i) != null && comparisons.get(i).isNeedsMicrRepair()) return i;
        }
        for (int i = 0; i < start && i < comparisons.size(); i++) {
            if (comparisons.get(i) != null && comparisons.get(i).isNeedsMicrRepair()) return i;
        }
        return -1;
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }
}
