package com.iispl.cts.controller.outward;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.service.outward.OutwardMakerAmountAccountService;

public class OutwardMakerAmountAccountDetailController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    @Wire
    private Label batchNumberLabel;

    @Wire
    private Label totalChequesLabel;

    @Wire
    private Label batchStatusLabel;

    @Wire
    private Label verificationStatusLabel;

    @Wire
    private Listbox chequeListbox;

    @Wire
    private Label messageLabel;

    @Wire
    private Button verifyButton;

    @Wire
    private Button backButton;

    private OutwardMakerAmountAccountService service;

    private String batchNumber;

    private OutwardBatch currentBatch;

    private List<OutwardCheque> currentCheques;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");


    // ============================================================
    // PAGE LOAD
    // ============================================================

    @Override
    public void doAfterCompose(Component comp) throws Exception {

        super.doAfterCompose(comp);

        service = new OutwardMakerAmountAccountService();

        batchNumber = Executions.getCurrent()
                .getParameter("batchNumber");

        if (batchNumber == null || batchNumber.trim().isEmpty()) {

            showError("Batch number is missing.");

            verifyButton.setDisabled(true);

            return;
        }

        batchNumber = batchNumber.trim();

        loadBatchDetails();
    }


    // ============================================================
    // LOAD BATCH DETAILS
    // ============================================================

    private void loadBatchDetails() {

        try {

            clearMessage();

            /*
             * The amount/account service already performs the
             * automatic account validation.
             *
             * Reload the batches so that the current batch status
             * reflects the latest validation result.
             */

            List<OutwardBatch> batches =
                    service.getAmountAccountBatches();

            currentBatch = null;

            if (batches != null) {

                for (OutwardBatch batch : batches) {

                    if (batch != null
                            && batch.getBatchNumber() != null
                            && batch.getBatchNumber()
                                    .equalsIgnoreCase(batchNumber)) {

                        currentBatch = batch;
                        break;
                    }
                }
            }

            if (currentBatch == null) {

                showError(
                        "Batch " + batchNumber
                                + " is not available for Amount And Account verification."
                );

                verifyButton.setDisabled(true);

                return;
            }

            displayBatchDetails();

            /*
             * Load cheque data.
             *
             * This controller does not access the DAO directly.
             * The service is responsible for business/data access.
             */

            currentCheques =
                    service.getChequesForBatch(batchNumber);

            displayCheques();

            updateVerificationButton();

        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "Unable to load batch details: "
                            + getExceptionMessage(e)
            );

            verifyButton.setDisabled(true);
        }
    }


    // ============================================================
    // DISPLAY BATCH
    // ============================================================

    private void displayBatchDetails() {

        batchNumberLabel.setValue(
                safeValue(currentBatch.getBatchNumber())
        );

        totalChequesLabel.setValue(
                String.valueOf(currentBatch.getNumberOfCheques())
        );

        batchStatusLabel.setValue(
                safeValue(currentBatch.getBatchStatus())
        );

        verificationStatusLabel.setValue(
                getVerificationStatus(
                        currentBatch.getBatchStatus()
                )
        );
    }


    // ============================================================
    // DISPLAY CHEQUES
    // ============================================================

    private void displayCheques() {

        chequeListbox.getItems().clear();

        if (currentCheques == null
                || currentCheques.isEmpty()) {

            return;
        }

        for (OutwardCheque cheque : currentCheques) {

            if (cheque == null) {
                continue;
            }

            Listitem item = new Listitem();

            // ----------------------------------------------------
            // CHEQUE NUMBER
            // ----------------------------------------------------

            Listcell chequeNumberCell = new Listcell();

            chequeNumberCell.appendChild(
                    new Label(
                            safeValue(
                                    cheque.getChequeNumber()
                            )
                    )
            );

            item.appendChild(chequeNumberCell);


            // ----------------------------------------------------
            // ACCOUNT NUMBER
            // ----------------------------------------------------

            Listcell accountCell = new Listcell();

            accountCell.appendChild(
                    new Label(
                            safeValue(
                                    getAccountNumber(cheque)
                            )
                    )
            );

            item.appendChild(accountCell);


            // ----------------------------------------------------
            // AMOUNT
            // ----------------------------------------------------

            Listcell amountCell = new Listcell();

            amountCell.appendChild(
                    new Label(
                            formatAmount(
                                    cheque.getAmount()
                            )
                    )
            );

            item.appendChild(amountCell);


            // ----------------------------------------------------
            // CHEQUE DATE
            // ----------------------------------------------------

            Listcell dateCell = new Listcell();

            dateCell.appendChild(
                    new Label(
                            formatDate(
                                    cheque.getChequeDate()
                            )
                    )
            );

            item.appendChild(dateCell);


            // ----------------------------------------------------
            // CHEQUE STATUS
            // ----------------------------------------------------

            Listcell statusCell = new Listcell();

            statusCell.appendChild(
                    new Label(
                            safeValue(
                                    cheque.getChequeStatus()
                            )
                    )
            );

            item.appendChild(statusCell);


            // ----------------------------------------------------
            // VALIDATION RESULT
            // ----------------------------------------------------

            Listcell validationCell = new Listcell();

            validationCell.appendChild(
                    new Label(
                            getValidationStatus(
                                    cheque
                            )
                    )
            );

            item.appendChild(validationCell);

            chequeListbox.appendChild(item);
        }
    }


    // ============================================================
    // VERIFY BATCH
    // ============================================================

    @Listen("onClick = #verifyButton")
    public void verifyBatch() {

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            showError("Batch number is missing.");

            return;
        }

        try {

            verifyButton.setDisabled(true);

            clearMessage();

            boolean verified =
                    service.verifyBatch(batchNumber);

            if (verified) {

                showSuccess(
                        "Account and Amount verification completed successfully."
                );

            } else {

                showError(
                        "Account and Amount verification failed. "
                                + "Batch has been marked as ACCOUNT_ERROR."
                );
            }

            /*
             * Reload the batch so the UI displays the latest
             * database status.
             */

            loadBatchDetails();

        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "Unable to verify batch: "
                            + getExceptionMessage(e)
            );

            verifyButton.setDisabled(false);
        }
    }


    // ============================================================
    // BACK BUTTON
    // ============================================================

    @Listen("onClick = #backButton")
    public void goBack() {

        Executions.sendRedirect(
                "outward-maker-amount-account.zul"
        );
    }


    // ============================================================
    // BUTTON STATE
    // ============================================================

    private void updateVerificationButton() {

        if (currentBatch == null) {

            verifyButton.setDisabled(true);

            return;
        }

        String status =
                currentBatch.getBatchStatus();

        if (status == null) {

            verifyButton.setDisabled(true);

            return;
        }

        /*
         * Do not allow verification again after the batch has
         * already completed account verification.
         */

        if ("ACCOUNT_VERIFICATION_COMPLETED"
                .equalsIgnoreCase(status)) {

            verifyButton.setDisabled(true);

            return;
        }

        /*
         * ACCOUNT_ERROR can be re-verified after correction.
         */

        if ("ACCOUNT_ERROR"
                .equalsIgnoreCase(status)) {

            verifyButton.setDisabled(false);

            return;
        }

        /*
         * Only batches that passed Data Entry Verification or
         * are already in Account Error are eligible.
         */

        if ("DATA_ENTRY_VERIFICATION_COMPLETED"
                .equalsIgnoreCase(status)) {

            verifyButton.setDisabled(false);

            return;
        }

        verifyButton.setDisabled(true);
    }


    // ============================================================
    // ACCOUNT NUMBER
    // ============================================================

    private String getAccountNumber(
            OutwardCheque cheque) {

        /*
         * OutwardCheque model contains depositorAccountNumber.
         *
         * The database payee_account_number is mapped to this
         * existing model field because the model does not contain
         * a separate payeeAccountNumber field.
         */

        if (cheque.getDepositorAccountNumber() != null
                && !cheque.getDepositorAccountNumber()
                        .trim().isEmpty()) {

            return cheque.getDepositorAccountNumber();
        }

        if (cheque.getDrawerAccountNumber() != null
                && !cheque.getDrawerAccountNumber()
                        .trim().isEmpty()) {

            return cheque.getDrawerAccountNumber();
        }

        return "-";
    }


    // ============================================================
    // VALIDATION STATUS
    // ============================================================

    private String getValidationStatus(
            OutwardCheque cheque) {

        if (cheque == null) {
            return "ERROR";
        }

        String status =
                cheque.getChequeStatus();

        if (status == null) {
            return "PENDING";
        }

        if ("ACCOUNT_VERIFIED"
                .equalsIgnoreCase(status)) {

            return "ACCOUNT VERIFIED";
        }

        if ("ACCOUNT_ERROR"
                .equalsIgnoreCase(status)) {

            return "ACCOUNT ERROR";
        }

        if ("REJECTED"
                .equalsIgnoreCase(status)) {

            return "REJECTED";
        }

        return status;
    }


    // ============================================================
    // BATCH VERIFICATION STATUS
    // ============================================================

    private String getVerificationStatus(
            String batchStatus) {

        if (batchStatus == null
                || batchStatus.trim().isEmpty()) {

            return "-";
        }

        if ("DATA_ENTRY_VERIFICATION_COMPLETED"
                .equalsIgnoreCase(batchStatus)) {

            return "READY FOR ACCOUNT VERIFICATION";
        }

        if ("ACCOUNT_VERIFICATION_COMPLETED"
                .equalsIgnoreCase(batchStatus)) {

            return "ACCOUNT VERIFICATION COMPLETED";
        }

        if ("ACCOUNT_ERROR"
                .equalsIgnoreCase(batchStatus)) {

            return "ACCOUNT ERROR";
        }

        return batchStatus;
    }


    // ============================================================
    // AMOUNT FORMAT
    // ============================================================

    private String formatAmount(
            BigDecimal amount) {

        if (amount == null) {
            return "-";
        }

        return amount.toPlainString();
    }


    // ============================================================
    // DATE FORMAT
    // ============================================================

    private String formatDate(
            LocalDate date) {

        if (date == null) {
            return "-";
        }

        return DATE_FORMAT.format(date);
    }


    // ============================================================
    // SAFE VALUE
    // ============================================================

    private String safeValue(
            String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return "-";
        }

        return value;
    }


    // ============================================================
    // ERROR MESSAGE
    // ============================================================

    private String getExceptionMessage(
            Exception e) {

        if (e == null) {
            return "Unknown error.";
        }

        String message = e.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            return e.getClass().getSimpleName();
        }

        return message;
    }


    // ============================================================
    // SHOW ERROR
    // ============================================================

    private void showError(
            String message) {

        if (messageLabel == null) {
            return;
        }

        messageLabel.setValue(
                safeValue(message)
        );

        messageLabel.setStyle(
                "font-size:14px;color:#C0392B;"
        );
    }


    // ============================================================
    // SHOW SUCCESS
    // ============================================================

    private void showSuccess(
            String message) {

        if (messageLabel == null) {
            return;
        }

        messageLabel.setValue(
                safeValue(message)
        );

        messageLabel.setStyle(
                "font-size:14px;color:#1E8449;"
        );
    }


    // ============================================================
    // CLEAR MESSAGE
    // ============================================================

    private void clearMessage() {

        if (messageLabel != null) {
            messageLabel.setValue("");
        }
    }
}