package com.iispl.cts.controller.outward.checker;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

import com.cts.admin.model.User;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.service.outward.checker.CheckerBatchService;

public class CheckerBatchQueueController extends SelectorComposer<Vlayout> {

	private static final long serialVersionUID = 1L;

	/*
	 * processing.zul location:
	 *
	 * src/main/webapp/ zul/ outward/ outward-checker/ processing.zul
	 */
	private static final String PROCESSING_PAGE = "/zul/outward/outward-checker/processing.zul";
	@Wire
	private Textbox batchSearchTextbox;

	@Wire
	private Button searchButton;

	@Wire
	private Button refreshButton;

	@Wire
	private Listbox batchListbox;

	@Wire
	private Paging batchPaging;

	private CheckerBatchService batchService;

	private long checkerUserId;

	@Override
	public void doAfterCompose(Vlayout comp) throws Exception {

		super.doAfterCompose(comp);

		System.out.println("========================================");
		System.out.println("CHECKER BATCH QUEUE INITIALIZING");
		System.out.println("========================================");

		batchService = new CheckerBatchService();

		/*
		 * Get logged-in user.
		 */
		User currentUser = (User) Sessions.getCurrent().getAttribute("loggedInUser");

		if (currentUser == null) {

			System.out.println("CHECKER BATCH QUEUE: No logged-in user found.");

			return;
		}

		checkerUserId = currentUser.getUserId();

		System.out.println("Checker User ID : " + checkerUserId);

		/*
		 * Load initial batches.
		 */
		loadBatches();

		/*
		 * Search button.
		 */
		if (searchButton != null) {

			searchButton.addEventListener(Events.ON_CLICK, event -> {

				System.out.println("CHECKER BATCH QUEUE: Search clicked.");

				batchPaging.setActivePage(0);

				loadBatches();
			});
		}

		/*
		 * Refresh button.
		 */
		if (refreshButton != null) {

			refreshButton.addEventListener(Events.ON_CLICK, event -> {

				System.out.println("CHECKER BATCH QUEUE: Refresh clicked.");

				batchSearchTextbox.setValue("");

				batchPaging.setActivePage(0);

				loadBatches();
			});
		}

		/*
		 * Pagination.
		 */
		if (batchPaging != null) {

			batchPaging.addEventListener("onPaging", event -> {

				System.out.println("CHECKER BATCH QUEUE: Page changed.");

				loadBatches();
			});
		}

		System.out.println("CHECKER BATCH QUEUE INITIALIZED SUCCESSFULLY.");

		System.out.println("========================================");
	}

	/**
	 * Loads batches assigned to the logged-in checker.
	 */
	private void loadBatches() {

		if (batchService == null) {

			System.out.println("CHECKER BATCH QUEUE: batchService is null.");

			return;
		}

		if (batchPaging == null || batchSearchTextbox == null || batchListbox == null) {

			System.out.println("CHECKER BATCH QUEUE: Required ZUL components are not wired.");

			return;
		}

		int pageSize = batchPaging.getPageSize();

		int pageNo = batchPaging.getActivePage();

		String searchText = batchSearchTextbox.getValue();

		if (searchText == null) {
			searchText = "";
		}

		searchText = searchText.trim();

		String checkerId = String.valueOf(checkerUserId);

		System.out.println("----------------------------------------");
		System.out.println("Loading checker batches");
		System.out.println("Checker ID : " + checkerId);
		System.out.println("Search     : " + searchText);
		System.out.println("Page No    : " + pageNo);
		System.out.println("Page Size  : " + pageSize);
		System.out.println("----------------------------------------");

		List<OutwardBatch> batches = batchService.getCheckerBatches(checkerId, searchText, pageNo, pageSize);

		int totalSize = batchService.getCheckerBatchCount(checkerId, searchText);

		batchPaging.setTotalSize(totalSize);

		/*
		 * Remove existing rows.
		 */
		batchListbox.getItems().clear();

		if (batches == null || batches.isEmpty()) {

			System.out.println("No checker batches found.");

			return;
		}

		/*
		 * Create list rows.
		 */
		for (OutwardBatch batch : batches) {

			if (batch == null) {
				continue;
			}

			Listitem item = new Listitem();

			/*
			 * ---------------------------------------- Batch Number
			 * ----------------------------------------
			 */
			Listcell batchNumberCell = new Listcell();

			String batchNumber = batch.getBatchNumber();

			batchNumberCell.appendChild(new Label(batchNumber != null ? batchNumber : "-"));

			item.appendChild(batchNumberCell);

			/*
			 * ---------------------------------------- Total Cheques
			 * ----------------------------------------
			 */
			Listcell chequeCountCell = new Listcell();

			chequeCountCell.appendChild(new Label(String.valueOf(batch.getNumberOfCheques())));

			item.appendChild(chequeCountCell);

			/*
			 * ---------------------------------------- Status
			 * ----------------------------------------
			 */
			Listcell statusCell = new Listcell();

			String status = batch.getBatchStatus();

			statusCell.appendChild(new Label(status != null ? status : "-"));

			item.appendChild(statusCell);

			/*
			 * ---------------------------------------- Action
			 * ----------------------------------------
			 */
			Listcell actionCell = new Listcell();

			Button openButton = new Button("Open");

			openButton.addEventListener(Events.ON_CLICK, event -> openBatch(batch));

			actionCell.appendChild(openButton);

			item.appendChild(actionCell);

			batchListbox.appendChild(item);
		}
	}

	/**
	 * Opens the selected batch in processing.zul.
	 */
	private void openBatch(OutwardBatch batch) {

		System.out.println("========================================");
		System.out.println("OPEN BUTTON CLICKED");
		System.out.println("========================================");

		if (batch == null) {
			System.out.println("OPEN BATCH ERROR: Batch object is null.");
			return;
		}

		String batchNumber = batch.getBatchNumber();

		if (batchNumber == null || batchNumber.trim().isEmpty()) {
			System.out.println("OPEN BATCH ERROR: Batch number is empty.");
			return;
		}

		batchNumber = batchNumber.trim();

		try {

			String contextPath = Executions.getCurrent().getContextPath();

			String encodedBatchNumber = URLEncoder.encode(batchNumber, StandardCharsets.UTF_8);

			String url = contextPath + PROCESSING_PAGE + "?batchNumber=" + encodedBatchNumber;

			System.out.println("Batch Number    : " + batchNumber);
			System.out.println("Context Path    : " + contextPath);
			System.out.println("Processing Page : " + PROCESSING_PAGE);
			System.out.println("Encoded Batch   : " + encodedBatchNumber);
			System.out.println("Final URL       : " + url);
			System.out.println("========================================");

			Executions.sendRedirect(url);

		} catch (Exception e) {
			System.out.println("OPEN BATCH ERROR: Unable to open processing page.");
			e.printStackTrace();
		}
	}
}