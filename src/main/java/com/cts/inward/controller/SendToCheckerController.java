package com.cts.inward.controller;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Window;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import com.cts.admin.model.User;
import com.cts.inward.dao.SendBatchToCheckerDao;
import com.cts.inward.dao.SendBatchToCheckerDaoImpl;
import com.cts.inward.model.NpciBatchData;

public class SendToCheckerController extends GenericForwardComposer<Component> {

	private static final long serialVersionUID = 1L;

	// UI Components
	private Rows batchRows;
	private Window confirmModal;
	private Label confirmMessage;
	private Button cancelBtn;
	private Button confirmSendBtn;

	// State
	private Long selectedBatchId;
	private Long loggedInUserId;

	// Initialize your specific DAO
	private SendBatchToCheckerDao sendBatchDao = SendBatchToCheckerDaoImpl.of();

	private void loadLoggedInUser() {

		Session session = Executions.getCurrent().getSession();

		User user = (User) session.getAttribute("loggedInUser");

		if (user != null) {
			loggedInUserId = user.getUserId();
		}
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		loadLoggedInUser();

		
		cancelBtn = (Button) confirmModal.getFellow("cancelBtn");
		confirmSendBtn = (Button) confirmModal.getFellow("confirmSendBtn");
		confirmMessage = (Label) confirmModal.getFellow("confirmMessage");

		
		cancelBtn.addEventListener(Events.ON_CLICK, event -> {
			confirmModal.setVisible(false);
		});

		
		confirmSendBtn.addEventListener(Events.ON_CLICK, event -> {
			try {
				
				sendBatchDao.updateBatchStatusToChecker(selectedBatchId);
				confirmModal.setVisible(false);

				
				Clients.showNotification("Sent to Checker", Clients.NOTIFICATION_TYPE_INFO, null, "top_right", 2000);

				
				Clients.evalJavaScript("setTimeout(function() { window.location.href = '"
						+ Executions.encodeURL("/zul/inward-maker/dashboard.zul") + "'; }, 2000);");

			} catch (Exception e) {
				e.printStackTrace();
				Messagebox.show("Error sending batch to checker: " + e.getMessage(), "Database Error", Messagebox.OK,
						Messagebox.ERROR);
			}
		});

		
		loadBatches();
	}

	private void loadBatches() {
		batchRows.getChildren().clear();
		loadLoggedInUser();

		try {
			
			List<NpciBatchData> activeBatches = sendBatchDao.getReadyBatches(loggedInUserId);

			for (NpciBatchData batch : activeBatches) {
				Row row = new Row();

				
				row.appendChild(new Label(String.valueOf(batch.getBatchId())));

				row.appendChild(new Label(String.valueOf(batch.getTotalCheques())));

				
				Hlayout statusLayout = new Hlayout();
				statusLayout.setSclass("status-badge-ready");

				Label checkIcon = new Label("✔");
				checkIcon.setSclass("status-icon-ready");

				Label statusLabel = new Label("Ready to Submit");
				statusLabel.setSclass("status-text-ready");

				statusLayout.appendChild(checkIcon);
				statusLayout.appendChild(statusLabel);
				row.appendChild(statusLayout);

				
				Button sendBtn = new Button("Send to Checker");
				sendBtn.setSclass("btn-action-send");

				sendBtn.addEventListener(Events.ON_CLICK, event -> {
					selectedBatchId = batch.getBatchId(); 
					confirmMessage.setValue("Are you sure you want to send batch " + selectedBatchId
							+ " to Inward Checker for verification?");
					confirmModal.doModal();
				});

				row.appendChild(sendBtn);
				batchRows.appendChild(row);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Messagebox.show("Failed to load batches from database.", "Error", Messagebox.OK, Messagebox.ERROR);
		}
	}
}