package com.cts.inward.service;

import java.util.List;
import java.util.Map;

import com.cts.inward.enums.FileType;
import com.cts.inward.model.InwardFile;

public interface CHIFileService {

    Map<FileType, List<InwardFile>> getCHIFilePaths();
}