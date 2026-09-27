package com.cts.inward.file;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

import com.cts.inward.service.FileProcessingService;


public class FileProcessingExecutorImpl
        implements FileProcessingExecutor {

    private final ExecutorService executorService;
    private final FileProcessingService fileProcessingService;

    private FileProcessingExecutorImpl(
            ExecutorService executorService,
            FileProcessingService fileProcessingService) {

        this.executorService =
                executorService;

        this.fileProcessingService = fileProcessingService;
    }

    public static FileProcessingExecutorImpl of(
            int threadPoolSize,
            FileProcessingService fileProcessingService) {

        ExecutorService executorService =
                Executors.newFixedThreadPool(
                        threadPoolSize);

        return new FileProcessingExecutorImpl(
                executorService,
                fileProcessingService);
    }

//    @Override
//    public void submit(String filePath) {
//
//        executorService.submit(() -> {
//            try {
//            	fileProcessingService.processFile(filePath);
//            } catch (Exception e) {
//                System.err.println(
//                        "[FileProcessingExecutor] ERROR processing file: "
//                        + filePath);
//                e.printStackTrace();
//            }
//        });
//    }

    @Override
    public void submitBatch(List<String> orderedFilePaths) {

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
            boolean pxfFailed = false;
            for (String filePath : orderedFilePaths) {
                if (pxfFailed) {
                    System.err.println(
                            "[FileProcessingExecutor] Skipping " + filePath
                            + " because PXF failed for this batch.");
                    break;
                }
                try {
                	fileProcessingService.processFile(filePath);
                } catch (Exception e) {
                    System.err.println(
                            "[FileProcessingExecutor] ERROR processing file: "
                            + filePath);
                    e.printStackTrace();

                    boolean isPxf = filePath.toLowerCase().contains("pxf");
                    if (isPxf) {
                        pxfFailed = true;
                        System.err.println(
                                "[FileProcessingExecutor] PXF failed. Aborting remaining files for this batch.");
                        break;
                    } else {
                        System.err.println(
                                "[FileProcessingExecutor] Non-PXF file failed. Continuing with remaining batch files.");
                    }
                }
            }
        });
    }

    @Override
    public void shutdown() {

        executorService.shutdown();
    }
}