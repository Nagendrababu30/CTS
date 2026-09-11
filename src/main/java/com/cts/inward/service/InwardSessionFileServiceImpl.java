package com.cts.inward.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import com.cts.inward.config.FileConfiguration;
import com.cts.inward.dao.InwardFileDao;
import com.cts.inward.enums.FileType;
import com.cts.inward.model.InwardFile;

public class InwardSessionFileServiceImpl
        implements InwardSessionFileService {

    private final FileConfiguration fileConfiguration;
    private final FileSummaryService fileSummaryService;
    private final InwardFileDao inwardFileDao;

    private InwardSessionFileServiceImpl(
            FileConfiguration fileConfiguration,
            FileSummaryService fileSummaryService,
            InwardFileDao inwardFileDao) {

        this.fileConfiguration = fileConfiguration;
        this.fileSummaryService = fileSummaryService;
        this.inwardFileDao = inwardFileDao;
    }

    public static InwardSessionFileServiceImpl of(
            FileConfiguration fileConfiguration,
            FileSummaryService fileSummaryService,
            InwardFileDao inwardFileDao) {

        return new InwardSessionFileServiceImpl(
                fileConfiguration,
                fileSummaryService,
                inwardFileDao);
    }

    @Override
    public void moveFilesToIncoming(Map<FileType, List<InwardFile>> files) {

        for (Map.Entry<FileType, List<InwardFile>> entry : files.entrySet()) {
            for (InwardFile file : entry.getValue()) {
                moveFileToIncoming(file);
            }
        }
    }

    @Override
    public void moveFileToIncoming(InwardFile file) {

        try {

            Path sourceFile = Path.of(file.getFilePath());

            Path targetDirectory =
                    fileConfiguration
                            .getIncomingPath()
                            .resolve(file.getFileType().name().toLowerCase());

            Files.createDirectories(targetDirectory);

            Path targetFile =
                    targetDirectory.resolve(sourceFile.getFileName());

            Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

            /*
             * File physically moved — mark as PROCESSED in inward_file
             * so it is never picked up again in future sessions.
             */
            inwardFileDao.markAsProcessed(file.getFileId());

            /*
             * INSERT into inward_file_summary for the first time —
             * file_stage = INCOMING. Subsequent stage updates
             * (PROCESSING, ARCHIVE) are done by FileProcessingServiceImpl.
             */
            fileSummaryService.insertFileSummary(
                    file.getFileId(),
                    file.getFileName());

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to move CHI file to incoming: "
                            + file.getFilePath(), e);
        }
    }
}
