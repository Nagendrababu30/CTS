package com.cts.inward.controller;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;

import com.cts.inward.model.InwardBatch;
import com.cts.inward.service.BatchService;
import com.cts.inward.service.BatchServiceImpl;
import com.cts.inward.dao.BatchDaoImpl;

public class DataEntryController extends GenericForwardComposer<Component> {

	private static final long serialVersionUID = 1L;

	// ZUL components
	private Listbox batchListbox;
	// Service
	private BatchService batchService;

	// ---------------------------------------------------------
	// Page lifecycle
	// ---------------------------------------------------------

	@Override
	public void doAfterCompose(Component comp) throws Exception {

		super.doAfterCompose(comp);

		batchService = BatchServiceImpl.of(BatchDaoImpl.of());

		loadBatches();
	}

	// ---------------------------------------------------------
	// Load batches
	// ---------------------------------------------------------

	private void loadBatches() {

		batchListbox.getItems().clear();

		try {

			List<InwardBatch> batches = batchService.getAvailableBatchesForMaker(null);

			if (batches == null || batches.isEmpty()) {
				return;
			}

			for (InwardBatch batch : batches) {
				addBatchRow(batch);
			}

		} catch (RuntimeException e) {

			e.printStackTrace();

			Messagebox.show("Unable to load batches from database.", "Data Entry", Messagebox.OK, Messagebox.ERROR);
		}
	}

	// ---------------------------------------------------------
	// Create batch row
	// ---------------------------------------------------------

	private void addBatchRow(InwardBatch batch) {

		Listitem item = new Listitem();

		// -----------------------------------------------------
		// Batch No
		// -----------------------------------------------------

		Listcell batchCell = new Listcell();

		batchCell.appendChild(new Label(String.valueOf(batch.getBatchId())));

		item.appendChild(batchCell);

		// -----------------------------------------------------
		// Total Cheques
		// -----------------------------------------------------

		Listcell totalCell = new Listcell();

		totalCell.appendChild(new Label(String.valueOf(batch.getTotalCheques())));

		item.appendChild(totalCell);

		// -----------------------------------------------------
		// Data Entry Pending
		// -----------------------------------------------------

		Listcell pendingCell = new Listcell();

		pendingCell.setStyle("text-align:center;");

		int pendingCount =
		        batchService.getDataEntryPendingCount(batch.getBatchId());

		pendingCell.appendChild(
		        new Label(String.valueOf(pendingCount) + " Cheques")
		);
		item.appendChild(pendingCell);

		// -----------------------------------------------------
		// Action
		// -----------------------------------------------------

		Listcell actionCell = new Listcell();

		actionCell.setStyle("text-align:center;");

		Button openButton = new Button("Open");

		openButton.setSclass("btn btn-action");

		long batchId = batch.getBatchId();

		openButton.addEventListener(Events.ON_CLICK, event -> openBatch(batchId));

		actionCell.appendChild(openButton);

		item.appendChild(actionCell);

		// -----------------------------------------------------
		// Add row
		// -----------------------------------------------------

		batchListbox.appendChild(item);
	}

	// ---------------------------------------------------------
	// Open batch
	// ---------------------------------------------------------

	private void openBatch(long batchId) {

		String url = "/zul/inward-maker/data-entryform.zul" + "?batchId=" + batchId;

		Executions.sendRedirect(url);
	}
}