package com.iispl.cts.service.outward;

import java.util.List;

import com.iispl.cts.dao.outward.OutwardMakerDataEntryDAO;
import com.iispl.cts.model.outward.OutwardBatch;

public class OutwardMakerDataEntryService {

    private final OutwardMakerDataEntryDAO dao;

    public OutwardMakerDataEntryService() {

        dao =
                new OutwardMakerDataEntryDAO();
    }

    public List<OutwardBatch> getBatches() {

        return dao.getAllBatches();
    }
}