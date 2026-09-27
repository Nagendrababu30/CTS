package com.cts.inward.file;

import java.util.List;

public interface FileProcessingExecutor {

    // Submits batch files sequentially on a single thread in dependency order: PXF -> OCR -> PIBF
    void submitBatch(List<String> orderedFilePaths);

    // Shuts down the thread pool
    void shutdown();
}
