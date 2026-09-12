package com.iispl.cts.controller.outward;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.service.outward.OutwardMakerDataEntryDetailService;

public class OutwardMakerDataEntryDetailController extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    // Header & Summary Strip
    @Wire
    private Label batchIdLabel;

    @Wire
    private Label chequeProgressLabel;

    @Wire
    private Label totalChequesLabel;

    @Wire
    private Label completedChequesLabel;

    @Wire
    private Label rejectedChequesLabel;

    @Wire
    private Label pendingChequesLabel;

    // Form Inputs
    @Wire
    private Textbox chequeNumberTextbox;

    @Wire
    private Textbox accountNumberTextbox;

    @Wire
    private Datebox chequeDatebox;

    @Wire
    private Textbox drawerNameTextbox;

    @Wire
    private Textbox payeeNameTextbox;

    @Wire
    private Decimalbox amountTextbox;

    @Wire
    private Textbox amountInWordsTextbox;

    // Cheque Image Elements
    @Wire
    private Image frontImage;

    @Wire
    private Image backImage;

    // Toolbar & Main Action Buttons
    @Wire
    private Button backToListButton;

    @Wire
    private Button toggleImageSideButton;

    @Wire
    private Button zoomInButton;

    @Wire
    private Button zoomOutButton;

    @Wire
    private Button rotateButton;

    @Wire
    private Button prevButton;

    @Wire
    private Button rejectRequestButton;

    @Wire
    private Button saveNextButton;

    // Modal Components
    @Wire
    private Window rejectModalWindow;

    @Wire
    private Combobox modalRejectReasonCombobox;

    @Wire
    private Textbox modalRejectRemarksTextbox;

    @Wire
    private Button cancelRejectModalButton;

    @Wire
    private Button confirmRejectModalButton;

    // Service & State
    private OutwardMakerDataEntryDetailService service;
    private List<OutwardCheque> cheques;
    private int currentIndex = 0;
    private String batchId;

    // Image Viewport State
    private boolean isShowingFront = true;
    private int zoomLevel = 100;
    private int rotationAngle = 0;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        service = new OutwardMakerDataEntryDetailService();

        batchId = Executions.getCurrent().getParameter("batchId");
        if (batchId == null || batchId.trim().isEmpty()) {
            batchId = (String) Sessions.getCurrent().getAttribute("selectedBatchId");
        }
        if (batchId == null || batchId.trim().isEmpty()) {
            batchId = "BATCH001";
        }

        cheques = service.getCheques(batchId);

        if (cheques == null || cheques.isEmpty()) {
            Messagebox.show(
                "No cheque data available for batch " + batchId + ".",
                "Information",
                Messagebox.OK,
                Messagebox.INFORMATION,
                e -> Executions.sendRedirect("outward-maker-data-entry.zul")
            );
            return;
        }

        // Check if all instruments are already verified
        boolean allVerified = true;
        for (OutwardCheque chq : cheques) {
            String status = chq.getChequeStatus();
            if (!("VERIFIED".equalsIgnoreCase(status) 
                    || "COMPLETED".equalsIgnoreCase(status) 
                    || "REJECT_REQUESTED".equalsIgnoreCase(status))) {
                allVerified = false;
                break;
            }
        }

        if (allVerified) {
            currentIndex = cheques.size() - 1; // Land on last cheque
            updateBatchSummaryMetrics();
            loadRejectReasonsIntoModal();
            loadCheque();

            Messagebox.show(
                "All cheques in batch " + batchId + " are already verified. Proceed to Send to Checker?",
                "Batch Already Processed",
                Messagebox.YES | Messagebox.NO,
                Messagebox.QUESTION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {
                        Executions.sendRedirect("outward-maker-send-to-checker.zul");
                    }
                }
            );
            return;
        }

        currentIndex = findFirstPendingChequeIndex();
        updateBatchSummaryMetrics();
        loadRejectReasonsIntoModal();
        loadCheque();
    }

    private void loadCheque() {
        if (cheques == null || cheques.isEmpty()) return;

        OutwardCheque cheque = cheques.get(currentIndex);

        // Reset image transforms & toggle state
        isShowingFront = true;
        zoomLevel = 100;
        rotationAngle = 0;

        if (toggleImageSideButton != null) {
            toggleImageSideButton.setLabel("View Back");
        }

        // Set Images
        if (frontImage != null && cheque.getFrontImagePath() != null) {
            frontImage.setSrc(cheque.getFrontImagePath().trim());
        }
        if (backImage != null && cheque.getBackImagePath() != null) {
            backImage.setSrc(cheque.getBackImagePath().trim());
        }

        applyImageVisibilityAndTransform();

        // 1. Batch & Cheque Progress
        if (batchIdLabel != null) {
            batchIdLabel.setValue(cheque.getBatchNumber());
        }
        if (chequeProgressLabel != null) {
            chequeProgressLabel.setValue("Cheque " + (currentIndex + 1) + " of " + cheques.size());
        }

        // 2. Instrument Identifiers
        if (chequeNumberTextbox != null) {
            chequeNumberTextbox.setValue(cheque.getChequeNumber() != null ? cheque.getChequeNumber() : "");
        }
        if (accountNumberTextbox != null) {
            accountNumberTextbox.setValue(cheque.getDrawerAccountNumber() != null ? cheque.getDrawerAccountNumber() : "");
        }

        // 3. Cheque Date (Convert LocalDate to java.util.Date)
        if (chequeDatebox != null) {
            if (cheque.getChequeDate() != null) {
                Date date = Date.from(cheque.getChequeDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
                chequeDatebox.setValue(date);
            } else {
                chequeDatebox.setValue(null);
            }
        }

        // 4. Names
        if (drawerNameTextbox != null) {
            drawerNameTextbox.setValue(cheque.getDrawerName() != null ? cheque.getDrawerName() : "");
        }
        if (payeeNameTextbox != null) {
            payeeNameTextbox.setValue(cheque.getPayeeName() != null ? cheque.getPayeeName() : "");
        }

        // 5. Amount
        if (amountTextbox != null) {
            amountTextbox.setValue(cheque.getAmount() != null ? cheque.getAmount() : BigDecimal.ZERO);
        }
        if (amountInWordsTextbox != null) {
            amountInWordsTextbox.setValue(cheque.getAmountInWords() != null ? cheque.getAmountInWords() : "");
        }

        updateNavButtons();
    }

    private void updateNavButtons() {
        if (prevButton != null) {
            boolean isFirst = (currentIndex == 0);
            prevButton.setDisabled(isFirst);
            if (isFirst) {
                prevButton.setStyle("background:#CBD5E1 !important; color:#94A3B8 !important; border:none !important; font-size:12px !important; font-weight:600 !important; border-radius:6px !important; cursor:not-allowed !important;");
            } else {
                prevButton.setStyle("background:#2563EB !important; color:#FFFFFF !important; border:none !important; font-size:12px !important; font-weight:600 !important; border-radius:6px !important; cursor:pointer !important;");
            }
        }
    }

    @Listen("onClick = #prevButton")
    public void previousCheque() {
        if (currentIndex > 0) {
            currentIndex--;
            loadCheque();
        }
    }

    @Listen("onClick = #saveNextButton")
    public void saveAndNext() {
        OutwardCheque cheque = cheques.get(currentIndex);

        String chqNo = chequeNumberTextbox != null ? chequeNumberTextbox.getValue() : null;
        String account = accountNumberTextbox != null ? accountNumberTextbox.getValue() : null;
        Date selectedDate = chequeDatebox != null ? chequeDatebox.getValue() : null;
        String drawerName = drawerNameTextbox != null ? drawerNameTextbox.getValue() : null;
        String payeeName = payeeNameTextbox != null ? payeeNameTextbox.getValue() : null;
        BigDecimal amount = amountTextbox != null ? amountTextbox.getValue() : null;
        String amountWords = amountInWordsTextbox != null ? amountInWordsTextbox.getValue() : null;

        // Validation Checks
        if (chqNo == null || chqNo.trim().isEmpty()) {
            Messagebox.show("Please enter Cheque Number.", "Validation", Messagebox.OK, Messagebox.EXCLAMATION);
            if (chequeNumberTextbox != null) chequeNumberTextbox.setFocus(true);
            return;
        }

        if (account == null || account.trim().isEmpty()) {
            Messagebox.show("Please enter Account Number.", "Validation", Messagebox.OK, Messagebox.EXCLAMATION);
            if (accountNumberTextbox != null) accountNumberTextbox.setFocus(true);
            return;
        }

        if (selectedDate == null) {
            Messagebox.show("Please select a Cheque Date.", "Validation", Messagebox.OK, Messagebox.EXCLAMATION);
            if (chequeDatebox != null) chequeDatebox.setFocus(true);
            return;
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            Messagebox.show("Please enter a valid Amount.", "Validation", Messagebox.OK, Messagebox.EXCLAMATION);
            if (amountTextbox != null) amountTextbox.setFocus(true);
            return;
        }

        // Model Updates
        cheque.setChequeNumber(chqNo.trim());
        cheque.setDrawerAccountNumber(account.trim());

        LocalDate chequeDate = selectedDate.toInstant()
                                          .atZone(ZoneId.systemDefault())
                                          .toLocalDate();
        cheque.setChequeDate(chequeDate);

        if (drawerName != null) cheque.setDrawerName(drawerName.trim());
        if (payeeName != null) cheque.setPayeeName(payeeName.trim());
        if (amountWords != null) cheque.setAmountInWords(amountWords.trim());
        cheque.setAmount(amount);

        // Mark in-memory status
        cheque.setChequeStatus("VERIFIED");

        // Safely extract maker ID (supports Integer, Long, String, or null)
        Object sessionUserIdObj = Sessions.getCurrent().getAttribute("userId");
        int makerId = (sessionUserIdObj instanceof Number) ? ((Number) sessionUserIdObj).intValue() : 1;

        // Persists cheque fields and records maker action 'VERIFY'
        service.saveAndVerifyCheque(cheque, makerId);

        // Sync Metrics Strip
        updateBatchSummaryMetrics();

        // Advance to next cheque or wrap batch
        handleNextOrComplete();
    }

    // =========================================================
    // MODAL REJECTION WORKFLOW
    // =========================================================

    /**
     * Open the Rejection Modal Window when user clicks '⚠ Reject Request'
     */
    /**
     * Open the Rejection Modal Window when user clicks '⚠ Reject Request'
     */
    @Listen("onClick = #rejectRequestButton")
    public void onOpenRejectModal() {
        if (rejectModalWindow != null) {
            // Populate/refresh items right at modal launch time
            loadRejectReasonsIntoModal();

            if (modalRejectReasonCombobox != null) {
                modalRejectReasonCombobox.setSelectedIndex(-1);
            }
            if (modalRejectRemarksTextbox != null) {
                modalRejectRemarksTextbox.setValue("");
            }

            rejectModalWindow.setVisible(true);
            rejectModalWindow.doModal();
        }
    }

    @Listen("onClick = #cancelRejectModalButton; onClose = #rejectModalWindow")
    public void onCancelRejectModal() {
        if (rejectModalWindow != null) {
            rejectModalWindow.setVisible(false);
        }
    }

    /**
     * Submit Rejection from the Modal
     */
    /**
     * Submit Rejection from the Modal
     */
    @Listen("onConfirmRejectModal = #rejectModalWindow; onClick = #confirmRejectModalButton")
    public void onConfirmRejectModal() {
        OutwardCheque currentCheque = cheques.get(currentIndex);

        Comboitem selectedItem = modalRejectReasonCombobox != null ? modalRejectReasonCombobox.getSelectedItem() : null;
        if (selectedItem == null || selectedItem.getValue() == null) {
            Messagebox.show(
                "Please select a rejection reason before proceeding.", 
                "Validation", 
                Messagebox.OK, 
                Messagebox.EXCLAMATION
            );
            if (modalRejectReasonCombobox != null) modalRejectReasonCombobox.setFocus(true);
            return;
        }

        // Fix: reason_code is a VARCHAR String, not a Number
        String reasonCode = selectedItem.getValue().toString();

        Object sessionUserIdObj = Sessions.getCurrent().getAttribute("userId");
        int makerId = (sessionUserIdObj instanceof Number) ? ((Number) sessionUserIdObj).intValue() : 1;

        boolean success = service.recordMakerReject(
            currentCheque.getBatchNumber(),
            currentCheque.getChequeNumber(),
            makerId,
            reasonCode // ensure service accepts String
        );

        if (success) {
            if (rejectModalWindow != null) {
                rejectModalWindow.setVisible(false);
            }

            currentCheque.setChequeStatus("REJECT_REQUESTED");
            updateBatchSummaryMetrics();
            handleNextOrComplete();
        } else {
            Messagebox.show("Failed to record rejection request.", "Error", Messagebox.OK, Messagebox.ERROR);
        }
    }

    private void handleNextOrComplete() {
        if (currentIndex < cheques.size() - 1) {
            currentIndex++;
            loadCheque();
        } else {
            int firstPending = findFirstPendingChequeIndex();
            boolean hasPending = false;

            for (OutwardCheque chq : cheques) {
                String status = chq.getChequeStatus();
                boolean isDone = "VERIFIED".equalsIgnoreCase(status)
                              || "COMPLETED".equalsIgnoreCase(status)
                              || "REJECT_REQUESTED".equalsIgnoreCase(status);
                if (!isDone) {
                    hasPending = true;
                    break;
                }
            }

            if (hasPending) {
                Messagebox.show(
                    "You have reached the end of the batch, but some cheques remain pending. Jumping to pending cheque.",
                    "Pending Cheques Found",
                    Messagebox.OK,
                    Messagebox.INFORMATION
                );
                currentIndex = firstPending;
                loadCheque();
            } else {
                Object sessionUserIdObj = Sessions.getCurrent().getAttribute("userId");
                int currentUserId = (sessionUserIdObj instanceof Number) ? ((Number) sessionUserIdObj).intValue() : 1;

                boolean completed = service.completeBatchDataEntry(batchId, currentUserId);

                if (completed) {
                    Messagebox.show(
                        "All cheques in batch " + batchId + " have been processed and moved to Send to Checker.",
                        "Batch Completed",
                        Messagebox.OK,
                        Messagebox.INFORMATION,
                        event -> Executions.sendRedirect("outward-maker-send-to-checker.zul")
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
    }

    private void updateBatchSummaryMetrics() {
        if (cheques == null || cheques.isEmpty()) {
            if (totalChequesLabel != null) totalChequesLabel.setValue("0");
            if (completedChequesLabel != null) completedChequesLabel.setValue("0");
            if (rejectedChequesLabel != null) rejectedChequesLabel.setValue("0");
            if (pendingChequesLabel != null) pendingChequesLabel.setValue("0");
            return;
        }

        int total = cheques.size();
        int completed = 0;
        int rejectRequests = 0;

        for (OutwardCheque chq : cheques) {
            String status = chq.getChequeStatus();
            if ("VERIFIED".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
                completed++;
            } else if ("REJECT_REQUESTED".equalsIgnoreCase(status)) {
                rejectRequests++;
            }
        }

        int pending = total - (completed + rejectRequests);

        if (totalChequesLabel != null) totalChequesLabel.setValue(String.valueOf(total));
        if (completedChequesLabel != null) completedChequesLabel.setValue(String.valueOf(completed));
        if (rejectedChequesLabel != null) rejectedChequesLabel.setValue(String.valueOf(rejectRequests));
        if (pendingChequesLabel != null) pendingChequesLabel.setValue(String.valueOf(pending));
    }

    private int findFirstPendingChequeIndex() {
        for (int i = 0; i < cheques.size(); i++) {
            OutwardCheque chq = cheques.get(i);
            String status = chq.getChequeStatus();
            boolean isHandled = "VERIFIED".equalsIgnoreCase(status)
                             || "COMPLETED".equalsIgnoreCase(status)
                             || "REJECT_REQUESTED".equalsIgnoreCase(status);
            if (!isHandled) {
                return i;
            }
        }
        return 0;
    }

    @Listen("onClick = #backToListButton")
    public void onBackToList() {
        Executions.sendRedirect("outward-maker-data-entry.zul");
    }

    // =========================================================
    // IMAGE TOOLBAR & VIEWPORT ACTIONS
    // =========================================================

    @Listen("onClick = #toggleImageSideButton")
    public void onToggleImageSide() {
        isShowingFront = !isShowingFront;

        if (toggleImageSideButton != null) {
            toggleImageSideButton.setLabel(isShowingFront ? "View Back" : "View Front");
        }

        applyImageVisibilityAndTransform();
    }

    private void applyImageVisibilityAndTransform() {
        double scale = zoomLevel / 100.0;
        String transformCss = "transform: scale(" + scale + ") rotate(" + rotationAngle + "deg);"
                            + " transform-origin: center center;"
                            + " transition: transform 0.2s ease;"
                            + " max-width: 100%; max-height: 100%; width: auto; height: auto;"
                            + " object-fit: contain; margin: auto;";

        if (isShowingFront) {
            if (frontImage != null) {
                frontImage.setVisible(true);
                frontImage.setStyle("display: block !important; " + transformCss);
            }
            if (backImage != null) {
                backImage.setVisible(false);
                backImage.setStyle("display: none !important; width: 0; height: 0;");
            }
        } else {
            if (frontImage != null) {
                frontImage.setVisible(false);
                frontImage.setStyle("display: none !important; width: 0; height: 0;");
            }
            if (backImage != null) {
                backImage.setVisible(true);
                backImage.setStyle("display: block !important; " + transformCss);
            }
        }
    }

    @Listen("onClick = #zoomInButton")
    public void zoomIn() {
        if (zoomLevel < 260) {
            zoomLevel += 20;
            applyImageVisibilityAndTransform();
        }
    }

    @Listen("onClick = #zoomOutButton")
    public void zoomOut() {
        if (zoomLevel > 60) {
            zoomLevel -= 20;
            applyImageVisibilityAndTransform();
        }
    }

    @Listen("onClick = #rotateButton")
    public void rotateImage() {
        rotationAngle = (rotationAngle + 90) % 360;
        applyImageVisibilityAndTransform();
    }

    // =========================================================
    // REJECTION REASONS LOADER (POPULATES MODAL COMBOBOX)
    // =========================================================
   
    
    
 // =========================================================
    // REJECTION REASONS LOADER (POPULATES MODAL COMBOBOX)
    // =========================================================
    private void loadRejectReasonsIntoModal() {
        // Fallback resolution in case nested @Wire was delayed
        if (modalRejectReasonCombobox == null && rejectModalWindow != null) {
            modalRejectReasonCombobox = (Combobox) rejectModalWindow.getFellowIfAny("modalRejectReasonCombobox");
        }

        if (modalRejectReasonCombobox == null) {
            System.err.println("[DEBUG-CTS] modalRejectReasonCombobox is NULL!");
            return;
        }

        modalRejectReasonCombobox.getItems().clear();

        Map<String, String> reasons = null;
        try {
            reasons = service.getReturnReasons();
        } catch (Exception ex) {
            System.err.println("[DEBUG-CTS] Exception when calling service.getReturnReasons():");
            ex.printStackTrace();
        }

        if (reasons != null && !reasons.isEmpty()) {
            for (Map.Entry<String, String> entry : reasons.entrySet()) {
                String code = entry.getKey();
                String name = entry.getValue();

                // Format: CODE - Description
                String displayLabel = code + " - " + name;

                Comboitem item = new Comboitem(displayLabel);
                item.setValue(code); // String varchar reason_code
                modalRejectReasonCombobox.appendChild(item);
            }
            System.out.println("[DEBUG-CTS] Appended " + modalRejectReasonCombobox.getItemCount() + " items to combobox.");
        } else {
            System.err.println("[DEBUG-CTS] return_reason_master query returned 0 rows or NULL.");
        }
    }
    @Listen("onChange = #amountTextbox; onChanging = #amountTextbox")
    public void onAmountChanged(Event event) {
        BigDecimal enteredAmount = null;

        if (event instanceof InputEvent) {
            String val = ((InputEvent) event).getValue();
            if (val != null && !val.trim().isEmpty()) {
                try {
                    String cleanVal = val.replace(",", "").trim();
                    enteredAmount = new BigDecimal(cleanVal);
                } catch (NumberFormatException ignored) {
                    return;
                }
            }
        } else {
            enteredAmount = amountTextbox.getValue();
        }

        if (enteredAmount == null || enteredAmount.compareTo(BigDecimal.ZERO) <= 0) {
            if (amountInWordsTextbox != null) {
                amountInWordsTextbox.setValue("");
            }
            return;
        }

        String words = convertNumberToIndianWords(enteredAmount);
        if (amountInWordsTextbox != null) {
            amountInWordsTextbox.setValue(words);
        }
    }

    public static String convertNumberToIndianWords(BigDecimal amount) {
        if (amount == null) return "";

        long rupees = amount.longValue();
        int paise = amount.remainder(BigDecimal.ONE).multiply(new BigDecimal(100)).intValue();

        StringBuilder result = new StringBuilder();

        if (rupees == 0) {
            result.append("Zero Rupees");
        } else {
            result.append(convertToIndianFormat(rupees)).append(" Rupees");
        }

        if (paise > 0) {
            result.append(" and ").append(convertToIndianFormat(paise)).append(" Paise");
        }

        result.append(" Only");
        return result.toString();
    }

    private static final String[] units = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] tens = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private static String convertToIndianFormat(long n) {
        if (n < 0) return "Minus " + convertToIndianFormat(-n);
        if (n == 0) return "";

        StringBuilder words = new StringBuilder();

        if (n / 10000000 > 0) {
            words.append(convertToIndianFormat(n / 10000000)).append(" Crore ");
            n %= 10000000;
        }
        if (n / 100000 > 0) {
            words.append(convertToIndianFormat(n / 100000)).append(" Lakh ");
            n %= 100000;
        }
        if (n / 100 > 0) {
            words.append(convertToIndianFormat(n / 100)).append(" Hundred ");
            n %= 100;
        }
        if (n > 0) {
            if (words.length() > 0) words.append("and ");
            if (n < 20) {
                words.append(units[(int) n]);
            } else {
                words.append(tens[(int) (n / 10)]);
                if (n % 10 > 0) {
                    words.append(" ").append(units[(int) (n % 10)]);
                }
            }
        }
        return words.toString().trim();
    }
    
}