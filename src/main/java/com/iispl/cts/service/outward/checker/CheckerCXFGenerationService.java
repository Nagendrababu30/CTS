package com.iispl.cts.service.outward.checker;

import com.iispl.cts.model.outward.OutwardCheque;

import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CheckerCXFGenerationService {

    /**
     * Generate CXF XML file for the selected batch.
     *
     * Temporary development version.
     */
    public String generateCXF(
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
                    "No cheques available for CXF generation."
            );
        }


        /*
         * Create output directory.
         *
         * Change this path later to your
         * actual CTS outward file directory.
         */
        String directoryPath =
                "C:/CTS/OUTWARD/" +
                batchNumber;

        File directory =
                new File(directoryPath);

        if (!directory.exists()) {

            directory.mkdirs();
        }


        /*
         * CXF file name.
         */
        String fileName =
                batchNumber + ".CXF.XML";

        File cxfFile =
                new File(
                        directory,
                        fileName
                );


        try (FileWriter writer =
                     new FileWriter(cxfFile)) {

            writer.write(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            );

            writer.write(
                    "<OutwardBatch>\n"
            );

            writer.write(
                    "    <BatchNumber>"
                            + escapeXML(batchNumber)
                            + "</BatchNumber>\n"
            );

            writer.write(
                    "    <ChequeCount>"
                            + cheques.size()
                            + "</ChequeCount>\n"
            );


            writer.write(
                    "    <Cheques>\n"
            );


            for (OutwardCheque cheque :
                    cheques) {

                writer.write(
                        "        <Cheque>\n"
                );


                writer.write(
                        "            <ChequeNumber>"
                                + escapeXML(
                                        cheque.getChequeNumber()
                                )
                                + "</ChequeNumber>\n"
                );


                writer.write(
                        "            <CityCode>"
                                + escapeXML(
                                        cheque.getCityCode()
                                )
                                + "</CityCode>\n"
                );


                writer.write(
                        "            <BankCode>"
                                + escapeXML(
                                        cheque.getBankCode()
                                )
                                + "</BankCode>\n"
                );


                writer.write(
                        "            <BranchCode>"
                                + escapeXML(
                                        cheque.getBranchCode()
                                )
                                + "</BranchCode>\n"
                );


                writer.write(
                        "            <DrawerAccountNumber>"
                                + escapeXML(
                                        cheque.getDrawerAccountNumber()
                                )
                                + "</DrawerAccountNumber>\n"
                );


                writer.write(
                        "            <DrawerName>"
                                + escapeXML(
                                        cheque.getDrawerName()
                                )
                                + "</DrawerName>\n"
                );


                writer.write(
                        "            <DepositorAccountNumber>"
                                + escapeXML(
                                        cheque.getDepositorAccountNumber()
                                )
                                + "</DepositorAccountNumber>\n"
                );


                writer.write(
                        "            <DepositorName>"
                                + escapeXML(
                                        cheque.getDepositorName()
                                )
                                + "</DepositorName>\n"
                );


                writer.write(
                        "            <PayeeName>"
                                + escapeXML(
                                        cheque.getPayeeName()
                                )
                                + "</PayeeName>\n"
                );


                writer.write(
                        "            <Amount>"
                                + amount(
                                        cheque.getAmount()
                                )
                                + "</Amount>\n"
                );


                writer.write(
                        "            <AmountInWords>"
                                + escapeXML(
                                        cheque.getAmountInWords()
                                )
                                + "</AmountInWords>\n"
                );


                writer.write(
                        "            <ChequeDate>"
                                + date(
                                        cheque.getChequeDate()
                                )
                                + "</ChequeDate>\n"
                );


                writer.write(
                        "            <ChequeStatus>"
                                + escapeXML(
                                        cheque.getChequeStatus()
                                )
                                + "</ChequeStatus>\n"
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


        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to generate CXF file.",
                    e
            );
        }


        return cxfFile.getAbsolutePath();
    }


    private String escapeXML(
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


    private String amount(
            BigDecimal value) {

        if (value == null) {
            return "0.00";
        }

        return value.toPlainString();
    }


    private String date(
            LocalDate value) {

        if (value == null) {
            return "";
        }

        return value.toString();
    }
}