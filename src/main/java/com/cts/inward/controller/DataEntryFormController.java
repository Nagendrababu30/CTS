package com.cts.inward.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import com.cts.admin.model.User;
import com.cts.inward.dao.BatchDaoImpl;
import com.cts.inward.dao.ChequeDaoImpl;
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
	private Long loggedInUserId;
	private List<InwardCheque> cheques;

	private int currentIndex = 0;

	// =========================================================
	// SERVICE
	// =========================================================
	private BatchService batchService;
	private ChequeService chequeService;

	// =========================================================
	// PAGE INITIALIZATION
	// =========================================================

	@Override
	public void doAfterCompose(Component comp) throws Exception {

		super.doAfterCompose(comp);
		loadLoggedInUser();
		/*
		 * Controller talks to Service. Service talks to DAO.
		 */
		chequeService = ChequeServiceImpl.of(ChequeDaoImpl.of());
		batchService = BatchServiceImpl.of(BatchDaoImpl.of());
		
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

	private void loadLoggedInUser() {

		Session session = Executions.getCurrent().getSession();

		User user = (User) session.getAttribute("loggedInUser");

		if (user != null) {
			loggedInUserId = user.getUserId();
		}
	}
	// =========================================================
	// LOAD CHEQUES
	// =========================================================

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

		if (loggedInUserId == null) {

			Messagebox.show("Unable to identify the logged-in user.", "Data Entry", Messagebox.OK, Messagebox.ERROR);

			return;
		}

		InwardCheque currentCheque = cheques.get(currentIndex);

		try {

			// 1. READ VALUES FROM SCREEN

			String accountNumber = txtAccountNo.getValue();

			BigDecimal amount = decAmount.getValue();

			java.util.Date selectedDate = dtChequeDate.getValue();

			LocalDate chequeDate = null;

			if (selectedDate != null) {
				chequeDate = new java.sql.Date(selectedDate.getTime()).toLocalDate();
			}

			/*
			 * --------------------------------------------------------- 2. SAVE ONLY
			 * CHANGED DATA ENTRY FIELDS
			 * ---------------------------------------------------------
			 */

			if (!sameString(currentCheque.getAccountNumber(), accountNumber)) {

				chequeService.saveDataEntryCorrections(currentCheque.getChequeNumber(), batchId, accountNumber, null,
						null, loggedInUserId);
			}

			if (!sameBigDecimal(currentCheque.getAmount(), amount)) {

				chequeService.saveDataEntryCorrections(currentCheque.getChequeNumber(), batchId, null, amount, null,
						loggedInUserId);
			}

			if (!sameLocalDate(currentCheque.getChequeDate(), chequeDate)) {

				chequeService.saveDataEntryCorrections(currentCheque.getChequeNumber(), batchId, null, null, chequeDate,
						loggedInUserId);
			}

		
		//	 3. CHANGE CHEQUE STATUS 

			chequeService.updateChequeStatus(currentCheque.getChequeNumber(), "DATA_ENTRY_COMPLETED", loggedInUserId);
			
			if (currentIndex == cheques.size() - 1) {

			    boolean completed = batchService.completeDataEntry(
			        batchId,
			        loggedInUserId
			    );

			    if (completed) {
			        Clients.showNotification(
			            "Batch completed and ready for Checker.",
			            Clients.NOTIFICATION_TYPE_INFO,
			            null,
			            "top_center",
			            3000
			        );
			    }
			}

		// 4. MOVE TO NEXT CHEQUE
			
			if (currentIndex < cheques.size() - 1) {

				currentIndex++;

				displayCurrentCheque();

			} else {

				Messagebox.show("Data Entry completed for this cheque.", "Data Entry", Messagebox.OK,
						Messagebox.INFORMATION);
			}

		} catch (Exception e) {

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
	
			// PREVIOUS
			btnPrev.setDisabled(currentIndex == 0);
	
			// SAVE / SAVE & NEXT
		
			if (currentIndex == cheques.size() - 1) {
			    btnSaveNext.setLabel("Save & Submit to Checker");
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

	// helper methods to check the cheque fields are changed or not

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