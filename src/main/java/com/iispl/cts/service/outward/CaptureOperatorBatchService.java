package com.iispl.cts.service.outward;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    // CAPTURE BATCH - UPLOADED XML + IMAGES
    // =========================================================

    public OutwardBatch captureBatch(
            String branchCode,
            Integer chequeCount,
            List<Media> uploadedFiles,
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

        if (uploadedFiles == null ||
                uploadedFiles.isEmpty()) {

            throw new IllegalArgumentException(
                    "Please select the XML file and cheque images.");
        }

        if (createdBy <= 0) {

            throw new IllegalArgumentException(
                    "Invalid logged-in user.");
        }

        // -----------------------------------------------------
        // CREATE SERVER-SIDE BATCH FOLDER
        // -----------------------------------------------------

        File batchFolder =
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


        // -----------------------------------------------------
        // SAVE UPLOADED FILES
        // -----------------------------------------------------

        File xmlFile =
                saveUploadedFiles(
                        uploadedFiles,
                        batchFolder);


        // -----------------------------------------------------
        // XML VALIDATION
        // -----------------------------------------------------

        if (xmlFile == null) {

            throw new IllegalArgumentException(
                    "No XML file found in selected files.");
        }

        System.out.println(
                "XML detected: "
                + xmlFile.getAbsolutePath());


        // -----------------------------------------------------
        // GENERATE BATCH NUMBER
        // -----------------------------------------------------

        String batchNumber =
                generateBatchNumber();

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
                        batchFolder);

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
                batchFolder.getAbsolutePath());

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
         * Role 3 = Outward Maker
         *
         * Notification is sent only after the batch
         * has been successfully saved.
         */

        List<Integer> makerUserIds =
                notificationService
                        .getActiveUserIdsByRole(3);


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
    }


    // =========================================================
    // CREATE SERVER BATCH FOLDER
    // =========================================================

    private File createBatchFolder()
            throws Exception {

        /*
         * Files are stored under the Tomcat/application
         * temporary directory.
         *
         * Example:
         *
         * /tmp/cts-capture-batches/
         *      BATCH-xxxxxxxx/
         *          input.xml
         *          front001.jpg
         *          back001.jpg
         */

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


        // -----------------------------------------------------
        // LOOP THROUGH UPLOADED FILES
        // -----------------------------------------------------

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


            // -------------------------------------------------
            // SECURITY
            // -------------------------------------------------

            /*
             * Prevent path traversal such as:
             *
             * ../../some-file
             */

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


            // -------------------------------------------------
            // CHECK XML
            // -------------------------------------------------

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


            // -------------------------------------------------
            // WRITE FILE
            // -------------------------------------------------

            writeMediaToFile(
                    media,
                    targetFile);


            System.out.println(
                    "Uploaded file saved: "
                    + targetFile.getAbsolutePath());
        }


        // -----------------------------------------------------
        // XML REQUIRED
        // -----------------------------------------------------

        if (xmlCount == 0) {

            throw new IllegalArgumentException(
                    "No XML file selected.");
        }


        return xmlFile;
    }


    // =========================================================
    // WRITE MEDIA TO FILE
    // =========================================================
 // =========================================================
 // WRITE MEDIA TO FILE
 // =========================================================

 private void writeMediaToFile(
         Media media,
         File targetFile)
         throws Exception {

     // -----------------------------------------------------
     // XML / TEXT FILE
     // -----------------------------------------------------

     /*
      * Your ZK version requires getStringData()
      * for non-binary Media.
      */

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


     // -----------------------------------------------------
     // BINARY FILE - IMAGE
     // -----------------------------------------------------

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