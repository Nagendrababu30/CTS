package com.iispl.cts.controller.outward.checker;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


import org.zkoss.zk.ui.Component;
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
	// CFX / CIBF
	//
	// Only CHECKER_ACCEPTED cheques are included.
	// Only this XML is submitted to NPCI.
	// ================================================================

	private void downloadValidXml(
			String batchNumber) {

		try {

			List<OutwardCheque> cheques =
					service.getValidCheques(
							batchNumber);

			String xml =
					buildValidXml(
							batchNumber,
							cheques);

			if (xml == null) {
				return;
			}

			String fileName =
					batchNumber + ".xml";

			// =========================================================
			// SAVE VALID XML TO ARCHIVE 
			// =========================================================

			String archivePath = "C:\\Users\\ginja\\eclipse-workspace\\CTS\\src\\main\\webapp\\css\\outward\\Archive\\Valid-Cheques";
			Path directory = Paths.get(archivePath); 
			if (!Files.exists(directory)) { 
				Files.createDirectories(directory);
			}
			Path filePath = directory.resolve(fileName); 
			Files.write( filePath, xml.getBytes(StandardCharsets.UTF_8));

			Filedownload.save(
					xml,
					"application/xml",
					fileName);

			// =========================================================
			// SAVE NPCI SUBMISSION INFORMATION
			//
			// The actual XML being sent to NPCI is the valid XML.
			// Store the filename/path information in
			// outward_npci_submission.
			// =========================================================

			int validChequeCount =
					cheques.size();

			int totalChequeCount =
					service.getBatchCheques(
							batchNumber).size();

			int invalidChequeCount =
					totalChequeCount
					- validChequeCount;

			boolean saved =
					service.saveNPCISubmission(
							batchNumber,
							validChequeCount,
							invalidChequeCount,
							fileName);

			if (saved) {

				Messagebox.show(
						"Valid Cheques XML downloaded successfully "
								+ "and NPCI submission details saved.",
								"Checker Reports",
								Messagebox.OK,
								Messagebox.INFORMATION);

			} else {

				Messagebox.show(
						"Valid Cheques XML not downloaded successfully, "
								+ "but NPCI submission details could not be saved.",
								"Checker Reports",
								Messagebox.OK,
								Messagebox.EXCLAMATION);
			}


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

		try {

			List<OutwardCheque> cheques =
					service.getRrfCheques(
							batchNumber);

			String xml =
					buildRejectedXml(
							batchNumber,
							cheques);

			if (xml == null) {
				return;
			}

			String fileName = batchNumber
					+ ".xml";

			// ========================================================= 
			// SAVE RRF XML TO ARCHIVE 
			// =========================================================

			String archivePath = "C:\\Users\\ginja\\eclipse-workspace\\CTS\\src\\main\\webapp\\css\\outward\\Archive\\RejectedCheques"; 
			Path directory = Paths.get(archivePath); 
			if (!Files.exists(directory)) { 
				Files.createDirectories(directory); 
			} Path filePath = directory.resolve(fileName); 
			Files.write( filePath, xml.getBytes(StandardCharsets.UTF_8));

			Filedownload.save(
					xml,
					"application/xml",
					fileName);

		} catch (IllegalStateException e) {

			Messagebox.show(
					"RRF is not available for this batch.",
					"Checker Reports",
					Messagebox.OK,
					Messagebox.INFORMATION);

		} catch (Exception e) {

			e.printStackTrace();

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