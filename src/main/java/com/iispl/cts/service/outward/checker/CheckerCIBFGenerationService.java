package com.iispl.cts.service.outward.checker;

import com.iispl.cts.model.outward.OutwardCheque;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 * ============================================================
 * CHECKER CIBF GENERATION SERVICE
 * ============================================================
 *
 * Generates CIBF file for a batch after Checker processing.
 *
 * Rule:
 * - Every batch has CIBF.
 * - CIBF is independent of RRF.
 * - RRF availability/rejection logic is NOT handled here.
 *
 * NOTE:
 * This is the existing development/test implementation.
 * Production NPCI CIBF format must follow the applicable
 * CTS/NPCI specification.
 *
 * ============================================================
 */
public class CheckerCIBFGenerationService {

    // ============================================================
    // GENERATE CIBF
    // ============================================================

    /**
     * Generate CIBF for the supplied batch.
     *
     * @param batchNumber Batch number
     * @param cheques     Cheques belonging to the batch
     * @return Absolute path of generated CIBF file
     * @throws Exception if generation fails
     */
    public String generateCIBF(
            String batchNumber,
            List<OutwardCheque> cheques) throws Exception {

        // ========================================================
        // VALIDATE BATCH NUMBER
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

        batchNumber = batchNumber.trim();

        // ========================================================
        // OUTPUT DIRECTORY
        // ========================================================

        String directoryPath =
                "C:/CTS/OUTWARD/"
                        + batchNumber;

        File directory =
                new File(directoryPath);

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
                        batchNumber + ".CIBF"
                );

        // ========================================================
        // GENERATE DEVELOPMENT CIBF
        // ========================================================

        int validImages = 0;

        try (FileWriter writer =
                     new FileWriter(cibfFile)) {

            // ----------------------------------------------------
            // HEADER
            // ----------------------------------------------------

            writer.write(
                    "CTS CIBF DEVELOPMENT FILE\n"
            );

            writer.write(
                    "BatchNumber="
                            + batchNumber
                            + "\n"
            );

            writer.write(
                    "ChequeCount="
                            + cheques.size()
                            + "\n"
            );

            // ----------------------------------------------------
            // CHECK IMAGES
            // ----------------------------------------------------

            for (OutwardCheque cheque :
                    cheques) {

                if (cheque == null) {
                    continue;
                }

                String frontImage =
                        cheque.getFrontImagePath();

                String backImage =
                        cheque.getBackImagePath();

                boolean frontValid =
                        isValidImage(frontImage);

                boolean backValid =
                        isValidImage(backImage);

                if (frontValid ||
                        backValid) {

                    validImages++;
                }

                writer.write(
                        "ChequeNumber="
                                + safe(
                                        cheque.getChequeNumber()
                                )
                                + "\n"
                );

                writer.write(
                        "FrontImage="
                                + safe(frontImage)
                                + "\n"
                );

                writer.write(
                        "BackImage="
                                + safe(backImage)
                                + "\n"
                );
            }

            // ----------------------------------------------------
            // IMAGE SUMMARY
            // ----------------------------------------------------

            writer.write(
                    "ValidImages="
                            + validImages
                            + "\n"
            );

            writer.write(
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
                        + cheques.size()
                        + ", Valid Images: "
                        + validImages
        );

        // ========================================================
        // RETURN PATH
        // ========================================================

        return cibfFile.getAbsolutePath();
    }

    // ============================================================
    // VALIDATE IMAGE
    // ============================================================

    private boolean isValidImage(
            String imagePath) {

        if (imagePath == null ||
                imagePath.trim().isEmpty()) {

            return false;
        }

        File imageFile =
                new File(
                        imagePath.trim()
                );

        return imageFile.exists() &&
                imageFile.isFile();
    }

    // ============================================================
    // SAFE STRING
    // ============================================================

    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }
}