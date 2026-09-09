package com.cts.admin.service;

import java.util.List;
import com.cts.admin.model.Batch;

public interface BatchService {
    List<Batch> getBatchCaptureBatches();
    List<Batch> getInwardBatches();
    List<Batch> getOutwardBatches();
}
