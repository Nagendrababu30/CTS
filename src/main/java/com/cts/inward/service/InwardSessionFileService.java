package com.cts.inward.service;

import java.util.List;
import java.util.Map;

import com.cts.inward.enums.FileType;
import com.cts.inward.model.InwardFile;

public interface InwardSessionFileService {

    void moveFilesToIncoming(
            Map<FileType, List<InwardFile>> files);

    void moveFileToIncoming(
            InwardFile file);
    
}