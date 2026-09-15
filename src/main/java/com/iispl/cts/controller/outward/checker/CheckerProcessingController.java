package com.iispl.cts.controller.outward.checker;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

import com.cts.admin.model.User;
import com.iispl.cts.model.outward.ChequeProcessing;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.model.outward.ReturnReason;
import com.iispl.cts.service.outward.checker.CheckerBatchService;
import com.iispl.cts.service.outward.checker.CheckerProcessingService;

public class CheckerProcessingController extends SelectorComposer<Vlayout> {

    private static final long serialVersionUID = 1L;

    // ============================================================
    // WIRED COMPONENTS
    // ============================================================

    @Wire
    private Label verificationBatchId;

    @Wire
    private Label chequeSequence;

    @Wire
    private Button backButton;

    // ============================================================
    // CHEQUE IMAGE
    // ============================================================

    @Wire
    private Image chequeImage;

    @Wire
    private Button frontButton;

    @Wire
    private Button backSideButton;

    @Wire
    private Button rotateButton;

    @Wire
    private Button zoomOutButton;

    @Wire
    private Button zoomInButton;

    @Wire
    private Label zoomLevelLabel;

    // ============================================================
    // CHEQUE DETAILS
    // ============================================================

    @Wire
    private Label chequeNumberLabel;

    @Wire
    private Label accountNumberLabel;

    @Wire
    private Label drawerNameLabel;

    @Wire
    private Label payeeNameLabel;

    @Wire
    private Label amountLabel;

    @Wire
    private Label amountInWordsLabel;

    @Wire
    private Label chequeDateLabel;

    @Wire
    private Label micrLabel;

    // ============================================================
    // CBS VALIDATION
    // ============================================================

    @Wire
    private Label accountVerificationIcon;

    @Wire
    private Label accountVerificationMessage;

    @Wire
    private Label accountVerificationReason;

    // ============================================================
    // MAKER REJECTION
    // ============================================================

    @Wire
    private Div makerRejectionBlock;

    @Wire
    private Label makerRejectionReason;

    // ============================================================
    // CHECKER ACTIONS
    // ============================================================

    @Wire
    private Button acceptButton;

    @Wire
    private Button rejectButton;

    @Wire
    private Button sendBackButton;

    @Wire
    private Hlayout reasonRow;

    @Wire
    private Combobox reasonCombobox;

    @Wire
    private Textbox checkerRemarksTextbox;

    @Wire
    private Button saveNextButton;

    // ============================================================
    // STATE
    // ============================================================

    private String cbsResult;

    private String selectedAction;

    private int currentChequeIndex = 0;

    /*
     * Image display state.
     *
     * 1.0 = 100%
     * 1.25 = 125%
     * 1.50 = 150%
     *
     * Rotation:
     * 0 -> 90 -> 180 -> 270 -> 0
     */
    private double imageScale = 1.0;

    private int imageRotation = 0;

    private CheckerBatchService batchService;

    private CheckerProcessingService processingService;

    private String batchNumber;

    /*
     * Re-Verify mode is enabled only when the Checker
     * opens this screen with mode=RE_VERIFY.
     */
    private boolean reVerifyMode = false;

    private long checkerUserId;

    private OutwardBatch currentBatch;

    private List<OutwardCheque> cheques;

    // ============================================================
    // INIT
    // ============================================================

