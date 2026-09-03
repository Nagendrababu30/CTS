package com.cts.inward.file;

public interface FileProcessingExecutor {

	void submit(String filePath);

    void shutdown();
	
}
