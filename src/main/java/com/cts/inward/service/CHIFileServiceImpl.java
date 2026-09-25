package com.cts.inward.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.cts.inward.dao.InwardFileDao;
import com.cts.inward.enums.FileType;
import com.cts.inward.model.InwardFile;

public class CHIFileServiceImpl implements CHIFileService {

    private final InwardFileDao inwardFileDao;

    private CHIFileServiceImpl(
            InwardFileDao inwardFileDao) {

        this.inwardFileDao = inwardFileDao;
    }

    public static CHIFileServiceImpl of(
            InwardFileDao inwardFileDao) {

        return new CHIFileServiceImpl(
                inwardFileDao);
    }
 
    @Override
    public Map<FileType, List<InwardFile>> getCHIFilePaths() { 

    	// Get file paths received from NPCI and stored in inward-chi-files.
        List<InwardFile> chiFiles =
                inwardFileDao.getChiFiles();

        // Group the files by their file type using EnumMap.
        Map<FileType, List<InwardFile>> filesByType =
                new EnumMap<>(FileType.class);

        for (InwardFile file : chiFiles) {

        	// Add a new file type key with an empty list if absent; otherwise, add the file to the existing list.   
            filesByType
                    .computeIfAbsent(
                            file.getFileType(),
                            key -> new ArrayList<>())
                    .add(file);
            
        }

        return filesByType;
    }
}