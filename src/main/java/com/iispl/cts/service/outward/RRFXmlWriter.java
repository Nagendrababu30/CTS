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

public class RRFXmlWriter {

    public File generateRRF(
            String batchNumber,
            List<OutwardCheque> cheques,
            String outputDirectory) throws Exception {

        File directory =
                new File(outputDirectory);

        if (!directory.exists()
                && !directory.mkdirs()) {

            throw new Exception(
                    "Unable to create output directory: "
                            + directory.getAbsolutePath()
            );
        }

        String fileName =
                "RRF_" + batchNumber + ".XML";

        File outputFile =
                new File(directory, fileName);

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

            writer.writeStartElement(
                    "FileHeader"
            );

            writeElement(
                    writer,
                    "BatchNumber",
                    batchNumber
            );

            writeElement(
                    writer,
                    "ItemCount",
                    cheques.size()
            );

            writer.writeEndElement();

            writer.writeStartElement(
                    "Items"
            );

            for (OutwardCheque cheque : cheques) {

                writer.writeStartElement(
                        "Item"
                );

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
                        "Amount",
                        cheque.getAmount()
                );

                writeElement(
                        writer,
                        "ChequeDate",
                        cheque.getChequeDate()
                );

                writer.writeEndElement();
            }

            writer.writeEndElement();

            writer.writeEndDocument();

            writer.flush();
            writer.close();
        }

        return outputFile;
    }


    private void writeElement(
            XMLStreamWriter writer,
            String name,
            Object value) throws Exception {

        writer.writeStartElement(name);

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