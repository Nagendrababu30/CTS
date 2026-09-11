package com.iispl.cts.service.outward;

import com.iispl.cts.model.outward.OutwardCheque;

import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.util.List;

/**
 * ============================================================
 * CFX XML WRITER
 * ============================================================
 *
 * Generates the CFX XML file for an outward batch.
 *
 * Rules:
 *
 * 1. CFX is available for every batch.
 * 2. All cheques belonging to the selected batch are included.
 * 3. Rejected-cheque/RRF logic is NOT handled here.
 *
 * NOTE:
 * This is the development/test XML structure already used
 * by the project. Production NPCI CFX fields/structure should
 * follow the applicable CTS/NPCI specification.
 *
 * ============================================================
 */
public class CXFXMLWriter {

    // ============================================================
    // GENERATE CFX
    // ============================================================

    public File generateCXF(
            String batchNumber,
            List<OutwardCheque> cheques,
            String outputDirectory) throws Exception {

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
        // CREATE OUTPUT DIRECTORY
        // ========================================================

        File directory =
                new File(
                        outputDirectory
                );

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
        // CREATE CFX FILE
        // ========================================================

        File cfxFile =
                new File(
                        directory,
                        "CFX_" + batchNumber + ".XML"
                );

        // ========================================================
        // WRITE XML
        // ========================================================

        try (FileWriter writer =
                     new FileWriter(cfxFile)) {

            writer.write(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            );

            writer.write(
                    "<OutwardBatch>\n"
            );

            // ====================================================
            // BATCH NUMBER
            // ====================================================

            writeElement(
                    writer,
                    "BatchNumber",
                    batchNumber,
                    4
            );

            // ====================================================
            // CHEQUE COUNT
            // ====================================================

            writeElement(
                    writer,
                    "ChequeCount",
                    String.valueOf(
                            cheques.size()
                    ),
                    4
            );

            // ====================================================
            // CHEQUES
            // ====================================================

            writer.write(
                    "    <Cheques>\n"
            );

            for (OutwardCheque cheque :
                    cheques) {

                if (cheque == null) {
                    continue;
                }

                writer.write(
                        "        <Cheque>\n"
                );

                // ------------------------------------------------
                // CHEQUE NUMBER
                // ------------------------------------------------

                writeElement(
                        writer,
                        "ChequeNumber",
                        cheque.getChequeNumber(),
                        12
                );

                // ------------------------------------------------
                // CITY CODE
                // ------------------------------------------------

                writeElement(
                        writer,
                        "CityCode",
                        cheque.getCityCode(),
                        12
                );

                // ------------------------------------------------
                // BANK CODE
                // ------------------------------------------------

                writeElement(
                        writer,
                        "BankCode",
                        cheque.getBankCode(),
                        12
                );

                // ------------------------------------------------
                // BRANCH CODE
                // ------------------------------------------------

                writeElement(
                        writer,
                        "BranchCode",
                        cheque.getBranchCode(),
                        12
                );

                // ------------------------------------------------
                // DRAWER ACCOUNT NUMBER
                // ------------------------------------------------

                writeElement(
                        writer,
                        "DrawerAccountNumber",
                        cheque.getDrawerAccountNumber(),
                        12
                );

                // ------------------------------------------------
                // DRAWER NAME
                // ------------------------------------------------

                writeElement(
                        writer,
                        "DrawerName",
                        cheque.getDrawerName(),
                        12
                );

                // ------------------------------------------------
                // DEPOSITOR ACCOUNT NUMBER
                // ------------------------------------------------

                writeElement(
                        writer,
                        "DepositorAccountNumber",
                        cheque.getDepositorAccountNumber(),
                        12
                );

                // ------------------------------------------------
                // DEPOSITOR NAME
                // ------------------------------------------------

                writeElement(
                        writer,
                        "DepositorName",
                        cheque.getDepositorName(),
                        12
                );

                // ------------------------------------------------
                // PAYEE NAME
                // ------------------------------------------------

                writeElement(
                        writer,
                        "PayeeName",
                        cheque.getPayeeName(),
                        12
                );

                // ------------------------------------------------
                // AMOUNT
                // ------------------------------------------------

                BigDecimal amount =
                        cheque.getAmount();

                writeElement(
                        writer,
                        "Amount",
                        amount == null
                                ? ""
                                : amount.toString(),
                        12
                );

                // ------------------------------------------------
                // AMOUNT IN WORDS
                // ------------------------------------------------

                writeElement(
                        writer,
                        "AmountInWords",
                        cheque.getAmountInWords(),
                        12
                );

                // ------------------------------------------------
                // CHEQUE DATE
                // ------------------------------------------------

                writeElement(
                        writer,
                        "ChequeDate",
                        cheque.getChequeDate() == null
                                ? ""
                                : cheque.getChequeDate().toString(),
                        12
                );

                // ------------------------------------------------
                // CHEQUE STATUS
                // ------------------------------------------------

                writeElement(
                        writer,
                        "ChequeStatus",
                        cheque.getChequeStatus(),
                        12
                );

                writer.write(
                        "        </Cheque>\n"
                );
            }

            writer.write(
                    "    </Cheques>\n"
            );

            writer.write(
                    "</OutwardBatch>\n"
            );
        }

        // ========================================================
        // VERIFY GENERATED FILE
        // ========================================================

        if (!cfxFile.exists()) {

            throw new IllegalStateException(
                    "CFX file was not created: "
                            + cfxFile.getAbsolutePath()
            );
        }

        if (cfxFile.length() == 0) {

            throw new IllegalStateException(
                    "Generated CFX file is empty: "
                            + cfxFile.getAbsolutePath()
            );
        }

        // ========================================================
        // LOG
        // ========================================================

        System.out.println(
                "CFX XML generated successfully: "
                        + cfxFile.getAbsolutePath()
        );

        System.out.println(
                "Batch: "
                        + batchNumber
                        + ", Cheques: "
                        + cheques.size()
        );

        // ========================================================
        // RETURN FILE
        // ========================================================

        return cfxFile;
    }

    // ============================================================
    // WRITE XML ELEMENT
    // ============================================================

    private void writeElement(
            FileWriter writer,
            String elementName,
            String value,
            int spaces) throws Exception {

        StringBuilder indentation =
                new StringBuilder();

        for (int i = 0; i < spaces; i++) {
            indentation.append(" ");
        }

        writer.write(
                indentation.toString()
        );

        writer.write(
                "<"
                        + elementName
                        + ">"
        );

        writer.write(
                escapeXml(value)
        );

        writer.write(
                "</"
                        + elementName
                        + ">\n"
        );
    }

    // ============================================================
    // ESCAPE XML
    // ============================================================

    private String escapeXml(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}