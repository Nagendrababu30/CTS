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

import com.cts.inward.model.OcrBatchData;
import com.cts.inward.model.OcrChequeData;

public class OcrParserImpl implements OcrParser {

    private final XMLInputFactory xmlInputFactory;

    private OcrParserImpl(XMLInputFactory xmlInputFactory) {
        this.xmlInputFactory = xmlInputFactory;
    }

    public static OcrParserImpl of(
            XMLInputFactory xmlInputFactory) {

        return new OcrParserImpl(xmlInputFactory);
    }

    @Override
    public OcrBatchData parse(String filePath) {

        try (InputStream inputStream =
                     Files.newInputStream(Path.of(filePath))) {

            XMLStreamReader reader =
                    xmlInputFactory.createXMLStreamReader(inputStream);

            OcrBatchData batchData = readBatch(reader);

            reader.close();

            return batchData;

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to parse OCR file: " + filePath,
                    e);
        }
    }

    private OcrBatchData readBatch(
            XMLStreamReader reader) throws Exception {

        long batchId = 0;
        String presentingBankName = null;
        int totalCheque = 0;
        String fileId = null;

        List<OcrChequeData> cheques = new ArrayList<>();

        while (reader.hasNext()) {

            int event = reader.next();

            if (event != XMLStreamConstants.START_ELEMENT) {
                continue;
            }

            String elementName = reader.getLocalName();

            switch (elementName) {

            case "BatchId":
                batchId = Long.parseLong(
                        reader.getElementText());
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

                OcrChequeData chequeData =
                        readCheque(reader);

                cheques.add(chequeData);

                break;

            default:
                break;
            }
        }

        return OcrBatchData.of(
                batchId,
                presentingBankName,
                totalCheque,
                fileId,
                cheques);
    }

    private OcrChequeData readCheque(
            XMLStreamReader reader) throws Exception {

        String chequeNumber = null;
        long batchId = 0;
        String accountNumber = null;
        LocalDate chequeDate = null;
        String drawerName = null;
        BigDecimal chequeAmount = null;
        String micrCode = null;
        String cityCode = null;
        String bankCode = null;
        String branchSpecificCode = null;

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
                batchId = Long.parseLong(
                        reader.getElementText());
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

            case "CityCode":
                cityCode = reader.getElementText();
                break;

            case "BankCode":
                bankCode = reader.getElementText();
                break;

            case "BranchSpecificCode":
                branchSpecificCode =
                        reader.getElementText();
                break;

            default:
                break;
            }
        }

        return OcrChequeData.of(
                chequeNumber,
                batchId,
                accountNumber,
                chequeDate,
                drawerName,
                chequeAmount,
                micrCode,
                cityCode,
                bankCode,
                branchSpecificCode);
    }
}