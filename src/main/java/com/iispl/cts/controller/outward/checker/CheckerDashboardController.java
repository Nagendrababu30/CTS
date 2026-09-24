package com.iispl.cts.controller.outward.checker;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Messagebox;

import com.cts.admin.service.SessionService;
import com.cts.admin.service.SessionServiceImpl;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.service.outward.checker.CheckerDashboardService;

public class CheckerDashboardController extends SelectorComposer<Component> {

	private static final long serialVersionUID = 1L;

	@Wire
	private Listbox batchListbox;

	@Wire
	private Label pendingVerificationCount;

	@Wire
	private Label cbsValidationCount;

	@Wire
	private Label readyToSendCount;

	@Wire
	private Button allBtn;

	@Wire
	private Button availableBtn;

	@Wire
	private Button myBatchesBtn;

	@Wire
	private Button reVerifyBatchesBtn;

	private CheckerDashboardService service;

	private long currentCheckerUser;

	private String currentFilter = "ALL";

	@Wire
	private Paging batchPaging;

	@Override
	public void doAfterCompose(Component comp) throws Exception {

		super.doAfterCompose(comp);
	     SessionService sessionService;

		Session session =
		        Executions.getCurrent().getSession();

		if (session == null) {

		    Executions.sendRedirect("/login.zul");

		    return;
		}

		Object sessionUserId =
		        session.getAttribute("userId");

		if (sessionUserId == null) {

		    Executions.sendRedirect("/login.zul");

		    return;
		}

		if (sessionUserId instanceof Number) {

		    currentCheckerUser =
		            ((Number) sessionUserId).longValue();

		} else {

		    try {

		        currentCheckerUser =
		                Long.parseLong(
		                        sessionUserId.toString());

		    } catch (NumberFormatException e) {

		        Executions.sendRedirect("/login.zul");

		        return;
		    }
		}

		sessionService = new SessionServiceImpl();

		com.cts.admin.model.Session clearingSession =
		        sessionService.getActiveSession();

		if (clearingSession == null
		        || clearingSession.getStatus() == null
		        || !"STARTED".equalsIgnoreCase(
		                clearingSession.getStatus().trim())) {

		    Messagebox.show(
		            "Clearing session is not started.\n\n"
		            + "Checker operations are currently unavailable.",
		            "Session Not Started",
		            Messagebox.OK,
		            Messagebox.EXCLAMATION,
		            event -> {

		                if (Messagebox.ON_OK.equals(
		                        event.getName())) {

		                    Executions.sendRedirect(
		                            "/login.zul");
		                }
		            });

		    return;
		}

		service = new CheckerDashboardService();

		currentFilter = "ALL";

		updateFilterButtonStyles();

		registerFilterEvents();

		loadDashboard();
	}

	private void registerFilterEvents() {

		if (allBtn != null) {

			allBtn.addEventListener(Events.ON_CLICK, event -> {

				currentFilter = "ALL";

				updateFilterButtonStyles();
				loadDashboard();
			});
		}

		if (availableBtn != null) {

			availableBtn.addEventListener(Events.ON_CLICK, event -> {

				currentFilter = "AVAILABLE";

				updateFilterButtonStyles();
				loadDashboard();
			});
		}

		if (myBatchesBtn != null) {

			myBatchesBtn.addEventListener(Events.ON_CLICK, event -> {

				currentFilter = "MY_BATCHES";

				updateFilterButtonStyles();
				loadDashboard();
			});
		}

		if (reVerifyBatchesBtn != null) {

			reVerifyBatchesBtn.addEventListener(Events.ON_CLICK, event -> {

				currentFilter = "RE_VERIFY_BATCHES";

				updateFilterButtonStyles();
				loadDashboard();
			});
		}
	}

	private void updateFilterButtonStyles() {

		if (allBtn != null) {

			allBtn.setSclass(
					"filter-btn"
							+ ("ALL".equals(currentFilter)
									? " active-filter"
									: ""));
		}

		if (availableBtn != null) {

			availableBtn.setSclass(
					"filter-btn"
							+ ("AVAILABLE".equals(currentFilter)
									? " active-filter"
									: ""));
		}

		if (myBatchesBtn != null) {

			myBatchesBtn.setSclass(
					"filter-btn"
							+ ("MY_BATCHES".equals(currentFilter)
									? " active-filter"
									: ""));
		}

		if (reVerifyBatchesBtn != null) {

			reVerifyBatchesBtn.setSclass(
					"filter-btn"
							+ ("RE_VERIFY_BATCHES".equals(currentFilter)
									? " active-filter"
									: ""));
		}
	}

