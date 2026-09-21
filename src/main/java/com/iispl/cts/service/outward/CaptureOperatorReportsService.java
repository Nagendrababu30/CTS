package com.iispl.cts.service.outward;

import java.util.Date;
import java.util.List;

import com.iispl.cts.dao.outward.CaptureOperatorReportsDAO;
import com.iispl.cts.model.outward.OutwardBatch;

public class CaptureOperatorReportsService {

    private final CaptureOperatorReportsDAO reportsDAO;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public CaptureOperatorReportsService() {

        reportsDAO =
                new CaptureOperatorReportsDAO();
    }

    // =========================================================
    // GET REPORT DATA
    // =========================================================

    public List<OutwardBatch> getReportData(
            long operatorId,
            Date fromDate,
            Date toDate) {

        return reportsDAO.getReportData(
                operatorId,
                fromDate,
                toDate
        );
    }

    // =========================================================
    // SAVE DOWNLOAD HISTORY
    // =========================================================

    public void saveDownloadHistory(
            long operatorId,
            Date fromDate,
            Date toDate,
            String format) {

        reportsDAO.saveDownloadHistory(
                operatorId,
                fromDate,
                toDate,
                format
        );
    }

    // =========================================================
    // GET DOWNLOAD HISTORY
    // =========================================================

    public List<Object[]> getDownloadHistory(
            long operatorId) {

        return reportsDAO.getDownloadHistory(
                operatorId
        );
    }
}