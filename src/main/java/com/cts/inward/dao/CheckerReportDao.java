package com.cts.inward.dao;

import java.util.List;
import java.util.Map;
import com.cts.inward.model.NpciBatchData;

public interface CheckerReportDao {
   
    
    List<Map<String, Object>> getRrfReportData();
    
    
    List<Map<String, Object>> getApprovedReportData();
}