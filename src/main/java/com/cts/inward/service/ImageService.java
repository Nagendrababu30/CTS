package com.cts.inward.service;

import com.cts.inward.model.ChequeImagePaths;
import com.cts.inward.model.PibfImageData;

public interface ImageService {

	String getFrontImagePath(String chequeId);

	String getBackImagePath(String chequeId);
	
	ChequeImagePaths saveChequeImages(
            String batchId,
            String chequeNumber,
            PibfImageData imageData);
	
}
