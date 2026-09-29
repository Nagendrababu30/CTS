package com.iispl.cts.service.outward.checker;

import com.iispl.cts.dao.outward.checker.CheckerBatchDAO;
import com.iispl.cts.model.outward.OutwardCheque;

import java.util.List;

public class CheckerFileGenerationService {

	private final CheckerBatchDAO checkerBatchDAO;

	// CONSTRUCTOR

	public CheckerFileGenerationService() {

		checkerBatchDAO = new CheckerBatchDAO();
	}

	// GET BATCH CHEQUES
	public List<OutwardCheque> getBatchCheques(String batchNumber) {


		if (batchNumber == null || batchNumber.trim().isEmpty()) {

			throw new IllegalArgumentException("Batch number is required");
		}

		batchNumber = batchNumber.trim();


		List<OutwardCheque> cheques = checkerBatchDAO.getChequesByBatchId(batchNumber);

		if (cheques == null) {

			return new java.util.ArrayList<OutwardCheque>();
		}

		return cheques;
	}
}