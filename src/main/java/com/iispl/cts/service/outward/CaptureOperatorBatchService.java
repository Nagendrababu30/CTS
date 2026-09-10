package com.iispl.cts.service.outward;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.iispl.cts.dao.outward.CaptureOperatorBatchDAO;
import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;

public class CaptureOperatorBatchService {

    private final CaptureOperatorBatchDAO dao;
    private final CaptureOperatorXMLParser parser;
    private final NotificationService notificationService;

    public CaptureOperatorBatchService() {

        dao = new CaptureOperatorBatchDAO();
        parser = new CaptureOperatorXMLParser();
        notificationService = new NotificationService();
    }

    // =========================================================
    // BRANCHES
    // =========================================================

    public List<String[]> getActiveBranches() {

        return dao.getActiveBranches();
    }

    public String getBranchName(String branchCode) {

        return dao.getBranchName(branchCode);
    }

    // =========================================================
    // CAPTURE BATCH
    // =========================================================

    public OutwardBatch captureBatch(
            String branchCode,
            Integer chequeCount,
            String folderPath,
            int createdBy) throws Exception {

        // -----------------------------------------------------
        // VALIDATION
        // -----------------------------------------------------

        if (branchCode == null ||
                branchCode.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Please select branch code.");
        }

        if (chequeCount == null ||
                chequeCount <= 0) {

            throw new IllegalArgumentException(
                    "Please enter a valid number of cheques.");
        }

        if (folderPath == null ||
                folderPath.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Please enter batch folder path.");
        }

        if (createdBy <= 0) {

            throw new IllegalArgumentException(
                    "Invalid logged-in user.");
        }

        // -----------------------------------------------------
        // CREATE FOLDER OBJECT
        // -----------------------------------------------------

        File folder = new File(folderPath.trim());

        System.out.println(
                "Checking folder: "
                + folder.getAbsolutePath());

        if (!folder.exists()) {

            throw new IllegalArgumentException(
                    "Folder does not exist:\n"
                    + folder.getAbsolutePath());
        }

        if (!folder.isDirectory()) {

            throw new IllegalArgumentException(
                    "Path is not a folder:\n"
                    + folder.getAbsolutePath());
        }

        // -----------------------------------------------------
        // FIND XML FILE
        // -----------------------------------------------------

        File[] xmlFiles = folder.listFiles(
                file -> file.isFile()
                        && file.getName()
                                .toLowerCase()
                                .endsWith(".xml"));

        if (xmlFiles == null ||
                xmlFiles.length == 0) {

            throw new IllegalArgumentException(
                    "No XML file found in:\n"
                    + folder.getAbsolutePath());
        }

        if (xmlFiles.length > 1) {

            throw new IllegalArgumentException(
                    "Multiple XML files found in:\n"
                    + folder.getAbsolutePath()
                    + "\n\nPlease keep only one XML file.");
        }

        File xmlFile = xmlFiles[0];

        System.out.println(
                "XML detected: "
                + xmlFile.getAbsolutePath());

        // -----------------------------------------------------
        // GENERATE BATCH NUMBER
        // -----------------------------------------------------

        String batchNumber = generateBatchNumber();

        System.out.println(
                "Generated Batch Number: "
                + batchNumber);

        // -----------------------------------------------------
        // PARSE XML
        // -----------------------------------------------------

        List<OutwardCheque> cheques =
                parser.parse(
                        xmlFile,
                        batchNumber,
                        folder);

        System.out.println(
                "XML cheque count: "
                + cheques.size());

        // -----------------------------------------------------
        // VALIDATE XML CHEQUES
        // -----------------------------------------------------

        if (cheques.isEmpty()) {

            throw new IllegalArgumentException(
                    "No cheque records found in XML.");
        }

        // -----------------------------------------------------
        // CHECK ENTERED COUNT WITH XML COUNT
        // -----------------------------------------------------

        if (cheques.size() != chequeCount) {

            throw new IllegalArgumentException(
                    "Cheque count mismatch.\n\n"
                    + "Entered Count: "
                    + chequeCount
                    + "\n"
                    + "XML Count: "
                    + cheques.size());
        }

        // -----------------------------------------------------
        // CREATE BATCH OBJECT
        // -----------------------------------------------------

        LocalDateTime now =
                LocalDateTime.now();

        OutwardBatch batch =
                new OutwardBatch();

        batch.setBatchNumber(
                batchNumber);

        batch.setBranchCode(
                branchCode.trim());

        batch.setNumberOfCheques(
                cheques.size());

        batch.setBatchFolderPath(
                folder.getAbsolutePath());

        batch.setXmlFilePath(
                xmlFile.getAbsolutePath());

        batch.setCreatedBy(
                String.valueOf(createdBy));

        batch.setCreatedAt(
                now);

        batch.setBatchStatus(
                "CAPTURED");

        // -----------------------------------------------------
        // UPDATE CHEQUES
        // -----------------------------------------------------

        for (OutwardCheque cheque : cheques) {

            // Same generated batch number
            cheque.setBatchNumber(
                    batchNumber);

            // Selected branch
            cheque.setBranchCode(
                    branchCode.trim());

            // Created user
            cheque.setCreatedBy(
                    String.valueOf(createdBy));

            cheque.setCreatedAt(
                    now);

            // Default status
            if (cheque.getChequeStatus() == null ||
                    cheque.getChequeStatus()
                            .trim()
                            .isEmpty()) {

                cheque.setChequeStatus(
                        "CAPTURED");
            }
        }

        // -----------------------------------------------------
        // SAVE BATCH + CHEQUES
        // -----------------------------------------------------

        /*
         * DAO handles the database transaction:
         *
         * 1. INSERT outward_batch
         * 2. INSERT outward_cheque records
         * 3. COMMIT
         *
         * If any operation fails:
         *
         * ROLLBACK
         */

        dao.saveBatchWithCheques(
                batch,
                cheques,
                createdBy);

        System.out.println(
                "Batch saved successfully: "
                + batchNumber);

        // =====================================================
        // AUTOMATIC NOTIFICATION
        // =====================================================

        /*
         * The notification is created ONLY after the batch
         * has been successfully saved.
         *
         * Role 3 = Outward Maker
         *
         * No Maker user ID is hardcoded here.
         */

        List<Integer> makerUserIds =
                notificationService
                        .getActiveUserIdsByRole(3);

        for (Integer makerUserId : makerUserIds) {

            notificationService.notifyUser(
                    makerUserId,
                    batchNumber,
                    null,
                    NotificationService.NEW_BATCH,
                    "New batch "
                            + batchNumber
                            + " has been received and is available for processing."
            );
        }

        System.out.println(
                "New batch notification sent to "
                + makerUserIds.size()
                + " active Outward Maker(s).");

        return batch;
    }

    // =========================================================
    // GENERATE DYNAMIC BATCH NUMBER
    // =========================================================

    private String generateBatchNumber() {

        return "OUT"
                + LocalDateTime.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyyMMddHHmmssSSS"));
    }

    // =========================================================
    // CAPTURED BATCHES
    // =========================================================

    public List<OutwardBatch> getCapturedBatches() {

        return dao.getCapturedBatches();
    }
}