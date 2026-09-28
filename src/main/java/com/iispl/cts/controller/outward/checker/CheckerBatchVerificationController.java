package com.iispl.cts.controller.outward.checker;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zk.ui.util.Clients;

import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.service.outward.checker.CheckerBatchService;

public class CheckerBatchVerificationController extends SelectorComposer<Component> {

	private static final long serialVersionUID = 1L;

	@Wire
	private Label batchIdLabel;

	@Wire
	private Label totalChequeLabel;

	@Wire
	private Label acceptedChequeLabel;

	@Wire
	private Label rejectedChequeLabel;

	@Wire
	private Label pendingChequeLabel;

	@Wire
	private Listbox chequeListbox;

	private CheckerBatchService batchService;

	private String batchId;
	private List<OutwardCheque> chequeList;
	private long currentUserId;

	// Initializes the page, validates the session, and loads the requested batch.
	@Override
	public void doAfterCompose(Component comp) throws Exception {

		super.doAfterCompose(comp);

		Session session = Executions.getCurrent().getSession();

		if (session == null) {
			Executions.sendRedirect("/zul/login.zul");
			return;
		}

		Object sessionUserId = session.getAttribute("userId");

		if (sessionUserId == null) {
			Executions.sendRedirect("/zul/login.zul");
			return;
		}

		if (sessionUserId instanceof Number) {
			currentUserId = ((Number) sessionUserId).longValue();
		} else {
			try {
				currentUserId = Long.parseLong(sessionUserId.toString());
			} catch (NumberFormatException e) {
				Executions.sendRedirect("/zul/login.zul");
				return;
			}
		}

		System.out.println(
				"Checker Batch Verification - User ID = " + currentUserId);

		batchService = new CheckerBatchService();

		batchId = Executions.getCurrent().getParameter("batchId");

		if (batchId == null || batchId.trim().isEmpty()) {
			goBackToQueue();
			return;
		}

		loadBatch();
	}

	// Loads the selected batch and displays its cheque details.
	private void loadBatch() {

		try {

			System.out.println(
					"==========================================");

			System.out.println(
					"LOADING BATCH = " + batchId);

			System.out.println(
					"CURRENT USER ID = " + currentUserId);

			System.out.println(
					"==========================================");

			chequeList = batchService.getChequesByBatchNumber(batchId);

			if (chequeList == null) {
				chequeList = new ArrayList<>();
			}

			System.out.println(
					"TOTAL CHEQUES FOUND = " + chequeList.size());

			batchIdLabel.setValue(batchId);

			updateSummary();

			displayCheques();

		} catch (Exception e) {

			e.printStackTrace();

			Clients.showNotification(
					"Unable to load batch verification.",
					Clients.NOTIFICATION_TYPE_ERROR,
					null,
					"top_center",
					4000);
		}
	}

	// Updates the total, accepted, rejected, and pending cheque counts.
	private void updateSummary() {

		int total = chequeList.size();
		int accepted = 0;
		int rejected = 0;
		int pending = 0;

		for (OutwardCheque cheque : chequeList) {

			String status = cheque.getChequeStatus();

			if (status == null || status.trim().isEmpty()) {
				pending++;

			} else if (status.equalsIgnoreCase("ACCEPTED")
					|| status.equalsIgnoreCase("ACCEPT")
					|| status.equalsIgnoreCase("VERIFIED")) {

				accepted++;

			} else if (status.equalsIgnoreCase("REJECTED")
					|| status.equalsIgnoreCase("REJECT")) {

				rejected++;

			} else {
				pending++;
			}
		}

		totalChequeLabel.setValue(String.valueOf(total));
		acceptedChequeLabel.setValue(String.valueOf(accepted));
		rejectedChequeLabel.setValue(String.valueOf(rejected));
		pendingChequeLabel.setValue(String.valueOf(pending));
	}

	// Displays all cheques in the batch with their details and action button.
	private void displayCheques() {

		ListModelList<OutwardCheque> model = new ListModelList<>();
		model.addAll(chequeList);

		chequeListbox.setModel(model);

		chequeListbox.setItemRenderer(
				new ListitemRenderer<OutwardCheque>() {

					@Override
					public void render(
							Listitem item,
							OutwardCheque cheque,
							int index) {

						item.appendChild(
								new Listcell(
										String.valueOf(index + 1)));

						item.appendChild(
								new Listcell(
										safe(cheque.getChequeNumber())));

						item.appendChild(
								new Listcell(
										safe(cheque.getDrawerAccountNumber())));

						String amountValue = "-";

						if (cheque.getAmount() != null) {
							amountValue =
									cheque.getAmount().toString();
						}

						item.appendChild(
								new Listcell(amountValue));

						String dateValue = "-";

						if (cheque.getChequeDate() != null) {
							dateValue =
									cheque.getChequeDate().toString();
						}

						item.appendChild(
								new Listcell(dateValue));

						String micr =
								safe(cheque.getCityCode())
								+ "-"
								+ safe(cheque.getBankCode())
								+ "-"
								+ safe(cheque.getBranchCode());

						item.appendChild(
								new Listcell(micr));

						String status =
								getDisplayStatus(cheque);

						item.appendChild(
								new Listcell(status));

						Listcell actionCell =
								new Listcell();

						Button openButton =
								new Button();

						openButton.setLabel("OPEN");
						openButton.setSclass("primary-button");

						final String currentBatchId = batchId;
						final String currentChequeNumber =
								cheque.getChequeNumber();

						openButton.addEventListener(
								"onClick",
								event -> {

									String url =
											"/outward/checker/"
											+ "chequeVerification.zul"
											+ "?batchId="
											+ currentBatchId
											+ "&chequeNumber="
											+ currentChequeNumber;

									System.out.println(
											"OPENING CHEQUE URL = "
											+ url);

									Executions.sendRedirect(url);
								});

						actionCell.appendChild(openButton);
						item.appendChild(actionCell);
					}
				});
	}

	// Returns the cheque status for display in the list.
	private String getDisplayStatus(OutwardCheque cheque) {

		String status = cheque.getChequeStatus();

		if (status == null || status.trim().isEmpty()) {
			return "PENDING";
		}

		return status.toUpperCase();
	}

	// Returns "-" when the value is null or empty.
	private String safe(String value) {

		if (value == null || value.trim().isEmpty()) {
			return "-";
		}

		return value;
	}

	// Returns to the checker batch queue.
	@Listen("onClick=#backButton")
	public void backButton() {

		goBackToQueue();
	}

	// Returns to the checker batch queue from the bottom button.
	@Listen("onClick=#backButtonBottom")
	public void backButtonBottom() {

		goBackToQueue();
	}

	// Redirects the checker to the batch queue.
	private void goBackToQueue() {

		Executions.sendRedirect(
				"/outward/checker/batchesQueue.zul");
	}

	// Opens the checker dashboard.
	@Listen("onClick=#dashboardButton")
	public void openDashboard() {

		navigate(
				"/outward/checker/dashboard.zul");
	}

	// Opens the checker batch queue.
	@Listen("onClick=#queueButton")
	public void openQueue() {

		navigate(
				"/outward/checker/batchesQueue.zul");
	}

	// Opens the checker reports page.
	@Listen("onClick=#reportsButton")
	public void openReports() {

		navigate(
				"/outward/checker/reports.zul");
	}

	// Opens the NPCI transmission page.
	@Listen("onClick=#npciButton")
	public void openNPCI() {

		navigate(
				"/outward/checker/sendToNPCI.zul");
	}

	// Redirects to the requested page.
	private void navigate(String page) {

		Executions.sendRedirect(page);
	}
}