package com.cts.inward.service;

// Service interface for processing inward clearing files
public interface FileProcessingService {

    // Resolves file type and delegates processing to specific handlers
    void processFile(String filePath);
}