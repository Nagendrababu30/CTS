
package com.iispl.cts.controller.outward.checker;

import com.iispl.cts.dao.outward.checker.CheckerReportsDAO;
import com.iispl.cts.model.outward.OutwardBatch;

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

import java.util.List;

public class CheckerSendToNPCIController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    @Wire
    private Listbox npciBatchListbox;

    private CheckerReportsDAO reportsDAO;

    private long currentUserId;

    @Override
    public void doAfterCompose(
            Component component) throws Exception {

        super.doAfterCompose(component);

        Session session =
                Executions.getCurrent().getSession();

        if (session == null) {

            Executions.sendRedirect(
                    "/zul/login.zul"
            );

            return;
        }

        Object sessionUserId =
                session.getAttribute("userId");

        if (sessionUserId == null) {

            Executions.sendRedirect(
                    "/zul/login.zul"
            );

            return;
        }

        if (sessionUserId instanceof Number) {

            currentUserId =
                    ((Number) sessionUserId).longValue();

        } else {

            try {

                currentUserId =
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

        reportsDAO =
                new CheckerReportsDAO();

        loadBatches();
    }

    // ============================================================
    // LOAD BATCHES
    // ============================================================

    private void loadBatches() {

        try {

            if (npciBatchListbox != null) {

                npciBatchListbox
                        .getItems()
                        .clear();
            }

            List<OutwardBatch> batches =
                    reportsDAO.getCheckerCompletedBatches();

            if (batches == null ||
                    batches.isEmpty()) {

                return;
            }

            for (OutwardBatch batch : batches) {

                addBatchRow(batch);
            }

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to load batches ready for NPCI.\n\n"
                            + e.getMessage(),
                    "Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }

    // ============================================================
    // ADD BATCH ROW
    // ============================================================

    private void addBatchRow(
            OutwardBatch batch) {

        if (batch == null) {

            return;
        }

        String batchNumber =
                batch.getBatchNumber();

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return;
        }

        batchNumber =
                batchNumber.trim();

        int totalCount =
                reportsDAO.getTotalChequeCount(
                        batchNumber
                );

        int validCount =
                reportsDAO.getValidChequeCount(
                        batchNumber
                );

        Listitem item =
                new Listitem();

        // ========================================================
        // BATCH NUMBER
        // ========================================================

        Listcell batchCell =
                new Listcell();

        Label batchLabel =
                new Label(batchNumber);

        batchLabel.setStyle(
                "font-weight:bold;"
                        + "color:#172B4D;"
        );

        batchCell.appendChild(
                batchLabel
        );

        item.appendChild(
                batchCell
        );

        // ========================================================
        // TOTAL CHEQUES
        // ========================================================

        Listcell totalCell =
                new Listcell(
                        String.valueOf(totalCount)
                );

        item.appendChild(
                totalCell
        );

     // ========================================================
     // ACCEPTED CHEQUES
     // ========================================================

     Listcell acceptedCell =
             new Listcell(
                     String.valueOf(validCount)
             );

     item.appendChild(
             acceptedCell
     );

     // ========================================================
     // FILE NAME
     // ========================================================

     String fileName =
             batch.getBatchNumber() + ".xml";

     Listcell fileNameCell =
             new Listcell(fileName);

     item.appendChild(
             fileNameCell
     );

        // ========================================================
        // ACTION
        // ========================================================

        Listcell actionCell =
                new Listcell();

        Hbox actionBox =
                new Hbox();

        actionBox.setSpacing(
                "8px"
        );

        Button sendButton =
                new Button(
                        "Send to NPCI"
                );

        sendButton.setStyle(
                "background:#172B4D;"
                        + "color:white;"
                        + "border:none;"
                        + "border-radius:5px;"
                        + "padding:7px 14px;"
                        + "font-weight:bold;"
                        + "cursor:pointer;"
        );

        final String selectedBatch =
                batchNumber;

        sendButton.addEventListener(
                "onClick",
                event -> {

                    sendToNPCI(
                            selectedBatch
                    );
                }
        );

        actionBox.appendChild(
                sendButton
        );

        actionCell.appendChild(
                actionBox
        );

        item.appendChild(
                actionCell
        );

        npciBatchListbox.appendChild(
                item
        );
    }

    // ============================================================
    // SEND TO NPCI
    // ============================================================

    private void sendToNPCI(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return;
        }

        batchNumber =
                batchNumber.trim();

        final String confirmedBatchNumber =
                batchNumber;

        Messagebox.show(
                "Are you sure you want to send batch "
                        + confirmedBatchNumber
                        + " to NPCI?",
                "Confirm NPCI Submission",
                Messagebox.YES | Messagebox.NO,
                Messagebox.QUESTION,
                event -> {

                    if (Messagebox.ON_YES.equals(
                            event.getName())) {

                        submitBatch(
                                confirmedBatchNumber
                        );
                    }
                }
        );
    }

    // ============================================================
    // SUBMIT BATCH
    // ============================================================

    private void submitBatch(
            String batchNumber) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            return;
        }

        batchNumber =
                batchNumber.trim();

        try {

            System.out.println(
                    "Submitting batch "
                            + batchNumber
                            + " to NPCI..."
            );

            System.out.println(
                    "SUBMISSION USER ID = "
                            + currentUserId
            );

            // ====================================================
            // MARK BATCH AS NPCI SENT
            // ====================================================

            boolean success =
                    reportsDAO.markBatchAsNPCISent(
                            batchNumber
                    );

            if (!success) {

                Messagebox.show(
                        "Unable to send batch "
                                + batchNumber
                                + " to NPCI.",
                        "NPCI Submission Failed",
                        Messagebox.OK,
                        Messagebox.ERROR
                );

                return;
            }

            System.out.println(
                    "NPCI SUBMISSION SUCCESS"
            );

            System.out.println(
                    "BATCH NUMBER = "
                            + batchNumber
            );

            System.out.println(
                    "USER ID = "
                            + currentUserId
            );

            System.out.println(
                    "BATCH STATUS = NPCI_SENT"
            );

            // ====================================================
            // SUCCESS MESSAGE
            // ====================================================

            Messagebox.show(
                    "Batch "
                            + batchNumber
                            + " sent to NPCI successfully.",
                    "NPCI Submission Successful",
                    Messagebox.OK,
                    Messagebox.INFORMATION
            );

            // ====================================================
            // REFRESH
            // ====================================================

            loadBatches();

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Error while sending batch "
                            + batchNumber
                            + " to NPCI.\n\n"
                            + e.getMessage(),
                    "NPCI Submission Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }
}

