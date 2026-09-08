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

        List<InwardFile> chiFiles =
                inwardFileDao.getChiFiles();

        Map<FileType, List<InwardFile>> filesByType =
                new EnumMap<>(FileType.class);

        for (InwardFile file : chiFiles) {

            filesByType
                    .computeIfAbsent(
                            file.getFileType(),
                            key -> new ArrayList<>())
                    .add(file);
        }

        return filesByType;
    }
}