	private void loadDashboard() {

		try {

			List<OutwardBatch> batches = getDashboardBatches();

			loadCounts(batches);
			loadBatchList(batches);

		} catch (Exception e) {

			e.printStackTrace();

			Clients.showNotification(
					"Unable to load Checker Dashboard.",
					Clients.NOTIFICATION_TYPE_ERROR,
					null,
					"top_center",
					4000);
		}
	}

	private List<OutwardBatch> getDashboardBatches() {

		List<OutwardBatch> batches =
				service.getBatches(
						String.valueOf(currentCheckerUser));

		if (batches == null) {

			batches = new ArrayList<>();
		}

		for (OutwardBatch batch : batches) {

			if (batch == null
					|| batch.getBatchNumber() == null) {

				continue;
			}

			boolean reVerified =
					service.hasReVerifiedCheques(
							batch.getBatchNumber(),
							String.valueOf(currentCheckerUser));

			if (reVerified) {

				batch.setLockStatus("RE_VERIFY");
			}
		}

		return batches;
	}

	private void loadCounts(List<OutwardBatch> batches) {

	    int pending = 0;
	    int completedToday = 0;
	    int readyToSend = 0;

	    if (batches != null) {

	        for (OutwardBatch batch : batches) {

	            if (batch == null) {
	                continue;
	            }

	            String status = batch.getBatchStatus();

	            if (status == null) {
	                continue;
	            }

	            status = status.trim().toUpperCase();

	            if ("READY_FOR_CHECKER".equals(status)
	                    || "SUBMITTED".equals(status)
	                    || "SUBMITTED_TO_CHECKER".equals(status)
	                    || "CHECKER_PENDING".equals(status)
	                    || "PENDING_CHECKER".equals(status)
	                    || "CHECKER_PROCESSING".equals(status)) {

	                pending++;
	            }

	            if ("NPCI_SENT".equals(status)) {
	                completedToday++;
	            }

	            if ("READY_TO_SEND".equals(status)
	                    || "READY_FOR_NPCI".equals(status)) {

	                readyToSend++;
	            }
	        }
	    }

	    if (pendingVerificationCount != null) {
	        pendingVerificationCount.setValue(
	                String.valueOf(pending));
	    }

	    if (cbsValidationCount != null) {
	        cbsValidationCount.setValue(
	                String.valueOf(completedToday));
	    }

	    if (readyToSendCount != null) {
	        readyToSendCount.setValue(
	                String.valueOf(readyToSend));
	    }
	}

	private void loadBatchList(List<OutwardBatch> batches) {
		List<OutwardBatch> filteredBatches = new ArrayList<>();

		if (batches != null) {
			for (OutwardBatch batch : batches) {
				if (batch != null && matchesCurrentFilter(batch)) {
					filteredBatches.add(batch);
				}
			}
		}

		ListModelList<OutwardBatch> model =
				new ListModelList<>();

		model.addAll(filteredBatches);

		batchListbox.setItemRenderer(
				new CheckerBatchRenderer());

		batchListbox.setModel(model);

		if (batchPaging != null) {
			batchPaging.setPageSize(10);
			batchPaging.setDetailed(false);
			batchListbox.setPaginal(batchPaging);
		}
	}

	private boolean matchesCurrentFilter(
			OutwardBatch batch) {

		if (batch == null) {

			return false;
		}

		if ("ALL".equals(currentFilter)) {

			return true;
		}

		if ("AVAILABLE".equals(currentFilter)) {

			return isBatchAvailable(batch);
		}

		if ("MY_BATCHES".equals(currentFilter)) {

			return isMyBatch(batch);
		}

		if ("RE_VERIFY_BATCHES".equals(currentFilter)) {

			return isReVerifyBatch(batch);
		}

		return true;
	}

	private boolean isBatchAvailable(
			OutwardBatch batch) {

		if (batch == null) {

			return false;
		}

		String lockStatus =
				batch.getLockStatus();

		return "AVAILABLE".equalsIgnoreCase(
				safe(lockStatus));
	}

	private boolean isMyBatch(
			OutwardBatch batch) {

		if (batch == null) {

			return false;
		}

		String checkerUser =
				batch.getCheckerUserNumber();

		if (checkerUser == null
				|| checkerUser.trim().isEmpty()) {

			return false;
		}

		return String.valueOf(
				currentCheckerUser)
				.equalsIgnoreCase(
						checkerUser.trim());
	}

	private boolean isReVerifyBatch(
			OutwardBatch batch) {

		if (batch == null) {

			return false;
		}

		return "RE_VERIFY".equalsIgnoreCase(
				safe(batch.getLockStatus()));
	}

