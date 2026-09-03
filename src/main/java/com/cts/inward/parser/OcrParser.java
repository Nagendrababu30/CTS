package com.cts.inward.parser;

import com.cts.inward.model.OcrBatchData;

public interface OcrParser {

	OcrBatchData parse(String filePath);
	
 }
