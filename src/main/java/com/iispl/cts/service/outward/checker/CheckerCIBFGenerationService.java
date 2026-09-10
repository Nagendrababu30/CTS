package com.iispl.cts.service.outward.checker;

import com.iispl.cts.model.outward.OutwardCheque;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CheckerCIBFGenerationService {

    /**
     * Development CIBF generation.
     *
     * Validates the front/back cheque images
     * and creates a batch CIBF output file.
     *
     * This is NOT the final NPCI production CIBF format.
     */
    public String generateCIBF(
            String batchNumber,
            List<OutwardCheque> cheques) {

        if (batchNumber == null ||
                batchNumber.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Batch number is required."
            );
        }

        if (cheques == null ||
                cheques.isEmpty()) {

            throw new IllegalArgumentException(
                    "No cheques available for CIBF generation."
            );
        }


        // =========================================
        // OUTPUT DIRECTORY
        // =========================================

        String directoryPath =
                "C:/CTS/OUTWARD/"
                        + batchNumber;

        File directory =
                new File(directoryPath);

        if (!directory.exists()) {

            if (!directory.mkdirs()) {

                throw new RuntimeException(
                        "Unable to create output directory:\n"
                                + directoryPath
                );
            }
        }


        // =========================================
        // CIBF FILE
        // =========================================

        String fileName =
                batchNumber + ".CIBF";

        File cibfFile =
                new File(
                        directory,
                        fileName
                );


        // =========================================
        // VALIDATE IMAGES
        // =========================================

        int validImages = 0;

        for (OutwardCheque cheque : cheques) {

            String frontImage =
                    cheque.getFrontImagePath();

            String backImage =
                    cheque.getBackImagePath();


            if (frontImage != null &&
                    !frontImage.trim().isEmpty()) {

                File frontFile =
                        new File(frontImage);

                if (frontFile.exists() &&
                        frontFile.isFile()) {

                    validImages++;
                }
            }


            if (backImage != null &&
                    !backImage.trim().isEmpty()) {

                File backFile =
                        new File(backImage);

                if (backFile.exists() &&
                        backFile.isFile()) {

                    validImages++;
                }
            }
        }


        // =========================================
        // DEVELOPMENT OUTPUT
        // =========================================

        try (FileOutputStream output =
                     new FileOutputStream(cibfFile)) {

            String header =
                    "CTS CIBF DEVELOPMENT FILE\n"
                            + "BatchNumber="
                            + batchNumber
                            + "\n"
                            + "ChequeCount="
                            + cheques.size()
                            + "\n"
                            + "ValidImages="
                            + validImages
                            + "\n";

            output.write(
                    header.getBytes("UTF-8")
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to generate CIBF file.",
                    e
            );
        }


        return cibfFile.getAbsolutePath();
    }
}