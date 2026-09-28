package com.iispl.cts.service.outward;

import java.util.Date;
import java.util.List;

import com.iispl.cts.dao.outward.CaptureOperatorReportsDAO;
import com.iispl.cts.model.outward.OutwardBatch;

public class CaptureOperatorReportsService {

    private final CaptureOperatorReportsDAO reportsDAO;

    public CaptureOperatorReportsService() {

        reportsDAO =
                new CaptureOperatorReportsDAO();
    }

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

    public List<Object[]> getDownloadHistory(
            long operatorId) {

        return reportsDAO.getDownloadHistory(
                operatorId
        );
    }
}