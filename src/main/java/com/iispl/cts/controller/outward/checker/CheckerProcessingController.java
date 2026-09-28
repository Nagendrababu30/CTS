package com.iispl.cts.controller.outward.checker;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Separator;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Window;

import com.cts.admin.model.User;
import com.iispl.cts.model.outward.ChequeProcessing;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.model.outward.ReturnReason;
import com.iispl.cts.service.outward.checker.CheckerBatchService;
import com.iispl.cts.service.outward.checker.CheckerProcessingService;

public class CheckerProcessingController extends SelectorComposer<Vlayout> {

	private static final long serialVersionUID = 1L;

	@Wire
	private Label verificationBatchId;

	@Wire
	private Label chequeSequence;

	@Wire
	private Label totalChequeCount;

	@Wire
	private Label completedChequeCount;

	@Wire
	private Label pendingChequeCount;

	@Wire
	private Textbox rightChequeNumber;

	@Wire
	private Button backButton;

	@Wire
	private Image chequeImage;

	@Wire
	private Button toggleSideButton;

	@Wire
	private Button rotateButton;

	@Wire
	private Button zoomOutButton;

	@Wire
	private Button zoomInButton;

	@Wire
	private Button prevButton;

	@Wire
	private Button nextButton;

	@Wire
	private Label zoomLevelLabel;

	/*
	 * LEFT SIDE chequeNumberLabel is a Label in ZUL.
	 */
	@Wire
	private Label chequeNumberLabel;

	/*
	 * RIGHT SIDE fields are Textbox in ZUL.
	 */
	@Wire
	private Textbox draweraccountNumberLabel;
	
	@Wire
	private Textbox payeeaccountNumberLabel;

	@Wire
	private Textbox drawerNameLabel;

	@Wire
	private Textbox payeeNameLabel;

	@Wire
	private Textbox amountLabel;

	@Wire
	private Textbox amountInWordsLabel;

	@Wire
	private Textbox chequeDateLabel;

	@Wire
	private Label chequeDateValidationMessage;

	@Wire
	private Textbox micrLabel;

	@Wire
	private Label accountVerificationIcon;

	@Wire
	private Label accountVerificationMessage;

	@Wire
	private Label accountVerificationReason;

	@Wire
	private Div makerRejectionBlock;

	@Wire
	private Label makerRejectionReason;

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

	/* DYNAMICALLY CREATED MODAL COMPONENTS */
	private Window actionModalWindow;
	private Combobox modalReasonCombobox;
	private Textbox modalCheckerRemarksTextbox;

	private String cbsResult;

	private String chequeDateResult;

	private String selectedAction;

	private int currentChequeIndex = 0;

	private double imageScale = 1.0;

	private int imageRotation = 0;

	private boolean showingFront = true;

	private CheckerBatchService batchService;

	private CheckerProcessingService processingService;

	private String batchNumber;

	private String chequeNumber;

	private boolean reVerifyMode = false;

	private long checkerUserId;

	private OutwardBatch currentBatch;

	private List<OutwardCheque> cheques;

	// ============================================================
	// INIT
	// ============================================================

	@Override
	public void doAfterCompose(Vlayout component) throws Exception {

		System.out.println("======================================");
		System.out.println("CHECKER PROCESSING doAfterCompose START");
		System.out.println("======================================");

		super.doAfterCompose(component);

		batchService = new CheckerBatchService();
		processingService = new CheckerProcessingService();

		/*
		 * Get logged-in Checker user.
		 */
		User currentUser = (User) Sessions.getCurrent().getAttribute("loggedInUser");

		if (currentUser == null) {
			Executions.sendRedirect("/login.zul");
			return;
		}

		checkerUserId = currentUser.getUserId();

		/*
		 * Read batch number from URL.
		 */
		batchNumber = Executions.getCurrent().getParameter("batchNumber");

		/*
		 * Read exact cheque number from URL.
		 */
		chequeNumber = Executions.getCurrent().getParameter("chequeNumber");

		if (chequeNumber == null) {
			chequeNumber = Executions.getCurrent().getParameter("amp;chequeNumber");
		}

		if (batchNumber == null || batchNumber.trim().isEmpty()) {
			showError("Batch number is missing.");
			return;
		}

		batchNumber = batchNumber.trim();

		if (chequeNumber != null) {
			chequeNumber = chequeNumber.trim();
		}

		currentBatch = batchService.findBatch(batchNumber);

		if (currentBatch == null) {
			showError("Batch not found.");
			return;
		}

		verificationBatchId.setValue(batchNumber);

		wireEvents();

		loadFirstCheque();
	}

