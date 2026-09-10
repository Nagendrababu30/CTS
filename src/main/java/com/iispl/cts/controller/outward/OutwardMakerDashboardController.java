package com.iispl.cts.controller.outward;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Messagebox;

import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardValidationResult;
import com.iispl.cts.model.outward.UserSession;
import com.iispl.cts.service.outward.OutwardMakerDashboardService;

public class OutwardMakerDashboardController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    // =========================================================
    // ZUL COMPONENTS
    // =========================================================

    @Wire
    private Listbox batchListbox;

    @Wire
    private Label pendingDataEntryCount;

    @Wire
    private Label micrRepairCount;

    @Wire
    private Label readyToSubmitCount;

    // =========================================================
    // SERVICE
    // =========================================================

    private OutwardMakerDashboardService service;

    // =========================================================
    // CURRENT MAKER USER
    // =========================================================

    private String currentUserId;

    // =========================================================
    // AFTER COMPOSE
    // =========================================================

    @Override
    public void doAfterCompose(Component comp)
            throws Exception {

        super.doAfterCompose(comp);

     // =====================================================
     // GET LOGGED-IN USER FROM SESSION
     // =====================================================

     Session sessionUser =
             Executions.getCurrent()
                     .getSession();

     if (sessionUser == null) {

         System.out.println(
                 "No logged-in user session found."
         );

         Executions.sendRedirect("/zul/login.zul");

         return;
     }


     // =====================================================
     // GET LOGGED-IN USER ID FROM SESSION
     // =====================================================
     //
     // Existing session design:
     // session attribute = "userId"
     //

     Object sessionUserId =
             sessionUser.getAttribute("userId");

     if (sessionUserId == null) {

         System.out.println(
                 "No logged-in user ID found in session."
         );

         Executions.sendRedirect("/zul/login.zul");

         return;
     }


     // =====================================================
     // CONVERT SESSION USER ID TO LONG
     // =====================================================

     long userId;

     if (sessionUserId instanceof Number) {

         userId =
                 ((Number) sessionUserId)
                         .longValue();

     } else {

         try {

             userId =
                     Long.parseLong(
                             sessionUserId.toString()
                     );

         } catch (NumberFormatException e) {

             System.out.println(
                     "Invalid userId in session: "
                     + sessionUserId
             );

             Executions.sendRedirect("/zul/login.zul");

             return;
         }
     }


     // =====================================================
     // CHECK OUTWARD MAKER ROLE
     // =====================================================

    

     // =====================================================
     // DYNAMIC LOGGED-IN USER ID
     // =====================================================

     currentUserId =
             String.valueOf(userId);


     System.out.println(
             "======================================"
     );

     System.out.println(
             "OUTWARD MAKER DASHBOARD"
     );

     System.out.println(
             "doAfterCompose() START"
     );

     System.out.println(
             "Current Maker User : "
             + currentUserId
     );

     


     // =====================================================
     // INITIALIZE SERVICE
     // =====================================================

     service =
             new OutwardMakerDashboardService();

     loadDashboard();


     System.out.println(
             "======================================"
     );

     System.out.println(
             "doAfterCompose() END"
     );
    }

    

    // =========================================================
    // LOAD DASHBOARD
    // =========================================================

    private void loadDashboard() {

        loadBatches();

        /*
         * Summary counts should be obtained through
         * Service -> DAO when those methods are available.
         *
         * No SQL is placed inside this controller.
         */
    }

    // =========================================================
    // LOAD BATCHES
    // =========================================================

    private void loadBatches() {

        if (batchListbox == null) {

            System.out.println(
                    "ERROR: batchListbox is NULL."
            );

            return;
        }

        if (service == null) {

            System.out.println(
                    "ERROR: service is NULL."
            );

            return;
        }

        try {

            System.out.println(
                    "Calling service.getBatches()..."
            );

            List<OutwardBatch> batches =
                    service.getBatches();

            if (batches == null) {
                batches = new ArrayList<>();
            }

            System.out.println(
                    "Batches returned = "
                    + batches.size()
            );

            // -------------------------------------------------
            // DEBUG DATABASE VALUES
            // -------------------------------------------------

            for (OutwardBatch batch : batches) {

                if (batch == null) {
                    continue;
                }

                System.out.println(
                        "--------------------------------------"
                );

                System.out.println(
                        "Batch Number      : "
                        + batch.getBatchNumber()
                );

                System.out.println(
                        "Number Of Cheques : "
                        + batch.getNumberOfCheques()
                );

                System.out.println(
                        "Batch Status      : "
                        + batch.getBatchStatus()
                );

                System.out.println(
                        "Maker User        : "
                        + batch.getMakerUserNumber()
                );

                System.out.println(
                        "Lock Status       : "
                        + batch.getLockStatus()
                );

                System.out.println(
                        "Locked By         : "
                        + batch.getLockedBy()
                );

                System.out.println(
                        "Locked At         : "
                        + batch.getLockedAt()
                );
            }

            // -------------------------------------------------
            // CREATE MODEL
            // -------------------------------------------------

            ListModelList<OutwardBatch> model =
                    new ListModelList<>();

            model.addAll(batches);

            // -------------------------------------------------
            // RENDERER
            // -------------------------------------------------

            batchListbox.setItemRenderer(
                    new ListitemRenderer<OutwardBatch>() {

                        @Override
                        public void render(
                                Listitem item,
                                OutwardBatch batch,
                                int index)
                                throws Exception {

                            renderBatchRow(
                                    item,
                                    batch
                            );
                        }
                    }
            );

            // -------------------------------------------------
            // SET MODEL
            // -------------------------------------------------

            batchListbox.setModel(model);

            System.out.println(
                    "Dashboard model loaded successfully."
            );

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to load batches from database.\n\n"
                    + "Error: "
                    + safeExceptionMessage(e),
                    "Dashboard Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }

    // =========================================================
    // RENDER ONE BATCH ROW
    //
    // ZUL COLUMN ORDER:
    //
    // 1. Batch No
    // 2. Total Cheques
    // 3. Status
    // 4. Action
    // 5. Assignment
    //
    // IMPORTANT:
    //
    // Do NOT add Maker User as a separate column.
    // =========================================================

    private void renderBatchRow(
            Listitem item,
            OutwardBatch batch) {

        if (batch == null) {
            return;
        }

        // =====================================================
        // 1. BATCH NUMBER
        // =====================================================

        Listcell batchCell =
                new Listcell();

        batchCell.appendChild(
                new Label(
                        safeValue(
                                batch.getBatchNumber()
                        )
                )
        );

        item.appendChild(batchCell);

        // =====================================================
        // 2. NUMBER OF CHEQUES
        // =====================================================

        Listcell totalCell =
                new Listcell();

        String chequeCount =
                batch.getNumberOfCheques() == null
                        ? "0"
                        : String.valueOf(
                                batch.getNumberOfCheques()
                        );

        totalCell.appendChild(
                new Label(chequeCount)
        );

        item.appendChild(totalCell);

        // =====================================================
        // 3. STATUS
        // =====================================================

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

        // =====================================================
        // READ ASSIGNMENT / LOCK INFORMATION
        // =====================================================

        String makerUserNumber =
                batch.getMakerUserNumber();

        String lockStatus =
                batch.getLockStatus();

        String lockedBy =
                batch.getLockedBy();

        boolean hasMakerAssignment =
                hasValue(
                        makerUserNumber
                );

        boolean hasLockedBy =
                hasValue(
                        lockedBy
                );

        boolean lockStatusLocked =
                isLockedStatus(
                        lockStatus
                );

        /*
         * A batch is available only when:
         *
         * 1. No Maker has claimed it
         * 2. Nobody has locked it
         *
         * The actual assignment information is obtained
         * from outward_batch_assignment by the DAO.
         */

        boolean isAvailable =
                !hasMakerAssignment
                &&
                !hasLockedBy
                &&
                !lockStatusLocked;

        // =====================================================
        // 4. ACTION
        // =====================================================

        Listcell actionCell =
                new Listcell();

        if (isAvailable) {

            Button openButton =
                    new Button("Open");

            openButton.setWidth("75px");

            openButton.setHeight("32px");

            openButton.setStyle(
                    "background:#12B76A;"
                    + "color:white;"
                    + "border:none;"
                    + "border-radius:5px;"
                    + "font-weight:bold;"
                    + "cursor:pointer;"
            );

            openButton.addEventListener(
                    Events.ON_CLICK,
                    event ->
                            openAndAssignBatch(
                                    batch.getBatchNumber()
                            )
            );

            actionCell.appendChild(
                    openButton
            );

        } else {

            Label lockedLabel =
                    new Label("🔒 Locked");

            lockedLabel.setStyle(
                    "color:#E74C3C;"
                    + "font-weight:bold;"
            );

            actionCell.appendChild(
                    lockedLabel
            );
        }

        item.appendChild(actionCell);

        // =====================================================
        // 5. ASSIGNMENT
        // =====================================================

        Listcell assignmentCell =
                new Listcell();

        if (hasMakerAssignment) {

            /*
             * Batch has already been assigned to a Maker.
             */

            assignmentCell.appendChild(
                    new Label(
                            "Maker "
                            + makerUserNumber
                    )
            );

        } else if (hasLockedBy) {

            /*
             * Locked but Maker number is not available.
             */

            assignmentCell.appendChild(
                    new Label(
                            "Locked by "
                            + lockedBy
                    )
            );

        } else {

            /*
             * Nobody has claimed this batch.
             */

            assignmentCell.appendChild(
                    new Label("Available")
            );
        }

        item.appendChild(assignmentCell);
    }

    // =========================================================
    // OPEN + AUTOMATICALLY ASSIGN BATCH
    // =========================================================

    private void openAndAssignBatch(
            String batchNumber) {

        // -----------------------------------------------------
        // VALIDATE BATCH NUMBER
        // -----------------------------------------------------

        if (!hasValue(batchNumber)) {

            Messagebox.show(
                    "Invalid batch number.",
                    "Batch",
                    Messagebox.OK,
                    Messagebox.ERROR
            );

            return;
        }

        String cleanBatchNumber =
                batchNumber.trim();

        // -----------------------------------------------------
        // CURRENT MAKER
        // -----------------------------------------------------

        String userId =
                currentUserId;

        System.out.println(
                "======================================"
        );

        System.out.println(
                "OPEN BATCH REQUEST"
        );

        System.out.println(
                "Batch : "
                + cleanBatchNumber
        );

        System.out.println(
                "User  : "
                + userId
        );

        System.out.println(
                "======================================"
        );

        try {

            // =================================================
            // GET CURRENT BATCH
            // =================================================

            OutwardBatch batch =
                    findBatch(
                            cleanBatchNumber
                    );

            if (batch == null) {

                Messagebox.show(
                        "Batch "
                        + cleanBatchNumber
                        + " was not found.",
                        "Batch Not Found",
                        Messagebox.OK,
                        Messagebox.ERROR
                );

                return;
            }

            // =================================================
            // CHECK EXISTING MAKER ASSIGNMENT
            // =================================================

            if (hasValue(
                    batch.getMakerUserNumber()
            )) {

                Messagebox.show(
                        "Batch "
                        + cleanBatchNumber
                        + " is already assigned to Maker "
                        + batch.getMakerUserNumber()
                        + ".",
                        "Batch Locked",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION
                );

                return;
            }

            // =================================================
            // CHECK LOCK
            // =================================================

            if (hasValue(
                    batch.getLockedBy()
            )) {

                Messagebox.show(
                        "Batch "
                        + cleanBatchNumber
                        + " is already locked by "
                        + batch.getLockedBy()
                        + ".",
                        "Batch Locked",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION
                );

                return;
            }

            // =================================================
            // ATOMIC ASSIGNMENT + VALIDATION
            // =================================================

            OutwardValidationResult result =
                    service.assignAndValidate(
                            cleanBatchNumber,
                            userId
                    );

            // =================================================
            // ASSIGNMENT FAILED
            // =================================================

            if (result == null) {

                Messagebox.show(
                        "Batch "
                        + cleanBatchNumber
                        + " could not be opened.\n\n"
                        + "It may already be assigned "
                        + "or locked by another Maker.",
                        "Batch Locked",
                        Messagebox.OK,
                        Messagebox.ERROR
                );

                /*
                 * Reload immediately so this Maker sees
                 * the latest database state.
                 */

                loadBatches();

                return;
            }

            // =================================================
            // RELOAD DASHBOARD
            // =================================================

            loadBatches();

            // =================================================
            // SHOW VALIDATION RESULT
            // =================================================

            showValidationResult(
                    cleanBatchNumber,
                    result
            );

            // =================================================
            // VALIDATION COUNTS
            // =================================================

            int dataEntry =
                    result.getDataEntryErrors();

            int micr =
                    result.getMicrErrors();

            int amountAccount =
                    result.getAmountAccountErrors();

            // =================================================
            // DATA ENTRY
            // =================================================

            if (dataEntry > 0) {

                openDataEntry(
                        cleanBatchNumber
                );

                return;
            }

            // =================================================
            // MICR REPAIR
            // =================================================

            if (micr > 0) {

                openMicrRepair(
                        cleanBatchNumber
                );

                return;
            }

            // =================================================
            // AMOUNT / ACCOUNT
            // =================================================

            if (amountAccount > 0) {

                openAmountAccount(
                        cleanBatchNumber
                );

                return;
            }

            // =================================================
            // NO ERRORS
            // =================================================

            Messagebox.show(
                    "Batch "
                    + cleanBatchNumber
                    + " is valid and ready for Checker.",
                    "Batch Ready",
                    Messagebox.OK,
                    Messagebox.INFORMATION
            );

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to open batch "
                    + cleanBatchNumber
                    + ".\n\n"
                    + "Error: "
                    + safeExceptionMessage(e),
                    "Open Batch Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }
    }

    // =========================================================
    // SHOW VALIDATION RESULT
    // =========================================================

    private void showValidationResult(
            String batchNumber,
            OutwardValidationResult result) {

        if (result == null) {
            return;
        }

        int dataEntry =
                result.getDataEntryErrors();

        int micr =
                result.getMicrErrors();

        int amountAccount =
                result.getAmountAccountErrors();

        int totalErrors =
                dataEntry
                + micr
                + amountAccount;

        if (totalErrors == 0) {

            Messagebox.show(
                    "Batch "
                    + batchNumber
                    + " has no validation errors.\n\n"
                    + "The batch is ready for Checker.",
                    "Validation Successful",
                    Messagebox.OK,
                    Messagebox.INFORMATION
            );

            return;
        }

        String message =
                "Batch "
                + batchNumber
                + " validation completed.\n\n"
                + "Total Cheques: "
                + result.getTotalCheques()
                + "\n\n"
                + "Data Entry Errors: "
                + dataEntry
                + "\n"
                + "MICR Errors: "
                + micr
                + "\n"
                + "Amount / Account Errors: "
                + amountAccount;

        Messagebox.show(
                message,
                "Validation Required",
                Messagebox.OK,
                Messagebox.EXCLAMATION
        );
    }

    // =========================================================
    // OPEN DATA ENTRY
    // =========================================================

    private void openDataEntry(
            String batchNumber) {

        Executions.sendRedirect(
                "/outward-maker-data-entry.zul"
                + "?batchNumber="
                + encode(batchNumber)
        );
    }

    // =========================================================
    // OPEN MICR REPAIR
    // =========================================================

    private void openMicrRepair(
            String batchNumber) {

        Executions.sendRedirect(
                "/outward-maker-micr-repair.zul"
                + "?batchNumber="
                + encode(batchNumber)
        );
    }

    // =========================================================
    // OPEN AMOUNT / ACCOUNT
    // =========================================================

    private void openAmountAccount(
            String batchNumber) {

        Executions.sendRedirect(
                "/outward-maker-amount-account.zul"
                + "?batchNumber="
                + encode(batchNumber)
        );
    }

    // =========================================================
    // FIND BATCH
    // =========================================================

    private OutwardBatch findBatch(
            String batchNumber) {

        if (!hasValue(batchNumber)) {
            return null;
        }

        try {

            List<OutwardBatch> batches =
                    service.getBatches();

            if (batches == null) {
                return null;
            }

            for (OutwardBatch batch : batches) {

                if (batch != null
                        &&
                        batchNumber.trim()
                                .equalsIgnoreCase(
                                        safeValue(
                                                batch.getBatchNumber()
                                        )
                                )) {

                    return batch;
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to find batch.\n\n"
                    + "Error: "
                    + safeExceptionMessage(e),
                    "Batch Error",
                    Messagebox.OK,
                    Messagebox.ERROR
            );
        }

        return null;
    }

    // =========================================================
    // CHECK LOCK STATUS
    // =========================================================

    private boolean isLockedStatus(
            String status) {

        if (!hasValue(status)) {
            return false;
        }

        String cleanStatus =
                status.trim();

        return "LOCKED".equalsIgnoreCase(
                    cleanStatus
                )
                ||
                "IN_PROGRESS".equalsIgnoreCase(
                    cleanStatus
                )
                ||
                "ASSIGNED".equalsIgnoreCase(
                    cleanStatus
                );
    }

    // =========================================================
    // CHECK VALUE
    // =========================================================

    private boolean hasValue(
            String value) {

        return value != null
                &&
                !value.trim().isEmpty();
    }

    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safeValue(
            String value) {

        if (!hasValue(value)) {
            return "-";
        }

        return value.trim();
    }

    // =========================================================
    // SAFE EXCEPTION MESSAGE
    // =========================================================

    private String safeExceptionMessage(
            Exception e) {

        if (e == null) {
            return "Unknown error";
        }

        String message =
                e.getMessage();

        if (!hasValue(message)) {

            return e.getClass()
                    .getSimpleName();
        }

        return message;
    }

    // =========================================================
    // URL ENCODING
    // =========================================================

    private String encode(
            String value) {

        try {

            return URLEncoder.encode(
                    value,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {

            return value;
        }
    }
}
