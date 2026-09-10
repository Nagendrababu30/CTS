package com.iispl.cts.controller.outward.checker;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zk.ui.util.Clients;

import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.service.outward.checker.CheckerBatchService;

public class CheckerBatchVerificationController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    // =========================================================
    // ZUL COMPONENTS
    // =========================================================

    @Wire
    private Label batchIdLabel;

    @Wire
    private Label totalChequeLabel;

    @Wire
    private Label acceptedChequeLabel;

    @Wire
    private Label rejectedChequeLabel;

    @Wire
    private Label pendingChequeLabel;

    @Wire
    private Listbox chequeListbox;

    // =========================================================
    // SERVICE
    // =========================================================

    private CheckerBatchService batchService;

    // =========================================================
    // VARIABLES
    // =========================================================

    private String batchId;

    private List<OutwardCheque> chequeList;

    private long currentUserId;

    // =========================================================
    // INITIALIZE
    // =========================================================

    @Override
    public void doAfterCompose(Component comp)
            throws Exception {

        super.doAfterCompose(comp);

        // =====================================================
        // GET ZK SESSION
        // =====================================================

        Session session =
                Executions.getCurrent().getSession();

        // =====================================================
        // NO SESSION
        // =====================================================

        if (session == null) {

            Executions.sendRedirect(
                    "/zul/login.zul"
            );

            return;
        }

        // =====================================================
        // GET USER ID FROM SESSION
        // =====================================================

        Object sessionUserId =
                session.getAttribute("userId");

        // =====================================================
        // USER ID NOT FOUND
        // =====================================================

        if (sessionUserId == null) {

            Executions.sendRedirect(
                    "/zul/login.zul"
            );

            return;
        }

        // =====================================================
        // CONVERT USER ID
        // =====================================================

        if (sessionUserId instanceof Number) {

            currentUserId =
                    ((Number) sessionUserId)
                            .longValue();

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

        // =====================================================
        // LOG CURRENT USER
        // =====================================================

        System.out.println(
                "Checker Batch Verification - User ID = "
                        + currentUserId
        );

        // =====================================================
        // CREATE SERVICE
        // =====================================================

        batchService =
                new CheckerBatchService();

        // =====================================================
        // GET BATCH ID FROM URL
        // =====================================================

        batchId =
                Executions
                        .getCurrent()
                        .getParameter("batchId");

        // =====================================================
        // IF BATCH ID IS MISSING
        // =====================================================

        if (batchId == null
                || batchId.trim().isEmpty()) {

            goBackToQueue();

            return;
        }

        // =====================================================
        // LOAD BATCH
        // =====================================================

        loadBatch();
    }

    // =========================================================
    // LOAD BATCH
    // =========================================================

    private void loadBatch() {

        try {

            System.out.println(
                    "=========================================="
            );

            System.out.println(
                    "LOADING BATCH = "
                            + batchId
            );

            System.out.println(
                    "CURRENT USER ID = "
                            + currentUserId
            );

            System.out.println(
                    "=========================================="
            );

            // =================================================
            // GET CHEQUES USING BATCH NUMBER
            // =================================================

            chequeList =
                    batchService
                            .getChequesByBatchNumber(
                                    batchId
                            );

            // =================================================
            // SAFETY CHECK
            // =================================================

            if (chequeList == null) {

                chequeList =
                        new ArrayList<>();
            }

            System.out.println(
                    "TOTAL CHEQUES FOUND = "
                            + chequeList.size()
            );

            // =================================================
            // DISPLAY BATCH ID
            // =================================================

            batchIdLabel.setValue(
                    batchId
            );

            // =================================================
            // UPDATE SUMMARY
            // =================================================

            updateSummary();

            // =================================================
            // DISPLAY CHEQUE LIST
            // =================================================

            displayCheques();

        } catch (Exception e) {

            e.printStackTrace();

            Clients.showNotification(
                    "Unable to load batch verification.",
                    Clients.NOTIFICATION_TYPE_ERROR,
                    null,
                    "top_center",
                    4000
            );
        }
    }

    // =========================================================
    // UPDATE SUMMARY
    // =========================================================

    private void updateSummary() {

        int total =
                chequeList.size();

        int accepted = 0;

        int rejected = 0;

        int pending = 0;

        for (OutwardCheque cheque : chequeList) {

            String status =
                    cheque.getChequeStatus();

            // =================================================
            // EMPTY STATUS
            // =================================================

            if (status == null
                    || status.trim().isEmpty()) {

                pending++;
            }

            // =================================================
            // ACCEPTED
            // =================================================

            else if (
                    status.equalsIgnoreCase("ACCEPTED")
                    || status.equalsIgnoreCase("ACCEPT")
                    || status.equalsIgnoreCase("VERIFIED")
            ) {

                accepted++;
            }

            // =================================================
            // REJECTED
            // =================================================

            else if (
                    status.equalsIgnoreCase("REJECTED")
                    || status.equalsIgnoreCase("REJECT")
            ) {

                rejected++;
            }

            // =================================================
            // EVERYTHING ELSE = PENDING
            // =================================================

            else {

                pending++;
            }
        }

        // =================================================
        // DISPLAY VALUES
        // =================================================

        totalChequeLabel.setValue(
                String.valueOf(total)
        );

        acceptedChequeLabel.setValue(
                String.valueOf(accepted)
        );

        rejectedChequeLabel.setValue(
                String.valueOf(rejected)
        );

        pendingChequeLabel.setValue(
                String.valueOf(pending)
        );
    }

    // =========================================================
    // DISPLAY CHEQUES
    // =========================================================

    private void displayCheques() {

        ListModelList<OutwardCheque> model =
                new ListModelList<>();

        model.addAll(
                chequeList
        );

        chequeListbox.setModel(
                model
        );

        chequeListbox.setItemRenderer(
                new ListitemRenderer<OutwardCheque>() {

                    @Override
                    public void render(
                            Listitem item,
                            OutwardCheque cheque,
                            int index) {

                        // =====================================
                        // SERIAL NUMBER
                        // =====================================

                        item.appendChild(
                                new Listcell(
                                        String.valueOf(
                                                index + 1
                                        )
                                )
                        );

                        // =====================================
                        // CHEQUE NUMBER
                        // =====================================

                        item.appendChild(
                                new Listcell(
                                        safe(
                                                cheque
                                                        .getChequeNumber()
                                        )
                                )
                        );

                        // =====================================
                        // ACCOUNT NUMBER
                        // =====================================

                        item.appendChild(
                                new Listcell(
                                        safe(
                                                cheque
                                                        .getDrawerAccountNumber()
                                        )
                                )
                        );

                        // =====================================
                        // AMOUNT
                        // =====================================

                        String amountValue = "-";

                        if (cheque.getAmount() != null) {

                            amountValue =
                                    cheque
                                            .getAmount()
                                            .toString();
                        }

                        item.appendChild(
                                new Listcell(
                                        amountValue
                                )
                        );

                        // =====================================
                        // CHEQUE DATE
                        // =====================================

                        String dateValue = "-";

                        if (cheque.getChequeDate() != null) {

                            dateValue =
                                    cheque
                                            .getChequeDate()
                                            .toString();
                        }

                        item.appendChild(
                                new Listcell(
                                        dateValue
                                )
                        );

                        // =====================================
                        // MICR
                        // =====================================

                        String micr =
                                safe(
                                        cheque.getCityCode()
                                )
                                + "-"
                                + safe(
                                        cheque.getBankCode()
                                )
                                + "-"
                                + safe(
                                        cheque.getBranchCode()
                                );

                        item.appendChild(
                                new Listcell(
                                        micr
                                )
                        );

                        // =====================================
                        // STATUS
                        // =====================================

                        String status =
                                getDisplayStatus(
                                        cheque
                                );

                        item.appendChild(
                                new Listcell(
                                        status
                                )
                        );

                        // =====================================
                        // ACTION
                        // =====================================

                        Listcell actionCell =
                                new Listcell();

                        Button openButton =
                                new Button();

                        openButton.setLabel(
                                "OPEN"
                        );

                        openButton.setSclass(
                                "primary-button"
                        );

                        // =====================================
                        // STORE CURRENT BATCH ID
                        // =====================================

                        final String currentBatchId =
                                batchId;

                        // =====================================
                        // STORE CHEQUE NUMBER
                        // =====================================

                        final String currentChequeNumber =
                                cheque.getChequeNumber();

                        // =====================================
                        // OPEN BUTTON CLICK
                        // =====================================

                        openButton.addEventListener(
                                "onClick",
                                event -> {

                                    String url =
                                            "/outward/checker/"
                                                    + "chequeVerification.zul"
                                                    + "?batchId="
                                                    + currentBatchId
                                                    + "&chequeNumber="
                                                    + currentChequeNumber;

                                    System.out.println(
                                            "OPENING CHEQUE URL = "
                                                    + url
                                    );

                                    Executions.sendRedirect(
                                            url
                                    );
                                }
                        );

                        actionCell.appendChild(
                                openButton
                        );

                        item.appendChild(
                                actionCell
                        );
                    }
                }
        );
    }

    // =========================================================
    // GET DISPLAY STATUS
    // =========================================================

    private String getDisplayStatus(
            OutwardCheque cheque) {

        String status =
                cheque.getChequeStatus();

        if (status == null
                || status.trim().isEmpty()) {

            return "PENDING";
        }

        return status.toUpperCase();
    }

    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safe(
            String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return "-";
        }

        return value;
    }

    // =========================================================
    // BACK BUTTON
    // =========================================================

    @Listen("onClick=#backButton")
    public void backButton() {

        goBackToQueue();
    }

    // =========================================================
    // BACK BUTTON BOTTOM
    // =========================================================

    @Listen("onClick=#backButtonBottom")
    public void backButtonBottom() {

        goBackToQueue();
    }

    // =========================================================
    // GO BACK TO QUEUE
    // =========================================================

    private void goBackToQueue() {

        // Correct: lowercase outward

        Executions.sendRedirect(
                "/outward/checker/batchesQueue.zul"
        );
    }

    // =========================================================
    // SIDEBAR - DASHBOARD
    // =========================================================

    @Listen("onClick=#dashboardButton")
    public void openDashboard() {

        navigate(
                "/outward/checker/dashboard.zul"
        );
    }

    // =========================================================
    // SIDEBAR - BATCH QUEUE
    // =========================================================

    @Listen("onClick=#queueButton")
    public void openQueue() {

        navigate(
                "/outward/checker/batchesQueue.zul"
        );
    }

    // =========================================================
    // SIDEBAR - REPORTS
    // =========================================================

    @Listen("onClick=#reportsButton")
    public void openReports() {

        navigate(
                "/outward/checker/reports.zul"
        );
    }

    // =========================================================
    // SIDEBAR - SEND TO NPCI
    // =========================================================

    @Listen("onClick=#npciButton")
    public void openNPCI() {

        navigate(
                "/outward/checker/sendToNPCI.zul"
        );
    }

    // =========================================================
    // COMMON NAVIGATION
    // =========================================================

    private void navigate(
            String page) {

        Executions.sendRedirect(
                page
        );
    }
}