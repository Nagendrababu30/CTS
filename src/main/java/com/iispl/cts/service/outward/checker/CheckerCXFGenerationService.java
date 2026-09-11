package com.iispl.cts.service.outward.checker;

import com.iispl.cts.model.outward.OutwardCheque;

import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.util.List;

/**
 * ============================================================
 * CHECKER CFX GENERATION SERVICE
 * ============================================================
 *
 * Generates CFX XML for a Checker batch.
 *
 * Rules:
 *
 * 1. Every batch has CFX.
 * 2. CFX is independent of RRF.
 * 3. All cheques belonging to the selected batch are included.
 * 4. RRF/rejected-cheque logic is NOT handled here.
 *
 * NOTE:
 * This is the development/test implementation.
 * Production NPCI CFX structure must follow the applicable
 * CTS/NPCI specification.
 *
 * ============================================================
 */
public class CheckerCXFGenerationService {

    // ============================================================
    // GENERATE CFX
    // ============================================================

    public String generateCXF(
            String batchNumber,
            List<OutwardCheque> cheques) throws Exception {

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
        // CFX FILE
        // ========================================================

        File cfxFile =
                new File(
                        directory,
                        batchNumber + ".CFX.XML"
                );

        // ========================================================
        // WRITE CFX XML
        // ========================================================

        try (FileWriter writer =
                     new FileWriter(cfxFile)) {

            writer.write(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            );

            writer.write(
                    "<OutwardBatch>\n"
            );

            // ----------------------------------------------------
            // BATCH NUMBER
            // ----------------------------------------------------

            writer.write(
                    "    <BatchNumber>"
            );

            writer.write(
                    escapeXml(batchNumber)
            );

            writer.write(
                    "</BatchNumber>\n"
            );

            // ----------------------------------------------------
            // CHEQUE COUNT
            // ----------------------------------------------------

            writer.write(
                    "    <ChequeCount>"
            );

            writer.write(
                    String.valueOf(cheques.size())
            );

            writer.write(
                    "</ChequeCount>\n"
            );

            // ----------------------------------------------------
            // CHEQUES
            // ----------------------------------------------------

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
        // VERIFY FILE
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
                "CFX generated successfully: "
                        + cfxFile.getAbsolutePath()
        );

        System.out.println(
                "Batch: "
                        + batchNumber
                        + ", Cheques: "
                        + cheques.size()
        );

        // ========================================================
        // RETURN PATH
        // ========================================================

        return cfxFile.getAbsolutePath();
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