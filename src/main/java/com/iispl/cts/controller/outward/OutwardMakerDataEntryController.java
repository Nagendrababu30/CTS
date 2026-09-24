package com.iispl.cts.controller.outward;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;

import com.cts.admin.service.SessionService;
import com.cts.admin.service.SessionServiceImpl;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.service.outward.OutwardMakerDataEntryService;


public class OutwardMakerDataEntryController
extends SelectorComposer<Component> {

	private static final long serialVersionUID = 1L;
	@Wire
	private Paging batchPaging;
	@Wire
	private Listbox batchListbox;

	private SessionService sessionService;


	private final  OutwardMakerDataEntryService dataEntryService =
			new  OutwardMakerDataEntryService();

	@Override
	public void doAfterCompose(Component comp) throws Exception {

		super.doAfterCompose(comp);

		// =====================================================
		// GET LOGGED-IN USER FROM SESSION
		// =====================================================

		Session sessionUser =
				Executions.getCurrent()
				.getSession();

		if (sessionUser == null) {

			System.out.println(
					"No logged-in user session found."
					);

			Executions.sendRedirect(
					"/zul/login.zul"
					);

			return;
		}
		
		 // =====================================================
	    // CHECK CLEARING SESSION
	    // =====================================================

	    sessionService = new SessionServiceImpl();

	    com.cts.admin.model.Session clearingSession =
	            sessionService.getActiveSession();

	    if (clearingSession == null
	            || clearingSession.getStatus() == null
	            || !"STARTED".equalsIgnoreCase(
	                    clearingSession.getStatus().trim()
	            )) {

	        Messagebox.show(
	                "Clearing session is not started.\n\n"
	                        + "Data Entry operations "
	                        + "are currently unavailable.",
	                "Session Not Started",
	                Messagebox.OK,
	                Messagebox.EXCLAMATION,
	                event -> {

						if (Messagebox.ON_OK.equals(
								event.getName())) {

							Executions.sendRedirect("/login.zul");
						}
					}
	        );

	        return;
	    }
		// =====================================================
		// DASHBOARD RETURNED-REPAIR ROUTING
		//
		// The dashboard can open this ZUL with:
		//
		// batchNumber=...
		// returnMode=HOLD
		// repairType=DATA_ENTRY
		// chequeNumber=...
		//
		// In that case, DO NOT load the normal queue.
		// Forward directly to the filtered detail screen.
		//
		// Existing normal queue behaviour remains unchanged.
		// =====================================================

		String requestBatchNumber =
				Executions.getCurrent()
				.getParameter("batchNumber");

		String returnMode =
				Executions.getCurrent()
				.getParameter("returnMode");

		String repairType =
				Executions.getCurrent()
				.getParameter("repairType");

		String chequeNumber =
				Executions.getCurrent()
				.getParameter("chequeNumber");

		// Some servlet/container combinations may expose
		// amp;returnMode instead of returnMode.
		if (returnMode == null || returnMode.trim().isEmpty()) {
			returnMode =
					Executions.getCurrent()
					.getParameter("amp;returnMode");
		}

		if (requestBatchNumber != null
				&& !requestBatchNumber.trim().isEmpty()
				&& "HOLD".equalsIgnoreCase(
						returnMode != null
						? returnMode.trim()
								: ""
						)
				&& "DATA_ENTRY".equalsIgnoreCase(
						repairType != null
						? repairType.trim()
								: ""
						)) {

			StringBuilder url =
					new StringBuilder(
							"outward-maker-data-entry-detail.zul"
							);

			url.append("?batchId=")
			.append(requestBatchNumber.trim());

			url.append("&returnMode=HOLD");

			url.append("&repairType=DATA_ENTRY");

			if (chequeNumber != null
					&& !chequeNumber.trim().isEmpty()) {

				url.append("&chequeNumber=")
				.append(chequeNumber.trim());
			}

			System.out.println(
					"======================================"
					);

			System.out.println(
					"OPENING FILTERED RETURNED DATA ENTRY"
					);

			System.out.println(
					"Batch Number : "
							+ requestBatchNumber
					);

			System.out.println(
					"Return Mode  : HOLD"
					);

			System.out.println(
					"Repair Type  : DATA_ENTRY"
					);

			System.out.println(
					"Cheque Number: "
							+ chequeNumber
					);

			System.out.println(
					"URL          : "
							+ url
					);

			System.out.println(
					"======================================"
					);

			Executions.sendRedirect(
					url.toString()
					);

			return;
		}

		// =====================================================
		// GET LOGGED-IN USER ID FROM SESSION
		// =====================================================
		//
		// Existing session design:
		//
		// session attribute = "userId"
		//

		Object sessionUserId =
				sessionUser.getAttribute("userId");

		if (sessionUserId == null) {

			System.out.println(
					"No logged-in user ID found in session."
					);

			Executions.sendRedirect(
					"/zul/login.zul"
					);

			return;
		}

		// =====================================================
		// CONVERT SESSION USER ID TO LONG
		// =====================================================

		long userId;

		if (sessionUserId instanceof Number) {

			userId =
					((Number) sessionUserId)
					.longValue();

		} else {

			try {

				userId =
						Long.parseLong(
								sessionUserId.toString()
								);

			} catch (NumberFormatException e) {

				System.out.println(
						"Invalid userId in session: "
								+ sessionUserId
						);

				Executions.sendRedirect(
						"/zul/login.zul"
						);

				return;
			}

		}

		// =====================================================
		// DYNAMIC LOGGED-IN USER ID
		// =====================================================

		System.out.println(
				"======================================"
				);

		System.out.println(
				"OUTWARD MAKER DATA ENTRY"
				);

		System.out.println(
				"doAfterCompose() START"
				);

		System.out.println(
				"Current Maker User : "
						+ userId
				);

		// =====================================================
		// LOAD USER-SPECIFIC BATCHES
		// =====================================================

		loadBatches(userId);
	}

	// =========================================================
	// LOAD BATCHES
	// =========================================================

	private void loadBatches(long userId) {

		if (batchListbox == null) {
			return;
		}

		List<OutwardBatch> batches = dataEntryService.getBatchesForMaker(userId);

		if (batches == null || batches.isEmpty()) {
			batchListbox.setModel(new org.zkoss.zul.ListModelList<OutwardBatch>());
			if (batchPaging != null) {
				batchPaging.setTotalSize(0);
				batchPaging.setDetailed(false);
			}
			return;
		}

		// 1. Model & Paging linkage
		org.zkoss.zul.ListModelList<OutwardBatch> model = new org.zkoss.zul.ListModelList<>(batches);
		batchListbox.setModel(model);

		if (batchPaging != null) {
			batchListbox.setPaginal(batchPaging);
			batchPaging.setDetailed(false); // Eliminates the [ 1 - 1 / 1 ] text at the component engine level
		}

		// 2. Define the Row Renderer
		batchListbox.setItemRenderer((item, data, index) -> {
			OutwardBatch batch = (OutwardBatch) data;
			if (batch == null) return;

			String batchNumber = batch.getBatchNumber();
			String status = batch.getBatchStatus() != null ? batch.getBatchStatus().trim() : "UNKNOWN";
			int totalCheques = batch.getNumberOfCheques() != null ? batch.getNumberOfCheques() : 0;

			// 1. Batch ID
			Listcell cellBatchId = new Listcell(batchNumber);
			cellBatchId.setStyle("font-weight: 600; color: #1E293B;");
			item.appendChild(cellBatchId);

			// 2. Total Cheques
			item.appendChild(new Listcell(String.valueOf(totalCheques)));

			// 3. Batch Status
			Listcell cellStatus = new Listcell();
			Label lblStatus = new Label(status);
			lblStatus.setSclass(
					"status-badge " + 
							("COMPLETED".equalsIgnoreCase(status) ? "badge-completed" : "badge-assigned")
					);
			cellStatus.appendChild(lblStatus);
			item.appendChild(cellStatus);

			// 4. Action Button
			Listcell cellAction = new Listcell();
			Button btn = new Button();
			btn.setSclass("action-btn");

			if ("SENT_TO_MAKER".equalsIgnoreCase(status)) {
				btn.setLabel("Open");
				btn.addEventListener("onClick", e -> {
					Executions.sendRedirect("outward-maker-data-entry-detail.zul?batchId=" + batchNumber + "&returnMode=RETURNED");
				});
			} else {
				btn.setLabel("Process");
				btn.addEventListener("onClick", e -> {
					Executions.sendRedirect("outward-maker-data-entry-detail.zul?batchId=" + batchNumber);
				});
			}

			cellAction.appendChild(btn);
			item.appendChild(cellAction);
		});

	}



}
