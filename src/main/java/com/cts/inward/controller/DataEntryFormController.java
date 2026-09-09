package com.cts.inward.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import com.cts.inward.dao.ChequeDaoImpl;
import com.cts.inward.model.InwardCheque;
import com.cts.inward.service.BatchService;
import com.cts.inward.service.ChequeService;
import com.cts.inward.service.ChequeServiceImpl;

public class DataEntryFormController extends GenericForwardComposer<Component> {

	private static final long serialVersionUID = 1L;

	// =========================================================
	// ZUL COMPONENTS
	// =========================================================

	private Button btnBackQueue;
	private Button btnPrev;
	private Button btnSaveNext;

	private Button btnSideFront;
	private Button btnSideBack;
	private Button btnZoomIn;
	private Button btnZoomOut;
	private Button btnRotate;

	private Label lblBatchInfo;
	private Label lblChequeInfo;
	private Label lblMicrBand;

	private Textbox txtChequeNo;
	private Textbox txtAccountNo;

	private Decimalbox decAmount;

	private Datebox dtChequeDate;

	// DATA
	private long batchId;
	private List<InwardCheque> cheques;
	private int currentIndex = 0;

	// SERVICE
	private ChequeService chequeService;
	private BatchService batchService;

	// PAGE INITIALIZATION

	@Override
	public void doAfterCompose(Component comp) throws Exception {

		super.doAfterCompose(comp);

		/* Controller talks to Service. Service talks to DAO. */

		chequeService = ChequeServiceImpl.of(ChequeDaoImpl.of());
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

		loadCheques();

		if (cheques != null && !cheques.isEmpty()) {

			currentIndex = 0;

			displayCurrentCheque();

		} else {

			Messagebox.show("No cheques found for Batch " + batchId + ".", "Data Entry", Messagebox.OK,
					Messagebox.INFORMATION);
		}
	}

	// LOAD CHEQUES

	private void loadCheques() {
		try {

			/*
			 * Controller -> Service
			 */
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
		// HEADER
		// -----------------------------------------------------

		lblBatchInfo.setValue("Inward Data Entry - Batch " + batchId);

		lblChequeInfo.setValue("Cheque " + (currentIndex + 1) + " of " + cheques.size());

		// -----------------------------------------------------
		// CHEQUE NUMBER
		// -----------------------------------------------------

		txtChequeNo.setValue(safeString(cheque.getChequeNumber()));

		// -----------------------------------------------------
		// ACCOUNT NUMBER
		// -----------------------------------------------------

		txtAccountNo.setValue(safeString(cheque.getAccountNumber()));

		// -----------------------------------------------------
		// CHEQUE AMOUNT
		// -----------------------------------------------------

		if (cheque.getAmount() != null) {

			decAmount.setValue(cheque.getAmount());

		} else {

			decAmount.setRawValue("");
		}

		// -----------------------------------------------------
		// CHEQUE DATE
		// -----------------------------------------------------

		if (cheque.getChequeDate() != null) {

			dtChequeDate.setValue(java.sql.Date.valueOf(cheque.getChequeDate()));

		} else {

			dtChequeDate.setValue(null);
		}

		// -----------------------------------------------------
		// MICR
		// -----------------------------------------------------

		lblMicrBand.setValue(safeString(cheque.getMicrCode()));

		// -----------------------------------------------------
		// NAVIGATION
		// -----------------------------------------------------

		updateNavigationButtons();
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
	// SAVE & NEXT
	// =========================================================
	public void onClick$btnSaveNext() {

		if (cheques == null || cheques.isEmpty()) {

			return;
		}

		// ---------------------------------------------------------
		// Validate current cheque
		// ---------------------------------------------------------

		String accountNumber = txtAccountNo.getValue();

		BigDecimal amount = decAmount.getValue();

		Date chequeDateValue = dtChequeDate.getValue();

		if (accountNumber == null || accountNumber.trim().isEmpty()) {

			Messagebox.show("Account Number is required.", "Data Entry", Messagebox.OK, Messagebox.EXCLAMATION);

			return;
		}

		if (amount == null) {

			Messagebox.show("Cheque Amount is required.", "Data Entry", Messagebox.OK, Messagebox.EXCLAMATION);

			return;
		}

		if (chequeDateValue == null) {

			Messagebox.show("Cheque Date is required.", "Data Entry", Messagebox.OK, Messagebox.EXCLAMATION);

			return;
		}

		// ---------------------------------------------------------
		// Get logged-in user
		// ---------------------------------------------------------

		Session session = Executions.getCurrent().getSession();

		Object userIdObject = session.getAttribute("userId");

		if (userIdObject == null) {

			Messagebox.show("User session has expired. Please login again.", "Data Entry", Messagebox.OK,
					Messagebox.ERROR);

			return;
		}

		long userId = ((Number) userIdObject).longValue();

		// ---------------------------------------------------------
		// Current cheque
		// ---------------------------------------------------------

		InwardCheque currentCheque = cheques.get(currentIndex);

		LocalDate chequeDate = new java.sql.Date(chequeDateValue.getTime()).toLocalDate();

		try {

			// -----------------------------------------------------
			// 1. Save cheque data
			// 2. Mark cheque DATA_ENTRY_COMPLETED
			// -----------------------------------------------------

			chequeService.updateDataEntryCheque(currentCheque.getChequeNumber(), batchId, accountNumber.trim(), amount,
					chequeDate, userId);

			// -----------------------------------------------------
			// If this is NOT the last cheque
			// -----------------------------------------------------

			if (currentIndex < cheques.size() - 1) {

				currentIndex++;

				displayCurrentCheque();

				return;
			}

			// -----------------------------------------------------
			// This was the LAST cheque
			// -----------------------------------------------------

			boolean completed = batchService.completeDataEntry(batchId, userId);

			if (!completed) {

				Messagebox.show(
						"The batch cannot be submitted because " + "one or more cheques are still pending Data Entry.",
						"Data Entry", Messagebox.OK, Messagebox.EXCLAMATION);

				return;
			}

			// -----------------------------------------------------
			// Batch is now DATA_ENTRY_COMPLETED
			// -----------------------------------------------------

			Messagebox.show("Batch " + batchId + " has been completed and is ready for Checker.",
					"Data Entry Completed", Messagebox.OK, Messagebox.INFORMATION,
					event -> Executions.sendRedirect("/zul/inward-maker/send-to-checker.zul"));

		} catch (RuntimeException e) {

			e.printStackTrace();

			Messagebox.show("Unable to save Data Entry for cheque " + currentCheque.getChequeNumber() + ".",
					"Data Entry", Messagebox.OK, Messagebox.ERROR);
		}
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

			btnPrev.setDisabled(true);
			btnSaveNext.setDisabled(true);

			return;
		}

		// -----------------------------------------------------
		// PREVIOUS
		// -----------------------------------------------------

		btnPrev.setDisabled(currentIndex == 0);

		// -----------------------------------------------------
		// SAVE / SAVE & NEXT
		// -----------------------------------------------------

		if (currentIndex == cheques.size() - 1) {

			btnSaveNext.setLabel("Save");

		} else {

			btnSaveNext.setLabel("Save & Next →");
		}
	}

	// =========================================================
	// NULL SAFE STRING
	// =========================================================

	private String safeString(String value) {

		return value == null ? "" : value;
	}
}