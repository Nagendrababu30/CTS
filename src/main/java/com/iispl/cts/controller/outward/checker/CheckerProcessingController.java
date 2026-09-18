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
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
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
	private Textbox accountNumberLabel;

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

	private String cbsResult;

	private String chequeDateResult;

	private String selectedAction;

	private int currentChequeIndex = 0;

	private double imageScale = 1.0;

	private int imageRotation = 0;

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

		System.out.println("AFTER super.doAfterCompose()");

		batchService = new CheckerBatchService();

		processingService = new CheckerProcessingService();

		System.out.println("SERVICES INITIALIZED");

		/*
		 * Get logged-in Checker user.
		 */

		User currentUser = (User) Sessions.getCurrent().getAttribute("loggedInUser");

		System.out.println("CURRENT USER: " + (currentUser == null ? "NULL" : currentUser.getUserId()));

		if (currentUser == null) {

			System.out.println("CURRENT USER IS NULL - REDIRECT LOGIN");

			Executions.sendRedirect("/login.zul");

			return;
		}

		checkerUserId = currentUser.getUserId();

		System.out.println("CHECKER USER ID: " + checkerUserId);

		System.out.println("REQUEST URI = " + Executions.getCurrent().getNativeRequest());

		/*
		 * Read batch number from URL.
		 */

		batchNumber = Executions.getCurrent().getParameter("batchNumber");

		System.out.println("URL batchNumber = " + batchNumber);

		/*
		 * Read exact cheque number from URL.
		 *
		 * Normal parameter: chequeNumber
		 *
		 * Current browser/ZK request is sending: amp;chequeNumber
		 *
		 * Therefore use the normal parameter first, and fall back to amp;chequeNumber.
		 */

		chequeNumber = Executions.getCurrent().getParameter("chequeNumber");

		System.out.println("DEBUG chequeNumber = " + chequeNumber);

		if (chequeNumber == null) {

			chequeNumber = Executions.getCurrent().getParameter("amp;chequeNumber");
		}

		System.out.println("URL chequeNumber = " + chequeNumber);

		if (batchNumber == null || batchNumber.trim().isEmpty()) {

			System.out.println("BATCH NUMBER IS MISSING");

			showError("Batch number is missing.");

			return;
		}

		batchNumber = batchNumber.trim();

		if (chequeNumber != null) {
			chequeNumber = chequeNumber.trim();
		}

		System.out.println("TRIMMED batchNumber = " + batchNumber);

		System.out.println("TRIMMED chequeNumber = " + chequeNumber);

		/*
		 * Find batch.
		 */

		System.out.println("CALLING batchService.findBatch()");

		currentBatch = batchService.findBatch(batchNumber);

		System.out.println("batchService.findBatch() RETURNED");

		if (currentBatch == null) {

			System.out.println("CURRENT BATCH IS NULL");

			showError("Batch not found.");

			return;
		}

		System.out.println("BATCH FOUND: " + currentBatch.getBatchNumber());

		verificationBatchId.setValue(batchNumber);

		System.out.println("BEFORE wireEvents");

		wireEvents();

		System.out.println("AFTER wireEvents");

		System.out.println("BEFORE loadFirstCheque");

		loadFirstCheque();

		System.out.println("AFTER loadFirstCheque");

		System.out.println("======================================");

		System.out.println("CHECKER PROCESSING doAfterCompose END");

		System.out.println("======================================");
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

		if (frontButton != null) {

			frontButton.addEventListener(Events.ON_CLICK, event -> showFrontImage());
		}

		if (backSideButton != null) {

			backSideButton.addEventListener(Events.ON_CLICK, event -> showBackImage());
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

			rejectButton.addEventListener(Events.ON_CLICK, event -> selectAction("REJECT"));
		}

		if (sendBackButton != null) {

			sendBackButton.addEventListener(Events.ON_CLICK, event -> selectAction("SEND_BACK"));
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
	// LOAD FIRST CHEQUE
	// ============================================================

	private void loadFirstCheque() {

		System.out.println("PROCESSING LOAD START");

		System.out.println("batchNumber = " + batchNumber);

		System.out.println("chequeNumber = " + chequeNumber);

		currentChequeIndex = 0;

		if (chequeNumber != null && !chequeNumber.isEmpty()) {

			System.out.println("CALLING getCheque()");

			OutwardCheque cheque = processingService.getCheque(batchNumber, chequeNumber);

			System.out.println("getCheque() RETURNED");

			if (cheque == null) {

				System.out.println("CHEQUE IS NULL");

				showError("Cheque " + chequeNumber + " not found.");

				return;
			}

			System.out.println("CHEQUE FOUND: " + cheque.getChequeNumber());

			System.out.println("ACCOUNT: " + cheque.getDrawerAccountNumber());

			cheques = new ArrayList<>();

			cheques.add(cheque);

			reVerifyMode = "RE_VERIFIED".equalsIgnoreCase(cheque.getChequeStatus());

			System.out.println("REVERIFY MODE: " + reVerifyMode);

			System.out.println("CHEQUE STATUS: " + cheque.getChequeStatus());

			System.out.println("CALLING displayCheque()");

			displayCheque();

			System.out.println("displayCheque() RETURNED");

			return;
		}

		System.out.println("NO CHEQUE NUMBER - LOADING FULL BATCH");

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

		System.out.println("DISPLAY CHEQUE START");

		if (cheques == null || cheques.isEmpty() || currentChequeIndex < 0 || currentChequeIndex >= cheques.size()) {

			System.out.println("DISPLAY CHEQUE - INVALID CHEQUE LIST");

			return;
		}
		totalChequeCount.setValue(String.valueOf(cheques.size()));

		int completedCount = 0;

		for (OutwardCheque c : cheques) {

			if ("CHECKER_ACCEPTED".equalsIgnoreCase(c.getChequeStatus())
					|| "CHECKER_REJECTED".equalsIgnoreCase(c.getChequeStatus())) {

				completedCount++;
			}
		}

		completedChequeCount.setValue(String.valueOf(completedCount));

		int pendingCount = cheques.size() - completedCount;

		pendingChequeCount.setValue(String.valueOf(pendingCount));

		OutwardCheque cheque = cheques.get(currentChequeIndex);

		System.out.println("DISPLAYING CHEQUE: " + cheque.getChequeNumber());

		/*
		 * Display sequence.
		 *
		 * Normal processing: Cheque : 01 / 25
		 *
		 * Exact re-verification: Cheque : 01 / 01
		 */

		chequeSequence.setValue("Cheque : " + String.format("%02d", currentChequeIndex + 1) + " / " + cheques.size());

		chequeNumberLabel.setValue(safe(cheque.getChequeNumber()));

		rightChequeNumber.setValue(safe(cheque.getChequeNumber()));

		accountNumberLabel.setValue(safe(cheque.getDrawerAccountNumber()));

		drawerNameLabel.setValue(safe(cheque.getDrawerName()));

		payeeNameLabel.setValue(safe(cheque.getPayeeName()));

		amountLabel.setValue(cheque.getAmount() == null ? "" : cheque.getAmount().toString());

		amountInWordsLabel.setValue(safe(cheque.getAmountInWords()));

		chequeDateLabel.setValue(cheque.getChequeDate() == null ? "" : cheque.getChequeDate().toString());

		/*
		 * MICR
		 */

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

		/*
		 * Reset image.
		 */

		resetImageState();

		System.out.println("DISPLAY - BEFORE showFrontImage");

		showFrontImage();

		System.out.println("DISPLAY - AFTER showFrontImage");

		/*
		 * Maker rejection information.
		 */

		System.out.println("DISPLAY - BEFORE loadMakerRejection");

		loadMakerRejection(cheque);

		System.out.println("DISPLAY - AFTER loadMakerRejection");

		/*
		 * CBS + cheque date validation.
		 */

		System.out.println("DISPLAY - BEFORE validateCbs");

		validateCbs(cheque);

		System.out.println("DISPLAY - AFTER validateCbs");

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

		System.out.println("DISPLAY CHEQUE END");
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

		// --------------------------------------------------------
		// CBS ACCOUNT VALIDATION
		// --------------------------------------------------------

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

		// --------------------------------------------------------
		// CHEQUE DATE VALIDATION
		// --------------------------------------------------------

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

			Clients.showNotification("Please select an action.", Clients.NOTIFICATION_TYPE_WARNING, null, "top_center",
					2500);

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

			Clients.showNotification("Unable to save checker decision.", Clients.NOTIFICATION_TYPE_ERROR, null,
					"top_center", 3500);

			return;
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

		Executions.sendRedirect("/zul/outward/outward-checker/" + "batchesQueue.zul");
	}

	// ============================================================
	// FRONT IMAGE
	// ============================================================

	private void showFrontImage() {

		if (cheques == null || cheques.isEmpty()) {

			return;
		}

		OutwardCheque cheque = cheques.get(currentChequeIndex);

		if (cheque.getFrontImagePath() == null || cheque.getFrontImagePath().trim().isEmpty()) {

			chequeImage.setSrc("");

			return;
		}

		System.out.println("FRONT IMAGE PATH = " + cheque.getFrontImagePath());

		chequeImage.setSrc(cheque.getFrontImagePath());

		System.out.println("IMAGE SRC AFTER SET = " + chequeImage.getSrc());

		System.out.println("IMAGE COMPONENT = " + chequeImage);

		resetImageState();

		if (frontButton != null) {

			frontButton.setSclass("image-side-button selected");
		}

		if (backSideButton != null) {

			backSideButton.setSclass("image-side-button");
		}
	}

	// ============================================================
	// BACK IMAGE
	// ============================================================

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

		if (frontButton != null) {

			frontButton.setSclass("image-side-button");
		}

		if (backSideButton != null) {

			backSideButton.setSclass("image-side-button selected");
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