    @Override
    public void doAfterCompose(Vlayout comp) throws Exception {

        super.doAfterCompose(comp);

        batchService = new CheckerBatchService();

        processingService = new CheckerProcessingService();

        // ========================================================
        // CURRENT USER
        // ========================================================

        User currentUser =
                (User) Sessions.getCurrent()
                        .getAttribute("loggedInUser");

        if (currentUser == null) {

            Executions.sendRedirect(
                    Executions.getCurrent().getContextPath()
                            + "/login.zul");

            return;
        }

        checkerUserId = currentUser.getUserId();

        // ========================================================
        // BATCH NUMBER
        // ========================================================

        batchNumber =
                Executions.getCurrent()
                        .getParameter("batchNumber");

        String mode = Executions.getCurrent().getParameter("mode");

        System.out.println("========== CHECKER PROCESSING ==========");
        System.out.println("Batch Number : " + batchNumber);
        System.out.println("Mode Parameter : [" + mode + "]");

        reVerifyMode = "RE_VERIFY".equalsIgnoreCase(
                mode != null ? mode.trim() : ""
        );

        System.out.println("Re-Verify Mode : " + reVerifyMode);

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            Messagebox.show(
                    "Batch number is missing.",
                    "Processing",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }

        batchNumber = batchNumber.trim();

        // ========================================================
        // LOAD DATA
        // ========================================================

        loadBatch();

        if (currentBatch == null) {

            Messagebox.show(
                    "Batch could not be found.",
                    "Processing",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        /*
         * The same Checker must own the batch.
         *
         * No new assignment is created here.
         * This is especially important for RE_VERIFY mode.
         */
        if (!batchService.isAssignedToChecker(
                batchNumber,
                checkerUserId)) {

            Messagebox.show(
                    "This batch is not assigned to the current Checker.",
                    "Access Denied",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        loadFirstCheque();

        // ========================================================
        // BUTTON EVENTS
        // ========================================================

        backButton.addEventListener(
                Events.ON_CLICK,
                event -> goBackToQueue());

        // ========================================================
        // FRONT / BACK IMAGE BUTTONS
        // ========================================================

        frontButton.addEventListener(
                Events.ON_CLICK,
                event -> showFrontImage());

        backSideButton.addEventListener(
                Events.ON_CLICK,
                event -> showBackImage());

        // ========================================================
        // IMAGE CONTROLS
        // ========================================================

        rotateButton.addEventListener(
                Events.ON_CLICK,
                event -> rotateImage());

        zoomOutButton.addEventListener(
                Events.ON_CLICK,
                event -> zoomOut());

        zoomInButton.addEventListener(
                Events.ON_CLICK,
                event -> zoomIn());

        // ========================================================
        // CHECKER ACTION BUTTONS
        // ========================================================

        acceptButton.addEventListener(
                Events.ON_CLICK,
                event -> confirmAction("ACCEPT"));

        rejectButton.addEventListener(
                Events.ON_CLICK,
                event -> confirmAction("REJECT"));

        sendBackButton.addEventListener(
                Events.ON_CLICK,
                event -> confirmAction("SEND_BACK"));

        // ========================================================
        // SAVE & NEXT
        // ========================================================

        saveNextButton.addEventListener(
                Events.ON_CLICK,
                event -> saveAndNext());

        saveNextButton.setDisabled(true);

        // ========================================================
        // REASON
        // ========================================================

        reasonCombobox.addEventListener(
                Events.ON_SELECT,
                event -> updateSaveNextButton());
    }

    // ============================================================
    // SHOW FRONT IMAGE
    // ============================================================

    private void showFrontImage() {

        if (cheques == null
                || cheques.isEmpty()
                || currentChequeIndex >= cheques.size()) {

            return;
        }

        OutwardCheque currentCheque =
                cheques.get(currentChequeIndex);

        String frontImagePath =
                currentCheque.getFrontImagePath();

        if (frontImagePath != null
                && !frontImagePath.trim().isEmpty()) {

            chequeImage.setSrc(
                    frontImagePath.trim());

            applyImageTransform();

        } else {

            chequeImage.setSrc("");

            Messagebox.show(
                    "Front image is not available.",
                    "Image",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);
        }
    }

    // ============================================================
    // SHOW BACK IMAGE
    // ============================================================

    private void showBackImage() {

        if (cheques == null
                || cheques.isEmpty()
                || currentChequeIndex >= cheques.size()) {

            return;
        }

        OutwardCheque currentCheque =
                cheques.get(currentChequeIndex);

        String backImagePath =
                currentCheque.getBackImagePath();

        if (backImagePath != null
                && !backImagePath.trim().isEmpty()) {

            chequeImage.setSrc(
                    backImagePath.trim());

            applyImageTransform();

        } else {

            chequeImage.setSrc("");

            Messagebox.show(
                    "Back image is not available.",
                    "Image",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);
        }
    }

    // ============================================================
    // ZOOM IN
    // ============================================================

    private void zoomIn() {

        if (imageScale < 3.0) {

            imageScale += 0.25;
        }

        applyImageTransform();
    }

    // ============================================================
    // ZOOM OUT
    // ============================================================

    private void zoomOut() {

        if (imageScale > 0.50) {

            imageScale -= 0.25;
        }

        applyImageTransform();
    }

    // ============================================================
    // ROTATE IMAGE
    // ============================================================

    private void rotateImage() {

        imageRotation += 90;

        if (imageRotation >= 360) {

            imageRotation = 0;
        }

        applyImageTransform();
    }

    // ============================================================
    // APPLY IMAGE TRANSFORM
    // ============================================================

    private void applyImageTransform() {

        String transform =
                "transform: scale("
                        + imageScale
                        + ") rotate("
                        + imageRotation
                        + "deg);"
                        + " transform-origin: center center;";

        chequeImage.setStyle(transform);

        zoomLevelLabel.setValue(
                String.format(
                        "%.0f%%",
                        imageScale * 100));
    }

    // ============================================================
    // SAVE & NEXT
    // ============================================================

    private void saveAndNext() {

        if (cheques == null
                || cheques.isEmpty()
                || currentChequeIndex >= cheques.size()) {

            return;
        }

        // ========================================================
        // ACTION VALIDATION
        // ========================================================

        if (selectedAction == null
                || selectedAction.trim().isEmpty()) {

            Messagebox.show(
                    "Please select an action.",
                    "Validation",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }

        // ========================================================
        // REASON CODE
        // ========================================================

        String reasonCode = null;

        if ("REJECT".equalsIgnoreCase(selectedAction)
                || "SEND_BACK".equalsIgnoreCase(selectedAction)) {

            Comboitem selectedItem =
                    reasonCombobox.getSelectedItem();

            if (selectedItem == null) {

                Messagebox.show(
                        "Please select a reason.",
                        "Validation",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION);

                return;
            }

            reasonCode = selectedItem.getValue();

            if (reasonCode == null
                    || reasonCode.trim().isEmpty()) {

                Messagebox.show(
                        "Please select a valid reason.",
                        "Validation",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION);

                return;
            }

            reasonCode = reasonCode.trim();
        }

        // ========================================================
        // CURRENT CHEQUE
        // ========================================================

        OutwardCheque currentCheque =
                cheques.get(currentChequeIndex);

        if (currentCheque == null
                || currentCheque.getChequeNumber() == null
                || currentCheque.getChequeNumber()
                        .trim()
                        .isEmpty()) {

            Messagebox.show(
                    "Invalid cheque information.",
                    "Error",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        // ========================================================
        // SAVE
        // ========================================================

        boolean saved =
                processingService.saveCheckerDecision(
                        batchNumber,
                        currentCheque.getChequeNumber(),
                        checkerUserId,
                        selectedAction,
                        reasonCode,
                        checkerRemarksTextbox.getValue());

        // ========================================================
        // SAVE FAILED
        // ========================================================

        if (!saved) {

            Messagebox.show(
                    "Unable to save Checker decision.",
                    "Error",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        // ========================================================
        // SAVE SUCCESSFUL
        // ========================================================

        currentChequeIndex++;

        // ========================================================
        // ALL CHEQUES COMPLETED
        // ========================================================

        if (currentChequeIndex >= cheques.size()) {

            /*
             * For the normal Checker cycle, mark the current
             * Checker assignment as completed.
             *
             * The DAO has already decided the correct batch
             * status:
             *
             *   SEND_BACK exists -> ON_HOLD
             *   no SEND_BACK    -> CHECKER_VERIFIED
             *
             * We only complete the assignment here.
             */
            if (!reVerifyMode) {

                boolean completed =
                        batchService.completeBatch(
                                batchNumber,
                                checkerUserId);

                if (!completed) {

                    Messagebox.show(
                            "Checker decision was saved, but the Checker assignment could not be completed.",
                            "Warning",
                            Messagebox.OK,
                            Messagebox.EXCLAMATION);

                    return;
                }
            }

            Executions.sendRedirect(
                    "/zul/outward/outward-checker/batchesQueue.zul");

            return;
        }

        // ========================================================
        // LOAD NEXT CHEQUE
        // ========================================================

        loadCurrentCheque();
    }

    // ============================================================
    // LOAD CURRENT CHEQUE
    // ============================================================

    private void loadCurrentCheque() {

        if (cheques == null
                || cheques.isEmpty()
                || currentChequeIndex >= cheques.size()) {

            return;
        }

        // ========================================================
        // CURRENT CHEQUE
        // ========================================================

        OutwardCheque cheque =
                cheques.get(currentChequeIndex);

        if (cheque == null) {

            return;
        }

        // ========================================================
        // RE-VERIFY MODE
        // ========================================================

        /*
         * A corrected cheque comes from Maker with:
         *
         * RE_VERIFIED
         *
         * When the same Checker opens it for verification:
         *
         * RE_VERIFIED
         *      ↓
         * CHECKER_PROCESSING
         *
         * Only this individual cheque is changed.
         */

        if (reVerifyMode
                && "RE_VERIFIED".equalsIgnoreCase(
                        cheque.getChequeStatus())) {

            boolean started =
                    processingService.startCheckerProcessing(
                            batchNumber,
                            cheque.getChequeNumber());

            if (!started) {

                Messagebox.show(
                        "Unable to start Checker processing for this cheque.",
                        "Re-Verify",
                        Messagebox.OK,
                        Messagebox.ERROR);

                return;
            }

            /*
             * Keep the in-memory object synchronized
             * with the database.
             */
            cheque.setChequeStatus(
                    "CHECKER_PROCESSING");
        }

        // ========================================================
        // RESET IMAGE
        // ========================================================

        imageScale = 1.0;

        imageRotation = 0;

        applyImageTransform();

        // ========================================================
        // LOAD CHEQUE
        // ========================================================

        displayCheque(cheque);

        chequeSequence.setValue(
                String.format(
                        "%02d / %02d",
                        currentChequeIndex + 1,
                        cheques.size()));

        // ========================================================
        // RESET ACTION STATE
        // ========================================================

        selectedAction = null;

        reasonRow.setVisible(false);

        reasonCombobox.getItems().clear();

        reasonCombobox.setSelectedItem(null);

        checkerRemarksTextbox.setValue("");

        saveNextButton.setDisabled(true);
    }

    // ============================================================
    // LOAD BATCH
    // ============================================================

    private void loadBatch() {

        currentBatch =
                batchService.findBatch(batchNumber);

        if (currentBatch == null) {

            return;
        }

        verificationBatchId.setValue(
                "Batch: "
                        + currentBatch.getBatchNumber());

        chequeSequence.setValue(
                "Cheque 1 of "
                        + currentBatch.getNumberOfCheques());
    }

    // ============================================================
    // LOAD FIRST CHEQUE
    // ============================================================

    private void loadFirstCheque() {

        // ========================================================
        // RE-VERIFY MODE
        // ========================================================

        if (reVerifyMode) {

            loadReVerifyCheques();

            return;
        }

        // ========================================================
        // NORMAL CHECKER MODE
        // ========================================================

        cheques =
                batchService.getChequesByBatchNumber(
                        batchNumber);

        if (cheques == null
                || cheques.isEmpty()) {

            Messagebox.show(
                    "No cheques are available for this batch.",
                    "Processing",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }

        currentChequeIndex = 0;

        loadCurrentCheque();
    }

    // ============================================================
    // LOAD RE-VERIFY CHEQUES
    // ============================================================
    /*
     * Re-Verify eligibility is based only on the individual
     * cheque.
     *
     * Conditions:
     *
     * 1. checker_id = current Checker
     * 2. checker_action = SEND_BACK
     * 3. cheque_status = RE_VERIFIED
     *
     * IMPORTANT:
     *
     * A cheque which is still with Maker does NOT block
     * other corrected cheques from being re-verified.
     */

    private void loadReVerifyCheques() {

        List<OutwardCheque> allCheques =
                batchService.getChequesByBatchNumber(
                        batchNumber);

        if (allCheques == null
                || allCheques.isEmpty()) {

            cheques =
                    new ArrayList<>();

            Messagebox.show(
                    "No cheques are available for re-verification.",
                    "Re-Verify",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }

        List<OutwardCheque> reVerifiedCheques =
                new ArrayList<>();

        for (OutwardCheque cheque : allCheques) {

            if (cheque == null
                    || cheque.getChequeNumber() == null
                    || cheque.getChequeNumber()
                            .trim()
                            .isEmpty()) {

                continue;
            }

            ChequeProcessing processing =
                    processingService.getChequeProcessing(
                            batchNumber,
                            cheque.getChequeNumber());

            if (processing == null) {

                continue;
            }

            // ====================================================
            // SAME CHECKER
            // ====================================================

            Integer processingCheckerId =
                    processing.getCheckerId();

            if (processingCheckerId == null
                    || processingCheckerId.longValue()
                            != checkerUserId) {

                continue;
            }

            // ====================================================
            // PREVIOUS CHECKER DECISION WAS SEND_BACK
            // ====================================================

            if (!"SEND_BACK".equalsIgnoreCase(
                    processing.getCheckerAction())) {

                continue;
            }

            // ====================================================
            // MAKER HAS COMPLETED REWORK
            // ====================================================

            if ("RE_VERIFIED".equalsIgnoreCase(
                    cheque.getChequeStatus())) {

                reVerifiedCheques.add(cheque);
            }

            /*
             * If status is not RE_VERIFIED, the cheque is still
             * with Maker.
             *
             * Do NOT block the other re-verified cheques.
             */
        }

        // ========================================================
        // ONLY READY CHEQUES
        // ========================================================

        cheques = reVerifiedCheques;

        if (cheques.isEmpty()) {

            Messagebox.show(
                    "No re-verified cheques are currently available.",
                    "Re-Verify",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }

        currentChequeIndex = 0;

        chequeSequence.setValue(
                "Cheque 1 of "
                        + cheques.size());

        loadCurrentCheque();
    }

    // ============================================================
    // DISPLAY CHEQUE
    // ============================================================

    private void displayCheque(
            OutwardCheque cheque) {

        if (cheque == null) {

            return;
        }

        chequeNumberLabel.setValue(
                valueOrDash(
                        cheque.getChequeNumber()));

        /*
         * Drawer Account is the account from which
         * the cheque is drawn.
         *
         * This account is used for CBS validation.
         */

        accountNumberLabel.setValue(
                valueOrDash(
                        cheque.getDrawerAccountNumber()));

        drawerNameLabel.setValue(
                valueOrDash(
                        cheque.getDrawerName()));

        payeeNameLabel.setValue(
                valueOrDash(
                        cheque.getPayeeName()));

        amountLabel.setValue(
                cheque.getAmount() != null
                        ? cheque.getAmount().toString()
                        : "-");

        amountInWordsLabel.setValue(
                valueOrDash(
                        cheque.getAmountInWords()));

        chequeDateLabel.setValue(
                cheque.getChequeDate() != null
                        ? cheque.getChequeDate().toString()
                        : "-");

        // ========================================================
        // MICR
        // ========================================================

        micrLabel.setValue(
                buildMicr(cheque));

        // ========================================================
        // CHEQUE IMAGE
        // ========================================================

        String frontImagePath =
                cheque.getFrontImagePath();

        if (frontImagePath != null
                && !frontImagePath.trim().isEmpty()) {

            chequeImage.setSrc(
                    frontImagePath.trim());

            applyImageTransform();

        } else {

            chequeImage.setSrc("");
        }

        // ========================================================
        // MAKER REJECTION
        // ========================================================

        displayMakerRejection(cheque);

        // ========================================================
        // CBS VALIDATION
        // ========================================================

        validateCbs(cheque);
    }

    // ============================================================
    // DISPLAY MAKER REJECTION
    // ============================================================

    private void displayMakerRejection(
            OutwardCheque cheque) {

        /*
         * Always reset the Maker rejection block first.
         *
         * This is important because the next cheque may not
         * have been rejected/requested for rework by Maker.
         */

        makerRejectionBlock.setVisible(false);

        makerRejectionReason.setValue("");

        if (cheque == null) {

            return;
        }

        String chequeNumber =
                cheque.getChequeNumber();

        if (chequeNumber == null
                || chequeNumber.trim().isEmpty()) {

            return;
        }

        // ========================================================
        // GET MAKER REJECTION STATUS
        // ========================================================

        boolean makerRejected =
                processingService.isMakerRejected(
                        batchNumber,
                        chequeNumber);

        if (!makerRejected) {

            return;
        }

        // ========================================================
        // GET MAKER REASON CODE
        // ========================================================

        String reasonCode =
                processingService.getMakerReasonCode(
                        batchNumber,
                        chequeNumber);

        if (reasonCode == null
                || reasonCode.trim().isEmpty()) {

            makerRejectionReason.setValue(
                    "Reason: Not specified");

            makerRejectionBlock.setVisible(true);

            return;
        }

        reasonCode = reasonCode.trim();

        // ========================================================
        // GET REASON DESCRIPTION
        // ========================================================

        String reasonName =
                processingService.getMakerReasonName(
                        reasonCode);

        // ========================================================
        // DISPLAY
        // ========================================================

        if (reasonName == null
                || reasonName.trim().isEmpty()) {

            makerRejectionReason.setValue(
                    "Reason: "
                            + reasonCode);

        } else {

            makerRejectionReason.setValue(
                    "Reason: "
                            + reasonName.trim());
        }

        makerRejectionBlock.setVisible(true);
    }

    // ============================================================
    // BUILD MICR
    // ============================================================

    private String buildMicr(
            OutwardCheque cheque) {

        if (cheque == null) {

            return "-";
        }

        String cityCode =
                cheque.getCityCode();

        String bankCode =
                cheque.getBankCode();

        String branchCode =
                cheque.getBranchCode();

        if (cityCode == null
                || cityCode.trim().isEmpty()
                || bankCode == null
                || bankCode.trim().isEmpty()
                || branchCode == null
                || branchCode.trim().isEmpty()) {

            return "-";
        }

        return cityCode.trim()
                + bankCode.trim()
                + branchCode.trim();
    }

    // ============================================================
    // CBS VALIDATION
    // ============================================================

    private void validateCbs(
            OutwardCheque cheque) {

        if (cheque == null) {

            return;
        }

        cbsResult =
                processingService.validateCbsAccount(
                        cheque.getDrawerAccountNumber());

        // ========================================================
        // CBS PASS
        // ========================================================

        if ("PASS".equals(cbsResult)) {

            acceptButton.setDisabled(false);

            rejectButton.setDisabled(false);

            sendBackButton.setDisabled(false);

            accountVerificationIcon.setValue("✓");

            accountVerificationMessage.setValue(
                    "CBS Verified");

            accountVerificationReason.setVisible(false);

            return;
        }

        // ========================================================
        // CBS FAILURE
        // ========================================================

        acceptButton.setDisabled(true);

        rejectButton.setDisabled(false);

        sendBackButton.setDisabled(true);

        accountVerificationIcon.setValue("✕");

        accountVerificationMessage.setValue(
                "CBS Validation Failed");

        accountVerificationReason.setValue(
                processingService
                        .getCbsValidationMessage(
                                cbsResult));

        accountVerificationReason.setVisible(true);
    }

    // ============================================================
    // VALUE OR DASH
    // ============================================================

    private String valueOrDash(
            String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return "-";
        }

        return value;
    }

    // ============================================================
    // SELECT ACTION
    // ============================================================

    private void selectAction(
            String action) {

        selectedAction = action;

        // ========================================================
        // ACCEPT
        // ========================================================

        if ("ACCEPT".equalsIgnoreCase(action)) {

            reasonRow.setVisible(false);

            reasonCombobox.getItems().clear();

            reasonCombobox.setSelectedItem(null);

            saveNextButton.setDisabled(
                    !"PASS".equalsIgnoreCase(
                            cbsResult));

            return;
        }

        // ========================================================
        // REJECT / SEND BACK
        // ========================================================

        reasonRow.setVisible(true);

        loadReasonsForAction(action);

        saveNextButton.setDisabled(true);
    }

    // ============================================================
    // LOAD REASONS FOR ACTION
    // ============================================================

    private void loadReasonsForAction(
            String action) {

        reasonCombobox.getItems().clear();

        reasonCombobox.setSelectedItem(null);

        List<ReturnReason> reasons =
                processingService.getReturnReasons(
                        action);

        if (reasons == null
                || reasons.isEmpty()) {

            return;
        }

        for (ReturnReason reason : reasons) {

            if (reason == null) {

                continue;
            }

            Comboitem item =
                    reasonCombobox.appendItem(
                            reason.getReasonName());

            /*
             * Store reason CODE as the Comboitem value.
             *
             * Example:
             *
             * ACCOUNT_DETAILS_MISMATCH
             *
             * NOT numeric ID.
             */

            item.setValue(
                    reason.getReasonCode());
        }
    }

    // ============================================================
    // UPDATE SAVE BUTTON
    // ============================================================

    private void updateSaveNextButton() {

        if (selectedAction == null
                || selectedAction.trim().isEmpty()) {

            saveNextButton.setDisabled(true);

            return;
        }

        // ========================================================
        // ACCEPT
        // ========================================================

        if ("ACCEPT".equalsIgnoreCase(
                selectedAction)) {

            saveNextButton.setDisabled(
                    !"PASS".equalsIgnoreCase(
                            cbsResult));

            return;
        }

        // ========================================================
        // REJECT / SEND BACK
        // ========================================================

        if ("REJECT".equalsIgnoreCase(
                selectedAction)
                || "SEND_BACK".equalsIgnoreCase(
                        selectedAction)) {

            saveNextButton.setDisabled(
                    reasonCombobox
                            .getSelectedItem() == null);

            return;
        }

        saveNextButton.setDisabled(true);
    }

    // ============================================================
    // BACK TO QUEUE
    // ============================================================

    private void goBackToQueue() {

        String url =
                "/zul/outward/outward-checker/batchesQueue.zul";

        Executions.sendRedirect(url);
    }

    // ============================================================
    // CONFIRM ACTION
    // ============================================================

    private void confirmAction(
            String action) {

        String message;

        if ("ACCEPT".equalsIgnoreCase(action)) {

            message =
                    "Are you sure you want to ACCEPT this cheque?";

        } else if ("REJECT".equalsIgnoreCase(action)) {

            message =
                    "Are you sure you want to REJECT this cheque?";

        } else if ("SEND_BACK".equalsIgnoreCase(action)) {

            message =
                    "Are you sure you want to SEND BACK this cheque to Maker?";

        } else {

            return;
        }

        Messagebox.show(
                message,
                "Confirm Action",
                Messagebox.YES | Messagebox.NO,
                Messagebox.QUESTION,
                event -> {

                    if ("onYes".equals(event.getName())) {

                        selectAction(action);
                    }
                });
    }
}