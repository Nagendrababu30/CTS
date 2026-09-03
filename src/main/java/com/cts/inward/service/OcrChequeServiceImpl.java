package com.cts.inward.service;

import com.cts.inward.dao.OcrChequeDao;
import com.cts.inward.model.OcrChequeData;

public class OcrChequeServiceImpl
        implements OcrChequeService {

    private final OcrChequeDao ocrChequeDataDao;

    private OcrChequeServiceImpl(
            OcrChequeDao ocrChequeDataDao) {

        this.ocrChequeDataDao =
                ocrChequeDataDao;
    }

    public static OcrChequeServiceImpl of(
            OcrChequeDao ocrChequeDataDao) {

        return new OcrChequeServiceImpl(
                ocrChequeDataDao);
    }

    @Override
    public void saveCheque(
            OcrChequeData chequeData) {

        ocrChequeDataDao.saveCheque(
                chequeData);
    }
}