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

    // =========================================================
    // PARSE XML
    // =========================================================

    public List<OutwardCheque> parse(
            File xmlFile,
            String batchNumber,
            File batchFolder) throws Exception {

        List<OutwardCheque> cheques = new ArrayList<>();

        if (xmlFile == null || !xmlFile.exists() || !xmlFile.isFile()) {
            throw new IllegalArgumentException(
                    "XML file does not exist: "
                            + (xmlFile == null
                                    ? "null"
                                    : xmlFile.getAbsolutePath()));
        }

        if (batchNumber == null || batchNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch number is required.");
        }

        if (batchFolder == null
                || !batchFolder.exists()
                || !batchFolder.isDirectory()) {

            throw new IllegalArgumentException(
                    "Batch folder does not exist: "
                            + (batchFolder == null
                                    ? "null"
                                    : batchFolder.getAbsolutePath()));
        }

        // ---------------------------------------------------------
        // Create secure StAX parser
        // ---------------------------------------------------------

        XMLInputFactory factory = XMLInputFactory.newFactory();

        try {
            factory.setProperty(
                    XMLInputFactory.SUPPORT_DTD,
                    false);
        } catch (IllegalArgumentException ignored) {
        }

        try {
            factory.setProperty(
                    "javax.xml.stream.isSupportingExternalEntities",
                    false);
        } catch (IllegalArgumentException ignored) {
        }

        // ---------------------------------------------------------
        // Read XML
        // ---------------------------------------------------------

        try (InputStream input =
                     new FileInputStream(xmlFile)) {

            XMLStreamReader reader =
                    factory.createXMLStreamReader(input);

            while (reader.hasNext()) {

                int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {

                    String element =
                            reader.getLocalName();

                    // XML should contain <Cheque>
                    if (equalsIgnoreCase(element, "Cheque")) {

                        OutwardCheque cheque =
                                parseCheque(
                                        reader,
                                        batchNumber,
                                        batchFolder);

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

    // =========================================================
    // PARSE ONE CHEQUE
    // =========================================================

    private OutwardCheque parseCheque(
            XMLStreamReader reader,
            String batchNumber,
            File batchFolder) throws Exception {

        OutwardCheque cheque =
                new OutwardCheque();

        // IMPORTANT:
        // Every cheque gets the SAME generated batch number.
        cheque.setBatchNumber(batchNumber);

        while (reader.hasNext()) {

            int event = reader.next();

            // -------------------------------------------------
            // END CHEQUE
            // -------------------------------------------------

            if (event == XMLStreamConstants.END_ELEMENT) {

                if (equalsIgnoreCase(
                        reader.getLocalName(),
                        "Cheque")) {

                    break;
                }

                continue;
            }

            // -------------------------------------------------
            // Ignore anything other than XML elements
            // -------------------------------------------------

            if (event != XMLStreamConstants.START_ELEMENT) {
                continue;
            }

            String field =
                    reader.getLocalName();

            String value =
                    readElementText(reader);

            if (value == null) {
                value = "";
            }

            value = value.trim();

            setChequeField(
                    cheque,
                    field,
                    value,
                    batchFolder);
        }

        return cheque;
    }

    // =========================================================
    // READ ELEMENT TEXT
    // =========================================================

    private String readElementText(
            XMLStreamReader reader) throws Exception {

        StringBuilder value =
                new StringBuilder();

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

    // =========================================================
    // MAP XML FIELD TO OUTWARD CHEQUE
    // =========================================================

    private void setChequeField(
            OutwardCheque cheque,
            String field,
            String value,
            File batchFolder) {

        // -----------------------------------------------------
        // CHEQUE NUMBER
        // -----------------------------------------------------

        if (equalsIgnoreCase(
                field,
                "ChequeNo",
                "ChequeNumber")) {

            cheque.setChequeNumber(value);
        }

        // -----------------------------------------------------
        // CITY CODE
        // -----------------------------------------------------

        else if (equalsIgnoreCase(
                field,
                "CityCode")) {

            cheque.setCityCode(value);
        }

        // -----------------------------------------------------
        // BANK CODE
        // -----------------------------------------------------

        else if (equalsIgnoreCase(
                field,
                "BankCode")) {

            cheque.setBankCode(value);
        }

        // -----------------------------------------------------
        // BRANCH CODE
        // -----------------------------------------------------

        else if (equalsIgnoreCase(
                field,
                "BranchCode")) {

            cheque.setBranchCode(value);
        }

        // -----------------------------------------------------
        // DRAWER ACCOUNT
        // -----------------------------------------------------

        else if (equalsIgnoreCase(
                field,
                "DrawerAccountNumber",
                "DrawerAccountNo",
                "AccountNumber",
                "AccountNo")) {

            cheque.setDrawerAccountNumber(value);
        }

        // -----------------------------------------------------
        // DRAWER NAME
        // -----------------------------------------------------

        else if (equalsIgnoreCase(
                field,
                "DrawerName")) {

            cheque.setDrawerName(value);
        }

        // -----------------------------------------------------
        // DEPOSITOR ACCOUNT
        // -----------------------------------------------------

        else if (equalsIgnoreCase(
                field,
                "DepositorAccountNumber",
                "DepositorAccountNo")) {

            cheque.setDepositorAccountNumber(value);
        }

        // -----------------------------------------------------
        // DEPOSITOR NAME
        // -----------------------------------------------------

        else if (equalsIgnoreCase(
                field,
                "DepositorName")) {

            cheque.setDepositorName(value);
        }

        // -----------------------------------------------------
        // PAYEE NAME
        // -----------------------------------------------------

        else if (equalsIgnoreCase(
                field,
                "PayeeName",
                "Payee")) {

            cheque.setPayeeName(value);
        }

        // -----------------------------------------------------
        // AMOUNT
        // -----------------------------------------------------

        else if (equalsIgnoreCase(
                field,
                "Amount",
                "ChequeAmount")) {

            if (!value.isEmpty()) {

                try {

                    cheque.setAmount(
                            new BigDecimal(value));

                } catch (NumberFormatException e) {

                    throw new IllegalArgumentException(
                            "Invalid cheque amount: " + value,
                            e);
                }
            }
        }

        // -----------------------------------------------------
        // AMOUNT IN WORDS
        // -----------------------------------------------------

        else if (equalsIgnoreCase(
                field,
                "AmountInWords",
                "AmountWords")) {

            cheque.setAmountInWords(value);
        }

        // -----------------------------------------------------
        // CHEQUE DATE
        // -----------------------------------------------------

        else if (equalsIgnoreCase(
                field,
                "ChequeDate")) {

            if (!value.isEmpty()) {

                cheque.setChequeDate(
                        parseDate(value));
            }
        }

        // -----------------------------------------------------
        // FRONT IMAGE
        // -----------------------------------------------------

        else if (equalsIgnoreCase(
                field,
                "FrontImagePath",
                "FrontImage",
                "FrontFile")) {

            cheque.setFrontImagePath(
                    resolveImagePath(
                            value,
                            batchFolder));
        }

        // -----------------------------------------------------
        // BACK IMAGE
        // -----------------------------------------------------

        else if (equalsIgnoreCase(
                field,
                "BackImagePath",
                "BackImage",
                "BackFile")) {

            cheque.setBackImagePath(
                    resolveImagePath(
                            value,
                            batchFolder));
        }

        // -----------------------------------------------------
        // CHEQUE STATUS
        // -----------------------------------------------------

        else if (equalsIgnoreCase(
                field,
                "ChequeStatus")) {

            cheque.setChequeStatus(value);
        }
    }

    // =========================================================
    // DATE
    // =========================================================

    private LocalDate parseDate(String value) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String dateValue = value.trim();

        // yyyy-MM-dd
        try {
            return LocalDate.parse(dateValue);
        } catch (Exception ignored) {
        }

        // dd-MM-yyyy
        try {
            return LocalDate.parse(
                    dateValue,
                    DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (Exception ignored) {
        }

        // dd/MM/yyyy
        try {
            return LocalDate.parse(
                    dateValue,
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception ignored) {
        }

        // MM/dd/yyyy
        try {
            return LocalDate.parse(
                    dateValue,
                    DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        } catch (Exception ignored) {
        }

        // Invalid date
        throw new IllegalArgumentException(
                "Invalid cheque date: " + value);
    }

    // =========================================================
    // IMAGE PATH
    // =========================================================

    private String resolveImagePath(
            String value,
            File batchFolder) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String imageValue = value.trim();

        // -----------------------------------------------------
        // IMPORTANT:
        // If XML contains an HTTP/HTTPS URL,
        // keep it exactly as a URL.
        // -----------------------------------------------------

        if (imageValue.startsWith("http://")
                || imageValue.startsWith("https://")) {

            return imageValue;
        }

        // -----------------------------------------------------
        // If XML contains absolute local path
        // -----------------------------------------------------

        File image = new File(imageValue);

        if (image.isAbsolute()) {
            return image.getAbsolutePath();
        }

        // -----------------------------------------------------
        // Otherwise image is inside batch folder
        // -----------------------------------------------------

        image = new File(
                batchFolder,
                imageValue);

        return image.getAbsolutePath();
    }

    // =========================================================
    // STRING COMPARISON
    // =========================================================

    private boolean equalsIgnoreCase(
            String value,
            String... expected) {

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