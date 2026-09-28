package com.iispl.cts.controller.outward.checker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;

import com.cts.admin.service.SessionService;
import com.cts.admin.service.SessionServiceImpl;
import com.iispl.cts.dao.outward.checker.CheckerReportsDAO;
import com.iispl.cts.model.outward.OutwardBatch;

public class CheckerSendToNPCIController extends SelectorComposer<Component> {

	private static final long serialVersionUID = 1L;

	@Wire
	private Listbox npciBatchListbox;

	private CheckerReportsDAO reportsDAO;

	private long currentUserId;

	// Initializes the controller, validates the user and clearing session, and loads batches.
	@Override
	public void doAfterCompose(Component component) throws Exception {

		super.doAfterCompose(component);

		Session session = Executions.getCurrent().getSession();

		if (session == null) {

			Executions.sendRedirect(Executions.getCurrent().getContextPath() + "/login.zul");

			return;
		}

		Object sessionUserId = session.getAttribute("userId");

		if (sessionUserId == null) {

			Executions.sendRedirect(Executions.getCurrent().getContextPath() + "/login.zul");

			return;
		}

		if (sessionUserId instanceof Number) {

			currentUserId = ((Number) sessionUserId).longValue();

		} else {

			try {

				currentUserId = Long.parseLong(sessionUserId.toString());

			} catch (NumberFormatException e) {

				Executions.sendRedirect(Executions.getCurrent().getContextPath() + "/login.zul");

				return;
			}
		}

		SessionService sessionService = new SessionServiceImpl();

		com.cts.admin.model.Session clearingSession = sessionService.getActiveSession();

		if (clearingSession == null || clearingSession.getStatus() == null
				|| !"STARTED".equalsIgnoreCase(clearingSession.getStatus().trim())) {

			Messagebox.show(
					"Clearing session is not started.\n\n" + "Checker operations " + "are currently unavailable.",
					"Session Not Started", Messagebox.OK, Messagebox.EXCLAMATION, event -> {

						if (Messagebox.ON_OK.equals(event.getName())) {

							Executions.sendRedirect("/login.zul");
						}
					});

			return;
		}

		reportsDAO = new CheckerReportsDAO();

		loadBatches();
	}

	// Loads checker-completed batches and displays them in the NPCI batch list.
	private void loadBatches() {

		try {

			if (npciBatchListbox != null) {

				npciBatchListbox.getItems().clear();
			}

			List<OutwardBatch> batches = reportsDAO.getCheckerCompletedBatches();

			if (batches == null || batches.isEmpty()) {

				return;
			}

			for (OutwardBatch batch : batches) {

				addBatchRow(batch);
			}

		} catch (Exception e) {

			e.printStackTrace();

			Messagebox.show(

					"Unable to load batches ready for NPCI.\n\n" + e.getMessage(),

					"Error",

					Messagebox.OK,

					Messagebox.ERROR);
		}
	}

	// Creates and adds a batch row containing cheque counts and the NPCI submission action.
	private void addBatchRow(OutwardBatch batch) {

		if (batch == null) {

			return;
		}

		String batchNumber = batch.getBatchNumber();

		if (batchNumber == null || batchNumber.trim().isEmpty()) {

			return;
		}

		batchNumber = batchNumber.trim();

		int totalCount = reportsDAO.getTotalChequeCount(batchNumber);

		int validCount = reportsDAO.getValidChequeCount(batchNumber);

		Listitem item = new Listitem();

		Listcell batchCell = new Listcell();

		Label batchLabel = new Label(batchNumber);

		batchLabel.setStyle("font-weight:bold;" + "color:#172B4D;");

		batchCell.appendChild(batchLabel);

		item.appendChild(batchCell);

		Listcell totalCell = new Listcell(String.valueOf(totalCount));

		item.appendChild(totalCell);

		Listcell acceptedCell = new Listcell(String.valueOf(validCount));

		item.appendChild(acceptedCell);

		String fileName = batchNumber + "_valid.xml";

		Listcell fileNameCell = new Listcell(fileName);

		item.appendChild(fileNameCell);

		Listcell actionCell = new Listcell();

		Hbox actionBox = new Hbox();

		actionBox.setSpacing("8px");

		Button sendButton = new Button("Send to NPCI");

		sendButton.setStyle("background:#172B4D;" + "color:white;" + "border:none;" + "border-radius:5px;"
				+ "padding:7px 14px;" + "font-weight:bold;" + "cursor:pointer;");

		final String selectedBatch = batchNumber;

		sendButton.addEventListener("onClick", event -> {

			sendToNPCI(selectedBatch);
		});

		actionBox.appendChild(sendButton);

		actionCell.appendChild(actionBox);

		item.appendChild(actionCell);

		npciBatchListbox.appendChild(item);
	}

	// Confirms the selected batch and starts the NPCI submission process.
	private void sendToNPCI(String batchNumber) {

		if (batchNumber == null || batchNumber.trim().isEmpty()) {

			return;
		}

		batchNumber = batchNumber.trim();

		final String confirmedBatchNumber = batchNumber;

		Messagebox.show(

				"Are you sure you want to send batch " + confirmedBatchNumber + " to NPCI?",

				"Confirm NPCI Submission",

				Messagebox.YES | Messagebox.NO,

				Messagebox.QUESTION,

				event -> {

					if (Messagebox.ON_YES.equals(event.getName())) {

						submitBatch(confirmedBatchNumber);
					}
				});
	}

