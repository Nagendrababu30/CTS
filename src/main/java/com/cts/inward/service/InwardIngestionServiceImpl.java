package com.cts.inward.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cts.inward.config.FileConfiguration;
import com.cts.inward.enums.FileType;
import com.cts.inward.file.FileProcessingExecutor;
import com.cts.inward.model.InwardFile;

public class InwardIngestionServiceImpl
        implements InwardIngestionService {

    private final FileProcessingService     fileProcessingService;
    private final CHIFileService            chiFileService;
    private final InwardSessionFileService  inwardSessionFileService;
    private final FileProcessingExecutor    fileProcessingExecutor;
    private final FileConfiguration         fileConfiguration;

    /* ------------------------------------------------------------------ */
    /* Constructor — full dependencies for processSessionFiles()           */
    /* ------------------------------------------------------------------ */

    private InwardIngestionServiceImpl(
            FileProcessingService     fileProcessingService,
            CHIFileService            chiFileService,
            InwardSessionFileService  inwardSessionFileService,
            FileProcessingExecutor    fileProcessingExecutor,
            FileConfiguration         fileConfiguration) {

        this.fileProcessingService    = fileProcessingService;
        this.chiFileService           = chiFileService;
        this.inwardSessionFileService = inwardSessionFileService;
        this.fileProcessingExecutor   = fileProcessingExecutor;
        this.fileConfiguration        = fileConfiguration;
    }

    /* ------------------------------------------------------------------ */
    /* Factory — full (used by SessionManagementController)                */
    /* ------------------------------------------------------------------ */

    public static InwardIngestionServiceImpl of(
            FileProcessingService     fileProcessingService,
            CHIFileService            chiFileService,
            InwardSessionFileService  inwardSessionFileService,
            FileProcessingExecutor    fileProcessingExecutor,
            FileConfiguration         fileConfiguration) {

        return new InwardIngestionServiceImpl(
                fileProcessingService,
                chiFileService,
                inwardSessionFileService,
                fileProcessingExecutor,
                fileConfiguration);
    }

    /* ------------------------------------------------------------------ */
    /* Factory — lightweight (used internally by FileProcessingExecutor)   */
    /* Only processFile() is needed in that context.                       */
    /* ------------------------------------------------------------------ */

    public static InwardIngestionServiceImpl of(
            FileProcessingService fileProcessingService) {

        return new InwardIngestionServiceImpl(
                fileProcessingService,
                null,
                null,
                null,
                null);
    }


    /* ------------------------------------------------------------------ */
    /* Called by FileProcessingExecutor for each file detected by watcher  */
    /* ------------------------------------------------------------------ */

    @Override
    public void processFile(String filePath) {
        fileProcessingService.processFile(filePath);
    }

    /* ------------------------------------------------------------------ */
    /* Called when Admin ends the clearing session                         */
    /*                                                                     */
    /* Flow:                                                               */
    /*  1. Get CHI file paths from DB (inward_file table)                  */
    /*  2. Move each file to incoming/{pxf|pibf|ocr}/                      */
    /*     and update file_summary stage to INCOMING                       */
    /*  3. Start the IncomingFileWatcher                                   */
    /*     → NIO WatchService detects the moved files                      */
    /*     → FileProcessingExecutor.submit(filePath) per file              */
    /*     → processFile(filePath) called in thread pool                   */
    /* ------------------------------------------------------------------ */

    @Override
    public void processSessionFiles() {

        /* Step 1 — get all CHI files from DB grouped by type */
        Map<FileType, List<InwardFile>> filesByType =
                chiFileService.getCHIFilePaths();

        if (filesByType == null || filesByType.isEmpty()) {
            System.out.println(
                    "[InwardIngestion] No CHI files found in DB. Nothing to process.");
            return;
        }

        /* Step 2 — move each file to incoming/{type}/ directory */
        inwardSessionFileService.moveFilesToIncoming(filesByType);

        System.out.println(
                "[InwardIngestion] All CHI files moved to incoming directories.");

        /* Step 3 — submit moved files ordered by batch (PXF -> OCR -> PIBF) */
        processIncomingFiles();

        System.out.println(
                "[InwardIngestion] Batches submitted for processing.");
    }


    /* ------------------------------------------------------------------ */
    /* Stub — bulk processing not needed for session-based flow            */
    /* ------------------------------------------------------------------ */

    @Override
    public void processIncomingFiles() {

        if (fileConfiguration == null || fileProcessingExecutor == null) {
            return;
        }

        Map<String, Map<String, String>> batchMap = new LinkedHashMap<>();

        collectFiles(fileConfiguration.getIncomingPath().resolve("pxf"), "pxf", batchMap);
        collectFiles(fileConfiguration.getIncomingPath().resolve("ocr"), "ocr", batchMap);
        collectFiles(fileConfiguration.getIncomingPath().resolve("pibf"), "pibf", batchMap);

        for (Map.Entry<String, Map<String, String>> entry : batchMap.entrySet()) {
            String batchName = entry.getKey();
            Map<String, String> filesByType = entry.getValue();

            List<String> ordered = new ArrayList<>();
            if (filesByType.containsKey("pxf")) {
                ordered.add(filesByType.get("pxf"));
            }
            if (filesByType.containsKey("ocr")) {
                ordered.add(filesByType.get("ocr"));
            }
            if (filesByType.containsKey("pibf")) {
                ordered.add(filesByType.get("pibf"));
            }

            System.out.println(
                    "[InwardIngestion] Submitting batch: " + batchName + " files in order: " + ordered);

            fileProcessingExecutor.submitBatch(ordered);
        }
    }

    private void collectFiles(Path directory, String fileType, Map<String, Map<String, String>> batchMap) {
        try {
            if (!Files.exists(directory)) {
                return;
            }
            Files.list(directory)
                    .filter(Files::isRegularFile)
                    .forEach(filePath -> {
                        String batchName = extractBatchName(filePath.getFileName().toString());
                        batchMap.computeIfAbsent(batchName, k -> new LinkedHashMap<>())
                                .put(fileType, filePath.toString());
                    });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan incoming directory: " + directory, e);
        }
    }

    private String extractBatchName(String fileName) {
        String nameWithoutExtension = fileName.contains(".")
                ? fileName.substring(0, fileName.lastIndexOf('.'))
                : fileName;

        String[] parts = nameWithoutExtension.split("_");
        for (String part : parts) {
            if (part.toUpperCase().startsWith("BATCH")) {
                return part.toUpperCase();
            }
        }
        return nameWithoutExtension.toUpperCase();
    }

}
