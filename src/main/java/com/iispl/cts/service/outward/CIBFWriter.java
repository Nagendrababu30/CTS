package com.iispl.cts.service.outward;

import com.iispl.cts.model.outward.OutwardCheque;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * ============================================================
 * CIBF WRITER
 * ============================================================
 *
 * Generates the CIBF file for an outward batch.
 *
 * Rules:
 *
 * 1. Every batch has CIBF.
 * 2. All cheques in the selected batch are processed.
 * 3. Front and back cheque images are included when available.
 * 4. RRF/rejection logic is NOT handled here.
 *
 * NOTE:
 * This is the development/test implementation.
 * Production NPCI CIBF binary structure must follow the
 * applicable CTS/NPCI specification.
 *
 * ============================================================
 */
public class CIBFWriter {

    // ============================================================
    // GENERATE CIBF
    // ============================================================

    public File generateCIBF(
            String batchNumber,
            List<OutwardCheque> cheques,
            String outputDirectory) throws Exception {

        // ========================================================
        // VALIDATE BATCH
        // ========================================================

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Batch number is required"
            );
        }

        // ========================================================
        // VALIDATE CHEQUES
        // ========================================================

        if (cheques == null ||
                cheques.isEmpty()) {

            throw new IllegalArgumentException(
                    "No cheque records found for batch: "
                            + batchNumber
            );
        }

        // ========================================================
        // VALIDATE OUTPUT DIRECTORY
        // ========================================================

        if (outputDirectory == null ||
                outputDirectory.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Output directory is required"
            );
        }

        batchNumber =
                batchNumber.trim();

        // ========================================================
        // CREATE DIRECTORY
        // ========================================================

        File directory =
                new File(outputDirectory);

        if (!directory.exists()) {

            if (!directory.mkdirs()) {

                throw new IllegalStateException(
                        "Unable to create output directory: "
                                + directory.getAbsolutePath()
                );
            }
        }

        if (!directory.isDirectory()) {

            throw new IllegalStateException(
                    "Output path is not a directory: "
                            + directory.getAbsolutePath()
            );
        }

        // ========================================================
        // CIBF FILE
        // ========================================================

        File cibfFile =
                new File(
                        directory,
                        "CIBF_" + batchNumber + ".IMG"
                );

        // ========================================================
        // GENERATE FILE
        // ========================================================

        int processedCheques = 0;
        int frontImages = 0;
        int backImages = 0;

        try (FileOutputStream output =
                     new FileOutputStream(cibfFile);
             FileWriter headerWriter =
                     new FileWriter(
                             cibfFile,
                             true
                     )) {

            /*
             * The development format is written as text markers
             * followed by image bytes.
             *
             * Production implementation must be replaced with
             * the actual NPCI CTS image-file structure.
             */

            headerWriter.write(
                    "CTS CIBF DEVELOPMENT FILE\n"
            );

            headerWriter.write(
                    "BatchNumber="
                            + batchNumber
                            + "\n"
            );

            headerWriter.write(
                    "ChequeCount="
                            + cheques.size()
                            + "\n"
            );

            headerWriter.flush();

        } catch (IOException e) {

            throw new IOException(
                    "Unable to create CIBF file: "
                            + e.getMessage(),
                    e
            );
        }

        /*
         * Re-open the file in append mode so that image bytes
         * can be added after the development header.
         */
        try (FileOutputStream output =
                     new FileOutputStream(
                             cibfFile,
                             true
                     )) {

            for (OutwardCheque cheque :
                    cheques) {

                if (cheque == null) {
                    continue;
                }

                processedCheques++;

                // =================================================
                // CHEQUE MARKER
                // =================================================

                writeText(
                        output,
                        "\nCHEQUE:"
                                + safe(
                                        cheque.getChequeNumber()
                                )
                                + "\n"
                );

                // =================================================
                // FRONT IMAGE
                // =================================================

                writeText(
                        output,
                        "FRONT_IMAGE\n"
                );

                String frontImagePath =
                        cheque.getFrontImagePath();

                if (isValidFile(frontImagePath)) {

                    copyFileBytes(
                            output,
                            frontImagePath
                    );

                    frontImages++;

                } else {

                    writeText(
                            output,
                            "NO_FRONT_IMAGE\n"
                    );
                }

                // =================================================
                // BACK IMAGE
                // =================================================

                writeText(
                        output,
                        "\nBACK_IMAGE\n"
                );

                String backImagePath =
                        cheque.getBackImagePath();

                if (isValidFile(backImagePath)) {

                    copyFileBytes(
                            output,
                            backImagePath
                    );

                    backImages++;

                } else {

                    writeText(
                            output,
                            "NO_BACK_IMAGE\n"
                    );
                }

                // =================================================
                // END CHEQUE
                // =================================================

                writeText(
                        output,
                        "\nEND_CHEQUE\n"
                );
            }

            // =====================================================
            // SUMMARY
            // =====================================================

            writeText(
                    output,
                    "\nProcessedCheques="
                            + processedCheques
                            + "\n"
            );

            writeText(
                    output,
                    "FrontImages="
                            + frontImages
                            + "\n"
            );

            writeText(
                    output,
                    "BackImages="
                            + backImages
                            + "\n"
            );

            writeText(
                    output,
                    "GenerationStatus=SUCCESS\n"
            );
        }

        // ========================================================
        // VERIFY FILE
        // ========================================================

        if (!cibfFile.exists()) {

            throw new IllegalStateException(
                    "CIBF file was not created: "
                            + cibfFile.getAbsolutePath()
            );
        }

        if (cibfFile.length() == 0) {

            throw new IllegalStateException(
                    "Generated CIBF file is empty: "
                            + cibfFile.getAbsolutePath()
            );
        }

        // ========================================================
        // LOG
        // ========================================================

        System.out.println(
                "CIBF generated successfully: "
                        + cibfFile.getAbsolutePath()
        );

        System.out.println(
                "Batch: "
                        + batchNumber
                        + ", Cheques: "
                        + processedCheques
        );

        System.out.println(
                "Front Images: "
                        + frontImages
                        + ", Back Images: "
                        + backImages
        );

        // ========================================================
        // RETURN FILE
        // ========================================================

        return cibfFile;
    }

    // ============================================================
    // WRITE TEXT
    // ============================================================

    private void writeText(
            FileOutputStream output,
            String value) throws IOException {

        if (value == null) {
            return;
        }

        output.write(
                value.getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                )
        );
    }

    // ============================================================
    // COPY IMAGE BYTES
    // ============================================================

    private void copyFileBytes(
            FileOutputStream output,
            String filePath) throws IOException {

        File imageFile =
                new File(
                        filePath.trim()
                );

        try (InputStream input =
                     new java.io.FileInputStream(
                             imageFile
                     )) {

            byte[] buffer =
                    new byte[8192];

            int bytesRead;

            while ((bytesRead =
                    input.read(buffer)) != -1) {

                output.write(
                        buffer,
                        0,
                        bytesRead
                );
            }
        }
    }

    // ============================================================
    // CHECK FILE
    // ============================================================

    private boolean isValidFile(
            String filePath) {

        if (filePath == null ||
                filePath.trim().isEmpty()) {

            return false;
        }

        File file =
                new File(
                        filePath.trim()
                );

        return file.exists() &&
                file.isFile() &&
                file.length() > 0;
    }

    // ============================================================
    // SAFE STRING
    // ============================================================

    private String safe(
            String value) {

        return value == null
                ? ""
                : value;
    }
}