	// ============================================================
	// EVENTS
	// ============================================================

	private void wireEvents() {

		if (prevButton != null) {
			prevButton.addEventListener(Events.ON_CLICK, event -> previousCheque());
		}

		if (nextButton != null) {
			nextButton.addEventListener(Events.ON_CLICK, event -> nextCheque());
		}

		if (backButton != null) {
			backButton.addEventListener(Events.ON_CLICK, event -> goBackToQueue());
		}

		if (toggleSideButton != null) {
			toggleSideButton.addEventListener(Events.ON_CLICK, event -> toggleImageSide());
		}

		if (rotateButton != null) {
			rotateButton.addEventListener(Events.ON_CLICK, event -> rotateImage());
		}

		if (zoomInButton != null) {
			zoomInButton.addEventListener(Events.ON_CLICK, event -> zoomIn());
		}

		if (zoomOutButton != null) {
			zoomOutButton.addEventListener(Events.ON_CLICK, event -> zoomOut());
		}

		if (acceptButton != null) {
			acceptButton.addEventListener(Events.ON_CLICK, event -> selectAction("ACCEPT"));
		}

		if (rejectButton != null) {
			rejectButton.addEventListener(Events.ON_CLICK, event -> openActionModal("REJECT"));
		}

		if (sendBackButton != null) {
			sendBackButton.addEventListener(Events.ON_CLICK, event -> openActionModal("SEND_BACK"));
		}

		if (saveNextButton != null) {
			saveNextButton.addEventListener(Events.ON_CLICK, event -> saveAndNext());
		}

		if (reasonCombobox != null) {
			reasonCombobox.addEventListener(Events.ON_CHANGE, event -> updateSaveNextButton());
		}

		if (checkerRemarksTextbox != null) {
			checkerRemarksTextbox.addEventListener(Events.ON_CHANGE, event -> updateSaveNextButton());
		}
	}

	// ============================================================
	// DYNAMIC MODAL CREATION & HANDLING IN CONTROLLER
	// ============================================================

