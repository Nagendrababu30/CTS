package com.iispl.cts.controller.outward.checker;

import java.util.List;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Vlayout;

import com.cts.admin.model.User;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;
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

	private CheckerBatchService batchService;
	private CheckerProcessingService processingService;

	private String batchNumber;
	private long checkerUserId;

	private OutwardBatch currentBatch;
	private List<OutwardCheque> cheques;

	@Override
	public void doAfterCompose(Vlayout comp) throws Exception {

		super.doAfterCompose(comp);

		batchService = new CheckerBatchService();
		processingService = new CheckerProcessingService();

		User currentUser = (User) Sessions.getCurrent().getAttribute("loggedInUser");

		if (currentUser == null) {
			Executions.sendRedirect(Executions.getCurrent().getContextPath() + "/login.zul");
			return;
		}

		checkerUserId = currentUser.getUserId();

		batchNumber = Executions.getCurrent().getParameter("batchNumber");

		if (batchNumber == null || batchNumber.trim().isEmpty()) {
			return;
		}

		batchNumber = batchNumber.trim();

		loadBatch();
		loadFirstCheque();

		backButton.addEventListener("onClick", event -> goBackToQueue());
	}

	private void loadBatch() {

		currentBatch = batchService.findBatch(batchNumber);

		if (currentBatch == null) {
			return;
		}

		verificationBatchId.setValue("Batch: " + currentBatch.getBatchNumber());

		chequeSequence.setValue("Cheque 1 of " + currentBatch.getNumberOfCheques());
	}

	private void loadFirstCheque() {

		cheques = batchService.getChequesByBatchNumber(batchNumber);

		if (cheques == null || cheques.isEmpty()) {
			return;
		}

		OutwardCheque cheque = cheques.get(0);

		displayCheque(cheque);
	}

	private void displayCheque(OutwardCheque cheque) {

		chequeNumberLabel.setValue(valueOrDash(cheque.getChequeNumber()));

		/*
		 * CBS account mapping will be finalized separately. For now we display the
		 * account field without performing CBS validation.
		 */
		accountNumberLabel.setValue(valueOrDash(cheque.getPayeeAccountNumber()));

		drawerNameLabel.setValue(valueOrDash(cheque.getDrawerName()));

		payeeNameLabel.setValue(valueOrDash(cheque.getPayeeName()));

		amountLabel.setValue(cheque.getAmount() != null ? cheque.getAmount().toString() : "-");

		amountInWordsLabel.setValue(valueOrDash(cheque.getAmountInWords()));

		chequeDateLabel.setValue(cheque.getChequeDate() != null ? cheque.getChequeDate().toString() : "-");
	}

	private String valueOrDash(String value) {

		if (value == null || value.trim().isEmpty()) {
			return "-";
		}

		return value;
	}

	private void goBackToQueue() {

		String url = Executions.getCurrent().getContextPath() + "/zul/outward/outward-checker/batchesQueue.zul";

		Executions.sendRedirect(url);
	}
}