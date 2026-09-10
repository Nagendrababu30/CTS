package com.iispl.cts.controller.outward.checker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.image.AImage;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;

import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.service.outward.checker.CheckerBatchService;

public class CheckerChequeVerificationController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;


    // ============================================
    // ZUL COMPONENTS
    // ============================================
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


    // ============================================
    // VARIABLES
    // ============================================

    private CheckerBatchService batchService;

    private String batchNumber;

    private List<OutwardCheque> chequeList;

    private int currentIndex = 0;


    // ============================================
    // PAGE INITIALIZATION
    // ============================================

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


        // ========================================
        // CREATE SERVICE
        // ========================================

        batchService = new CheckerBatchService();


        // ========================================
        // GET BATCH NUMBER FROM URL
        //
        // URL WILL BE:
        //
        // chequeVerification.zul?batchNumber=XXXX
        // ========================================

        batchNumber = Executions.getCurrent()
                .getParameter("batchNumber");


        System.out.println(
                "BATCH NUMBER = " + batchNumber
        );


        // ========================================
        // VALIDATE BATCH NUMBER
        // ========================================

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            System.out.println(
                    "BATCH NUMBER NOT FOUND"
            );

            goBackToQueue();

            return;
        }


        // ========================================
        // LOAD CHEQUES
        // ========================================

        loadCheques();
    }


    // ============================================
    // LOAD CHEQUES
    // ============================================

    private void loadCheques() {

        try {

            System.out.println();
            System.out.println(
                    "LOADING CHEQUES FOR BATCH = "
                            + batchNumber
            );


            // ========================================
            // GET CHEQUES
            // ========================================

            chequeList =
                    batchService.getChequesByBatchNumber(
                            batchNumber
                    );


            // ========================================
            // NULL SAFETY
            // ========================================

            if (chequeList == null) {

                chequeList =
                        new ArrayList<>();

            }


            System.out.println(
                    "TOTAL CHEQUES = "
                            + chequeList.size()
            );


            // ========================================
            // NO CHEQUES
            // ========================================

            if (chequeList.isEmpty()) {

                chequeSequence.setValue(
                        "No cheques found"
                );

                previousButton.setDisabled(true);

                nextButton.setDisabled(true);

                return;
            }


            // ========================================
            // START FROM FIRST CHEQUE
            // ========================================

            currentIndex = 0;


            // ========================================
            // DISPLAY FIRST CHEQUE
            // ========================================

            displayCurrentCheque();


        } catch (Exception e) {

            e.printStackTrace();

        }
    }


    // ============================================
    // DISPLAY CURRENT CHEQUE
    // ============================================

    private void displayCurrentCheque() {

        if (chequeList == null
                || chequeList.isEmpty()) {

            return;
        }


        // ========================================
        // GET CURRENT CHEQUE
        // ========================================

        OutwardCheque cheque =
                chequeList.get(currentIndex);


        System.out.println();
        System.out.println(
                "DISPLAYING CHEQUE INDEX = "
                        + currentIndex
        );


        // ========================================
        // BATCH NUMBER
        // ========================================

        verificationBatchId.setValue(
                batchNumber
        );


        // ========================================
        // CHEQUE SEQUENCE
        // ========================================

        chequeSequence.setValue(

                "Cheque "

                + (currentIndex + 1)

                + " of "

                + chequeList.size()

        );


        // ========================================
        // CHEQUE NUMBER
        // ========================================

        chequeNumberLabel.setValue(

                safe(
                        cheque.getChequeNumber()
                )

        );


        // ========================================
        // ACCOUNT NUMBER
        // ========================================

        accountNumberLabel.setValue(

                safe(
                        cheque.getDrawerAccountNumber()
                )

        );
     // ========================================
     // VERIFY ACCOUNT
     // ========================================

     boolean accountVerified =
             batchService.verifyAccount(
                     cheque.getDrawerAccountNumber()
             );

     if (accountVerified) {

         accountVerificationIcon.setValue("✓");

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

         accountVerificationIcon.setValue("✕");

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


        // ========================================
        // DRAWER NAME
        // ========================================

        drawerNameLabel.setValue(

                safe(
                        cheque.getDrawerName()
                )

        );


        // ========================================
        // PAYEE NAME
        // ========================================

        payeeNameLabel.setValue(

                safe(
                        cheque.getPayeeName()
                )

        );


        // ========================================
        // AMOUNT
        // ========================================

        if (cheque.getAmount() != null) {

            amountLabel.setValue(

                    cheque.getAmount()
                            .toPlainString()

            );

        } else {

            amountLabel.setValue("-");

        }


        // ========================================
        // AMOUNT IN WORDS
        // ========================================

        amountInWordsLabel.setValue(

                safe(
                        cheque.getAmountInWords()
                )

        );


        // ========================================
        // CHEQUE DATE
        // ========================================

        if (cheque.getChequeDate() != null) {

            chequeDateLabel.setValue(

                    cheque.getChequeDate()
                            .toString()

            );

        } else {

            chequeDateLabel.setValue("-");

        }


        // ========================================
        // MICR
        // ========================================

        String micr =

                safe(cheque.getCityCode())

                + "-"

                + safe(cheque.getBankCode())

                + "-"

                + safe(cheque.getBranchCode());


        micrLabel.setValue(micr);


     
        

        // ========================================
        // LOAD CHEQUE IMAGE
        // ========================================

        loadChequeImage(cheque);


        // ========================================
        // PREVIOUS BUTTON
        // ========================================

        previousButton.setDisabled(

                currentIndex == 0

        );


        // ========================================
        // NEXT BUTTON
        // ========================================

        nextButton.setDisabled(

                currentIndex
                        == chequeList.size() - 1

        );


        System.out.println(
                "DISPLAYED CHEQUE = "
                        + cheque.getChequeNumber()
        );

    }


    // ============================================
    // LOAD CHEQUE IMAGE
    // ============================================

    private void loadChequeImage(
            OutwardCheque cheque) {

        try {

            String imagePath =
                    cheque.getFrontImagePath();


            System.out.println(
                    "IMAGE PATH = "
                            + imagePath
            );


            // ========================================
            // NO IMAGE
            // ========================================

            if (imagePath == null
                    || imagePath.trim().isEmpty()) {

                chequeImage.setSrc("");

                return;
            }


            File imageFile =
                    new File(imagePath);


            // ========================================
            // LOCAL FILE PATH
            // ========================================

            if (imageFile.exists()
                    && imageFile.isFile()) {

                chequeImage.setContent(

                        new AImage(imageFile)

                );

            }


            // ========================================
            // WEB PATH
            // ========================================

            else {

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


    // ============================================
    // PREVIOUS CHEQUE
    // ============================================

    @Listen("onClick = #previousButton")
    public void previousCheque() {

        if (currentIndex > 0) {

            currentIndex--;

            displayCurrentCheque();

        }

    }


    // ============================================
    // NEXT CHEQUE
    // ============================================

    @Listen("onClick = #nextButton")
    public void nextCheque() {

        if (chequeList != null

                && currentIndex
                < chequeList.size() - 1) {

            currentIndex++;

            displayCurrentCheque();

        }

    }


    // ============================================
    // BACK BUTTON
    // ============================================

    @Listen("onClick = #backButton")
    public void backButton() {

        System.out.println(
                "BACK TO BATCH QUEUE"
        );

        goBackToQueue();

    }


    // ============================================
    // BACK TO BATCH QUEUE
    // ============================================

    private void goBackToQueue() {

        Executions.sendRedirect(

                "/outward/checker/batchesQueue.zul"

        );

    }


    // ============================================
    // SAFE VALUE
    // ============================================

    private String safe(String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return "-";

        }

        return value;

    }

}
