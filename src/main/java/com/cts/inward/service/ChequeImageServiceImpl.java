package com.cts.inward.service;

import com.cts.inward.dao.ChequeImageDao;
import com.cts.inward.model.ChequeImage;

public class ChequeImageServiceImpl
        implements ChequeImageService {

    private final ChequeImageDao chequeImageDao;

    private ChequeImageServiceImpl(
            ChequeImageDao chequeImageDao) {

        this.chequeImageDao =
                chequeImageDao;
    }

    public static ChequeImageServiceImpl of(
            ChequeImageDao chequeImageDao) {

        return new ChequeImageServiceImpl(
                chequeImageDao);
    }

    @Override
    public void saveImage(
            ChequeImage chequeImage) {

        chequeImageDao.save(
                chequeImage);
    }

    @Override
    public ChequeImage getImageByChequeNumber(
            String chequeNumber) {

        return chequeImageDao
                .findByChequeNumber(
                        chequeNumber);
    }
}