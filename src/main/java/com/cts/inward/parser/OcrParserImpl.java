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

    public static OcrParserImpl of(XMLInputFactory xmlInputFactory) {
        return new OcrParserImpl(xmlInputFactory);
    }

    @Override
    public OcrBatchData parse(String filePath) {

        try (InputStream inputStream = Files.newInputStream(Path.of(filePath))) {

            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(inputStream);
            OcrBatchData batchData = readBatch(reader);
            reader.close();
            return batchData;

        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse OCR file: " + filePath, e);
        }
    }

    private OcrBatchData readBatch(XMLStreamReader reader) throws Exception {

        long batchCode = 0;
        String presentingBankName = null;
        int totalCheques = 0;
        long fileId = 0;
        List<OcrChequeData> cheques = new ArrayList<>();

        while (reader.hasNext()) {

            int event = reader.next();

            if (event != XMLStreamConstants.START_ELEMENT) {
                continue;
            }

            String elementName = reader.getLocalName();

            switch (elementName) {

            case "batch_code":
                batchCode = Long.parseLong(reader.getElementText());
                break;

            case "presenting_bank_name":
                presentingBankName = reader.getElementText();
                break;

            case "total_cheques":
                totalCheques = Integer.parseInt(reader.getElementText());
                break;

            case "file_id":
                fileId = Long.parseLong(reader.getElementText());
                break;

            case "cheque":
                cheques.add(readCheque(reader, batchCode));
                break;

            default:
                break;
            }
        }

        OcrBatchData batch = OcrBatchData.of(batchCode, presentingBankName, totalCheques, fileId);
        batch.setCheques(cheques);
        return batch;
    }

    private OcrChequeData readCheque(XMLStreamReader reader, long batchCode) throws Exception {

        String chequeNumber = null;
        String accountNumber = null;
        LocalDate chequeDate = null;
        LocalDate presentingDate = null;
        BigDecimal chequeAmount = null;
        String micrCode = null;
        String cityCode = null;
        String bankCode = null;
        String branchCode = null;
        String drawerName = null;
        String payeeName = null;
        String payeeAccountNumber = null;

        while (reader.hasNext()) {

            int event = reader.next();

            if (event == XMLStreamConstants.END_ELEMENT
                    && "cheque".equals(reader.getLocalName())) {
                break;
            }

            if (event != XMLStreamConstants.START_ELEMENT) {
                continue;
            }

            String elementName = reader.getLocalName();

            switch (elementName) {

            case "cheque_number":
                chequeNumber = reader.getElementText();
                break;

            case "account_number":
                accountNumber = reader.getElementText();
                break;

            case "cheque_amount":
                chequeAmount = new BigDecimal(reader.getElementText());
                break;

            case "cheque_date":
                chequeDate = LocalDate.parse(reader.getElementText());
                break;

            case "presenting_date":
                presentingDate = LocalDate.parse(reader.getElementText());
                break;

            case "micr_code":
                micrCode = reader.getElementText();
                break;

            case "city_code":
                cityCode = reader.getElementText();
                break;

            case "bank_code":
                bankCode = reader.getElementText();
                break;

            case "branch_code":
                branchCode = reader.getElementText();
                break;

            case "drawer_name":
                drawerName = reader.getElementText();
                break;

            case "payee_name":
                payeeName = reader.getElementText();
                break;

            case "payee_account_number":
                payeeAccountNumber = reader.getElementText();
                break;

            default:
                break;
            }
        }

        return OcrChequeData.of(
                chequeNumber,
                batchCode,
                accountNumber,
                chequeDate,
                presentingDate,
                chequeAmount,
                micrCode,
                cityCode,
                bankCode,
                branchCode,
                drawerName,
                payeeName,
                payeeAccountNumber);
    }
}
