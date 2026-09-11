package com.iispl.cts.controller.outward;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.service.outward.OutwardMakerMicrRepairDetailService;
import com.iispl.cts.service.outward.OutwardValidationService;

public class OutwardMakerMicrRepairDetailController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    @Wire
    private Label batchIdLabel;

    @Wire
    private Image frontImage;

    @Wire
    private Image backImage;

    @Wire
    private Textbox chequeNumberTextbox;

    @Wire
    private Label currentStatusLabel;

    @Wire
    private Textbox cityCodeTextbox;

    @Wire
    private Textbox bankCodeTextbox;

    @Wire
    private Textbox branchCodeTextbox;

    @Wire
    private Textbox originalMicrTextbox;

    @Wire
    private Label cityCodeLabel;

    @Wire
    private Label bankCodeLabel;

    @Wire
    private Label branchCodeLabel;

    @Wire
    private Button prevButton;

    @Wire
    private Button saveNextButton;

    private List<OutwardCheque> cheques;

    private int currentIndex = 0;

    private String batchNumber;

    private OutwardMakerMicrRepairDetailService service;

    private OutwardValidationService validationService;


    // =========================================================
    // PAGE INITIALIZATION
    // =========================================================

    @Override
    public void doAfterCompose(Component comp)
            throws Exception {

        super.doAfterCompose(comp);

        service =
                new OutwardMakerMicrRepairDetailService();

        validationService =
                new OutwardValidationService();

        batchNumber =
                Executions.getCurrent()
                        .getParameter("batchNumber");

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            Messagebox.show(
                    "Batch number is missing.",
                    "Error",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        batchNumber = batchNumber.trim();

        if (batchIdLabel != null) {
            batchIdLabel.setValue(batchNumber);
        }

        loadCheques();
    }


    // =========================================================
    // LOAD MICR ERROR CHEQUES
    // =========================================================

    private void loadCheques() {

        cheques =
                service.getMicrErrorCheques(
                        batchNumber);

        if (cheques == null
                || cheques.isEmpty()) {

            Messagebox.show(
                    "No MICR error cheques found for this batch.",
                    "Information",
                    Messagebox.OK,
                    Messagebox.INFORMATION);

            return;
        }

        currentIndex = 0;

        loadCurrentCheque();
    }


    // =========================================================
    // LOAD CURRENT CHEQUE
    // =========================================================

    private void loadCurrentCheque() {

        if (cheques == null
                || cheques.isEmpty()) {

            return;
        }

        if (currentIndex < 0) {
            currentIndex = 0;
        }

        if (currentIndex >= cheques.size()) {

            currentIndex =
                    cheques.size() - 1;
        }

        OutwardCheque cheque =
                cheques.get(currentIndex);


        // -----------------------------------------------------
        // Reset label styles before applying error highlighting
        // -----------------------------------------------------

        if (cityCodeLabel != null) {
            cityCodeLabel.setSclass(
                    "single-field-label");
        }

        if (bankCodeLabel != null) {
            bankCodeLabel.setSclass(
                    "single-field-label");
        }

        if (branchCodeLabel != null) {
            branchCodeLabel.setSclass(
                    "single-field-label");
        }


        // -----------------------------------------------------
        // Identify MICR error component
        // -----------------------------------------------------

        String micrErrorType =
                validationService.getMicrErrorType(
                        cheque);


        /*
         * Highlight only the incorrect
         * MICR component.
         */

        if ("CITY".equals(micrErrorType)) {

            if (cityCodeLabel != null) {
                cityCodeLabel.setSclass(
                        "micr-component-error");
            }

        } else if ("BANK".equals(micrErrorType)) {

            if (bankCodeLabel != null) {
                bankCodeLabel.setSclass(
                        "micr-component-error");
            }

        } else if ("BRANCH".equals(micrErrorType)) {

            if (branchCodeLabel != null) {
                branchCodeLabel.setSclass(
                        "micr-component-error");
            }
        }


        // -----------------------------------------------------
        // Populate cheque information
        // -----------------------------------------------------

        if (chequeNumberTextbox != null) {

            chequeNumberTextbox.setValue(
                    safe(cheque.getChequeNumber()));
        }

        if (currentStatusLabel != null) {

            currentStatusLabel.setValue(
                    safe(cheque.getChequeStatus()));
        }

        if (cityCodeTextbox != null) {

            cityCodeTextbox.setValue(
                    safe(cheque.getCityCode()));
        }

        if (bankCodeTextbox != null) {

            bankCodeTextbox.setValue(
                    safe(cheque.getBankCode()));
        }

        if (branchCodeTextbox != null) {

            branchCodeTextbox.setValue(
                    safe(cheque.getBranchCode()));
        }


        // -----------------------------------------------------
        // Original MICR
        // -----------------------------------------------------

        if (originalMicrTextbox != null) {

            originalMicrTextbox.setValue(
                    buildMicr(
                            cheque.getCityCode(),
                            cheque.getBankCode(),
                            cheque.getBranchCode()));
        }


        // -----------------------------------------------------
        // Apply validation CSS to current values
        // -----------------------------------------------------

        updateMicrFieldStyles();

        loadImages(cheque);

        updateStatusLabel();


        // -----------------------------------------------------
        // Previous button
        // -----------------------------------------------------

        if (prevButton != null) {

            prevButton.setDisabled(
                    currentIndex == 0);
        }


        // -----------------------------------------------------
        // Save button starts disabled
        // -----------------------------------------------------

        if (saveNextButton != null) {

            saveNextButton.setDisabled(true);
        }
    }


    // =========================================================
    // MICR FIELD CHANGE / TYPING VALIDATION
    // =========================================================

    @Listen(
        "onChange = #cityCodeTextbox, #bankCodeTextbox, #branchCodeTextbox; "
        + "onChanging = #cityCodeTextbox, #bankCodeTextbox, #branchCodeTextbox"
    )
    public void checkMicrFields() {

        if (cityCodeTextbox == null
                || bankCodeTextbox == null
                || branchCodeTextbox == null) {

            return;
        }

        String cityCode =
                cityCodeTextbox.getValue();

        String bankCode =
                bankCodeTextbox.getValue();

        String branchCode =
                branchCodeTextbox.getValue();


        boolean isCityValid =
                validationService.isValidMicrCode(
                        cityCode);

        boolean isBankValid =
                validationService.isValidMicrCode(
                        bankCode);

        boolean isBranchValid =
                validationService.isValidMicrCode(
                        branchCode);


        // -----------------------------------------------------
        // Apply corrected/error styling
        // -----------------------------------------------------

        cityCodeTextbox.setSclass(
                isCityValid
                        ? "micr-component-corrected"
                        : "micr-component-error");

        bankCodeTextbox.setSclass(
                isBankValid
                        ? "micr-component-corrected"
                        : "micr-component-error");

        branchCodeTextbox.setSclass(
                isBranchValid
                        ? "micr-component-corrected"
                        : "micr-component-error");


        updateStatusLabel();


        // -----------------------------------------------------
        // Enable Save only when user modified values
        // -----------------------------------------------------

        if (cheques != null
                && !cheques.isEmpty()
                && saveNextButton != null) {

            OutwardCheque cheque =
                    cheques.get(currentIndex);

            boolean isModified =
                    !cityCode.trim()
                            .equalsIgnoreCase(
                                    safe(
                                            cheque.getCityCode())
                                            .trim())

                    || !bankCode.trim()
                            .equalsIgnoreCase(
                                    safe(
                                            cheque.getBankCode())
                                            .trim())

                    || !branchCode.trim()
                            .equalsIgnoreCase(
                                    safe(
                                            cheque.getBranchCode())
                                            .trim());


            saveNextButton.setDisabled(
                    !isModified);
        }
    }


    // =========================================================
    // UPDATE FIELD CSS
    // =========================================================

    private void updateMicrFieldStyles() {

        if (cityCodeTextbox == null
                || bankCodeTextbox == null
                || branchCodeTextbox == null) {

            return;
        }

        String cityCode =
                cityCodeTextbox.getValue();

        String bankCode =
                bankCodeTextbox.getValue();

        String branchCode =
                branchCodeTextbox.getValue();


        boolean cityValid =
                validationService.isValidMicrCode(
                        cityCode);

        boolean bankValid =
                validationService.isValidMicrCode(
                        bankCode);

        boolean branchValid =
                validationService.isValidMicrCode(
                        branchCode);


        cityCodeTextbox.setSclass(
                cityValid
                        ? "micr-component-corrected"
                        : "micr-component-error");

        bankCodeTextbox.setSclass(
                bankValid
                        ? "micr-component-corrected"
                        : "micr-component-error");

        branchCodeTextbox.setSclass(
                branchValid
                        ? "micr-component-corrected"
                        : "micr-component-error");
    }


    // =========================================================
    // UPDATE MICR STATUS
    // =========================================================

    private void updateStatusLabel() {

        if (currentStatusLabel == null) {
            return;
        }

        String cityCode =
                cityCodeTextbox.getValue();

        String bankCode =
                bankCodeTextbox.getValue();

        String branchCode =
                branchCodeTextbox.getValue();


        boolean isAllValid =
                validationService.isValidMicrCode(
                        cityCode)

                && validationService.isValidMicrCode(
                        bankCode)

                && validationService.isValidMicrCode(
                        branchCode);


        if (isAllValid) {

            currentStatusLabel.setValue(
                    "MICR_REPAIRED");

            currentStatusLabel.setStyle(
                    "color:#008000;font-weight:bold;");

            currentStatusLabel.setSclass("");

        } else {

            currentStatusLabel.setValue(
                    "MICR_ERROR");

            currentStatusLabel.setStyle(
                    "color:#D92D20;font-weight:bold;");

            currentStatusLabel.setSclass(
                    "micr-error-label");
        }
    }


    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }


    // =========================================================
    // BUILD ORIGINAL MICR
    // =========================================================

    private String buildMicr(
            String cityCode,
            String bankCode,
            String branchCode) {

        return safe(cityCode)
                + safe(bankCode)
                + safe(branchCode);
    }


    // =========================================================
    // LOAD CHEQUE IMAGES
    // =========================================================

    private void loadImages(
            OutwardCheque cheque) {

        if (frontImage == null
                || backImage == null) {

            return;
        }

        String frontPath =
                cheque.getFrontImagePath();

        String backPath =
                cheque.getBackImagePath();


        if (frontPath != null
                && !frontPath.trim().isEmpty()) {

            frontImage.setSrc(frontPath);

        } else {

            frontImage.setSrc(
                    "https://placehold.co/900x400?text=Cheque+Front");
        }


        if (backPath != null
                && !backPath.trim().isEmpty()) {

            backImage.setSrc(backPath);

        } else {

            backImage.setSrc(
                    "https://placehold.co/900x400?text=Cheque+Back");
        }
    }


    // =========================================================
    // SAVE & NEXT
    // =========================================================

    @Listen("onClick = #saveNextButton")
    public void saveAndNext() {

        if (cheques == null
                || cheques.isEmpty()) {

            return;
        }


        OutwardCheque cheque =
                cheques.get(currentIndex);


        String cityCode =
                cityCodeTextbox.getValue().trim();

        String bankCode =
                bankCodeTextbox.getValue().trim();

        String branchCode =
                branchCodeTextbox.getValue().trim();


        // =====================================================
        // 1. VALIDATE MICR FORMAT
        // =====================================================

        if (!validationService.isValidMicrCode(
                    cityCode)

                || !validationService.isValidMicrCode(
                    bankCode)

                || !validationService.isValidMicrCode(
                    branchCode)) {

            Messagebox.show(
                    "Please enter valid 3-digit MICR codes for City, Bank, and Branch.",
                    "MICR Validation",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }


        // =====================================================
        // 2. SAVE TO DATABASE
        // =====================================================

        boolean updated =
                service.updateCorrectedMicr(
                        batchNumber,
                        cheque.getChequeNumber(),
                        cityCode,
                        bankCode,
                        branchCode);


        if (!updated) {

            Messagebox.show(
                    "MICR repair could not be saved.",
                    "Error",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }


        // =====================================================
        // 3. UPDATE CURRENT OBJECT
        // =====================================================

        cheque.setCityCode(cityCode);

        cheque.setBankCode(bankCode);

        cheque.setBranchCode(branchCode);

        cheque.setChequeStatus(
                "MICR_REPAIRED");


        // Disable Save after successful save

        if (saveNextButton != null) {

            saveNextButton.setDisabled(true);
        }


        // =====================================================
        // 4. CHECK REMAINING MICR ERRORS
        // =====================================================

        boolean remaining =
                service.hasRemainingMicrErrors(
                        batchNumber);


        // =====================================================
        // 5. ALL MICR ERRORS REPAIRED
        // =====================================================

        if (!remaining) {

            boolean batchUpdated =
                    service.updateBatchStatus(
                            batchNumber);


            if (!batchUpdated) {

                Messagebox.show(
                        "MICR repair completed, but batch status could not be updated.",
                        "Warning",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION);

                return;
            }


            Messagebox.show(
                    "All MICR errors have been repaired.\n"
                    + "Batch status changed to MICR_REPAIR_COMPLETED.",
                    "MICR Repair Completed",
                    Messagebox.OK,
                    Messagebox.INFORMATION);


            Executions.getCurrent()
                    .sendRedirect(
                            "outward-maker-micr-repair.zul");

            return;
        }


        // =====================================================
        // 6. REMOVE REPAIRED CHEQUE
        // =====================================================

        cheques.remove(currentIndex);


        // =====================================================
        // 7. LOAD NEXT REMAINING CHEQUE
        // =====================================================

        if (!cheques.isEmpty()) {

            if (currentIndex >= cheques.size()) {

                currentIndex =
                        cheques.size() - 1;
            }

            loadCurrentCheque();

        } else {

            Executions.getCurrent()
                    .sendRedirect(
                            "outward-maker-micr-repair.zul");
        }
    }


    // =========================================================
    // PREVIOUS CHEQUE
    // =========================================================

    @Listen("onClick = #prevButton")
    public void previousCheque() {

        if (cheques == null
                || cheques.isEmpty()) {

            return;
        }


        if (currentIndex > 0) {

            currentIndex--;

            loadCurrentCheque();

        } else {

            Messagebox.show(
                    "This is the first cheque.",
                    "Information",
                    Messagebox.OK,
                    Messagebox.INFORMATION);
        }
    }


    // =========================================================
    // BACK
    // =========================================================

    @Listen("onClick = #backButton")
    public void back() {

        Executions.getCurrent()
                .sendRedirect(
                        "outward-maker-micr-repair.zul");
    }
}