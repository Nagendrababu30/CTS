package com.iispl.cts.controller.outward;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Vlayout;

import com.cts.admin.service.SessionService;
import com.cts.admin.service.SessionServiceImpl;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.service.outward.OutwardMakerMicrRepairService;

public class OutwardMakerMicrRepairController extends GenericForwardComposer<Component> {

	private static final long serialVersionUID = 1L;

	@Wire
	private Listbox batchListbox;

	@Wire
	private Vlayout checkerReturnInformationPanel;

	@Wire
	private Label checkerReasonLabel;

	@Wire
	private Label checkerRemarksLabel;

	private SessionService sessionService;
	private OutwardMakerMicrRepairService service;

	private boolean returnedMode = false;
	private String returnedChequeNumber;
	private String repairType;

	@Override
	public void doAfterCompose(Component comp) throws Exception {

		super.doAfterCompose(comp);

		service = new OutwardMakerMicrRepairService();

		// CHECK WHETHER THIS IS RETURNED MODE
		String returnMode = Executions.getCurrent().getParameter("returnMode");

		if (returnMode == null || returnMode.trim().isEmpty()) {
			returnMode = Executions.getCurrent().getParameter("amp;returnMode");
		}

		returnedMode = "RETURNED".equalsIgnoreCase(returnMode) || "HOLD".equalsIgnoreCase(returnMode);

		returnedChequeNumber = Executions.getCurrent().getParameter("chequeNumber");
		repairType = Executions.getCurrent().getParameter("repairType");

		setListItemRenderer();
		loadMicrErrorBatches();
	}

	private void setListItemRenderer() {

		if (batchListbox == null) {
			return;
		}

		batchListbox.setItemRenderer(new ListitemRenderer<OutwardBatch>() {

			@Override
			public void render(Listitem item, OutwardBatch batch, int index) {

				item.setValue(batch);
				item.appendChild(new Listcell(batch.getBatchNumber()));
				item.appendChild(new Listcell(String.valueOf(batch.getNumberOfCheques())));

				int micrErrorCount = service.getMicrErrorCount(batch.getBatchNumber());
				item.appendChild(new Listcell(String.valueOf(micrErrorCount)));

				Listcell statusCell = new Listcell();
				Label statusLabel = new Label("MICR REPAIR");
				statusLabel.setSclass("micr-repair-status");
				statusCell.appendChild(statusLabel);
				item.appendChild(statusCell);

				Listcell actionCell = new Listcell();
				Button openButton = new Button("OPEN");

				openButton.addEventListener("onClick", event -> openBatch(batch));

				actionCell.appendChild(openButton);
				item.appendChild(actionCell);
			}
		});
	}

	private void loadMicrErrorBatches() {
		Long userId = getLoggedInUserId();

		if (userId == null) {
			return;
		}

		sessionService = new SessionServiceImpl();
		com.cts.admin.model.Session clearingSession = sessionService.getActiveSession();

		if (clearingSession == null || clearingSession.getStatus() == null
				|| !"STARTED".equalsIgnoreCase(clearingSession.getStatus().trim())) {
			Messagebox.show(
					"Clearing session is not started.\n\n" + "Micr Repair operations are currently unavailable.",
					"Session Not Started", Messagebox.OK, Messagebox.EXCLAMATION, event -> {
						if (Messagebox.ON_OK.equals(event.getName())) {
							Executions.sendRedirect("/login.zul");
						}
					});

			return;
		}

		
		// DYNAMIC LOGGED-IN USER ID
		String currentUserId = String.valueOf(userId);

		System.out.println("OUTWARD MAKER MICR REPAIR");
		System.out.println("doAfterCompose() START");
		System.out.println("Current Maker User : " + currentUserId);
		System.out.println("Returned Mode      : " + returnedMode);
		System.out.println("Returned Cheque    : " + returnedChequeNumber);

		
		List<OutwardBatch> batches = service.getMicrErrorBatches(userId);
		System.out.println("MICR REPAIR - BATCHES FOUND = " + (batches == null ? 0 : batches.size()));

		if (batches != null) {
			for (OutwardBatch batch : batches) {
				System.out.println("MICR REPAIR - BATCH = " + batch.getBatchNumber());
			}
		}

		ListModelList<OutwardBatch> model = new ListModelList<>(batches);
		if (batchListbox != null) {
			batchListbox.setModel(model);
		}
	}

	
	// GET LOGGED-IN USER FROM SESSION
	private Long getLoggedInUserId() {
		Session sessionUser = Executions.getCurrent().getSession();

		if (sessionUser == null) {
			System.out.println("No logged-in user session found.");
			Executions.sendRedirect("/zul/login.zul");
			return null;
		}

		Object sessionUserId = sessionUser.getAttribute("userId");

		if (sessionUserId == null) {
			System.out.println("No logged-in user ID found in session.");
			Executions.sendRedirect("/zul/login.zul");
			return null;
		}

		if (sessionUserId instanceof Number) {
			return ((Number) sessionUserId).longValue();
		}

		try {
			return Long.parseLong(sessionUserId.toString());

		} catch (NumberFormatException e) {
			System.out.println("Invalid userId in session: " + sessionUserId);
			Executions.sendRedirect("/zul/login.zul");
			return null;
		}
	}

	private void openBatch(OutwardBatch batch) {
		if (batch == null || batch.getBatchNumber() == null) {
			return;
		}

		String batchNumber = batch.getBatchNumber().trim();

		if (returnedMode) {
			StringBuilder url = new StringBuilder("outward-maker-micr-repair-detail.zul");

			url.append("?batchNumber=").append(batchNumber);
			url.append("&returnMode=HOLD");

			if (repairType != null && !repairType.trim().isEmpty()) {
				url.append("&repairType=").append(repairType.trim());
			}

			if (returnedChequeNumber != null && !returnedChequeNumber.trim().isEmpty()) {
				url.append("&chequeNumber=").append(returnedChequeNumber.trim());
			}

			System.out.println("OPENING RETURNED MICR BATCH");
			System.out.println("Batch Number : " + batchNumber);
			System.out.println("Return Mode  : RETURNED");
			System.out.println("Cheque       : " + returnedChequeNumber);
			System.out.println("URL          : " + url.toString());

			Executions.sendRedirect(url.toString());
			return;
		}

		Executions.sendRedirect("outward-maker-micr-repair-detail.zul" + "?batchNumber=" + batchNumber);
	}
}