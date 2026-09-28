package com.cts.inward.controller;

import java.io.File;
import org.zkoss.zk.ui.util.Composer;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.zkoss.image.AImage;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Window;
import com.cts.admin.model.User;
import com.cts.inward.dao.BatchDetailsDaoImpl;
import com.cts.inward.dao.ChequeImageDaoImpl;
import com.cts.inward.dao.MicrMasterDaoImpl;
import com.cts.inward.dao.MicrRepairDao;
import com.cts.inward.dao.MicrRepairDaoImpl;
import com.cts.inward.model.ChequeImage;
import com.cts.inward.service.BatchDetailsService;
import com.cts.inward.service.BatchDetailsServiceImpl;

public class BatchDetailsController extends GenericForwardComposer<Component> {

    private static final long serialVersionUID = 1L;

    // BATCH
    private Long batchId;
    private List<Map<String, Object>> cheques = new ArrayList<>();
    private int currentChequeIndex = 0;
    private BatchDetailsService batchDetailsService;

    // CHEQUE IMAGE & CONTROLS
    private Image chequeImage;
    private Component chequePreview;
    private Button toggleImageButton;
    private Button zoomInButton;
    private Button zoomOutButton;
    private Button rotateButton;
    private Button resetViewButton;
    private Button btnFront;
    private Button btnBack;
    private String currentFrontImagePath;
    private String currentBackImagePath;
    private boolean showingFront = true;
    private double currentScale = 1.0;
    private int currentRotation = 0;
    private ChequeImageDaoImpl chequeImageDao;
    private final Map<String, ChequeImage> chequeImageCache = new HashMap<>();
    private final Map<String, AImage> aImageCache = new HashMap<>();
    private final Map<String, Map<String, Object>> micrDetailsCache = new HashMap<>();
    private final Map<String, Map<String, Object>> dataEntryDetailsCache = new HashMap<>();
    private final Map<String, Map<String, Object>> cbsValidationCache = new HashMap<>();
    private final Map<String, List<Map<String, String>>> makerReturnReasonsCache = new HashMap<>();

    // HEADER & METRICS
    private Label pageTitle;
    private Label batchLabel;
    private Label totalCountLabel;
    private Label completedCountLabel;
    private Label pendingCountLabel;
    private Label chequeCounter;

    // LEFT CHEQUE PREVIEW
    private Label imageChequeNumber;
    private Label imageChequeDate;
    private Label payeeImage;
    private Label amountImage;
    private Label micrImage;
    private Label chequeNumberLabel;
    private Label leftCbsStatus;

    // RIGHT VERIFICATION HEADER
    private Label rightChequeNumber;
    private Label chequePosition;

    // MICR SECTION
    private Vlayout micrUnchanged;
    private Label micrValue;
    private Hlayout micrCorrection;
    private Label oldMicrValue;
    private Label correctedMicrValue;
    private Label micrStatus;

    // DATA ENTRY - ACCOUNT NUMBER
    private Label accountStatus;
    private Hlayout accountCorrection;
    private Label oldAccountNumber;
    private Label correctedAccountNumber;
    private Hlayout accountUnchanged;
    private Label accountNumber;

    // DATA ENTRY - AMOUNT
    private Label amountStatus;
    private Hlayout amountCorrection;
    private Label oldAmount;
    private Label correctedAmount;
    private Hlayout amountUnchanged;
    private Label amount;

    // DATA ENTRY - CHEQUE DATE
    private Label dateStatus;
    private Hlayout dateCorrection;
    private Label oldChequeDate;
    private Label correctedChequeDate;
    private Hlayout dateUnchanged;
    private Label chequeDate;
    private Label dataEntrySummary;

    // DATA ENTRY - CHEQUE NUMBER (DB FIELD = cheque_number)
    private Label chequeNoStatus;
    private Hlayout chequeNoCorrection;
    private Label oldChequeNumber;
    private Label correctedChequeNumber;
    private Hlayout chequeNoUnchanged;
    private Label chequeNumberVal;

    // CBS VALIDATION
    private Label cbsTitle;
    private Label cbsActionStatus;
    private Hlayout cbsFailure;
    private Label cbsFailureText;

    // CHEQUE NAVIGATION
    private Button cheque1;
    private Button cheque2;
    private Button cheque3;
    private Button cheque4;
    private Button cheque5;
    private Button cheque6;
    private Button cheque7;
    private Button cheque8;
    private Button cheque9;
    private Button cheque10;
    private Button cheque11;
    private Button cheque12;
    private Button cheque13;
    private Button cheque14;
    private Button cheque15;
    private Button nextChequeArrow;
    private Button previousCheque;
    private Button nextCheque;
    private Button completeVerification;

    // INIT

    // CHECKER DECISION
    private Integer userId;

    private Button acceptButton;
    private Button returnButton;
    private Button rejectButton;

    private Hlayout selectedDecision;
    private Label selectedDecisionText;

    private Window acceptConfirmWindow;
    private Button acceptCancelButton;
    private Button acceptConfirmButton;

    private Window rejectWindow;
    private Vlayout rejectReasonsContainer;
    private Textbox rejectRemark;
    private Button rejectCancelButton;
    private Button rejectConfirmButton;

    private Window returnWindow;
    private Vlayout returnReasonsContainer;
    private Textbox returnRemark;
    private Button returnCancelButton;
    private Button returnConfirmButton;
    private boolean cbsPassed = false;
    private List<String> currentCbsFailedReasonCodes = new ArrayList<>();

    // BATCH COMPLETE MODALS
    private Window batchCompleteConfirmWindow;
    private Button batchConfirmYesBtn;
    private Button batchConfirmNoBtn;

    private Window batchCompleteSuccessWindow;
    private Label lblBatchSuccessTitle;
    private Label lblBatchSuccessSubtitle;
    private Label lblBatchSuccessMessage;
    private Button batchSuccessOkBtn;

    // MAKER RETURN PANEL (RETURN_BY_MAKER)
    private Vlayout makerReturnPanel;
    private Vlayout verificationFormContainer;
    private Vlayout makerReturnReasonsList;
    private Label lblMakerReturnRemarks;
    private Component boxMakerRemarks;
    private Component boxMicrMasterVerification;
    private Label badgeMicrMasterStatus;
    private Label lblNpciMicrValue;
    private Label lblNpciMicrCheck;
    private Label lblOcrMicrValue;
    private Label lblOcrMicrCheck;
    private Component micrMasterSummaryBox;
    private Label lblMicrMasterSummary;

    @Override
    public void doAfterCompose(Component component)
            throws Exception {

        super.doAfterCompose(component);

        // POPUP CONTROLS
        if (acceptConfirmWindow != null) {
            acceptCancelButton = (Button) acceptConfirmWindow.getFellow("acceptCancelButton");

            acceptConfirmButton = (Button) acceptConfirmWindow.getFellow("acceptConfirmButton");

            acceptCancelButton.addEventListener(Events.ON_CLICK, event -> handleAcceptCancelButton());

            acceptConfirmButton.addEventListener(Events.ON_CLICK, event -> handleAcceptConfirmButton());
        }

        if (rejectWindow != null) {
            rejectReasonsContainer =(Vlayout) rejectWindow.getFellowIfAny("rejectReasonsContainer");

            rejectRemark = (Textbox) rejectWindow.getFellow("rejectRemark");

            rejectCancelButton = (Button) rejectWindow.getFellow("rejectCancelButton");

            rejectConfirmButton = (Button) rejectWindow.getFellow("rejectConfirmButton");

            rejectCancelButton.addEventListener(Events.ON_CLICK, event -> handleRejectCancelButton());

            rejectConfirmButton.addEventListener(Events.ON_CLICK, event -> handleRejectConfirmButton());
        }

        if (returnWindow != null) {
        	returnReasonsContainer = (Vlayout) returnWindow.getFellowIfAny("returnReasonsContainer");

            returnRemark = (Textbox) returnWindow.getFellow("returnRemark");

            returnCancelButton = (Button) returnWindow.getFellow("returnCancelButton");

            returnConfirmButton = (Button) returnWindow.getFellow("returnConfirmButton");

            returnCancelButton.addEventListener(Events.ON_CLICK,event -> handleReturnCancelButton());

            returnConfirmButton.addEventListener(Events.ON_CLICK,event -> handleReturnConfirmButton());
        }

        if (batchCompleteConfirmWindow != null) {
            batchConfirmYesBtn = (Button) batchCompleteConfirmWindow.getFellowIfAny("batchConfirmYesBtn");
            batchConfirmNoBtn = (Button) batchCompleteConfirmWindow.getFellowIfAny("batchConfirmNoBtn");

            if (batchConfirmYesBtn != null) {
                batchConfirmYesBtn.addEventListener(Events.ON_CLICK, event -> {
                    batchCompleteConfirmWindow.setVisible(false);
                    onClick$completeVerification();
                });
            }
            if (batchConfirmNoBtn != null) {
                batchConfirmNoBtn.addEventListener(Events.ON_CLICK, event -> {
                    batchCompleteConfirmWindow.setVisible(false);
                });
            }
            batchCompleteConfirmWindow.setVisible(false);
        }

        if (batchCompleteSuccessWindow != null) {
            lblBatchSuccessTitle = (Label) batchCompleteSuccessWindow.getFellowIfAny("lblBatchSuccessTitle");
            lblBatchSuccessSubtitle = (Label) batchCompleteSuccessWindow.getFellowIfAny("lblBatchSuccessSubtitle");
            lblBatchSuccessMessage = (Label) batchCompleteSuccessWindow.getFellowIfAny("lblBatchSuccessMessage");
            batchSuccessOkBtn = (Button) batchCompleteSuccessWindow.getFellowIfAny("batchSuccessOkBtn");

            if (batchSuccessOkBtn != null) {
                batchSuccessOkBtn.addEventListener(Events.ON_CLICK, event -> {
                    batchCompleteSuccessWindow.setVisible(false);
                    Executions.sendRedirect("/zul/inward-checker/verification.zul");
                });
            }
            batchCompleteSuccessWindow.setVisible(false);
        }

        if (acceptConfirmWindow != null) {
            acceptConfirmWindow.setVisible(false);
        }
        if (rejectWindow != null) {
            rejectWindow.setVisible(false);
        }
        if (returnWindow != null) {
            returnWindow.setVisible(false);
        }

        Session session = Executions.getCurrent().getSession();
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null && user.getUserId() != null) {
            userId = user.getUserId().intValue();
        } else {
            Object sessionUserId = Executions.getCurrent().getAttribute("userId");
            if (sessionUserId instanceof Number) {
                userId = ((Number) sessionUserId).intValue();
            } else if (sessionUserId != null) {
                try {
                    userId = Integer.valueOf(String.valueOf(sessionUserId));
                } catch (NumberFormatException e) {
                    userId = null;
                }
            }
        }

        System.out.println("CHECKER USER ID = " + userId);

