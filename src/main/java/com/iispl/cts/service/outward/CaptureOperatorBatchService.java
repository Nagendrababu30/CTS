package com.iispl.cts.service.outward;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.zkoss.util.media.Media;

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

        notificationService =
                new NotificationService();
    }

    // =========================================================
    // BRANCHES
    // =========================================================

    public List<String[]> getActiveBranches() {

        return dao.getActiveBranches();
    }

    public String getBranchName(
            String branchCode) {

        return dao.getBranchName(
                branchCode);
    }

    // =========================================================
    // COUNT MISMATCH EXCEPTION
    // =========================================================
    //
    // This exception is used ONLY when:
    //
    // Entered cheque count != XML parsed cheque count
    //
    // The controller catches this exception and asks:
    //
    // Continue / Reject Batch
    //
    // =========================================================

    public static class ChequeCountMismatchException
            extends IllegalArgumentException {

        private static final long serialVersionUID = 1L;

        private final int enteredCount;
        private final int xmlCount;

        public ChequeCountMismatchException(
                int enteredCount,
                int xmlCount) {

            super(
                    "Batch cheque count mismatch.\n\n"
                    + "Entered Count: "
                    + enteredCount
                    + "\n"
                    + "XML Cheque Count: "
                    + xmlCount);

            this.enteredCount =
                    enteredCount;

            this.xmlCount =
                    xmlCount;
        }

        public int getEnteredCount() {

            return enteredCount;
        }

        public int getXmlCount() {

            return xmlCount;
        }
    }

    // =========================================================
    // CAPTURE BATCH
    // =========================================================
    //
    // Default behavior:
    //
    // allowCountMismatch = false
    //
    // If mismatch is found, ChequeCountMismatchException
    // is thrown BEFORE batch creation.
    //
    // =========================================================

    public OutwardBatch captureBatch(
            String branchCode,
            Integer chequeCount,
            List<Media> uploadedFiles,
            int createdBy)
            throws Exception {

        return captureBatch(
                branchCode,
                chequeCount,
                uploadedFiles,
                createdBy,
                false);
    }

    // =========================================================
    // CAPTURE BATCH WITH MISMATCH DECISION
    // =========================================================
    //
    // allowCountMismatch:
    //
    // false = mismatch requires operator decision
    //
    // true  = continue with XML parsed count
    //
    // =========================================================

    public OutwardBatch captureBatch(
            String branchCode,
            Integer chequeCount,
            List<Media> uploadedFiles,
            int createdBy,
            boolean allowCountMismatch)
            throws Exception {

        File batchFolder = null;

        try {

            // =================================================
            // VALIDATION
            // =================================================

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

            if (uploadedFiles == null ||
                    uploadedFiles.isEmpty()) {

                throw new IllegalArgumentException(
                        "Please select the XML file and cheque images.");
            }

            if (createdBy <= 0) {

                throw new IllegalArgumentException(
                        "Invalid logged-in user.");
            }

            // =================================================
            // CREATE TEMPORARY SERVER-SIDE BATCH FOLDER
            // =================================================

            batchFolder =
                    createBatchFolder();

            System.out.println(
                    "=================================");

            System.out.println(
                    "CAPTURE OPERATOR UPLOAD");

            System.out.println(
                    "Server batch folder: "
                    + batchFolder.getAbsolutePath());

            System.out.println(
                    "=================================");

            // =================================================
            // SAVE UPLOADED FILES
            // =================================================

            File xmlFile =
                    saveUploadedFiles(
                            uploadedFiles,
                            batchFolder);

            // =================================================
            // XML VALIDATION
            // =================================================

            if (xmlFile == null) {

                throw new IllegalArgumentException(
                        "No XML file found in selected files.");
            }

            System.out.println(
                    "XML detected: "
                    + xmlFile.getAbsolutePath());

            // =================================================
            // GENERATE BATCH NUMBER
            // =================================================

            String batchNumber =
                    generateBatchNumber();

            System.out.println(
                    "Generated Batch Number: "
                    + batchNumber);

            // =================================================
            // PARSE XML
            // =================================================

            List<OutwardCheque> cheques =
                    parser.parse(
                            xmlFile,
                            batchNumber,
                            batchFolder);

            if (cheques == null) {

                throw new IllegalArgumentException(
                        "Unable to parse XML cheque records.");
            }

            System.out.println(
                    "XML cheque count: "
                    + cheques.size());

            // =================================================
            // XML CHEQUE VALIDATION
            // =================================================

            if (cheques.isEmpty()) {

                throw new IllegalArgumentException(
                        "No cheque records found in XML.");
            }

            // =================================================
            // COUNT VALIDATION
            // =================================================
            //
            // IMPORTANT:
            //
            // We DO NOT silently reject mismatch.
            //
            // First capture attempt:
            //
            // entered = 5
            // XML     = 3
            //
            // throws ChequeCountMismatchException.
            //
            // Controller asks operator.
            //
            // =================================================

            if (cheques.size() != chequeCount) {

                if (!allowCountMismatch) {

                    throw new ChequeCountMismatchException(
                            chequeCount,
                            cheques.size());
                }

                // =================================================
                // CONTINUE WAS SELECTED
                // =================================================
                //
                // IMPORTANT:
                //
                // The manually entered count is now ignored.
                //
                // Actual count = XML parsed cheque objects.
                //
                // =================================================

                System.out.println(
                        "COUNT MISMATCH OVERRIDDEN BY OPERATOR.");

                System.out.println(
                        "Entered Count: "
                        + chequeCount);

                System.out.println(
                        "XML Count: "
                        + cheques.size());

                System.out.println(
                        "Using XML parsed cheque count: "
                        + cheques.size());
            }

            // =================================================
            // FINAL ACTUAL CHEQUE COUNT
            // =================================================
            //
            // ALWAYS use parsed cheque objects.
            //
            // =================================================

            int actualChequeCount =
                    cheques.size();

            // =================================================
            // DUPLICATE CHECK - WITHIN XML
            // =================================================
            //
            // Prevent the same cheque from appearing twice
            // in the same uploaded XML.
            //
            // Branch is NOT part of this key.
            //
            // =================================================

            Set<String> seenChequeKeys =
                    new HashSet<>();

            StringBuilder internalDuplicates =
                    new StringBuilder();

            int internalDuplicateCount = 0;

            for (OutwardCheque cheque :
                    cheques) {

                if (cheque == null) {
                    continue;
                }

                String duplicateKey =
                        buildDuplicateKey(
                                cheque);

                if (!seenChequeKeys.add(
                        duplicateKey)) {

                    internalDuplicateCount++;

                    internalDuplicates.append(
                            "\nDuplicate #")
                            .append(
                                    internalDuplicateCount)
                            .append(
                                    "\n");

                    appendChequeDetails(
                            internalDuplicates,
                            cheque);

                    internalDuplicates.append(
                            "\n");
                }
            }

            if (internalDuplicateCount > 0) {

                throw new IllegalArgumentException(
                        "DUPLICATE CHEQUE(S) FOUND "
                        + "IN THE UPLOADED XML."
                        + "\n"
                        + internalDuplicates
                        + "\nBATCH NOT CREATED.");
            }

            // =================================================
            // DATABASE DUPLICATE CHECK
            // =================================================
            //
            // Branch is intentionally NOT supplied to duplicate
            // matching.
            //
            // Same cheque in another branch = duplicate.
            //
            // =================================================

            List<String> duplicateCheques =
                    dao.findDuplicateChequeDetails(
                            cheques);

            if (duplicateCheques != null &&
                    !duplicateCheques.isEmpty()) {

                StringBuilder duplicateMessage =
                        new StringBuilder();

                duplicateMessage.append(
                        "DUPLICATE CHEQUE(S) FOUND");

                duplicateMessage.append(
                        "\n\n");

                duplicateMessage.append(
                        "The following cheque(s) already "
                        + "exist in the system:");

                duplicateMessage.append(
                        "\n\n");

                int count = 1;

                for (String duplicate :
                        duplicateCheques) {

                    duplicateMessage.append(
                            "----------------------------------------");

                    duplicateMessage.append(
                            "\nDuplicate #")
                            .append(count++)
                            .append(
                                    "\n");

                    duplicateMessage.append(
                            duplicate);

                    duplicateMessage.append(
                            "\n");
                }

                duplicateMessage.append(
                        "----------------------------------------");

                duplicateMessage.append(
                        "\n\nBATCH NOT CREATED.");

                duplicateMessage.append(
                        "\nDuplicate cheque(s) must be removed "
                        + "before creating the batch.");

                throw new IllegalArgumentException(
                        duplicateMessage.toString());
            }

            // =================================================
            // CREATE BATCH OBJECT
            // =================================================

            LocalDateTime now =
                    LocalDateTime.now();

            OutwardBatch batch =
                    new OutwardBatch();

            batch.setBatchNumber(
                    batchNumber);

            batch.setBranchCode(
                    branchCode.trim());

            // =================================================
            // IMPORTANT
            // =================================================
            //
            // This is ALWAYS the XML parsed count.
            //
            // If:
            //
            // Entered = 5
            // XML     = 3
            //
            // after Continue:
            //
            // batch.cheque_count = 3
            //
            // =================================================

            batch.setNumberOfCheques(
                    actualChequeCount);

            batch.setBatchFolderPath(
                    batchFolder.getAbsolutePath());

            batch.setXmlFilePath(
                    xmlFile.getAbsolutePath());

            batch.setCreatedBy(
                    String.valueOf(
                            createdBy));

            batch.setCreatedAt(
                    now);

            batch.setBatchStatus(
                    "CAPTURED");

            // =================================================
            // UPDATE CHEQUES
            // =================================================

            for (OutwardCheque cheque :
                    cheques) {

                // Same generated batch number
                cheque.setBatchNumber(
                        batchNumber);

                // Selected branch
                cheque.setBranchCode(
                        branchCode.trim());

                // Created user
                cheque.setCreatedBy(
                        String.valueOf(
                                createdBy));

                // Created time
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

            // =================================================
            // SAVE BATCH + CHEQUES
            // =================================================
            //
            // DAO performs another duplicate check inside the
            // transaction before outward_batch INSERT.
            //
            // This gives an additional protection against
            // concurrent capture requests.
            //
            // =================================================

            dao.saveBatchWithCheques(
                    batch,
                    cheques,
                    createdBy);

            System.out.println(
                    "Batch saved successfully: "
                    + batchNumber);

            // =================================================
            // NOTIFICATION
            // =================================================

            List<Integer> makerUserIds =
                    notificationService
                            .getActiveUserIdsByRole(
                                    3);

            for (Integer makerUserId :
                    makerUserIds) {

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

        } catch (Exception e) {

            // =================================================
            // DELETE TEMPORARY FOLDER ON FAILURE
            // =================================================
            //
            // No batch was created if an exception happens
            // before DAO successfully commits.
            //
            // =================================================

            if (batchFolder != null) {

                try {

                    deleteFolder(
                            batchFolder);

                } catch (Exception cleanupException) {

                    cleanupException.printStackTrace();
                }
            }

            throw e;
        }
    }

    // =========================================================
    // BUILD DUPLICATE KEY
    // =========================================================
    //
    // Branch is intentionally excluded.
    //
    // =========================================================

    private String buildDuplicateKey(
            OutwardCheque cheque) {

        return normalize(
                    cheque.getChequeNumber())
                + "|"
                + normalize(
                    cheque.getDrawerAccountNumber())
                + "|"
                + normalize(
                    cheque.getDrawerName())
                + "|"
                + normalize(
                    cheque.getDepositorAccountNumber())
                + "|"
                + normalize(
                    cheque.getPayeeName())
                + "|"
                + (
                    cheque.getAmount() == null
                            ? ""
                            : cheque.getAmount()
                                    .stripTrailingZeros()
                                    .toPlainString()
                  )
                + "|"
                + normalize(
                    cheque.getAmountInWords())
                + "|"
                + (
                    cheque.getChequeDate() == null
                            ? ""
                            : cheque.getChequeDate()
                                    .toString()
                  )
                + "|"
                + normalize(
                    cheque.getBankCode())
                + "|"
                + normalize(
                    cheque.getCityCode());
    }

    // =========================================================
    // NORMALIZE
    // =========================================================

    private String normalize(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toUpperCase();
    }

    // =========================================================
    // APPEND CHEQUE DETAILS
    // =========================================================

    private void appendChequeDetails(
            StringBuilder builder,
            OutwardCheque cheque) {

        builder.append(
                "Cheque No: ")
                .append(
                        safeValue(
                                cheque.getChequeNumber()));

        builder.append(
                "\nDrawer Account: ")
                .append(
                        safeValue(
                                cheque.getDrawerAccountNumber()));

        builder.append(
                "\nDrawer Name: ")
                .append(
                        safeValue(
                                cheque.getDrawerName()));

        builder.append(
                "\nPayee Account: ")
                .append(
                        safeValue(
                                cheque.getDepositorAccountNumber()));

        builder.append(
                "\nPayee Name: ")
                .append(
                        safeValue(
                                cheque.getPayeeName()));

        builder.append(
                "\nAmount: ")
                .append(
                        cheque.getAmount() == null
                                ? ""
                                : cheque.getAmount()
                                        .toPlainString());

        builder.append(
                "\nCheque Date: ")
                .append(
                        cheque.getChequeDate() == null
                                ? ""
                                : cheque.getChequeDate()
                                        .toString());

        builder.append(
                "\nBank Code: ")
                .append(
                        safeValue(
                                cheque.getBankCode()));

        builder.append(
                "\nCity Code: ")
                .append(
                        safeValue(
                                cheque.getCityCode()));
    }

    // =========================================================
    // SAFE VALUE
    // =========================================================

    private String safeValue(
            String value) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }

    // =========================================================
    // CREATE SERVER BATCH FOLDER
    // =========================================================

    private File createBatchFolder()
            throws Exception {

        String tempDirectory =
                System.getProperty(
                        "java.io.tmpdir");

        File rootFolder =
                new File(
                        tempDirectory,
                        "cts-capture-batches");

        if (!rootFolder.exists()) {

            if (!rootFolder.mkdirs()) {

                throw new IllegalStateException(
                        "Unable to create capture batch root folder:\n"
                        + rootFolder.getAbsolutePath());
            }
        }

        String uniqueFolderName =
                "BATCH-"
                + UUID.randomUUID()
                        .toString();

        File batchFolder =
                new File(
                        rootFolder,
                        uniqueFolderName);

        if (!batchFolder.mkdirs()) {

            throw new IllegalStateException(
                    "Unable to create batch upload folder:\n"
                    + batchFolder.getAbsolutePath());
        }

        return batchFolder;
    }

    // =========================================================
    // SAVE UPLOADED FILES
    // =========================================================

    private File saveUploadedFiles(
            List<Media> uploadedFiles,
            File batchFolder)
            throws Exception {

        File xmlFile = null;

        int xmlCount = 0;

        for (Media media :
                uploadedFiles) {

            if (media == null) {
                continue;
            }

            String originalName =
                    media.getName();

            if (originalName == null ||
                    originalName.trim().isEmpty()) {

                continue;
            }

            String fileName =
                    new File(originalName)
                            .getName();

            if (fileName.isEmpty()) {
                continue;
            }

            File targetFile =
                    new File(
                            batchFolder,
                            fileName);

            // =================================================
            // SECURITY
            // =================================================

            String canonicalFolder =
                    batchFolder
                            .getCanonicalPath()
                    + File.separator;

            String canonicalTarget =
                    targetFile
                            .getCanonicalPath();

            if (!canonicalTarget
                    .startsWith(
                            canonicalFolder)) {

                throw new IllegalArgumentException(
                        "Invalid uploaded file name: "
                        + fileName);
            }

            // =================================================
            // XML
            // =================================================

            boolean isXml =
                    fileName
                            .toLowerCase()
                            .endsWith(".xml");

            if (isXml) {

                xmlCount++;

                if (xmlCount > 1) {

                    throw new IllegalArgumentException(
                            "Multiple XML files selected.\n\n"
                            + "Please select only one XML file.");
                }

                xmlFile =
                        targetFile;
            }

            // =================================================
            // WRITE
            // =================================================

            writeMediaToFile(
                    media,
                    targetFile);

            System.out.println(
                    "Uploaded file saved: "
                    + targetFile.getAbsolutePath());
        }

        if (xmlCount == 0) {

            throw new IllegalArgumentException(
                    "No XML file selected.");
        }

        return xmlFile;
    }

    // =========================================================
    // WRITE MEDIA TO FILE
    // =========================================================

    private void writeMediaToFile(
            Media media,
            File targetFile)
            throws Exception {

        // =====================================================
        // XML / TEXT FILE
        // =====================================================

        if (!media.isBinary()) {

            String data =
                    media.getStringData();

            if (data == null) {

                throw new IllegalArgumentException(
                        "Unable to read uploaded file: "
                        + media.getName());
            }

            try (FileOutputStream output =
                         new FileOutputStream(
                                 targetFile)) {

                output.write(
                        data.getBytes(
                                java.nio.charset.StandardCharsets.UTF_8));
            }

            return;
        }

        // =====================================================
        // BINARY FILE - IMAGE
        // =====================================================

        byte[] data =
                media.getByteData();

        if (data == null) {

            throw new IllegalArgumentException(
                    "Unable to read uploaded image: "
                    + media.getName());
        }

        try (FileOutputStream output =
                     new FileOutputStream(
                             targetFile)) {

            output.write(data);
        }
    }

    // =========================================================
    // DELETE TEMPORARY FOLDER
    // =========================================================

    private void deleteFolder(
            File folder) {

        if (folder == null ||
                !folder.exists()) {

            return;
        }

        File[] files =
                folder.listFiles();

        if (files != null) {

            for (File file :
                    files) {

                if (file.isDirectory()) {

                    deleteFolder(file);

                } else {

                    try {
                        file.delete();
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        try {
            folder.delete();
        } catch (Exception ignored) {
        }
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