	private class CheckerBatchRenderer
    implements ListitemRenderer<OutwardBatch> {

@Override
public void render(
        Listitem item,
        OutwardBatch batch,
        int index) throws Exception {

    Listcell batchCell =
            new Listcell();

    batchCell.setLabel(
            safe(batch.getBatchNumber()));

    item.appendChild(batchCell);

    Listcell chequeCell =
            new Listcell();

    int chequeCount =
            batch.getNumberOfCheques();

    if ("RE_VERIFY".equalsIgnoreCase(
            batch.getLockStatus())) {

        chequeCount =
                service.getReVerifiedChequeCount(
                        batch.getBatchNumber(),
                        String.valueOf(
                                currentCheckerUser));
    }

    chequeCell.setLabel(
            String.valueOf(chequeCount));

    item.appendChild(chequeCell);

    Listcell statusCell =
            new Listcell();

    boolean reVerifyStatus =
            "RE_VERIFY".equalsIgnoreCase(
                    batch.getLockStatus());

    if (reVerifyStatus) {

        statusCell.setLabel(
                "RE-VERIFIED");

    } else {

        statusCell.setLabel(
                safe(batch.getBatchStatus()));
    }

    item.appendChild(statusCell);

    Listcell assignmentCell =
            new Listcell();

    String lockStatus =
            batch.getLockStatus();

    if (lockStatus == null) {

        assignmentCell.setLabel(
                "AVAILABLE");

    } else if ("AVAILABLE"
            .equalsIgnoreCase(lockStatus)) {

        assignmentCell.setLabel(
                "Available");

    } else if ("RE_VERIFY"
            .equalsIgnoreCase(lockStatus)
            && "RE_VERIFY_BATCHES"
                    .equals(currentFilter)) {

        assignmentCell.setLabel(
                "Re-Verify");

    } else {

        String checker =
                batch.getCheckerUserNumber();

        if (checker != null
                && !checker.trim().isEmpty()) {

            assignmentCell.setLabel(
                    "Locked by Checker "
                            + checker);

        } else {

            assignmentCell.setLabel(
                    "Locked");
        }
    }

    item.appendChild(assignmentCell);

 Listcell actionCell =
         new Listcell();

 boolean available =
         "AVAILABLE".equalsIgnoreCase(
                 batch.getLockStatus());

 boolean reVerify =
         "RE_VERIFY_BATCHES".equals(currentFilter)
                 && "RE_VERIFY".equalsIgnoreCase(
                         batch.getLockStatus());

 boolean assignedToCurrentChecker =
         service.isAssignedToChecker(
                 batch.getBatchNumber(),
                 String.valueOf(
                         currentCheckerUser));

 boolean originalCheckerCanReVerify =
         service.hasReVerifiedCheques(
                 batch.getBatchNumber(),
                 String.valueOf(
                         currentCheckerUser));

 if (assignedToCurrentChecker) {

     Button openButton =
             new Button("Open");

     openButton.setSclass(
             "action-btn");

     openButton.addEventListener(
             Events.ON_CLICK,
             event -> openBatch(batch));

     Button releaseButton =
             new Button("Release Lock");

     releaseButton.setSclass(
             "action-btn release-btn");

     releaseButton.addEventListener(
             Events.ON_CLICK,
             event -> releaseBatchLock(
                     batch.getBatchNumber()));

     Hlayout actionLayout =
             new Hlayout();

     actionLayout.setSpacing(
             "4px");

     actionLayout.appendChild(
             openButton);

     actionLayout.appendChild(
             releaseButton);

     actionCell.appendChild(
             actionLayout);

 } else if (available
         || reVerify
         || originalCheckerCanReVerify) {

     Button openButton =
             new Button("Open");

     openButton.setSclass(
             "action-btn");

     openButton.addEventListener(
             Events.ON_CLICK,
             event -> openBatch(batch));

     actionCell.appendChild(
             openButton);

 } else {

     Button lockedButton =
             new Button("🔒 Locked");

     lockedButton.setDisabled(
             true);

     actionCell.appendChild(
             lockedButton);
 }

 item.appendChild(
         actionCell);
}
}

