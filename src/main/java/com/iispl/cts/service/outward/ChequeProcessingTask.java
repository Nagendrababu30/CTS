package com.iispl.cts.service.outward;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

import com.iispl.cts.model.outward.OutwardCheque;

/**
 * Processes one cheque independently.
 *
 * IMPORTANT:
 * This class does NOT modify database data.
 * It only performs parallel file/image inspection.
 */
public class ChequeProcessingTask
        implements Callable<ChequeProcessingTask.ProcessingResult> {

    private final OutwardCheque cheque;
    private final File batchFolder;
    private final int index;
    private final ProcessingListener listener;

    public ChequeProcessingTask(
            OutwardCheque cheque,
            File batchFolder,
            int index,
            ProcessingListener listener) {

        this.cheque = cheque;
        this.batchFolder = batchFolder;
        this.index = index;
        this.listener = listener;
    }

    @Override
    public ProcessingResult call() {

        String threadName =
                Thread.currentThread().getName();

        String chequeNumber =
                safe(cheque.getChequeNumber());

        notifyListener(
                ProcessingStatus.PROCESSING,
                chequeNumber,
                threadName,
                null);

        try {

            /*
             * -----------------------------------------------------
             * FRONT IMAGE
             * -----------------------------------------------------
             */

            File frontImage =
                    findUploadedFile(
                            cheque.getFrontImagePath());

            long frontSize =
                    getFileSize(frontImage);

            /*
             * -----------------------------------------------------
             * BACK IMAGE
             * -----------------------------------------------------
             */

            File backImage =
                    findUploadedFile(
                            cheque.getBackImagePath());

            long backSize =
                    getFileSize(backImage);

            /*
             * -----------------------------------------------------
             * REAL PARALLEL FILE WORK
             * -----------------------------------------------------
             *
             * We read the image files in this worker thread.
             *
             * Existing cheque object is NOT changed.
             * Database is NOT touched.
             */

            if (frontImage != null) {
                readFile(frontImage);
            }

            if (backImage != null) {
                readFile(backImage);
            }

            String message =
                    "Front="
                    + frontSize
                    + " bytes, Back="
                    + backSize
                    + " bytes";

            notifyListener(
                    ProcessingStatus.COMPLETED,
                    chequeNumber,
                    threadName,
                    message);

            return new ProcessingResult(
                    cheque,
                    index,
                    true,
                    message);

        } catch (Exception e) {

            /*
             * IMPORTANT:
             *
             * Existing capture functionality must not be
             * changed just because this demonstration/
             * parallel inspection encounters a file problem.
             *
             * Therefore we report the error but return the
             * original cheque.
             */

            String message =
                    e.getMessage();

            if (message == null ||
                    message.trim().isEmpty()) {

                message =
                        "Parallel image processing error";
            }

            notifyListener(
                    ProcessingStatus.ERROR,
                    chequeNumber,
                    threadName,
                    message);

            return new ProcessingResult(
                    cheque,
                    index,
                    false,
                    message);
        }
    }

    // =========================================================
    // FIND UPLOADED FILE
    // =========================================================

    private File findUploadedFile(
            String imagePath) {

        if (imagePath == null ||
                imagePath.trim().isEmpty()) {

            return null;
        }

        /*
         * XML parser converts paths to web paths such as:
         *
         * /zul/outward/images/Batch1/000101_front.png
         *
         * Uploaded files themselves are stored directly inside
         * the temporary batch folder.
         *
         * Therefore we use only the filename here.
         */

        String normalized =
                imagePath
                        .replace("\\", "/")
                        .trim();

        int lastSlash =
                normalized.lastIndexOf('/');

        String fileName =
                lastSlash >= 0
                        ? normalized.substring(
                                lastSlash + 1)
                        : normalized;

        if (fileName.isEmpty()) {
            return null;
        }

        File file =
                new File(
                        batchFolder,
                        fileName);

        if (file.exists() &&
                file.isFile()) {

            return file;
        }

        return null;
    }

    // =========================================================
    // FILE SIZE
    // =========================================================

    private long getFileSize(
            File file) {

        if (file == null ||
                !file.exists() ||
                !file.isFile()) {

            return 0L;
        }

        return file.length();
    }

    // =========================================================
    // READ FILE
    // =========================================================

    private void readFile(
            File file)
            throws IOException {

        byte[] buffer =
                new byte[8192];

        try (FileInputStream input =
                     new FileInputStream(file)) {

            while (input.read(buffer) != -1) {
                // Intentionally reading the file.
                // Actual image processing can be added later.
            }
        }
    }

    // =========================================================
    // SAFE
    // =========================================================

    private String safe(
            String value) {

        return value == null
                ? ""
                : value.trim();
    }

    // =========================================================
    // LISTENER
    // =========================================================

    private void notifyListener(
            ProcessingStatus status,
            String chequeNumber,
            String threadName,
            String message) {

        if (listener == null) {
            return;
        }

        try {

            listener.onProcessing(
                    status,
                    chequeNumber,
                    threadName,
                    message);

        } catch (Exception e) {

            /*
             * Listener failure must never affect
             * cheque processing.
             */

            e.printStackTrace();
        }
    }

    // =========================================================
    // STATUS
    // =========================================================

    public enum ProcessingStatus {

        PROCESSING,

        COMPLETED,

        ERROR
    }

    // =========================================================
    // LISTENER
    // =========================================================

    public interface ProcessingListener {

        void onProcessing(
                ProcessingStatus status,
                String chequeNumber,
                String threadName,
                String message);
    }

    // =========================================================
    // RESULT
    // =========================================================

    public static class ProcessingResult {

        private final OutwardCheque cheque;

        private final int index;

        private final boolean successful;

        private final String message;

        public ProcessingResult(
                OutwardCheque cheque,
                int index,
                boolean successful,
                String message) {

            this.cheque = cheque;
            this.index = index;
            this.successful = successful;
            this.message = message;
        }

        public OutwardCheque getCheque() {
            return cheque;
        }

        public int getIndex() {
            return index;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getMessage() {
            return message;
        }
    }
}