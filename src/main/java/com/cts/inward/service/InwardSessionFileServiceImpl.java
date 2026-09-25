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

    	// Move each CHI file to its respective incoming folder.
        for (Map.Entry<FileType, List<InwardFile>> entry : files.entrySet()) {
            for (InwardFile file : entry.getValue()) {
            	
            	// Move the file to the incoming folder.
                moveFileToIncoming(file);
                
            }
        }
        
    }

    @Override
    public void moveFileToIncoming(InwardFile file) {

        try {

        	// Resolve the source path of the CHI file.
            Path sourceFile = resolveSourcePath(file.getFilePath());

            // Get the target incoming folder based on the file type.
            Path targetDirectory =
                    fileConfiguration
                            .getIncomingPath()
                            .resolve(file.getFileType().name().toLowerCase());

            //  Create the target directory if it does not exist.
            Files.createDirectories(targetDirectory);

            // Create the target path using the original file name.
            Path targetFile =
                    targetDirectory.resolve(sourceFile.getFileName());

            // Move the file from the CHI folder to the incoming folder.
            Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

            // Mark the file as PROCESSED so it is not picked up again in future sessions. 
            inwardFileDao.markAsProcessed(file.getFileId());

            // Record the file movement in the file summary table.
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
    	
        if (filePath == null || filePath.isBlank()) {
        	return null;
        }
        
        // Return the path if it is an existing absolute path.
        Path path = Path.of(filePath);
        if (path.isAbsolute() && Files.exists(path)) {
            return path.normalize();
        }

        String normalized = filePath.replace("\\", "/").trim();
        if (normalized.startsWith("/")) {
        	
            normalized = normalized.substring(1);
            
        }

        // 1. Try the path relative to the current working directory.
        Path direct = Path.of(normalized);
        if (Files.exists(direct)) {
        	
            return direct.toAbsolutePath().normalize();
            
        }

        // 2. Try the path relative to the workspace src/main/webapp directory.
        String cleanSub = normalized.startsWith("src/main/webapp/")
                ? normalized.substring("src/main/webapp/".length())
                : normalized;

        Path workspaceCandidate = Path.of("src/main/webapp", cleanSub);
        if (Files.exists(workspaceCandidate)) {
        	
            return workspaceCandidate.toAbsolutePath().normalize();
            
        }

        // 3. Try resolving the path using the configured inward root path.
        Path rootPath = fileConfiguration.getInwardRootPath();
        if (rootPath != null) {
        	
            Path webAppRoot = rootPath.getParent();

            // Try the path as a sibling folder under the web application root.
            if (webAppRoot != null) {
                Path candidate = webAppRoot.resolve(cleanSub);
                if (Files.exists(candidate)) {
                    return candidate.normalize();
                }
            }

            // Try the path inside the inward-files folder.
            if (cleanSub.startsWith("inward-files/")) {
                String afterInward = cleanSub.substring("inward-files/".length());
                Path inwardCandidate = rootPath.resolve(afterInward);
                if (Files.exists(inwardCandidate)) {
                    return inwardCandidate.normalize();
                }
                return inwardCandidate.normalize();
            }

            // Handle a path pointing to the inward-chi-files sibling folder.
            if (webAppRoot != null && cleanSub.startsWith("inward-chi-files/")) {
                return webAppRoot.resolve(cleanSub).normalize();
            }

         // Resolve the path relative to the configured inward root folder.
            return rootPath.resolve(cleanSub).normalize();
            
        }

        // Fall back to the absolute path of the original input.
        return path.toAbsolutePath().normalize();
        
    }
}
