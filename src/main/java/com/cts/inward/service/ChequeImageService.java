package com.cts.inward.service;

import com.cts.inward.model.ChequeImage;

public interface ChequeImageService {

	void saveImage(ChequeImage chequeImage);

    ChequeImage getImageByChequeNumber(
            String chequeNumber);
	
}
