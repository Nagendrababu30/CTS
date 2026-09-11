package com.iispl.cts.service.outward;

import com.iispl.cts.model.outward.OutwardCheque;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 * Generates RRF XML file for rejected cheques.
 *
 * IMPORTANT:
 * Only rejected cheques should be passed to this writer.
 * The Reports DAO is responsible for identifying rejected
 * cheques from cheque_processing.
 */
public class RRFXmlWriter {

    /**
     * Generate RRF XML for rejected cheques.
     *
     * @param batchNumber    Batch number
     * @param rejectedCheques Only rejected cheques
     * @param outputDirectory Output directory
     * @return Generated RRF file
     * @throws Exception if generation fails
     */
    public File generateRRF(
            String batchNumber,
            List<OutwardCheque> rejectedCheques,
            String outputDirectory) throws Exception {

        // -----------------------------
        // Basic validation
        // -----------------------------
        if (batchNumber == null || batchNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch number is required");
        }

        if (rejectedCheques == null || rejectedCheques.isEmpty()) {
            throw new IllegalArgumentException(
                    "RRF not available for this batch - no rejected cheques found");
        }

        if (outputDirectory == null || outputDirectory.trim().isEmpty()) {
            throw new IllegalArgumentException("Output directory is required");
        }

        batchNumber = batchNumber.trim();

        // -----------------------------
        // Create output directory
        // -----------------------------
        File directory = new File(outputDirectory);

        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IllegalStateException(
                        "Unable to create output directory: "
                                + directory.getAbsolutePath());
            }
        }

        if (!directory.isDirectory()) {
            throw new IllegalStateException(
                    "Output path is not a directory: "
                            + directory.getAbsolutePath());
        }

        // -----------------------------
        // RRF file
        // -----------------------------
        File rrfFile = new File(
                directory,
                "RRF_" + batchNumber + ".XML"
        );

        // -----------------------------
        // Generate XML
        // -----------------------------
        try (FileWriter writer = new FileWriter(rrfFile)) {

            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

            writer.write("<RRF>\n");

            // -----------------------------
            // File Header
            // -----------------------------
            writer.write("    <FileHeader>\n");

            writer.write("        <BatchNumber>");
            writer.write(escapeXml(batchNumber));
            writer.write("</BatchNumber>\n");

            writer.write("        <ItemCount>");
            writer.write(String.valueOf(rejectedCheques.size()));
            writer.write("</ItemCount>\n");

            writer.write("    </FileHeader>\n");

            // -----------------------------
            // Rejected cheque items
            // -----------------------------
            writer.write("    <Items>\n");

            for (OutwardCheque cheque : rejectedCheques) {

                if (cheque == null) {
                    continue;
                }

                writer.write("        <Item>\n");

                writer.write("            <ChequeNumber>");
                writer.write(escapeXml(cheque.getChequeNumber()));
                writer.write("</ChequeNumber>\n");

                writer.write("            <CityCode>");
                writer.write(escapeXml(cheque.getCityCode()));
                writer.write("</CityCode>\n");

                writer.write("            <BankCode>");
                writer.write(escapeXml(cheque.getBankCode()));
                writer.write("</BankCode>\n");

                writer.write("            <BranchCode>");
                writer.write(escapeXml(cheque.getBranchCode()));
                writer.write("</BranchCode>\n");

                writer.write("            <DrawerAccountNumber>");
                writer.write(
                        escapeXml(cheque.getDrawerAccountNumber())
                );
                writer.write("</DrawerAccountNumber>\n");

                writer.write("            <DrawerName>");
                writer.write(
                        escapeXml(cheque.getDrawerName())
                );
                writer.write("</DrawerName>\n");

                writer.write("            <DepositorAccountNumber>");
                writer.write(
                        escapeXml(cheque.getDepositorAccountNumber())
                );
                writer.write("</DepositorAccountNumber>\n");

                writer.write("            <Amount>");
                writer.write(
                        cheque.getAmount() == null
                                ? ""
                                : cheque.getAmount().toString()
                );
                writer.write("</Amount>\n");

                writer.write("            <ChequeDate>");
                writer.write(
                        cheque.getChequeDate() == null
                                ? ""
                                : cheque.getChequeDate().toString()
                );
                writer.write("</ChequeDate>\n");

                writer.write("        </Item>\n");
            }

            writer.write("    </Items>\n");

            writer.write("</RRF>\n");
        }

        // -----------------------------
        // Verify file
        // -----------------------------
        if (!rrfFile.exists()) {
            throw new IllegalStateException(
                    "RRF file was not created: "
                            + rrfFile.getAbsolutePath());
        }

        return rrfFile;
    }

    /**
     * Escape XML special characters.
     */
    private String escapeXml(String value) {

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