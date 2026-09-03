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

        registerDirectory(fileConfiguration.getIncomingPath()
                .resolve("pxf"));

        registerDirectory(fileConfiguration.getIncomingPath()
                .resolve("pibf"));

        registerDirectory(fileConfiguration.getIncomingPath()
                .resolve("ocr"));

        Thread watcherThread = new Thread(
                this::watchIncomingDirectories,
                "cts-inward-file-watcher");

        watcherThread.setDaemon(false);
        watcherThread.start();
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