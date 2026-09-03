package com.cts.inward.parser;

import java.util.List;

import com.cts.inward.model.PibfImageData;

public interface PibfProcessor {

    List<PibfImageData> extractImages(String filePath);
}