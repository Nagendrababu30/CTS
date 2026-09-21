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

            Path sourceFile = resolveSourcePath(file.getFilePath());

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

    private Path resolveSourcePath(String filePath) {
        if (filePath == null || filePath.isBlank()) return null;
        Path path = Path.of(filePath);
        if (path.isAbsolute() && Files.exists(path)) {
            return path.normalize();
        }

        String normalized = filePath.replace("\\", "/").trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        // 1. Direct path relative to working directory
        Path direct = Path.of(normalized);
        if (Files.exists(direct)) {
            return direct.toAbsolutePath().normalize();
        }

        // 2. Direct path relative to workspace src/main/webapp/
        String cleanSub = normalized.startsWith("src/main/webapp/")
                ? normalized.substring("src/main/webapp/".length())
                : normalized;

        Path workspaceCandidate = Path.of("src/main/webapp", cleanSub);
        if (Files.exists(workspaceCandidate)) {
            return workspaceCandidate.toAbsolutePath().normalize();
        }

        // 3. Resolve using rootPath and webAppRoot
        Path rootPath = fileConfiguration.getInwardRootPath();
        if (rootPath != null) {
            Path webAppRoot = rootPath.getParent();

            // Sibling folder under webAppRoot (e.g. inward-chi-files/...)
            if (webAppRoot != null) {
                Path candidate = webAppRoot.resolve(cleanSub);
                if (Files.exists(candidate)) {
                    return candidate.normalize();
                }
            }

            // Folder inside inward-files/
            if (cleanSub.startsWith("inward-files/")) {
                String afterInward = cleanSub.substring("inward-files/".length());
                Path inwardCandidate = rootPath.resolve(afterInward);
                if (Files.exists(inwardCandidate)) {
                    return inwardCandidate.normalize();
                }
                return inwardCandidate.normalize();
            }

            // If path is a sibling like inward-chi-files/
            if (webAppRoot != null && cleanSub.startsWith("inward-chi-files/")) {
                return webAppRoot.resolve(cleanSub).normalize();
            }

            return rootPath.resolve(cleanSub).normalize();
        }

        return path.toAbsolutePath().normalize();
    }
}
