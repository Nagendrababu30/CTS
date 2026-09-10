package com.iispl.cts.controller.outward.checker;

import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.service.outward.checker.CheckerReportsService;
import com.iispl.cts.service.outward.checker.CheckerFileGenerationService;
import com.iispl.cts.service.outward.checker.CheckerCXFGenerationService;
import com.iispl.cts.service.outward.checker.CheckerCIBFGenerationService;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;

import java.util.List;

public class CheckerReportsController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    @Wire
    private Listbox reportListbox;

    private CheckerReportsService service;

    private CheckerFileGenerationService fileGenerationService;

    private CheckerCXFGenerationService cxfGenerationService;

    private CheckerCIBFGenerationService cibfGenerationService;


    @Override
    public void doAfterCompose(Component comp)
            throws Exception {

        super.doAfterCompose(comp);

        service =
                new CheckerReportsService();

        fileGenerationService =
                new CheckerFileGenerationService();

        cxfGenerationService =
                new CheckerCXFGenerationService();

        cibfGenerationService =
                new CheckerCIBFGenerationService();

        loadCompletedBatches();
    }


    /**
     * Load ONLY ASSIGNED batches.
     *
     * ASSIGNED is being used temporarily.
     */
    private void loadCompletedBatches() {

        reportListbox.getItems().clear();

        try {

            List<OutwardBatch> batches =
                    service.getCheckerCompletedBatches();

            if (batches == null ||
                    batches.isEmpty()) {

                return;
            }

            for (OutwardBatch batch : batches) {

                addBatchToList(batch);
            }

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to load assigned batches.\n\n"
                            + e.getMessage(),
                    "Reports Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }


    private void addBatchToList(
            OutwardBatch batch) {

        Listitem item =
                new Listitem();


        // =========================
        // BATCH NUMBER
        // =========================

        Listcell batchCell =
                new Listcell();

        batchCell.appendChild(
                new Label(
                        safe(batch.getBatchNumber())
                )
        );

        item.appendChild(batchCell);


        // =========================
        // TOTAL CHEQUES
        // =========================

        Listcell totalCell =
                new Listcell();

        Integer count =
                batch.getNumberOfCheques();

        totalCell.appendChild(
                new Label(
                        count == null
                                ? "0"
                                : String.valueOf(count)
                )
        );

        item.appendChild(totalCell);


        // =========================
        // ACCEPTED
        // =========================

        Listcell acceptedCell =
                new Listcell();

        acceptedCell.appendChild(
                new Label("—")
        );

        item.appendChild(acceptedCell);


        // =========================
        // REJECTED
        // =========================

        Listcell rejectedCell =
                new Listcell();

        rejectedCell.appendChild(
                new Label("—")
        );

        item.appendChild(rejectedCell);


        // =========================
        // RETURNED
        // =========================

        Listcell returnedCell =
                new Listcell();

        returnedCell.appendChild(
                new Label("—")
        );

        item.appendChild(returnedCell);


        // =========================
        // STATUS
        // =========================

        Listcell statusCell =
                new Listcell();

        statusCell.appendChild(
                new Label(
                        safe(batch.getBatchStatus())
                )
        );

        item.appendChild(statusCell);


        // =========================
        // ACTION
        // =========================

        Listcell actionCell =
                new Listcell();

        Button generateButton =
                new Button(
                        "Generate CXF + CIBF"
                );

        generateButton.setSclass(
                "primary-button"
        );


        generateButton.addEventListener(
                Events.ON_CLICK,
                new EventListener<Event>() {

                    @Override
                    public void onEvent(
                            Event event)
                            throws Exception {

                        generateFiles(batch);
                    }
                }
        );


        actionCell.appendChild(
                generateButton
        );

        item.appendChild(actionCell);


        // =========================
        // ADD ROW
        // =========================

        reportListbox.appendChild(item);
    }


    /**
     * Generate CXF + CIBF files
     * for the selected batch.
     */
    private void generateFiles(
            OutwardBatch batch) {

        String batchNumber =
                batch.getBatchNumber();

        try {


            // =========================================
            // LOAD CHEQUES
            // =========================================

            List<OutwardCheque> cheques =
                    fileGenerationService
                            .getBatchCheques(
                                    batchNumber
                            );


            if (cheques == null ||
                    cheques.isEmpty()) {

                Messagebox.show(
                        "No cheque records found for batch:\n\n"
                                + batchNumber,

                        "File Generation",

                        Messagebox.OK,

                        Messagebox.EXCLAMATION
                );

                return;
            }


            // =========================================
            // GENERATE CXF
            // =========================================

            String cxfFilePath =
                    cxfGenerationService.generateCXF(
                            batchNumber,
                            cheques
                    );


            // =========================================
            // GENERATE CIBF
            // =========================================

            String cibfFilePath =
                    cibfGenerationService.generateCIBF(
                            batchNumber,
                            cheques
                    );


            // =========================================
            // SUCCESS MESSAGE
            // =========================================

            Messagebox.show(

                    "CXF + CIBF generated successfully.\n\n"

                            + "Batch: "
                            + batchNumber

                            + "\n\n"

                            + "Total Cheques: "
                            + cheques.size()

                            + "\n\n"

                            + "CXF File:\n"
                            + cxfFilePath

                            + "\n\n"

                            + "CIBF File:\n"
                            + cibfFilePath,

                    "File Generation",

                    Messagebox.OK,

                    Messagebox.INFORMATION
            );


        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(

                    "File generation failed.\n\n"

                            + "Batch: "
                            + batchNumber

                            + "\n\n"

                            + e.getMessage(),

                    "File Generation Error",

                    Messagebox.OK,

                    Messagebox.ERROR
            );
        }
    }


    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }
}