	private void openActionModal(String action) {

	    selectedAction = action;

	    // Clean up any previously created modal instance
	    if (actionModalWindow != null) {
	        actionModalWindow.detach();
	        actionModalWindow = null;
	    }

	    // 1. Create the Window modal
	    actionModalWindow = new Window();
	    actionModalWindow.setTitle("REJECT".equalsIgnoreCase(action) ? "Reject Cheque" : "Return Cheque to Maker");
	    actionModalWindow.setWidth("480px");
	    actionModalWindow.setBorder("normal");
	    actionModalWindow.setClosable(true);
	    actionModalWindow.setSclass("action-reason-modal");
	    actionModalWindow.addEventListener(Events.ON_CLOSE, event -> closeActionModal());

	    // 2. Main layout inside the modal
	    Vlayout modalBody = new Vlayout();
	    modalBody.setWidth("100%");
	    modalBody.setSpacing("12px");
	    modalBody.setStyle("padding: 16px;");
	    modalBody.setParent(actionModalWindow);

	    // Reason Field Section
	    Vlayout reasonSection = new Vlayout();
	    reasonSection.setWidth("100%");
	    reasonSection.setSpacing("4px");
	    reasonSection.setParent(modalBody);

	    Label reasonLabel = new Label("Reason *");
	    reasonLabel.setSclass("form-label");
	    reasonLabel.setParent(reasonSection);

	    modalReasonCombobox = new Combobox();
	    modalReasonCombobox.setWidth("100%");
	    modalReasonCombobox.setPlaceholder("Select reason");
	    modalReasonCombobox.setReadonly(true);
	    modalReasonCombobox.setParent(reasonSection);

	    loadModalReturnReasons(action);

	    // Remarks Field Section
	    Vlayout remarksSection = new Vlayout();
	    remarksSection.setWidth("100%");
	    remarksSection.setSpacing("4px");
	    remarksSection.setParent(modalBody);

	    Label remarksLabel = new Label("Remarks");
	    remarksLabel.setSclass("form-label");
	    remarksLabel.setParent(remarksSection);

	    modalCheckerRemarksTextbox = new Textbox();
	    modalCheckerRemarksTextbox.setWidth("100%");
	    modalCheckerRemarksTextbox.setRows(3);
	    modalCheckerRemarksTextbox.setMaxlength(1000);
	    modalCheckerRemarksTextbox.setPlaceholder("Enter remarks (optional)");
	    modalCheckerRemarksTextbox.setSclass("form-control");
	    modalCheckerRemarksTextbox.setParent(remarksSection);

	    Separator sep = new Separator();
	    sep.setHeight("10px");
	    sep.setParent(modalBody);

	    // Buttons Action Bar
	    Hlayout buttonBar = new Hlayout();
	    buttonBar.setWidth("100%");
	    buttonBar.setSpacing("10px");
	    buttonBar.setValign("middle");
	    buttonBar.setParent(modalBody);

	    Space spacer = new Space();
	    spacer.setHflex("1");
	    spacer.setParent(buttonBar);

	    Button modalCancelButton = new Button("Cancel");
	    modalCancelButton.setSclass("navigation-button");
	    modalCancelButton.addEventListener(Events.ON_CLICK, event -> closeActionModal());
	    modalCancelButton.setParent(buttonBar);

	    Button modalSubmitButton = new Button("Submit");
	    modalSubmitButton.setSclass("complete-button modal-submit-button");
	    modalSubmitButton.addEventListener(Events.ON_CLICK, event -> submitModalDecision());
	    modalSubmitButton.setParent(buttonBar);

	    // Attach to parent root and show
	    actionModalWindow.setParent(getSelf());
	    actionModalWindow.doModal();
	}

	private void closeActionModal() {

		if (actionModalWindow != null) {
			actionModalWindow.detach();
			actionModalWindow = null;
		}

		selectedAction = null;
		updateSaveNextButton();
	}

	private void loadModalReturnReasons(String action) {

		if (modalReasonCombobox == null) {
			return;
		}

		modalReasonCombobox.getItems().clear();

		List<ReturnReason> reasons = processingService.getReturnReasons(action);

		if (reasons == null || reasons.isEmpty()) {
			return;
		}

		for (ReturnReason reason : reasons) {
			Comboitem item = modalReasonCombobox.appendItem(reason.getReasonName());
			item.setValue(reason.getReasonCode());
		}
	}

	private void submitModalDecision() {

		if (modalReasonCombobox == null || modalReasonCombobox.getSelectedItem() == null) {
			Clients.showNotification("Please select a reason.", Clients.NOTIFICATION_TYPE_WARNING, modalReasonCombobox, "middle_center", 2500);
			return;
		}

		String reasonCode = modalReasonCombobox.getSelectedItem().getValue() != null
				? modalReasonCombobox.getSelectedItem().getValue().toString()
				: null;

		String remarks = modalCheckerRemarksTextbox != null ? modalCheckerRemarksTextbox.getValue() : null;

		if (cheques == null || cheques.isEmpty() || currentChequeIndex < 0 || currentChequeIndex >= cheques.size()) {
			return;
		}

		OutwardCheque cheque = cheques.get(currentChequeIndex);

		boolean saved = processingService.saveCheckerDecision(batchNumber, cheque.getChequeNumber(), checkerUserId,
				selectedAction, reasonCode, remarks);

		if (!saved) {
			Clients.showNotification("Unable to save checker decision.", Clients.NOTIFICATION_TYPE_ERROR, null, "top_center", 3500);
			return;
		}
		
		if ("REJECT".equalsIgnoreCase(selectedAction)) {
			cheque.setChequeStatus("CHECKER_REJECTED");
		} else if ("SEND_BACK".equalsIgnoreCase(selectedAction)) {
			cheque.setChequeStatus("CHECKER_RETURNED");
		}

		closeActionModal();

		boolean lastCheque = currentChequeIndex >= cheques.size() - 1;

		if (!lastCheque) {
			currentChequeIndex++;
			displayCheque();
			return;
		}

		finishProcessing();
	}

