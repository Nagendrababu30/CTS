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

    public CaptureOperatorBatchService() {
        dao = new CaptureOperatorBatchDAO();
        parser = new CaptureOperatorXMLParser();
    }

    // get active branches
    public List<String[]> getActiveBranches() {
        return dao.getActiveBranches();
    }

    // get branch name
    public String getBranchName(String branchCode) {
        return dao.getBranchName(branchCode);
    }

    public static class ChequeCountMismatchException extends IllegalArgumentException {

        private static final long serialVersionUID = 1L;
        private final int enteredCount;
        private final int xmlCount;

        public ChequeCountMismatchException(int enteredCount, int xmlCount) {
            super(
                "Batch cheque count mismatch.\n\n" +
                "Entered Count: " + enteredCount +
                "\nXML Cheque Count: " + xmlCount
            );
            this.enteredCount = enteredCount;
            this.xmlCount = xmlCount;
        }

        public int getEnteredCount() {
            return enteredCount;
        }

        public int getXmlCount() {
            return xmlCount;
        }
    }

    // capture batch
    public OutwardBatch captureBatch(
        String branchCode,
        Integer chequeCount,
        List<Media> uploadedFiles,
        int createdBy) throws Exception {

        return captureBatch(
            branchCode,
            chequeCount,
            uploadedFiles,
            createdBy,
            false
        );
    }

    // capture batch with count validation
    public OutwardBatch captureBatch(
        String branchCode,
        Integer chequeCount,
        List<Media> uploadedFiles,
        int createdBy,
        boolean allowCountMismatch) throws Exception {

        File batchFolder = null;

        try {
            if (branchCode == null || branchCode.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "Please select branch code."
                );
            }

            if (chequeCount == null || chequeCount <= 0) {
                throw new IllegalArgumentException(
                    "Please enter a valid number of cheques."
                );
            }

            if (uploadedFiles == null || uploadedFiles.isEmpty()) {
                throw new IllegalArgumentException(
                    "Please select the XML file and cheque images."
                );
            }

            if (createdBy <= 0) {
                throw new IllegalArgumentException(
                    "Invalid logged-in user."
                );
            }

            batchFolder = createBatchFolder();

            // save uploaded files
            File xmlFile = saveUploadedFiles(
                uploadedFiles,
                batchFolder
            );

            System.out.println("=================================");
            System.out.println("CAPTURE OPERATOR UPLOAD");
            System.out.println(
                "Server batch folder: " +
                batchFolder.getAbsolutePath()
            );
            System.out.println("=================================");

            if (xmlFile == null) {
                throw new IllegalArgumentException(
                    "No XML file found in selected files."
                );
            }

            System.out.println(
                "XML detected: " +
                xmlFile.getAbsolutePath()
            );

            String batchNumber = generateBatchNumber();

            System.out.println(
                "Generated Batch Number: " +
                batchNumber
            );

            // parse XML
            List<OutwardCheque> cheques =
                parser.parse(
                    xmlFile,
                    batchNumber,
                    batchFolder
                );

            if (cheques == null) {
                throw new IllegalArgumentException(
                    "Unable to parse XML cheque records."
                );
            }

            System.out.println(
                "XML cheque count: " +
                cheques.size()
            );

            if (cheques.isEmpty()) {
                throw new IllegalArgumentException(
                    "No cheque records found in XML."
                );
            }

            // validate cheque count
            if (cheques.size() != chequeCount) {
                if (!allowCountMismatch) {
                    throw new ChequeCountMismatchException(
                        chequeCount,
                        cheques.size()
                    );
                }

                System.out.println(
                    "COUNT MISMATCH OVERRIDDEN BY OPERATOR."
                );
                System.out.println(
                    "Entered Count: " +
                    chequeCount
                );
                System.out.println(
                    "XML Count: " +
                    cheques.size()
                );
                System.out.println(
                    "Using XML parsed cheque count: " +
                    cheques.size()
                );
            }

            int actualChequeCount = cheques.size();

            // check internal duplicate cheques
            Set<String> seenChequeKeys = new HashSet<>();
            StringBuilder internalDuplicates = new StringBuilder();
            int internalDuplicateCount = 0;

            for (OutwardCheque cheque : cheques) {
                if (cheque == null) {
                    continue;
                }

                String duplicateKey = buildDuplicateKey(cheque);

                if (!seenChequeKeys.add(duplicateKey)) {
                    internalDuplicateCount++;

                    internalDuplicates
                        .append("\nDuplicate #")
                        .append(internalDuplicateCount)
                        .append("\n");

                    appendChequeDetails(
                        internalDuplicates,
                        cheque
                    );

                    internalDuplicates.append("\n");
                }
            }

            if (internalDuplicateCount > 0) {
                throw new IllegalArgumentException(
                    "DUPLICATE CHEQUE(S) FOUND IN THE UPLOADED XML." +
                    "\n" +
                    internalDuplicates +
                    "\nBATCH NOT CREATED."
                );
            }

            // check database duplicates
            List<String> duplicateCheques =
                dao.findDuplicateChequeDetails(cheques);

            if (duplicateCheques != null && !duplicateCheques.isEmpty()) {
                StringBuilder duplicateMessage =
                    new StringBuilder();

                duplicateMessage.append(
                    "DUPLICATE CHEQUE(S) FOUND"
                );
                duplicateMessage.append("\n\n");
                duplicateMessage.append(
                    "The following cheque(s) already exist in the system:"
                );
                duplicateMessage.append("\n\n");

                int count = 1;

                for (String duplicate : duplicateCheques) {
                    duplicateMessage.append(
                        "----------------------------------------"
                    );
                    duplicateMessage
                        .append("\nDuplicate #")
                        .append(count++)
                        .append("\n");
                    duplicateMessage.append(duplicate);
                    duplicateMessage.append("\n");
                }

                duplicateMessage.append(
                    "----------------------------------------"
                );
                duplicateMessage.append(
                    "\n\nBATCH NOT CREATED."
                );
                duplicateMessage.append(
                    "\nDuplicate cheque(s) must be removed before creating the batch."
                );

                throw new IllegalArgumentException(
                    duplicateMessage.toString()
                );
            }

            // prepare batch data
            LocalDateTime now = LocalDateTime.now();

            OutwardBatch batch = new OutwardBatch();
            batch.setBatchNumber(batchNumber);
            batch.setBranchCode(branchCode.trim());
            batch.setNumberOfCheques(actualChequeCount);
            batch.setBatchFolderPath(
                batchFolder.getAbsolutePath()
            );
            batch.setXmlFilePath(
                xmlFile.getAbsolutePath()
            );
            batch.setCreatedBy(String.valueOf(createdBy));
            batch.setCreatedAt(now);
            batch.setBatchStatus("CAPTURED");

            // prepare cheque data
            for (OutwardCheque cheque : cheques) {
                cheque.setBatchNumber(batchNumber);
                cheque.setCreatedBy(String.valueOf(createdBy));
                cheque.setCreatedAt(now);

                if (cheque.getChequeStatus() == null ||
                    cheque.getChequeStatus().trim().isEmpty()) {

                    cheque.setChequeStatus("CAPTURED");
                }
            }

            // save batch and cheques
            dao.saveBatchWithCheques(
                batch,
                cheques,
                createdBy
            );

            System.out.println(
                "Batch saved successfully: " +
                batchNumber
            );

            return batch;

        } catch (Exception e) {
            // clean failed batch folder
            if (batchFolder != null) {
                try {
                    deleteFolder(batchFolder);
                } catch (Exception cleanupException) {
                    cleanupException.printStackTrace();
                }
            }

            throw e;
        }
    }

    // build duplicate cheque key
    private String buildDuplicateKey(OutwardCheque cheque) {
        return normalize(cheque.getChequeNumber()) + "|" +
            normalize(cheque.getDrawerAccountNumber()) + "|" +
            normalize(cheque.getDrawerName()) + "|" +
            normalize(cheque.getPayeeAccountNumber()) + "|" +
            normalize(cheque.getPayeeName()) + "|" +
            (
                cheque.getAmount() == null
                    ? ""
                    : cheque.getAmount()
                        .stripTrailingZeros()
                        .toPlainString()
            ) + "|" +
            normalize(cheque.getAmountInWords()) + "|" +
            (
                cheque.getChequeDate() == null
                    ? ""
                    : cheque.getChequeDate().toString()
            ) + "|" +
            normalize(cheque.getBankCode()) + "|" +
            normalize(cheque.getCityCode());
    }

    // normalize duplicate value
    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toUpperCase();
    }

    // append cheque details
    private void appendChequeDetails(
        StringBuilder builder,
        OutwardCheque cheque) {

        builder
            .append("Cheque No: ")
            .append(safeValue(cheque.getChequeNumber()));

        builder
            .append("\nDrawer Account: ")
            .append(safeValue(cheque.getDrawerAccountNumber()));

        builder
            .append("\nDrawer Name: ")
            .append(safeValue(cheque.getDrawerName()));

        builder
            .append("\nPayee Account: ")
            .append(safeValue(cheque.getPayeeAccountNumber()));

        builder
            .append("\nPayee Name: ")
            .append(safeValue(cheque.getPayeeName()));

        builder
            .append("\nAmount: ")
            .append(
                cheque.getAmount() == null
                    ? ""
                    : cheque.getAmount().toPlainString()
            );

        builder
            .append("\nCheque Date: ")
            .append(
                cheque.getChequeDate() == null
                    ? ""
                    : cheque.getChequeDate().toString()
            );

        builder
            .append("\nBank Code: ")
            .append(safeValue(cheque.getBankCode()));

        builder
            .append("\nCity Code: ")
            .append(safeValue(cheque.getCityCode()));
    }

    // return safe value
    private String safeValue(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    // create batch folder
    private File createBatchFolder() throws Exception {
        String tempDirectory =
            System.getProperty("java.io.tmpdir");

        File rootFolder =
            new File(
                tempDirectory,
                "cts-capture-batches"
            );

        if (!rootFolder.exists() && !rootFolder.mkdirs()) {
            throw new IllegalStateException(
                "Unable to create capture batch root folder:\n" +
                rootFolder.getAbsolutePath()
            );
        }

        String uniqueFolderName =
            "BATCH-" + UUID.randomUUID();

        File batchFolder =
            new File(
                rootFolder,
                uniqueFolderName
            );

        if (!batchFolder.mkdirs()) {
            throw new IllegalStateException(
                "Unable to create batch upload folder:\n" +
                batchFolder.getAbsolutePath()
            );
        }

        return batchFolder;
    }

    // save uploaded files
    private File saveUploadedFiles(
        List<Media> uploadedFiles,
        File batchFolder) throws Exception {

        File xmlFile = null;
        int xmlCount = 0;

        for (Media media : uploadedFiles) {
            if (media == null) {
                continue;
            }

            String originalName = media.getName();

            if (originalName == null ||
                originalName.trim().isEmpty()) {

                continue;
            }

            String fileName =
                new File(originalName).getName();

            if (fileName.isEmpty()) {
                continue;
            }

            File targetFile =
                new File(
                    batchFolder,
                    fileName
                );

            String canonicalFolder =
                batchFolder.getCanonicalPath() +
                File.separator;

            String canonicalTarget =
                targetFile.getCanonicalPath();

            if (!canonicalTarget.startsWith(canonicalFolder)) {
                throw new IllegalArgumentException(
                    "Invalid uploaded file name: " +
                    fileName
                );
            }

            boolean isXml =
                fileName.toLowerCase().endsWith(".xml");

            if (isXml) {
                xmlCount++;

                if (xmlCount > 1) {
                    throw new IllegalArgumentException(
                        "Multiple XML files selected.\n\n" +
                        "Please select only one XML file."
                    );
                }

                xmlFile = targetFile;
            }

            writeMediaToFile(
                media,
                targetFile
            );

            System.out.println(
                "Uploaded file saved: " +
                targetFile.getAbsolutePath()
            );
        }

        if (xmlCount == 0) {
            throw new IllegalArgumentException(
                "No XML file selected."
            );
        }

        return xmlFile;
    }

    // write uploaded media to file
    private void writeMediaToFile(
        Media media,
        File targetFile) throws Exception {

        if (!media.isBinary()) {
            String data = media.getStringData();

            if (data == null) {
                throw new IllegalArgumentException(
                    "Unable to read uploaded file: " +
                    media.getName()
                );
            }

            try (
                FileOutputStream output =
                    new FileOutputStream(targetFile)
            ) {
                output.write(
                    data.getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                    )
                );
            }

            return;
        }

        byte[] data = media.getByteData();

        if (data == null) {
            throw new IllegalArgumentException(
                "Unable to read uploaded image: " +
                media.getName()
            );
        }

        try (
            FileOutputStream output =
                new FileOutputStream(targetFile)
        ) {
            output.write(data);
        }
    }

    // delete batch folder
    private void deleteFolder(File folder) {
        if (folder == null || !folder.exists()) {
            return;
        }

        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
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

 
 // generate 8 character batch number
    private String generateBatchNumber() {
        return "OUT" + String.format("%05d",
            (int) (Math.random() * 100000));
    }
    // get captured batches
    public List<OutwardBatch> getCapturedBatches() {
        return dao.getCapturedBatches();
    }
}