        // Existing DAO + Service
        batchDetailsService = BatchDetailsServiceImpl.of(
                BatchDetailsDaoImpl.of());

        chequeImageDao = ChequeImageDaoImpl.of();

        // Wire Image 2 toolbar events
        if (toggleImageButton != null) {
            toggleImageButton.addEventListener(Events.ON_CLICK, event -> {
                showingFront = !showingFront;
                toggleImageButton.setLabel(showingFront ? "View Back" : "View Front");
                if (showingFront) {
                    showFrontImage();
                } else {
                    showBackImage();
                }
                applyImageStyle();
            });
        }

        if (zoomInButton != null) {
            zoomInButton.addEventListener(Events.ON_CLICK, event -> {
                if (currentScale < 3.0) {
                    currentScale += 0.2;
                    applyImageStyle();
                }
            });
        }

        if (zoomOutButton != null) {
            zoomOutButton.addEventListener(Events.ON_CLICK, event -> {
                if (currentScale > 0.4) {
                    currentScale -= 0.2;
                    applyImageStyle();
                }
            });
        }

        if (rotateButton != null) {
            rotateButton.addEventListener(Events.ON_CLICK, event -> {
                currentRotation = (currentRotation + 90) % 360;
                applyImageStyle();
            });
        }

        if (resetViewButton != null) {
            resetViewButton.addEventListener(Events.ON_CLICK, event -> {
                currentScale = 1.0;
                currentRotation = 0;
                applyImageStyle();
            });
        }

        // Backwards compatibility if legacy btnFront/btnBack exist
        if (btnFront != null) {
            btnFront.addEventListener(Events.ON_CLICK, event -> showFrontImage());
        }
        if (btnBack != null) {
            btnBack.addEventListener(Events.ON_CLICK, event -> showBackImage());
        }

        // Get batch ID from URL
        String batchIdParameter = Executions.getCurrent()
                .getParameter("batchId");

        if (batchIdParameter != null
                && !batchIdParameter.trim().isEmpty()) {
            try {
                batchId = Long.valueOf(
                        batchIdParameter.trim());
            } catch (NumberFormatException e) {
                batchId = null;
            }
        } else {
            batchId = null;
        }

        System.out.println();
        System.out.println(
                "======================================");

        System.out.println(
                "BATCH DETAILS - Batch ID = "
                        + batchId);

        System.out.println(
                "======================================");

        if (batchId == null) {

            System.out.println(
                    "BATCH DETAILS - Batch ID NOT FOUND");

            return;
        }

        // Load cheques
        loadCheques();

