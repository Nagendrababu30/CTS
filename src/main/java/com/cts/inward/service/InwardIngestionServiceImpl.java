package com.cts.inward.service;

import java.util.List;
import java.util.Map;

import com.cts.inward.enums.FileType;
import com.cts.inward.file.IncomingFileWatcher;
import com.cts.inward.model.InwardFile;

public class InwardIngestionServiceImpl
        implements InwardIngestionService {

    private final FileProcessingService     fileProcessingService;
    private final CHIFileService            chiFileService;
    private final InwardSessionFileService  inwardSessionFileService;
    private final IncomingFileWatcher       incomingFileWatcher;

    /* ------------------------------------------------------------------ */
    /* Constructor — full dependencies for processSessionFiles()           */
    /* ------------------------------------------------------------------ */

    private InwardIngestionServiceImpl(
            FileProcessingService     fileProcessingService,
            CHIFileService            chiFileService,
            InwardSessionFileService  inwardSessionFileService,
            IncomingFileWatcher       incomingFileWatcher) {

        this.fileProcessingService    = fileProcessingService;
        this.chiFileService           = chiFileService;
        this.inwardSessionFileService = inwardSessionFileService;
        this.incomingFileWatcher      = incomingFileWatcher;
    }

    /* ------------------------------------------------------------------ */
    /* Factory — full (used by SessionManagementController)                */
    /* ------------------------------------------------------------------ */

    public static InwardIngestionServiceImpl of(
            FileProcessingService     fileProcessingService,
            CHIFileService            chiFileService,
            InwardSessionFileService  inwardSessionFileService,
            IncomingFileWatcher       incomingFileWatcher) {

        return new InwardIngestionServiceImpl(
                fileProcessingService,
                chiFileService,
                inwardSessionFileService,
                incomingFileWatcher);
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

        /* Step 3 — start the file watcher                              */
        /* The watcher will detect the newly moved files (ENTRY_CREATE) */
        /* and submit each one to the FileProcessingExecutor            */
        incomingFileWatcher.startWatching();

        System.out.println(
                "[InwardIngestion] IncomingFileWatcher started. File processing in progress.");
    }

    /* ------------------------------------------------------------------ */
    /* Stub — bulk processing not needed for session-based flow            */
    /* ------------------------------------------------------------------ */

    @Override
    public void processIncomingFiles() {
        /* Used for bulk/session processing if required. */
    }
}
