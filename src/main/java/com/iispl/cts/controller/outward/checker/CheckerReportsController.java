package com.iispl.cts.controller.outward.checker;

import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.service.outward.checker.CheckerReportsService;
import com.iispl.cts.service.outward.checker.CheckerFileGenerationService;
import com.iispl.cts.service.outward.checker.CheckerCXFGenerationService;
import com.iispl.cts.service.outward.checker.CheckerCIBFGenerationService;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
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

    // ============================================================
    // ZUL COMPONENT
    // ============================================================

    @Wire
    private Listbox reportListbox;

    // ============================================================
    // SERVICES
    // ============================================================

    private CheckerReportsService service;

    private CheckerFileGenerationService fileGenerationService;

    private CheckerCXFGenerationService cxfGenerationService;

    private CheckerCIBFGenerationService cibfGenerationService;

    // ============================================================
    // CURRENT CHECKER USER ID
    // ============================================================

    private long currentCheckerUser;

    // ============================================================
    // PAGE INITIALIZATION
    // ============================================================

    @Override
    public void doAfterCompose(Component comp)
            throws Exception {

        super.doAfterCompose(comp);

        // ========================================================
        // GET ZK SESSION
        // ========================================================

        Session session =
                Executions.getCurrent().getSession();

        // ========================================================
        // NO SESSION
        // ========================================================

        if (session == null) {

            Executions.sendRedirect(
                    "/zul/login.zul"
            );

            return;
        }

        // ========================================================
        // GET USER ID FROM SESSION
        // ========================================================

        Object sessionUserId =
                session.getAttribute("userId");

        // ========================================================
        // USER ID NOT FOUND
        // ========================================================

        if (sessionUserId == null) {

            Executions.sendRedirect(
                    "/zul/login.zul"
            );

            return;
        }

        // ========================================================
        // CONVERT USER ID
        // ========================================================

        if (sessionUserId instanceof Number) {

            currentCheckerUser =
                    ((Number) sessionUserId)
                            .longValue();

        } else {

            try {

                currentCheckerUser =
                        Long.parseLong(
                                sessionUserId.toString()
                        );

            } catch (NumberFormatException e) {

                Executions.sendRedirect(
                        "/zul/login.zul"
                );

                return;
            }
        }

        // ========================================================
        // LOG CURRENT USER
        // ========================================================

        System.out.println(
                "CHECKER REPORTS SESSION: "
                        + "userId="
                        + currentCheckerUser
        );

        // ========================================================
        // CREATE SERVICES
        // ========================================================

        service =
                new CheckerReportsService();

        fileGenerationService =
                new CheckerFileGenerationService();

        cxfGenerationService =
                new CheckerCXFGenerationService();

        cibfGenerationService =
                new CheckerCIBFGenerationService();

        // ========================================================
        // LOAD COMPLETED BATCHES
        // ========================================================

        loadCompletedBatches();
    }

    // ============================================================
    // LOAD COMPLETED BATCHES
    // ============================================================

    private void loadCompletedBatches() {

        reportListbox.getItems().clear();

        try {

            List<OutwardBatch> batches =
                    service.getCheckerCompletedBatches();

            if (batches == null
                    || batches.isEmpty()) {

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

    // ============================================================
    // ADD BATCH TO LIST
    // ============================================================

    private void addBatchToList(
            OutwardBatch batch) {

        Listitem item =
                new Listitem();

        // ========================================================
        // BATCH NUMBER
        // ========================================================

        Listcell batchCell =
                new Listcell();

        batchCell.appendChild(
                new Label(
                        safe(
                                batch.getBatchNumber()
                        )
                )
        );

        item.appendChild(
                batchCell
        );

        // ========================================================
        // TOTAL CHEQUES
        // ========================================================

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

        item.appendChild(
                totalCell
        );

        // ========================================================
        // ACCEPTED
        // ========================================================

        Listcell acceptedCell =
                new Listcell();

        acceptedCell.appendChild(
                new Label("—")
        );

        item.appendChild(
                acceptedCell
        );

        // ========================================================
        // REJECTED
        // ========================================================

        Listcell rejectedCell =
                new Listcell();

        rejectedCell.appendChild(
                new Label("—")
        );

        item.appendChild(
                rejectedCell
        );

        // ========================================================
        // RETURNED
        // ========================================================

        Listcell returnedCell =
                new Listcell();

        returnedCell.appendChild(
                new Label("—")
        );

        item.appendChild(
                returnedCell
        );

        // ========================================================
        // STATUS
        // ========================================================

        Listcell statusCell =
                new Listcell();

        statusCell.appendChild(
                new Label(
                        safe(
                                batch.getBatchStatus()
                        )
                )
        );

        item.appendChild(
                statusCell
        );

        // ========================================================
        // ACTION
        // ========================================================

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

        item.appendChild(
                actionCell
        );

        // ========================================================
        // ADD ROW
        // ========================================================

        reportListbox.appendChild(
                item
        );
    }

    // ============================================================
    // GENERATE CXF + CIBF
    // ============================================================

    private void generateFiles(
            OutwardBatch batch) {

        String batchNumber =
                batch.getBatchNumber();

        try {

            // ====================================================
            // LOAD CHEQUES
            // ====================================================

            List<OutwardCheque> cheques =
                    fileGenerationService
                            .getBatchCheques(
                                    batchNumber
                            );

            if (cheques == null
                    || cheques.isEmpty()) {

                Messagebox.show(
                        "No cheque records found for batch:\n\n"
                                + batchNumber,
                        "File Generation",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION
                );

                return;
            }

            // ====================================================
            // GENERATE CXF
            // ====================================================

            String cxfFilePath =
                    cxfGenerationService.generateCXF(
                            batchNumber,
                            cheques
                    );

            // ====================================================
            // GENERATE CIBF
            // ====================================================

            String cibfFilePath =
                    cibfGenerationService.generateCIBF(
                            batchNumber,
                            cheques
                    );

            // ====================================================
            // SUCCESS MESSAGE
            // ====================================================

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

    // ============================================================
    // SAFE STRING
    // ============================================================

    private String safe(
            String value) {

        return value == null
                ? ""
                : value;
    }
}