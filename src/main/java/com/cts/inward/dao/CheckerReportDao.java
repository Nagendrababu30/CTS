package com.cts.inward.dao;

import java.util.List;
import java.util.Map;

public interface CheckerReportDao {

    List<Map<String, Object>> getRrfReportData();

    List<Map<String, Object>> getApprovedReportData();

    void updateRrfReportGenerated(List<Long> statusHistoryIds);

    void updateApprovedReportGenerated(List<Long> statusHistoryIds);
}