package com.cts.inward.service;

import java.util.List;
import java.util.Map;

public interface CheckerReportService {

    List<Map<String, Object>> getRrfReportData();

    List<Map<String, Object>> getApprovedReportData();

    String generateRrfXml();

    String generateApprovedXml();
    
}