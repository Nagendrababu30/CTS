package com.iispl.cts.service.outward;

import com.iispl.cts.model.outward.OutwardCheque;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

public class CXFXMLWriter {

    public File generateCXF(
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

        String fileName = "CXF_" + batchNumber + ".XML";

        File outputFile = new File(directory, fileName);

        XMLOutputFactory factory =
                XMLOutputFactory.newFactory();

        try (FileOutputStream fos =
                     new FileOutputStream(outputFile)) {

            XMLStreamWriter writer =
                    factory.createXMLStreamWriter(
                            fos,
                            StandardCharsets.UTF_8.name()
                    );

            writer.writeStartDocument(
                    StandardCharsets.UTF_8.name(),
                    "1.0"
            );

            writer.writeStartElement("OutwardBatch");

            writeElement(
                    writer,
                    "BatchNumber",
                    batchNumber
            );

            writeElement(
                    writer,
                    "ChequeCount",
                    cheques.size()
            );

            writer.writeStartElement("Cheques");

            for (OutwardCheque cheque : cheques) {

                writer.writeStartElement("Cheque");

                writeElement(
                        writer,
                        "ChequeNumber",
                        cheque.getChequeNumber()
                );

                writeElement(
                        writer,
                        "CityCode",
                        cheque.getCityCode()
                );

                writeElement(
                        writer,
                        "BankCode",
                        cheque.getBankCode()
                );

                writeElement(
                        writer,
                        "BranchCode",
                        cheque.getBranchCode()
                );

                writeElement(
                        writer,
                        "DrawerAccountNumber",
                        cheque.getDrawerAccountNumber()
                );

                writeElement(
                        writer,
                        "DrawerName",
                        cheque.getDrawerName()
                );

                writeElement(
                        writer,
                        "DepositorAccountNumber",
                        cheque.getDepositorAccountNumber()
                );

                writeElement(
                        writer,
                        "DepositorName",
                        cheque.getDepositorName()
                );

                writeElement(
                        writer,
                        "PayeeName",
                        cheque.getPayeeName()
                );

                writeElement(
                        writer,
                        "Amount",
                        cheque.getAmount()
                );

                writeElement(
                        writer,
                        "AmountInWords",
                        cheque.getAmountInWords()
                );

                writeElement(
                        writer,
                        "ChequeDate",
                        cheque.getChequeDate()
                );

                writer.writeEndElement(); // Cheque
            }

            writer.writeEndElement(); // Cheques

            writer.writeEndElement(); // OutwardBatch

            writer.writeEndDocument();

            writer.flush();
            writer.close();
        }

        return outputFile;
    }


    private void writeElement(
            XMLStreamWriter writer,
            String elementName,
            Object value) throws Exception {

        writer.writeStartElement(elementName);

        if (value != null) {

            if (value instanceof BigDecimal) {

                writer.writeCharacters(
                        ((BigDecimal) value)
                                .toPlainString()
                );

            } else if (value instanceof LocalDate) {

                writer.writeCharacters(
                        value.toString()
                );

            } else {

                writer.writeCharacters(
                        String.valueOf(value)
                );
            }
        }

        writer.writeEndElement();
    }
}