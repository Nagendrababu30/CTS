package com.cts.inward.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.cts.inward.config.FileConfiguration;

public class IncomingFileWatcherImpl implements IncomingFileWatcher {

    private final FileConfiguration fileConfiguration;
    private final WatchService watchService;
    private final FileProcessingExecutor fileProcessingExecutor;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private IncomingFileWatcherImpl(
            FileConfiguration fileConfiguration,
            WatchService watchService,
            FileProcessingExecutor fileProcessingExecutor) {

        this.fileConfiguration = fileConfiguration;
        this.watchService = watchService;
        this.fileProcessingExecutor = fileProcessingExecutor;
    }

    public static IncomingFileWatcherImpl of(
            FileConfiguration fileConfiguration,
            WatchService watchService,
            FileProcessingExecutor fileProcessingExecutor) {

        return new IncomingFileWatcherImpl(
                fileConfiguration,
                watchService,
                fileProcessingExecutor);
    }

    @Override
    public void startWatching() {

        if (!running.compareAndSet(false, true)) {
            return;
        }

        registerDirectory(fileConfiguration.getIncomingPath().resolve("pxf"));
        registerDirectory(fileConfiguration.getIncomingPath().resolve("pibf"));
        registerDirectory(fileConfiguration.getIncomingPath().resolve("ocr"));

        /*
         * Files were moved to incoming/ BEFORE the watcher registered.
         * Group them by batch name and submit each batch as one ordered
         * task: PXF → OCR → PIBF on a single thread.
         */
        submitExistingFilesOrdered();

        Thread watcherThread = new Thread(
                this::watchIncomingDirectories,
                "cts-inward-file-watcher");

        watcherThread.setDaemon(false);
        watcherThread.start();
    }

    /*
     * Groups all files in incoming/{pxf,ocr,pibf}/ by batch name,
     * then submits each batch group as one ordered task to the executor.
     *
     * Batch name extraction:
     *   BATCH001.xml          → BATCH001
     *   BATCH001_OCR.xml      → BATCH001
     *   wPIBF_BATCH001_01.img → BATCH001
     */
    private void submitExistingFilesOrdered() {

        java.util.Map<String, java.util.Map<String, String>> batchMap =
                new java.util.LinkedHashMap<>();

        collectFiles(
                fileConfiguration.getIncomingPath().resolve("pxf"),
                "pxf",
                batchMap);

        collectFiles(
                fileConfiguration.getIncomingPath().resolve("ocr"),
                "ocr",
                batchMap);

        collectFiles(
                fileConfiguration.getIncomingPath().resolve("pibf"),
                "pibf",
                batchMap);

        for (java.util.Map.Entry<String, java.util.Map<String, String>> entry
                : batchMap.entrySet()) {

            String batchName = entry.getKey();
            java.util.Map<String, String> filesByType = entry.getValue();

            java.util.List<String> ordered = new java.util.ArrayList<>();

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
                    "[FileWatcher] Submitting batch: "
                    + batchName
                    + " files in order: "
                    + ordered);

            fileProcessingExecutor.submitBatch(ordered);
        }
    }

    private void collectFiles(
            Path directory,
            String fileType,
            java.util.Map<String, java.util.Map<String, String>> batchMap) {

        try {
            if (!Files.exists(directory)) {
                return;
            }

            Files.list(directory)
                    .filter(Files::isRegularFile)
                    .forEach(filePath -> {

                        String batchName =
                                extractBatchName(
                                        filePath.getFileName().toString());

                        batchMap
                                .computeIfAbsent(
                                        batchName,
                                        k -> new java.util.LinkedHashMap<>())
                                .put(fileType, filePath.toString());
                    });

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to scan incoming directory: " + directory, e);
        }
    }

    /*
     * Extracts the batch name token from a filename.
     *
     * Examples:
     *   BATCH001.xml          → BATCH001
     *   BATCH001_OCR.xml      → BATCH001
     *   wPIBF_BATCH001_01.img → BATCH001
     */
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

    @Override
    public void stopWatching() {

        if (!running.compareAndSet(true, false)) {
            return;
        }

        try {
            watchService.close();
        } catch (IOException e) {
            // Log using your logging framework later.
        }

        fileProcessingExecutor.shutdown();
    }

    private void watchIncomingDirectories() {

        while (running.get()) {

            WatchKey watchKey;

            try {
                watchKey = watchService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IllegalStateException e) {
                break;
            }

            Path watchedDirectory =
                    (Path) watchKey.watchable();

            List<WatchEvent<?>> events =
                    watchKey.pollEvents();

            for (WatchEvent<?> event : events) {

                WatchEvent.Kind<?> kind =
                        event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                if (kind != StandardWatchEventKinds.ENTRY_CREATE) {
                    continue;
                }

//                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent =
                        (WatchEvent<Path>) event;

                Path fileName = pathEvent.context();

                Path filePath =
                        watchedDirectory.resolve(fileName);

                if (!Files.isRegularFile(filePath)) {
                    continue;
                }

                fileProcessingExecutor.submit(
                        filePath.toString());
            }

            boolean valid = watchKey.reset();

            if (!valid) {
                break;
            }
        }
    }

    private void registerDirectory(Path directory) {

        try {

            Files.createDirectories(directory);

            directory.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE);

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to register incoming directory: "
                            + directory,
                    e);
        }
    }
}