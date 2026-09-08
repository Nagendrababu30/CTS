package com.cts.inward.dao;

import java.util.List;

import com.cts.inward.dto.DashboardBatchDto;

public interface DashboardDao {

    List<DashboardBatchDto> getDashboardBatches();

    boolean lockBatch(Long batchId, Long userId);
    
    public boolean updateBatchStatus(
            Long batchId,
            String batchStatus,
            Long userId);
    
}