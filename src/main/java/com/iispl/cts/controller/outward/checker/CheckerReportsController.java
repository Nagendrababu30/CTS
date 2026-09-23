package com.iispl.cts.controller.outward.checker;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;

import com.iispl.cts.model.outward.OutwardBatch;
import com.iispl.cts.model.outward.OutwardCheque;
import com.iispl.cts.service.outward.checker.CheckerReportsService;

public class CheckerReportsController
        extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    @Wire
    private Listbox reportList;

    private CheckerReportsService service =
            new CheckerReportsService();

    @Override
    public void doAfterCompose(Component component)
            throws Exception {

        super.doAfterCompose(component);

        loadReportBatches();
    }

    // ================================================================
    // GET ARCHIVE DIRECTORY
    // ================================================================
   
    
    
    private Path getArchiveDirectory(
            String folderName) throws Exception {

        Path projectDirectory =
                Paths.get(System.getProperty("user.dir"));

        Path archiveDirectory =
                projectDirectory
                        .resolve("src")
                        .resolve("main")
                        .resolve("webapp")
                        .resolve("css")
                        .resolve("outward")
                        .resolve("Archive")
                        .resolve(folderName);

        Files.createDirectories(archiveDirectory);

        System.out.println("========================================");
        System.out.println("ARCHIVE DIRECTORY");
        System.out.println("Folder    : " + folderName);
        System.out.println("Full Path : "
                + archiveDirectory.toAbsolutePath());
        System.out.println("Exists    : "
                + Files.exists(archiveDirectory));
        System.out.println("========================================");

        return archiveDirectory;
    }
    private void loadReportBatches() {

        try {

            List<OutwardBatch> batches =
                    service.getCheckerCompletedBatches();

            reportList.getItems().clear();

            for (OutwardBatch batch : batches) {

                String batchNumber =
                        batch.getBatchNumber();

                Listitem item =
                        new Listitem();

                // =====================================================
                // BATCH NUMBER
                // =====================================================

                Listcell batchCell =
                        new Listcell();

                Label batchLabel =
                        new Label(batchNumber);

                batchCell.appendChild(batchLabel);

                item.appendChild(batchCell);

                // =====================================================
                // TOTAL CHEQUES
                // =====================================================

                Listcell totalCell =
                        new Listcell(
                                String.valueOf(
                                        batch.getNumberOfCheques()));

                item.appendChild(totalCell);

                // =====================================================
                // CFX / VALID XML
                // =====================================================

                Listcell validCell =
                        new Listcell();

                Button validButton =
                        new Button();

                validButton.setLabel(
                        "Download XML");

                validButton.setSclass(
                        "primary-button");

                validButton.addEventListener(
                        "onClick",
                        event ->
                                downloadValidXml(
                                        batchNumber));

                validCell.appendChild(
                        validButton);

                item.appendChild(validCell);

                // =====================================================
                // RRF / REJECTED XML
                // =====================================================

                Listcell rejectedCell =
                        new Listcell();

                Button rejectedButton =
                        new Button();

                rejectedButton.setLabel(
                        "Download XML");

                rejectedButton.setSclass(
                        "secondary-button");

                rejectedButton.setDisabled(
                        !service.isRrfAvailable(
                                batchNumber));

                rejectedButton.addEventListener(
                        "onClick",
                        event ->
                                downloadRejectedXml(
                                        batchNumber));

                rejectedCell.appendChild(
                        rejectedButton);

                item.appendChild(rejectedCell);

                reportList.appendChild(item);
            }

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to load report batches.\n\n"
                            + e.getMessage(),
                    "Checker Reports",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    // ================================================================
    // DOWNLOAD VALID XML
    // ================================================================

    private void downloadValidXml(
            String batchNumber) {

        System.out.println();
        System.out.println();
        System.out.println(
                "================================================");
        System.out.println(
                "          VALID XML DOWNLOAD STARTED");
        System.out.println(
                "================================================");
        System.out.println(
                "Batch number:");
        System.out.println(
                batchNumber);

        try {

            // =========================================================
            // STEP 1 - GET VALID CHEQUES
            // =========================================================

            System.out.println();
            System.out.println(
                    "STEP 1 -> Getting valid cheques");

            List<OutwardCheque> cheques =
                    service.getValidCheques(
                            batchNumber);

            System.out.println(
                    "Valid cheque count:");

            System.out.println(
                    cheques == null
                            ? "NULL"
                            : cheques.size());

            // =========================================================
            // STEP 2 - BUILD XML
            // =========================================================

            System.out.println();
            System.out.println(
                    "STEP 2 -> Building valid XML");

            String xml =
                    buildValidXml(
                            batchNumber,
                            cheques);

            if (xml == null) {

                System.out.println(
                        "XML is NULL. Nothing to download.");

                return;
            }

            System.out.println(
                    "XML generated successfully.");

            System.out.println(
                    "XML size:");

            System.out.println(
                    xml.length());

            byte[] xmlBytes =
                    xml.getBytes(
                            StandardCharsets.UTF_8);

            String fileName =
                    batchNumber + ".xml";

            System.out.println(
                    "File name:");

            System.out.println(
                    fileName);

            // =========================================================
            // STEP 3 - SAVE XML TO SERVER ARCHIVE
            // =========================================================

            System.out.println();
            System.out.println(
                    "STEP 3 -> Saving XML to ValidCheques archive");

            Path archiveDirectory =
                    getArchiveDirectory("ValidCheques");

            Path filePath =
                    archiveDirectory.resolve(fileName);

            Files.write(
                    filePath,
                    xmlBytes);


            System.out.println(
                    "XML archived successfully.");

            System.out.println(
                    "XML archive location:");

            System.out.println(
                    filePath.toAbsolutePath());

            // =========================================================
            // STEP 4 - DOWNLOAD XML TO BROWSER
            // =========================================================

            System.out.println();
            System.out.println(
                    "STEP 4 -> Downloading XML to browser");

            Filedownload.save(
                    xmlBytes,
                    "application/xml",
                    fileName);

            System.out.println(
                    "Browser download triggered.");

            // =========================================================
            // STEP 5 - SAVE NPCI INFORMATION
            // =========================================================

            System.out.println();
            System.out.println(
                    "STEP 5 -> Saving NPCI submission information");

            int validChequeCount =
                    cheques.size();

            int totalChequeCount =
                    service.getBatchCheques(
                            batchNumber)
                            .size();

            int invalidChequeCount =
                    totalChequeCount
                            - validChequeCount;

            System.out.println(
                    "Total cheques:");

            System.out.println(
                    totalChequeCount);

            System.out.println(
                    "Valid cheques:");

            System.out.println(
                    validChequeCount);

            System.out.println(
                    "Invalid cheques:");

            System.out.println(
                    invalidChequeCount);

            boolean saved =
                    service.saveNPCISubmission(
                            batchNumber,
                            validChequeCount,
                            invalidChequeCount,
                            fileName);

            System.out.println(
                    "NPCI submission saved:");

            System.out.println(
                    saved);

            // =========================================================
            // FINAL
            // =========================================================

            System.out.println();
            System.out.println(
                    "================================================");
            System.out.println(
                    "          VALID XML COMPLETED");
            System.out.println(
                    "================================================");

            if (saved) {

                Messagebox.show(
                        "Valid Cheques XML downloaded successfully "
                                + "and archived in ValidCheques.",
                        "Checker Reports",
                        Messagebox.OK,
                        Messagebox.INFORMATION);

            } else {

                Messagebox.show(
                        "Valid Cheques XML downloaded successfully, "
                                + "but NPCI submission details could not be saved.",
                        "Checker Reports",
                        Messagebox.OK,
                        Messagebox.EXCLAMATION);
            }

        } catch (Exception e) {

            System.out.println();
            System.out.println(
                    "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println(
                    "       VALID XML FAILED");
            System.out.println(
                    "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

            System.out.println(
                    "Exception:");

            System.out.println(
                    e.getClass().getName());

            System.out.println(
                    "Message:");

            System.out.println(
                    e.getMessage());

            e.printStackTrace();

            System.out.println(
                    "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

            Messagebox.show(
                    "Unable to download Valid XML.\n\n"
                            + e.getMessage(),
                    "Checker Reports",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    // ================================================================
    // DOWNLOAD REJECTED XML
    // RRF
    //
    // Only rejected cheques are included.
    // This XML is NOT sent to NPCI.
    // It is the RRF file.
    // ================================================================

    private void downloadRejectedXml(
            String batchNumber) {

        System.out.println();
        System.out.println();
        System.out.println(
                "================================================");
        System.out.println(
                "        REJECTED XML DOWNLOAD STARTED");
        System.out.println(
                "================================================");

        System.out.println(
                "Batch number:");

        System.out.println(
                batchNumber);

        try {

            // =========================================================
            // STEP 1 - GET RRF CHEQUES
            // =========================================================

            System.out.println();
            System.out.println(
                    "STEP 1 -> Getting rejected cheques");

            List<OutwardCheque> cheques =
                    service.getRrfCheques(
                            batchNumber);

            System.out.println(
                    "Rejected cheque count:");

            System.out.println(
                    cheques == null
                            ? "NULL"
                            : cheques.size());

            // =========================================================
            // STEP 2 - BUILD RRF XML
            // =========================================================

            System.out.println();
            System.out.println(
                    "STEP 2 -> Building RRF XML");

            String xml =
                    buildRejectedXml(
                            batchNumber,
                            cheques);

            if (xml == null) {

                System.out.println(
                        "RRF XML is NULL.");

                return;
            }

            byte[] xmlBytes =
                    xml.getBytes(
                            StandardCharsets.UTF_8);

            String fileName =
                    batchNumber + ".xml";

            // =========================================================
            // STEP 3 - SAVE RRF XML TO SERVER ARCHIVE
            // =========================================================

            System.out.println();
            System.out.println(
                    "STEP 3 -> Saving RRF XML to RejectedCheques archive");

            Path archiveDirectory =
                    getArchiveDirectory("RejectedCheques");

            Path filePath =
                    archiveDirectory.resolve(fileName);

            Files.write(
                    filePath,
                    xmlBytes);

            System.out.println(
                    "RRF XML archived successfully.");

            System.out.println(
                    "RRF XML archive location:");

            System.out.println(
                    filePath.toAbsolutePath());

            // =========================================================
            // STEP 4 - DOWNLOAD RRF TO BROWSER
            // =========================================================

            System.out.println();
            System.out.println(
                    "STEP 4 -> Downloading RRF to browser");

            Filedownload.save(
                    xmlBytes,
                    "application/xml",
                    fileName);

            System.out.println(
                    "Browser download triggered.");

            System.out.println();
            System.out.println(
                    "================================================");
            System.out.println(
                    "        REJECTED XML COMPLETED");
            System.out.println(
                    "================================================");

            Messagebox.show(
                    "Rejected XML downloaded successfully "
                            + "and archived in RejectedCheques.",
                    "Checker Reports",
                    Messagebox.OK,
                    Messagebox.INFORMATION);

        } catch (IllegalStateException e) {

            System.out.println(
                    "RRF is not available.");

            Messagebox.show(
                    "RRF is not available for this batch.",
                    "Checker Reports",
                    Messagebox.OK,
                    Messagebox.INFORMATION);

        } catch (Exception e) {

            System.out.println();
            System.out.println(
                    "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println(
                    "       RRF XML FAILED");
            System.out.println(
                    "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

            System.out.println(
                    "Exception:");

            System.out.println(
                    e.getClass().getName());

            System.out.println(
                    "Message:");

            System.out.println(
                    e.getMessage());

            e.printStackTrace();

            System.out.println(
                    "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

            Messagebox.show(
                    "Unable to download RRF XML.\n\n"
                            + e.getMessage(),
                    "Checker Reports",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }

    // ================================================================
    // BUILD VALID XML
    // ================================================================

    private String buildValidXml(
            String batchNumber,
            List<OutwardCheque> cheques) {

        StringBuilder xml =
                new StringBuilder();

        int validCount = 0;

        for (OutwardCheque cheque : cheques) {

            if (cheque != null &&
                    "CHECKER_ACCEPTED".equalsIgnoreCase(
                            cheque.getChequeStatus())) {

                validCount++;
            }
        }

        if (validCount == 0) {

            Messagebox.show(
                    "Valid XML is not available for batch "
                            + batchNumber
                            + ".",
                    "Checker Reports",
                    Messagebox.OK,
                    Messagebox.INFORMATION);

            return null;
        }

        xml.append(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

        xml.append(
                "<ValidChequesReport>\n");

        xml.append(
                "    <BatchNumber>")
                .append(xmlValue(batchNumber))
                .append("</BatchNumber>\n");

        xml.append(
                "    <TotalValidCheques>")
                .append(validCount)
                .append("</TotalValidCheques>\n");

        xml.append(
                "    <Cheques>\n");

        for (OutwardCheque cheque : cheques) {

            if (!"CHECKER_ACCEPTED".equalsIgnoreCase(
                    cheque.getChequeStatus())) {

                continue;
            }

            appendChequeXml(
                    xml,
                    cheque,
                    false);
        }

        xml.append(
                "    </Cheques>\n");

        xml.append(
                "</ValidChequesReport>\n");

        return xml.toString();
    }

    // ================================================================
    // BUILD RRF XML
    // ================================================================

    private String buildRejectedXml(
            String batchNumber,
            List<OutwardCheque> cheques) {

        StringBuilder xml =
                new StringBuilder();

        int rejectedCount =
                cheques.size();

        if (rejectedCount == 0) {

            Messagebox.show(
                    "Rejected XML is not available for batch "
                            + batchNumber
                            + ".",
                    "Checker Reports",
                    Messagebox.OK,
                    Messagebox.INFORMATION);

            return null;
        }

        xml.append(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

        xml.append(
                "<RejectedChequesReport>\n");

        xml.append(
                "    <BatchNumber>")
                .append(xmlValue(batchNumber))
                .append("</BatchNumber>\n");

        xml.append(
                "    <TotalRejectedCheques>")
                .append(rejectedCount)
                .append("</TotalRejectedCheques>\n");

        xml.append(
                "    <Cheques>\n");

        for (OutwardCheque cheque : cheques) {

            appendChequeXml(
                    xml,
                    cheque,
                    true);
        }

        xml.append(
                "    </Cheques>\n");

        xml.append(
                "</RejectedChequesReport>\n");

        return xml.toString();
    }

    // ================================================================
    // APPEND CHEQUE XML
    // ================================================================

    private void appendChequeXml(
            StringBuilder xml,
            OutwardCheque cheque,
            boolean rejected) {

        xml.append(
                "        <Cheque>\n");

        xml.append(
                "            <ChequeNumber>")
                .append(xmlValue(
                        cheque.getChequeNumber()))
                .append("</ChequeNumber>\n");

        xml.append(
                "            <BatchNumber>")
                .append(xmlValue(
                        cheque.getBatchNumber()))
                .append("</BatchNumber>\n");

        xml.append(
                "            <ChequeDate>")
                .append(xmlValue(
                        cheque.getChequeDate()))
                .append("</ChequeDate>\n");

        xml.append(
                "            <CityCode>")
                .append(xmlValue(
                        cheque.getCityCode()))
                .append("</CityCode>\n");

        xml.append(
                "            <BankCode>")
                .append(xmlValue(
                        cheque.getBankCode()))
                .append("</BankCode>\n");

        xml.append(
                "            <BranchCode>")
                .append(xmlValue(
                        cheque.getBranchCode()))
                .append("</BranchCode>\n");

        xml.append(
                "            <DrawerAccountNumber>")
                .append(xmlValue(
                        cheque.getDrawerAccountNumber()))
                .append("</DrawerAccountNumber>\n");

        xml.append(
                "            <DrawerName>")
                .append(xmlValue(
                        cheque.getDrawerName()))
                .append("</DrawerName>\n");

        xml.append(
                "            <PayeeName>")
                .append(xmlValue(
                        cheque.getPayeeName()))
                .append("</PayeeName>\n");

        xml.append(
                "            <PayeeAccountNumber>")
                .append(xmlValue(
                        cheque.getPayeeAccountNumber()))
                .append("</PayeeAccountNumber>\n");

        xml.append(
                "            <Amount>")
                .append(xmlValue(
                        cheque.getAmount()))
                .append("</Amount>\n");

        xml.append(
                "            <AmountInWords>")
                .append(xmlValue(
                        cheque.getAmountInWords()))
                .append("</AmountInWords>\n");

        xml.append(
                "            <ChequeStatus>")
                .append(xmlValue(
                        cheque.getChequeStatus()))
                .append("</ChequeStatus>\n");

        if (rejected) {

            xml.append(
                    "            <ReturnReasonId>")
                    .append(xmlValue(
                            cheque.getReturnReasonId()))
                    .append("</ReturnReasonId>\n");

            xml.append(
                    "            <CheckerRemarks>")
                    .append(xmlValue(
                            cheque.getCheckerRemarks()))
                    .append("</CheckerRemarks>\n");
        }

        xml.append(
                "        </Cheque>\n");
    }

    // ================================================================
    // XML VALUE
    // ================================================================

    private String xmlValue(Object value) {

        if (value == null) {
            return "";
        }

        String text =
                String.valueOf(value);

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    // ================================================================
    // REFRESH
    // ================================================================

    @Listen("onClick = #refreshReportBtn")
    public void refreshReports() {

        loadReportBatches();
    }
}