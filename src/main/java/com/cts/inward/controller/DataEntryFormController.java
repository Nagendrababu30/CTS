package com.cts.inward.controller;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import com.cts.inward.dao.ChequeDao;
import com.cts.inward.dao.ChequeDaoImpl;
import com.cts.inward.model.InwardCheque;

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

	// =========================================================
	// DATA
	// =========================================================

	private long batchId;

	private List<InwardCheque> cheques;

	private int currentIndex = 0;

	// =========================================================
	// DAO
	// =========================================================

	private ChequeDao chequeDao;

	// =========================================================
	// PAGE INITIALIZATION
	// =========================================================

	@Override
	public void doAfterCompose(Component comp) throws Exception {

		super.doAfterCompose(comp);

		chequeDao = ChequeDaoImpl.of();

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

	// =========================================================
	// LOAD CHEQUES FOR SELECTED BATCH
	// =========================================================

	private void loadCheques() {

		try {

			cheques = chequeDao.getChequesForBatch(String.valueOf(batchId));

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
	//
	// For now this only moves to the next cheque.
	// Database save will be implemented after the
	// Data Entry update DAO/service is created.
	//
	// =========================================================

	public void onClick$btnSaveNext() {

		if (cheques == null || cheques.isEmpty()) {
			return;
		}

		if (currentIndex < cheques.size() - 1) {

			currentIndex++;

			displayCurrentCheque();

		} else {

			Messagebox.show("You have reached the last cheque in this batch.", "Data Entry", Messagebox.OK,
					Messagebox.INFORMATION);
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
		// SAVE & NEXT
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