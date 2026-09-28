package com.iispl.cts.controller.outward.checker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.image.AImage;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;

import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.service.outward.checker.CheckerBatchService;

public class CheckerChequeVerificationController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    @Wire
    private Label accountVerificationIcon;

    @Wire
    private Label accountVerificationMessage;

    @Wire
    private Label verificationBatchId;

    @Wire
    private Label chequeSequence;

    @Wire
    private Image chequeImage;

    @Wire
    private Label chequeNumberLabel;

    @Wire
    private Label accountNumberLabel;

    @Wire
    private Label drawerNameLabel;

    @Wire
    private Label payeeNameLabel;

    @Wire
    private Label amountLabel;

    @Wire
    private Label amountInWordsLabel;

    @Wire
    private Label chequeDateLabel;

    @Wire
    private Label micrLabel;

    @Wire
    private Button previousButton;

    @Wire
    private Button nextButton;

    private CheckerBatchService batchService;

    private String batchNumber;

    private List<OutwardCheque> chequeList;

    private int currentIndex = 0;

    private long currentUserId;

    // Initializes the page, validates the checker session, and loads the requested batch.
    @Override
    public void doAfterCompose(Component comp)
            throws Exception {

        super.doAfterCompose(comp);

        System.out.println();
        System.out.println(
                "======================================"
        );
        System.out.println(
                "CHEQUE VERIFICATION CONTROLLER STARTED"
        );
        System.out.println(
                "======================================"
        );

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

        System.out.println(
                "CURRENT CHECKER USER ID = "
                        + currentUserId
        );

        batchService =
                new CheckerBatchService();

        batchNumber =
                Executions
                        .getCurrent()
                        .getParameter(
                                "batchNumber"
                        );

        System.out.println(
                "BATCH NUMBER = "
                        + batchNumber
        );

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            System.out.println(
                    "BATCH NUMBER NOT FOUND"
            );

            goBackToQueue();

            return;
        }

        loadCheques();
    }

    // Loads all cheques for the selected batch and displays the first cheque.
    private void loadCheques() {

        try {

            System.out.println();

            System.out.println(
                    "LOADING CHEQUES FOR BATCH = "
                            + batchNumber
            );

            System.out.println(
                    "CHECKER USER ID = "
                            + currentUserId
            );

            chequeList =
                    batchService
                            .getChequesByBatchNumber(
                                    batchNumber
                            );

            if (chequeList == null) {

                chequeList =
                        new ArrayList<>();
            }

            System.out.println(
                    "TOTAL CHEQUES = "
                            + chequeList.size()
            );

            if (chequeList.isEmpty()) {

                chequeSequence.setValue(
                        "No cheques found"
                );

                previousButton.setDisabled(
                        true
                );

                nextButton.setDisabled(
                        true
                );

                return;
            }

            currentIndex = 0;

            displayCurrentCheque();

        } catch (Exception e) {

            e.printStackTrace();

            Clients.showNotification(
                    "Unable to load cheques.",
                    Clients.NOTIFICATION_TYPE_ERROR,
                    null,
                    "top_center",
                    4000
            );
        }
    }

    // Displays the current cheque details, verifies the account, and loads its image.
    private void displayCurrentCheque() {

        if (chequeList == null
                || chequeList.isEmpty()) {

            return;
        }

        OutwardCheque cheque =
                chequeList.get(
                        currentIndex
                );

        System.out.println();

        System.out.println(
                "DISPLAYING CHEQUE INDEX = "
                        + currentIndex
        );

        verificationBatchId.setValue(
                batchNumber
        );

        chequeSequence.setValue(
                "Cheque "
                        + (currentIndex + 1)
                        + " of "
                        + chequeList.size()
        );

        chequeNumberLabel.setValue(
                safe(
                        cheque.getChequeNumber()
                )
        );

        accountNumberLabel.setValue(
                safe(
                        cheque.getDrawerAccountNumber()
                )
        );

        boolean accountVerified =
                batchService.verifyAccount(
                        cheque.getDrawerAccountNumber()
                );

        if (accountVerified) {

            accountVerificationIcon.setValue(
                    "✓"
            );

            accountVerificationMessage.setValue(
                    "Account Verified"
            );

            accountVerificationIcon.setStyle(
                    "font-size:20px;"
                            + "font-weight:bold;"
                            + "color:#039855;"
            );

            accountVerificationMessage.setStyle(
                    "font-size:15px;"
                            + "font-weight:bold;"
                            + "padding-left:8px;"
                            + "color:#039855;"
            );

        } else {

            accountVerificationIcon.setValue(
                    "✕"
            );

            accountVerificationMessage.setValue(
                    "Account Not Verified"
            );

            accountVerificationIcon.setStyle(
                    "font-size:20px;"
                            + "font-weight:bold;"
                            + "color:#D92D20;"
            );

            accountVerificationMessage.setStyle(
                    "font-size:15px;"
                            + "font-weight:bold;"
                            + "padding-left:8px;"
                            + "color:#D92D20;"
            );
        }

        drawerNameLabel.setValue(
                safe(
                        cheque.getDrawerName()
                )
        );

        payeeNameLabel.setValue(
                safe(
                        cheque.getPayeeName()
                )
        );

        if (cheque.getAmount() != null) {

            amountLabel.setValue(
                    cheque.getAmount()
                            .toPlainString()
            );

        } else {

            amountLabel.setValue(
                    "-"
            );
        }

        amountInWordsLabel.setValue(
                safe(
                        cheque.getAmountInWords()
                )
        );

        if (cheque.getChequeDate() != null) {

            chequeDateLabel.setValue(
                    cheque.getChequeDate()
                            .toString()
            );

        } else {

            chequeDateLabel.setValue(
                    "-"
            );
        }

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

        micrLabel.setValue(
                micr
        );

        loadChequeImage(
                cheque
        );

        previousButton.setDisabled(
                currentIndex == 0
        );

        nextButton.setDisabled(
                currentIndex
                        == chequeList.size() - 1
        );

        System.out.println(
                "DISPLAYED CHEQUE = "
                        + cheque.getChequeNumber()
        );
    }

    // Loads the cheque image from a local file or uses the configured web path.
    private void loadChequeImage(
            OutwardCheque cheque) {

        try {

            String imagePath =
                    cheque.getFrontImagePath();

            System.out.println(
                    "IMAGE PATH = "
                            + imagePath
            );

            if (imagePath == null
                    || imagePath.trim().isEmpty()) {

                chequeImage.setSrc("");

                return;
            }

            File imageFile =
                    new File(imagePath);

            if (imageFile.exists()
                    && imageFile.isFile()) {

                chequeImage.setContent(
                        new AImage(
                                imageFile
                        )
                );

            } else {

                chequeImage.setSrc(
                        imagePath
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "ERROR LOADING IMAGE"
            );

            e.printStackTrace();
        }
    }

    // Moves to the previous cheque when one is available.
    @Listen("onClick = #previousButton")
    public void previousCheque() {

        if (currentIndex > 0) {

            currentIndex--;

            displayCurrentCheque();
        }
    }

    // Moves to the next cheque when one is available.
    @Listen("onClick = #nextButton")
    public void nextCheque() {

        if (chequeList != null
                && currentIndex
                        < chequeList.size() - 1) {

            currentIndex++;

            displayCurrentCheque();
        }
    }

    // Returns the checker to the batch queue when the back button is clicked.
    @Listen("onClick = #backButton")
    public void backButton() {

        System.out.println(
                "BACK TO BATCH QUEUE"
        );

        goBackToQueue();
    }

    // Redirects the checker back to the outward checker batch queue.
    private void goBackToQueue() {

        Executions.sendRedirect(
                "/outward/checker/batchesQueue.zul"
        );
    }

    // Returns "-" for null or empty values so the UI does not display blank fields.
    private String safe(
            String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return "-";
        }

        return value;
    }
}