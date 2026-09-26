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
import org.zkoss.zk.ui.util.Composer;

// Import your new DAO and Model
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
    private Long selectedBatchId; // Changed to Long to match NpciBatchData
    private Long loggedInUserId;

    // Initialize your specific DAO
    private SendBatchToCheckerDao sendBatchDao = SendBatchToCheckerDaoImpl.of();

    private void loadLoggedInUser() {
        org.zkoss.zk.ui.Session session = Executions.getCurrent().getSession();
        com.cts.admin.model.User user = (com.cts.admin.model.User) session.getAttribute("loggedInUser");
        if (user != null) {
            loggedInUserId = user.getUserId();
        }
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        loadLoggedInUser();

        // Bind Modal Components
        cancelBtn = (Button) confirmModal.getFellow("cancelBtn");
        confirmSendBtn = (Button) confirmModal.getFellow("confirmSendBtn");
        confirmMessage = (Label) confirmModal.getFellow("confirmMessage");

        // Cancel Button: Close the confirmation modal
        cancelBtn.addEventListener(Events.ON_CLICK, event -> {
            confirmModal.setVisible(false);
        });

        // Confirm Button: Send to Database, show 2s notification, and redirect to
        // dashboard
        confirmSendBtn.addEventListener(Events.ON_CLICK, event -> {
            try {
                // 1. Update the database using the DAO
                sendBatchDao.updateBatchStatusToChecker(selectedBatchId);

                // 2. Hide confirm modal
                confirmModal.setVisible(false);

                // 3. Show notification popup for 2s
                Clients.showNotification(
                        "Sent to Checker",
                        Clients.NOTIFICATION_TYPE_INFO,
                        null,
                        "top_center",
                        2000);

                // 4. Redirect to Inward Maker dashboard after 2s
                Clients.evalJavaScript(
                        "setTimeout(function() { window.location.href = '" +
                                Executions.encodeURL("/zul/inward-maker/dashboard.zul") + "'; }, 2000);");

            } catch (Exception e) {
                e.printStackTrace();
                Messagebox.show("Error sending batch to checker: " + e.getMessage(),
                        "Database Error", Messagebox.OK, Messagebox.ERROR);
            }
        });

        // Initial Data Load
        loadBatches();
    }

    private void loadBatches() {
        batchRows.getChildren().clear();
        loadLoggedInUser();

        try {
            // Fetch live data from the database
            List<NpciBatchData> activeBatches = sendBatchDao.getReadyBatches(loggedInUserId);

            for (NpciBatchData batch : activeBatches) {
                Row row = new Row();

                // Column 1: Batch ID
                Label batchIdLabel = new Label(String.valueOf(batch.getBatchId()));
                batchIdLabel.setStyle("text-align: center; display: block; width: 100%;");
                row.appendChild(batchIdLabel);

                // Column 2: Total Cheques
                Label totalLabel = new Label(String.valueOf(batch.getTotalCheques()));
                totalLabel.setStyle("text-align: center; display: block; width: 100%;");
                row.appendChild(totalLabel);

                // Column 3: Status Badge
                Hlayout statusLayout = new Hlayout();
                statusLayout.setSclass("status-badge-ready");
                statusLayout.setStyle(
                        "margin: 0 auto; justify-content: center; display: inline-flex; align-items: center;");

                Label checkIcon = new Label("✔");
                checkIcon.setSclass("status-icon-ready");

                Label statusLabel = new Label("Ready to Submit");
                statusLabel.setSclass("status-text-ready");

                statusLayout.appendChild(checkIcon);
                statusLayout.appendChild(statusLabel);
                row.appendChild(statusLayout);

                // Column 4: Action Button
                Button sendBtn = new Button("Send to Checker");
                sendBtn.setSclass("btn-action-send");
                sendBtn.setStyle("margin: 0 auto; display: block;");

                sendBtn.addEventListener(Events.ON_CLICK, event -> {
                    selectedBatchId = batch.getBatchId(); // Store the Long ID
                    confirmMessage.setValue("Are you sure you want to send batch " +
                            selectedBatchId + " to Inward Checker for verification?");
                    confirmModal.doModal();
                });

                row.appendChild(sendBtn);

                // Append the completed row to the grid
                batchRows.appendChild(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Messagebox.show("Failed to load batches from database.",
                    "Error", Messagebox.OK, Messagebox.ERROR);
        }
    }
}