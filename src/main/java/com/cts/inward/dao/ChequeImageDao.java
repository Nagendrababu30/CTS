package com.cts.inward.dao;

import com.cts.inward.model.ChequeImage;

public interface ChequeImageDao {

    void save(ChequeImage chequeImage);

    ChequeImage findByChequeNumber(
            String chequeNumber);
}