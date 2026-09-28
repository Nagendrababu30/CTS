package com.cts.inward.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.cts.inward.config.FileConfiguration;
import com.cts.inward.dao.InwardFileDao;
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
    private final InwardFileDao inwardFileDao;

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
            InwardFileDao inwardFileDao) {

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
            InwardFileDao inwardFileDao) {

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

    private Path resolvePath(String filePath) {
        if (filePath == null || filePath.isBlank()) return null;
        Path path = Path.of(filePath);
        if (path.isAbsolute()) {
            return path.normalize();
        }

        if (Files.exists(path)) {
            return path.toAbsolutePath().normalize();
        }

        Path rootPath = fileConfiguration.getInwardRootPath();
        if (rootPath == null) {
            return path.toAbsolutePath().normalize();
        }

        String normalized = filePath.replace("\\", "/");
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        if (normalized.startsWith("src/main/webapp/")) {
            String afterWebapp = normalized.substring("src/main/webapp/".length());
            Path webAppRoot = rootPath.getParent();
            if (webAppRoot != null) {
                Path candidate = webAppRoot.resolve(afterWebapp);
                if (Files.exists(candidate)) {
                    return candidate.normalize();
                }
            }
            return Path.of(normalized).toAbsolutePath().normalize();
        }

        if (normalized.startsWith("inward-files/")) {
            String afterInward = normalized.substring("inward-files/".length());
            return rootPath.resolve(afterInward).normalize();
        }

        return rootPath.resolve(filePath).normalize();
    }

    @Override
    public void processFile(String filePath) {

        Path path = resolvePath(filePath);
        String resolvedFilePath = path != null ? path.toString() : filePath;

        try {
            if (path != null && Files.exists(path) && Files.size(path) == 0) {
                System.err.println("[FileProcessing] Skipping empty (0 bytes) file: " + resolvedFilePath);
                return;
            }
        } catch (IOException e) {
            System.err.println("[FileProcessing] Could not determine file size for: " + resolvedFilePath);
        }

        FileType fileType = resolveFileType(resolvedFilePath);

        // Move file to processing directory before parsing
        String processingFilePath = moveToProcessing(resolvedFilePath, fileType);

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
            throw new IllegalArgumentException(
                    "Unsupported file type: " + fileType);
        }
    }

    // Move file from incoming to processing and update stage to PROCESSING
    private String moveToProcessing(String filePath, FileType fileType) {

        try {

            Path source = resolvePath(filePath);

            Path targetDir = fileConfiguration
                    .getProcessingPath()
                    .resolve(fileType.name().toLowerCase());

            Files.createDirectories(targetDir);

            Path target = targetDir.resolve(source.getFileName());

            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

            System.out.println(
                    "[FileProcessing] Moved to processing: " + target);

            // Resolve file_id from incoming path and update stage to PROCESSING
            long fileId = inwardFileDao.getFileIdByPath(
                    normalizePathForDb(filePath));
            if (fileId <= 0 && source != null) {
                fileId = inwardFileDao.getFileIdByFileName(source.getFileName().toString());
            }
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

    // Move file from processing to archive directory after parsing succeeds
    private void moveToArchive(String filePath, FileType fileType) {

        try {

            Path source = resolvePath(filePath);

            Path targetDir = fileConfiguration
                    .getArchivePath()
                    .resolve(fileType.name().toLowerCase());

            Files.createDirectories(targetDir);

            Path target = targetDir.resolve(source.getFileName());

            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

            System.out.println(
                    "[FileProcessing] Archived: " + target);

            // Update inward_file_summary stage to ARCHIVE
            long fileId = inwardFileDao.getFileIdByPath(
                    normalizePathForDb(filePath));
            if (fileId <= 0 && source != null) {
                fileId = inwardFileDao.getFileIdByFileName(source.getFileName().toString());
            }
            if (fileId > 0) {
                fileSummaryService.updateFileStage(fileId, FileStage.ARCHIVE);
            }

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to move file to archive directory: "
                    + filePath, e);
        }
    }

    // Parses and persists PXF metadata and cheques
    private void processPxfFile(String filePath) {

        PxfParserResult result = pxfParser.parse(filePath);
        NpciBatchData batchData = result.getBatchData();
        List<NpciChequeData> chequeDataList = result.getChequeDataList();

        // Check if batch already exists in database to avoid duplicate records
        if (batchService.isBatchExists(batchData.getBatchId())) {
            System.out.println("[PXF] Batch " + batchData.getBatchId() + " already exists in DB. Skipping duplicate PXF file: " + filePath);
            return;
        }

        // Resolve actual file_id from inward_file to prevent foreign key violations
        String fileName = Path.of(filePath).getFileName().toString();
        long actualFileId = inwardFileDao.getFileIdByFileName(fileName);
        if (actualFileId > 0) {
            batchData.setFileId(actualFileId);
        }

        System.out.println("[PXF] Parsed batch: " + batchData.getBatchId()
                + ", cheques parsed: " + chequeDataList.size()
                + ", fileId: " + batchData.getFileId());

        batchService.saveBatch(batchData);

        System.out.println("[PXF] Batch saved: " + batchData.getBatchId());

        for (NpciChequeData chequeData : chequeDataList) {
            chequeService.saveCheque(chequeData);
            System.out.println("[PXF] Cheque saved: " + chequeData.getChequeNumber());
        }

        System.out.println("[PXF] All cheques saved for batch: " + batchData.getBatchId());
    }

    // Extracts cheque images from PIBF container and links them to cheques
    private void processPibfFile(String filePath) {

        // Extract batch name from filename and resolve batch_id from DB
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

        // Check if cheque images for this batch already exist
        if (!cheques.isEmpty() && chequeImageService.getImageByChequeNumber(cheques.get(0).getChequeNumber()) != null) {
            System.out.println("[PIBF] Cheque images already saved for batch " + batchId + ". Skipping duplicate PIBF file: " + filePath);
            return;
        }

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

    // Parses OCR metadata and links to inward cheques
    private void processOcrFile(String filePath) {

        OcrBatchData batchData =
                ocrParser.parse(filePath);

        // Check if OCR batch already exists in database
        if (ocrBatchService.isBatchExists(batchData.getBatchId())) {
            System.out.println("[OCR] OCR batch " + batchData.getBatchId() + " already exists in DB. Skipping duplicate OCR file: " + filePath);
            return;
        }

        // Resolve actual file_id from inward_file table for OCR
        String fileName = Path.of(filePath).getFileName().toString();
        long actualFileId = inwardFileDao.getFileIdByFileName(fileName);
        if (actualFileId > 0) {
            batchData.setFileId(actualFileId);
        }

        // Save batch and use generated ocr_batch_id for cheques
        long generatedOcrBatchId =
                ocrBatchService.saveBatch(batchData);

        for (OcrChequeData chequeData :
                batchData.getCheques()) {

            chequeData.setInwardChequeId(0);
            chequeData.setBatchId(generatedOcrBatchId);

            ocrChequeService.saveCheque(chequeData);
        }

        // Link ocr_cheque_data to inward_cheque via cheque_number
        ocrChequeService.linkInwardChequeIds(generatedOcrBatchId);
    }

    // Converts Windows backslash paths to forward slashes for DB matching
    private String normalizePathForDb(String filePath) {
        return filePath.replace("\\", "/");
    }

    // Extracts batch ID from supported filename patterns
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