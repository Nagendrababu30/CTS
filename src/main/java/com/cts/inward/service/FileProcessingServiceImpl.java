package com.cts.inward.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.cts.inward.config.FileConfiguration;
import com.cts.inward.dto.PxfParserResult;
import com.cts.inward.enums.FileStage;
import com.cts.inward.enums.FileType;
import com.cts.inward.model.ChequeImage;
import com.cts.inward.model.ChequeImagePaths;
import com.cts.inward.model.InwardCheque;
import com.cts.inward.model.NpciBatchData;
import com.cts.inward.model.NpciChequeData;
import com.cts.inward.model.OcrBatchData;
import com.cts.inward.model.OcrChequeData;
import com.cts.inward.model.PibfImageData;
import com.cts.inward.parser.OcrParser;
import com.cts.inward.parser.PibfProcessor;
import com.cts.inward.parser.PxfParser;
import com.cts.inward.service.BatchService;
import com.cts.inward.service.ChequeImageService;
import com.cts.inward.service.ChequeService;
import com.cts.inward.service.FileProcessingService;
import com.cts.inward.service.ImageService;
import com.cts.inward.service.OcrBatchService;
import com.cts.inward.service.OcrChequeService;

public class FileProcessingServiceImpl
        implements FileProcessingService {

    private final PxfParser pxfParser;
    private final PibfProcessor pibfProcessor;
    private final OcrParser ocrParser;

    private final FileConfiguration fileConfiguration;

    private final BatchService batchService;
    private final ChequeService chequeService;

    private final OcrBatchService ocrBatchService;
    private final OcrChequeService ocrChequeService;

    private final ImageService imageService;
    private final ChequeImageService chequeImageService;

    private final FileSummaryService fileSummaryService;
    private final com.cts.inward.dao.InwardFileDao inwardFileDao;

    private FileProcessingServiceImpl(
            FileConfiguration fileConfiguration,
            PxfParser pxfParser,
            PibfProcessor pibfProcessor,
            OcrParser ocrParser,
            BatchService batchService,
            ChequeService chequeService,
            OcrBatchService ocrBatchService,
            OcrChequeService ocrChequeService,
            ImageService imageService,
            ChequeImageService chequeImageService,
            FileSummaryService fileSummaryService,
            com.cts.inward.dao.InwardFileDao inwardFileDao) {

        this.fileConfiguration = fileConfiguration;
        this.pxfParser = pxfParser;
        this.pibfProcessor = pibfProcessor;
        this.ocrParser = ocrParser;
        this.batchService = batchService;
        this.chequeService = chequeService;
        this.ocrBatchService = ocrBatchService;
        this.ocrChequeService = ocrChequeService;
        this.imageService = imageService;
        this.chequeImageService = chequeImageService;
        this.fileSummaryService = fileSummaryService;
        this.inwardFileDao = inwardFileDao;
    }

    public static FileProcessingServiceImpl of(
            FileConfiguration fileConfiguration,
            PxfParser pxfParser,
            PibfProcessor pibfProcessor,
            OcrParser ocrParser,
            BatchService batchService,
            ChequeService chequeService,
            OcrBatchService ocrBatchService,
            OcrChequeService ocrChequeService,
            ImageService imageService,
            ChequeImageService chequeImageService,
            FileSummaryService fileSummaryService,
            com.cts.inward.dao.InwardFileDao inwardFileDao) {

        return new FileProcessingServiceImpl(
                fileConfiguration,
                pxfParser,
                pibfProcessor,
                ocrParser,
                batchService,
                chequeService,
                ocrBatchService,
                ocrChequeService,
                imageService,
                chequeImageService,
                fileSummaryService,
                inwardFileDao);
    }

    @Override
    public void processFile(String filePath) {

        FileType fileType = resolveFileType(filePath);

        /* CATCH 1 — move to processing/{type}/ before parsing */
        String processingFilePath = moveToProcessing(filePath, fileType);

        switch (fileType) {

        case PXF:
            processPxfFile(processingFilePath);
            moveToArchive(processingFilePath, fileType);
            break;

        case PIBF:
            processPibfFile(processingFilePath);
            moveToArchive(processingFilePath, fileType);
            break;

        case OCR:
            processOcrFile(processingFilePath);
            moveToArchive(processingFilePath, fileType);
            break;

        default:
            throw new IllegalStateException(
                    "Unsupported file type: "
                            + fileType);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Move file from incoming/{type}/ to processing/{type}/               */
    /* Returns the new file path in the processing directory.              */
    /* ------------------------------------------------------------------ */

    private String moveToProcessing(String filePath, FileType fileType) {

        try {

            Path source = Path.of(filePath);

            Path targetDir = fileConfiguration
                    .getProcessingPath()
                    .resolve(fileType.name().toLowerCase());

            Files.createDirectories(targetDir);

            Path target = targetDir.resolve(source.getFileName());

            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

            System.out.println(
                    "[FileProcessing] Moved to processing: " + target);

            /*
             * Resolve file_id from the original incoming path
             * and update inward_file_summary stage to PROCESSING.
             */
            long fileId = inwardFileDao.getFileIdByPath(
                    normalizePathForDb(filePath));
            if (fileId > 0) {
                fileSummaryService.updateFileStage(fileId, FileStage.PROCESSING);
            }

            return target.toString();

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to move file to processing directory: "
                    + filePath, e);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Move file from processing/{type}/ to archive/{type}/ after parsing  */
    /* Called only after parse succeeds — failed files stay in processing  */
    /* Image files are NOT archived — they stay in images/                 */
    /* ------------------------------------------------------------------ */

    private void moveToArchive(String filePath, FileType fileType) {

        try {

            Path source = Path.of(filePath);

            Path targetDir = fileConfiguration
                    .getArchivePath()
                    .resolve(fileType.name().toLowerCase());

            Files.createDirectories(targetDir);

            Path target = targetDir.resolve(source.getFileName());

            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

            System.out.println(
                    "[FileProcessing] Archived: " + target);

            /*
             * Update inward_file_summary stage to ARCHIVE.
             */
            long fileId = inwardFileDao.getFileIdByPath(
                    normalizePathForDb(filePath));
            if (fileId > 0) {
                fileSummaryService.updateFileStage(fileId, FileStage.ARCHIVE);
            }

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to move file to archive directory: "
                    + filePath, e);
        }
    }

    @Override
    public void processPxfFile(String filePath) {

        PxfParserResult result = pxfParser.parse(filePath);
        NpciBatchData batchData = result.getBatchData();
        List<NpciChequeData> chequeDataList = result.getChequeDataList();

        System.out.println("[PXF] Parsed batch: " + batchData.getBatchId()
                + ", cheques parsed: " + chequeDataList.size());

        batchService.saveBatch(batchData);

        System.out.println("[PXF] Batch saved: " + batchData.getBatchId());

        for (NpciChequeData chequeData : chequeDataList) {
            chequeService.saveCheque(chequeData);
            System.out.println("[PXF] Cheque saved: " + chequeData.getChequeNumber());
        }

        System.out.println("[PXF] All cheques saved for batch: " + batchData.getBatchId());
    }

    @Override
    public void processPibfFile(String filePath) {

        /*
         * Extract batch name from filename e.g. "wPIBF_BATCH001_01.img" → "BATCH001"
         * Then look up the numeric batch_id from DB by matching
         * against the PXF file name in inward_file.
         */
        String batchName = extractBatchId(filePath);

        long batchId = batchService.getBatchIdByFileName(batchName);

        if (batchId <= 0) {
            throw new IllegalStateException(
                    "Could not resolve batch_id for PIBF file: "
                    + filePath + " (batchName=" + batchName + ")");
        }

        List<PibfImageData> imageDataList =
                pibfProcessor.extractImages(filePath);

        List<InwardCheque> cheques =
                chequeService.getAllChequesForBatch(batchId);

        if (imageDataList.size() != cheques.size()) {
            System.err.println(
                    "[PIBF] Image count mismatch — "
                    + "PIBF images: " + imageDataList.size()
                    + ", DB cheques: " + cheques.size()
                    + ", batchId: " + batchId);
            throw new IllegalStateException(
                    "PIBF image count does not match "
                    + "cheque count for batch: "
                    + batchId);
        }

        for (int index = 0; index < cheques.size(); index++) {

            InwardCheque cheque = cheques.get(index);
            PibfImageData imageData = imageDataList.get(index);
            String chequeNumber = cheque.getChequeNumber();

            ChequeImagePaths imagePaths =
                    imageService.saveChequeImages(
                            String.valueOf(batchId),
                            chequeNumber,
                            imageData);

            ChequeImage chequeImage =
                    ChequeImage.of(
                            chequeNumber,
                            imagePaths.getFrontImagePath(),
                            imagePaths.getBackImagePath());

            chequeImageService.saveImage(chequeImage);
        }
    }

    @Override
    public void processOcrFile(String filePath) {

        OcrBatchData batchData =
                ocrParser.parse(filePath);

        /*
         * saveBatch() returns the generated ocr_batch_id via RETURNING.
         * Each cheque must use this ID — NOT the inward batch_id —
         * because ocr_cheque_data.ocr_batch_id is a FK to ocr_batch.ocr_batch_id.
         */
        long generatedOcrBatchId =
                ocrBatchService.saveBatch(batchData);

        for (OcrChequeData chequeData :
                batchData.getCheques()) {

            chequeData.setInwardChequeId(0);
            chequeData.setBatchId(generatedOcrBatchId);

            ocrChequeService.saveCheque(chequeData);
        }

        /*
         * Link each ocr_cheque_data row to its corresponding
         * inward_cheque row via inward_cheque_id.
         * Uses cheque_number as the bridge — single UPDATE for the batch.
         */
        ocrChequeService.linkInwardChequeIds(generatedOcrBatchId);
    }

    /*
     * Converts Windows backslash paths to forward slashes
     * to match the file_path values stored in the DB.
     */
    private String normalizePathForDb(String filePath) {
        return filePath.replace("\\", "/");
    }

    private String extractBatchId(String filePath) {

        Path file = Path.of(filePath);
        String fileName = file.getFileName().toString();

        /*
         * Supported filename formats:
         *
         * wPIBF_BATCH001_01.img  → parts[1] = BATCH001
         * BATCH001.img           → strip extension = BATCH001
         * BATCH001_01.img        → parts[0] = BATCH001
         */
        String nameWithoutExtension = fileName.contains(".")
                ? fileName.substring(0, fileName.lastIndexOf('.'))
                : fileName;

        String[] parts = nameWithoutExtension.split("_");

        if (parts.length >= 3) {
            // wPIBF_BATCH001_01 → parts[1]
            return parts[1];
        }

        if (parts.length == 2) {
            // BATCH001_01 → parts[0]
            return parts[0];
        }

        // BATCH001 → nameWithoutExtension directly
        return nameWithoutExtension;
    }

    private FileType resolveFileType(
            String filePath) {

        Path file =
                Path.of(filePath);

        Path parentDirectory =
                file.getParent();

        if (parentDirectory == null) {

            throw new IllegalArgumentException(
                    "File parent directory not found: "
                            + filePath);
        }

        String directoryName =
                parentDirectory
                        .getFileName()
                        .toString()
                        .toLowerCase();

        switch (directoryName) {

        case "pxf":
            return FileType.PXF;

        case "pibf":
            return FileType.PIBF;

        case "ocr":
            return FileType.OCR;

        default:
            throw new IllegalArgumentException(
                    "Unable to determine file type from path: "
                            + filePath);
        }
    }
}