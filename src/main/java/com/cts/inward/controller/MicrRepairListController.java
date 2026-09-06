package com.cts.inward.controller;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import com.cts.inward.dto.MicrRepairBatchDto;
import com.cts.inward.service.MicrRepairService;
import com.cts.inward.service.MicrRepairServiceImpl;

public class MicrRepairListController
        extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    private Grid micrRepairGrid;
    private Rows micrRepairRows;
    private Label emptyMessage;

    private MicrRepairService micrRepairService;

    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

        micrRepairService =
                new MicrRepairServiceImpl();

        loadRepairBatches();
    }

    private void loadRepairBatches() {

        micrRepairRows.getChildren().clear();

        List<MicrRepairBatchDto> batches =
                micrRepairService.getRepairBatches();

        /*
         * No batches require MICR repair.
         */
        if (batches == null || batches.isEmpty()) {

            micrRepairGrid.setVisible(false);
            emptyMessage.setVisible(true);

            return;
        }

        /*
         * At least one batch requires
         * MICR repair.
         */
        micrRepairGrid.setVisible(true);
        emptyMessage.setVisible(false);

        for (MicrRepairBatchDto batch : batches) {

            Row row = new Row();

            /*
             * Batch ID
             */
            row.appendChild(
                    new Label(
                            String.valueOf(
                                    batch.getBatchId())));

            /*
             * Total Cheques
             */
            row.appendChild(
                    new Label(
                            String.valueOf(
                                    batch.getTotalCheques())));

            /*
             * MICR Error Count
             */
            Label errorCount =
                    new Label(
                            String.valueOf(
                                    batch.getMicrErrorCount()));

            errorCount.setSclass(
                    "micr-error-count");

            row.appendChild(errorCount);

            /*
             * MICR Repair button
             */
            Button repairButton =
                    new Button("MICR Repair");

            repairButton.setSclass(
                    "repair-button");

            long batchId =
                    batch.getBatchId();

            repairButton.addEventListener(
                    Events.ON_CLICK,
                    event -> openRepairPage(batchId));

            row.appendChild(repairButton);

            micrRepairRows.appendChild(row);
        }
    }

    private void openRepairPage(long batchId) {

        String url =
                "/zul/inward-maker/"
                + "micr-repair.zul"
                + "?batchId="
                + batchId
                + "&source=list";

        Executions.sendRedirect(url);
    }
}