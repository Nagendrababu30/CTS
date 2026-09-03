package com.cts.inward.service;

public interface FileProcessingService {

    void processFile(String filePath);

    void processPxfFile(String filePath);

    void processPibfFile(String filePath);

    void processOcrFile(String filePath);
}