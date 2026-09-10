package com.iispl.cts.controller.outward;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Listbox;

import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.service.outward.OutwardMakerAmountAccountService;

public class OutwardMakerAmountAccountController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    @Wire
    private Listbox batchListbox;

    private OutwardMakerAmountAccountService service;

    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

        service = new OutwardMakerAmountAccountService();

        loadBatches();
    }

    /**
     * Load Amount & Account batches dynamically.
     */
    private void loadBatches() {

        batchListbox.getItems().clear();

        try {

            List<OutwardBatch> batches =
                    service.getAmountAccountBatches();

            if (batches == null || batches.isEmpty()) {
                return;
            }

            for (OutwardBatch batch : batches) {

                Listitem item = new Listitem();

                /*
                 * ==========================================
                 * BATCH NUMBER
                 * ==========================================
                 */
                Listcell batchNumberCell =
                        new Listcell();

                batchNumberCell.appendChild(
                        new Label(
                                safeValue(
                                        batch.getBatchNumber()
                                )
                        )
                );

                item.appendChild(batchNumberCell);


                /*
                 * ==========================================
                 * TOTAL CHEQUES
                 * ==========================================
                 */
                Listcell chequeCountCell =
                        new Listcell();

                chequeCountCell.appendChild(
                        new Label(
                                String.valueOf(
                                        batch.getNumberOfCheques()
                                )
                        )
                );

                item.appendChild(chequeCountCell);


                /*
                 * ==========================================
                 * BATCH STATUS
                 * ==========================================
                 */
                Listcell statusCell =
                        new Listcell();

                statusCell.appendChild(
                        new Label(
                                safeValue(
                                        batch.getBatchStatus()
                                )
                        )
                );

                item.appendChild(statusCell);


                /*
                 * ==========================================
                 * ACTION
                 * ==========================================
                 */
                Listcell actionCell =
                        new Listcell();

                Button openButton =
                        new Button("Open");

                openButton.setWidth("100px");

                openButton.addEventListener(
                        "onClick",
                        event -> openBatch(
                                batch.getBatchNumber()
                        )
                );

                actionCell.appendChild(
                        openButton
                );

                item.appendChild(actionCell);


                /*
                 * Add complete row.
                 */
                batchListbox.appendChild(item);
            }

        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "Unable to load Account & Amount batches: "
                            + e.getMessage()
            );
        }
    }


    /**
     * Open selected batch.
     */
    private void openBatch(
            String batchNumber) {

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            showError(
                    "Batch number is missing."
            );

            return;
        }

        try {

            String encodedBatchNumber =
                    java.net.URLEncoder.encode(
                            batchNumber,
                            java.nio.charset.StandardCharsets.UTF_8
                    );

            Executions.sendRedirect(
                    "outward-maker-amount-account-detail.zul"
                            + "?batchNumber="
                            + encodedBatchNumber
            );

        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "Unable to open batch "
                            + batchNumber
                            + ": "
                            + e.getMessage()
            );
        }
    }


    /**
     * Reload the batch list.
     *
     * Can be called later after a batch has been
     * corrected and re-verified.
     */
    public void refreshBatches() {

        loadBatches();
    }


    /**
     * Safely display database values.
     */
    private String safeValue(
            String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return "-";
        }

        return value;
    }


    /**
     * Display an error row in the list.
     */
    private void showError(
            String message) {

        batchListbox.getItems().clear();

        Listitem errorItem =
                new Listitem();

        Listcell errorCell =
                new Listcell();

        errorCell.appendChild(
                new Label(message)
        );

        errorItem.appendChild(
                errorCell
        );

        batchListbox.appendChild(
                errorItem
        );
    }
}