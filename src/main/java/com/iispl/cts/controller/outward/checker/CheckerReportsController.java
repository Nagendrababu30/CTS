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
    // GET PROJECT ARCHIVE DIRECTORY
    // ================================================================

    private Path getArchiveDirectory(String folderName)
            throws Exception {

        /*
         * Get the deployed web application path.
         *
         * Example:
         * C:\Users\ginja\eclipse-workspace\.metadata\
         * .plugins\org.eclipse.wst.server.core\tmp1\
         * wtpwebapps\CTS_OUTWARD
         */
        String deployedPath = Executions.getCurrent()
                .getDesktop()
                .getWebApp()
                .getRealPath("/");

        if (deployedPath == null) {
            throw new Exception(
                    "Unable to determine deployed application path.");
        }

        Path deployedRoot =
                Paths.get(deployedPath).toAbsolutePath().normalize();

        System.out.println();
        System.out.println("========================================");
        System.out.println("PROJECT ARCHIVE LOCATION");
        System.out.println("========================================");
        System.out.println("Deployed application:");
        System.out.println(deployedRoot);

        /*
         * The Eclipse WTP deployed application is normally:
         *
         * <workspace>\.metadata\.plugins\
         * org.eclipse.wst.server.core\tmp1\wtpwebapps\
         * <project>
         *
         * We need to go back to the Eclipse workspace.
         */

        Path workspaceRoot = deployedRoot;

        while (workspaceRoot != null
                && workspaceRoot.getParent() != null) {

            Path fileName =
                    workspaceRoot.getFileName();

            if (fileName != null
                    && ".metadata".equalsIgnoreCase(
                            fileName.toString())) {

                workspaceRoot =
                        workspaceRoot.getParent();

                break;
            }

            workspaceRoot =
                    workspaceRoot.getParent();
        }

        if (workspaceRoot == null) {
            throw new Exception(
                    "Unable to locate Eclipse workspace.");
        }

        System.out.println("Eclipse workspace:");
        System.out.println(workspaceRoot);

        /*
         * Get project name from deployed application.
         */
        Path projectNamePath =
                deployedRoot.getFileName();

        if (projectNamePath == null) {
            throw new Exception(
                    "Unable to determine project name.");
        }

        String projectName =
                projectNamePath.toString();

        System.out.println("Project name:");
        System.out.println(projectName);

        /*
         * Source project directory:
         *
         * <workspace>\<project>
         */
        Path projectRoot =
                workspaceRoot.resolve(projectName);

        System.out.println("Project root:");
        System.out.println(projectRoot);

        /*
         * Final location:
         *
         * <project>
         *   \src
         *     \main
         *       \webapp
         *         \css
         *           \outward
         *             \Archive
         *               \<folderName>
         */
        Path archiveDirectory =
                projectRoot.resolve(
                        Paths.get(
                                "src",
                                "main",
                                "webapp",
                                "css",
                                "outward",
                                "Archive",
                                folderName));

        Files.createDirectories(
                archiveDirectory);

        System.out.println("Archive directory:");
        System.out.println(
                archiveDirectory.toAbsolutePath());

        System.out.println("========================================");

        return archiveDirectory;
    }

    // ================================================================
    // LOAD REPORT BATCHES
    // ================================================================

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

                batchCell.appendChild(
                        batchLabel);

                item.appendChild(
                        batchCell);

                // =====================================================
                // TOTAL CHEQUES
                // =====================================================

                Listcell totalCell =
                        new Listcell(
                                String.valueOf(
                                        batch.getNumberOfCheques()));

                item.appendChild(
                        totalCell);

                // =====================================================
                // VALID XML
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

                item.appendChild(
                        validCell);

                // =====================================================
                // REJECTED XML
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

                item.appendChild(
                        rejectedCell);

                reportList.appendChild(
                        item);
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
            // STEP 3 - SAVE XML INSIDE PROJECT
            // =========================================================

            System.out.println();
            System.out.println(
                    "STEP 3 -> Saving XML internally");

            Path archiveDirectory =
                    getArchiveDirectory(
                            "ValidCheques");

            Path internalFilePath =
                    archiveDirectory.resolve(
                            fileName);

            Files.write(
                    internalFilePath,
                    xmlBytes);

            System.out.println(
                    "XML saved internally.");

            System.out.println(
                    "Internal file path:");

            System.out.println(
                    internalFilePath
                            .toAbsolutePath());

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
            // STEP 1 - GET REJECTED CHEQUES
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
            // STEP 3 - SAVE RRF XML INSIDE PROJECT
            // =========================================================

            System.out.println();
            System.out.println(
                    "STEP 3 -> Saving RRF XML internally");

            Path archiveDirectory =
                    getArchiveDirectory(
                            "RejectedCheques");

            Path internalFilePath =
                    archiveDirectory.resolve(
                            fileName);

            Files.write(
                    internalFilePath,
                    xmlBytes);

            System.out.println(
                    "RRF XML saved internally.");

            System.out.println(
                    "Internal file path:");

            System.out.println(
                    internalFilePath
                            .toAbsolutePath());

            // =========================================================
            // STEP 4 - DOWNLOAD RRF
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

            if (cheque != null
                    && "CHECKER_ACCEPTED"
                            .equalsIgnoreCase(
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
                .append(
                        xmlValue(batchNumber))
                .append(
                        "</BatchNumber>\n");

        xml.append(
                "    <TotalValidCheques>")
                .append(
                        validCount)
                .append(
                        "</TotalValidCheques>\n");

        xml.append(
                "    <Cheques>\n");

        for (OutwardCheque cheque : cheques) {

            if (!"CHECKER_ACCEPTED"
                    .equalsIgnoreCase(
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
                .append(
                        xmlValue(batchNumber))
                .append(
                        "</BatchNumber>\n");

        xml.append(
                "    <TotalRejectedCheques>")
                .append(
                        rejectedCount)
                .append(
                        "</TotalRejectedCheques>\n");

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
                .append(
                        xmlValue(
                                cheque.getChequeNumber()))
                .append(
                        "</ChequeNumber>\n");

        xml.append(
                "            <BatchNumber>")
                .append(
                        xmlValue(
                                cheque.getBatchNumber()))
                .append(
                        "</BatchNumber>\n");

        xml.append(
                "            <ChequeDate>")
                .append(
                        xmlValue(
                                cheque.getChequeDate()))
                .append(
                        "</ChequeDate>\n");

        xml.append(
                "            <CityCode>")
                .append(
                        xmlValue(
                                cheque.getCityCode()))
                .append(
                        "</CityCode>\n");

        xml.append(
                "            <BankCode>")
                .append(
                        xmlValue(
                                cheque.getBankCode()))
                .append(
                        "</BankCode>\n");

        xml.append(
                "            <BranchCode>")
                .append(
                        xmlValue(
                                cheque.getBranchCode()))
                .append(
                        "</BranchCode>\n");

        xml.append(
                "            <DrawerAccountNumber>")
                .append(
                        xmlValue(
                                cheque.getDrawerAccountNumber()))
                .append(
                        "</DrawerAccountNumber>\n");

        xml.append(
                "            <DrawerName>")
                .append(
                        xmlValue(
                                cheque.getDrawerName()))
                .append(
                        "</DrawerName>\n");

        xml.append(
                "            <PayeeName>")
                .append(
                        xmlValue(
                                cheque.getPayeeName()))
                .append(
                        "</PayeeName>\n");

        xml.append(
                "            <PayeeAccountNumber>")
                .append(
                        xmlValue(
                                cheque.getPayeeAccountNumber()))
                .append(
                        "</PayeeAccountNumber>\n");

        xml.append(
                "            <Amount>")
                .append(
                        xmlValue(
                                cheque.getAmount()))
                .append(
                        "</Amount>\n");

        xml.append(
                "            <AmountInWords>")
                .append(
                        xmlValue(
                                cheque.getAmountInWords()))
                .append(
                        "</AmountInWords>\n");

        xml.append(
                "            <ChequeStatus>")
                .append(
                        xmlValue(
                                cheque.getChequeStatus()))
                .append(
                        "</ChequeStatus>\n");

        if (rejected) {

            xml.append(
                    "            <ReturnReasonId>")
                    .append(
                            xmlValue(
                                    cheque.getReturnReasonId()))
                    .append(
                            "</ReturnReasonId>\n");

            xml.append(
                    "            <CheckerRemarks>")
                    .append(
                            xmlValue(
                                    cheque.getCheckerRemarks()))
                    .append(
                            "</CheckerRemarks>\n");
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