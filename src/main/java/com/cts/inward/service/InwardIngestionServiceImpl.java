package com.cts.inward.service;

//import com.cts.inward.service.FileProcessingService;
//import com.cts.inward.service.InwardIngestionService;

public class InwardIngestionServiceImpl
        implements InwardIngestionService {

    private final FileProcessingService fileProcessingService;

    private InwardIngestionServiceImpl(
            FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    public static InwardIngestionServiceImpl of(
            FileProcessingService fileProcessingService) {

        return new InwardIngestionServiceImpl(
                fileProcessingService);
    }

    @Override
    public void processFile(String filePath) {

        fileProcessingService.processFile(filePath);
    }

    @Override
    public void processIncomingFiles() {
        // Used for bulk/session processing if required.
    }

    @Override
    public void processSessionFiles() {
        // Used for session-based processing if required.
    }
}