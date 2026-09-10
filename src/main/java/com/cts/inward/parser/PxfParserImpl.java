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

import com.cts.inward.dto.PxfParserResult;
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
    public PxfParserResult parse(String filePath) {

        try (InputStream inputStream =
                     Files.newInputStream(Path.of(filePath))) {

            XMLStreamReader reader =
                    xmlInputFactory.createXMLStreamReader(inputStream);

            PxfParserResult result =
                    readPxf(reader);

            reader.close();

            return result;

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to parse PXF file: "
                            + filePath,
                    e);
        }
    }

    private PxfParserResult readPxf(
            XMLStreamReader reader) throws Exception {

        long batchCode = 0;
        long fileId = 0;
        String presentingBankName = null;
        int totalCheques = 0;

        List<NpciChequeData> chequeDataList =
                new ArrayList<>();

        while (reader.hasNext()) {

            int event = reader.next();

            if (event != XMLStreamConstants.START_ELEMENT) {
                continue;
            }

            String elementName =
                    reader.getLocalName();

            switch (elementName) {

            case "batch_code":

                batchCode =
                        Long.parseLong(reader.getElementText());

                break;

            case "presenting_bank_name":

                presentingBankName =
                        reader.getElementText();

                break;

            case "total_cheques":

                totalCheques =
                        Integer.parseInt(
                                reader.getElementText());

                break;

            case "file_id":

                fileId =
                		Long.parseLong(reader.getElementText());

                break;

            case "cheque":

                NpciChequeData chequeData =
                        readCheque(reader, batchCode);

                chequeDataList.add(chequeData);

                break;

            default:
                break;
            }
        }

        NpciBatchData batchData =
                new NpciBatchData(
                        batchCode,
                        fileId,
                        presentingBankName,
                        totalCheques);

        return PxfParserResult.of(
                batchData,
                chequeDataList);
    }

    private NpciChequeData readCheque(
            XMLStreamReader reader, long batchCode) throws Exception {

        String chequeNumber = null;
        String accountNumber = null;
        LocalDate chequeDate = null;
        BigDecimal chequeAmount = null;
        String micrCode = null;
        String cityCode = null;
        String bankCode = null;
        String branchCode = null;
        String payeeName = null;
        String payeeAccountNumber = null;

        while (reader.hasNext()) {

            int event = reader.next();

            if (event == XMLStreamConstants.END_ELEMENT
                    && "cheque".equals(
                            reader.getLocalName())) {

                break;
            }

            if (event != XMLStreamConstants.START_ELEMENT) {
                continue;
            }

            String elementName =
                    reader.getLocalName();

            switch (elementName) {

            case "cheque_number":

                chequeNumber =
                        reader.getElementText();

                break;

            case "account_number":

                accountNumber =
                        reader.getElementText();

                break;

            case "cheque_amount":

                chequeAmount =
                        new BigDecimal(
                                reader.getElementText());

                break;

            case "cheque_date":

                chequeDate =
                        LocalDate.parse(
                                reader.getElementText());

                break;

            case "micr_code":

                micrCode =
                        reader.getElementText();

                break;

            case "city_code":

                cityCode =
                        reader.getElementText();

                break;

            case "bank_code":

                bankCode =
                        reader.getElementText();

                break;

            case "branch_code":

                branchCode =
                        reader.getElementText();

                break;

            case "payee_name":

                payeeName =
                        reader.getElementText();

                break;

            case "payee_account_number":

                payeeAccountNumber =
                        reader.getElementText();

                break;

            default:
                break;
            }
        }

        return NpciChequeData.of(
                chequeNumber,
                batchCode,
                accountNumber,
                chequeDate,
                chequeAmount,
                micrCode,
                cityCode,
                bankCode,
                branchCode,
                payeeName,
                payeeAccountNumber);
    }
}