package com.cts.inward.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import com.cts.inward.config.FileConfiguration;
import com.cts.inward.enums.FileStage;
import com.cts.inward.enums.FileType;
import com.cts.inward.model.InwardFile;

public class InwardSessionFileServiceImpl
        implements InwardSessionFileService {

    private final FileConfiguration fileConfiguration;
    private final FileSummaryService fileSummaryService;

    private InwardSessionFileServiceImpl(
            FileConfiguration fileConfiguration,
            FileSummaryService fileSummaryService) {

        this.fileConfiguration =
                fileConfiguration;

        this.fileSummaryService =
                fileSummaryService;
    }

    public static InwardSessionFileServiceImpl of(
            FileConfiguration fileConfiguration,
            FileSummaryService fileSummaryService) {

        return new InwardSessionFileServiceImpl(
                fileConfiguration,
                fileSummaryService);
    }

    @Override
    public void moveFilesToIncoming(
            Map<FileType, List<InwardFile>> files) {

        for (Map.Entry<FileType, List<InwardFile>> entry :
                files.entrySet()) {

            for (InwardFile file :
                    entry.getValue()) {

                moveFileToIncoming(file);
            }
        }
    }

    @Override
    public void moveFileToIncoming(
            InwardFile file) {

        try {

            Path sourceFile =
                    Path.of(
                            file.getFilePath());

            Path targetDirectory =
                    fileConfiguration
                            .getIncomingPath()
                            .resolve(
                                    file.getFileType()
                                            .name()
                                            .toLowerCase());

            Files.createDirectories(
                    targetDirectory);

            Path targetFile =
                    targetDirectory.resolve(
                            sourceFile.getFileName());

            Files.move(
                    sourceFile,
                    targetFile,
                    StandardCopyOption.REPLACE_EXISTING);

            /*
             * Update FILE_SUMMARY only after
             * the physical file move succeeds.
             */
            fileSummaryService.updateFileStage(
                    file.getFileId(),
                    FileStage.INCOMING);

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to move CHI file to incoming: "
                            + file.getFilePath(),
                    e);
        }
    }
}