package com.cts.inward.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Window;

import com.cts.admin.model.User;
import com.cts.inward.dao.BatchDaoImpl;
import com.cts.inward.dao.ChequeDaoImpl;
import com.cts.inward.dao.ChequeImageDaoImpl;
import com.cts.inward.model.ChequeImage;
import com.cts.inward.model.InwardBatch;
import com.cts.inward.model.InwardCheque;
import com.cts.inward.service.BatchService;
import com.cts.inward.service.BatchServiceImpl;
import com.cts.inward.service.ChequeService;
import com.cts.inward.service.ChequeServiceImpl;

public class DataEntryFormController extends GenericForwardComposer<Component> {

	private static final long serialVersionUID = 1L;

	// =========================================================
	// ZUL COMPONENTS
	// =========================================================

	private Button btnBackQueue;
	private Button btnPrev;
	private Button btnNext;
	private Button btnReturn;
	private Button btnSaveNext;

	private Window returnWindow;
	private Vlayout returnReasonsContainer;
	private Textbox txtReturnRemarks;
	private Button btnCancelReturn;
	private Button btnConfirmReturn;

	private Button btnSideToggle;
	private Button btnSideFront;
	private Button btnSideBack;
	private Button btnZoomIn;
	private Button btnZoomOut;
	private Button btnRotate;
	private Button btnResetView;

	private Label lblBatchInfo;
	private Label lblTotalCheques;
	private Label lblCompletedCheques;
	private Label lblPendingCheques;
	private Label lblChequeInfo;
	private Label lblMicrBand;

	private org.zkoss.zul.Div returnReasonBanner;
	private Label lblReturnReason;
	private Label lblReturnRemarks;
	private org.zkoss.zul.Hlayout rowReturnRemarks;

	private Image imgCheque;

	private Textbox txtChequeNo;
	private Textbox txtAccountNo;
	private Decimalbox decAmount;
	private Textbox txtAmountInWords;
	private Datebox dtChequeDate;

	// =========================================================
	// DATA & IMAGE STATE
	// =========================================================

	private long batchId;
	private Long loggedInUserId;
	private List<InwardCheque> cheques;

	private int currentIndex = 0;

	private String currentFrontImagePath;
	private String currentBackImagePath;

	private boolean showingFront = true;
	private double currentScale = 1.0;
	private int currentRotation = 0;

	// =========================================================
	// SERVICE
	// =========================================================
	private BatchService batchService;
	private ChequeService chequeService;
	private ChequeImageDaoImpl chequeImageDao;

	// =========================================================
	// PAGE INITIALIZATION
	// =========================================================

	@Override
	public void doAfterCompose(Component comp) throws Exception {

		super.doAfterCompose(comp);
		loadLoggedInUser();

		chequeService = ChequeServiceImpl.of(ChequeDaoImpl.of());
		batchService = BatchServiceImpl.of(BatchDaoImpl.of());
		chequeImageDao = ChequeImageDaoImpl.of();

		String batchIdParameter = Executions.getCurrent().getParameter("batchId");

		if (batchIdParameter == null || batchIdParameter.trim().isEmpty()) {
			Messagebox.show("Batch ID is missing.", "Data Entry", Messagebox.OK, Messagebox.ERROR);
			return;
		}

		try {
			batchId = Long.parseLong(batchIdParameter);
		} catch (NumberFormatException e) {
			Messagebox.show("Invalid Batch ID: " + batchIdParameter, "Data Entry", Messagebox.OK, Messagebox.ERROR);
			return;
		}

		Long lockOwner = BatchDaoImpl.of().getBatchLockOwner(batchId);
		if (lockOwner == null || loggedInUserId == null || !lockOwner.equals(loggedInUserId)) {
			String msg = (lockOwner == null)
					? "This batch is not locked. Please lock the batch from the dashboard first."
					: "This batch is locked by another user.";
			Messagebox.show(
					msg,
					"Access Denied",
					Messagebox.OK,
					Messagebox.EXCLAMATION,
					e -> Executions.sendRedirect("/zul/inward-maker/dashboard.zul"));
			return;
		}

		if (returnWindow != null) {
			returnReasonsContainer = (Vlayout) returnWindow.getFellowIfAny("returnReasonsContainer");
			txtReturnRemarks = (Textbox) returnWindow.getFellowIfAny("txtReturnRemarks");
			btnCancelReturn = (Button) returnWindow.getFellowIfAny("btnCancelReturn");
			btnConfirmReturn = (Button) returnWindow.getFellowIfAny("btnConfirmReturn");

			if (btnCancelReturn != null) {
				btnCancelReturn.addEventListener(Events.ON_CLICK, event -> handleCancelReturn());
			}
			if (btnConfirmReturn != null) {
				btnConfirmReturn.addEventListener(Events.ON_CLICK, event -> handleConfirmReturn());
			}
			returnWindow.setVisible(false);
		}

		loadCheques();
		updateBatchSummaryCounts();

		if (cheques != null && !cheques.isEmpty()) {
			currentIndex = 0;
			displayCurrentCheque();
		} else {
			Messagebox.show("No cheques found for Batch " + batchId + ".", "Data Entry", Messagebox.OK,
					Messagebox.INFORMATION);
		}
	}