	// ============================================================
	// LOAD FIRST CHEQUE
	// ============================================================

	private void loadFirstCheque() {

		currentChequeIndex = 0;

		if (chequeNumber != null && !chequeNumber.isEmpty()) {

			OutwardCheque cheque = processingService.getCheque(batchNumber, chequeNumber);

			if (cheque == null) {
				showError("Cheque " + chequeNumber + " not found.");
				return;
			}

			cheques = new ArrayList<>();
			cheques.add(cheque);

			reVerifyMode = "RE_VERIFIED".equalsIgnoreCase(cheque.getChequeStatus());

			displayCheque();
			return;
		}

		cheques = batchService.getChequesByBatchNumber(batchNumber);

		if (cheques == null || cheques.isEmpty()) {
			showError("No cheques found for this batch.");
			return;
		}

		reVerifyMode = false;
		displayCheque();
	}

	// ============================================================
	// DISPLAY CHEQUE
	// ============================================================

	private void displayCheque() {

		if (cheques == null || cheques.isEmpty() || currentChequeIndex < 0 || currentChequeIndex >= cheques.size()) {
			return;
		}

		totalChequeCount.setValue(String.valueOf(cheques.size()));

		int completedCount = 0;

		for (OutwardCheque c : cheques) {
			String status = c.getChequeStatus();
			if (status != null && (
					"CHECKER_ACCEPTED".equalsIgnoreCase(status)
					|| "CHECKER_REJECTED".equalsIgnoreCase(status)
					|| "CHECKER_RETURNED".equalsIgnoreCase(status)
					|| "RETURN_BY_CHECKER".equalsIgnoreCase(status)
					|| "SEND_BACK".equalsIgnoreCase(status))) {

				completedCount++;
			}
		}

		completedChequeCount.setValue(String.valueOf(completedCount));

		int pendingCount = cheques.size() - completedCount;

		pendingChequeCount.setValue(String.valueOf(pendingCount));

		OutwardCheque cheque = cheques.get(currentChequeIndex);

		chequeSequence.setValue("Cheque : " + String.format("%02d", currentChequeIndex + 1) + " / " + cheques.size());

		chequeNumberLabel.setValue(safe(cheque.getChequeNumber()));

		rightChequeNumber.setValue(safe(cheque.getChequeNumber()));

		draweraccountNumberLabel.setValue(safe(cheque.getDrawerAccountNumber()));

		drawerNameLabel.setValue(safe(cheque.getDrawerName()));

		payeeNameLabel.setValue(safe(cheque.getPayeeName()));

		payeeaccountNumberLabel.setValue(safe(cheque.getPayeeAccountNumber()));

		amountLabel.setValue(cheque.getAmount() == null ? "" : cheque.getAmount().toString());

		amountInWordsLabel.setValue(safe(cheque.getAmountInWords()));

		chequeDateLabel.setValue(cheque.getChequeDate() == null ? "" : cheque.getChequeDate().toString());

		String micr = "";

		if (cheque.getCityCode() != null) {
			micr += cheque.getCityCode();
		}

		if (cheque.getBankCode() != null) {
			if (!micr.isEmpty()) {
				micr += " ";
			}
			micr += cheque.getBankCode();
		}

		if (cheque.getBranchCode() != null) {
			if (!micr.isEmpty()) {
				micr += " ";
			}
			micr += cheque.getBranchCode();
		}

		micrLabel.setValue(micr);

		resetImageState();

		showFrontImage();

		loadMakerRejection(cheque);

		validateCbs(cheque);

		selectedAction = null;

		if (reasonRow != null) {
			reasonRow.setVisible(false);
		}

		if (reasonCombobox != null) {
			reasonCombobox.getItems().clear();
			reasonCombobox.setValue("");
		}

		if (checkerRemarksTextbox != null) {
			checkerRemarksTextbox.setValue("");
		}

		updateSaveNextButton();
	}

