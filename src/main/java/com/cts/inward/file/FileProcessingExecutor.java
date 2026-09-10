package com.cts.inward.file;

public interface FileProcessingExecutor {

	void submit(String filePath);

	/**
	 * Submits a group of files belonging to the same batch
	 * to be processed sequentially on a single thread.
	 *
	 * Files must be passed in dependency order: PXF → OCR → PIBF.
	 * This guarantees FK dependencies are satisfied before dependent files run.
	 */
	void submitBatch(java.util.List<String> orderedFilePaths);

    void shutdown();
	
}
