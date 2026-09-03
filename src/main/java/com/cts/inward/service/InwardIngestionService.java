package com.cts.inward.service;

public interface InwardIngestionService {

	void processIncomingFiles();

	void processFile(String filePath);

	void processSessionFiles();
	
}