        // Load first cheque - loadCurrentCheque() also performs CBS validation.
        if (!cheques.isEmpty()) {

            int firstUnverified = findFirstUnverifiedChequeIndex();
            currentChequeIndex = (firstUnverified != -1) ? firstUnverified : 0;

            loadCurrentCheque();

        } else {

            System.out.println(
                    "NO CHEQUES FOUND FOR BATCH ID = "
                            + batchId);

            updateCompleteVerificationButtonState();
        }
    }

    // LOAD CHEQUES
    private void loadCheques() {

        cheques = batchDetailsService
                .getChequesByBatchId(batchId);

        if (cheques == null) {

            cheques = new ArrayList<>();
        }

        System.out.println();

        System.out.println(
                "======================================");

        System.out.println(
                "BATCH ID = " + batchId);

        System.out.println(
                "TOTAL CHEQUES FOUND = "
                        + cheques.size());

        System.out.println(
                "======================================");
    }

    // LOAD CURRENT CHEQUE
    private void loadCurrentCheque() {

        if (cheques == null
                || cheques.isEmpty()) {

            return;
        }

        // Protect index
        if (currentChequeIndex < 0) {

            currentChequeIndex = 0;
        }

        if (currentChequeIndex >= cheques.size()) {

            currentChequeIndex = cheques.size() - 1;
        }

        // Get current cheque
        Map<String, Object> cheque = cheques.get(currentChequeIndex);
        currentCbsFailedReasonCodes.clear();

        // If current cheque is already verified, redirect to first unverified cheque if available
        if (isChequeVerified(cheque)) {
            int firstUnverified = findFirstUnverifiedChequeIndex();
            if (firstUnverified != -1 && firstUnverified != currentChequeIndex) {
                currentChequeIndex = firstUnverified;
                cheque = cheques.get(currentChequeIndex);
            }
        }

        // GET VALUES FROM inward_cheque
        String chequeNumber = getString(
                cheque,
                "chequeNumber");

        String accountNumber = getString(
                cheque,
                "accountNumber");

        String drawerName = getString(
                cheque,
                "drawerName");

        String micrCode = getString(
                cheque,
                "micrCode");

        String chequeDate = getString(
                cheque,
                "chequeDate");

        String amount = getString(
                cheque,
                "amount");

        // CONSOLE
        System.out.println();

        System.out.println(
                "========== CURRENT CHEQUE ==========");

        System.out.println(
                "Batch ID = " + batchId);

        System.out.println(
                "Cheque Number = "
                        + chequeNumber);

        System.out.println(
                "Account Number = "
                        + accountNumber);

        System.out.println(
                "Drawer Name = "
                        + drawerName);

        System.out.println(
                "Amount = "
                        + amount);

        System.out.println(
                "MICR = "
                        + micrCode);

        System.out.println(
                "Cheque Date = "
                        + chequeDate);

        System.out.println(
                "Cheque Position = "
                        + (currentChequeIndex + 1)
                        + " of "
                        + cheques.size());

        System.out.println(
                "====================================");

        // UPDATE HEADER
        if (pageTitle != null) {

            pageTitle.setValue(
                    "Verify Inward Batch - BATCH"
                            + batchId);
        }

        if (rightChequeNumber != null) {

            rightChequeNumber.setValue(
                    nullToEmpty(chequeNumber));
        }

        if (chequePosition != null) {

            chequePosition.setValue(" | Bank:");
        }

        // UPDATE LEFT CHEQUE INFORMATION
        if (imageChequeNumber != null) {

            imageChequeNumber.setValue(
                    nullToEmpty(chequeNumber));
        }

        if (chequeNumberLabel != null) {

            chequeNumberLabel.setValue(
                    "Cheque Number : "
                            + nullToEmpty(
                                    chequeNumber));
        }

        if (payeeImage != null) {

            payeeImage.setValue(
                    nullToEmpty(drawerName));
        }

        if (amountImage != null) {

            amountImage.setValue(
                    "₹ "
                            + nullToEmpty(amount));
        }

        if (imageChequeDate != null) {

            imageChequeDate.setValue(
                    "DATE: "
                            + formatDate(
                                    chequeDate));
        }

        if (micrImage != null) {

            micrImage.setValue(
                    nullToEmpty(micrCode));
        }

        // CHECK IF CHEQUE WAS RETURNED BY MAKER
        String status = getString(cheque, "status");
        boolean isReturnByMaker = "RETURN_BY_MAKER".equalsIgnoreCase(status);

        if (isReturnByMaker) {
            // Show Maker Return Panel, hide verification form
            if (makerReturnPanel != null) {
                makerReturnPanel.setVisible(true);
            }
            if (verificationFormContainer != null) {
                verificationFormContainer.setVisible(false);
            }

            // Populate Maker Return info
            String makerRemarks = getString(cheque, "makerRemarks");
            String ocrMicrCode = getString(cheque, "ocrMicrCode");

            if (makerReturnReasonsList != null) {
                makerReturnReasonsList.getChildren().clear();
                List<Map<String, String>> returnReasons = makerReturnReasonsCache.get(chequeNumber);
                if (returnReasons == null) {
                    returnReasons = batchDetailsService.getMakerReturnReasons(chequeNumber);
                    if (returnReasons != null) {
                        makerReturnReasonsCache.put(chequeNumber, returnReasons);
                    }
                }
                if (returnReasons != null && !returnReasons.isEmpty()) {
                    for (Map<String, String> r : returnReasons) {
                        Hlayout row = new Hlayout();
                        row.setSpacing("10px");
                        row.setValign("middle");
                        row.setWidth("100%");
                        row.setStyle("background:#FFF5F5; border:1.5px solid #FECDCA; border-radius:6px; padding:8px 12px;");

                        String code = r.get("returnReasonCode");
                        String desc = r.get("description");
                        if (desc == null || desc.trim().isEmpty()) {
                            desc = "Return requested by maker";
                        }

                        Label badge = new Label(code != null ? code : "—");
                        badge.setStyle("font-size:12px; font-weight:700; background:#FEE4E2; color:#B42318; border:1px solid #FECDCA; border-radius:4px; padding:2px 8px; flex-shrink:0;");

                        Label lblDesc = new Label(desc);
                        lblDesc.setStyle("font-size:13px; font-weight:600; color:#1E293B; word-break:break-word;");
                        lblDesc.setHflex("1");

                        row.appendChild(badge);
                        row.appendChild(lblDesc);
                        makerReturnReasonsList.appendChild(row);
                    }
                } else {
                    String singleCode = getString(cheque, "returnReasonCode");
                    String singleDesc = getString(cheque, "returnReasonDescription");
                    Hlayout row = new Hlayout();
                    row.setSpacing("10px");
                    row.setValign("middle");
                    row.setWidth("100%");
                    row.setStyle("background:#FFF5F5; border:1.5px solid #FECDCA; border-radius:6px; padding:8px 12px;");

                    Label badge = new Label(singleCode != null && !singleCode.trim().isEmpty() ? singleCode : "RETURN");
                    badge.setStyle("font-size:12px; font-weight:700; background:#FEE4E2; color:#B42318; border:1px solid #FECDCA; border-radius:4px; padding:2px 8px; flex-shrink:0;");

                    Label lblDesc = new Label(singleDesc != null && !singleDesc.trim().isEmpty() ? singleDesc : "Returned by Maker");
                    lblDesc.setStyle("font-size:13px; font-weight:600; color:#1E293B;");
                    lblDesc.setHflex("1");

                    row.appendChild(badge);
                    row.appendChild(lblDesc);
                    makerReturnReasonsList.appendChild(row);
                }
            }

            if (lblMakerReturnRemarks != null) {
                if (makerRemarks != null && !makerRemarks.trim().isEmpty()) {
                    lblMakerReturnRemarks.setValue(makerRemarks);
                    if (boxMakerRemarks != null) {
                        boxMakerRemarks.setVisible(true);
                    }
                } else {
                    lblMakerReturnRemarks.setValue("No remarks provided by Maker");
                    if (boxMakerRemarks != null) {
                        boxMakerRemarks.setVisible(false);
                    }
                }
            }

            // Verify MICR with MicrMaster (Image 2) - ONLY for MR-MICR-001
            boolean isMrMicr001 = isMakerReturnReasonMrMicr001(cheque);
            if (isMrMicr001) {
                String checkMicr = (ocrMicrCode != null && !ocrMicrCode.trim().isEmpty()) ? ocrMicrCode.trim() : (micrCode != null ? micrCode.trim() : null);
                boolean ocrExists = false;
                if (checkMicr != null && !checkMicr.isEmpty()) {
                    try {
                        ocrExists = MicrMasterDaoImpl.of().exists(checkMicr);
                    } catch (Exception e) {
                        System.err.println("Error verifying MICR in micr_master: " + e.getMessage());
                    }
                }

                if (boxMicrMasterVerification != null) {
                    boxMicrMasterVerification.setVisible(true);
                }

                if (lblOcrMicrValue != null) {
                    lblOcrMicrValue.setValue(checkMicr != null && !checkMicr.isEmpty() ? checkMicr : "Not Present");
                }
                if (lblOcrMicrCheck != null) {
                    if (checkMicr != null && !checkMicr.isEmpty()) {
                        if (ocrExists) {
                            lblOcrMicrCheck.setValue("✓ Found in Master");
                            lblOcrMicrCheck.setStyle("color:#027A48; background:#ECFDF3; border:1px solid #A6F4C5; font-weight:700; padding:4px 10px; border-radius:6px;");
                        } else {
                            lblOcrMicrCheck.setValue("✗ NOT Found in Master");
                            lblOcrMicrCheck.setStyle("color:#B42318; background:#FEF3F2; border:1px solid #FECDCA; font-weight:700; padding:4px 10px; border-radius:6px;");
                        }
                    } else {
                        lblOcrMicrCheck.setValue("— N/A");
                        lblOcrMicrCheck.setStyle("color:#64748B; background:#F1F5F9; border:1px solid #E2E8F0; font-weight:700; padding:4px 10px; border-radius:6px;");
                    }
                }

                if (lblMicrMasterSummary != null) {
                    if (ocrExists) {
                        lblMicrMasterSummary.setValue("System Verification: MICR code (" + checkMicr + ") was found in Master directory.");
                    } else {
                        lblMicrMasterSummary.setValue("System Verification: MICR code is absent from the MICR Master Directory. Maker return is verified by system.");
                    }
                }
            } else {
                if (boxMicrMasterVerification != null) {
                    boxMicrMasterVerification.setVisible(false);
                }
            }

            if (leftCbsStatus != null) {
                leftCbsStatus.setValue("● RETURNED BY MAKER");
                leftCbsStatus.setSclass("cbs-fail-badge");
            }

            boolean currentVerified = isChequeVerified(cheque);

            if (currentVerified) {
                // Cheque has already been verified (or entire batch complete) - Lock all action buttons so checker cannot re-action it
                if (acceptButton != null) {
                    acceptButton.setDisabled(true);
                    acceptButton.setSclass("decision-button accept-button accept-button-dull");
                    acceptButton.setStyle("cursor: not-allowed !important; opacity: 0.55 !important; pointer-events: auto !important;");
                    acceptButton.setTooltiptext("This cheque has already been verified and cannot be re-opened.");
                }
                if (returnButton != null) {
                    returnButton.setDisabled(true);
                    returnButton.setSclass("decision-button return-button return-button-dull");
                    returnButton.setStyle("cursor: not-allowed !important; opacity: 0.55 !important; pointer-events: auto !important;");
                    returnButton.setTooltiptext("This cheque has already been verified and cannot be re-opened.");
                }
                if (rejectButton != null) {
                    rejectButton.setDisabled(true);
                    rejectButton.setSclass("decision-button reject-button reject-button-dull");
                    rejectButton.setStyle("cursor: not-allowed !important; opacity: 0.55 !important; pointer-events: auto !important;");
                    rejectButton.setTooltiptext("This cheque has already been verified and cannot be re-opened.");
                }
            } else {
                // Action buttons: Accept is disabled
                if (acceptButton != null) {
                    acceptButton.setDisabled(true);
                    acceptButton.setSclass("decision-button accept-button accept-button-dull");
                    acceptButton.setStyle("cursor: not-allowed !important; opacity: 0.55 !important; pointer-events: auto !important;");
                    acceptButton.setTooltiptext("Cannot accept cheque returned by Maker.");
                }

                // Send Back button: Enabled
                if (returnButton != null) {
                    returnButton.setDisabled(false);
                    returnButton.setSclass("decision-button return-button");
                    returnButton.setStyle(null);
                    returnButton.setTooltiptext(null);
                }
                if (rejectButton != null) {
                    rejectButton.setDisabled(false);
                    rejectButton.setSclass("decision-button reject-button");
                    rejectButton.setStyle(null);
                    rejectButton.setTooltiptext(null);
                }
            }

        } else {
            // Normal cheque: show verification form, hide maker return panel
            if (makerReturnPanel != null) {
                makerReturnPanel.setVisible(false);
            }
            if (boxMicrMasterVerification != null) {
                boxMicrMasterVerification.setVisible(false);
            }
            if (verificationFormContainer != null) {
                verificationFormContainer.setVisible(true);
            }

            boolean currentVerified = isChequeVerified(cheque);

            if (currentVerified) {
                // Already actioned/verified cheque - lock all decision buttons
                if (acceptButton != null) {
                    acceptButton.setDisabled(true);
                    acceptButton.setSclass("decision-button accept-button accept-button-dull");
                    acceptButton.setStyle("cursor: not-allowed !important; opacity: 0.55 !important; pointer-events: auto !important;");
                    acceptButton.setTooltiptext("This cheque has already been verified and cannot be re-opened.");
                }
                if (returnButton != null) {
                    returnButton.setDisabled(true);
                    returnButton.setSclass("decision-button return-button return-button-dull");
                    returnButton.setStyle("cursor: not-allowed !important; opacity: 0.55 !important; pointer-events: auto !important;");
                    returnButton.setTooltiptext("This cheque has already been verified and cannot be re-opened.");
                }
                if (rejectButton != null) {
                    rejectButton.setDisabled(true);
                    rejectButton.setSclass("decision-button reject-button reject-button-dull");
                    rejectButton.setStyle("cursor: not-allowed !important; opacity: 0.55 !important; pointer-events: auto !important;");
                    rejectButton.setTooltiptext("This cheque has already been verified and cannot be re-opened.");
                }
            } else {
                if (acceptButton != null) {
                    acceptButton.setTooltiptext(null);
                }
                if (returnButton != null) {
                    returnButton.setDisabled(false);
                    returnButton.setSclass("decision-button return-button");
                    returnButton.setStyle(null);
                    returnButton.setTooltiptext(null);
                }
                if (rejectButton != null) {
                    rejectButton.setDisabled(false);
                    rejectButton.setSclass("decision-button reject-button");
                    rejectButton.setStyle(null);
                    rejectButton.setTooltiptext(null);
                }
            }

            // MICR
            loadMicrDetails(chequeNumber);

            // DATA ENTRY
            loadDataEntryDetails(chequeNumber);

            // CBS VALIDATION
            loadCbsValidation(chequeNumber);
        }

        // NAVIGATION
        updateChequeNavigation();

        loadChequeImages(chequeNumber);

        // BATCH HEADER COUNTS (Image 4)
        updateBatchHeaderCounts();

        // DECISION BADGE FOR CURRENT CHEQUE
        if (selectedDecision != null && selectedDecisionText != null) {
            if (!isChequeVerified(cheque)) {
                selectedDecision.setVisible(false);
                selectedDecisionText.setValue("");
            } else {
                String action = getString(cheque, "checker_action");
                if (action == null) {
                    action = getString(cheque, "checkerAction");
                }
                if (action == null) {
                    String st = getString(cheque, "cheque_status");
                    if (st == null) st = getString(cheque, "status");
                    if ("ACCEPT".equalsIgnoreCase(st)) {
                        action = "Accepted";
                    } else if ("REJECT".equalsIgnoreCase(st)) {
                        action = "Returned";
                    } else if ("RETURN_TO_MAKER".equalsIgnoreCase(st)) {
                        action = "Sent Back";
                    }
                }
                if (action != null && !action.trim().isEmpty()) {
                    selectedDecision.setVisible(true);
                    if ("Accepted".equalsIgnoreCase(action)) {
                        selectedDecisionText.setValue("✓ Selected Decision: Accepted");
                        selectedDecision.setSclass("selected-decision accepted");
                    } else if ("Returned".equalsIgnoreCase(action) || "Rejected".equalsIgnoreCase(action)) {
                        selectedDecisionText.setValue("⚠ Selected Decision: Returned");
                        selectedDecision.setSclass("selected-decision rejected");
                    } else {
                        selectedDecisionText.setValue("↶ Selected Decision: Sent Back");
                        selectedDecision.setSclass("selected-decision returned");
                    }
                } else {
                    selectedDecision.setVisible(false);
                    selectedDecisionText.setValue("");
                }
            }
        }
    }

    // MICR DETAILS
    private void loadMicrDetails(
            String chequeNumber) {

        if (chequeNumber == null
                || chequeNumber.trim().isEmpty()) {

            System.out.println(
                    "MICR DETAILS - CHEQUE NUMBER IS EMPTY");

            return;
        }

        Map<String, Object> micrDetails = micrDetailsCache.get(chequeNumber);
        if (micrDetails == null) {
            micrDetails = batchDetailsService.getMicrDetails(chequeNumber);
            if (micrDetails != null) {
                micrDetailsCache.put(chequeNumber, micrDetails);
            }
        }

        System.out.println();

        System.out.println(
                "========== MICR DETAILS ==========");

        System.out.println(
                "Cheque Number = "
                        + chequeNumber);

        if (micrDetails == null
                || micrDetails.isEmpty()) {

            System.out.println(
                    "MICR DETAILS NOT FOUND");

            if (micrCorrection != null) {

                micrCorrection
                        .setVisible(false);
            }

            if (micrUnchanged != null) {

                micrUnchanged
                        .setVisible(true);
            }

            if (micrStatus != null) {

                micrStatus.setValue(
                        "• No MICR Correction");

                micrStatus.setSclass(
                        "unchanged-status");
            }

            return;
        }

        String currentMicr = getString(
                micrDetails,
                "currentMicr");

        String oldMicr = getString(
                micrDetails,
                "oldMicr");

        String correctedMicr = getString(
                micrDetails,
                "correctedMicr");

        System.out.println(
                "Current MICR = "
                        + currentMicr);

        System.out.println(
                "Old MICR = "
                        + oldMicr);

        System.out.println(
                "Corrected MICR = "
                        + correctedMicr);

        boolean hasCorrection = oldMicr != null
                && !oldMicr.trim().isEmpty()
                && correctedMicr != null
                && !correctedMicr.trim().isEmpty()
                && !oldMicr.equals(correctedMicr);

        // MICR CORRECTED
        if (hasCorrection) {

            System.out.println(
                    "MICR CORRECTION = YES");

            if (micrUnchanged != null) {

                micrUnchanged
                        .setVisible(false);
            }

            if (micrCorrection != null) {

                micrCorrection
                        .setVisible(true);
            }

            if (micrStatus != null) {

                micrStatus.setValue(
                        "• MICR Correction");

                micrStatus.setSclass(
                        "green-status");
            }

            if (oldMicrValue != null) {

                oldMicrValue.setValue(
                        oldMicr);
            }

            if (correctedMicrValue != null) {

                correctedMicrValue.setValue(
                        correctedMicr);
            }

        }

        // MICR NOT CORRECTED
        else {

            System.out.println(
                    "MICR CORRECTION = NO");

            if (micrCorrection != null) {

                micrCorrection
                        .setVisible(false);
            }

            if (micrUnchanged != null) {

                micrUnchanged
                        .setVisible(true);
            }

            if (micrStatus != null) {

                micrStatus.setValue(
                        "• No MICR Correction");

                micrStatus.setSclass(
                        "unchanged-status");
            }

            if (micrValue != null) {

                micrValue.setValue(
                        nullToEmpty(
                                currentMicr));
            }
        }

        System.out.println(
                "==================================");
    }

    // DATA ENTRY DETAILS
    private void loadDataEntryDetails(
            String chequeNumber) {

        if (chequeNumber == null
                || chequeNumber.trim().isEmpty()) {

            System.out.println(
                    "DATA ENTRY - CHEQUE NUMBER IS EMPTY");

            return;
        }

        Map<String, Object> details = dataEntryDetailsCache.get(chequeNumber);
        if (details == null) {
            details = batchDetailsService.getDataEntryDetails(chequeNumber);
            if (details != null) {
                dataEntryDetailsCache.put(chequeNumber, details);
            }
        }

        System.out.println();

        System.out.println(
                "========== DATA ENTRY DETAILS ==========");

        System.out.println(
                "Cheque Number = "
                        + chequeNumber);

        if (details == null
                || details.isEmpty()) {

            System.out.println(
                    "DATA ENTRY DETAILS NOT FOUND");

            resetDataEntryUI();

            return;
        }

        // ACCOUNT NUMBER
        String oldAccount = getString(
                details,
                "oldAccountNumber");

        String newAccount = getString(
                details,
                "newAccountNumber");

        boolean accountCorrected = newAccount != null
                && !newAccount.trim().isEmpty();

        if (accountCorrected) {

            if (accountStatus != null) {

                accountStatus.setValue(
                        "Corrected");

                accountStatus.setSclass(
                        "green-status");
            }

            if (accountCorrection != null) {

                accountCorrection
                        .setVisible(true);
            }

            if (accountUnchanged != null) {

                accountUnchanged
                        .setVisible(false);
            }

            if (oldAccountNumber != null) {

                oldAccountNumber.setValue(
                        nullToEmpty(
                                oldAccount));
            }

            if (correctedAccountNumber != null) {

                correctedAccountNumber
                        .setValue(
                                newAccount);
            }

        } else {

            if (accountStatus != null) {

                accountStatus.setValue(
                        "Unchanged");

                accountStatus.setSclass(
                        "unchanged-status");
            }

            if (accountCorrection != null) {

                accountCorrection
                        .setVisible(false);
            }

            if (accountUnchanged != null) {

                accountUnchanged
                        .setVisible(true);
            }

            if (accountNumber != null) {

                accountNumber.setValue(
                        nullToEmpty(
                                oldAccount));
            }
        }

        // AMOUNT
        String oldAmountValue = getString(
                details,
                "oldAmount");

        String newAmountValue = getString(
                details,
                "newAmount");

        boolean amountCorrected = newAmountValue != null
                && !newAmountValue.trim().isEmpty();

        if (amountCorrected) {

            if (amountStatus != null) {

                amountStatus.setValue(
                        "Corrected");

                amountStatus.setSclass(
                        "green-status");
            }

            if (amountCorrection != null) {

                amountCorrection
                        .setVisible(true);
            }

            if (amountUnchanged != null) {

                amountUnchanged
                        .setVisible(false);
            }

            if (oldAmount != null) {

                oldAmount.setValue(
                        formatAmount(
                                oldAmountValue));
            }

            if (correctedAmount != null) {

                correctedAmount.setValue(
                        formatAmount(
                                newAmountValue));
            }

        } else {

            if (amountStatus != null) {

                amountStatus.setValue(
                        "Unchanged");

                amountStatus.setSclass(
                        "unchanged-status");
            }

            if (amountCorrection != null) {

                amountCorrection
                        .setVisible(false);
            }

            if (amountUnchanged != null) {

                amountUnchanged
                        .setVisible(true);
            }

            if (amount != null) {

                amount.setValue(
                        formatAmount(
                                oldAmountValue));
            }
        }

        // CHEQUE DATE
        String oldDate = getString(
                details,
                "oldChequeDate");

        String newDate = getString(
                details,
                "newChequeDate");

        boolean dateCorrected = newDate != null
                && !newDate.trim().isEmpty();

        if (dateCorrected) {

            if (dateStatus != null) {

                dateStatus.setValue(
                        "Corrected");

                dateStatus.setSclass(
                        "green-status");
            }

            if (dateCorrection != null) {

                dateCorrection
                        .setVisible(true);
            }

            if (dateUnchanged != null) {

                dateUnchanged
                        .setVisible(false);
            }

            if (oldChequeDate != null) {

                oldChequeDate.setValue(
                        formatDate(oldDate));
            }

            if (correctedChequeDate != null) {

                correctedChequeDate.setValue(
                        formatDate(newDate));
            }

        } else {

            if (dateStatus != null) {

                dateStatus.setValue(
                        "Unchanged");

                dateStatus.setSclass(
                        "unchanged-status");
            }

            if (dateCorrection != null) {

                dateCorrection
                        .setVisible(false);
            }

            if (dateUnchanged != null) {

                dateUnchanged
                        .setVisible(true);
            }

            if (chequeDate != null) {

                chequeDate.setValue(
                        formatDate(oldDate));
            }
        }

        // CHEQUE NUMBER - DB FIELD = cheque_number
        String oldChqNo = getString(
                details,
                "oldChequeNumber");
        if (oldChqNo == null || oldChqNo.trim().isEmpty()) {
            oldChqNo = getString(details, "chequeNumber");
        }

        String newChqNo = getString(
                details,
                "newChequeNumber");

        boolean chequeNoCorrected = newChqNo != null
                && !newChqNo.trim().isEmpty()
                && !newChqNo.trim().equals(oldChqNo != null ? oldChqNo.trim() : "");

        if (chequeNoCorrected) {

            if (chequeNoStatus != null) {

                chequeNoStatus.setValue(
                        "Corrected");

                chequeNoStatus.setSclass(
                        "green-status");
            }

            if (chequeNoCorrection != null) {

                chequeNoCorrection
                        .setVisible(true);
            }

            if (chequeNoUnchanged != null) {

                chequeNoUnchanged
                        .setVisible(false);
            }

            if (oldChequeNumber != null) {

                oldChequeNumber.setValue(
                        nullToEmpty(
                                oldChqNo));
            }

            if (correctedChequeNumber != null) {

                correctedChequeNumber.setValue(
                        newChqNo.trim());
            }

        } else {

            if (chequeNoStatus != null) {

                chequeNoStatus.setValue(
                        "Unchanged");

                chequeNoStatus.setSclass(
                        "unchanged-status");
            }

            if (chequeNoCorrection != null) {

                chequeNoCorrection
                        .setVisible(false);
            }

            if (chequeNoUnchanged != null) {

                chequeNoUnchanged
                        .setVisible(true);
            }

            if (chequeNumberVal != null) {

                chequeNumberVal.setValue(
                        nullToEmpty(
                                oldChqNo));
            }
        }

        // DATA ENTRY SUMMARY BADGE (e.g. • 2 Fields Corrected)
        int correctedFieldsCount = (accountCorrected ? 1 : 0)
                + (amountCorrected ? 1 : 0)
                + (dateCorrected ? 1 : 0)
                + (chequeNoCorrected ? 1 : 0);

        if (dataEntrySummary != null) {
            if (correctedFieldsCount > 0) {
                dataEntrySummary.setValue("• " + correctedFieldsCount
                        + (correctedFieldsCount == 1 ? " Field Corrected" : " Fields Corrected"));
                dataEntrySummary.setSclass("green-status");
            } else {
                dataEntrySummary.setValue("• No Corrections");
                dataEntrySummary.setSclass("unchanged-status");
            }
        }

        System.out.println(
                "Account Corrected = "
                        + accountCorrected);

        System.out.println(
                "Amount Corrected = "
                        + amountCorrected);

        System.out.println(
                "Date Corrected = "
                        + dateCorrected);

        System.out.println(
                "Cheque Number Corrected = "
                        + chequeNoCorrected);

        System.out.println(
                "==========================================");
    }

    // CBS VALIDATION
    private void loadCbsValidation(
            String chequeNumber) {

        cbsPassed = false;

        if (acceptButton != null) {
            acceptButton.setDisabled(true);
            acceptButton.setSclass("decision-button accept-button accept-button-dull");
            acceptButton.setStyle("cursor: not-allowed !important; opacity: 0.55 !important; pointer-events: auto !important;");
        }

        System.out.println();

        System.out.println(
                "========== CBS VALIDATION ==========");

        System.out.println(
                "Cheque Number = "
                        + chequeNumber);

        // Invalid cheque number
        if (chequeNumber == null
                || chequeNumber.trim().isEmpty()) {

            showCbsFailure(
                    "CBS validation failed: cheque details not found.");

            return;
        }

        // Get CBS data
        Map<String, Object> cbsDetails = cbsValidationCache.get(chequeNumber);
        if (cbsDetails == null) {
            cbsDetails = batchDetailsService.getCbsValidation(chequeNumber);
            if (cbsDetails != null) {
                cbsValidationCache.put(chequeNumber, cbsDetails);
            }
        }

        if (cbsDetails == null
                || cbsDetails.isEmpty()) {

            showCbsFailure(
                    "CBS validation failed: cheque details not found.");

            return;
        }

        // VALUES FROM DATABASE
        BigDecimal chequeAmount = getBigDecimal(
                cbsDetails,
                "chequeAmount");

        BigDecimal availableBalance = getBigDecimal(
                cbsDetails,
                "availableBalance");

        Date chequeSqlDate = getSqlDate(
                cbsDetails,
                "chequeDate");

        String accountStatus = getString(
                cbsDetails,
                "accountStatus");

        // 1. AMOUNT / FUNDS VALIDATION - available balance MUST be greater than cheque amount.
        boolean amountPassed = chequeAmount != null
                && availableBalance != null
                && availableBalance.compareTo(
                        chequeAmount) > 0;

        // 2. CHEQUE DATE VALIDATION - Cheque date should NOT be before 3 months.
        boolean datePassed = false;

        if (chequeSqlDate != null) {

            LocalDate chequeDate = chequeSqlDate.toLocalDate();

            LocalDate minimumValidDate = LocalDate.now()
                    .minusMonths(3);

            datePassed = !chequeDate.isBefore(
                    minimumValidDate);
        }

        // 3. ACCOUNT STATUS VALIDATION - Account must be ACTIVE.
        boolean accountPassed = "ACTIVE".equalsIgnoreCase(
                accountStatus);

        // 4. DUPLICATE CHEQUE VALIDATION
        boolean isDuplicate = Boolean.TRUE.equals(
                cbsDetails.get("isDuplicate"));

        boolean duplicatePassed = !isDuplicate;

        // PRINT VALUES
        System.out.println(
                "CBS Cheque Amount = "
                        + chequeAmount);

        System.out.println(
                "CBS Available Balance = "
                        + availableBalance);

        System.out.println(
                "CBS Cheque Date = "
                        + chequeSqlDate);

        System.out.println(
                "CBS Account Status = "
                        + accountStatus);

        System.out.println(
                "CBS Duplicate Cheque = "
                        + isDuplicate);

        System.out.println(
                "Amount / Funds = "
                        + amountPassed);

        System.out.println(
                "Cheque Date = "
                        + datePassed);

        System.out.println(
                "Account Status = "
                        + accountPassed);

        System.out.println(
                "Duplicate Cheque = "
                        + duplicatePassed);

        // FINAL RESULT
        boolean allPassed = amountPassed
                && datePassed
                && accountPassed
                && duplicatePassed;

        cbsPassed = allPassed;

        if (allPassed) {

            showCbsPassed();

        } else {

            StringBuilder failure = new StringBuilder();
            currentCbsFailedReasonCodes.clear();

            if (!amountPassed) {

                failure.append(
                        "Amount / Funds validation failed.");
                currentCbsFailedReasonCodes.add("RJ001");
            }

            if (!datePassed) {

                appendFailureSeparator(
                        failure);

                failure.append(
                        "Cheque date is older than 3 months.");
                currentCbsFailedReasonCodes.add("RJ003");
            }

            if (!accountPassed) {

                appendFailureSeparator(
                        failure);

                failure.append(
                        "Account status is not ACTIVE.");
                currentCbsFailedReasonCodes.add("RJ002");
            }

            if (!duplicatePassed) {

                appendFailureSeparator(
                        failure);

                failure.append(
                        "Duplicate cheque detected in system.");
                currentCbsFailedReasonCodes.add("RJ004");
            }

            showCbsFailure(
                    failure.toString());
        }

        System.out.println(
                "CBS FINAL RESULT = "
                        + allPassed);

        System.out.println(
                "====================================");
    }

    // CBS RESULT UI

    // CBS PASSED
    private void showCbsPassed() {
        cbsPassed = true;
        currentCbsFailedReasonCodes.clear();

        if (cbsTitle != null) {

            cbsTitle.setValue(
                    "CBS Validation: PASSED");

            cbsTitle.setSclass(
                    "cbs-title");
        }

        if (cbsActionStatus != null) {

            cbsActionStatus.setValue("");

            cbsActionStatus.setVisible(false);
        }

        if (cbsFailure != null) {

            cbsFailure.setVisible(false);
        }

        if (leftCbsStatus != null) {

            leftCbsStatus.setValue(
                    "● CBS: PASSED");

            leftCbsStatus.setSclass(
                    "cbs-pass-badge");
        }

        if (acceptButton != null) {
            if (cheques != null && currentChequeIndex >= 0 && currentChequeIndex < cheques.size()
                    && isChequeVerified(cheques.get(currentChequeIndex))) {
                acceptButton.setDisabled(true);
                acceptButton.setSclass("decision-button accept-button accept-button-dull");
                acceptButton.setStyle("cursor: not-allowed !important; opacity: 0.55 !important; pointer-events: auto !important;");
                acceptButton.setTooltiptext("This cheque has already been verified and cannot be re-opened.");
            } else {
                acceptButton.setDisabled(false);
                acceptButton.setSclass(
                        "decision-button accept-button");
                acceptButton.setStyle(null);
                acceptButton.setTooltiptext(null);
            }
        }
    }

    // CBS FAILED
    private void showCbsFailure(
            String message) {

        if (cbsTitle != null) {

            cbsTitle.setValue(
                    "CBS Validation: FAILED");

            cbsTitle.setSclass(
                    "cbs-title cbs-title-failed");
        }

        if (cbsActionStatus != null) {

            cbsActionStatus.setValue("");

            cbsActionStatus.setVisible(false);
        }

        if (cbsFailure != null) {

            cbsFailure.setVisible(true);
        }

        if (cbsFailureText != null) {

            cbsFailureText.setValue(
                    message);
        }

        if (leftCbsStatus != null) {

            leftCbsStatus.setValue(
                    "● CBS: FAILED");

            leftCbsStatus.setSclass(
                    "cbs-fail-badge");
        }

        cbsPassed = false;

        if (acceptButton != null) {
            acceptButton.setDisabled(true);
            acceptButton.setSclass(
                    "decision-button accept-button accept-button-dull");
            acceptButton.setStyle("cursor: not-allowed !important; opacity: 0.55 !important; pointer-events: auto !important;");
        }
    }

    // RESET DATA ENTRY UI
    private void resetDataEntryUI() {

        // ACCOUNT
        if (accountStatus != null) {

            accountStatus.setValue(
                    "Unchanged");

            accountStatus.setSclass(
                    "unchanged-status");
        }

        if (accountCorrection != null) {

            accountCorrection
                    .setVisible(false);
        }

        if (accountUnchanged != null) {

            accountUnchanged
                    .setVisible(true);
        }

        if (accountNumber != null) {

            accountNumber.setValue("");
        }

        if (oldAccountNumber != null) {

            oldAccountNumber.setValue("");
        }

        if (correctedAccountNumber != null) {

            correctedAccountNumber.setValue("");
        }

        // AMOUNT
        if (amountStatus != null) {

            amountStatus.setValue(
                    "Unchanged");

            amountStatus.setSclass(
                    "unchanged-status");
        }

        if (amountCorrection != null) {

            amountCorrection
                    .setVisible(false);
        }

        if (amountUnchanged != null) {

            amountUnchanged
                    .setVisible(true);
        }

        if (amount != null) {

            amount.setValue("");
        }

        if (oldAmount != null) {

            oldAmount.setValue("");
        }

        if (correctedAmount != null) {

            correctedAmount.setValue("");
        }

        // DATE
        if (dateStatus != null) {

            dateStatus.setValue(
                    "Unchanged");

            dateStatus.setSclass(
                    "unchanged-status");
        }

        if (dateCorrection != null) {

            dateCorrection
                    .setVisible(false);
        }

        if (dateUnchanged != null) {

            dateUnchanged
                    .setVisible(true);
        }

        if (chequeDate != null) {

            chequeDate.setValue("");
        }

        if (oldChequeDate != null) {

            oldChequeDate.setValue("");
        }

        if (correctedChequeDate != null) {

            correctedChequeDate.setValue("");
        }

        // CHEQUE NUMBER
        if (chequeNoStatus != null) {

            chequeNoStatus.setValue(
                    "Unchanged");

            chequeNoStatus.setSclass(
                    "unchanged-status");
        }

        if (chequeNoCorrection != null) {

            chequeNoCorrection
                    .setVisible(false);
        }

        if (chequeNoUnchanged != null) {

            chequeNoUnchanged
                    .setVisible(true);
        }

        if (chequeNumberVal != null) {

            chequeNumberVal.setValue("");
        }

        if (oldChequeNumber != null) {

            oldChequeNumber.setValue("");
        }

        if (correctedChequeNumber != null) {

            correctedChequeNumber.setValue("");
        }

        if (dataEntrySummary != null) {

            dataEntrySummary.setValue("• No Corrections");

            dataEntrySummary.setSclass("unchanged-status");
        }
    }

    // CHEQUE NAVIGATION

    // CHECKER DECISION - ACCEPT
    public void onClick$acceptButton() {
        Map<String, Object> currentCheque = (cheques != null && currentChequeIndex >= 0 && currentChequeIndex < cheques.size())
                ? cheques.get(currentChequeIndex) : null;
        if (currentCheque != null && isChequeVerified(currentCheque)) {
            Messagebox.show("This cheque has already been verified and cannot be modified.",
                    "Cheque Already Verified", Messagebox.OK, Messagebox.INFORMATION);
            return;
        }
        if (currentCheque != null && "RETURN_BY_MAKER".equalsIgnoreCase(getString(currentCheque, "status"))) {
            Messagebox.show("This cheque was returned by the Maker and cannot be accepted. Please select Return or Send Back to Maker.",
                    "Action Restricted", Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }
        if (!cbsPassed) {
            Messagebox.show("CBS validation has failed. This cheque cannot be accepted.",
                    "CBS Validation", Messagebox.OK, Messagebox.ERROR);
            return;
        }
        if (acceptConfirmWindow != null) {
            acceptConfirmWindow.doModal();
        }
    }

    private void handleAcceptCancelButton() {
        if (acceptConfirmWindow != null) {
            acceptConfirmWindow.setVisible(false);
        }
    }

    private void handleAcceptConfirmButton() {
        Map<String, Object> currentCheque = (cheques != null && currentChequeIndex >= 0 && currentChequeIndex < cheques.size())
                ? cheques.get(currentChequeIndex) : null;
        if (currentCheque != null && "RETURN_BY_MAKER".equalsIgnoreCase(getString(currentCheque, "status"))) {
            if (acceptConfirmWindow != null) {
                acceptConfirmWindow.setVisible(false);
            }
            Messagebox.show("This cheque was returned by the Maker and cannot be accepted. Please select Return or Send Back to Maker.",
                    "Action Restricted", Messagebox.OK, Messagebox.EXCLAMATION);
            return;
        }
        if (!cbsPassed) {
            if (acceptConfirmWindow != null) {
                acceptConfirmWindow.setVisible(false);
            }
            Messagebox.show("CBS validation has failed. This cheque cannot be accepted.",
                    "CBS Validation", Messagebox.OK, Messagebox.ERROR);
            return;
        }
        saveDecision("ACCEPT", null, null, "Accepted", null, acceptConfirmWindow);
    }

    // CHECKER DECISION - REJECT (RETURN)
    public void onClick$rejectButton() {
        Map<String, Object> currentCheque = (cheques != null && currentChequeIndex >= 0 && currentChequeIndex < cheques.size())
                ? cheques.get(currentChequeIndex) : null;
        if (currentCheque != null && isChequeVerified(currentCheque)) {
            Messagebox.show("This cheque has already been verified and cannot be modified.",
                    "Cheque Already Verified", Messagebox.OK, Messagebox.INFORMATION);
            return;
        }
        if (rejectReasonsContainer != null) {
            rejectReasonsContainer.getChildren().clear();
            List<Map<String, String>> reasons =
                    batchDetailsService.getCheckerRejectionReasons();
            if (reasons != null) {
                boolean isMrMicr001 = isMakerReturnReasonMrMicr001(currentCheque);

                for (Map<String, String> r : reasons) {
                    String code = r.get("rejection_reason_code");
                    if (code == null) {
                        code = r.get("code");
                    }
                    String desc = r.get("description");

                    // If Maker returned with MR-MICR-001, display ONLY RJ006 (MICR Not Available) and auto-select it
                    if (isMrMicr001) {
                        boolean isMicrRejection = (code != null && "RJ006".equalsIgnoreCase(code.trim()))
                                || (desc != null && desc.toLowerCase().contains("micr"));
                        if (!isMicrRejection) {
                            continue; // Skip all other reasons
                        }
                    } else if (!cbsPassed && currentCbsFailedReasonCodes != null && !currentCbsFailedReasonCodes.isEmpty()) {
                        // If CBS validation failed, display ONLY the reason(s) that caused CBS failure
                        if (!currentCbsFailedReasonCodes.contains(code)) {
                            continue; // Skip unrelated reasons
                        }
                    }

                    Checkbox cb = new Checkbox();
                    cb.setLabel((code != null ? code : "") + " - " + (desc != null ? desc : ""));
                    cb.setAttribute("reasonCode", code);
                    cb.setSclass("modal-reason-checkbox");

                    // Pre-check if MR-MICR-001 or CBS failure reason
                    if (isMrMicr001) {
                        cb.setChecked(true);
                    } else if (!cbsPassed && currentCbsFailedReasonCodes != null && currentCbsFailedReasonCodes.contains(code)) {
                        cb.setChecked(true);
                    }

                    rejectReasonsContainer.appendChild(cb);
                }
            }
        }
        if (rejectRemark != null) {
            rejectRemark.setValue("");
        }
        if (rejectWindow != null) {
            rejectWindow.doModal();
        }
    }

    private void handleRejectCancelButton() {
        if (rejectWindow != null) {
            rejectWindow.setVisible(false);
        }
    }

    private void handleRejectConfirmButton() {

        List<String> selectedCodes = new ArrayList<>();
        if (rejectReasonsContainer != null) {
            for (Component comp : rejectReasonsContainer.getChildren()) {
                if (comp instanceof Checkbox) {
                    Checkbox cb = (Checkbox) comp;
                    if (cb.isChecked()) {
                        String code = (String) cb.getAttribute("reasonCode");
                        if (code != null && !code.trim().isEmpty()) {
                            selectedCodes.add(code.trim());
                        }
                    }
                }
            }
        }

        if (selectedCodes.isEmpty()) {
            Messagebox.show(
                    "Please select at least one return reason.",
                    "Validation",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION
            );
            return;
        }

        String remarks = rejectRemark == null
                ? null
                : rejectRemark.getValue();

        System.out.println(
                "RETURN REASON CODES = " + selectedCodes
        );

        System.out.println(
                "RETURN REMARKS = " + remarks);

        saveDecision(
                "REJECT",
                selectedCodes,
                null,
                "Returned",
                remarks,
                rejectWindow);
    }

    // CHECKER DECISION - RETURN TO MAKER (SEND BACK)
    public void onClick$returnButton() {
        if (cheques != null && currentChequeIndex >= 0 && currentChequeIndex < cheques.size()) {
            Map<String, Object> currentCheque = cheques.get(currentChequeIndex);
            if (isChequeVerified(currentCheque)) {
                Messagebox.show("This cheque has already been verified and cannot be modified.",
                        "Cheque Already Verified", Messagebox.OK, Messagebox.INFORMATION);
                return;
            }
        }
        if (returnReasonsContainer != null) {
            returnReasonsContainer.getChildren().clear();
            List<Map<String, String>> reasons =
                    batchDetailsService.getCheckerReturnReasons();
            if (reasons != null) {
                for (Map<String, String> r : reasons) {
                    Checkbox cb = new Checkbox();
                    String code = r.get("return_reason_code");
                    if (code == null) {
                        code = r.get("code");
                    }
                    String desc = r.get("description");
                    cb.setLabel((code != null ? code : "") + " - " + (desc != null ? desc : ""));
                    cb.setAttribute("reasonCode", code);
                    cb.setSclass("modal-reason-checkbox");
                    returnReasonsContainer.appendChild(cb);
                }
            }
        }
        if (returnRemark != null) {
            returnRemark.setValue("");
        }
        if (returnWindow != null) {
            returnWindow.doModal();
        }
    }

    private void handleReturnCancelButton() {
        if (returnWindow != null) {
            returnWindow.setVisible(false);
        }
    }

    private void handleReturnConfirmButton() {

        List<String> selectedCodes = new ArrayList<>();
        if (returnReasonsContainer != null) {
            for (Component comp : returnReasonsContainer.getChildren()) {
                if (comp instanceof Checkbox) {
                    Checkbox cb = (Checkbox) comp;
                    if (cb.isChecked()) {
                        String code = (String) cb.getAttribute("reasonCode");
                        if (code != null && !code.trim().isEmpty()) {
                            selectedCodes.add(code.trim());
                        }
                    }
                }
            }
        }

        if (selectedCodes.isEmpty()) {
            Messagebox.show(
                    "Please select at least one send back reason.",
                    "Validation",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION
            );
            return;
        }

        String remarks = returnRemark == null
                ? null
                : returnRemark.getValue();

        System.out.println(
                "SEND BACK REASON CODES = " + selectedCodes
        );

        System.out.println(
                "SEND BACK REMARKS = " + remarks);

        saveDecision(
                "RETURN_TO_MAKER",
                null,
                selectedCodes,
                "Sent Back",
                remarks,
                returnWindow);
    }

    // SAVE CHECKER DECISION
    private void saveDecision(String status,
            List<String> rejectionReasonCodes,
            List<String> returnReasonCodes,
            String checkerAction,
            String remarks,
            Window popupWindow) {

        String chequeNumber = getCurrentChequeNumber();

        if (chequeNumber == null || chequeNumber.trim().isEmpty()) {
            Messagebox.show("No cheque is currently selected.",
                    "Error", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        if (userId == null) {
            Messagebox.show("Logged-in checker ID was not found in the session.",
                    "Authentication Error", Messagebox.OK, Messagebox.ERROR);
            return;
        }

        try {
            batchDetailsService.saveCheckerDecision(
                    chequeNumber,
                    status,
                    rejectionReasonCodes,
                    returnReasonCodes,
                    userId,
                    checkerAction,
                    remarks);

            if (cheques != null && currentChequeIndex >= 0 && currentChequeIndex < cheques.size()) {
                Map<String, Object> curr = cheques.get(currentChequeIndex);
                curr.put("status", status);
                curr.put("cheque_status", status);
                curr.put("checker_action", checkerAction);
                curr.put("checkerAction", checkerAction);
                curr.put("checker_id", userId);
                curr.put("checkerId", userId);
                curr.put("session_verified", Boolean.TRUE);
            }

            if (popupWindow != null) {
                popupWindow.setVisible(false);
            }

            if (selectedDecisionText != null) {
                if ("Accepted".equals(checkerAction)) {
                    selectedDecisionText.setValue("✓ Selected Decision: Accepted");
                } else if ("Returned".equals(checkerAction) || "Rejected".equals(checkerAction)) {
                    selectedDecisionText.setValue("⚠ Selected Decision: Returned");
                } else {
                    selectedDecisionText.setValue("↶ Selected Decision: Sent Back");
                }
            }

            if (selectedDecision != null) {
                String decisionClass = "Accepted".equals(checkerAction)
                        ? "accepted"
                        : ("Returned".equals(checkerAction) || "Rejected".equals(checkerAction))
                                ? "rejected"
                                : "returned";
                selectedDecision.setSclass("selected-decision " + decisionClass);
                selectedDecision.setVisible(true);
            }

            moveToNextAfterDecision();

        } catch (Exception e) {
            e.printStackTrace();
            Messagebox.show("Failed to save checker decision. Please check the server log.",
                    "Database Error", Messagebox.OK, Messagebox.ERROR);
        }
    }

    private String getCurrentChequeNumber() {
        if (cheques == null || cheques.isEmpty()
                || currentChequeIndex < 0
                || currentChequeIndex >= cheques.size()) {
            return null;
        }
        return getString(cheques.get(currentChequeIndex), "chequeNumber");
    }

    private void moveToNextAfterDecision() {
        if (isAllChequesVerified()) {
            loadCurrentCheque();
            updateBatchHeaderCounts();
            updateCompleteVerificationButtonState();
            updateChequeNavigation();
            if (batchCompleteConfirmWindow != null) {
                batchCompleteConfirmWindow.doModal();
            } else {
                Messagebox.show(
                        "All cheques in this batch have been verified. Do you want to complete batch verification now?",
                        "Batch Verification Complete",
                        Messagebox.YES | Messagebox.NO,
                        Messagebox.QUESTION,
                        event -> {
                            if (Messagebox.ON_YES.equals(event.getName())) {
                                onClick$completeVerification();
                            }
                        });
            }
        } else {
            int nextUnverified = findNextUnverifiedChequeIndex(currentChequeIndex);
            if (nextUnverified != -1 && nextUnverified != currentChequeIndex) {
                currentChequeIndex = nextUnverified;
                loadCurrentCheque();
            } else {
                updateBatchHeaderCounts();
                updateCompleteVerificationButtonState();
                updateChequeNavigation();
            }
        }
    }

    // CHEQUE IMAGE LOADING & TRANSFORMS (Image 2)
    private void loadChequeImages(String chequeNumber) {

        currentFrontImagePath = null;
        currentBackImagePath = null;

        if (chequeNumber != null) {
            chequeNumber = chequeNumber.trim();
        }

        try {
            ChequeImage image = chequeImageCache.get(chequeNumber);
            if (image == null) {
                image = chequeImageDao.findByChequeNumber(chequeNumber);
                if (image != null) {
                    chequeImageCache.put(chequeNumber, image);
                }
            }
            if (image != null) {
                currentFrontImagePath = image.getFrontPath();
                currentBackImagePath = image.getBackPath();
            }
            if (currentFrontImagePath == null || currentFrontImagePath.trim().isEmpty()) {
                MicrRepairDao repairDao = new MicrRepairDaoImpl();
                currentFrontImagePath = repairDao.getFrontImagePath(chequeNumber);
                currentBackImagePath = repairDao.getBackImagePath(chequeNumber);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("LOAD CHEQUE IMAGES: cheque=" + chequeNumber +
                ", front=" + currentFrontImagePath +
                ", back=" + currentBackImagePath);

        showingFront = true;
        currentScale = 1.0;
        currentRotation = 0;

        if (toggleImageButton != null) {
            toggleImageButton.setLabel("View Back");
        }

        // Default to front image
        showFrontImage();
        applyImageStyle();

        // Reset legacy button styles if present
        if (btnFront != null) {
            btnFront.setSclass("image-button image-button-active");
        }
        if (btnBack != null) {
            btnBack.setSclass("image-button");
        }
    }

    private File resolveImageFile(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        String normalized = path.replace("\\", "/").trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        // 1. Direct file / absolute path
        File directFile = new File(path);
        if (directFile.isAbsolute() && directFile.isFile()) {
            return directFile;
        }

        // 2. Deployed webApp realPath
        try {
            if (Executions.getCurrent() != null
                    && Executions.getCurrent().getDesktop() != null) {
                WebApp webApp =
                        Executions.getCurrent().getDesktop().getWebApp();
                String realPath = webApp.getRealPath("/" + normalized);
                if (realPath != null) {
                    File realFile = new File(realPath);
                    if (realFile.isFile()) {
                        return realFile;
                    }
                }
            }
        } catch (Exception ignored) {}

        // 3. Local workspace development path: src/main/webapp/ + subPath
        String subPath = normalized.startsWith("src/main/webapp/")
                ? normalized.substring("src/main/webapp/".length())
                : normalized;
        File devFile = new File("src/main/webapp", subPath);
        if (devFile.isFile()) {
            return devFile;
        }

        // 4. Relative path as-is
        if (directFile.isFile()) {
            return directFile;
        }

        return null;
    }

    private void renderChequeImage(String imagePath) {
        if (chequeImage == null) return;
        if (imagePath == null || imagePath.trim().isEmpty()) {
            chequeImage.setContent((AImage) null);
            chequeImage.setSrc("");
            chequeImage.setVisible(false);
            if (chequePreview != null) {
                chequePreview.setVisible(true);
            }
            return;
        }

        try {
            File file = resolveImageFile(imagePath);
            if (file != null) {
                AImage aimg = aImageCache.get(file.getAbsolutePath());
                if (aimg == null) {
                    aimg = new AImage(file);
                    aImageCache.put(file.getAbsolutePath(), aimg);
                }
                chequeImage.setContent(aimg);
                chequeImage.setVisible(true);
                if (chequePreview != null) {
                    chequePreview.setVisible(false);
                }
            } else {
                String clean = imagePath.replace("\\", "/").trim();
                if (clean.startsWith("/")) clean = clean.substring(1);
                if (clean.startsWith("src/main/webapp/")) {
                    clean = clean.substring("src/main/webapp/".length());
                }
                String webSrc = "/" + clean;
                chequeImage.setContent((AImage) null);
                chequeImage.setSrc(webSrc);
                chequeImage.setVisible(true);
                if (chequePreview != null) {
                    chequePreview.setVisible(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            chequeImage.setContent((AImage) null);
            chequeImage.setVisible(false);
            if (chequePreview != null) {
                chequePreview.setVisible(true);
            }
        }
    }

    private void showFrontImage() {

        if (chequeImage == null)
            return;

        if (btnFront != null) {
            btnFront.setSclass("image-button image-button-active");
        }
        if (btnBack != null) {
            btnBack.setSclass("image-button");
        }
        if (toggleImageButton != null) {
            toggleImageButton.setLabel("View Back");
        }

        renderChequeImage(currentFrontImagePath);
        applyImageStyle();
    }

    private void showBackImage() {

        if (chequeImage == null)
            return;

        if (btnFront != null) {
            btnFront.setSclass("image-button");
        }
        if (btnBack != null) {
            btnBack.setSclass("image-button image-button-active");
        }
        if (toggleImageButton != null) {
            toggleImageButton.setLabel("View Front");
        }

        renderChequeImage(currentBackImagePath);
        applyImageStyle();
    }

    private void applyImageStyle() {
        if (chequeImage != null) {
            chequeImage.setStyle(String.format(
                    Locale.US,
                    "object-fit:contain; max-width:100%%; max-height:100%%; display:block; margin:auto; transform: scale(%.2f) rotate(%ddeg); transform-origin: center; transition: transform 0.2s;",
                    currentScale,
                    currentRotation));
        }
    }

    // BATCH HEADER METRICS (Image 4)


    private boolean isMakerReturnReasonMrMicr001(Map<String, Object> currentCheque) {
        if (currentCheque == null) {
            return false;
        }
        String code = getString(currentCheque, "returnReasonCode");
        if (code == null) {
            code = getString(currentCheque, "return_reason_code");
        }
        if (code != null && ("MR-MICR-001".equalsIgnoreCase(code.trim()) || "MR-MICR-01".equalsIgnoreCase(code.trim()))) {
            return true;
        }
        String chequeNumber = getString(currentCheque, "chequeNumber");
        if (chequeNumber != null) {
            List<Map<String, String>> reasons = makerReturnReasonsCache.get(chequeNumber);
            if (reasons == null) {
                reasons = batchDetailsService.getMakerReturnReasons(chequeNumber);
                if (reasons != null) {
                    makerReturnReasonsCache.put(chequeNumber, reasons);
                }
            }
            if (reasons != null) {
                for (Map<String, String> r : reasons) {
                    String rCode = r.get("returnReasonCode");
                    if (rCode == null) {
                        rCode = r.get("code");
                    }
                    if (rCode != null && ("MR-MICR-001".equalsIgnoreCase(rCode.trim()) || "MR-MICR-01".equalsIgnoreCase(rCode.trim()))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isChequeVerified(Map<String, Object> chq) {
        if (chq == null) {
            return false;
        }
        if (Boolean.TRUE.equals(chq.get("session_verified"))) {
            return true;
        }
        String st = getString(chq, "cheque_status");
        if (st == null) {
            st = getString(chq, "status");
        }
        // Cheques pending verification or waiting for maker are NOT verified
        if (st != null && (
                "SENT_TO_CHECKER".equalsIgnoreCase(st)
                || "RETURN_TO_MAKER".equalsIgnoreCase(st)
                || "RETURN_BY_MAKER".equalsIgnoreCase(st)
                || "DATA_ENTRY".equalsIgnoreCase(st)
                || "DATA_ENTRY_COMPLETED".equalsIgnoreCase(st)
                || "PARSED".equalsIgnoreCase(st)
                || "RECEIVED".equalsIgnoreCase(st))) {
            return false;
        }
        if ("ACCEPT".equalsIgnoreCase(st) || "REJECT".equalsIgnoreCase(st)) {
            return true;
        }
        String action = getString(chq, "checker_action");
        if (action == null) {
            action = getString(chq, "checkerAction");
        }
        if (action != null && !action.trim().isEmpty()
                && !"Sent Back".equalsIgnoreCase(action.trim())
                && !"RETURN_TO_MAKER".equalsIgnoreCase(action.trim())) {
            return true;
        }
        return false;
    }

    private boolean isAllChequesVerified() {
        if (cheques == null || cheques.isEmpty()) {
            return false;
        }
        for (Map<String, Object> chq : cheques) {
            if (!isChequeVerified(chq)) {
                return false;
            }
        }
        return true;
    }

    private int findNextUnverifiedChequeIndex(int fromIndex) {
        if (cheques == null || cheques.isEmpty()) {
            return -1;
        }
        int total = cheques.size();
        for (int i = fromIndex + 1; i < total; i++) {
            if (!isChequeVerified(cheques.get(i))) {
                return i;
            }
        }
        for (int i = 0; i <= fromIndex && i < total; i++) {
            if (!isChequeVerified(cheques.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private int findFirstUnverifiedChequeIndex() {
        if (cheques == null || cheques.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < cheques.size(); i++) {
            if (!isChequeVerified(cheques.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private int findPreviousUnverifiedChequeIndex(int fromIndex) {
        if (cheques == null || cheques.isEmpty()) {
            return -1;
        }
        for (int i = fromIndex - 1; i >= 0; i--) {
            if (!isChequeVerified(cheques.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasUnverifiedChequeBefore(int index) {
        return findPreviousUnverifiedChequeIndex(index) != -1;
    }

    private boolean hasUnverifiedChequeAfter(int index) {
        if (cheques == null || cheques.isEmpty()) {
            return false;
        }
        for (int i = index + 1; i < cheques.size(); i++) {
            if (!isChequeVerified(cheques.get(i))) {
                return true;
            }
        }
        return false;
    }

    // BATCH HEADER METRICS (Image 4)
    private void updateBatchHeaderCounts() {
        if (batchLabel != null) {
            batchLabel.setValue("Batch No : " + (batchId != null ? batchId : "—"));
        }

        int total = (cheques != null) ? cheques.size() : 0;
        int completed = 0;

        if (cheques != null) {
            for (Map<String, Object> chq : cheques) {
                if (isChequeVerified(chq)) {
                    completed++;
                }
            }
        }

        int pending = Math.max(0, total - completed);

        if (totalCountLabel != null) {
            totalCountLabel.setValue("Total: " + total);
        }
        if (completedCountLabel != null) {
            completedCountLabel.setValue("Completed: " + completed);
        }
        if (pendingCountLabel != null) {
            pendingCountLabel.setValue("Pending: " + pending);
        }
        if (chequeCounter != null) {
            int current = (cheques != null && !cheques.isEmpty()) ? (currentChequeIndex + 1) : 0;
            chequeCounter.setValue("Cheque " + current + " of " + total);
        }
    }

    private void updateChequeNavigation() {

        Button[] chequeButtons = {
                cheque1, cheque2, cheque3, cheque4, cheque5,
                cheque6, cheque7, cheque8, cheque9, cheque10,
                cheque11, cheque12, cheque13, cheque14, cheque15
        };

        for (Button button : chequeButtons) {
            if (button != null) {
                button.setVisible(false);
                button.setSclass("cheque-button");
            }
        }

        int visibleCount = Math.min(
                cheques != null ? cheques.size() : 0,
                chequeButtons.length);

        for (int i = 0; i < visibleCount; i++) {
            if (chequeButtons[i] != null) {
                chequeButtons[i].setVisible(true);
                chequeButtons[i].setLabel(String.valueOf(i + 1));
                boolean verified = cheques != null && isChequeVerified(cheques.get(i));
                if (i == currentChequeIndex) {
                    chequeButtons[i].setSclass("cheque-button cheque-button-selected");
                    chequeButtons[i].setDisabled(false);
                    chequeButtons[i].setStyle(null);
                } else if (verified) {
                    chequeButtons[i].setSclass("cheque-button cheque-button-verified");
                    chequeButtons[i].setDisabled(true);
                    chequeButtons[i].setStyle("cursor: not-allowed !important; opacity: 0.65 !important;");
                    chequeButtons[i].setTooltiptext("Cheque " + (i + 1) + " has already been verified and cannot be re-opened.");
                } else {
                    chequeButtons[i].setSclass("cheque-button");
                    chequeButtons[i].setDisabled(false);
                    chequeButtons[i].setStyle(null);
                    chequeButtons[i].setTooltiptext(null);
                }
            }
        }

        // PREVIOUS CHEQUE: Only enable if an UNVERIFIED cheque exists before current index
        boolean canGoPrev = hasUnverifiedChequeBefore(currentChequeIndex);
        if (previousCheque != null) {
            previousCheque.setDisabled(!canGoPrev);
            if (!canGoPrev) {
                previousCheque.setStyle("cursor: not-allowed !important; opacity: 0.5 !important;");
            } else {
                previousCheque.setStyle(null);
            }
        }

        // NEXT CHEQUE: Only enable if an UNVERIFIED cheque exists after current index (or elsewhere)
        boolean hasUnverifiedAhead = hasUnverifiedChequeAfter(currentChequeIndex);
        boolean hasAnyUnverified = findFirstUnverifiedChequeIndex() != -1;
        boolean canGoNext = hasUnverifiedAhead || (hasAnyUnverified && cheques != null && currentChequeIndex < cheques.size() && !isChequeVerified(cheques.get(currentChequeIndex)));
        if (cheques != null && currentChequeIndex >= 0 && currentChequeIndex < cheques.size()
                && isChequeVerified(cheques.get(currentChequeIndex)) && hasAnyUnverified) {
            canGoNext = true;
        }

        if (nextCheque != null) {
            nextCheque.setDisabled(!canGoNext);
            if (!canGoNext) {
                nextCheque.setStyle("cursor: not-allowed !important; opacity: 0.5 !important;");
            } else {
                nextCheque.setStyle(null);
            }
        }

        if (nextChequeArrow != null) {
            nextChequeArrow.setDisabled(!canGoNext);
            if (!canGoNext) {
                nextChequeArrow.setStyle("cursor: not-allowed !important; opacity: 0.5 !important;");
            } else {
                nextChequeArrow.setStyle(null);
            }
        }

        updateCompleteVerificationButtonState();
    }

    // NEXT CHEQUE
    public void onClick$nextCheque() {

        if (cheques == null || cheques.isEmpty()) {
            return;
        }

        int nextUnverified = findNextUnverifiedChequeIndex(currentChequeIndex);
        if (nextUnverified != -1 && nextUnverified != currentChequeIndex) {
            currentChequeIndex = nextUnverified;
            loadCurrentCheque();
        } else {
            if (nextCheque != null) {
                nextCheque.setDisabled(true);
                nextCheque.setStyle("cursor: not-allowed !important; opacity: 0.5 !important;");
            }
        }
    }

    public void onClick$nextChequeArrow() {
        onClick$nextCheque();
    }

    // PREVIOUS CHEQUE
    public void onClick$previousCheque() {

        int prevUnverified = findPreviousUnverifiedChequeIndex(currentChequeIndex);
        if (prevUnverified != -1) {
            currentChequeIndex = prevUnverified;
            loadCurrentCheque();
        } else {
            if (previousCheque != null) {
                previousCheque.setDisabled(true);
                previousCheque.setStyle("cursor: not-allowed !important; opacity: 0.5 !important;");
            }
        }
    }

    // CHEQUE 1
    public void onClick$cheque1() {
        selectCheque(0);
    }

    // CHEQUE 2
    public void onClick$cheque2() {
        selectCheque(1);
    }

    // CHEQUE 3
    public void onClick$cheque3() {
        selectCheque(2);
    }

    // CHEQUE 4
    public void onClick$cheque4() {
        selectCheque(3);
    }

    // CHEQUE 5
    public void onClick$cheque5() {
        selectCheque(4);
    }

    // CHEQUE 6
    public void onClick$cheque6() {
        selectCheque(5);
    }

    // CHEQUE 7
    public void onClick$cheque7() {
        selectCheque(6);
    }

    // CHEQUE 8
    public void onClick$cheque8() {
        selectCheque(7);
    }

    // CHEQUE 9
    public void onClick$cheque9() {
        selectCheque(8);
    }

    // CHEQUE 10
    public void onClick$cheque10() {
        selectCheque(9);
    }

    // CHEQUE 11
    public void onClick$cheque11() {
        selectCheque(10);
    }

    // CHEQUE 12
    public void onClick$cheque12() {
        selectCheque(11);
    }

    // CHEQUE 13
    public void onClick$cheque13() {
        selectCheque(12);
    }

    // CHEQUE 14
    public void onClick$cheque14() {
        selectCheque(13);
    }

    // CHEQUE 15
    public void onClick$cheque15() {
        selectCheque(14);
    }

    // SELECT CHEQUE
    private void selectCheque(int index) {

        if (index < 0
                || index >= cheques.size()) {

            return;
        }

        if (isChequeVerified(cheques.get(index))) {
            Messagebox.show(
                    "Cheque " + (index + 1) + " has already been verified and cannot be re-opened.",
                    "Cheque Already Verified",
                    Messagebox.OK,
                    Messagebox.INFORMATION);
            return;
        }

        currentChequeIndex = index;

        loadCurrentCheque();
    }

    // BACK TO LIST
    public void onClick$backToList() {

        Executions.sendRedirect(
                "/zul/inward-checker/verification.zul");
    }

    // COMPLETE VERIFICATION - BUTTON STATE & CLICK
    private void updateCompleteVerificationButtonState() {

        if (completeVerification == null) {
            return;
        }

        if (cheques == null || cheques.isEmpty()) {
            completeVerification.setDisabled(true);
            completeVerification.setSclass("complete-button complete-button-disabled");
            return;
        }

        boolean allVerified = isAllChequesVerified();
        completeVerification.setDisabled(!allVerified);

        if (allVerified) {
            completeVerification.setSclass("complete-button complete-button-enabled");
            completeVerification.setTooltiptext("All cheques verified. Click to complete batch verification.");
        } else {
            completeVerification.setSclass("complete-button complete-button-disabled");
            int pending = 0;
            for (Map<String, Object> c : cheques) {
                if (!isChequeVerified(c)) {
                    pending++;
                }
            }
            completeVerification.setTooltiptext(
                    pending + " cheque(s) pending verification. Please verify all cheques to complete batch verification.");
        }
    }

    public void onClick$completeVerification() {

        if (completeVerification == null) {
            return;
        }

        if (cheques == null || cheques.isEmpty()) {
            completeVerification.setDisabled(true);
            return;
        }

        // Safety check: All cheques must be verified!
        if (!isAllChequesVerified()) {
            updateCompleteVerificationButtonState();
            int unverified = findFirstUnverifiedChequeIndex();
            if (unverified != -1) {
                currentChequeIndex = unverified;
                loadCurrentCheque();
            }
            Messagebox.show(
                    "Cannot complete verification. All cheques must be verified (Accepted, Returned, or Sent Back) before completing the batch.",
                    "Verification Incomplete",
                    Messagebox.OK,
                    Messagebox.EXCLAMATION);
            return;
        }

        if (batchId == null) {
            Messagebox.show(
                    "Batch ID is missing.",
                    "Error",
                    Messagebox.OK,
                    Messagebox.ERROR);
            return;
        }

        try {
            boolean returnedToMaker = batchDetailsService.completeVerification(batchId, userId);

            if (batchCompleteSuccessWindow != null) {
                if (returnedToMaker) {
                    if (lblBatchSuccessTitle != null) {
                        lblBatchSuccessTitle.setValue("Returned to Maker");
                    }
                    if (lblBatchSuccessSubtitle != null) {
                        lblBatchSuccessSubtitle.setValue("Batch contains returned cheques");
                    }
                    if (lblBatchSuccessMessage != null) {
                        lblBatchSuccessMessage.setValue("Batch contains returned cheque(s). Batch has been returned to Maker for reprocessing.");
                        lblBatchSuccessMessage.setStyle("font-family: 'Plus Jakarta Sans', sans-serif; font-size: 14px; font-weight: 500; color: #B45309; line-height: 1.6; display: block;");
                    }
                } else {
                    if (lblBatchSuccessTitle != null) {
                        lblBatchSuccessTitle.setValue("Verification Completed");
                    }
                    if (lblBatchSuccessSubtitle != null) {
                        lblBatchSuccessSubtitle.setValue("Batch clearing processed successfully");
                    }
                    if (lblBatchSuccessMessage != null) {
                        lblBatchSuccessMessage.setValue("Verification completed successfully.");
                        lblBatchSuccessMessage.setStyle("font-family: 'Plus Jakarta Sans', sans-serif; font-size: 14px; font-weight: 500; color: #166534; line-height: 1.6; display: block;");
                    }
                }
                batchCompleteSuccessWindow.doModal();
            } else {
                if (returnedToMaker) {
                    Messagebox.show(
                            "Batch contains returned cheque(s). Batch has been returned to Maker for reprocessing.",
                            "Verification Completed - Returned to Maker",
                            Messagebox.OK,
                            Messagebox.INFORMATION,
                            event -> Executions.sendRedirect("/zul/inward-checker/verification.zul"));
                } else {
                    Messagebox.show(
                            "Verification completed successfully.",
                            "Verification Completed",
                            Messagebox.OK,
                            Messagebox.INFORMATION,
                            event -> Executions.sendRedirect("/zul/inward-checker/verification.zul"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Messagebox.show(
                    "Failed to complete verification: " + e.getMessage(),
                    "Error",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    // GET STRING
    private String getString(
            Map<String, Object> map,
            String key) {

        if (map == null) {
            return null;
        }

        Object value = map.get(key);

        if (value == null) {
            return null;
        }

        return String.valueOf(value);
    }

    // GET BIG DECIMAL
    private BigDecimal getBigDecimal(
            Map<String, Object> map,
            String key) {

        if (map == null) {
            return null;
        }

        Object value = map.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal) {

            return (BigDecimal) value;
        }

        try {

            return new BigDecimal(
                    String.valueOf(value));

        } catch (NumberFormatException e) {

            return null;
        }
    }

    // GET SQL DATE
    private Date getSqlDate(
            Map<String, Object> map,
            String key) {

        if (map == null) {
            return null;
        }

        Object value = map.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof Date) {

            return (Date) value;
        }

        if (value instanceof java.util.Date) {

            return new Date(
                    ((java.util.Date) value)
                            .getTime());
        }

        try {

            return Date.valueOf(
                    String.valueOf(value));

        } catch (IllegalArgumentException e) {

            return null;
        }
    }

    // APPEND FAILURE SEPARATOR
    private void appendFailureSeparator(
            StringBuilder builder) {

        if (builder.length() > 0) {

            builder.append(" ");
        }
    }

    // NULL TO EMPTY
    private String nullToEmpty(
            String value) {

        return value == null
                ? ""
                : value;
    }

    // FORMAT AMOUNT
    private String formatAmount(String value) {

        if (value == null || value.trim().isEmpty()) {
            return "";
        }

        return "₹ " + value;
    }

    // FORMAT DATE — converts any date string to dd/MM/yyyy - Handles: yyyy-MM-dd, yyyy-MM-dd HH:mm:ss, dd/MM/yyyy
    private String formatDate(String value) {

        if (value == null || value.trim().isEmpty()) {
            return "";
        }

        try {
            // Strip time part if present
            String datePart = value.trim().split(" ")[0];

            LocalDate date;

            if (datePart.contains("-")) {
                // yyyy-MM-dd
                date = LocalDate.parse(datePart);
            } else if (datePart.contains("/")) {
                // already dd/MM/yyyy
                return datePart;
            } else {
				return value;
			}

            return date.format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        } catch (Exception e) {
            return value;
        }
    }
}
