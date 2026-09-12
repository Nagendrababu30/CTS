package com.iispl.cts.controller.outward.checker;

import java.util.List;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.event.Events;

import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

import com.cts.admin.model.User;

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

    @Wire
    private Label accountVerificationIcon;

    @Wire
    private Label accountVerificationMessage;

    @Wire
    private Label accountVerificationReason;

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

    private CheckerBatchService batchService;

    private CheckerProcessingService processingService;

    private String batchNumber;

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

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            return;
        }

        batchNumber = batchNumber.trim();


        // ========================================================
        // LOAD DATA
        // ========================================================

        loadBatch();

        loadFirstCheque();


        // ========================================================
        // BUTTON EVENTS
        // ========================================================

        backButton.addEventListener(
                "onClick",
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
        // CHECKER ACTION BUTTONS
        // ========================================================

        acceptButton.addEventListener(
                "onClick",
                event -> confirmAction("ACCEPT"));

        rejectButton.addEventListener(
                "onClick",
                event -> confirmAction("REJECT"));

        sendBackButton.addEventListener(
                "onClick",
                event -> confirmAction("SEND_BACK"));


        // ========================================================
        // SAVE & NEXT
        // ========================================================

        saveNextButton.addEventListener(
                "onClick",
                event -> saveAndNext());

        saveNextButton.setDisabled(true);


        // ========================================================
        // REASON
        // ========================================================

        reasonCombobox.addEventListener(
                "onSelect",
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

        OutwardCheque cheque =
                cheques.get(currentChequeIndex);

        displayCheque(cheque);


        chequeSequence.setValue(
                String.format(
                        "%02d / %02d",
                        currentChequeIndex + 1,
                        cheques.size()));


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

        cheques =
                batchService.getChequesByBatchNumber(
                        batchNumber);

        if (cheques == null
                || cheques.isEmpty()) {

            return;
        }

        currentChequeIndex = 0;

        loadCurrentCheque();
    }


    // ============================================================
    // DISPLAY CHEQUE
    // ============================================================

    private void displayCheque(OutwardCheque cheque) {

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


        /*
         * MICR:
         *
         * City Code + Bank Code + Branch Code
         */

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

        } else {

            chequeImage.setSrc("");
        }


        // ========================================================
        // CBS VALIDATION
        // ========================================================

        validateCbs(cheque);
    }


    // ============================================================
    // BUILD MICR
    // ============================================================

    private String buildMicr(OutwardCheque cheque) {

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

    private void validateCbs(OutwardCheque cheque) {

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
                        .getCbsValidationMessage(cbsResult));

        accountVerificationReason.setVisible(true);
    }


    // ============================================================
    // VALUE OR DASH
    // ============================================================

    private String valueOrDash(String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return "-";
        }

        return value;
    }


    // ============================================================
    // SELECT ACTION
    // ============================================================

    private void selectAction(String action) {

        selectedAction = action;


        // ========================================================
        // ACCEPT
        // ========================================================

        if ("ACCEPT".equalsIgnoreCase(action)) {

            reasonRow.setVisible(false);

            reasonCombobox.getItems().clear();

            reasonCombobox.setSelectedItem(null);

            saveNextButton.setDisabled(
                    !"PASS".equalsIgnoreCase(cbsResult));

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

    private void loadReasonsForAction(String action) {

        reasonCombobox.getItems().clear();

        reasonCombobox.setSelectedItem(null);


        List<ReturnReason> reasons =
                processingService.getReturnReasons(action);


        if (reasons == null
                || reasons.isEmpty()) {

            return;
        }


        for (ReturnReason reason : reasons) {

            Comboitem item =
                    reasonCombobox.appendItem(
                            reason.getReasonName());

            /*
             * IMPORTANT:
             *
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

        if ("ACCEPT".equalsIgnoreCase(selectedAction)) {

            saveNextButton.setDisabled(
                    !"PASS".equalsIgnoreCase(cbsResult));

            return;
        }


        // ========================================================
        // REJECT / SEND BACK
        // ========================================================

        if ("REJECT".equalsIgnoreCase(selectedAction)
                || "SEND_BACK".equalsIgnoreCase(selectedAction)) {

            saveNextButton.setDisabled(
                    reasonCombobox.getSelectedItem() == null);

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

    private void confirmAction(String action) {

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