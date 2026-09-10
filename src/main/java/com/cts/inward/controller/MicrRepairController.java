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

    private Label batchLabel;
    private Label chequeCounter;

    private Button frontButton;
    private Button backButton;

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

    private MicrRepairService micrRepairService;

    private long batchId;
    private int chequeIndex;
    private String source;

    private List<MicrComparisonDto> comparisons;

    private String frontImagePath;
    private String backImagePath;

    private String correctedMicr;

    @Override
    public void doAfterCompose(Component comp)
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
                                .getNextRepairIndex(batchId);
            }

        } else {

            chequeIndex =
                    micrRepairService
                            .getNextRepairIndex(batchId);
        }

        initializeReturnWindowComponents();

        if (chequeIndex < 0) {

            showBatchReadyAndOpenDataEntry();
            return;
        }

        loadBatch();

        registerEvents();
    }

    private void initializeReturnWindowComponents() {

        if (returnWindow == null) {
            return;
        }

        returnReason =
                (Combobox) returnWindow
                        .getFellow("returnReason");

        returnRemarks =
                (Textbox) returnWindow
                        .getFellow("returnRemarks");

        cancelReturnButton =
                (Button) returnWindow
                        .getFellow("cancelReturnButton");

        confirmReturnButton =
                (Button) returnWindow
                        .getFellow("confirmReturnButton");
    }

    private void loadBatch() {

        comparisons =
                micrRepairService
                        .compareBatch(batchId);

        if (comparisons == null
                || comparisons.isEmpty()) {

            showBatchReadyAndOpenDataEntry();
            return;
        }

        if (chequeIndex < 0
                || chequeIndex >= comparisons.size()
                || comparisons.get(chequeIndex) == null
                || !comparisons.get(chequeIndex)
                        .isNeedsMicrRepair()) {

            chequeIndex =
                    findNextRepairIndex(
                            chequeIndex);

            if (chequeIndex < 0) {

                showBatchReadyAndOpenDataEntry();
                return;
            }
        }

        batchLabel.setValue(
                "Batch ID : " + batchId);

        loadCheque();
    }

    private int findNextRepairIndex(
            int startIndex) {

        if (comparisons == null
                || comparisons.isEmpty()) {

            return -1;
        }

        int start =
                Math.max(0, startIndex);

        for (int i = start;
                i < comparisons.size();
                i++) {

            MicrComparisonDto comparison =
                    comparisons.get(i);

            if (comparison != null
                    && comparison.isNeedsMicrRepair()) {

                return i;
            }
        }

        for (int i = 0;
                i < start
                && i < comparisons.size();
                i++) {

            MicrComparisonDto comparison =
                    comparisons.get(i);

            if (comparison != null
                    && comparison.isNeedsMicrRepair()) {

                return i;
            }
        }

        return -1;
    }

    private void loadCheque() {

        if (comparisons == null
                || comparisons.isEmpty()
                || chequeIndex < 0
                || chequeIndex >= comparisons.size()) {

            return;
        }

        MicrComparisonDto comparison =
                comparisons.get(chequeIndex);

        if (comparison == null) {
            return;
        }

        if (!comparison.isNeedsMicrRepair()) {

            int nextIndex =
                    findNextRepairIndex(
                            chequeIndex + 1);

            if (nextIndex < 0) {

                showBatchReadyAndOpenDataEntry();
                return;
            }

            chequeIndex = nextIndex;
            comparison = comparisons.get(chequeIndex);
        }

        batchLabel.setValue(
                "Batch ID : " + batchId);

        updateChequeCounter();

        populateNpciData(comparison);
        populateOcrData(comparison);

        loadImages(comparison.getChequeNumber());

        clearOcrErrorStyles();
        applyOcrErrorStyles(comparison);
        configureEditableFields(comparison);

        correctedMicr =
                generateCorrectedMicr();

        previousButton.setDisabled(true);
        nextButton.setDisabled(true);
    }

    private void populateNpciData(
            MicrComparisonDto comparison) {

        chequeNumber.setValue(
                safe(comparison.getChequeNumber()));

        npciCityCode.setValue(
                safe(comparison.getNpciCityCode()));

        npciBankCode.setValue(
                safe(comparison.getNpciBankCode()));

        npciBranchCode.setValue(
                safe(comparison.getNpciBranchCode()));

        npciMicrCode.setValue(
                safe(comparison.getNpciMicrCode()));

        bankName.setValue("");

        chequeNumberImage.setValue(
                safe(comparison.getChequeNumber()));
    }

    private void populateOcrData(
            MicrComparisonDto comparison) {

        ocrCityCode.setValue(
                safe(comparison.getOcrCityCode()));

        ocrBankCode.setValue(
                safe(comparison.getOcrBankCode()));

        ocrBranchCode.setValue(
                safe(comparison.getOcrBranchCode()));

        ocrMicrCode.setValue(
                safe(comparison.getOcrMicrCode()));
    }

    private void configureEditableFields(
            MicrComparisonDto comparison) {

        boolean wholeMicrInvalid =
                !comparison.isNpciMicrFoundInMaster();

        if (wholeMicrInvalid) {

            ocrCityCode.setReadonly(false);
            ocrBankCode.setReadonly(false);
            ocrBranchCode.setReadonly(false);

        } else {

            ocrCityCode.setReadonly(
                    !comparison.isCityCodeMismatch());

            ocrBankCode.setReadonly(
                    !comparison.isBankCodeMismatch());

            ocrBranchCode.setReadonly(
                    !comparison.isBranchCodeMismatch());
        }

        ocrMicrCode.setReadonly(true);
    }

    private void applyOcrErrorStyles(
            MicrComparisonDto comparison) {

        boolean wholeMicrInvalid =
                !comparison.isNpciMicrFoundInMaster();

        if (wholeMicrInvalid) {

            ocrCityCode.setValue("");
            ocrBankCode.setValue("");
            ocrBranchCode.setValue("");

            ocrCityCode.setReadonly(false);
            ocrBankCode.setReadonly(false);
            ocrBranchCode.setReadonly(false);

            ocrCityCode.setSclass(
                    "ocr-field ocr-error");

            ocrBankCode.setSclass(
                    "ocr-field ocr-error");

            ocrBranchCode.setSclass(
                    "ocr-field ocr-error");

            ocrMicrCode.setSclass(
                    "ocr-field ocr-error");

            return;
        }

        if (comparison.isCityCodeMismatch()) {

            ocrCityCode.setValue("");
            ocrCityCode.setReadonly(false);
            ocrCityCode.setSclass(
                    "ocr-field ocr-error");
        }

        if (comparison.isBankCodeMismatch()) {

            ocrBankCode.setValue("");
            ocrBankCode.setReadonly(false);
            ocrBankCode.setSclass(
                    "ocr-field ocr-error");
        }

        if (comparison.isBranchCodeMismatch()) {

            ocrBranchCode.setValue("");
            ocrBranchCode.setReadonly(false);
            ocrBranchCode.setSclass(
                    "ocr-field ocr-error");
        }

        if (comparison.isMicrMismatch()) {

            ocrMicrCode.setSclass(
                    "ocr-field ocr-error");
        }
    }

    private void clearOcrErrorStyles() {

        ocrCityCode.setSclass("ocr-field");
        ocrBankCode.setSclass("ocr-field");
        ocrBranchCode.setSclass("ocr-field");
        ocrMicrCode.setSclass("ocr-field");

        ocrCityCode.setReadonly(true);
        ocrBankCode.setReadonly(true);
        ocrBranchCode.setReadonly(true);
        ocrMicrCode.setReadonly(true);
    }

    private String generateCorrectedMicr() {

        String city =
                safe(ocrCityCode.getValue()).trim();

        String bank =
                safe(ocrBankCode.getValue()).trim();

        String branch =
                safe(ocrBranchCode.getValue()).trim();

        if (!city.matches("\\d{3}")
                || !bank.matches("\\d{3}")
                || !branch.matches("\\d{3}")) {

            return "";
        }

        return city + bank + branch;
    }

    private boolean validateMicrComponents() {

        String city =
                safe(ocrCityCode.getValue()).trim();

        String bank =
                safe(ocrBankCode.getValue()).trim();

        String branch =
                safe(ocrBranchCode.getValue()).trim();

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

    private void showFrontImage() {

        resetImageStyle();

        frontButton.setSclass(
                "image-button image-button-active");

        backButton.setSclass(
                "image-button");

        if (frontImagePath != null
                && !frontImagePath.trim().isEmpty()) {

            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(
                        java.nio.file.Path.of(frontImagePath));
                org.zkoss.image.AImage aImage =
                        new org.zkoss.image.AImage("front.jpg", bytes);
                chequeImage.setContent(aImage);
            } catch (Exception e) {
                e.printStackTrace();
                chequeImage.setContent((org.zkoss.image.AImage) null);
            }

        } else {

            chequeImage.setContent((org.zkoss.image.AImage) null);
        }
    }

    private void showBackImage() {

        resetImageStyle();

        frontButton.setSclass(
                "image-button");

        backButton.setSclass(
                "image-button image-button-active");

        if (backImagePath != null
                && !backImagePath.trim().isEmpty()) {

            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(
                        java.nio.file.Path.of(backImagePath));
                org.zkoss.image.AImage aImage =
                        new org.zkoss.image.AImage("back.jpg", bytes);
                chequeImage.setContent(aImage);
            } catch (Exception e) {
                e.printStackTrace();
                chequeImage.setContent((org.zkoss.image.AImage) null);
            }

        } else {

            chequeImage.setContent((org.zkoss.image.AImage) null);
        }
    }

    private void updateChequeCounter() {

        chequeCounter.setValue(
                "☷  Cheque "
                        + (chequeIndex + 1)
                        + " of "
                        + comparisons.size());
    }

    private void goToPrevious() {

        Messagebox.show(
                "Use Save & Next after completing the current MICR repair.",
                "MICR Repair",
                Messagebox.OK,
                Messagebox.INFORMATION);
    }

    private void goToNext() {

        Messagebox.show(
                "Use Save & Next after completing the current MICR repair.",
                "MICR Repair",
                Messagebox.OK,
                Messagebox.INFORMATION);
    }

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

        MicrComparisonDto comparison =
                comparisons.get(chequeIndex);

        if (comparison == null) {

            Messagebox.show(
                    "Current cheque data is unavailable.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        if (!comparison.isNeedsMicrRepair()) {

            moveAfterSave();
            return;
        }

        if (!validateMicrComponents()) {
            return;
        }

        correctedMicr =
                generateCorrectedMicr();

        if (!correctedMicr.matches("\\d{9}")) {

            Messagebox.show(
                    "Corrected MICR could not be generated.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        String chequeNumberValue =
                safe(comparison.getChequeNumber()).trim();

        if (chequeNumberValue.isEmpty()) {

            Messagebox.show(
                    "Cheque number is missing.",
                    "MICR Repair",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        String originalMicr =
                safe(comparison.getOcrMicrCode()).trim();

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
                        .getAttribute("loggedInUser");

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
                    micrRepairService
                            .saveMicrRepair(
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

    private void moveAfterSave() {

        comparisons =
                micrRepairService
                        .compareBatch(batchId);

        int nextRepairIndex =
                micrRepairService
                        .getNextRepairIndex(batchId);

        if (nextRepairIndex < 0) {

            showBatchReadyAndOpenDataEntry();
            return;
        }

        chequeIndex =
                nextRepairIndex;

        loadCheque();
    }

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
        returnReason.setSelectedItem(null);
        returnRemarks.setValue("");

        List<ReturnReasonDto> reasons =
                micrRepairService
                        .getMakerReturnReasons();

        if (reasons != null) {

            for (ReturnReasonDto reason : reasons) {

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

        MicrComparisonDto comparison =
                comparisons.get(chequeIndex);

        if (comparison == null) {

            Messagebox.show(
                    "Current cheque data is unavailable.",
                    "Return Cheque",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        String chequeNumberValue =
                safe(comparison.getChequeNumber()).trim();

        if (chequeNumberValue.isEmpty()) {

            Messagebox.show(
                    "Cheque number is missing.",
                    "Return Cheque",
                    Messagebox.OK,
                    Messagebox.ERROR);

            return;
        }

        String remarks =
                returnRemarks == null
                        ? ""
                        : returnRemarks.getValue();

        User loggedInUser =
                (User) Executions.getCurrent()
                        .getSession()
                        .getAttribute("loggedInUser");

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
                    micrRepairService
                            .saveMakerReturn(
                                    chequeNumberValue,
                                    returnReasonCode.trim(),
                                    remarks,
                                    userId);

            if (!saved) {

                Messagebox.show(
                        "Unable to save the cheque return.",
                        "Return Cheque",
                        Messagebox.OK,
                        Messagebox.ERROR);

                return;
            }

            returnWindow.setVisible(false);

            Messagebox.show(
                    "Cheque "
                            + chequeNumberValue
                            + " has been marked RETURN_BY_MAKER.",
                    "Return Cheque",
                    Messagebox.OK,
                    Messagebox.INFORMATION,
                    event -> moveAfterReturn());

        } catch (Exception e) {

            Messagebox.show(
                    "Unable to return cheque: "
                            + e.getMessage(),
                    "Return Cheque",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    private void moveAfterReturn() {

        comparisons =
                micrRepairService
                        .compareBatch(batchId);

        int nextRepairIndex =
                micrRepairService
                        .getNextRepairIndex(batchId);

        if (nextRepairIndex < 0) {

            showBatchReadyAndOpenDataEntry();
            return;
        }

        chequeIndex =
                nextRepairIndex;

        loadCheque();
    }

    private void showBatchReadyAndOpenDataEntry() {

        goToDataEntry();
    }

    private void goToDataEntry() {

        Executions.sendRedirect(
                "/zul/inward-maker/data-entry.zul"
                        + "?batchId="
                        + batchId);
    }

    private void goBack() {

        if ("dashboard".equalsIgnoreCase(source)) {

            Executions.sendRedirect(
                    "/zul/inward-maker/dashboard.zul");

        } else {

            Executions.sendRedirect(
                    "/zul/inward-maker/micr-repair-list.zul");
        }
    }

    private void registerEvents() {

        frontButton.addEventListener(
                Events.ON_CLICK,
                event -> showFrontImage());

        backButton.addEventListener(
                Events.ON_CLICK,
                event -> showBackImage());

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

        cancelReturnButton.addEventListener(
                Events.ON_CLICK,
                event -> cancelReturn());

        confirmReturnButton.addEventListener(
                Events.ON_CLICK,
                event -> confirmReturn());

        ocrCityCode.addEventListener(
                Events.ON_CHANGE,
                event -> correctedMicr =
                        generateCorrectedMicr());

        ocrBankCode.addEventListener(
                Events.ON_CHANGE,
                event -> correctedMicr =
                        generateCorrectedMicr());

        ocrBranchCode.addEventListener(
                Events.ON_CHANGE,
                event -> correctedMicr =
                        generateCorrectedMicr());
    }

    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }

    private void resetImageStyle() {

        chequeImage.setStyle(
                "width:100%;"
                        + "height:430px;"
                        + "object-fit:contain;"
                        + "border:1px solid #dce3ee;"
                        + "background:#effdf3;");
    }
}
