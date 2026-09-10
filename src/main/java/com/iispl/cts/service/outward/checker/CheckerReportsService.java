package com.iispl.cts.service.outward.checker;

import com.iispl.cts.dao.outward.checker.CheckerReportsDAO;
import com.iispl.cts.model.outward.OutwardBatch;

import java.util.List;

public class CheckerReportsService {

    private final CheckerReportsDAO dao;

    public CheckerReportsService() {

        dao = new CheckerReportsDAO();
    }

    /**
     * Returns ONLY batches with
     * CHECKER_COMPLETED status.
     */
    public List<OutwardBatch>
    getCheckerCompletedBatches() {

        return dao.getCheckerCompletedBatches();
    }
}