	private void loadLoggedInUser() {
		Session session = Executions.getCurrent().getSession();
		User user = (User) session.getAttribute("loggedInUser");
		if (user != null) {
			loggedInUserId = user.getUserId();
		}
	}

	private void updateBatchSummaryCounts() {

		try {
			InwardBatch batch = batchService.getBatch(String.valueOf(batchId));
			int total = (cheques != null) ? cheques.size() : (batch != null ? batch.getTotalCheques() : 0);
			int pending = batchService.getDataEntryPendingCount(batchId);
			int completed = Math.max(0, total - pending);

			if (lblTotalCheques != null) {
				lblTotalCheques.setValue("Total: " + total);
			}

			if (lblCompletedCheques != null) {
				lblCompletedCheques.setValue("Completed: " + completed);
			}

			if (lblPendingCheques != null) {
				lblPendingCheques.setValue("Pending: " + pending);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// =========================================================
	// LOAD CHEQUES
	// =========================================================

	private void loadCheques() {
		try {
			cheques = chequeService.getChequesForBatch(String.valueOf(batchId));
		} catch (RuntimeException e) {
			e.printStackTrace();
			Messagebox.show("Unable to load cheques for Batch " + batchId + ".", "Data Entry", Messagebox.OK,
					Messagebox.ERROR);
		}
	}

	// =========================================================
	// DISPLAY CURRENT CHEQUE
	// =========================================================

	private void displayCurrentCheque() {

		if (cheques == null || cheques.isEmpty()) {
			return;
		}

		if (currentIndex < 0 || currentIndex >= cheques.size()) {
			return;
		}

		InwardCheque cheque = cheques.get(currentIndex);

		// -----------------------------------------------------
		// HEADER & METRICS
		// -----------------------------------------------------
		if (lblBatchInfo != null) {
			lblBatchInfo.setValue("Batch No : " + batchId);
		}

		updateBatchSummaryCounts();

		if (lblChequeInfo != null) {
			lblChequeInfo.setValue("Cheque " + (currentIndex + 1) + " of " + cheques.size());
		}

		boolean isReturned = isChequeReturnedByMaker(cheque.getChequeNumber());

		// -----------------------------------------------------
		// CHEQUE NUMBER
		// -----------------------------------------------------

		if (txtChequeNo != null) {
			txtChequeNo.setValue(safeString(cheque.getChequeNumber()));
			txtChequeNo.setReadonly(isReturned);
		}

		// -----------------------------------------------------
		// ACCOUNT NUMBER
		// -----------------------------------------------------

		if (txtAccountNo != null) {
			txtAccountNo.setValue(safeString(cheque.getAccountNumber()));
			txtAccountNo.setReadonly(isReturned);
		}

		// -----------------------------------------------------
		// CHEQUE AMOUNT & AMOUNT IN WORDS
		// -----------------------------------------------------

		if (decAmount != null) {
			if (cheque.getAmount() != null) {
				decAmount.setValue(cheque.getAmount());
			} else {
				decAmount.setRawValue("");
			}
			decAmount.setReadonly(isReturned);
		}

		// -----------------------------------------------------
		// CHEQUE DATE
		// -----------------------------------------------------

		if (dtChequeDate != null) {
			if (cheque.getChequeDate() != null) {
				dtChequeDate.setValue(java.sql.Date.valueOf(cheque.getChequeDate()));
			} else {
				dtChequeDate.setValue(null);
			}
			dtChequeDate.setDisabled(isReturned);
		}

		// -----------------------------------------------------
		// MICR (Null-safe check to prevent NullPointerException)
		// -----------------------------------------------------

		if (lblMicrBand != null) {
			lblMicrBand.setValue(safeString(cheque.getMicrCode()));
		}

		// -----------------------------------------------------
		// CHEQUE IMAGE
		// -----------------------------------------------------
		loadChequeImages(cheque.getChequeNumber());

		// -----------------------------------------------------
		// RETURN REASON BANNER (IF RETURNED FROM CHECKER)
		// -----------------------------------------------------
		updateReturnBanner(cheque.getChequeNumber());

		// -----------------------------------------------------
		// NAVIGATION
		// -----------------------------------------------------
		updateNavigationButtons();

		// Refresh amount in words on client side
		Clients.evalJavaScript("if (typeof updateAmountWordsFromInput === 'function') { var dec = zk.Widget.$('$decAmount'); if (dec) updateAmountWordsFromInput(dec.getInputNode ? dec.getInputNode() : dec.$n()); }");
	}

	private void updateReturnBanner(String chqNo) {
		if (returnReasonBanner == null) {
			return;
		}

		if (chqNo == null || chqNo.trim().isEmpty() || chequeService == null) {
			returnReasonBanner.setVisible(false);
			return;
		}

		java.util.Map<String, String> returnInfo = chequeService.getChequeReturnInfo(chqNo);
		if (returnInfo != null && "RETURN_TO_MAKER".equalsIgnoreCase(returnInfo.get("status"))) {
			returnReasonBanner.setVisible(true);

			String desc = returnInfo.get("returnReasonDescription");
			if (desc == null || desc.trim().isEmpty()) {
				desc = returnInfo.get("returnReasonCode");
			}
			if (lblReturnReason != null) {
				lblReturnReason.setValue(desc != null ? desc : "Returned to Maker by Checker");
			}

			String remarks = returnInfo.get("remarks");
			if (lblReturnRemarks != null) {
				lblReturnRemarks.setValue(remarks != null && !remarks.trim().isEmpty() ? remarks : "No remarks provided");
			}
		} else {
			returnReasonBanner.setVisible(false);
		}
	}

	// =========================================================
	// LIVE AMOUNT IN WORDS SYNCHRONIZATION
	// =========================================================

	public void onChange$decAmount(Event event) {
		handleAmountChange(event);
	}

	private void handleAmountChange(Event event) {
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
		} else if (decAmount != null) {
			enteredAmount = decAmount.getValue();
		}

		if (enteredAmount == null || enteredAmount.compareTo(BigDecimal.ZERO) <= 0) {
			if (txtAmountInWords != null) {
				txtAmountInWords.setValue("");
			}
			return;
		}

		String words = convertNumberToIndianWords(enteredAmount);
		if (txtAmountInWords != null) {
			txtAmountInWords.setValue(words);
		}
	}

	// =========================================================
	// PREVIOUS CHEQUE
	// =========================================================

	public void onClick$btnPrev() {
		if (cheques == null || cheques.isEmpty()) {
			return;
		}

		if (currentIndex > 0) {
			currentIndex--;
			displayCurrentCheque();
		}
	}

	// =========================================================
	// NEXT CHEQUE (WITHOUT SAVING)
	// =========================================================

	public void onClick$btnNext() {
		if (cheques == null || cheques.isEmpty()) {
			return;
		}

		if (currentIndex < cheques.size() - 1) {
			currentIndex++;
			displayCurrentCheque();
		}
	}

	// =========================================================
	// SAVE & NEXT
	// =========================================================

	public void onClick$btnSaveNext() {
		if (cheques == null || cheques.isEmpty()) {
			return;
		}

		if (loggedInUserId == null) {
			Messagebox.show("Unable to identify the logged-in user.", "Data Entry", Messagebox.OK, Messagebox.ERROR);
			return;
		}

		InwardCheque currentCheque = cheques.get(currentIndex);

		try {
			// 1. READ VALUES FROM SCREEN
			String enteredChequeNo = txtChequeNo != null ? txtChequeNo.getValue() : null;
			String accountNumber = txtAccountNo != null ? txtAccountNo.getValue() : null;
			BigDecimal amount = decAmount != null ? decAmount.getValue() : null;
			java.util.Date selectedDate = dtChequeDate != null ? dtChequeDate.getValue() : null;

			LocalDate chequeDate = null;
			if (selectedDate != null) {
				chequeDate = new java.sql.Date(selectedDate.getTime()).toLocalDate();
			}

			// Validation
			if (enteredChequeNo == null || enteredChequeNo.trim().isEmpty()) {
				Messagebox.show("Please enter Cheque Number.", "Validation", Messagebox.OK, Messagebox.EXCLAMATION);
				if (txtChequeNo != null) txtChequeNo.setFocus(true);
				return;
			}

			if (accountNumber == null || accountNumber.trim().isEmpty()) {
				Messagebox.show("Please enter Account Number.", "Validation", Messagebox.OK, Messagebox.EXCLAMATION);
				if (txtAccountNo != null) txtAccountNo.setFocus(true);
				return;
			}

			if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
				Messagebox.show("Please enter a valid Cheque Amount.", "Validation", Messagebox.OK, Messagebox.EXCLAMATION);
				if (decAmount != null) decAmount.setFocus(true);
				return;
			}

			if (chequeDate == null) {
				Messagebox.show("Please select a Cheque Date.", "Validation", Messagebox.OK, Messagebox.EXCLAMATION);
				if (dtChequeDate != null) dtChequeDate.setFocus(true);
				return;
			}

			// 2. SAVE ONLY CHANGED DATA ENTRY FIELDS
			if (!sameString(currentCheque.getChequeNumber(), enteredChequeNo)) {
				chequeService.saveDataEntryCorrections(currentCheque.getChequeNumber(), batchId, enteredChequeNo.trim(),
						null, null, null, loggedInUserId);
			}

			if (!sameString(currentCheque.getAccountNumber(), accountNumber)) {
				chequeService.saveDataEntryCorrections(currentCheque.getChequeNumber(), batchId, null, accountNumber,
						null, null, loggedInUserId);
			}

			if (!sameBigDecimal(currentCheque.getAmount(), amount)) {
				chequeService.saveDataEntryCorrections(currentCheque.getChequeNumber(), batchId, null, null, amount,
						null, loggedInUserId);
			}

			if (!sameLocalDate(currentCheque.getChequeDate(), chequeDate)) {
				chequeService.saveDataEntryCorrections(currentCheque.getChequeNumber(), batchId, null, null, null,
						chequeDate, loggedInUserId);
			}

			// 3. CHANGE CHEQUE STATUS ONLY IF NOT ALREADY RETURN_BY_MAKER
			if (!isChequeReturnedByMaker(currentCheque.getChequeNumber())) {
				chequeService.updateChequeStatus(currentCheque.getChequeNumber(), "DATA_ENTRY_COMPLETED", loggedInUserId);
			}
			updateBatchSummaryCounts();

			// 4. UPDATE IN-MEMORY CHEQUE SO NAVIGATING BACK REFLECTS SAVED VALUES
			InwardCheque updatedCheque = InwardCheque.of(
					(enteredChequeNo != null && !enteredChequeNo.trim().isEmpty()) ? enteredChequeNo.trim() : currentCheque.getChequeNumber(),
					currentCheque.getBatchId(),
					accountNumber,
					currentCheque.getDrawerName(),
					amount,
					currentCheque.getMicrCode(),
					chequeDate,
					currentCheque.getPresentingDate());
			cheques.set(currentIndex, updatedCheque);

			// Pop-up for 2 seconds saying "Changes saved"
			Clients.showNotification(
					"Changes saved",
					Clients.NOTIFICATION_TYPE_INFO,
					null,
					"top_right",
					2000);

			if (currentIndex == cheques.size() - 1) {
				batchService.completeDataEntry(batchId, loggedInUserId);

				String redirectUrl = Executions.encodeURL("/zul/inward-maker/send-to-checker.zul");
				Clients.evalJavaScript("setTimeout(function() { window.location.href = '" + redirectUrl + "'; }, 2000);");
			} else {
				currentIndex++;
				displayCurrentCheque();
			}

		} catch (Exception e) {
			e.printStackTrace();
			Messagebox.show("Unable to save Data Entry for cheque " + currentCheque.getChequeNumber() + ".",
					"Data Entry", Messagebox.OK, Messagebox.ERROR);
		}
	}

	// =========================================================
	// RETURN BY MAKER (DATA ENTRY)
	// =========================================================

	public void onClick$btnReturn() {
		openReturnWindow();
	}

	private void openReturnWindow() {
		if (returnWindow == null) {
			Messagebox.show("Return dialog unavailable.", "Return", Messagebox.OK, Messagebox.ERROR);
			return;
		}

		if (returnReasonsContainer != null) {
			returnReasonsContainer.getChildren().clear();
		}

		if (txtReturnRemarks != null) {
			txtReturnRemarks.setValue("");
		}

		List<com.cts.inward.dto.ReturnReasonDto> reasons = chequeService.getDataEntryReturnReasons();
		if (reasons != null && returnReasonsContainer != null) {
			for (com.cts.inward.dto.ReturnReasonDto r : reasons) {
				Checkbox cb = new Checkbox();
				cb.setLabel(r.getReturnReasonCode() + " - " + r.getDescription());
				cb.setAttribute("reasonCode", r.getReturnReasonCode());
				cb.setStyle("display:block; margin-bottom:6px; font-size:13.5px; color:#1E293B; cursor:pointer;");
				returnReasonsContainer.appendChild(cb);
			}
		}

		returnWindow.doModal();
	}

	private void handleCancelReturn() {
		if (returnWindow != null) {
			returnWindow.setVisible(false);
		}
	}

	private void handleConfirmReturn() {
		List<String> selectedCodes = new java.util.ArrayList<>();
		if (returnReasonsContainer != null) {
			for (Component comp : returnReasonsContainer.getChildren()) {
				if (comp instanceof Checkbox) {
					Checkbox cb = (Checkbox) comp;
					if (cb.isChecked()) {
						String code = (String) cb.getAttribute("reasonCode");
						if (code != null && !code.trim().isEmpty()) {
							selectedCodes.add(code.trim());
						}
					}
				}
			}
		}

		if (selectedCodes.isEmpty()) {
			Messagebox.show("Please select at least one return reason.", "Validation", Messagebox.OK, Messagebox.EXCLAMATION);
			return;
		}

		if (cheques == null || currentIndex < 0 || currentIndex >= cheques.size()) {
			Messagebox.show("No cheque selected.", "Validation", Messagebox.OK, Messagebox.EXCLAMATION);
			return;
		}

		InwardCheque currentCheque = cheques.get(currentIndex);
		String remarks = txtReturnRemarks != null ? txtReturnRemarks.getValue() : "";

		try {
			boolean ok = chequeService.saveMakerDataEntryReturn(currentCheque.getChequeNumber(), selectedCodes, remarks, loggedInUserId);
			if (!ok) {
				Messagebox.show("Unable to save return for cheque " + currentCheque.getChequeNumber() + ".", "Error", Messagebox.OK, Messagebox.ERROR);
				return;
			}

			if (returnWindow != null) {
				returnWindow.setVisible(false);
			}

			Clients.showNotification("Cheque " + currentCheque.getChequeNumber() + " marked as RETURN_BY_MAKER.",
					Clients.NOTIFICATION_TYPE_INFO, null, "top_right", 2000);

			updateBatchSummaryCounts();

			batchService.completeDataEntry(batchId, loggedInUserId);
			int pending = batchService.getDataEntryPendingCount(batchId);
			if (pending == 0) {
				String redirectUrl = Executions.encodeURL("/zul/inward-maker/send-to-checker.zul");
				Clients.evalJavaScript("setTimeout(function() { window.location.href = '" + redirectUrl + "'; }, 2000);");
			}

			if (currentIndex < cheques.size() - 1) {
				currentIndex++;
			}

			displayCurrentCheque();

		} catch (Exception e) {
			e.printStackTrace();
			Messagebox.show("Failed to return cheque: " + e.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR);
		}
	}

	private boolean isChequeReturnedByMaker(String chequeNumber) {
		if (chequeNumber == null || chequeNumber.trim().isEmpty() || chequeService == null) {
			return false;
		}
		java.util.Map<String, String> returnInfo = chequeService.getChequeReturnInfo(chequeNumber);
		return returnInfo != null && "RETURN_BY_MAKER".equalsIgnoreCase(returnInfo.get("status"));
	}

	// =========================================================
	// CHEQUE IMAGE LOADING & TRANSFORMS
	// =========================================================

	private void loadChequeImages(String chequeNumber) {
		currentFrontImagePath = null;
		currentBackImagePath = null;

		try {
			ChequeImage image = chequeImageDao.findByChequeNumber(chequeNumber);
			if (image != null) {
				currentFrontImagePath = image.getFrontPath();
				currentBackImagePath = image.getBackPath();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		showingFront = true;
		currentScale = 1.0;
		currentRotation = 0;

		if (btnSideToggle != null) {
			btnSideToggle.setLabel("View Back");
		}

		showFrontImage();
		applyImageStyle();
	}

	private java.io.File resolveImageFile(String path) {
		if (path == null || path.trim().isEmpty()) {
			return null;
		}

		String normalized = path.replace("\\", "/").trim();
		if (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}

		// 1. Direct file / absolute path
		java.io.File directFile = new java.io.File(path);
		if (directFile.isAbsolute() && directFile.isFile()) {
			return directFile;
		}

		// 2. Deployed webApp realPath
		try {
			if (org.zkoss.zk.ui.Executions.getCurrent() != null
					&& org.zkoss.zk.ui.Executions.getCurrent().getDesktop() != null) {
				org.zkoss.zk.ui.WebApp webApp =
						org.zkoss.zk.ui.Executions.getCurrent().getDesktop().getWebApp();
				String realPath = webApp.getRealPath("/" + normalized);
				if (realPath != null) {
					java.io.File realFile = new java.io.File(realPath);
					if (realFile.isFile()) {
						return realFile;
					}
				}
			}
		} catch (Exception ignored) {}

		// 3. Local workspace development path: src/main/webapp/ + subPath
		String subPath = normalized.startsWith("src/main/webapp/")
				? normalized.substring("src/main/webapp/".length())
				: normalized;
		java.io.File devFile = new java.io.File("src/main/webapp", subPath);
		if (devFile.isFile()) {
			return devFile;
		}

		// 4. Relative path as-is
		if (directFile.isFile()) {
			return directFile;
		}

		return null;
	}

	private void renderChequeImage(String imagePath) {
		if (imgCheque == null) return;
		if (imagePath == null || imagePath.trim().isEmpty()) {
			imgCheque.setContent((org.zkoss.image.AImage) null);
			imgCheque.setSrc("");
			return;
		}

		try {
			java.io.File file = resolveImageFile(imagePath);
			if (file != null) {
				imgCheque.setContent(new org.zkoss.image.AImage(file));
			} else {
				String clean = imagePath.replace("\\", "/").trim();
				if (clean.startsWith("/")) clean = clean.substring(1);
				if (clean.startsWith("src/main/webapp/")) {
					clean = clean.substring("src/main/webapp/".length());
				}
				String webSrc = "/" + clean;
				imgCheque.setContent((org.zkoss.image.AImage) null);
				imgCheque.setSrc(webSrc);
			}
		} catch (Exception e) {
			String clean = imagePath.replace("\\", "/").trim();
			if (clean.startsWith("/")) clean = clean.substring(1);
			if (clean.startsWith("src/main/webapp/")) {
				clean = clean.substring("src/main/webapp/".length());
			}
			String webSrc = "/" + clean;
			imgCheque.setContent((org.zkoss.image.AImage) null);
			imgCheque.setSrc(webSrc);
		}
	}

	private void showFrontImage() {
		if (imgCheque == null) return;
		renderChequeImage(currentFrontImagePath);
		applyImageStyle();
	}

	private void showBackImage() {
		if (imgCheque == null) return;
		renderChequeImage(currentBackImagePath);
		applyImageStyle();
	}

	private void applyImageStyle() {

		if (imgCheque != null) {
			imgCheque.setStyle(String.format(
					java.util.Locale.US,
					"object-fit:contain; max-width:100%%; max-height:100%%; display:block; margin:auto; transform: scale(%.2f) rotate(%ddeg); transform-origin: center; transition: transform 0.2s;",
					currentScale,
					currentRotation));
		}
	}

	public void onClick$btnSideToggle() {
		showingFront = !showingFront;
		if (btnSideToggle != null) {
			btnSideToggle.setLabel(showingFront ? "View Back" : "View Front");
		}
		if (showingFront) {
			showFrontImage();
		} else {
			showBackImage();
		}
		applyImageStyle();
	}

	public void onClick$btnSideFront() {
		showFrontImage();
		showingFront = true;
		if (btnSideToggle != null) {
			btnSideToggle.setLabel("View Back");
		}
		applyImageStyle();
	}

	public void onClick$btnSideBack() {
		showBackImage();
		showingFront = false;
		if (btnSideToggle != null) {
			btnSideToggle.setLabel("View Front");
		}
		applyImageStyle();
	}

	// =========================================================
	// ZOOM / ROTATE / RESET BUTTON HANDLERS
	// =========================================================

	public void onClick$btnZoomIn() {
		currentScale += 0.2;
		applyImageStyle();
	}

	public void onClick$btnZoomOut() {
		if (currentScale > 0.4) {
			currentScale -= 0.2;
			applyImageStyle();
		}
	}

	public void onClick$btnRotate() {
		currentRotation = (currentRotation + 90) % 360;
		applyImageStyle();
	}

	public void onClick$btnResetView() {
		currentScale = 1.0;
		currentRotation = 0;
		applyImageStyle();
	}

	// =========================================================
	// BACK TO DATA ENTRY QUEUE
	// =========================================================

	public void onClick$btnBackQueue() {
		Executions.sendRedirect("/zul/inward-maker/data-entry.zul");
	}

	// =========================================================
	// UPDATE NAVIGATION BUTTONS
	// =========================================================

	private void updateNavigationButtons() {
		if (cheques == null || cheques.isEmpty()) {
			if (btnPrev != null) btnPrev.setDisabled(true);
			if (btnNext != null) btnNext.setDisabled(true);
			if (btnReturn != null) btnReturn.setDisabled(true);
			if (btnSaveNext != null) btnSaveNext.setDisabled(true);
			return;
		}

		boolean isReturned = false;
		if (currentIndex >= 0 && currentIndex < cheques.size()) {
			isReturned = isChequeReturnedByMaker(cheques.get(currentIndex).getChequeNumber());
		}

		// PREVIOUS
		if (btnPrev != null) {
			btnPrev.setDisabled(currentIndex == 0);
		}

		// NEXT
		if (btnNext != null) {
			btnNext.setDisabled(currentIndex >= cheques.size() - 1);
		}

		// RETURN
		if (btnReturn != null) {
			btnReturn.setDisabled(isReturned);
		}

		// SAVE / SAVE & NEXT
		if (btnSaveNext != null) {
			btnSaveNext.setDisabled(isReturned);
			if (currentIndex == cheques.size() - 1) {
				btnSaveNext.setLabel("Save & Send to Checker");
			} else {
				btnSaveNext.setLabel("Save & Next →");
			}
		}
	}

	// =========================================================
	// INDIAN NUMBER TO WORDS CONVERSION
	// =========================================================

	public static String convertNumberToIndianWords(BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return "";

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
		return result.toString().toUpperCase();
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
		if (n / 1000 > 0) {
			words.append(convertToIndianFormat(n / 1000)).append(" Thousand ");
			n %= 1000;
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

	// =========================================================
	// NULL SAFE & COMPARISON HELPERS
	// =========================================================

	private String safeString(String value) {
		return value == null ? "" : value;
	}

	private boolean sameString(String oldValue, String newValue) {
		String oldText = oldValue == null ? "" : oldValue.trim();
		String newText = newValue == null ? "" : newValue.trim();
		return oldText.equals(newText);
	}

	private boolean sameBigDecimal(BigDecimal oldValue, BigDecimal newValue) {
		if (oldValue == null && newValue == null) {
			return true;
		}
		if (oldValue == null || newValue == null) {
			return false;
		}
		return oldValue.compareTo(newValue) == 0;
	}

	private boolean sameLocalDate(LocalDate oldValue, LocalDate newValue) {
		if (oldValue == null && newValue == null) {
			return true;
		}
		if (oldValue == null || newValue == null) {
			return false;
		}
		return oldValue.equals(newValue);
	}
}