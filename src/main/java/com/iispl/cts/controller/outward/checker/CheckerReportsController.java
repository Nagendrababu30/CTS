package com.iispl.cts.controller.outward.checker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Filedownload;

import com.iispl.cts.dao.outward.checker.CheckerChequeDAO;
import com.iispl.cts.data.CTSStaticData;
import com.iispl.cts.model.outward.OutwardCheque;

public class CheckerReportsController extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    @Wire
    private Listbox reportList;

    private CheckerChequeDAO checkerChequeDAO =
            new CheckerChequeDAO();


    @Override
    public void doAfterCompose(Component component)
            throws Exception {

        super.doAfterCompose(component);

        loadReportBatches();
    }


    private void loadReportBatches() {

        String sql =
                "SELECT ob.batch_number, " +
                "       COUNT(oc.cheque_number) AS total_cheques, " +
                "       COUNT(CASE WHEN UPPER(oc.cheque_status) = 'CHECKER_ACCEPTED' " +
                "                  THEN 1 END) AS valid_cheques, " +
                "       COUNT(CASE WHEN UPPER(oc.cheque_status) = 'CHECKER_REJECTED' " +
                "                  THEN 1 END) AS rejected_cheques " +
                "FROM outward_batch ob " +
                "LEFT JOIN outward_cheque oc " +
                "       ON ob.batch_number = oc.batch_number " +
                "WHERE UPPER(ob.batch_status) = 'CHECKER_COMPLETED' " +
                "GROUP BY ob.batch_number " +
                "ORDER BY ob.batch_number DESC";

        try (Connection connection =
                     CTSStaticData.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs =
                     statement.executeQuery()) {

            reportList.getItems().clear();

            while (rs.next()) {

                String batchNumber =
                        rs.getString("batch_number");

                int totalCheques =
                        rs.getInt("total_cheques");

                int validCheques =
                        rs.getInt("valid_cheques");

                int rejectedCheques =
                        rs.getInt("rejected_cheques");

                Listitem item =
                        new Listitem();

                Listcell batchCell =
                        new Listcell();

                Label batchLabel =
                        new Label(batchNumber);

                batchCell.appendChild(batchLabel);

                item.appendChild(batchCell);


                Listcell totalCell =
                        new Listcell(
                                String.valueOf(
                                        totalCheques));

                item.appendChild(totalCell);


                Listcell validCell =
                        new Listcell();

                Button validButton =
                        new Button();

                validButton.setLabel(
                        "Download XML");

                validButton.setSclass(
                        "primary-button");

                validButton.setDisabled(
                        validCheques == 0);

                validButton.addEventListener(
                        "onClick",
                        event -> downloadValidXml(
                                batchNumber));

                validCell.appendChild(
                        validButton);

                item.appendChild(validCell);


                Listcell rejectedCell =
                        new Listcell();

                Button rejectedButton =
                        new Button();

                rejectedButton.setLabel(
                        "Download XML");

                rejectedButton.setSclass(
                        "secondary-button");

                rejectedButton.setDisabled(
                        rejectedCheques == 0);

                rejectedButton.addEventListener(
                        "onClick",
                        event -> downloadRejectedXml(
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


    private void downloadValidXml(
            String batchNumber) {

        try {

            List<OutwardCheque> cheques =
                    checkerChequeDAO.getChequesByBatch(
                            batchNumber);

            String xml =
                    buildValidXml(
                            batchNumber,
                            cheques);

            if (xml == null) {
                return;
            }

            String fileName =
                    "Valid_Cheques_"
                            + batchNumber
                            + ".xml";

            Filedownload.save(
                    xml,
                    "application/xml",
                    fileName);

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to download Valid XML.\n\n"
                            + e.getMessage(),
                    "Checker Reports",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }


    private void downloadRejectedXml(
            String batchNumber) {

        try {

            List<OutwardCheque> cheques =
                    checkerChequeDAO.getChequesByBatch(
                            batchNumber);

            String xml =
                    buildRejectedXml(
                            batchNumber,
                            cheques);

            if (xml == null) {
                return;
            }

            String fileName =
                    "Rejected_Cheques_"
                            + batchNumber
                            + ".xml";

            Filedownload.save(
                    xml,
                    "application/xml",
                    fileName);

        } catch (Exception e) {

            e.printStackTrace();

            Messagebox.show(
                    "Unable to download Rejected XML.\n\n"
                            + e.getMessage(),
                    "Checker Reports",
                    Messagebox.OK,
                    Messagebox.ERROR);
        }
    }


    private String buildValidXml(
            String batchNumber,
            List<OutwardCheque> cheques) {

        StringBuilder xml =
                new StringBuilder();

        int validCount = 0;

        for (OutwardCheque cheque : cheques) {

            if ("CHECKER_ACCEPTED".equalsIgnoreCase(
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

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

        xml.append("<ValidChequesReport>\n");

        xml.append("    <BatchNumber>")
                .append(xmlValue(batchNumber))
                .append("</BatchNumber>\n");

        xml.append("    <TotalValidCheques>")
                .append(validCount)
                .append("</TotalValidCheques>\n");

        xml.append("    <Cheques>\n");


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


        xml.append("    </Cheques>\n");

        xml.append("</ValidChequesReport>\n");

        return xml.toString();
    }


    private String buildRejectedXml(
            String batchNumber,
            List<OutwardCheque> cheques) {

        StringBuilder xml =
                new StringBuilder();

        int rejectedCount = 0;

        for (OutwardCheque cheque : cheques) {

            if ("CHECKER_REJECTED".equalsIgnoreCase(
                    cheque.getChequeStatus())) {

                rejectedCount++;
            }
        }

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

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

        xml.append("<RejectedChequesReport>\n");

        xml.append("    <BatchNumber>")
                .append(xmlValue(batchNumber))
                .append("</BatchNumber>\n");

        xml.append("    <TotalRejectedCheques>")
                .append(rejectedCount)
                .append("</TotalRejectedCheques>\n");

        xml.append("    <Cheques>\n");


        for (OutwardCheque cheque : cheques) {

            if (!"CHECKER_REJECTED".equalsIgnoreCase(
                    cheque.getChequeStatus())) {

                continue;
            }

            appendChequeXml(
                    xml,
                    cheque,
                    true);
        }


        xml.append("    </Cheques>\n");

        xml.append("</RejectedChequesReport>\n");

        return xml.toString();
    }


    private void appendChequeXml(
            StringBuilder xml,
            OutwardCheque cheque,
            boolean rejected) {

        xml.append("        <Cheque>\n");

        xml.append("            <ChequeNumber>")
                .append(xmlValue(
                        cheque.getChequeNumber()))
                .append("</ChequeNumber>\n");

        xml.append("            <BatchNumber>")
                .append(xmlValue(
                        cheque.getBatchNumber()))
                .append("</BatchNumber>\n");

        xml.append("            <ChequeDate>")
                .append(xmlValue(
                        cheque.getChequeDate()))
                .append("</ChequeDate>\n");

        xml.append("            <CityCode>")
                .append(xmlValue(
                        cheque.getCityCode()))
                .append("</CityCode>\n");

        xml.append("            <BankCode>")
                .append(xmlValue(
                        cheque.getBankCode()))
                .append("</BankCode>\n");

        xml.append("            <BranchCode>")
                .append(xmlValue(
                        cheque.getBranchCode()))
                .append("</BranchCode>\n");

        xml.append("            <DrawerAccountNumber>")
                .append(xmlValue(
                        cheque.getDrawerAccountNumber()))
                .append("</DrawerAccountNumber>\n");

        xml.append("            <DrawerName>")
                .append(xmlValue(
                        cheque.getDrawerName()))
                .append("</DrawerName>\n");

        xml.append("            <DepositorAccountNumber>")
                .append(xmlValue(
                        cheque.getDepositorAccountNumber()))
                .append("</DepositorAccountNumber>\n");

        xml.append("            <DepositorName>")
                .append(xmlValue(
                        cheque.getDepositorName()))
                .append("</DepositorName>\n");

        xml.append("            <PayeeName>")
                .append(xmlValue(
                        cheque.getPayeeName()))
                .append("</PayeeName>\n");

        xml.append("            <PayeeAccountNumber>")
                .append(xmlValue(
                        cheque.getPayeeAccountNumber()))
                .append("</PayeeAccountNumber>\n");

        xml.append("            <Amount>")
                .append(xmlValue(
                        cheque.getAmount()))
                .append("</Amount>\n");

        xml.append("            <AmountInWords>")
                .append(xmlValue(
                        cheque.getAmountInWords()))
                .append("</AmountInWords>\n");

        xml.append("            <ChequeStatus>")
                .append(xmlValue(
                        cheque.getChequeStatus()))
                .append("</ChequeStatus>\n");


        if (rejected) {

            xml.append("            <ReturnReasonId>")
                    .append(xmlValue(
                            cheque.getReturnReasonId()))
                    .append("</ReturnReasonId>\n");

            xml.append("            <CheckerRemarks>")
                    .append(xmlValue(
                            cheque.getCheckerRemarks()))
                    .append("</CheckerRemarks>\n");
        }


        xml.append("        </Cheque>\n");
    }


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


    @Listen("onClick = #refreshReportBtn")
    public void refreshReports() {

        loadReportBatches();
    }
}