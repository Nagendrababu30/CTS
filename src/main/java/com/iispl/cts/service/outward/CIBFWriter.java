package com.iispl.cts.service.outward;

import com.iispl.cts.model.outward.OutwardCheque;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CIBFWriter {

    public File generateCIBF(
            String batchNumber,
            List<OutwardCheque> cheques,
            String outputDirectory) throws Exception {

        File directory = new File(outputDirectory);

        if (!directory.exists() && !directory.mkdirs()) {
            throw new Exception(
                    "Unable to create output directory: "
                            + directory.getAbsolutePath()
            );
        }

        String fileName =
                "CIBF_" + batchNumber + ".IMG";

        File outputFile =
                new File(directory, fileName);

        try (FileOutputStream outputStream =
                     new FileOutputStream(outputFile)) {

            for (OutwardCheque cheque : cheques) {

                /*
                 * Write cheque number marker.
                 *
                 * This is useful for the internal/test package.
                 * The production NPCI CIBF structure must follow
                 * the applicable CTS specification.
                 */
                writeText(
                        outputStream,
                        "CHEQUE:"
                                + safe(cheque.getChequeNumber())
                                + "\n"
                );

                // FRONT IMAGE
                if (hasValue(cheque.getFrontImagePath())) {

                    writeText(
                            outputStream,
                            "FRONT_IMAGE\n"
                    );

                    writeImage(
                            outputStream,
                            cheque.getFrontImagePath()
                    );
                }

                // BACK IMAGE
                if (hasValue(cheque.getBackImagePath())) {

                    writeText(
                            outputStream,
                            "\nBACK_IMAGE\n"
                    );

                    writeImage(
                            outputStream,
                            cheque.getBackImagePath()
                    );
                }

                writeText(
                        outputStream,
                        "\nEND_CHEQUE\n"
                );
            }
        }

        return outputFile;
    }


    private void writeImage(
            FileOutputStream outputStream,
            String imagePath) throws IOException {

        File imageFile =
                new File(imagePath);

        if (!imageFile.exists()) {

            throw new IOException(
                    "Cheque image not found: "
                            + imagePath
            );
        }

        try (FileInputStream inputStream =
                     new FileInputStream(imageFile)) {

            byte[] buffer =
                    new byte[8192];

            int bytesRead;

            while ((bytesRead =
                    inputStream.read(buffer)) != -1) {

                outputStream.write(
                        buffer,
                        0,
                        bytesRead
                );
            }
        }
    }


    private void writeText(
            FileOutputStream outputStream,
            String text) throws IOException {

        outputStream.write(
                text.getBytes()
        );
    }


    private boolean hasValue(String value) {

        return value != null
                && !value.trim().isEmpty();
    }


    private String safe(String value) {

        return value == null ? "" : value;
    }
}