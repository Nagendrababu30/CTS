package com.iispl.cts.service.outward;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import com.iispl.cts.model.outward.OutwardCheque;

public class CaptureOperatorXMLParser {

    // parse XML file and create cheque records
    public List<OutwardCheque> parse(File xmlFile, String batchNumber, File batchFolder) throws Exception {
        List<OutwardCheque> cheques = new ArrayList<>();

        if (xmlFile == null || !xmlFile.exists() || !xmlFile.isFile()) {
            throw new IllegalArgumentException(
                    "XML file does not exist: "
                            + (xmlFile == null ? "null" : xmlFile.getAbsolutePath()));
        }

        if (batchNumber == null || batchNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch number is required.");
        }

        if (batchFolder == null || !batchFolder.exists() || !batchFolder.isDirectory()) {
            throw new IllegalArgumentException(
                    "Batch folder does not exist: "
                            + (batchFolder == null ? "null" : batchFolder.getAbsolutePath()));
        }

        XMLInputFactory factory = XMLInputFactory.newFactory();

        try {
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        } catch (IllegalArgumentException ignored) {
        }

        try {
            factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        } catch (IllegalArgumentException ignored) {
        }

        try (InputStream input = new FileInputStream(xmlFile)) {
            XMLStreamReader reader = factory.createXMLStreamReader(input);

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    String element = reader.getLocalName();

                    if (equalsIgnoreCase(element, "Cheque")) {
                        OutwardCheque cheque = parseCheque(reader, batchNumber, batchFolder);

                        if (cheque != null) {
                            cheques.add(cheque);
                        }
                    }
                }
            }

            reader.close();
        }

        return cheques;
    }

    // parse one cheque from XML
    private OutwardCheque parseCheque(XMLStreamReader reader, String batchNumber, File batchFolder)
            throws Exception {
        OutwardCheque cheque = new OutwardCheque();
        cheque.setBatchNumber(batchNumber);

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.END_ELEMENT) {
                if (equalsIgnoreCase(reader.getLocalName(), "Cheque")) {
                    break;
                }
                continue;
            }

            if (event != XMLStreamConstants.START_ELEMENT) {
                continue;
            }

            String field = reader.getLocalName();
            String value = readElementText(reader);

            if (value == null) {
                value = "";
            }

            value = value.trim();
            setChequeField(cheque, field, value, batchFolder);
        }

        return cheque;
    }

    // read XML element text value
    private String readElementText(XMLStreamReader reader) throws Exception {
        StringBuilder value = new StringBuilder();

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.CHARACTERS
                    || event == XMLStreamConstants.CDATA) {
                value.append(reader.getText());
            }

            if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }

        return value.toString();
    }

    // map XML field to cheque object
    private void setChequeField(
            OutwardCheque cheque,
            String field,
            String value,
            File batchFolder) {

        if (equalsIgnoreCase(field, "ChequeNo", "ChequeNumber")) {
            cheque.setChequeNumber(value);

        } 
        else if (equalsIgnoreCase(field, "CityCode")) {
            cheque.setCityCode(value);

        } else if (equalsIgnoreCase(field, "BankCode")) {
            cheque.setBankCode(value);

        } else if (equalsIgnoreCase(field, "BranchCode")) {
            cheque.setBranchCode(value);

        } 
        else if (equalsIgnoreCase(
                field,
                "DrawerAccountNumber",
                "DrawerAccountNo",
                "AccountNumber",
                "AccountNo")) {
            cheque.setDrawerAccountNumber(value);

        } 
        else if (equalsIgnoreCase(field, "DrawerName")) {
            cheque.setDrawerName(value);

        } else if (equalsIgnoreCase(
                field,
                "PayeeAccountNumber",
                "PayeeAccountNo")) {
            cheque.setPayeeAccountNumber(value);

        } else if (equalsIgnoreCase(field, "PayeeName", "Payee")) {
            cheque.setPayeeName(value);

        } else if (equalsIgnoreCase(field, "Amount", "ChequeAmount")) {
            if (!value.isEmpty()) {
                try {
                    cheque.setAmount(new BigDecimal(value));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Invalid cheque amount: " + value, e);
                }
            }

        } else if (equalsIgnoreCase(field, "AmountInWords", "AmountWords")) {
            cheque.setAmountInWords(value);

        } else if (equalsIgnoreCase(field, "ChequeDate")) {
            if (!value.isEmpty()) {
                cheque.setChequeDate(parseDate(value));
            }

        } 
        else if (equalsIgnoreCase(
                field,
                "FrontImagePath",
                "FrontImage",
                "FrontFile")) {
            cheque.setFrontImagePath(resolveImagePath(value, batchFolder));

        } else if (equalsIgnoreCase(
                field,
                "BackImagePath",
                "BackImage",
                "BackFile")) {
            cheque.setBackImagePath(resolveImagePath(value, batchFolder));

        } else if (equalsIgnoreCase(field, "ChequeStatus")) {
            cheque.setChequeStatus(value);
        }
    }

    // parse cheque date using supported formats
    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String dateValue = value.trim();

        try {
            return LocalDate.parse(dateValue);
        } catch (Exception ignored) {
        }

        try {
            return LocalDate.parse(
                    dateValue,
                    DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (Exception ignored) {
        }

        try {
            return LocalDate.parse(
                    dateValue,
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception ignored) {
        }

        try {
            return LocalDate.parse(
                    dateValue,
                    DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        } catch (Exception ignored) {
        }

        throw new IllegalArgumentException("Invalid cheque date: " + value);
    }

    // resolve cheque image path
    private String resolveImagePath(String value, File batchFolder) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String imageValue = value.trim();

        if (imageValue.startsWith("http://")
                || imageValue.startsWith("https://")) {
            return imageValue;
        }

        imageValue = imageValue.replace("\\", "/");

        while (imageValue.startsWith("/")) {
            imageValue = imageValue.substring(1);
        }

        if (imageValue.matches("(?i)^outward/images/.*")) {
            imageValue = imageValue.replaceFirst(
                    "(?i)^outward/images/",
                    "zul/outward/images/");

        } else if (imageValue.matches("(?i)^outward/Images/.*")) {
            imageValue = imageValue.replaceFirst(
                    "(?i)^outward/Images/",
                    "zul/outward/images/");

        } else if (imageValue.matches("(?i)^Batch1/.*")) {
            imageValue = "zul/outward/images/" + imageValue;

        } else if (!imageValue.matches("(?i)^zul/outward/images/.*")) {
        }

        return "/" + imageValue;
    }

    // compare field names without case sensitivity
    private boolean equalsIgnoreCase(String value, String... expected) {
        if (value == null) {
            return false;
        }

        for (String item : expected) {
            if (value.equalsIgnoreCase(item)) {
                return true;
            }
        }

        return false;
    }
}