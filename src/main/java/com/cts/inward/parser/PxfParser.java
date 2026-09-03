package com.cts.inward.parser;

import com.cts.inward.model.NpciBatchData;

public interface PxfParser {

    NpciBatchData parse(String filePath);
    
}