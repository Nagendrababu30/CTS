package com.cts.inward.controller;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.util.Clients;

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
import com.cts.inward.dao.ChequeDaoImpl;
import com.cts.inward.dto.MicrComparisonDto;
import com.cts.inward.dto.ReturnReasonDto;
import com.cts.inward.service.ChequeService;
import com.cts.inward.service.ChequeServiceImpl;
import com.cts.inward.service.MicrRepairService;
import com.cts.inward.service.MicrRepairServiceImpl;

public class MicrRepairController
        extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    // =========================================================
    // Status constants
    // =========================================================

    private static final String STATUS_MICR_REPAIR =
            "MICR_REPAIR";

    private static final String STATUS_MICR_REPAIRED =
            "MICR_REPAIRED";

    private static final String STATUS_DATA_ENTRY =
            "DATA_ENTRY";

    private static final String STATUS_RETURN_BY_MAKER =
            "RETURN_BY_MAKER";

    // =========================================================
    // ZUL components — top bar
    // =========================================================

    private Label batchLabel;
    private Label totalCountLabel;
    private Label completedCountLabel;
    private Label pendingCountLabel;
    private Label chequeCounter;

    private Button backToList;

    // =========================================================
    // ZUL components — return reason banner
    // =========================================================

    private org.zkoss.zul.Div returnReasonBanner;
    private Label lblReturnReason;
    private Label lblReturnRemarks;
    private org.zkoss.zul.Hlayout rowReturnRemarks;

    // =========================================================
    // ZUL components — image panel
    // =========================================================

    private Image chequeImage;
    private Button toggleImageButton;
    private Button zoomInButton;
    private Button zoomOutButton;
    private Button rotateButton;
    private Button resetViewButton;

    // =========================================================
    // ZUL components — details panel
    // =========================================================

    private Textbox chequeNumber;
    private Label currentStatusLabel;

    private Textbox ocrCityCode;
    private Textbox ocrBankCode;
    private Textbox ocrBranchCode;

    private Button previousButton;
    private Button nextButton;
    private Button saveNextButton;
    private Button returnButton;

    // =========================================================
    // ZUL components — return window
    // =========================================================

    private Window returnWindow;
    private Combobox returnReason;
    private Textbox returnRemarks;

    private Button cancelReturnButton;
    private Button confirmReturnButton;

    // =========================================================
    // State
    // =========================================================

    private MicrRepairService micrRepairService;
    private ChequeService chequeService;

    private long batchId;

    /**
     * Index in the complete comparison list.
     */
    private int chequeIndex;

    private String source;

    private List<MicrComparisonDto> comparisons;

    /**
     * Number of cheques that originally entered
     * MICR Repair when the page was opened.
     */
    private int totalMicrErrors;

    /**
     * Current pending repair indexes.
     */
    private List<Integer> originalRepairIndexes;

    private String frontImagePath;
    private String backImagePath;

    private boolean showingFront = true;
    private double currentScale = 1.0;
    private int currentRotation = 0;

    private String correctedMicr;

    private Long loggedInUserId;

    private void loadLoggedInUser() {
        org.zkoss.zk.ui.Session session = Executions.getCurrent().getSession();
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null) {
            loggedInUserId = user.getUserId();
        }
    }

    // =========================================================
    // Init
    // =========================================================

    @Override
    public void doAfterCompose(Component comp)
            throws Exception {

        super.doAfterCompose(comp);

        micrRepairService =
                new MicrRepairServiceImpl();

        chequeService =
                ChequeServiceImpl.of(ChequeDaoImpl.of());

        loadLoggedInUser();

        String batchIdParam =
                Executions.getCurrent()
                        .getParameter("batchId");

        String chequeIndexParam =
                Executions.getCurrent()
                        .getParameter("chequeIndex");

        source =
                Executions.getCurrent()
                        .getParameter("source");

        if (batchIdParam == null
                || batchIdParam.trim().isEmpty()) {

            Messagebox.show(
                    "Batch ID is missing.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        try {

            batchId =
                    Long.parseLong(
                            batchIdParam.trim());

        } catch (NumberFormatException e) {

            Messagebox.show(
                    "Invalid Batch ID.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        loadLoggedInUser();

        Long lockOwner = com.cts.inward.dao.BatchDaoImpl.of().getBatchLockOwner(batchId);
        if (lockOwner == null || loggedInUserId == null || !lockOwner.equals(loggedInUserId)) {
            String msg = (lockOwner == null)
                    ? "This batch is not locked. Please lock the batch from the dashboard first."
                    : "This batch is locked by another user.";
            Messagebox.show(
                    msg,
                    "Access Denied",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION,
                    e -> Executions.sendRedirect("/zul/inward-maker/dashboard.zul"));
            return;
        }

        /*
         * If an index is supplied, use it.
         * Otherwise open the first pending MICR repair cheque.
         */
        if (chequeIndexParam != null
                && !chequeIndexParam.trim().isEmpty()) {

            try {

                chequeIndex =
                        Integer.parseInt(
                                chequeIndexParam.trim());

            } catch (NumberFormatException e) {

                chequeIndex =
                        micrRepairService
                                .getNextRepairIndex(
                                        batchId);
            }

        } else {

            chequeIndex =
                    micrRepairService
                            .getNextRepairIndex(
                                    batchId);
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
    // Init Return Window
    // =========================================================

    private void initReturnWindow() {

        if (returnWindow == null) {
            return;
        }

        returnReason =
                (Combobox)
                        returnWindow
                                .getFellow(
                                        "returnReason");

        returnRemarks =
                (Textbox)
                        returnWindow
                                .getFellow(
                                        "returnRemarks");

        cancelReturnButton =
                (Button)
                        returnWindow
                                .getFellow(
                                        "cancelReturnButton");

        confirmReturnButton =
                (Button)
                        returnWindow
                                .getFellow(
                                        "confirmReturnButton");
    }

    // =========================================================
    // Load Batch
    // =========================================================

    private void loadBatch() {

        comparisons =
                micrRepairService
                        .compareBatch(batchId);

        if (comparisons == null
                || comparisons.isEmpty()) {

            goToDataEntry();

            return;
        }

        /*
         * Build the CURRENT pending MICR queue.
         *
         * Only isNeedsMicrRepair() determines whether
         * the cheque is currently pending.
         */
        buildRepairIndexes();

        if (totalMicrErrors == 0) {

            goToDataEntry();

            return;
        }

        if (!isValidRepairIndex(chequeIndex)) {

            chequeIndex =
                    findNextRepairIndexNoWrap(0);

            if (chequeIndex < 0) {

                goToDataEntry();

                return;
            }
        }

        updateTopBar();

        loadCheque();
    }

    // =========================================================
    // Build Repair Indexes
    // =========================================================

    private void buildRepairIndexes() {

        if (comparisons == null) {
            originalRepairIndexes =
                    new ArrayList<Integer>();

            totalMicrErrors = 0;

            return;
        }

        /*
         * Build the original MICR repair queue only once.
         *
         * This list must NOT shrink after a cheque is repaired.
         *
         * Example:
         *
         * Original:
         * 1, 2, 3
         *
         * After repairing cheque 1:
         * Current pending:
         * 2, 3
         *
         * But originalRepairIndexes must remain:
         * 1, 2, 3
         *
         * This is required for the counter:
         * Cheque 2 of 3
         * Cheque 3 of 3
         */

        if (originalRepairIndexes == null
                || originalRepairIndexes.isEmpty()) {

            originalRepairIndexes =
                    new ArrayList<Integer>();

            for (int i = 0;
                    i < comparisons.size();
                    i++) {

                MicrComparisonDto c =
                        comparisons.get(i);

                if (c == null) {
                    continue;
                }

                // Keep pending, repaired, and return_by_maker cheques in the error list
                if (c.isNeedsMicrRepair() || c.isMicrRepaired() || c.isReturnByMaker()) {
                    originalRepairIndexes.add(i);
                }
            }


            totalMicrErrors =
                    originalRepairIndexes.size();
        }
    }

    // =========================================================
    // Top Bar
    // =========================================================

    private void updateTopBar() {

        if (batchLabel != null) {

            batchLabel.setValue(
                    "Batch No :  " + batchId);
        }

        if (comparisons == null) {

            if (totalCountLabel != null) {

                totalCountLabel.setValue(
                        "Total: 0");
            }

            if (completedCountLabel != null) {

                completedCountLabel.setValue(
                        "Completed: 0");
            }

            if (pendingCountLabel != null) {

                pendingCountLabel.setValue(
                        "Pending: 0");
            }

            if (chequeCounter != null) {

                chequeCounter.setValue(
                        "Cheque 0 of 0");
            }

            return;
        }

        int pending = 0;

        for (MicrComparisonDto c :
                comparisons) {

            if (c != null
                    && c.isNeedsMicrRepair()) {

                pending++;
            }
        }

        int completed =
                totalMicrErrors - pending;

        if (completed < 0) {
            completed = 0;
        }

        if (totalCountLabel != null) {

            totalCountLabel.setValue(
                    "Total: "
                            + totalMicrErrors);
        }

        if (completedCountLabel != null) {

            completedCountLabel.setValue(
                    "Completed: "
                            + completed);
        }

        if (pendingCountLabel != null) {

            pendingCountLabel.setValue(
                    "Pending: "
                            + pending);
        }

        updateChequeCounter();
    }

    // =========================================================
    // Cheque Counter
    // =========================================================

    private void updateChequeCounter() {

        if (chequeCounter == null) {
            return;
        }

        if (comparisons == null
                || originalRepairIndexes == null
                || originalRepairIndexes.isEmpty()
                || totalMicrErrors <= 0) {

            chequeCounter.setValue(
                    "Cheque 0 of 0");

            return;
        }

        /*
         * The counter position is based on the ORIGINAL
         * MICR repair queue.
         *
         * It must not be based on the current pending
         * queue because repaired cheques disappear from
         * the pending queue.
         */

        int currentPosition = 0;

        for (int i = 0;
                i < originalRepairIndexes.size();
                i++) {

            int comparisonIndex =
                    originalRepairIndexes.get(i);

            if (comparisonIndex == chequeIndex) {

                currentPosition =
                        i + 1;

                break;
            }
        }

        if (currentPosition > 0) {

            chequeCounter.setValue(
                    "Cheque "
                            + currentPosition
                            + " of "
                            + totalMicrErrors);

        } else {

            chequeCounter.setValue(
                    "Cheque 0 of "
                            + totalMicrErrors);
        }
    }

    // =========================================================
    // Load Cheque
    // =========================================================

    private void loadCheque() {

        if (comparisons == null
                || chequeIndex < 0
                || chequeIndex >= comparisons.size()) {

            return;
        }

        MicrComparisonDto c =
                comparisons.get(chequeIndex);

        if (c == null) {
            return;
        }

        populateMicrFields(c);

        if (c.isReturnByMaker()) {

            setCurrentStatus(
                    STATUS_RETURN_BY_MAKER
            );

        } else if (c.isMicrRepaired()) {

            setCurrentStatus(
                    STATUS_MICR_REPAIRED
            );

        } else {

            setCurrentStatus(
                    STATUS_MICR_REPAIR
            );
        }

        loadImages(
                c.getChequeNumber()
        );

        updateChequeCounter();

        if (chequeNumber != null) {

            chequeNumber.setValue(
                    safe(c.getChequeNumber())
            );
        }

        updateReturnBanner(c.getChequeNumber());

        updateNavigationButtons();
    }

    // =========================================================
    // Update Return Banner
    // =========================================================

    private void updateReturnBanner(String chqNo) {
        if (returnReasonBanner == null) {
            return;
        }

        if (chqNo == null || chqNo.trim().isEmpty() || chequeService == null) {
            returnReasonBanner.setVisible(false);
            return;
        }

        java.util.Map<String, String> returnInfo = chequeService.getChequeReturnInfo(chqNo);
        if (returnInfo != null && "RETURN_TO_MAKER".equalsIgnoreCase(returnInfo.get("status"))) {
            returnReasonBanner.setVisible(true);

            String desc = returnInfo.get("returnReasonDescription");
            if (desc == null || desc.trim().isEmpty()) {
                desc = returnInfo.get("returnReasonCode");
            }
            if (lblReturnReason != null) {
                lblReturnReason.setValue(desc != null ? desc : "Returned to Maker by Checker");
            }

            String remarks = returnInfo.get("remarks");
            if (lblReturnRemarks != null) {
                lblReturnRemarks.setValue(remarks != null && !remarks.trim().isEmpty() ? remarks : "No remarks provided");
            }
        } else {
            returnReasonBanner.setVisible(false);
        }
    }

    // =========================================================
    // Current Status
    // =========================================================

    private void setCurrentStatus(
            String status) {

        if (currentStatusLabel == null) {
            return;
        }

        String normalized =
                status == null
                        ? ""
                        : status.trim()
                                .toUpperCase();

        currentStatusLabel.setValue(
                normalized);

        if (STATUS_MICR_REPAIR.equals(
                normalized)) {

            currentStatusLabel.setSclass(
                    "micr-status-value "
                            + "micr-status-repair");

        } else if (STATUS_MICR_REPAIRED.equals(
                normalized)) {

            currentStatusLabel.setSclass(
                    "micr-status-value "
                            + "micr-status-repaired");

        } else if (STATUS_DATA_ENTRY.equals(
                normalized)) {

            currentStatusLabel.setSclass(
                    "micr-status-value "
                            + "micr-status-data-entry");

        } else {

            currentStatusLabel.setSclass(
                    "micr-status-value "
                            + "micr-status-error");
        }
    }

    // =========================================================
    // Populate MICR Fields
    // =========================================================

    private void populateMicrFields(
            MicrComparisonDto c) {

        boolean isRepaired = c.isMicrRepaired() && c.getRepairedMicrCode() != null;

        // Display saved values if repaired; otherwise display original NPCI values
        String cityValue = isRepaired ? c.getRepairedCityCode() : safe(c.getNpciCityCode());
        String bankValue = isRepaired ? c.getRepairedBankCode() : safe(c.getNpciBankCode());
        String branchValue = isRepaired ? c.getRepairedBranchCode() : safe(c.getNpciBranchCode());

        if (c.isReturnByMaker()) {
            if (ocrCityCode != null) {
                ocrCityCode.setValue(cityValue);
                ocrCityCode.setSclass("micr-editable-field");
                ocrCityCode.setReadonly(true);
            }
            if (ocrBankCode != null) {
                ocrBankCode.setValue(bankValue);
                ocrBankCode.setSclass("micr-editable-field");
                ocrBankCode.setReadonly(true);
            }
            if (ocrBranchCode != null) {
                ocrBranchCode.setValue(branchValue);
                ocrBranchCode.setSclass("micr-editable-field");
                ocrBranchCode.setReadonly(true);
            }
            return;
        }

        boolean anyFieldMismatch = c.isCityCodeMismatch() || c.isBankCodeMismatch() || c.isBranchCodeMismatch();

        // -----------------------------------------------------
        // City Code
        // -----------------------------------------------------
        if (ocrCityCode != null) {
            ocrCityCode.setValue(cityValue);
            boolean isCityMismatch = c.isCityCodeMismatch();
            boolean isCityEditable = isCityMismatch || (!anyFieldMismatch && !c.isNpciMicrFoundInMaster());
            if (isCityMismatch && !isRepaired) {
                ocrCityCode.setSclass("micr-editable-field micr-field-error");
            } else {
                ocrCityCode.setSclass("micr-editable-field");
            }
            ocrCityCode.setReadonly(!isCityEditable);
        }

        // -----------------------------------------------------
        // Bank Code
        // -----------------------------------------------------
        if (ocrBankCode != null) {
            ocrBankCode.setValue(bankValue);
            boolean isBankMismatch = c.isBankCodeMismatch();
            boolean isBankEditable = isBankMismatch || (!anyFieldMismatch && !c.isNpciMicrFoundInMaster());
            if (isBankMismatch && !isRepaired) {
                ocrBankCode.setSclass("micr-editable-field micr-field-error");
            } else {
                ocrBankCode.setSclass("micr-editable-field");
            }
            ocrBankCode.setReadonly(!isBankEditable);
        }

        // -----------------------------------------------------
        // Branch Code
        // -----------------------------------------------------
        if (ocrBranchCode != null) {
            ocrBranchCode.setValue(branchValue);
            boolean isBranchMismatch = c.isBranchCodeMismatch();
            boolean isBranchEditable = isBranchMismatch || (!anyFieldMismatch && !c.isNpciMicrFoundInMaster());
            if (isBranchMismatch && !isRepaired) {
                ocrBranchCode.setSclass("micr-editable-field micr-field-error");
            } else {
                ocrBranchCode.setSclass("micr-editable-field");
            }
            ocrBranchCode.setReadonly(!isBranchEditable);
        }
    }


    // =========================================================
    // Images
    // =========================================================

    private void loadImages(
            String chequeNum) {

        frontImagePath =
                micrRepairService
                        .getFrontImagePath(
                                chequeNum);

        backImagePath =
                micrRepairService
                        .getBackImagePath(
                                chequeNum);

        showingFront = true;
        currentScale = 1.0;
        currentRotation = 0;

        showCurrentImage();

        if (toggleImageButton != null) {

            toggleImageButton.setLabel(
                    "View Back");
        }
    }

    private java.io.File resolveImageFile(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        String normalized = path.replace("\\", "/").trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        // 1. Direct file / absolute path
        java.io.File directFile = new java.io.File(path);
        if (directFile.isAbsolute() && directFile.isFile()) {
            return directFile;
        }

        // 2. Deployed webApp realPath
        try {
            if (org.zkoss.zk.ui.Executions.getCurrent() != null
                    && org.zkoss.zk.ui.Executions.getCurrent().getDesktop() != null) {
                org.zkoss.zk.ui.WebApp webApp =
                        org.zkoss.zk.ui.Executions.getCurrent().getDesktop().getWebApp();
                String realPath = webApp.getRealPath("/" + normalized);
                if (realPath != null) {
                    java.io.File realFile = new java.io.File(realPath);
                    if (realFile.isFile()) {
                        return realFile;
                    }
                }
            }
        } catch (Exception ignored) {}

        // 3. Local workspace development path: src/main/webapp/ + subPath
        String subPath = normalized.startsWith("src/main/webapp/")
                ? normalized.substring("src/main/webapp/".length())
                : normalized;
        java.io.File devFile = new java.io.File("src/main/webapp", subPath);
        if (devFile.isFile()) {
            return devFile;
        }

        // 4. Relative path as-is
        if (directFile.isFile()) {
            return directFile;
        }

        return null;
    }

    private void showCurrentImage() {

        if (chequeImage == null) {
            return;
        }

        String path =
                showingFront
                        ? frontImagePath
                        : backImagePath;

        if (path != null
                && !path.trim().isEmpty()) {

            try {
                java.io.File file = resolveImageFile(path);
                if (file != null) {
                    chequeImage.setContent(new org.zkoss.image.AImage(file));
                } else {
                    String clean = path.replace("\\", "/").trim();
                    if (clean.startsWith("/")) clean = clean.substring(1);
                    if (clean.startsWith("src/main/webapp/")) {
                        clean = clean.substring("src/main/webapp/".length());
                    }
                    String webSrc = "/" + clean;
                    chequeImage.setContent((org.zkoss.image.AImage) null);
                    chequeImage.setSrc(webSrc);
                }
            } catch (Exception e) {
                String clean = path.replace("\\", "/").trim();
                if (clean.startsWith("/")) clean = clean.substring(1);
                if (clean.startsWith("src/main/webapp/")) {
                    clean = clean.substring("src/main/webapp/".length());
                }
                String webSrc = "/" + clean;
                chequeImage.setContent((org.zkoss.image.AImage) null);
                chequeImage.setSrc(webSrc);
            }

        } else {

            chequeImage.setContent(
                    (org.zkoss.image.AImage) null);
            chequeImage.setSrc("");
        }

        applyImageStyle();
    }

    private void applyImageStyle() {

        if (chequeImage != null) {
            chequeImage.setStyle(String.format(
                    java.util.Locale.US,
                    "object-fit:contain; max-width:100%%; max-height:100%%; display:block; transform: scale(%.2f) rotate(%ddeg); transform-origin: center; transition: transform 0.2s;",
                    currentScale,
                    currentRotation));
        }
    }

    // =========================================================
    // Save & Next
    // =========================================================

    private void saveAndNext() {

        if (comparisons == null
                || chequeIndex < 0
                || chequeIndex >= comparisons.size()) {

            Messagebox.show(
                    "No cheque selected.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        MicrComparisonDto c =
                comparisons.get(chequeIndex);

        if (c == null) {
            return;
        }

        /*
         * A cheque already repaired should not be
         * saved again.
         */
        
        
        /*
         * Allow re-saving if user navigated back to an already repaired cheque
         */

        String city =
                ocrCityCode == null
                        || ocrCityCode.getValue() == null
                        ? ""
                        : ocrCityCode
                                .getValue()
                                .trim();

        String bank =
                ocrBankCode == null
                        || ocrBankCode.getValue() == null
                        ? ""
                        : ocrBankCode
                                .getValue()
                                .trim();

        String branch =
                ocrBranchCode == null
                        || ocrBranchCode.getValue() == null
                        ? ""
                        : ocrBranchCode
                                .getValue()
                                .trim();

        // -----------------------------------------------------
        // Server-side validation
        // -----------------------------------------------------

        if (!city.matches("\\d{3}")) {

            Messagebox.show(
                    "City Code must be exactly 3 digits.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }

        if (!bank.matches("\\d{3}")) {

            Messagebox.show(
                    "Bank Code must be exactly 3 digits.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }

        if (!branch.matches("\\d{3}")) {

            Messagebox.show(
                    "Branch Code must be exactly 3 digits.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }

        correctedMicr =
                city + bank + branch;

        User user =
                (User)
                        Executions
                                .getCurrent()
                                .getSession()
                                .getAttribute(
                                        "loggedInUser");

        if (user == null) {

            Messagebox.show(
                    "Session expired. Please login again.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        try {

            boolean saved =
                    micrRepairService.saveMicrRepair(
                            c.getChequeNumber(),
                            safe(c.getNpciMicrCode()),
                            correctedMicr,
                            null,
                            user.getUserId());

            if (!saved) {

                Messagebox.show(
                        "Unable to save MICR repair.",
                        "MICR Repair",
                        Messagebox.OK,
                        Messagebox.ERROR);

                return;
            }

            /*
             * Important:
             *
             * Individual cheque is now MICR_REPAIRED.
             *
             * It must NOT become DATA_ENTRY here.
             *
             * DATA_ENTRY happens only when the entire
             * MICR stage for the batch is complete.
             */
            setCurrentStatus(
                    STATUS_MICR_REPAIRED);

            // Non-blocking notification at top-right for 2s (2000 ms)
            Clients.showNotification(
                    "MICR repair successful",
                    Clients.NOTIFICATION_TYPE_INFO,
                    null,
                    "top_right",
                    2000
            );

            // Advance immediately to next cheque without requiring OK click
            moveAfterSave();


        } catch (IllegalArgumentException e) {

            Clients.showNotification(
                    "MICR not found",
                    Clients.NOTIFICATION_TYPE_WARNING,
                    null,
                    "top_right",
                    2000
            );

        } catch (Exception e) {

            Messagebox.show(
                    "Unable to save: "
                            + e.getMessage(),
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    // =========================================================
    // Move After Save
    // =========================================================

    private void moveAfterSave() {

        comparisons =
                micrRepairService
                        .compareBatch(batchId);

        if (originalRepairIndexes == null || originalRepairIndexes.isEmpty()) {
            navigateAfterMicrCompletion();
            return;
        }

        int currentPos = originalRepairIndexes.indexOf(chequeIndex);

        if (currentPos >= 0 && currentPos < originalRepairIndexes.size() - 1) {
            // Advance to the next cheque in the error list
            chequeIndex = originalRepairIndexes.get(currentPos + 1);
            updateTopBar();
            loadCheque();
        } else {
            // End of error list reached
            navigateAfterMicrCompletion();
        }
    }


    // =========================================================
    // Previous Cheque
    // =========================================================

    private void goToPreviousCheque() {

        if (originalRepairIndexes == null || originalRepairIndexes.isEmpty()) {
            return;
        }

        int currentPos = originalRepairIndexes.indexOf(chequeIndex);

        if (currentPos > 0) {
            chequeIndex = originalRepairIndexes.get(currentPos - 1);
            updateTopBar();
            loadCheque();
        }
    }

    private void goToNextCheque() {

        if (originalRepairIndexes == null || originalRepairIndexes.isEmpty()) {
            return;
        }

        int currentPos = originalRepairIndexes.indexOf(chequeIndex);

        if (currentPos >= 0 && currentPos < originalRepairIndexes.size() - 1) {
            chequeIndex = originalRepairIndexes.get(currentPos + 1);
            updateTopBar();
            loadCheque();
        }
    }


    // =========================================================
    // Update Prev / Next Button State
    // =========================================================

    private void updateNavigationButtons() {

        if (previousButton == null
                || nextButton == null) {

            return;
        }

        if (originalRepairIndexes == null || originalRepairIndexes.isEmpty()) {
            previousButton.setDisabled(true);
            nextButton.setDisabled(true);
            return;
        }

        int currentPos = originalRepairIndexes.indexOf(chequeIndex);

        // Prev is disabled only on the very first cheque of the error list
        previousButton.setDisabled(currentPos <= 0);

        // Next is disabled only on the very last cheque of the error list
        nextButton.setDisabled(currentPos < 0 || currentPos >= originalRepairIndexes.size() - 1);

        boolean isReturned = false;
        if (comparisons != null && chequeIndex >= 0 && chequeIndex < comparisons.size()) {
            MicrComparisonDto c = comparisons.get(chequeIndex);
            if (c != null && c.isReturnByMaker()) {
                isReturned = true;
            }
        }

        if (saveNextButton != null) {
            saveNextButton.setDisabled(isReturned);
        }

        if (returnButton != null) {
            returnButton.setDisabled(isReturned);
        }
    }


    // =========================================================
    // Return
    // =========================================================

    private void openReturnWindow() {

        if (returnWindow == null) {
            initReturnWindow();
        }

        if (returnWindow == null
                || returnReason == null) {

            Messagebox.show(
                    "Return dialog unavailable.",
                    "Return",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        returnReason.getItems().clear();

        returnReason.setSelectedItem(null);

        if (returnRemarks != null) {

            returnRemarks.setValue("");
        }

        List<ReturnReasonDto> reasons =
                micrRepairService
                        .getMakerReturnReasons();

        if (reasons != null) {

            for (ReturnReasonDto r :
                    reasons) {

                if (r == null) {
                    continue;
                }

                Comboitem item =
                        new Comboitem();

                item.setLabel(
                        r.getDescription());

                item.setValue(
                        r.getReturnReasonCode());

                returnReason.appendChild(item);
            }
        }

        returnWindow.doModal();
    }

    private void cancelReturn() {

        if (returnReason != null) {

            returnReason.setSelectedItem(null);
        }

        if (returnRemarks != null) {

            returnRemarks.setValue("");
        }

        if (returnWindow != null) {

            returnWindow.setVisible(false);
        }
    }

    private void confirmReturn() {

        if (returnReason == null
                || returnReason.getSelectedItem() == null) {

            Messagebox.show(
                    "Please select a return reason.",
                    "Return",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }

        String code =
                (String)
                        returnReason
                                .getSelectedItem()
                                .getValue();

        if (code == null
                || code.trim().isEmpty()) {

            Messagebox.show(
                    "Invalid return reason.",
                    "Return",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        if (comparisons == null
                || chequeIndex < 0
                || chequeIndex >= comparisons.size()) {

            Messagebox.show(
                    "No cheque selected.",
                    "Return",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        MicrComparisonDto c =
                comparisons.get(chequeIndex);

        if (c == null) {
            return;
        }

        User user =
                (User)
                        Executions
                                .getCurrent()
                                .getSession()
                                .getAttribute(
                                        "loggedInUser");

        if (user == null) {

            Messagebox.show(
                    "Session expired.",
                    "Return",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        String remarks =
                returnRemarks != null
                        && returnRemarks.getValue() != null
                        ? returnRemarks.getValue().trim()
                        : "";

        try {

            boolean saved =
                    micrRepairService.saveMakerReturn(
                            c.getChequeNumber(),
                            code.trim(),
                            remarks,
                            user.getUserId());

            if (!saved) {

                Messagebox.show(
                        "Unable to save return.",
                        "Return",
                        Messagebox.OK,
                        Messagebox.ERROR);

                return;
            }

            if (returnWindow != null) {

                returnWindow.setVisible(false);
            }

            Clients.showNotification(
                    "Cheque "
                            + c.getChequeNumber()
                            + " marked as RETURN_BY_MAKER.",
                    Clients.NOTIFICATION_TYPE_INFO,
                    null,
                    "top_right",
                    2000);

            moveAfterReturn();

        } catch (Exception e) {

            Messagebox.show(
                    "Unable to return: "
                            + e.getMessage(),
                    "Return",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    // =========================================================
    // Move After Return
    // =========================================================

    private void moveAfterReturn() {

        int currentPos = originalRepairIndexes != null ? originalRepairIndexes.indexOf(chequeIndex) : -1;

        comparisons =
                micrRepairService
                        .compareBatch(batchId);

        if (originalRepairIndexes != null) {
            originalRepairIndexes.clear();
        }
        buildRepairIndexes();

        if (originalRepairIndexes != null && !originalRepairIndexes.isEmpty()) {
            if (currentPos >= 0 && currentPos < originalRepairIndexes.size() - 1) {
                chequeIndex = originalRepairIndexes.get(currentPos + 1);
            } else if (currentPos >= originalRepairIndexes.size() - 1) {
                chequeIndex = originalRepairIndexes.get(originalRepairIndexes.size() - 1);
            }
        }

        updateTopBar();

        loadCheque();

        if (!micrRepairService.needsMicrRepair(batchId)) {
            if (micrRepairService.hasChequesNeedingDataEntry(batchId)) {
                Clients.showNotification(
                        "All cheques in this batch are completed or returned. Moving to Data Entry in 2 seconds...",
                        Clients.NOTIFICATION_TYPE_INFO,
                        null,
                        "top_right",
                        2000);
                org.zkoss.zk.ui.util.Clients.evalJavaScript(
                        "setTimeout(function() { window.location.href = '"
                                + Executions.encodeURL("/zul/inward-maker/data-entryform.zul?batchId=" + batchId)
                                + "'; }, 2000);"
                );
            } else {
                Clients.showNotification(
                        "MICR repair completed. Batch is ready to be sent to Checker.",
                        Clients.NOTIFICATION_TYPE_INFO,
                        null,
                        "top_right",
                        2000);
                org.zkoss.zk.ui.util.Clients.evalJavaScript(
                        "setTimeout(function() { window.location.href = '"
                                + Executions.encodeURL("/zul/inward-maker/send-to-checker.zul")
                                + "'; }, 2000);"
                );
            }
        }
    }

    // =========================================================
    // Navigation
    // =========================================================

    private void navigateAfterMicrCompletion() {
        if (micrRepairService.hasChequesNeedingDataEntry(batchId)) {
            goToDataEntry();
        } else {
            Clients.showNotification(
                    "MICR repair completed. Batch is ready to be sent to Checker.",
                    Clients.NOTIFICATION_TYPE_INFO,
                    null,
                    "top_right",
                    2000);
            org.zkoss.zk.ui.util.Clients.evalJavaScript(
                    "setTimeout(function() { window.location.href = '"
                            + Executions.encodeURL("/zul/inward-maker/send-to-checker.zul")
                            + "'; }, 2000);"
            );
        }
    }

    private void goToDataEntry() {
        Executions.sendRedirect(
                "/zul/inward-maker/data-entryform.zul?batchId="
                        + batchId);
    }

    private void goBack() {

        if ("dashboard"
                .equalsIgnoreCase(source)) {

            Executions.sendRedirect(
                    "/zul/inward-maker/dashboard.zul");

        } else {

            Executions.sendRedirect(
                    "/zul/inward-maker/micr-repair-list.zul");
        }
    }

    // =========================================================
    // Register Events
    // =========================================================

    private void registerEvents() {

        // -----------------------------------------------------
        // Toggle Image
        // -----------------------------------------------------

        if (toggleImageButton != null) {

            toggleImageButton.addEventListener(
                    Events.ON_CLICK,
                    event -> {

                        showingFront =
                                !showingFront;

                        toggleImageButton.setLabel(
                                showingFront
                                        ? "View Back"
                                        : "View Front");

                        showCurrentImage();
                    });
        }

        // -----------------------------------------------------
        // Zoom In
        // -----------------------------------------------------

        if (zoomInButton != null) {

            zoomInButton.addEventListener(
                    Events.ON_CLICK,
                    event -> {
                        if (currentScale < 3.0) {
                            currentScale += 0.2;
                            applyImageStyle();
                        }
                    });
        }

        // -----------------------------------------------------
        // Zoom Out
        // -----------------------------------------------------

        if (zoomOutButton != null) {

            zoomOutButton.addEventListener(
                    Events.ON_CLICK,
                    event -> {
                        if (currentScale > 0.4) {
                            currentScale -= 0.2;
                            applyImageStyle();
                        }
                    });
        }

        // -----------------------------------------------------
        // Rotate
        // -----------------------------------------------------

        if (rotateButton != null) {

            rotateButton.addEventListener(
                    Events.ON_CLICK,
                    event -> {
                        currentRotation =
                                (currentRotation + 90) % 360;
                        applyImageStyle();
                    });
        }

        // -----------------------------------------------------
        // Reset View
        // -----------------------------------------------------

        if (resetViewButton != null) {

            resetViewButton.addEventListener(
                    Events.ON_CLICK,
                    event -> {
                        currentScale = 1.0;
                        currentRotation = 0;
                        applyImageStyle();
                    });
        }

        // -----------------------------------------------------
        // Save & Next
        // -----------------------------------------------------

        if (saveNextButton != null) {

            saveNextButton.addEventListener(
                    Events.ON_CLICK,
                    event -> saveAndNext());
        }

        // -----------------------------------------------------
        // Return
        // -----------------------------------------------------

        if (returnButton != null) {

            returnButton.addEventListener(
                    Events.ON_CLICK,
                    event -> openReturnWindow());
        }

        // -----------------------------------------------------
        // Back
        // -----------------------------------------------------

        if (backToList != null) {

            backToList.addEventListener(
                    Events.ON_CLICK,
                    event -> goBack());
        }

        // -----------------------------------------------------
        // Previous
        // -----------------------------------------------------

        if (previousButton != null) {

            previousButton.addEventListener(
                    Events.ON_CLICK,
                    event -> goToPreviousCheque());
        }

        // -----------------------------------------------------
        // Next
        // -----------------------------------------------------

        if (nextButton != null) {

            nextButton.addEventListener(
                    Events.ON_CLICK,
                    event -> goToNextCheque());
        }

        // -----------------------------------------------------
        // Return Window
        // -----------------------------------------------------

        if (cancelReturnButton != null) {

            cancelReturnButton.addEventListener(
                    Events.ON_CLICK,
                    event -> cancelReturn());
        }

        if (confirmReturnButton != null) {

            confirmReturnButton.addEventListener(
                    Events.ON_CLICK,
                    event -> confirmReturn());
        }

        // -----------------------------------------------------
        // MICR change events
        // -----------------------------------------------------

        if (ocrCityCode != null) {

            ocrCityCode.addEventListener(
                    Events.ON_CHANGE,
                    event ->
                            correctedMicr =
                                    buildMicr());
        }

        if (ocrBankCode != null) {

            ocrBankCode.addEventListener(
                    Events.ON_CHANGE,
                    event ->
                            correctedMicr =
                                    buildMicr());
        }

        if (ocrBranchCode != null) {

            ocrBranchCode.addEventListener(
                    Events.ON_CHANGE,
                    event ->
                            correctedMicr =
                                    buildMicr());
        }

        updateNavigationButtons();
    }

    // =========================================================
    // Build MICR
    // =========================================================

    private String buildMicr() {

        String c =
                ocrCityCode == null
                        || ocrCityCode.getValue() == null
                        ? ""
                        : ocrCityCode
                                .getValue()
                                .trim();

        String b =
                ocrBankCode == null
                        || ocrBankCode.getValue() == null
                        ? ""
                        : ocrBankCode
                                .getValue()
                                .trim();

        String r =
                ocrBranchCode == null
                        || ocrBranchCode.getValue() == null
                        ? ""
                        : ocrBranchCode
                                .getValue()
                                .trim();

        if (c.matches("\\d{3}")
                && b.matches("\\d{3}")
                && r.matches("\\d{3}")) {

            return c + b + r;
        }

        return "";
    }

    // =========================================================
    // Check Valid Repair Index
    // =========================================================

    private boolean isValidRepairIndex(
            int index) {

        return originalRepairIndexes != null && originalRepairIndexes.contains(index);
    }


    // =========================================================
    // Find Next MICR Repair Cheque
    // No wrapping
    // =========================================================

    private int findNextRepairIndexNoWrap(
            int start) {

        if (comparisons == null) {
            return -1;
        }

        if (start < 0) {
            start = 0;
        }

        for (int i = start;
                i < comparisons.size();
                i++) {

            MicrComparisonDto c =
                    comparisons.get(i);

            if (c != null
                    && c.isNeedsMicrRepair()) {

                return i;
            }
        }

        return -1;
    }

    // =========================================================
    // Find Previous MICR Repair Cheque
    // No wrapping
    // =========================================================

    private int findPreviousRepairIndex(
            int start) {

        if (comparisons == null) {
            return -1;
        }

        if (start >= comparisons.size()) {

            start =
                    comparisons.size() - 1;
        }

        for (int i = start;
                i >= 0;
                i--) {

            MicrComparisonDto c =
                    comparisons.get(i);

            if (c != null
                    && c.isNeedsMicrRepair()) {

                return i;
            }
        }

        return -1;
    }

    // =========================================================
    // Safe
    // =========================================================

    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }
}