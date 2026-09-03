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

        executorService.submit(
                () -> inwardIngestionService
                        .processFile(filePath));
    }

    @Override
    public void shutdown() {

        executorService.shutdown();
    }
}