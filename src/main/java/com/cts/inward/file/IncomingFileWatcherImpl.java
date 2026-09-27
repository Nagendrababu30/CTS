package com.cts.inward.file;

// Legacy file watcher superseded by InwardIngestionService
public class IncomingFileWatcherImpl implements IncomingFileWatcher {

    public static IncomingFileWatcherImpl of() {
        return new IncomingFileWatcherImpl();
    }

    @Override
    public void startWatching() {
        // File ingestion is handled on-demand by InwardIngestionService
    }

    @Override
    public void stopWatching() {
        // File ingestion is handled on-demand by InwardIngestionService
    }
}