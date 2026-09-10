
package com.iispl.cts.controller.outward;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Window;

import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.service.outward.OutwardMakerDataEntryDetailService;

public class OutwardMakerDataEntryDetailController extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    // Header & Meta Labels
    @Wire
    private Label batchIdLabel;

    @Wire
    private Label chequeProgressLabel;

    // Form Input / Display Textboxes
    @Wire
    private Textbox chequeNumberTextbox;

    @Wire
    private Textbox accountNumberTextbox;

    @Wire
    private Textbox drawerNameTextbox;

    @Wire
    private Textbox payeeAccountTextbox;

    @Wire
    private Textbox payeeNameTextbox;

    @Wire
    private Datebox chequeDatebox;

    @Wire
    private Decimalbox amountTextbox;

    @Wire
    private Textbox amountInWordsTextbox;

    // Image Elements
    @Wire
    private Image frontImage;

    @Wire
    private Image backImage;

    // Action & Nav Buttons
    @Wire
    private Button prevButton;

    @Wire
    private Button nextButton;

    @Wire
    private Button frontImageButton;

    @Wire
    private Button backImageButton;

    @Wire
    private Button zoomInButton;

    @Wire
    private Button zoomOutButton;

    @Wire
    private Button rotateButton;

    private OutwardMakerDataEntryDetailService service;
    private List<OutwardCheque> cheques;
    private int currentIndex = 0;
    private String batchId;

    private boolean showingBackImage = false;
    private int zoomLevel = 100;
    private int rotationAngle = 0;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        service = new OutwardMakerDataEntryDetailService();

        batchId = Executions.getCurrent().getParameter("batchId");
        if (batchId == null || batchId.trim().isEmpty()) {
            batchId = "BATCH001";
        }

        cheques = service.getCheques(batchId);

        if (cheques == null || cheques.isEmpty()) {
            Messagebox.show(
                    "No cheque data available for batch " + batchId + ".",
                    "Information",
                    Messagebox.OK,
                    Messagebox.INFORMATION
            );
            return;
        }

        currentIndex = 0;
        loadCheque();
    }

    private void loadCheque() {
        if (cheques == null || cheques.isEmpty()) return;

        OutwardCheque cheque = cheques.get(currentIndex);

        // Reset image transformations
        showingBackImage = false;
        zoomLevel = 100;
        rotationAngle = 0;

        // Set Images
        if (frontImage != null && cheque.getFrontImagePath() != null) {
            frontImage.setSrc(cheque.getFrontImagePath());
        }
        if (backImage != null && cheque.getBackImagePath() != null) {
            backImage.setSrc(cheque.getBackImagePath());
        }
        showFrontImage();

        // 1. Batch & Cheque Counters
        if (batchIdLabel != null) {
            batchIdLabel.setValue(cheque.getBatchNumber());
        }
        if (chequeProgressLabel != null) {
            chequeProgressLabel.setValue("Cheque " + (currentIndex + 1) + " of " + cheques.size());
        }

        // 2. Cheque Number
        if (chequeNumberTextbox != null) {
            chequeNumberTextbox.setValue(cheque.getChequeNumber() != null ? cheque.getChequeNumber() : "");
        }

        // 3. Drawer Details (Account & Name)
        if (accountNumberTextbox != null) {
            accountNumberTextbox.setValue(cheque.getDrawerAccountNumber() != null ? cheque.getDrawerAccountNumber() : "");
        }
        if (drawerNameTextbox != null) {
            drawerNameTextbox.setValue(cheque.getDrawerName() != null ? cheque.getDrawerName() : "");
        }

        // 4. Payee Details (Account & Name)
        if (payeeAccountTextbox != null) {
            payeeAccountTextbox.setValue(cheque.getDepositorAccountNumber() != null ? cheque.getDepositorAccountNumber() : "");
        }
        if (payeeNameTextbox != null) {
            payeeNameTextbox.setValue(cheque.getPayeeName() != null ? cheque.getPayeeName() : "");
        }

        // 5. Cheque Date
        /*
         * Cheque Date (Convert LocalDate to Date for Datebox)
         */
        if (chequeDatebox != null) {
            if (cheque.getChequeDate() != null) {
                Date date = Date.from(cheque.getChequeDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
                chequeDatebox.setValue(date);
            } else {
                chequeDatebox.setValue(null);
            }
        }

        // 6. Amount (Direct BigDecimal)
        if (amountTextbox != null) {
            amountTextbox.setValue(cheque.getAmount() != null ? cheque.getAmount() : BigDecimal.ZERO);
        }

        // 7. Amount In Words
        if (amountInWordsTextbox != null) {
            amountInWordsTextbox.setValue(cheque.getAmountInWords() != null ? cheque.getAmountInWords() : "");
        }

        updateButtons();
    }

    private void updateButtons() {
        if (prevButton != null) {
            prevButton.setDisabled(currentIndex == 0);
        }
        if (nextButton != null) {
            nextButton.setDisabled(currentIndex >= cheques.size() - 1);
        }
    }

    @Listen("onClick = #prevButton")
    public void previousCheque() {
        if (currentIndex > 0) {
            currentIndex--;
            loadCheque();
        }
    }

    @Listen("onClick = #nextButton")
    public void nextCheque() {
        if (currentIndex < cheques.size() - 1) {
            currentIndex++;
            loadCheque();
        }
    }

    @Listen("onClick = #saveNextButton")
    public void saveAndNext() {
        OutwardCheque cheque = cheques.get(currentIndex);

        String account = accountNumberTextbox != null ? accountNumberTextbox.getValue() : null;
        String drawerName = drawerNameTextbox != null ? drawerNameTextbox.getValue() : null;
        String payeeName = payeeNameTextbox != null ? payeeNameTextbox.getValue() : null;
        java.util.Date selectedDate = chequeDatebox != null ? chequeDatebox.getValue() : null;
        BigDecimal amount = amountTextbox != null ? amountTextbox.getValue() : null;
        String amountWords = amountInWordsTextbox != null ? amountInWordsTextbox.getValue() : null;

        // 1. Validations
        if (account == null || account.trim().isEmpty()) {
            Messagebox.show("Please enter Account Number.", "Validation", Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }

        if (selectedDate == null) {
            Messagebox.show("Please select a Cheque Date.", "Validation", Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }

        if (amount == null) {
            Messagebox.show("Please enter Amount.", "Validation", Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }

        // 2. Set Model Values
        cheque.setDrawerAccountNumber(account.trim());

        if (drawerName != null) {
            cheque.setDrawerName(drawerName.trim());
        }
        if (payeeName != null) {
            cheque.setPayeeName(payeeName.trim());
        }
        if (amountWords != null) {
            cheque.setAmountInWords(amountWords.trim());
        }
        cheque.setAmount(amount);

        // Convert java.util.Date to java.time.LocalDate
        LocalDate chequeDate = selectedDate.toInstant()
                                          .atZone(java.time.ZoneId.systemDefault())
                                          .toLocalDate();
        cheque.setChequeDate(chequeDate);

        // 3. Save to Database
        service.saveCheque(cheque);

        // 4. Move to Next Cheque or Finish
        if (currentIndex < cheques.size() - 1) {
            currentIndex++;
            loadCheque();
            
        } else {
        	int currentUserId = LoginController.getCurrentUserId();
         
            boolean completed = service.completeBatchDataEntry(batchId, currentUserId);

            if (completed) {
                Messagebox.show(
                        "All cheques in batch " + batchId + " have been completed and moved to Send to Checker.",
                        "Batch Completed",
                        Messagebox.OK,
                        Messagebox.INFORMATION,
                        event -> Executions.sendRedirect("outward-maker-data-entry.zul")
                );
            } else {
                Messagebox.show(
                        "Cheque saved, but failed to update batch status to READY_TO_SUBMIT.",
                        "Warning",
                        Messagebox.OK,
                        Messagebox.ERROR
                );
            }
        }
      
    }
    @Listen("onClick = #rejectButton")
    public void rejectCheque() {
        Window rejectWindow = new Window();
        rejectWindow.setTitle("Reject Cheque");
        rejectWindow.setWidth("450px");
        rejectWindow.setClosable(true);
        rejectWindow.setBorder("normal");
        rejectWindow.setParent(
                Executions.getCurrent().getDesktop().getFirstPage().getFirstRoot()
        );

        Vlayout layout = new Vlayout();
        layout.setSpacing("12px");
        layout.setStyle("padding:20px;");

        Label label = new Label("Enter rejection reason:");
        Textbox reasonBox = new Textbox();
        reasonBox.setRows(4);
        reasonBox.setWidth("100%");

        Button confirm = new Button("Confirm Reject");
        confirm.setWidth("140px");

        confirm.addEventListener("onClick", event -> {
            String reason = reasonBox.getValue();
            if (reason == null || reason.trim().isEmpty()) {
                Messagebox.show("Please enter rejection reason.", "Validation", Messagebox.OK, Messagebox.EXCLAMATION);
                return;
            }

            OutwardCheque cheque = cheques.get(currentIndex);
            service.rejectCheque(cheque, reason.trim());

            rejectWindow.detach();

            Messagebox.show(
                    "Cheque " + (currentIndex + 1) + " rejected and sent back to Maker.",
                    "Rejected",
                    Messagebox.OK,
                    Messagebox.INFORMATION,
                    e -> Executions.sendRedirect("outward-maker-data-entry.zul")
            );
        });

        layout.appendChild(label);
        layout.appendChild(reasonBox);
        layout.appendChild(confirm);
        rejectWindow.appendChild(layout);

        rejectWindow.doModal();
    }

    @Listen("onClick = #backButton")
    public void backToList() {
        Executions.sendRedirect("outward-maker-data-entry.zul");
    }

    private void showFrontImage() {
        showingBackImage = false;
        zoomLevel = 100;
        rotationAngle = 0;
        if (frontImage != null) frontImage.setVisible(true);
        if (backImage != null) backImage.setVisible(false);
        applyZoom();
    }

    private void showBackImage() {
        showingBackImage = true;
        zoomLevel = 100;
        rotationAngle = 0;
        if (frontImage != null) frontImage.setVisible(false);
        if (backImage != null) backImage.setVisible(true);
        applyZoom();
    }

    private void applyZoom() {
        Image currentImage = showingBackImage ? backImage : frontImage;
        if (currentImage == null) return;

        currentImage.setStyle(
                "object-fit:contain;"
                + "transform:scale(" + (zoomLevel / 100.0) + ") rotate(" + rotationAngle + "deg);"
                + "transform-origin:center center;"
                + "transition:transform 0.2s ease;"
        );
    }

    @Listen("onClick = #frontImageButton")
    public void frontImage() {
        showFrontImage();
    }

    @Listen("onClick = #backImageButton")
    public void backImage() {
        showBackImage();
    }

    @Listen("onClick = #zoomInButton")
    public void zoomIn() {
        if (zoomLevel < 200) {
            zoomLevel += 20;
            applyZoom();
        }
    }

    @Listen("onClick = #zoomOutButton")
    public void zoomOut() {
        if (zoomLevel > 60) {
            zoomLevel -= 20;
            applyZoom();
        }
    }

    @Listen("onClick = #rotateButton")
    public void rotateImage() {
        rotationAngle = (rotationAngle + 90) % 360;
        applyZoom();
    }
}