	private void openBatch(
			OutwardBatch batch) {

		if (batch == null
				|| batch.getBatchNumber() == null) {

			Clients.showNotification(
					"Invalid batch.",
					Clients.NOTIFICATION_TYPE_ERROR,
					null,
					"top_center",
					3000);

			return;
		}

		String batchNumber =
				batch.getBatchNumber();

		try {

			OutwardBatch latest =
					service.findBatch(batchNumber);

			if (latest == null) {

				Clients.showNotification(
						"Batch no longer exists.",
						Clients.NOTIFICATION_TYPE_ERROR,
						null,
						"top_center",
						3000);

				loadDashboard();

				return;
			}

			boolean reVerifyAllowed =
					service.hasReVerifiedCheques(
							batchNumber,
							String.valueOf(
									currentCheckerUser));

			if (reVerifyAllowed) {

				List<String> reVerifiedChequeNumbers =
						service.getReVerifiedChequeNumbers(
								batchNumber,
								String.valueOf(
										currentCheckerUser));

				if (reVerifiedChequeNumbers == null
						|| reVerifiedChequeNumbers.isEmpty()) {

					Clients.showNotification(
							"No corrected cheque is available for re-verification.",
							Clients.NOTIFICATION_TYPE_WARNING,
							null,
							"top_center",
							4000);

					return;
				}

				String chequeNumber =
						reVerifiedChequeNumbers.get(0);

				String url =
						"/zul/outward/outward-checker/processing.zul"
								+ "?batchNumber="
								+ Executions.encodeURL(
										batchNumber)
								+ "&chequeNumber="
								+ Executions.encodeURL(
										chequeNumber);

				Executions.sendRedirect(url);

				return;
			}

			boolean pendingMakerCheques =
					service.hasPendingMakerCheques(
							batchNumber,
							String.valueOf(
									currentCheckerUser));

			if (pendingMakerCheques) {

				Clients.showNotification(
						"Still in process by Maker.",
						Clients.NOTIFICATION_TYPE_WARNING,
						null,
						"top_center",
						4000);

				return;
			}

			boolean assignedToCurrentChecker =
					service.isAssignedToChecker(
							batchNumber,
							String.valueOf(
									currentCheckerUser));

			if (assignedToCurrentChecker) {

				Executions.sendRedirect(
						"/zul/outward/outward-checker/batchesQueue.zul"
								+ "?batchNumber="
								+ Executions.encodeURL(
										batchNumber));

				return;
			}

			if (!"AVAILABLE".equalsIgnoreCase(
					latest.getLockStatus())) {

				Clients.showNotification(
						"Batch is already locked.",
						Clients.NOTIFICATION_TYPE_WARNING,
						null,
						"top_center",
						3000);

				loadDashboard();

				return;
			}

			boolean assigned =
					service.assignBatch(
							batchNumber,
							currentCheckerUser);

			if (!assigned) {

				Clients.showNotification(
						"Batch was already assigned to another Checker.",
						Clients.NOTIFICATION_TYPE_WARNING,
						null,
						"top_center",
						4000);

				loadDashboard();

				return;
			}

			Clients.showNotification(
					"Batch assigned successfully.",
					Clients.NOTIFICATION_TYPE_INFO,
					null,
					"top_center",
					2000);

			Executions.sendRedirect(
					"/zul/outward/outward-checker/batchesQueue.zul"
							+ "?batchNumber="
							+ Executions.encodeURL(
									batchNumber));

		} catch (Exception e) {

			e.printStackTrace();

			Clients.showNotification(
					"Unable to open batch.",
					Clients.NOTIFICATION_TYPE_ERROR,
					null,
					"top_center",
					4000);
		}
	}

	private void releaseBatchLock(
	        String batchNumber) {

	    if (batchNumber == null
	            || batchNumber.trim().isEmpty()) {

	        Messagebox.show(
	                "Invalid batch number.",
	                "Release Lock",
	                Messagebox.OK,
	                Messagebox.ERROR);

	        return;
	    }

	    final String cleanBatchNumber =
	            batchNumber.trim();

	    Messagebox.show(
	            "Are you sure you want to release the lock for batch "
	                    + cleanBatchNumber
	                    + "?\n\n"
	                    + "The batch will become available again "
	                    + "for Checker processing.",

	            "Confirm Release Lock",

	            Messagebox.YES | Messagebox.NO,

	            Messagebox.QUESTION,

	            event -> {

	                if (!Messagebox.ON_YES.equals(
	                        event.getName())) {

	                    return;
	                }

	                try {

	                    boolean released =
	                            service.releaseBatchLock(
	                                    cleanBatchNumber,
	                                    currentCheckerUser);

	                    if (released) {

	                        Messagebox.show(
	                                "Batch "
	                                        + cleanBatchNumber
	                                        + " has been released successfully.",

	                                "Release Lock",

	                                Messagebox.OK,

	                                Messagebox.INFORMATION);

	                        loadDashboard();

	                    } else {

	                        Messagebox.show(
	                                "Unable to release the batch.\n\n"
	                                        + "The batch may no longer "
	                                        + "be assigned to you.",

	                                "Release Lock",

	                                Messagebox.OK,

	                                Messagebox.ERROR);

	                        loadDashboard();
	                    }

	                } catch (Exception e) {

	                    e.printStackTrace();

	                    Messagebox.show(
	                            "Error while releasing batch "
	                                    + cleanBatchNumber
	                                    + ".\n\n"
	                                    + e.getMessage(),

	                            "Release Lock",

	                            Messagebox.OK,

	                            Messagebox.ERROR);
	                }
	            });
	}

	private String safe(String value) {

		return value == null ? "" : value;
	}
}
