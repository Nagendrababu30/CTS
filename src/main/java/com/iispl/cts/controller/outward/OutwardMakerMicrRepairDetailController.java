package com.iispl.cts.controller.outward;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

import com.iispl.cts.model.outward.ChequeProcessing;
import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.service.outward.OutwardMakerMicrRepairDetailService;
import com.iispl.cts.service.outward.OutwardMakerDashboardService;
import com.iispl.cts.service.outward.OutwardValidationService;


public class OutwardMakerMicrRepairDetailController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;


    // =========================================================
    // UI Components - Header & Progress
    // ========.,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,;=================================================

    @Wire
    private Label batchIdLabel;

    @Wire
    private Label chequeProgressLabel;

    @Wire
    private Label currentStatusLabel;


    // =========================================================
    // UI Components - Metric Pills
    // =========================================================

    @Wire
    private Label totalPillLabel;

    @Wire
    private Label completedPillLabel;

    @Wire
    private Label rejectedPillLabel;

    @Wire
    private Label pendingPillLabel;


    // =========================================================
    // UI Components - Cheque Viewer
    // =========================================================

    @Wire
    private Image frontImage;

    @Wire
    private Image backImage;

    @Wire
    private Button frontImageButton;

    @Wire
    private Button backImageButton;

    @Wire
    private Button zoomInButton;

    @Wire
    private Button zoomOutButton;

    @Wire
    private Button rotateButton;


    // =========================================================
    // UI Components - Form Inputs
    // =========================================================

    @Wire
    private Textbox chequeNumberTextbox;

    @Wire
    private Textbox cityCodeTextbox;

    @Wire
    private Textbox bankCodeTextbox;

    @Wire
    private Textbox branchCodeTextbox;


    // =========================================================
    // UI Components - Navigation & Action Controls
    // =========================================================

    @Wire
    private Button prevButton;

    @Wire
    private Button saveNextButton;

    @Wire
    private Button btnBackToQueue;


    // =========================================================
    // CHECKER RETURN INFORMATION
    //
    // These components are shown ONLY when the cheque was
    // returned from Checker to Maker.
    // =========================================================

    @Wire
    private Vlayout checkerReturnInformationPanel;

    @Wire
    private Label checkerReasonLabel;

    @Wire
    private Label checkerRemarksLabel;


    // =========================================================
    // State Variables
    // =========================================================

    private List<OutwardCheque> cheques;

    private int currentIndex = 0;

    private int totalChequesCount = 0;

    private String batchNumber;

    private double currentScale = 1.0;

    private int currentRotation = 0;


    // =========================================================
    // RETURNED CHEQUE / MICR REPAIR STATE
    // =========================================================

    private boolean returnedMode = false;

    private String returnedChequeNumber;

    /*
     * Dashboard repair type.
     * For the returned MICR queue this is MICR.
     */
    private String repairType;

    private String checkerReasonCode;

    private String checkerRemarks;


    // =========================================================
    // SERVICE DEPENDENCIES
    // =========================================================

    private OutwardMakerMicrRepairDetailService service;

    private OutwardValidationService validationService;


    // =========================================================
    // AFTER COMPOSE
    // =========================================================

    @Override
    public void doAfterCompose(Component comp)
            throws Exception {

        super.doAfterCompose(comp);


        service =
                new OutwardMakerMicrRepairDetailService();

        validationService =
                new OutwardValidationService();


        // =====================================================
        // GET BATCH NUMBER
        // =====================================================

        batchNumber =
                Executions.getCurrent()
                        .getParameter("batchNumber");

        if (batchNumber == null
                || batchNumber.trim().isEmpty()) {

            Messagebox.show(
                    "Batch number is missing.",
                    "Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );

            return;
        }


        batchNumber =
                batchNumber.trim();


        if (batchIdLabel != null) {

            batchIdLabel.setValue(
                    batchNumber
            );
        }


        // =====================================================
        // CHECK RETURNED MODE
        //
        // Dashboard sends:
        //
        // returnMode=HOLD
        //
        // and:
        //
        // chequeNumber=XXXXXX
        //
        // Also support RETURNED because the Data Entry
        // implementation uses RETURNED.
        // =====================================================

        String returnMode =
                Executions.getCurrent()
                        .getParameter("returnMode");


        // -----------------------------------------------------
        // Support malformed browser parameter:
        //
        // amp;returnMode
        // -----------------------------------------------------

        if (returnMode == null
                || returnMode.trim().isEmpty()) {

            returnMode =
                    Executions.getCurrent()
                            .getParameter(
                                    "amp;returnMode"
                            );
        }


        returnedMode =
                "HOLD".equalsIgnoreCase(
                        returnMode
                )
                || "RETURNED".equalsIgnoreCase(
                        returnMode
                );


        // =====================================================
        // GET SPECIFIC RETURNED CHEQUE NUMBER
        // =====================================================

        returnedChequeNumber =
                Executions.getCurrent()
                        .getParameter(
                                "chequeNumber"
                        );


        if (returnedChequeNumber == null
                || returnedChequeNumber.trim().isEmpty()) {

            returnedChequeNumber =
                    Executions.getCurrent()
                            .getParameter(
                                    "amp;chequeNumber"
                            );
        }

        /*
         * =====================================================
         * GET REPAIR TYPE
         * =====================================================
         *
         * Dashboard sends repairType=MICR for the
         * returned MICR queue.
         */
        repairType =
                Executions.getCurrent()
                        .getParameter("repairType");

        if (repairType == null
                || repairType.trim().isEmpty()) {

            repairType =
                    Executions.getCurrent()
                            .getParameter("amp;repairType");
        }


        System.out.println(
                "======================================"
        );

        System.out.println(
                "OUTWARD MAKER MICR REPAIR DETAIL"
        );

        System.out.println(
                "Batch Number : "
                        + batchNumber
        );

        System.out.println(
                "Return Mode : "
                        + returnMode
        );

        System.out.println(
                "Returned Mode : "
                        + returnedMode
        );

        System.out.println(
                "Returned Cheque : "
                        + returnedChequeNumber
        );

        System.out.println(
                "======================================"
        );


        // =====================================================
        // LOAD CHEQUES
        // =====================================================

        loadCheques();
    }


    // =========================================================
    // LOAD CHEQUES
    // =========================================================

    private void loadCheques() throws SQLException {

        // =====================================================
        // NORMAL MICR REPAIR FLOW
        //
        // EXISTING LOGIC PRESERVED
        // =====================================================

        if (!returnedMode) {

            cheques =
                    service.getMicrErrorCheques(
                            batchNumber
                    );


            if (cheques == null
                    || cheques.isEmpty()) {

                Messagebox.show(
                        "No MICR error cheques found for this batch.",
                        "Information",
                        Messagebox.OK,
                        Messagebox.INFORMATION
                );

                return;
            }


            totalChequesCount =
                    cheques.size();

            currentIndex = 0;


            updateHeaderPillMetrics();

            loadCurrentCheque();

            return;
        }


        // =====================================================
        // RETURNED MICR CHEQUE FLOW
        //
        // IMPORTANT:
        //
        // For Dashboard -> MICR repair, filter at CHEQUE level
        // using this cheque's checker_reason_code.
        //
        // The batch number alone must NEVER cause all returned
        // cheques in the batch to enter the MICR queue.
        // =====================================================

        List<OutwardCheque> loadedCheques =
                new OutwardMakerDashboardService()
                        .getCheques(batchNumber);


        if (loadedCheques == null
                || loadedCheques.isEmpty()) {

            Messagebox.show(
                    "No cheque data available for batch "
                            + batchNumber
                            + ".",
                    "Information",
                    Messagebox.OK,
                    Messagebox.INFORMATION
            );

            return;
        }


        List<OutwardCheque> returnedCheques =
                new ArrayList<>();


        OutwardMakerDashboardService dashboardService =
                new OutwardMakerDashboardService();


        // =====================================================
        // KEEP ONLY SENT_BACK_TO_MAKER + SEND_BACK + MICR
        // =====================================================

        for (OutwardCheque cheque
                : loadedCheques) {

            if (cheque == null) {
                continue;
            }

            String status =
                    cheque.getChequeStatus();

            if (status == null
                    || !"SENT_BACK_TO_MAKER"
                            .equalsIgnoreCase(
                                    status.trim()
                            )) {
                continue;
            }

            String chequeNumber =
                    cheque.getChequeNumber();

            if (chequeNumber == null
                    || chequeNumber.trim().isEmpty()) {
                continue;
            }

            ChequeProcessing processing =
                    dashboardService
                            .getChequeProcessing(
                                    batchNumber,
                                    chequeNumber.trim()
                            );

            if (processing == null) {
                continue;
            }

            String checkerAction =
                    processing.getCheckerAction();

            String checkerReason =
                    processing.getCheckerReasonCode();

            if (checkerAction == null
                    || !"SEND_BACK"
                            .equalsIgnoreCase(
                                    checkerAction.trim()
                            )) {
                continue;
            }

            /*
             * Strict MICR filtering:
             * only the actual cheque reason decides whether
             * this cheque belongs to the MICR repair queue.
             */
            if (!isMicrRepairReason(checkerReason)) {
                continue;
            }

            returnedCheques.add(cheque);
        }


        // =====================================================
        // OPTIONAL INITIAL CHEQUE SELECTION
        //
        // Dashboard may send chequeNumber so the first screen
        // opens on the clicked cheque. It does NOT reduce the
        // queue to one cheque.
        // =====================================================

        if (returnedChequeNumber != null
                && !returnedChequeNumber
                        .trim()
                        .isEmpty()) {

            String requestedCheque =
                    returnedChequeNumber.trim();

            for (int i = 0;
                    i < returnedCheques.size();
                    i++) {

                OutwardCheque cheque =
                        returnedCheques.get(i);

                if (cheque != null
                        && cheque.getChequeNumber() != null
                        && requestedCheque
                                .equalsIgnoreCase(
                                        cheque.getChequeNumber()
                                                .trim()
                                )) {

                    currentIndex = i;
                    break;
                }
            }
        }


        // =====================================================
        // NO RETURNED MICR CHEQUE
        // =====================================================

        if (returnedCheques.isEmpty()) {

            Messagebox.show(
                    "No returned MICR cheque is available "
                            + "for batch "
                            + batchNumber
                            + ".",
                    "Checker Return",
                    Messagebox.OK,
                    Messagebox.ERROR
            );

            return;
        }


        // =====================================================
        // ONLY RETURNED MICR CHEQUES ARE USED
        // =====================================================

        cheques =
                returnedCheques;


        totalChequesCount =
                cheques.size();


        // Make sure the requested cheque is used as the
        // starting cheque, otherwise start from the first MICR
        // cheque in the filtered queue.
        if (currentIndex < 0
                || currentIndex >= cheques.size()) {

            currentIndex = 0;
        }


        updateHeaderPillMetrics();

        loadCurrentCheque();


        /*
         * Returned MICR mode can contain multiple MICR cheques.
         * Therefore Prev/Next navigation is allowed inside the
         * filtered MICR subset.
         */
        if (prevButton != null) {

            prevButton.setDisabled(
                    currentIndex == 0
            );
        }
    }


    // =========================================================
    // MICR REPAIR REASON CHECK
    // =========================================================
    //
    // This check is intentionally cheque-level. The batch number
    // is not enough to decide whether a returned cheque belongs
    // to the MICR repair queue.
    // =========================================================

    private boolean isMicrRepairReason(
            String reasonCode) {

        if (reasonCode == null
                || reasonCode.trim().isEmpty()) {

            return false;
        }

        String cleanReason =
                reasonCode.trim()
                        .toUpperCase()
                        .replace("-", "_")
                        .replace(" ", "_");

        return "MICR".equals(cleanReason)
                || "MICR_CORRECTION".equals(cleanReason)
                || "MICR_MISMATCH".equals(cleanReason);
    }


    // =========================================================
    // HEADER METRICS
    // =========================================================

    private void updateHeaderPillMetrics() {

        if (totalPillLabel != null) {

            totalPillLabel.setValue(
                    String.valueOf(
                            totalChequesCount
                    )
            );
        }


        if (completedPillLabel != null) {

            completedPillLabel.setValue(
                    String.valueOf(
                            currentIndex
                    )
            );
        }


        if (pendingPillLabel != null) {

            pendingPillLabel.setValue(
                    String.valueOf(
                            totalChequesCount
                                    - currentIndex
                    )
            );
        }
    }


    // =========================================================
    // LOAD CURRENT CHEQUE
    // =========================================================

    private void loadCurrentCheque() {

        if (cheques == null
                || cheques.isEmpty()) {

            return;
        }


        // =====================================================
        // STRICTLY ENFORCE BOUNDS
        // =====================================================

        if (currentIndex < 0) {

            currentIndex = 0;
        }


        if (currentIndex >= cheques.size()) {

            currentIndex =
                    cheques.size() - 1;
        }


        // =====================================================
        // UPDATE HEADER METRICS
        // =====================================================

        updateHeaderPillMetrics();


        if (chequeProgressLabel != null) {

            chequeProgressLabel.setValue(
                    "Cheque "
                            + (currentIndex + 1)
                            + " of "
                            + totalChequesCount
            );
        }


        // =====================================================
        // PREV BUTTON
        // =====================================================

        if (prevButton != null) {

            prevButton.setDisabled(
                    currentIndex == 0
            );
        }


        OutwardCheque cheque =
                cheques.get(currentIndex);


        // =====================================================
        // SET CHEQUE NUMBER
        // =====================================================

        if (chequeNumberTextbox != null) {

            chequeNumberTextbox.setValue(
                    safe(
                            cheque.getChequeNumber()
                    )
            );
        }


        // =====================================================
        // VALIDATE MICR COMPONENTS
        // =====================================================

        boolean cityValid =
                validationService.isValidMicrCode(
                        cheque.getCityCode()
                );


        boolean bankValid =
                validationService.isValidMicrCode(
                        cheque.getBankCode()
                );


        boolean branchValid =
                validationService.isValidMicrCode(
                        cheque.getBranchCode()
                );


        // =====================================================
        // CITY CODE
        // =====================================================

        if (cityCodeTextbox != null) {

            cityCodeTextbox.setSclass(
                    cityValid
                            ? "micr-component-corrected"
                            : "micr-component-error"
            );

            cityCodeTextbox.setValue(
                    safe(
                            cheque.getCityCode()
                    )
            );
        }


        // =====================================================
        // BANK CODE
        // =====================================================

        if (bankCodeTextbox != null) {

            bankCodeTextbox.setSclass(
                    bankValid
                            ? "micr-component-corrected"
                            : "micr-component-error"
            );

            bankCodeTextbox.setValue(
                    safe(
                            cheque.getBankCode()
                    )
            );
        }


        // =====================================================
        // BRANCH CODE
        // =====================================================

        if (branchCodeTextbox != null) {

            branchCodeTextbox.setSclass(
                    branchValid
                            ? "micr-component-corrected"
                            : "micr-component-error"
            );

            branchCodeTextbox.setValue(
                    safe(
                            cheque.getBranchCode()
                    )
            );
        }


        // =====================================================
        // IMAGES
        // =====================================================

        loadImages(cheque);

        resetImageTransformations();


        // =====================================================
        // STATUS
        // =====================================================

        updateStatusLabel(
                cityValid
                        && bankValid
                        && branchValid
        );


        if (saveNextButton != null) {

            saveNextButton.setDisabled(
                    false
            );
        }


        // =====================================================
        // CHECKER RETURN INFORMATION
        //
        // ONLY FOR SENT_TO_MAKER / RETURNED MODE
        // =====================================================

        if (returnedMode) {

            loadCheckerReturnInformation(
                    cheque
            );

        } else {

            hideCheckerReturnInformation();
        }
    }


    // =========================================================
    // LOAD CHECKER RETURN INFORMATION
    // =========================================================

    private void loadCheckerReturnInformation(
            OutwardCheque cheque) {

        checkerReasonCode = null;

        checkerRemarks = null;


        if (cheque == null) {

            hideCheckerReturnInformation();

            return;
        }


        String chequeNumber =
                cheque.getChequeNumber();


        if (chequeNumber == null
                || chequeNumber.trim().isEmpty()) {

            hideCheckerReturnInformation();

            return;
        }


        try {

            // =================================================
            // GET CHECKER PROCESSING
            //
            // IMPORTANT:
            // Reason belongs to THIS cheque.
            // =================================================

            OutwardMakerDashboardService
                    dashboardService =
                    new OutwardMakerDashboardService();


            ChequeProcessing processing =
                    dashboardService
                            .getChequeProcessing(
                                    batchNumber,
                                    chequeNumber.trim()
                            );


            if (processing != null) {

                checkerReasonCode =
                        processing
                                .getCheckerReasonCode();


                String checkerAction =
                        processing
                                .getCheckerAction();


                // -------------------------------------------------
                // Returned cheque must have SEND_BACK action.
                // -------------------------------------------------

                if (checkerAction != null
                        && !"SEND_BACK"
                                .equalsIgnoreCase(
                                        checkerAction.trim()
                                )) {

                    System.out.println(
                            "WARNING: Checker action for "
                                    + chequeNumber
                                    + " is "
                                    + checkerAction
                    );
                }
            }


            // =====================================================
            // CHECKER REMARKS
            // =====================================================

            checkerRemarks =
                    cheque.getCheckerRemarks();


            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "MICR CHECKER RETURN INFORMATION"
            );

            System.out.println(
                    "Batch Number : "
                            + batchNumber
            );

            System.out.println(
                    "Cheque Number : "
                            + chequeNumber
            );

            System.out.println(
                    "Checker Reason Code : "
                            + checkerReasonCode
            );

            System.out.println(
                    "Checker Remarks : "
                            + checkerRemarks
            );

            System.out.println(
                    "======================================"
            );


            updateCheckerReturnPanel();


        } catch (Exception e) {

            e.printStackTrace();


            System.err.println(
                    "Unable to load Checker return "
                            + "information for cheque "
                            + chequeNumber
            );


            hideCheckerReturnInformation();
        }
    }


    // =========================================================
    // UPDATE CHECKER RETURN PANEL
    // =========================================================

    private void updateCheckerReturnPanel() {

        if (checkerReturnInformationPanel == null) {

            return;
        }


        if (!returnedMode) {

            checkerReturnInformationPanel
                    .setVisible(false);

            return;
        }


        checkerReturnInformationPanel
                .setVisible(true);


        // =====================================================
        // REASON
        // =====================================================

        if (checkerReasonLabel != null) {

            String reason =
                    getDisplayReason(
                            checkerReasonCode
                    );


            if (reason == null
                    || reason.trim().isEmpty()) {

                reason =
                        "Not specified";
            }


            checkerReasonLabel.setValue(
                    reason.trim()
            );
        }


        // =====================================================
        // REMARKS
        // =====================================================

        if (checkerRemarksLabel != null) {

            String remarks =
                    checkerRemarks;


            if (remarks == null
                    || remarks.trim().isEmpty()) {

                remarks =
                        "No remarks provided";
            }


            checkerRemarksLabel.setValue(
                    remarks.trim()
            );
        }
    }


    // =========================================================
    // DISPLAY REASON
    //
    // Converts DB reason code into the same type of readable
    // text shown by the Checker screen.
    // =========================================================

    private String getDisplayReason(
            String reasonCode) {

        if (reasonCode == null
                || reasonCode.trim().isEmpty()) {

            return "Not specified";
        }


        String cleanReason =
                reasonCode.trim()
                        .toUpperCase()
                        .replace("-", "_")
                        .replace(" ", "_");


        if ("MICR".equals(cleanReason)
                || "MICR_CORRECTION".equals(cleanReason)
                || "MICR_MISMATCH".equals(cleanReason)) {

            return "MICR correction required";
        }


        if ("ACCOUNT_DETAILS_CORRECTION"
                .equals(cleanReason)
                || "ACCOUNT_DETAILS_MISMATCH"
                        .equals(cleanReason)
                || "ACCOUNT_MISMATCH"
                        .equals(cleanReason)) {

            return "Account details correction required";
        }


        if ("AMOUNT_CORRECTION"
                .equals(cleanReason)
                || "AMOUNT_MISMATCH"
                        .equals(cleanReason)) {

            return "Amount correction required";
        }


        if ("DATE_CORRECTION"
                .equals(cleanReason)
                || "DATE_MISMATCH"
                        .equals(cleanReason)
                || "CHEQUE_DATE_INVALID"
                        .equals(cleanReason)) {

            return "Date correction required";
        }


        if ("IMAGE_CORRECTION"
                .equals(cleanReason)) {

            return "Image correction required";
        }


        if ("CHEQUE_DETAILS_INVALID"
                .equals(cleanReason)) {

            return "Cheque details correction required";
        }


        if ("DUPLICATE_CHEQUE"
                .equals(cleanReason)) {

            return "Duplicate cheque";
        }


        if ("PAYEE_DETAILS_CORRECTION"
                .equals(cleanReason)
                || "PAYEE_DETAILS_MISMATCH"
                        .equals(cleanReason)) {

            return "Payee details correction required";
        }


        return reasonCode.trim();
    }


    // =========================================================
    // HIDE CHECKER RETURN PANEL
    // =========================================================

    private void hideCheckerReturnInformation() {

        if (checkerReturnInformationPanel != null) {

            checkerReturnInformationPanel
                    .setVisible(false);
        }


        if (checkerReasonLabel != null) {

            checkerReasonLabel.setValue(
                    ""
            );
        }


        if (checkerRemarksLabel != null) {

            checkerRemarksLabel.setValue(
                    ""
            );
        }
    }


    // =========================================================
    // LOAD IMAGES
    // =========================================================

    private void loadImages(
            OutwardCheque cheque) {

        if (cheque == null) {

            return;
        }


        String frontPath =
                cheque.getFrontImagePath();


        String backPath =
                cheque.getBackImagePath();


        if (frontImage != null) {

            if (frontPath != null
                    && !frontPath.trim().isEmpty()) {

                frontImage.setSrc(
                        frontPath.trim()
                );

            } else {

                frontImage.setSrc(
                        null
                );
            }
        }


        if (backImage != null) {

            if (backPath != null
                    && !backPath.trim().isEmpty()) {

                backImage.setSrc(
                        backPath.trim()
                );

            } else {

                backImage.setSrc(
                        null
                );
            }
        }


        showFrontImage();
    }


    // =========================================================
    // FRONT IMAGE
    // =========================================================

    @Listen("onClick = #frontImageButton")
    public void showFrontImage() {

        if (frontImage != null) {

            frontImage.setVisible(
                    true
            );
        }


        if (backImage != null) {

            backImage.setVisible(
                    false
            );
        }
    }


    // =========================================================
    // BACK IMAGE
    // =========================================================

    @Listen("onClick = #backImageButton")
    public void showBackImage() {

        if (frontImage != null) {

            frontImage.setVisible(
                    false
            );
        }


        if (backImage != null) {

            backImage.setVisible(
                    true
            );
        }
    }


    // =========================================================
    // ZOOM IN
    // =========================================================

    @Listen("onClick = #zoomInButton")
    public void zoomIn() {

        currentScale += 0.2;

        applyImageStyle();
    }


    // =========================================================
    // ZOOM OUT
    // =========================================================

    @Listen("onClick = #zoomOutButton")
    public void zoomOut() {

        if (currentScale > 0.4) {

            currentScale -= 0.2;

            applyImageStyle();
        }
    }


    // =========================================================
    // ROTATE IMAGE
    // =========================================================

    @Listen("onClick = #rotateButton")
    public void rotateImage() {

        currentRotation =
                (currentRotation + 90)
                        % 360;

        applyImageStyle();
    }


    // =========================================================
    // RESET IMAGE TRANSFORMATIONS
    // =========================================================

    private void resetImageTransformations() {

        currentScale = 1.0;

        currentRotation = 0;

        applyImageStyle();
    }


    // =========================================================
    // APPLY IMAGE STYLE
    // =========================================================

    private void applyImageStyle() {

        String transformStyle =
                String.format(
                        "transform: scale(%.2f) rotate(%ddeg);",
                        currentScale,
                        currentRotation
                );


        if (frontImage != null) {

            frontImage.setStyle(
                    transformStyle
            );
        }


        if (backImage != null) {

            backImage.setStyle(
                    transformStyle
            );
        }
    }


    // =========================================================
    // MICR FIELD VALIDATION
    // =========================================================

    @Listen(
            "onChange = #cityCodeTextbox, "
                    + "#bankCodeTextbox, "
                    + "#branchCodeTextbox; "
                    + "onChanging = #cityCodeTextbox, "
                    + "#bankCodeTextbox, "
                    + "#branchCodeTextbox"
    )
    public void checkMicrFields(
            Event event) {

        String cityCode =
                cityCodeTextbox != null
                        ? cityCodeTextbox.getValue()
                        : "";


        String bankCode =
                bankCodeTextbox != null
                        ? bankCodeTextbox.getValue()
                        : "";


        String branchCode =
                branchCodeTextbox != null
                        ? branchCodeTextbox.getValue()
                        : "";


        // =====================================================
        // EXTRACT LIVE VALUE DURING onChanging
        // =====================================================

        if (event instanceof InputEvent) {

            InputEvent inputEvent =
                    (InputEvent) event;


            Component target =
                    event.getTarget();


            if (target == cityCodeTextbox) {

                cityCode =
                        inputEvent.getValue();

            } else if (target == bankCodeTextbox) {

                bankCode =
                        inputEvent.getValue();

            } else if (target == branchCodeTextbox) {

                branchCode =
                        inputEvent.getValue();
            }
        }


        boolean isCityValid =
                validationService.isValidMicrCode(
                        cityCode
                );


        boolean isBankValid =
                validationService.isValidMicrCode(
                        bankCode
                );


        boolean isBranchValid =
                validationService.isValidMicrCode(
                        branchCode
                );


        // =====================================================
        // APPLY TEXTBOX COLORS
        // =====================================================

        if (cityCodeTextbox != null) {

            cityCodeTextbox.setSclass(
                    isCityValid
                            ? "micr-component-corrected"
                            : "micr-component-error"
            );
        }


        if (bankCodeTextbox != null) {

            bankCodeTextbox.setSclass(
                    isBankValid
                            ? "micr-component-corrected"
                            : "micr-component-error"
            );
        }


        if (branchCodeTextbox != null) {

            branchCodeTextbox.setSclass(
                    isBranchValid
                            ? "micr-component-corrected"
                            : "micr-component-error"
            );
        }


        // =====================================================
        // UPDATE STATUS LABEL
        // =====================================================

        boolean isAllValid =
                isCityValid
                        && isBankValid
                        && isBranchValid;


        updateStatusLabel(
                isAllValid
        );
    }


    // =========================================================
    // UPDATE STATUS LABEL
    // =========================================================

    private void updateStatusLabel(
            boolean isAllValid) {

        if (currentStatusLabel != null) {

            if (isAllValid) {

                currentStatusLabel.setValue(
                        "MICR_REPAIRED"
                );

                currentStatusLabel.setSclass(
                        "status-label status-repaired"
                );

            } else {

                currentStatusLabel.setValue(
                        "MICR_ERROR"
                );

                currentStatusLabel.setSclass(
                        "status-label status-error"
                );
            }
        }
    }


    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safe(
            String value) {

        return value == null
                ? ""
                : value;
    }


    // =========================================================
    // SAVE & NEXT
    //
    // EXISTING LOGIC PRESERVED
    // =========================================================

    @Listen("onClick = #saveNextButton")
    public void saveAndNext() {

        if (cheques == null
                || cheques.isEmpty()) {

            return;
        }


        OutwardCheque cheque =
                cheques.get(currentIndex);


        String cityCode =
                cityCodeTextbox != null
                        ? cityCodeTextbox
                                .getValue()
                                .trim()
                        : "";


        String bankCode =
                bankCodeTextbox != null
                        ? bankCodeTextbox
                                .getValue()
                                .trim()
                        : "";


        String branchCode =
                branchCodeTextbox != null
                        ? branchCodeTextbox
                                .getValue()
                                .trim()
                        : "";


        // =====================================================
        // CHECK IF USER MADE CHANGES
        // =====================================================

        boolean isModified =
                !cityCode.equals(
                        cheque.getCityCode()
                )
                || !bankCode.equals(
                        cheque.getBankCode()
                )
                || !branchCode.equals(
                        cheque.getBranchCode()
                );


        // =====================================================
        // ONLY SAVE IF MODIFIED
        // =====================================================

        if (isModified) {

            // =================================================
            // VALIDATION
            // =================================================

            if (!validationService.isValidMicrCode(cityCode)
                    || !validationService.isValidMicrCode(bankCode)
                    || !validationService.isValidMicrCode(branchCode)) {

                Messagebox.show(
                        "Please enter valid 3-digit MICR codes for City, Bank, and Branch.",
                        "MICR Validation Error",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION
                );

                return;
            }


            // =================================================
            // DATABASE UPDATE
            // =================================================

            boolean updated =
                    service.updateCorrectedMicr(
                            batchNumber,
                            cheque.getChequeNumber(),
                            cityCode,
                            bankCode,
                            branchCode,
                            returnedMode
                    );

            if (!updated) {
                Messagebox.show(
                        "Database Alert: MICR repair could not be saved to the database. Please check DB connection or logs.",
                        "Database Error Alert",
                        Messagebox.OK,
                        Messagebox.ERROR
                );
                return;
            }

            // =================================================
            // UPDATE IN-MEMORY STATE
            // =================================================

            cheque.setCityCode(
                    cityCode
            );

            cheque.setBankCode(
                    bankCode
            );

            cheque.setBranchCode(
                    branchCode
            );

            if (returnedMode) {
                cheque.setChequeStatus("RE_VERIFIED");
            } else {
                cheque.setChequeStatus("MICR_REPAIRED");
            }
        }


        // =====================================================
        // NAVIGATE TO NEXT CHEQUE
        // =====================================================

        if (currentIndex < cheques.size() - 1) {

            currentIndex++;

            loadCurrentCheque();

        } else {

            /*
             * =====================================================
             * COMPLETION
             * =====================================================
             *
             * NORMAL MODE:
             * Keep the existing MICR batch-status logic exactly
             * as before.
             *
             * RETURNED MODE:
             * Do NOT complete the whole batch here. A returned
             * batch may still contain Data Entry repairs (or other
             * returned reasons). The dashboard will naturally
             * remove this MICR action once these MICR cheques are
             * no longer SENT_BACK_TO_MAKER.
             */
            if (!returnedMode) {

                boolean remaining =
                        service.hasRemainingMicrErrors(
                                batchNumber
                        );

                if (!remaining) {

                    service.updateBatchStatus(
                            batchNumber
                    );
                }
            }


            Messagebox.show(
                    returnedMode
                            ? "MICR repair completed for all returned MICR cheques in this queue."
                            : "MICR Repair Completed for all cheques in this batch.",
                    "Success",
                    Messagebox.OK,
                    Messagebox.INFORMATION,
                    e -> Executions
                            .getCurrent()
                            .sendRedirect(
                                    returnedMode
                                            ? "/zul/outward/outward-maker/outward-maker-dashboard.zul"
                                            : "/zul/outward/outward-maker/outward-maker-data-entry.zul"
                            )
            );
        }
    }


    // =========================================================
    // PREVIOUS CHEQUE
    // =========================================================

    @Listen("onClick = #prevButton")
    public void previousCheque() {

        if (cheques == null
                || cheques.isEmpty()) {

            return;
        }


        if (currentIndex > 0) {

            currentIndex--;

            loadCurrentCheque();

        } else {

            Messagebox.show(
                    "This is the first cheque.",
                    "Information",
                    Messagebox.OK,
                    Messagebox.INFORMATION
            );
        }
    }


    // =========================================================
    // BACK TO MICR QUEUE
    // =========================================================

    @Listen("onClick = #btnBackToQueue")
    public void backToMicrQueue() {

        Executions
                .getCurrent()
                .sendRedirect(
                        "outward-maker-micr-repair.zul"
                );
    }
}