	// ============================================================
	// MAKER REJECTION
	// ============================================================

	private void loadMakerRejection(OutwardCheque cheque) {

		if (makerRejectionBlock == null) {
			return;
		}

		ChequeProcessing processing = processingService.getChequeProcessing(batchNumber, cheque.getChequeNumber());

		if (processing == null) {
			makerRejectionBlock.setVisible(false);
			return;
		}

		boolean makerRejected = "REJECT_REQUEST".equalsIgnoreCase(processing.getMakerAction());

		if (!makerRejected) {
			makerRejectionBlock.setVisible(false);
			return;
		}

		makerRejectionBlock.setVisible(true);

		String reason = processing.getMakerReasonCode();

		if (reason == null || reason.trim().isEmpty()) {
			makerRejectionReason.setValue("Reason not specified");
			return;
		}

		String reasonName = processingService.getMakerReasonName(reason);

		if (reasonName != null && !reasonName.trim().isEmpty()) {
			makerRejectionReason.setValue(reasonName);
		} else {
			makerRejectionReason.setValue(reason);
		}
	}

	// ============================================================
	// CBS + CHEQUE DATE VALIDATION
	// ============================================================

	private void validateCbs(OutwardCheque cheque) {

		cbsResult = processingService.validateCbsAccount(cheque.getDrawerAccountNumber());

		boolean cbsPassed = "PASS".equalsIgnoreCase(cbsResult);

		if (cbsPassed) {
			accountVerificationIcon.setValue("✓");
			accountVerificationMessage.setValue("CBS Verified");
			accountVerificationReason.setValue("");
			accountVerificationReason.setVisible(false);
		} else {
			accountVerificationIcon.setValue("✕");
			accountVerificationMessage.setValue("CBS Validation Failed");
			accountVerificationReason.setValue(processingService.getCbsValidationMessage(cbsResult));
			accountVerificationReason.setVisible(true);
		}

		chequeDateResult = processingService.validateChequeDate(cheque.getChequeDate());

		boolean datePassed = "PASS".equalsIgnoreCase(chequeDateResult);

		if (datePassed) {
			chequeDateValidationMessage.setValue("");
			chequeDateValidationMessage.setVisible(false);
		} else {
			chequeDateValidationMessage.setValue("✕ " + processingService.getCbsValidationMessage(chequeDateResult));
			chequeDateValidationMessage.setVisible(true);
		}

		boolean validationPassed = cbsPassed && datePassed;

		if (acceptButton != null) {
			acceptButton.setDisabled(!validationPassed);
		}

		if (rejectButton != null) {
			rejectButton.setDisabled(false);
		}

		if (sendBackButton != null) {
			sendBackButton.setDisabled(!validationPassed);
		}
	}

	// ============================================================
	// SELECT ACTION
	// ============================================================

	private void selectAction(String action) {

		selectedAction = action;

		if ("REJECT".equalsIgnoreCase(action) || "SEND_BACK".equalsIgnoreCase(action)) {

			if (reasonRow != null) {
				reasonRow.setVisible(true);
			}

			loadReturnReasons(action);

		} else {

			if (reasonRow != null) {
				reasonRow.setVisible(false);
			}

			if (reasonCombobox != null) {
				reasonCombobox.getItems().clear();
				reasonCombobox.setValue("");
			}
		}

		updateSaveNextButton();
	}

	// ============================================================
	// LOAD CHECKER REASONS
	// ============================================================

