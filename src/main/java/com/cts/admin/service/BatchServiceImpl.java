package com.cts.admin.service;

import java.util.List;
import com.cts.admin.dao.BatchDAO;
import com.cts.admin.dao.BatchDAOImpl;
import com.cts.admin.model.Batch;

public class BatchServiceImpl implements BatchService {

    private final BatchDAO batchDAO;

    public BatchServiceImpl() {
        batchDAO = new BatchDAOImpl();
    }

    @Override
    public List<Batch> getBatchCaptureBatches() {
        return batchDAO.getBatchCaptureBatches();
    }

    @Override
    public List<Batch> getInwardBatches() {
        return batchDAO.getInwardBatches();
    }

    @Override
    public List<Batch> getOutwardBatches() {
        return batchDAO.getOutwardBatches();
    }
}
