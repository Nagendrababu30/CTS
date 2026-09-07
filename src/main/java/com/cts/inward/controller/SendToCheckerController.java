package com.cts.inward.controller;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Window;

// Import your new DAO and Model
import com.cts.inward.dao.SendBatchToCheckerDao;
import com.cts.inward.dao.SendBatchToCheckerDaoImpl;
import com.cts.inward.model.NpciBatchData;

public class SendToCheckerController extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    // UI Components
    private Rows batchRows;
    private Window confirmModal;
    private Window successModal;
    private Label confirmMessage;
    private Label successBatchIdLbl;
    
    private Button cancelBtn;
    private Button confirmSendBtn;
    private Button okBtn;

    // State
    private Long selectedBatchId; // Changed to Long to match NpciBatchData

    // Initialize your specific DAO
    private SendBatchToCheckerDao sendBatchDao = SendBatchToCheckerDaoImpl.of();

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        // Bind Modal Components
        cancelBtn = (Button) confirmModal.getFellow("cancelBtn");
        confirmSendBtn = (Button) confirmModal.getFellow("confirmSendBtn");
        confirmMessage = (Label) confirmModal.getFellow("confirmMessage");
        
        okBtn = (Button) successModal.getFellow("okBtn");
        successBatchIdLbl = (Label) successModal.getFellow("successBatchIdLbl");

        // Cancel Button: Close the confirmation modal
        cancelBtn.addEventListener(Events.ON_CLICK, event -> {
            confirmModal.setVisible(false);
        });

        // Confirm Button: Send to Database
        confirmSendBtn.addEventListener(Events.ON_CLICK, event -> {
            try {
                // 1. Update the database using the DAO
                sendBatchDao.updateBatchStatusToChecker(selectedBatchId);
                
                // 2. Hide confirm modal and show success modal
                confirmModal.setVisible(false);
                successBatchIdLbl.setValue(": " + selectedBatchId);
                successModal.doModal();
                
            } catch (Exception e) {
                e.printStackTrace();
                Messagebox.show("Error sending batch to checker: " + e.getMessage(), 
                                "Database Error", Messagebox.OK, Messagebox.ERROR);
            }
        });

        // OK Button: Close success modal and refresh table
        okBtn.addEventListener(Events.ON_CLICK, event -> {
            successModal.setVisible(false);
            loadBatches(); // Reload the UI with fresh DB data
        });

        // Initial Data Load
        loadBatches();
    }

    private void loadBatches() {
        batchRows.getChildren().clear();

        try {
            // Fetch live data from the database
            List<NpciBatchData> activeBatches = sendBatchDao.getReadyBatches();

            for (NpciBatchData batch : activeBatches) {
                Row row = new Row();
                
                // Column 1: Batch ID
                row.appendChild(new Label(String.valueOf(batch.getBatchId())));
                
                // Column 2: Total Cheques
                row.appendChild(new Label(String.valueOf(batch.getTotalCheques())));

                // Column 3: Status Badge
                Hlayout statusLayout = new Hlayout();
                statusLayout.setSclass("status-badge-ready"); 
                
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