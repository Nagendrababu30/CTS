package com.cts.inward.service;

import java.util.List;

import com.cts.inward.dao.DashboardDao;
import com.cts.inward.dao.DashboardDaoImpl;
import com.cts.inward.dto.DashboardBatchDto;

public class DashboardServiceImpl
        implements DashboardService {

    private final DashboardDao dashboardDao;

    public DashboardServiceImpl() {

        this.dashboardDao =
                new DashboardDaoImpl();
    }

    @Override
    public List<DashboardBatchDto> getDashboardBatches() {

        return dashboardDao.getDashboardBatches();
    }

    @Override
    public boolean lockBatch(
            Long batchId,
            Long userId) {

        return dashboardDao.lockBatch(
                batchId,
                userId);
    }
}