package com.cts.admin.dao;

import java.util.List;
import com.cts.admin.model.Batch;

public interface BatchDAO {
    List<Batch> getBatchCaptureBatches();
    List<Batch> getInwardBatches();
    List<Batch> getOutwardBatches();
}
