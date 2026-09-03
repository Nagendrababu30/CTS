package com.cts.inward.parser;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import com.cts.inward.model.NpciBatchData;
import com.cts.inward.model.NpciChequeData;

public class PxfParserImpl implements PxfParser {

    private final XMLInputFactory xmlInputFactory;

    private PxfParserImpl(XMLInputFactory xmlInputFactory) {
        this.xmlInputFactory = xmlInputFactory;
    }

    public static PxfParserImpl of(
            XMLInputFactory xmlInputFactory) {

        return new PxfParserImpl(xmlInputFactory);
    }

    @Override
    public NpciBatchData parse(String filePath) {

        try (InputStream inputStream =
                     Files.newInputStream(Path.of(filePath))) {

            XMLStreamReader reader =
                    xmlInputFactory.createXMLStreamReader(inputStream);

            NpciBatchData batchData = readBatch(reader);

            reader.close();

            return batchData;

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to parse PXF file: " + filePath,
                    e);
        }
    }

    private NpciBatchData readBatch(
            XMLStreamReader reader) throws Exception {

        String batchId = null;
        String presentingBankName = null;
        int totalCheque = 0;
        String fileId = null;

        List<NpciChequeData> cheques = new ArrayList<>();

        while (reader.hasNext()) {

            int event = reader.next();

            if (event != XMLStreamConstants.START_ELEMENT) {
                continue;
            }

            String elementName = reader.getLocalName();

            switch (elementName) {

            case "BatchId":
                batchId = reader.getElementText();
                break;

            case "PresentingBankName":
                presentingBankName =
                        reader.getElementText();
                break;

            case "TotalCheque":
                totalCheque =
                        Integer.parseInt(
                                reader.getElementText());
                break;

            case "FileId":
                fileId = reader.getElementText();
                break;

            case "Cheque":

                NpciChequeData chequeData =
                        readCheque(reader);

                cheques.add(chequeData);

                break;

            default:
                break;
            }
        }

        return NpciBatchData.of(
                batchId,
                presentingBankName,
                totalCheque,
                fileId,
                cheques);
    }

    private NpciChequeData readCheque(
            XMLStreamReader reader) throws Exception {

        String chequeNumber = null;
        String batchId = null;
        String accountNumber = null;
        LocalDate chequeDate = null;
        String drawerName = null;
        BigDecimal chequeAmount = null;
        String micrCode = null;

        while (reader.hasNext()) {

            int event = reader.next();

            if (event == XMLStreamConstants.END_ELEMENT
                    && "Cheque".equals(reader.getLocalName())) {

                break;
            }

            if (event != XMLStreamConstants.START_ELEMENT) {
                continue;
            }

            String elementName = reader.getLocalName();

            switch (elementName) {

            case "ChequeNumber":
                chequeNumber = reader.getElementText();
                break;

            case "BatchId":
                batchId = reader.getElementText();
                break;

            case "AccountNumber":
                accountNumber = reader.getElementText();
                break;

            case "ChequeDate":
                chequeDate =
                        LocalDate.parse(
                                reader.getElementText());
                break;

            case "DrawerName":
                drawerName = reader.getElementText();
                break;

            case "ChequeAmount":
                chequeAmount =
                        new BigDecimal(
                                reader.getElementText());
                break;

            case "MicrCode":
                micrCode = reader.getElementText();
                break;

            default:
                break;
            }
        }

        return NpciChequeData.of(
                chequeNumber,
                batchId,
                accountNumber,
                chequeDate,
                drawerName,
                chequeAmount,
                micrCode);
    }
}