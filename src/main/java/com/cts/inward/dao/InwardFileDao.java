package com.cts.inward.dao;

import java.util.List;

import com.cts.inward.model.InwardFile;

public interface InwardFileDao {

    List<InwardFile> getChiFiles();

    void markAsProcessed(long fileId);

    long getFileIdByPath(String filePath);
}