	// Validates the batch XML and updates the batch status after NPCI submission.
	private void submitBatch(String batchNumber) {

		if (batchNumber == null || batchNumber.trim().isEmpty()) {

			return;
		}

		batchNumber = batchNumber.trim();

		try {

			System.out.println("========================================");

			System.out.println("       NPCI SUBMISSION STARTED");

			System.out.println("========================================");

			System.out.println("Batch Number = " + batchNumber);

			System.out.println("User ID = " + currentUserId);

			String fileName = batchNumber + "_valid.xml";

			System.out.println("Valid XML File = " + fileName);

			Path validXmlPath = getValidXmlPath(fileName);

			System.out.println("Valid XML Path = " + validXmlPath.toAbsolutePath());

			if (!Files.exists(validXmlPath)) {

				Messagebox.show(

						"Valid XML file was not found.\n\n" + "Batch Number: " + batchNumber + "\n\n"
								+ "Expected File: " + fileName + "\n\n" + "Expected Location:\n"
								+ validXmlPath.toAbsolutePath(),

						"NPCI Submission Failed",

						Messagebox.OK,

						Messagebox.ERROR);

				return;
			}

			if (!Files.isRegularFile(validXmlPath)) {

				Messagebox.show(

						"The valid XML path is not a file.\n\n" + validXmlPath.toAbsolutePath(),

						"NPCI Submission Failed",

						Messagebox.OK,

						Messagebox.ERROR);

				return;
			}

			long fileSize = Files.size(validXmlPath);

			System.out.println("Valid XML found successfully.");

			System.out.println("File Size = " + fileSize + " bytes");

			if (fileSize <= 0) {

				Messagebox.show(

						"The valid XML file is empty.\n\n" + "File: " + fileName,

						"NPCI Submission Failed",

						Messagebox.OK,

						Messagebox.ERROR);

				return;
			}

			boolean success = reportsDAO.markBatchAsNPCISent(batchNumber);

			if (!success) {

				Messagebox.show(

						"Unable to send batch " + batchNumber + " to NPCI.\n\n" + "Valid XML was found, "
								+ "but the batch status " + "could not be updated.",

						"NPCI Submission Failed",

						Messagebox.OK,

						Messagebox.ERROR);

				return;
			}

			System.out.println("========================================");

			System.out.println("       NPCI SUBMISSION SUCCESS");

			System.out.println("========================================");

			System.out.println("Batch Number = " + batchNumber);

			System.out.println("Valid XML = " + fileName);

			System.out.println("User ID = " + currentUserId);

			System.out.println("Batch Status = NPCI_SENT");

			Messagebox.show(

					"Batch " + batchNumber + " sent to NPCI successfully.\n\n" + "Valid XML:\n" + fileName,

					"NPCI Submission Successful",

					Messagebox.OK,

					Messagebox.INFORMATION,

					event -> {

						if (Messagebox.ON_OK.equals(event.getName())) {

							loadBatches();
						}
					});

		} catch (Exception e) {

			e.printStackTrace();

			Messagebox.show(

					"Error while sending batch " + batchNumber + " to NPCI.\n\n" + e.getMessage(),

					"NPCI Submission Error",

					Messagebox.OK,

					Messagebox.ERROR);
		}
	}

	// Resolves the filesystem path of the valid XML file stored in the project archive.
	private Path getValidXmlPath(String fileName) throws Exception {

		String deployedPath = Executions.getCurrent().getDesktop().getWebApp().getRealPath("/");

		if (deployedPath == null) {

			throw new Exception("Unable to determine deployed application path.");
		}

		Path deployedRoot = Paths.get(deployedPath).toAbsolutePath().normalize();

		System.out.println();
		System.out.println("========================================");

		System.out.println("NPCI VALID XML PATH RESOLUTION");

		System.out.println("========================================");

		System.out.println("Deployed application:");

		System.out.println(deployedRoot);

		Path workspaceRoot = deployedRoot;

		while (workspaceRoot != null && workspaceRoot.getParent() != null) {

			Path currentName = workspaceRoot.getFileName();

			if (currentName != null && ".metadata".equalsIgnoreCase(currentName.toString())) {

				workspaceRoot = workspaceRoot.getParent();

				break;
			}

			workspaceRoot = workspaceRoot.getParent();
		}

		if (workspaceRoot == null) {

			throw new Exception("Unable to locate Eclipse workspace.");
		}

		System.out.println("Eclipse workspace:");

		System.out.println(workspaceRoot);

		Path projectNamePath = deployedRoot.getFileName();

		if (projectNamePath == null) {

			throw new Exception("Unable to determine project name.");
		}

		String projectName = projectNamePath.toString();

		System.out.println("Project name:");

		System.out.println(projectName);

		Path projectRoot = workspaceRoot.resolve(projectName);

		System.out.println("Project root:");

		System.out.println(projectRoot.toAbsolutePath());

		Path validDirectory = projectRoot
				.resolve(Paths.get("src", "main", "webapp", "css", "outward", "Archive", "ValidCheques"));

		System.out.println("ValidCheques directory:");

		System.out.println(validDirectory.toAbsolutePath());

		Path validXmlPath = validDirectory.resolve(fileName).normalize();

		System.out.println("Expected file:");

		System.out.println(fileName);

		System.out.println("Final XML path:");

		System.out.println(validXmlPath.toAbsolutePath());

		System.out.println("File exists:");

		System.out.println(Files.exists(validXmlPath));

		System.out.println("Is regular file:");

		System.out.println(Files.isRegularFile(validXmlPath));

		System.out.println("========================================");

		return validXmlPath;
	}
}