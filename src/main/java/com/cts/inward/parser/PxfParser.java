package com.cts.inward.parser;

import com.cts.inward.dto.PxfParserResult;

public interface PxfParser {

	 PxfParserResult parse(String filePath);
    
}