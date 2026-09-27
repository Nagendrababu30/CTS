package com.cts.inward.file;

// Interface for inward directory file watcher
public interface IncomingFileWatcher {

    // Starts directory watching
    void startWatching();

    // Stops directory watching
    void stopWatching();
}
