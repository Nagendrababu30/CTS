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
	private Label zoomLevelLabel;

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
	private Label chequeDateValidationMessage;

	@Wire
	private Label micrLabel;

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
	private boolean reVerifyMode = false;
	private long checkerUserId;

	private OutwardBatch currentBatch;
	private List<OutwardCheque> cheques;

	// ============================================================
	// INIT
	// ============================================================

	@Override
	public void doAfterCompose(Vlayout component) throws Exception {

		super.doAfterCompose(component);

		batchService = new CheckerBatchService();
		processingService = new CheckerProcessingService();

		/*
		 * Use the same session attribute as Batch Queue.
		 */
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

		batchNumber = Executions.getCurrent()
				.getParameter("batchNumber");

		String mode = Executions.getCurrent()
				.getParameter("mode");

		reVerifyMode = "RE_VERIFY".equalsIgnoreCase(mode);

		if (batchNumber == null || batchNumber.trim().isEmpty()) {

			showError("Batch number is missing.");

			return;
		}

		batchNumber = batchNumber.trim();

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

		if (backButton != null) {
			backButton.addEventListener(Events.ON_CLICK,
					event -> goBackToQueue());
		}

		if (frontButton != null) {
			frontButton.addEventListener(Events.ON_CLICK,
					event -> showFrontImage());
		}

		if (backSideButton != null) {
			backSideButton.addEventListener(Events.ON_CLICK,
					event -> showBackImage());
		}

		if (rotateButton != null) {
			rotateButton.addEventListener(Events.ON_CLICK,
					event -> rotateImage());
		}

		if (zoomInButton != null) {
			zoomInButton.addEventListener(Events.ON_CLICK,
					event -> zoomIn());
		}

		if (zoomOutButton != null) {
			zoomOutButton.addEventListener(Events.ON_CLICK,
					event -> zoomOut());
		}

		if (acceptButton != null) {
			acceptButton.addEventListener(Events.ON_CLICK,
					event -> selectAction("ACCEPT"));
		}

		if (rejectButton != null) {
			rejectButton.addEventListener(Events.ON_CLICK,
					event -> selectAction("REJECT"));
		}

		if (sendBackButton != null) {
			sendBackButton.addEventListener(Events.ON_CLICK,
					event -> selectAction("SEND_BACK"));
		}

		if (saveNextButton != null) {
			saveNextButton.addEventListener(Events.ON_CLICK,
					event -> saveAndNext());
		}

		if (reasonCombobox != null) {
			reasonCombobox.addEventListener(Events.ON_CHANGE,
					event -> updateSaveNextButton());
		}

		if (checkerRemarksTextbox != null) {
			checkerRemarksTextbox.addEventListener(Events.ON_CHANGE,
					event -> updateSaveNextButton());
		}
	}

	// ============================================================
	// LOAD FIRST CHEQUE
	// ============================================================

	private void loadFirstCheque() {

		currentChequeIndex = 0;

		if (reVerifyMode) {

			loadReVerifyCheques();

			return;
		}

		cheques =
				batchService.getChequesByBatchNumber(batchNumber);

		if (cheques == null || cheques.isEmpty()) {

			showError("No cheques found for this batch.");

			return;
		}

		displayCheque();
	}

	// ============================================================
	// LOAD RE-VERIFY CHEQUES
	// ============================================================

	private void loadReVerifyCheques() {

		List<OutwardCheque> allCheques =
				batchService.getChequesByBatchNumber(batchNumber);

		List<OutwardCheque> reVerifiedCheques =
				new ArrayList<>();

		if (allCheques != null) {

			for (OutwardCheque cheque : allCheques) {

				ChequeProcessing processing =
						processingService.getChequeProcessing(
								batchNumber,
								cheque.getChequeNumber());

				if (processing == null) {
					continue;
				}

				boolean sameChecker =
						processing.getCheckerId() != null
								&& processing.getCheckerId().longValue()
										== checkerUserId;

				boolean wasSentBack =
						"SEND_BACK".equalsIgnoreCase(
								processing.getCheckerAction());

				boolean corrected =
						"RE_VERIFIED".equalsIgnoreCase(
								cheque.getChequeStatus());

				if (sameChecker && wasSentBack && corrected) {
					reVerifiedCheques.add(cheque);
				}
			}
		}

		cheques = reVerifiedCheques;

		if (cheques.isEmpty()) {

			Clients.showNotification(
					"No corrected cheques are ready for re-verification.",
					Clients.NOTIFICATION_TYPE_INFO,
					null,
					"top_center",
					4000);

			return;
		}

		currentChequeIndex = 0;

		displayCheque();
	}

	// ============================================================
	// DISPLAY CHEQUE
	// ============================================================

	private void displayCheque() {

		if (cheques == null
				|| cheques.isEmpty()
				|| currentChequeIndex < 0
				|| currentChequeIndex >= cheques.size()) {
			return;
		}

		OutwardCheque cheque =
				cheques.get(currentChequeIndex);

		chequeSequence.setValue(
				"Cheque : "
						+ String.format(
								"%02d",
								currentChequeIndex + 1)
						+ " / "
						+ cheques.size());

		chequeNumberLabel.setValue(
				safe(cheque.getChequeNumber()));

		accountNumberLabel.setValue(
				safe(cheque.getDrawerAccountNumber()));

		drawerNameLabel.setValue(
				safe(cheque.getDrawerName()));

		payeeNameLabel.setValue(
				safe(cheque.getPayeeName()));

		amountLabel.setValue(
				cheque.getAmount() == null
						? ""
						: cheque.getAmount().toString());

		amountInWordsLabel.setValue(
				safe(cheque.getAmountInWords()));

		chequeDateLabel.setValue(
				cheque.getChequeDate() == null
						? ""
						: cheque.getChequeDate().toString());

		String micr = "";

		if (cheque.getBankCode() != null) {
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

		ChequeProcessing processing =
				processingService.getChequeProcessing(
						batchNumber,
						cheque.getChequeNumber());

		if (processing == null) {

			makerRejectionBlock.setVisible(false);

			return;
		}

		boolean makerRejected =
				"REJECT_REQUEST".equalsIgnoreCase(
						processing.getMakerAction());

		if (!makerRejected) {

			makerRejectionBlock.setVisible(false);

			return;
		}

		makerRejectionBlock.setVisible(true);

		String reason =
				processing.getMakerReasonCode();

		if (reason == null || reason.trim().isEmpty()) {

			makerRejectionReason.setValue(
					"Reason not specified");

			return;
		}

		String reasonName =
				processingService.getMakerReasonName(reason);

		if (reasonName != null
				&& !reasonName.trim().isEmpty()) {

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

		cbsResult =
				processingService.validateCbsAccount(
						cheque.getDrawerAccountNumber());

		boolean cbsPassed =
				"PASS".equalsIgnoreCase(cbsResult);

		if (cbsPassed) {

			accountVerificationIcon.setValue("✓");

			accountVerificationMessage.setValue(
					"CBS Verified");

			accountVerificationReason.setValue("");

			accountVerificationReason.setVisible(false);

		} else {

			accountVerificationIcon.setValue("✕");

			accountVerificationMessage.setValue(
					"CBS Validation Failed");

			accountVerificationReason.setValue(
					processingService.getCbsValidationMessage(
							cbsResult));

			accountVerificationReason.setVisible(true);
		}

		// --------------------------------------------------------
		// CHEQUE DATE VALIDATION
		// --------------------------------------------------------

		chequeDateResult =
				processingService.validateChequeDate(
						cheque.getChequeDate());

		boolean datePassed =
				"PASS".equalsIgnoreCase(
						chequeDateResult);

		if (datePassed) {

			chequeDateValidationMessage.setValue("");

			chequeDateValidationMessage.setVisible(false);

		} else {

			chequeDateValidationMessage.setValue(
					"✕ "
							+ processingService
									.getCbsValidationMessage(
											chequeDateResult));

			chequeDateValidationMessage.setVisible(true);
		}

		boolean validationPassed =
				cbsPassed && datePassed;

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

		if ("REJECT".equalsIgnoreCase(action)
				|| "SEND_BACK".equalsIgnoreCase(action)) {

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

		List<ReturnReason> reasons =
				processingService.getReturnReasons(action);

		if (reasons == null || reasons.isEmpty()) {
			return;
		}

		for (ReturnReason reason : reasons) {

			org.zkoss.zul.Comboitem item =
					reasonCombobox.appendItem(
							reason.getReasonName());

			item.setValue(
					reason.getReasonCode());
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

		boolean validationPassed =
				"PASS".equalsIgnoreCase(cbsResult)
						&& "PASS".equalsIgnoreCase(
								chequeDateResult);

		if ("ACCEPT".equalsIgnoreCase(selectedAction)) {

			saveNextButton.setDisabled(
					!validationPassed);

			return;
		}

		if ("SEND_BACK".equalsIgnoreCase(selectedAction)) {

			boolean reasonSelected =
					reasonCombobox != null
							&& reasonCombobox
									.getSelectedItem() != null;

			saveNextButton.setDisabled(
					!validationPassed
							|| !reasonSelected);

			return;
		}

		if ("REJECT".equalsIgnoreCase(selectedAction)) {

			boolean reasonSelected =
					reasonCombobox != null
							&& reasonCombobox
									.getSelectedItem() != null;

			saveNextButton.setDisabled(
					!reasonSelected);

			return;
		}

		saveNextButton.setDisabled(true);
	}

	// ============================================================
	// SAVE & NEXT
	// ============================================================

	private void saveAndNext() {

		if (selectedAction == null) {

			Clients.showNotification(
					"Please select an action.",
					Clients.NOTIFICATION_TYPE_WARNING,
					null,
					"top_center",
					2500);

			return;
		}

		boolean validationPassed =
				"PASS".equalsIgnoreCase(cbsResult)
						&& "PASS".equalsIgnoreCase(
								chequeDateResult);

		if (("ACCEPT".equalsIgnoreCase(selectedAction)
				|| "SEND_BACK".equalsIgnoreCase(
						selectedAction))
				&& !validationPassed) {

			StringBuilder message =
					new StringBuilder();

			if (!"PASS".equalsIgnoreCase(cbsResult)) {

				message.append(
						processingService
								.getCbsValidationMessage(
										cbsResult));
			}

			if (!"PASS".equalsIgnoreCase(
					chequeDateResult)) {

				if (message.length() > 0) {
					message.append(" | ");
				}

				message.append(
						processingService
								.getCbsValidationMessage(
										chequeDateResult));
			}

			Clients.showNotification(
					message.toString(),
					Clients.NOTIFICATION_TYPE_ERROR,
					null,
					"top_center",
					4000);

			return;
		}

		String reasonCode = null;

		if (("REJECT".equalsIgnoreCase(selectedAction)
				|| "SEND_BACK".equalsIgnoreCase(
						selectedAction))
				&& reasonCombobox != null
				&& reasonCombobox.getSelectedItem() != null) {

			Object value =
					reasonCombobox
							.getSelectedItem()
							.getValue();

			if (value != null) {
				reasonCode = value.toString();
			}
		}

		String remarks = null;

		if (checkerRemarksTextbox != null) {
			remarks = checkerRemarksTextbox.getValue();
		}

		if (cheques == null
				|| cheques.isEmpty()
				|| currentChequeIndex < 0
				|| currentChequeIndex >= cheques.size()) {
			return;
		}

		OutwardCheque cheque =
				cheques.get(currentChequeIndex);

		boolean saved =
				processingService.saveCheckerDecision(
						batchNumber,
						cheque.getChequeNumber(),
						checkerUserId,
						selectedAction,
						reasonCode,
						remarks);

		if (!saved) {

			Clients.showNotification(
					"Unable to save checker decision.",
					Clients.NOTIFICATION_TYPE_ERROR,
					null,
					"top_center",
					3500);

			return;
		}

		boolean lastCheque =
				currentChequeIndex >= cheques.size() - 1;

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

		Clients.showNotification(
				reVerifyMode
						? "Re-verification completed."
						: "Checker processing completed.",
				Clients.NOTIFICATION_TYPE_INFO,
				null,
				"top_center",
				2500);

		goBackToQueue();
	}

	// ============================================================
	// BACK TO QUEUE
	// ============================================================

	private void goBackToQueue() {

		Executions.sendRedirect(
				"/zul/outward/outward-checker/batchesQueue.zul");
	}

	// ============================================================
	// FRONT IMAGE
	// ============================================================

	private void showFrontImage() {

		if (cheques == null || cheques.isEmpty()) {
			return;
		}

		OutwardCheque cheque =
				cheques.get(currentChequeIndex);

		if (cheque.getFrontImagePath() == null
				|| cheque.getFrontImagePath().trim().isEmpty()) {

			chequeImage.setSrc("");

			return;
		}

		chequeImage.setSrc(
				cheque.getFrontImagePath());

		resetImageState();

		if (frontButton != null) {
			frontButton.setSclass(
					"image-side-button selected");
		}

		if (backSideButton != null) {
			backSideButton.setSclass(
					"image-side-button");
		}
	}

	// ============================================================
	// BACK IMAGE
	// ============================================================

	private void showBackImage() {

		if (cheques == null || cheques.isEmpty()) {
			return;
		}

		OutwardCheque cheque =
				cheques.get(currentChequeIndex);

		if (cheque.getBackImagePath() == null
				|| cheque.getBackImagePath().trim().isEmpty()) {

			chequeImage.setSrc("");

			return;
		}

		chequeImage.setSrc(
				cheque.getBackImagePath());

		resetImageState();

		if (frontButton != null) {
			frontButton.setSclass(
					"image-side-button");
		}

		if (backSideButton != null) {
			backSideButton.setSclass(
					"image-side-button selected");
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

			chequeImage.setStyle(
					"transform: rotate("
							+ imageRotation
							+ "deg) scale("
							+ imageScale
							+ ");");
		}

		if (zoomLevelLabel != null) {

			zoomLevelLabel.setValue(
					Math.round(imageScale * 100)
							+ "%");
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

		Clients.showNotification(
				message,
				Clients.NOTIFICATION_TYPE_ERROR,
				null,
				"top_center",
				3000);
	}
}