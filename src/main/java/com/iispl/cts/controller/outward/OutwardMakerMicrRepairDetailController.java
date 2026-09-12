package com.iispl.cts.controller.outward;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.InputEvent;
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

public class OutwardMakerMicrRepairDetailController extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    // UI Components - Header & Progress
    @Wire private Label batchIdLabel;
    @Wire private Label chequeProgressLabel;
    @Wire private Label currentStatusLabel;

    // UI Components - Metric Pills
    @Wire private Label totalPillLabel;
    @Wire private Label completedPillLabel;
    @Wire private Label rejectedPillLabel;
    @Wire private Label pendingPillLabel;

    // UI Components - Cheque Viewer
    @Wire private Image frontImage;
    @Wire private Image backImage;
    @Wire private Button frontImageButton;
    @Wire private Button backImageButton;
    @Wire private Button zoomInButton;
    @Wire private Button zoomOutButton;
    @Wire private Button rotateButton;

    // UI Components - Form Inputs
    @Wire private Textbox chequeNumberTextbox;
    @Wire private Textbox cityCodeTextbox;
    @Wire private Textbox bankCodeTextbox;
    @Wire private Textbox branchCodeTextbox;

    // UI Components - Navigation & Action Controls
    @Wire private Button prevButton;
    @Wire private Button saveNextButton;
    @Wire private Button btnBackToQueue; 

    // State Variables
    private List<OutwardCheque> cheques;
    private int currentIndex = 0;
    private int totalChequesCount = 0;
    private String batchNumber;
    private double currentScale = 1.0;
    private int currentRotation = 0;

    // Service Dependencies
    private OutwardMakerMicrRepairDetailService service;
    private OutwardValidationService validationService;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        service = new OutwardMakerMicrRepairDetailService();
        validationService = new OutwardValidationService();

        batchNumber = Executions.getCurrent().getParameter("batchNumber");

        if (batchNumber == null || batchNumber.trim().isEmpty()) {
            Messagebox.show("Batch number is missing.", "Error", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        batchNumber = batchNumber.trim();
        if (batchIdLabel != null) {
            batchIdLabel.setValue(batchNumber);
        }

        loadCheques();
    }

    private void loadCheques() {
        cheques = service.getMicrErrorCheques(batchNumber);

        if (cheques == null || cheques.isEmpty()) {
            Messagebox.show("No MICR error cheques found for this batch.", "Information", Messagebox.OK, Messagebox.INFORMATION);
            return;
        }

        totalChequesCount = cheques.size(); // Preserves true batch count
        currentIndex = 0;

        updateHeaderPillMetrics();
        loadCurrentCheque();
    }

    private void updateHeaderPillMetrics() {
        if (totalPillLabel != null) totalPillLabel.setValue(String.valueOf(totalChequesCount));
        if (completedPillLabel != null) completedPillLabel.setValue(String.valueOf(currentIndex));
        if (pendingPillLabel != null) pendingPillLabel.setValue(String.valueOf(totalChequesCount - currentIndex));
    }

    private void loadCurrentCheque() {
        if (cheques == null || cheques.isEmpty()) {
            return;
        }

        // Strictly enforce bounds
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        if (currentIndex >= cheques.size()) {
            currentIndex = cheques.size() - 1;
        }

        // Force updates for top metrics & Cheque Progress (e.g. "Cheque 1 of 5")
        updateHeaderPillMetrics();
        if (chequeProgressLabel != null) {
            chequeProgressLabel.setValue("Cheque " + (currentIndex + 1) + " of " + totalChequesCount);
        }

        // EXPLICITLY ENABLE/DISABLE PREV BUTTON
        if (prevButton != null) {
            prevButton.setDisabled(currentIndex == 0); // Enabled on index > 0
        }

        OutwardCheque cheque = cheques.get(currentIndex);

        // Set Cheque Number
        if (chequeNumberTextbox != null) {
            chequeNumberTextbox.setValue(safe(cheque.getChequeNumber()));
        }

        // Validate MICR components for visual state highlighting
        boolean cityValid = validationService.isValidMicrCode(cheque.getCityCode());
        boolean bankValid = validationService.isValidMicrCode(cheque.getBankCode());
        boolean branchValid = validationService.isValidMicrCode(cheque.getBranchCode());

        if (cityCodeTextbox != null) {
            cityCodeTextbox.setSclass(cityValid ? "micr-component-corrected" : "micr-component-error");
            cityCodeTextbox.setValue(safe(cheque.getCityCode()));
        }
        if (bankCodeTextbox != null) {
            bankCodeTextbox.setSclass(bankValid ? "micr-component-corrected" : "micr-component-error");
            bankCodeTextbox.setValue(safe(cheque.getBankCode()));
        }
        if (branchCodeTextbox != null) {
            branchCodeTextbox.setSclass(branchValid ? "micr-component-corrected" : "micr-component-error");
            branchCodeTextbox.setValue(safe(cheque.getBranchCode()));
        }

        loadImages(cheque);
        resetImageTransformations();
        
        // Pass initial state boolean to status label on load
        updateStatusLabel(cityValid && bankValid && branchValid);

        if (saveNextButton != null) {
            saveNextButton.setDisabled(false);
        }
    }

    private void loadImages(OutwardCheque cheque) {
        if (cheque == null) {
            return;
        }

        String frontPath = cheque.getFrontImagePath();
        String backPath = cheque.getBackImagePath();

        if (frontImage != null) {
            if (frontPath != null && !frontPath.trim().isEmpty()) {
                frontImage.setSrc(frontPath.trim());
            } else {
                frontImage.setSrc(null); // Clear image if path is missing
            }
        }

        if (backImage != null) {
            if (backPath != null && !backPath.trim().isEmpty()) {
                backImage.setSrc(backPath.trim());
            } else {
                backImage.setSrc(null); // Clear image if path is missing
            }
        }

        showFrontImage();
    }

    @Listen("onClick = #frontImageButton")
    public void showFrontImage() {
        if (frontImage != null) frontImage.setVisible(true);
        if (backImage != null) backImage.setVisible(false);
    }

    @Listen("onClick = #backImageButton")
    public void showBackImage() {
        if (frontImage != null) frontImage.setVisible(false);
        if (backImage != null) backImage.setVisible(true);
    }

    @Listen("onClick = #zoomInButton")
    public void zoomIn() {
        currentScale += 0.2;
        applyImageStyle();
    }

    @Listen("onClick = #zoomOutButton")
    public void zoomOut() {
        if (currentScale > 0.4) {
            currentScale -= 0.2;
            applyImageStyle();
        }
    }

    @Listen("onClick = #rotateButton")
    public void rotateImage() {
        currentRotation = (currentRotation + 90) % 360;
        applyImageStyle();
    }

    private void resetImageTransformations() {
        currentScale = 1.0;
        currentRotation = 0;
        applyImageStyle();
    }

    private void applyImageStyle() {
        String transformStyle = String.format("transform: scale(%.2f) rotate(%ddeg);", currentScale, currentRotation);
        if (frontImage != null) frontImage.setStyle(transformStyle);
        if (backImage != null) backImage.setStyle(transformStyle);
    }

    @Listen("onChange = #cityCodeTextbox, #bankCodeTextbox, #branchCodeTextbox; "
            + "onChanging = #cityCodeTextbox, #bankCodeTextbox, #branchCodeTextbox")
    public void checkMicrFields(Event event) {
        String cityCode = cityCodeTextbox != null ? cityCodeTextbox.getValue() : "";
        String bankCode = bankCodeTextbox != null ? bankCodeTextbox.getValue() : "";
        String branchCode = branchCodeTextbox != null ? branchCodeTextbox.getValue() : "";

        // Extract live value during onChanging keystrokes
        if (event instanceof InputEvent) {
            InputEvent inputEvent = (InputEvent) event;
            Component target = event.getTarget();

            if (target == cityCodeTextbox) cityCode = inputEvent.getValue();
            else if (target == bankCodeTextbox) bankCode = inputEvent.getValue();
            else if (target == branchCodeTextbox) branchCode = inputEvent.getValue();
        }

        boolean isCityValid = validationService.isValidMicrCode(cityCode);
        boolean isBankValid = validationService.isValidMicrCode(bankCode);
        boolean isBranchValid = validationService.isValidMicrCode(branchCode);

        // Apply textbox colors
        if (cityCodeTextbox != null) cityCodeTextbox.setSclass(isCityValid ? "micr-component-corrected" : "micr-component-error");
        if (bankCodeTextbox != null) bankCodeTextbox.setSclass(isBankValid ? "micr-component-corrected" : "micr-component-error");
        if (branchCodeTextbox != null) branchCodeTextbox.setSclass(isBranchValid ? "micr-component-corrected" : "micr-component-error");

        // Update status label instantly using the live evaluated boolean
        boolean isAllValid = isCityValid && isBankValid && isBranchValid;
        updateStatusLabel(isAllValid);
    }

    // Your single updateStatusLabel method accepting the live boolean flag
    private void updateStatusLabel(boolean isAllValid) {
        if (currentStatusLabel != null) {
            if (isAllValid) {
                currentStatusLabel.setValue("MICR_REPAIRED");
                currentStatusLabel.setSclass("status-label status-repaired");
            } else {
                currentStatusLabel.setValue("MICR_ERROR");
                currentStatusLabel.setSclass("status-label status-error");
            }
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Listen("onClick = #saveNextButton")
    public void saveAndNext() {
        if (cheques == null || cheques.isEmpty()) return;

        OutwardCheque cheque = cheques.get(currentIndex);
        String cityCode = cityCodeTextbox != null ? cityCodeTextbox.getValue().trim() : "";
        String bankCode = bankCodeTextbox != null ? bankCodeTextbox.getValue().trim() : "";
        String branchCode = branchCodeTextbox != null ? branchCodeTextbox.getValue().trim() : "";

        // Check if the user made changes in the UI relative to the original cheque values
        boolean isModified = !cityCode.equals(cheque.getCityCode())
                          || !bankCode.equals(cheque.getBankCode())
                          || !branchCode.equals(cheque.getBranchCode());

        // ONLY save if modifications were actually made
        if (isModified) {
            // 1. Validation check before hitting DB
            if (!validationService.isValidMicrCode(cityCode)
                    || !validationService.isValidMicrCode(bankCode)
                    || !validationService.isValidMicrCode(branchCode)) {
                Messagebox.show("Please enter valid 3-digit MICR codes for City, Bank, and Branch.", 
                                "MICR Validation Error", Messagebox.OK, Messagebox.EXCLAMATION);
                return; // Block navigation only if they typed invalid edits
            }

            // 2. Database update execution
            boolean updated = service.updateCorrectedMicr(batchNumber, cheque.getChequeNumber(), cityCode, bankCode, branchCode);
            
            // 3. Database error alert handling
            if (!updated) {
                Messagebox.show("Database Alert: MICR repair could not be saved to the database. Please check DB connection or logs.", 
                                "Database Error Alert", Messagebox.OK, Messagebox.ERROR);
                return; // Stop navigation execution on DB failure
            }

            // 4. Update in-memory cheque state
            cheque.setCityCode(cityCode);
            cheque.setBankCode(bankCode);
            cheque.setBranchCode(branchCode);
            cheque.setChequeStatus("MICR_REPAIRED");
        }

        // 5. Navigate to next cheque or complete batch (runs whether modified or skipped)
        if (currentIndex < cheques.size() - 1) {
            currentIndex++;
            loadCurrentCheque();
        } else {
            boolean remaining = service.hasRemainingMicrErrors(batchNumber);
            if (!remaining) {
                service.updateBatchStatus(batchNumber);
            }
            Messagebox.show("MICR Repair Completed for all cheques in this batch.", 
                            "Success", Messagebox.OK, Messagebox.INFORMATION,
                    e -> Executions.getCurrent().sendRedirect("outward-maker-micr-repair.zul"));
        }
    }
    
    @Listen("onClick = #prevButton")
    public void previousCheque() {
        if (cheques == null || cheques.isEmpty()) return;

        if (currentIndex > 0) {
            currentIndex--;
            loadCurrentCheque();
        } else {
            Messagebox.show("This is the first cheque.", "Information", Messagebox.OK, Messagebox.INFORMATION);
        }
    }
    
 // ADD THIS METHOD HERE AT THE BOTTOM
    @Listen("onClick = #btnBackToQueue")
    public void backToMicrQueue() {
        Executions.getCurrent().sendRedirect("outward-maker-micr-repair.zul");
    }
}