package com.cts.inward.controller;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import com.cts.admin.model.User;
import com.cts.inward.dto.MicrRepairBatchDto;
import com.cts.inward.service.MicrRepairService;
import com.cts.inward.service.MicrRepairServiceImpl;

// Controller for the Maker MICR Repair batch queue listing
public class MicrRepairListController extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    // UI Grid components
    private Grid micrRepairGrid;
    private Rows micrRepairRows;
    private Label emptyMessage;

    private MicrRepairService micrRepairService;
    private Long loggedInUserId;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        micrRepairService = new MicrRepairServiceImpl();

        // Load active user session and repair batches
        loadLoggedInUser();
        loadRepairBatches();
    }

    // Resolves currently logged-in user ID from session
    private void loadLoggedInUser() {
        Session session = Executions.getCurrent().getSession();
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null) {
            loggedInUserId = user.getUserId();
        }
    }

    // Loads pending MICR repair batches assigned to the current Maker
    private void loadRepairBatches() {

        micrRepairRows.getChildren().clear();

        List<MicrRepairBatchDto> batches = micrRepairService.getRepairBatches(loggedInUserId);

        if (batches == null || batches.isEmpty()) {
            micrRepairGrid.setVisible(true);
            emptyMessage.setVisible(true);
            return;
        }

        micrRepairGrid.setVisible(true);
        emptyMessage.setVisible(false);

        for (MicrRepairBatchDto batch : batches) {
            if (batch == null) {
                continue;
            }

            Row row = new Row();
            row.appendChild(new Label(String.valueOf(batch.getBatchId())));
            row.appendChild(new Label(String.valueOf(batch.getTotalCheques())));

            Label errorCount = new Label(String.valueOf(batch.getMicrErrorCount()));
            errorCount.setSclass("micr-error-count");
            row.appendChild(errorCount);

            Button repairButton = new Button("MICR Repair");
            repairButton.setSclass("repair-button");

            long batchId = batch.getBatchId();
            repairButton.addEventListener(Events.ON_CLICK, event -> openRepairPage(batchId));

            row.appendChild(repairButton);
            micrRepairRows.appendChild(row);
        }

        if (micrRepairRows.getChildren().isEmpty()) {
            micrRepairGrid.setVisible(true);
            emptyMessage.setVisible(true);
        }
    }

    // Navigates to the MICR Repair workstation for the selected batch
    private void openRepairPage(long batchId) {

        if (batchId <= 0L) {
            Messagebox.show("Invalid Batch ID.", "MICR Repair", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        String url = "/zul/inward-maker/micr-repair.zul?batchId=" + batchId + "&source=list";
        Executions.sendRedirect(url);
    }
}