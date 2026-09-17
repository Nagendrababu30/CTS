package com.iispl.cts.service.outward;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.service.outward.ChequeProcessingTask.ProcessingListener;

/**
 * Parallel processing service for Capture Operator.
 *
 * IMPORTANT:
 *
 * 1. Does NOT access the database.
 * 2. Does NOT modify DAO logic.
 * 3. Does NOT change duplicate validation.
 * 4. Does NOT change XML parsing.
 * 5. Does NOT change cheque data.
 *
 * It only performs independent file/image processing
 * using multiple worker threads.
 */
public class ParallelChequeProcessingService {

    // =========================================================
    // PROCESSING LISTENER
    // =========================================================

    private ProcessingListener processingListener;

    /*
     * Number of parallel workers.
     *
     * Four threads are enough for a live demonstration
     * and avoid creating excessive threads.
     */
    private static final int WORKER_COUNT = 4;


    // =========================================================
    // PROCESS
    // =========================================================

    public List<OutwardCheque> processCheques(
            List<OutwardCheque> cheques,
            File batchFolder) {

        /*
         * IMPORTANT:
         *
         * Use the listener that was registered through
         * setProcessingListener().
         *
         * Previously this was passing null, which meant
         * the controller could not receive live updates.
         */
        return processCheques(
                cheques,
                batchFolder,
                processingListener);
    }


    // =========================================================
    // PROCESS WITH LISTENER
    // =========================================================

    public List<OutwardCheque> processCheques(
            List<OutwardCheque> cheques,
            File batchFolder,
            ProcessingListener listener) {

        if (cheques == null ||
                cheques.isEmpty()) {

            return cheques;
        }


        if (batchFolder == null ||
                !batchFolder.exists() ||
                !batchFolder.isDirectory()) {

            /*
             * Do not change existing functionality.
             *
             * If the parallel processing folder is unavailable,
             * return the original cheque list.
             */
            return cheques;
        }


        // =====================================================
        // START LOG
        // =====================================================

        System.out.println();

        System.out.println(
                "================================================");

        System.out.println(
                "PARALLEL CHEQUE PROCESSING STARTED");

        System.out.println(
                "Total Cheques : "
                        + cheques.size());

        System.out.println(
                "Worker Threads: "
                        + WORKER_COUNT);

        System.out.println(
                "================================================");


        // =====================================================
        // CREATE EXECUTOR
        // =====================================================

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        WORKER_COUNT);


        CompletionService<
                ChequeProcessingTask.ProcessingResult>
                completionService =
                new ExecutorCompletionService<>(
                        executor);


        /*
         * Create a copy so the original cheque order
         * can be preserved.
         */
        List<OutwardCheque> result =
                new ArrayList<>(
                        cheques);


        try {

            // =================================================
            // SUBMIT ALL CHEQUES
            // =================================================

            for (int i = 0;
                    i < cheques.size();
                    i++) {

                OutwardCheque cheque =
                        cheques.get(i);


                /*
                 * Each cheque gets its own task.
                 *
                 * The listener registered by the controller
                 * is passed to every task.
                 */
                ChequeProcessingTask task =
                        new ChequeProcessingTask(
                                cheque,
                                batchFolder,
                                i,
                                listener);


                completionService.submit(
                        task);
            }


            // =================================================
            // COLLECT COMPLETED WORK
            // =================================================

            for (int i = 0;
                    i < cheques.size();
                    i++) {

                Future<
                        ChequeProcessingTask.ProcessingResult>
                        future =
                        completionService.take();


                ChequeProcessingTask.ProcessingResult
                        processingResult =
                        future.get();


                /*
                 * Preserve the original cheque order.
                 */
                result.set(
                        processingResult.getIndex(),
                        processingResult.getCheque());


                System.out.println(
                        "[Parallel Processing] "
                                + "Completed "
                                + (i + 1)
                                + "/"
                                + cheques.size()
                                + " | "
                                + processingResult
                                        .getCheque()
                                        .getChequeNumber());
            }


        } catch (InterruptedException e) {

            /*
             * Restore interrupted status.
             */
            Thread.currentThread().interrupt();

            System.err.println(
                    "Parallel cheque processing interrupted.");


        } catch (Exception e) {

            /*
             * Parallel processing must not replace
             * existing capture behaviour.
             */
            e.printStackTrace();


        } finally {

            // =================================================
            // SHUTDOWN EXECUTOR
            // =================================================

            executor.shutdown();
        }


        // =====================================================
        // END LOG
        // =====================================================

        System.out.println(
                "================================================");

        System.out.println(
                "PARALLEL CHEQUE PROCESSING COMPLETED");

        System.out.println(
                "Processed Cheques: "
                        + result.size());

        System.out.println(
                "================================================");

        System.out.println();


        return result;
    }


    // =========================================================
    // SET PROCESSING LISTENER
    // =========================================================

    /**
     * Registers the listener used by the Capture Operator
     * controller to receive live processing updates.
     */
    public void setProcessingListener(
            ProcessingListener processingListener) {

        this.processingListener =
                processingListener;
    }
}