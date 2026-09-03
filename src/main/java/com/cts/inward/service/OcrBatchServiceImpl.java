package com.cts.inward.service;

import com.cts.inward.dao.OcrBatchDao;
import com.cts.inward.model.OcrBatchData;

public class OcrBatchServiceImpl
        implements OcrBatchService {

    private final OcrBatchDao ocrBatchDao;

    private OcrBatchServiceImpl(
            OcrBatchDao ocrBatchDao) {

        this.ocrBatchDao =
                ocrBatchDao;
    }

    public static OcrBatchServiceImpl of(
            OcrBatchDao ocrBatchDao) {

        return new OcrBatchServiceImpl(
                ocrBatchDao);
    }

    @Override
    public void saveBatch(
            OcrBatchData batchData) {

        ocrBatchDao.saveBatch(
                batchData);
    }
}