	private void loadReturnReasons(String action) {

		if (reasonCombobox == null) {
			return;
		}

		reasonCombobox.getItems().clear();

		List<ReturnReason> reasons = processingService.getReturnReasons(action);

		if (reasons == null || reasons.isEmpty()) {
			return;
		}

		for (ReturnReason reason : reasons) {
			org.zkoss.zul.Comboitem item = reasonCombobox.appendItem(reason.getReasonName());
			item.setValue(reason.getReasonCode());
		}
	}

	// ============================================================
	// SAVE BUTTON STATE
	// ============================================================

	private void updateSaveNextButton() {

		if (saveNextButton == null) {
			return;
		}

		if (selectedAction == null) {
			saveNextButton.setDisabled(true);
			return;
		}

		boolean validationPassed = "PASS".equalsIgnoreCase(cbsResult) && "PASS".equalsIgnoreCase(chequeDateResult);

		if ("ACCEPT".equalsIgnoreCase(selectedAction)) {
			saveNextButton.setDisabled(!validationPassed);
			return;
		}

		if ("SEND_BACK".equalsIgnoreCase(selectedAction)) {
			boolean reasonSelected = reasonCombobox != null && reasonCombobox.getSelectedItem() != null;
			saveNextButton.setDisabled(!validationPassed || !reasonSelected);
			return;
		}

		if ("REJECT".equalsIgnoreCase(selectedAction)) {
			boolean reasonSelected = reasonCombobox != null && reasonCombobox.getSelectedItem() != null;
			saveNextButton.setDisabled(!reasonSelected);
			return;
		}

		saveNextButton.setDisabled(true);
	}

	private void previousCheque() {

		if (cheques == null || cheques.isEmpty()) {
			return;
		}

		if (currentChequeIndex > 0) {
			currentChequeIndex--;
			displayCheque();
		}
	}

	private void nextCheque() {

		if (cheques == null || cheques.isEmpty()) {
			return;
		}

		if (currentChequeIndex < cheques.size() - 1) {
			currentChequeIndex++;
			displayCheque();
		}
	}

	// ============================================================
	// SAVE & NEXT
	// ============================================================

	private void saveAndNext() {

		if (selectedAction == null) {
			Clients.showNotification("Please select an action.", Clients.NOTIFICATION_TYPE_WARNING, null, "top_center", 2500);
			return;
		}

		boolean validationPassed = "PASS".equalsIgnoreCase(cbsResult) && "PASS".equalsIgnoreCase(chequeDateResult);

		if (("ACCEPT".equalsIgnoreCase(selectedAction) || "SEND_BACK".equalsIgnoreCase(selectedAction))
				&& !validationPassed) {

			StringBuilder message = new StringBuilder();

			if (!"PASS".equalsIgnoreCase(cbsResult)) {
				message.append(processingService.getCbsValidationMessage(cbsResult));
			}

			if (!"PASS".equalsIgnoreCase(chequeDateResult)) {
				if (message.length() > 0) {
					message.append(" | ");
				}
				message.append(processingService.getCbsValidationMessage(chequeDateResult));
			}

			Clients.showNotification(message.toString(), Clients.NOTIFICATION_TYPE_ERROR, null, "top_center", 4000);
			return;
		}

		String reasonCode = null;

		if (("REJECT".equalsIgnoreCase(selectedAction) || "SEND_BACK".equalsIgnoreCase(selectedAction))
				&& reasonCombobox != null && reasonCombobox.getSelectedItem() != null) {

			Object value = reasonCombobox.getSelectedItem().getValue();
			if (value != null) {
				reasonCode = value.toString();
			}
		}

		String remarks = null;

		if (checkerRemarksTextbox != null) {
			remarks = checkerRemarksTextbox.getValue();
		}

		if (cheques == null || cheques.isEmpty() || currentChequeIndex < 0 || currentChequeIndex >= cheques.size()) {
			return;
		}

		OutwardCheque cheque = cheques.get(currentChequeIndex);

		boolean saved = processingService.saveCheckerDecision(batchNumber, cheque.getChequeNumber(), checkerUserId,
				selectedAction, reasonCode, remarks);

		if (!saved) {
			Clients.showNotification("Unable to save checker decision.", Clients.NOTIFICATION_TYPE_ERROR, null, "top_center", 3500);
			return;
		}
		
		if ("ACCEPT".equalsIgnoreCase(selectedAction)) {
			cheque.setChequeStatus("CHECKER_ACCEPTED");
		}

		boolean lastCheque = currentChequeIndex >= cheques.size() - 1;

		if (!lastCheque) {
			currentChequeIndex++;
			displayCheque();
			return;
		}

		finishProcessing();
	}

