package com.cts.inward.file;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.cts.inward.service.InwardIngestionService;

public class FileProcessingExecutorImpl
        implements FileProcessingExecutor {

    private final ExecutorService executorService;
    private final InwardIngestionService inwardIngestionService;

    private FileProcessingExecutorImpl(
            ExecutorService executorService,
            InwardIngestionService inwardIngestionService) {

        this.executorService =
                executorService;

        this.inwardIngestionService =
                inwardIngestionService;
    }

    public static FileProcessingExecutorImpl of(
            int threadPoolSize,
            InwardIngestionService inwardIngestionService) {

        ExecutorService executorService =
                Executors.newFixedThreadPool(
                        threadPoolSize);

        return new FileProcessingExecutorImpl(
                executorService,
                inwardIngestionService);
    }

    @Override
    public void submit(String filePath) {

        executorService.submit(() -> {
            try {
                inwardIngestionService.processFile(filePath);
            } catch (Exception e) {
                System.err.println(
                        "[FileProcessingExecutor] ERROR processing file: "
                        + filePath);
                e.printStackTrace();
            }
        });
    }

    @Override
    public void submitBatch(java.util.List<String> orderedFilePaths) {

        /*
         * All files of a batch run sequentially on ONE thread.
         * Order must be: PXF → OCR → PIBF
         *
         * This guarantees:
         *  - PXF creates inward_batch + inward_cheque rows
         *  - OCR can safely FK-reference inward_batch
         *  - PIBF can safely look up inward_cheque rows
         */
        executorService.submit(() -> {
            for (String filePath : orderedFilePaths) {
                try {
                    inwardIngestionService.processFile(filePath);
                } catch (Exception e) {
                    System.err.println(
                            "[FileProcessingExecutor] ERROR processing file: "
                            + filePath);
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void shutdown() {

        executorService.shutdown();
    }
}