package com.cts.inward.controller;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Combobox;
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

    // -------------------------------------------------------------------------
    // ZUL Components
    // -------------------------------------------------------------------------

    private Label batchLabel;
    private Label chequeCounter;

    private Button frontButton;
    private Button backButton;
    private Button grayButton;

    private Label bankName;
    private Label chequeNumberImage;
    private Image chequeImage;

    private Textbox chequeNumber;

    private Textbox npciCityCode;
    private Textbox npciBankCode;
    private Textbox npciBranchCode;
    private Textbox npciMicrCode;

    private Textbox ocrCityCode;
    private Textbox ocrBankCode;
    private Textbox ocrBranchCode;
    private Textbox ocrMicrCode;

    /*
     * Internally generated corrected MICR.
     * Maker does not manually type this.
     */
    private Textbox micrCode;

    private Button backToList;
    private Button previousButton;
    private Button nextButton;
    private Button saveNextButton;
    private Button returnButton;

    private Window returnWindow;
    private Combobox returnReason;
    private Textbox returnRemarks;
    private Button cancelReturnButton;
    private Button confirmReturnButton;

    // -------------------------------------------------------------------------
    // Service
    // -------------------------------------------------------------------------

    private MicrRepairService micrRepairService;

    // -------------------------------------------------------------------------
    // Page state
    // -------------------------------------------------------------------------

    private long batchId;
    private int chequeIndex;
    private String source;

    private List<MicrComparisonDto> comparisons;

    // -------------------------------------------------------------------------
    // Image state
    // -------------------------------------------------------------------------

    private String frontImagePath;
    private String backImagePath;

    // -------------------------------------------------------------------------
    // Composer lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void doAfterCompose(
            Component comp)
            throws Exception {

        super.doAfterCompose(comp);

        micrRepairService =
                new MicrRepairServiceImpl();


        String batchIdParameter =
                Executions.getCurrent()
                        .getParameter("batchId");

        String chequeIndexParameter =
                Executions.getCurrent()
                        .getParameter("chequeIndex");

        source =
                Executions.getCurrent()
                        .getParameter("source");


        if (batchIdParameter == null
                || batchIdParameter.trim().isEmpty()) {

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
                            batchIdParameter.trim());

        } catch (NumberFormatException e) {

            Messagebox.show(
                    "Invalid Batch ID.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }


        if (chequeIndexParameter != null
                && !chequeIndexParameter.trim().isEmpty()) {

            try {

                chequeIndex =
                        Integer.parseInt(
                                chequeIndexParameter.trim());

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


        initializeReturnWindowComponents();


        if (chequeIndex < 0) {

            Messagebox.show(
                    "No MICR repair is pending for Batch ID: "
                            + batchId,
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.INFORMATION,
                    event -> goBack());

            return;
        }


        loadBatch();

        registerEvents();
    }

    // -------------------------------------------------------------------------
    // Return window components
    // -------------------------------------------------------------------------

    private void initializeReturnWindowComponents() {

        if (returnWindow == null) {
            return;
        }


        returnReason =
                (Combobox) returnWindow
                        .getFellow(
                                "returnReason");


        returnRemarks =
                (Textbox) returnWindow
                        .getFellow(
                                "returnRemarks");


        cancelReturnButton =
                (Button) returnWindow
                        .getFellow(
                                "cancelReturnButton");


        confirmReturnButton =
                (Button) returnWindow
                        .getFellow(
                                "confirmReturnButton");
    }

    // -------------------------------------------------------------------------
    // Load batch
    // -------------------------------------------------------------------------

    private void loadBatch() {

        comparisons =
                micrRepairService.compareBatch(
                        batchId);


        if (comparisons == null
                || comparisons.isEmpty()) {

            Messagebox.show(
                    "No cheque data found for Batch ID: "
                            + batchId,
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.INFORMATION,
                    event -> goBack());

            return;
        }


        if (chequeIndex < 0) {

            chequeIndex = 0;
        }


        if (chequeIndex >= comparisons.size()) {

            chequeIndex =
                    comparisons.size() - 1;
        }


        batchLabel.setValue(
                "Batch ID : " + batchId);


        loadCheque();
    }

    // -------------------------------------------------------------------------
    // Load current cheque
    // -------------------------------------------------------------------------

    private void loadCheque() {

        if (comparisons == null
                || comparisons.isEmpty()) {

            return;
        }


        if (chequeIndex < 0
                || chequeIndex >= comparisons.size()) {

            return;
        }


        MicrComparisonDto comparison =
                comparisons.get(
                        chequeIndex);


        updateChequeCounter();

        populateNpciData(comparison);

        populateOcrData(comparison);

        loadImages(
                comparison.getChequeNumber());

        clearOcrErrorStyles();

        applyOcrErrorStyles(
                comparison);

        configureEditableFields(
                comparison);

        updateCorrectedMicr();


        /*
         * Static repair page.
         */
        previousButton.setDisabled(true);

        nextButton.setDisabled(true);
    }

    // -------------------------------------------------------------------------
    // NPCI data
    // -------------------------------------------------------------------------

    private void populateNpciData(
            MicrComparisonDto comparison) {

        chequeNumber.setValue(
                safe(
                        comparison.getChequeNumber()));


        npciCityCode.setValue(
                safe(
                        comparison.getNpciCityCode()));


        npciBankCode.setValue(
                safe(
                        comparison.getNpciBankCode()));


        npciBranchCode.setValue(
                safe(
                        comparison.getNpciBranchCode()));


        npciMicrCode.setValue(
                safe(
                        comparison.getNpciMicrCode()));


        bankName.setValue("");

        chequeNumberImage.setValue(
                safe(
                        comparison.getChequeNumber()));
    }

    // -------------------------------------------------------------------------
    // OCR data
    // -------------------------------------------------------------------------

    private void populateOcrData(
            MicrComparisonDto comparison) {

        ocrCityCode.setValue(
                safe(
                        comparison.getOcrCityCode()));


        ocrBankCode.setValue(
                safe(
                        comparison.getOcrBankCode()));


        ocrBranchCode.setValue(
                safe(
                        comparison.getOcrBranchCode()));


        ocrMicrCode.setValue(
                safe(
                        comparison.getOcrMicrCode()));
    }

    // -------------------------------------------------------------------------
    // Configure editable fields
    // -------------------------------------------------------------------------

    private void configureEditableFields(
            MicrComparisonDto comparison) {

        /*
         * Only the mismatching component becomes editable.
         *
         * Matching fields remain read-only.
         */

        ocrCityCode.setReadonly(
                !comparison.isCityCodeMismatch());


        ocrBankCode.setReadonly(
                !comparison.isBankCodeMismatch());


        ocrBranchCode.setReadonly(
                !comparison.isBranchCodeMismatch());


        /*
         * Complete OCR MICR is never manually edited.
         */
        ocrMicrCode.setReadonly(true);


        /*
         * Corrected MICR is generated internally.
         */
        micrCode.setReadonly(true);
    }

    // -------------------------------------------------------------------------
    // Generate corrected MICR
    // -------------------------------------------------------------------------

    private void updateCorrectedMicr() {

        String city =
                safe(
                        ocrCityCode.getValue()).trim();

        String bank =
                safe(
                        ocrBankCode.getValue()).trim();

        String branch =
                safe(
                        ocrBranchCode.getValue()).trim();


        /*
         * If any component is incomplete,
         * there is no complete MICR yet.
         */
        if (!city.matches("\\d{3}")
                || !bank.matches("\\d{3}")
                || !branch.matches("\\d{3}")) {

            micrCode.setValue("");

            return;
        }


        /*
         * Corrected MICR is:
         *
         * City + Bank + Branch
         */
        String correctedMicr =
                city
                + bank
                + branch;


        micrCode.setValue(
                correctedMicr);
    }

    // -------------------------------------------------------------------------
    // Validate components
    // -------------------------------------------------------------------------

    private boolean validateMicrComponents() {

        String city =
                safe(
                        ocrCityCode.getValue()).trim();

        String bank =
                safe(
                        ocrBankCode.getValue()).trim();

        String branch =
                safe(
                        ocrBranchCode.getValue()).trim();


        if (!city.matches("\\d{3}")) {

            Messagebox.show(
                    "City Code must contain exactly 3 digits.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return false;
        }


        if (!bank.matches("\\d{3}")) {

            Messagebox.show(
                    "Bank Code must contain exactly 3 digits.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return false;
        }


        if (!branch.matches("\\d{3}")) {

            Messagebox.show(
                    "Branch Code must contain exactly 3 digits.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return false;
        }


        return true;
    }

    // -------------------------------------------------------------------------
    // Image loading
    // -------------------------------------------------------------------------

    private void loadImages(
            String chequeNumberValue) {

        frontImagePath =
                micrRepairService
                        .getFrontImagePath(
                                chequeNumberValue);

        backImagePath =
                micrRepairService
                        .getBackImagePath(
                                chequeNumberValue);


        resetImageStyle();

        showFrontImage();
    }

    // -------------------------------------------------------------------------
    // Front image
    // -------------------------------------------------------------------------

    private void showFrontImage() {

        resetImageStyle();

        frontButton.setSclass(
                "image-button image-button-active");

        backButton.setSclass(
                "image-button");

        grayButton.setSclass(
                "image-button");


        if (frontImagePath != null
                && !frontImagePath.trim().isEmpty()) {

            chequeImage.setSrc(
                    frontImagePath);

        } else {

            chequeImage.setSrc(null);
        }
    }

    // -------------------------------------------------------------------------
    // Back image
    // -------------------------------------------------------------------------

    private void showBackImage() {

        resetImageStyle();

        frontButton.setSclass(
                "image-button");

        backButton.setSclass(
                "image-button image-button-active");

        grayButton.setSclass(
                "image-button");


        if (backImagePath != null
                && !backImagePath.trim().isEmpty()) {

            chequeImage.setSrc(
                    backImagePath);

        } else {

            chequeImage.setSrc(null);
        }
    }

    // -------------------------------------------------------------------------
    // Grayscale image
    // -------------------------------------------------------------------------

    private void showGrayImage() {

        frontButton.setSclass(
                "image-button");

        backButton.setSclass(
                "image-button");

        grayButton.setSclass(
                "image-button image-button-active");


        chequeImage.setStyle(
                "width:100%;"
                + "height:430px;"
                + "object-fit:contain;"
                + "border:1px solid #dce3ee;"
                + "background:#effdf3;"
                + "filter:grayscale(100%);");
    }

    // -------------------------------------------------------------------------
    // Static counter
    // -------------------------------------------------------------------------

    private void updateChequeCounter() {

        chequeCounter.setValue(
                "☷  Cheque "
                + (chequeIndex + 1)
                + " of "
                + comparisons.size());
    }

    // -------------------------------------------------------------------------
    // Previous
    // -------------------------------------------------------------------------

    private void goToPrevious() {

        Messagebox.show(
                "This repair page is fixed to the current cheque. "
                        + "Use Save & Next after completing the repair.",
                "MICR Repair",
                Messagebox.OK,
                Messagebox.INFORMATION);
    }

    // -------------------------------------------------------------------------
    // Next
    // -------------------------------------------------------------------------

    private void goToNext() {

        Messagebox.show(
                "This repair page is fixed to the current cheque. "
                        + "Use Save & Next after completing the repair.",
                "MICR Repair",
                Messagebox.OK,
                Messagebox.INFORMATION);
    }

    // -------------------------------------------------------------------------
    // Save & Next
    // -------------------------------------------------------------------------

    private void saveAndNext() {

        if (comparisons == null
                || comparisons.isEmpty()
                || chequeIndex < 0
                || chequeIndex >= comparisons.size()) {

            Messagebox.show(
                    "Current cheque data is unavailable.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }


        if (!validateMicrComponents()) {
            return;
        }


        updateCorrectedMicr();


        String correctedMicr =
                safe(
                        micrCode.getValue()).trim();


        if (!correctedMicr.matches(
                "\\d{9}")) {

            Messagebox.show(
                    "Corrected MICR could not be generated.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }


        MicrComparisonDto comparison =
                comparisons.get(
                        chequeIndex);


        String chequeNumberValue =
                safe(
                        comparison.getChequeNumber()).trim();


        if (chequeNumberValue.isEmpty()) {

            Messagebox.show(
                    "Cheque number is missing.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }


        String originalMicr =
                safe(
                        comparison.getOcrMicrCode()).trim();


        if (originalMicr.isEmpty()) {

            Messagebox.show(
                    "OCR MICR code is missing.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }


        User loggedInUser =
                (User) Executions.getCurrent()
                        .getSession()
                        .getAttribute(
                                "loggedInUser");


        if (loggedInUser == null) {

            Messagebox.show(
                    "User session has expired. Please login again.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }


        long userId =
                loggedInUser.getUserId();


        try {

            boolean saved =
                    micrRepairService.saveMicrRepair(
                            chequeNumberValue,
                            originalMicr,
                            correctedMicr,
                            null,
                            userId);


            if (!saved) {

                Messagebox.show(
                        "Unable to save MICR repair.",
                        "MICR Repair",
                        Messagebox.OK,
                        Messagebox.ERROR);

                return;
            }


            Messagebox.show(
                    "MICR repair saved successfully.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.INFORMATION,
                    event -> moveAfterSave());


        } catch (IllegalArgumentException e) {

            Messagebox.show(
                    e.getMessage(),
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);


        } catch (Exception e) {

            Messagebox.show(
                    "Unable to save MICR repair: "
                            + e.getMessage(),
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    // -------------------------------------------------------------------------
    // Move after save
    // -------------------------------------------------------------------------

    private void moveAfterSave() {

        comparisons =
                micrRepairService.compareBatch(
                        batchId);


        int nextRepairIndex =
                micrRepairService.getNextRepairIndex(
                        batchId);


        if (nextRepairIndex < 0) {

            Messagebox.show(
                    "MICR repair completed for this batch.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.INFORMATION,
                    event -> goBack());

            return;
        }


        chequeIndex =
                nextRepairIndex;


        loadCheque();
    }

    // -------------------------------------------------------------------------
    // Return
    // -------------------------------------------------------------------------

    private void openReturnWindow() {

        if (returnWindow == null
                || returnReason == null
                || returnRemarks == null) {

            Messagebox.show(
                    "Return dialog is not available.",
                    "Return Cheque",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }


        returnReason.getItems().clear();

        returnReason.setSelectedItem(
                null);

        returnRemarks.setValue("");


        List<ReturnReasonDto> reasons =
                micrRepairService
                        .getMakerReturnReasons();


        if (reasons != null) {

            for (ReturnReasonDto reason :
                    reasons) {

                Comboitem item =
                        new Comboitem();

                item.setLabel(
                        reason.getDescription());

                item.setValue(
                        reason.getReturnReasonCode());

                returnReason.appendChild(item);
            }
        }


        returnWindow.setVisible(true);
    }

    // -------------------------------------------------------------------------
    // Cancel return
    // -------------------------------------------------------------------------

    private void cancelReturn() {

        if (returnReason != null) {

            returnReason.setSelectedItem(
                    null);
        }


        if (returnRemarks != null) {

            returnRemarks.setValue("");
        }


        if (returnWindow != null) {

            returnWindow.setVisible(false);
        }
    }

    // -------------------------------------------------------------------------
    // Confirm return
    // -------------------------------------------------------------------------

    private void confirmReturn() {

        if (returnReason == null) {

            Messagebox.show(
                    "Return reason field is unavailable.",
                    "Return Cheque",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }


        Comboitem selectedItem =
                returnReason.getSelectedItem();


        if (selectedItem == null) {

            Messagebox.show(
                    "Please select a return reason.",
                    "Return Cheque",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);

            return;
        }


        String returnReasonCode =
                (String) selectedItem.getValue();


        if (returnReasonCode == null
                || returnReasonCode.trim().isEmpty()) {

            Messagebox.show(
                    "Invalid return reason.",
                    "Return Cheque",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }


        if (comparisons == null
                || comparisons.isEmpty()
                || chequeIndex < 0
                || chequeIndex >= comparisons.size()) {

            Messagebox.show(
                    "Current cheque data is unavailable.",
                    "Return Cheque",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }


        String remarks =
                returnRemarks == null
                        ? null
                        : returnRemarks.getValue();


        MicrComparisonDto comparison =
                comparisons.get(
                        chequeIndex);


        String chequeNumberValue =
                safe(
                        comparison.getChequeNumber()).trim();


        if (chequeNumberValue.isEmpty()) {

            Messagebox.show(
                    "Cheque number is missing.",
                    "Return Cheque",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }


        User loggedInUser =
                (User) Executions.getCurrent()
                        .getSession()
                        .getAttribute(
                                "loggedInUser");


        if (loggedInUser == null) {

            Messagebox.show(
                    "User session has expired. Please login again.",
                    "Return Cheque",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }


        long userId =
                loggedInUser.getUserId();


        try {

            boolean saved =
                    micrRepairService.saveMakerReturn(
                            chequeNumberValue,
                            returnReasonCode,
                            remarks,
                            userId);


            if (!saved) {

                Messagebox.show(
                        "Unable to save the return request.",
                        "Return Cheque",
                        Messagebox.OK,
                        Messagebox.ERROR);

                return;
            }


            returnWindow.setVisible(false);


            Messagebox.show(
                    "Cheque "
                            + chequeNumberValue
                            + " has been submitted for return.",
                    "Return Cheque",
                    Messagebox.OK,
                    Messagebox.INFORMATION,
                    event -> goBack());


        } catch (Exception e) {

            Messagebox.show(
                    "Unable to submit return request: "
                            + e.getMessage(),
                    "Return Cheque",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    // -------------------------------------------------------------------------
    // Back
    // -------------------------------------------------------------------------

    private void goBack() {

        if ("dashboard".equalsIgnoreCase(source)) {

            Executions.sendRedirect(
                    "/zul/inward-maker/dashboard.zul");

        } else {

            Executions.sendRedirect(
                    "/zul/inward-maker/micr-repair-list.zul");
        }
    }

    // -------------------------------------------------------------------------
    // Event registration
    // -------------------------------------------------------------------------

    private void registerEvents() {

        frontButton.addEventListener(
                Events.ON_CLICK,
                event -> showFrontImage());


        backButton.addEventListener(
                Events.ON_CLICK,
                event -> showBackImage());


        grayButton.addEventListener(
                Events.ON_CLICK,
                event -> showGrayImage());


        previousButton.addEventListener(
                Events.ON_CLICK,
                event -> goToPrevious());


        nextButton.addEventListener(
                Events.ON_CLICK,
                event -> goToNext());


        saveNextButton.addEventListener(
                Events.ON_CLICK,
                event -> saveAndNext());


        returnButton.addEventListener(
                Events.ON_CLICK,
                event -> openReturnWindow());


        backToList.addEventListener(
                Events.ON_CLICK,
                event -> goBack());


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


        /*
         * When a Maker edits one of the mismatching components,
         * regenerate the complete corrected MICR.
         */
        ocrCityCode.addEventListener(
                Events.ON_CHANGE,
                event -> updateCorrectedMicr());


        ocrBankCode.addEventListener(
                Events.ON_CHANGE,
                event -> updateCorrectedMicr());


        ocrBranchCode.addEventListener(
                Events.ON_CHANGE,
                event -> updateCorrectedMicr());
    }

    // -------------------------------------------------------------------------
    // Clear OCR error styles
    // -------------------------------------------------------------------------

    private void clearOcrErrorStyles() {

        ocrCityCode.setSclass(
                "ocr-field");

        ocrBankCode.setSclass(
                "ocr-field");

        ocrBranchCode.setSclass(
                "ocr-field");

        ocrMicrCode.setSclass(
                "ocr-field");
    }

    // -------------------------------------------------------------------------
    // Apply OCR error styles
    // -------------------------------------------------------------------------

    private void applyOcrErrorStyles(
            MicrComparisonDto comparison) {

        /*
         * Only the individual mismatching component
         * gets the error style.
         */

        if (comparison.isCityCodeMismatch()) {

            ocrCityCode.setSclass(
                    "ocr-field ocr-error");
        }


        if (comparison.isBankCodeMismatch()) {

            ocrBankCode.setSclass(
                    "ocr-field ocr-error");
        }


        if (comparison.isBranchCodeMismatch()) {

            ocrBranchCode.setSclass(
                    "ocr-field ocr-error");
        }


        /*
         * Do NOT highlight the complete OCR MICR here.
         *
         * The mismatch is represented by the individual
         * component(s).
         */
        ocrMicrCode.setSclass(
                "ocr-field");
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private String safe(
            String value) {

        return value == null
                ? ""
                : value;
    }

    // -------------------------------------------------------------------------
    // Reset image style
    // -------------------------------------------------------------------------

    private void resetImageStyle() {

        chequeImage.setStyle(
                "width:100%;"
                + "height:430px;"
                + "object-fit:contain;"
                + "border:1px solid #dce3ee;"
                + "background:#effdf3;");
    }
}