	// ============================================================
	// FINISH PROCESSING
	// ============================================================

	private void finishProcessing() {

		Clients.showNotification(reVerifyMode ? "Re-verification completed." : "Checker processing completed.",
				Clients.NOTIFICATION_TYPE_INFO, null, "top_center", 2500);

		goBackToQueue();
	}

	// ============================================================
	// BACK TO QUEUE
	// ============================================================

	private void goBackToQueue() {

		Executions.sendRedirect("/zul/outward/outward-checker/batchesQueue.zul");
	}

	// ============================================================
	// IMAGE TOGGLE (FRONT / BACK)
	// ============================================================

	private void toggleImageSide() {

		if (showingFront) {
			showBackImage();
		} else {
			showFrontImage();
		}
	}

	private void showFrontImage() {

		if (cheques == null || cheques.isEmpty()) {
			return;
		}

		OutwardCheque cheque = cheques.get(currentChequeIndex);

		if (cheque.getFrontImagePath() == null || cheque.getFrontImagePath().trim().isEmpty()) {
			chequeImage.setSrc("");
			return;
		}

		chequeImage.setSrc(cheque.getFrontImagePath());
		resetImageState();
		showingFront = true;

		if (toggleSideButton != null) {
			toggleSideButton.setLabel("View Back");
		}
	}

	private void showBackImage() {

		if (cheques == null || cheques.isEmpty()) {
			return;
		}

		OutwardCheque cheque = cheques.get(currentChequeIndex);

		if (cheque.getBackImagePath() == null || cheque.getBackImagePath().trim().isEmpty()) {
			chequeImage.setSrc("");
			return;
		}

		chequeImage.setSrc(cheque.getBackImagePath());
		resetImageState();
		showingFront = false;

		if (toggleSideButton != null) {
			toggleSideButton.setLabel("View Front");
		}
	}

	// ============================================================
	// ROTATE
	// ============================================================

	private void rotateImage() {

		imageRotation += 90;

		if (imageRotation >= 360) {
			imageRotation = 0;
		}

		updateImageTransform();
	}

	// ============================================================
	// ZOOM IN
	// ============================================================

	private void zoomIn() {

		imageScale += 0.1;

		if (imageScale > 3.0) {
			imageScale = 3.0;
		}

		updateImageTransform();
	}

	// ============================================================
	// ZOOM OUT
	// ============================================================

	private void zoomOut() {

		imageScale -= 0.1;

		if (imageScale < 0.5) {
			imageScale = 0.5;
		}

		updateImageTransform();
	}

	// ============================================================
	// IMAGE TRANSFORM
	// ============================================================

	private void updateImageTransform() {

		if (chequeImage != null) {
			chequeImage.setStyle("transform: rotate(" + imageRotation + "deg) scale(" + imageScale + ");");
		}

		if (zoomLevelLabel != null) {
			zoomLevelLabel.setValue(Math.round(imageScale * 100) + "%");
		}
	}

	// ============================================================
	// RESET IMAGE
	// ============================================================

	private void resetImageState() {

		imageScale = 1.0;
		imageRotation = 0;
		updateImageTransform();
	}

	// ============================================================
	// SAFE STRING
	// ============================================================

	private String safe(String value) {

		return value == null ? "" : value;
	}

	// ============================================================
	// ERROR MESSAGE
	// ============================================================

	private void showError(String message) {

		Clients.showNotification(message, Clients.NOTIFICATION_TYPE_ERROR, null, "top_center", 3000);
	}
}