package com.cts.inward.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cts.inward.config.FileConfiguration;
import com.cts.inward.enums.FileType;
import com.cts.inward.file.FileProcessingExecutor;
import com.cts.inward.model.InwardFile;

public class InwardIngestionServiceImpl
        implements InwardIngestionService {

    private final CHIFileService chiFileService;
    private final InwardSessionFileService inwardSessionFileService;
    private final FileProcessingExecutor fileProcessingExecutor;
    private final FileConfiguration fileConfiguration;

    private InwardIngestionServiceImpl(
            CHIFileService chiFileService,
            InwardSessionFileService inwardSessionFileService,
            FileProcessingExecutor fileProcessingExecutor,
            FileConfiguration fileConfiguration) {

        this.chiFileService = chiFileService;
        this.inwardSessionFileService = inwardSessionFileService;
        this.fileProcessingExecutor = fileProcessingExecutor;
        this.fileConfiguration = fileConfiguration;
    }

    public static InwardIngestionServiceImpl of(
            CHIFileService            chiFileService,
            InwardSessionFileService  inwardSessionFileService,
            FileProcessingExecutor    fileProcessingExecutor,
            FileConfiguration         fileConfiguration) {

        return new InwardIngestionServiceImpl(
                chiFileService,
                inwardSessionFileService,
                fileProcessingExecutor,
                fileConfiguration);
    }

    @Override
    public void processSessionFiles() {

    	//Get file paths received from NPCI and stored in the inward-chi-files folder by CHI
        Map<FileType, List<InwardFile>> filesByType =
                chiFileService.getCHIFilePaths();

        if (filesByType == null || filesByType.isEmpty()) {
            System.out.println(
                    "[InwardIngestion] No CHI files found in DB. Nothing to process.");
            return;
        }

        // Move received files to the incoming folder under inward-files
        inwardSessionFileService.moveFilesToIncoming(filesByType);

        System.out.println(
                "[InwardIngestion] All CHI files moved to incoming directories.");

     // Group incoming PXF, OCR and PIBF files batch-wise and submit them in order for processing
        processIncomingFiles();

        System.out.println(
                "[InwardIngestion] Batches submitted for processing.");
    }

    @Override
    public void processIncomingFiles() {

        if (fileConfiguration == null || fileProcessingExecutor == null) {
            return;
        }

        // Stores incoming files grouped by batch name and file type
        Map<String, Map<String, String>> batchMap = new LinkedHashMap<>();

        // Collect PXF files and group them by batch name
        collectFiles(fileConfiguration.getIncomingPath().resolve("pxf"), "pxf", batchMap);
        
        // Collect OCR files and group them by batch name
        collectFiles(fileConfiguration.getIncomingPath().resolve("ocr"), "ocr", batchMap);
        
        // Collect PIBF files and group them by batch name
        collectFiles(fileConfiguration.getIncomingPath().resolve("pibf"), "pibf", batchMap);

        for (Map.Entry<String, Map<String, String>> entry : batchMap.entrySet()) {
            String batchName = entry.getKey();
            Map<String, String> filesByType = entry.getValue();

            // Arrange files for each batch in the required processing order: PXF, OCR and PIBF
            List<String> ordered = new ArrayList<>();
            
            if (filesByType.containsKey("pxf")) {
                ordered.add(filesByType.get("pxf"));
            }
            
            if (filesByType.containsKey("ocr")) {
                ordered.add(filesByType.get("ocr"));
            }
            
            if (filesByType.containsKey("pibf")) {
                ordered.add(filesByType.get("pibf"));
            }

            System.out.println(
                    "[InwardIngestion] Submitting batch: " + batchName + " files in order: " + ordered);

            // Submit each batch for processing in the required file order
            fileProcessingExecutor.submitBatch(ordered);
        }
    }

    private void collectFiles(Path directory, String fileType, Map<String, Map<String, String>> batchMap) {
        try {
        	
            if (!Files.exists(directory)) {
                return;
            }
            
            // Read all files from the respective incoming file-type folder
            Files.list(directory)
                    .filter(Files::isRegularFile)
                    .forEach(filePath -> {
                    	
                    	// Extract the batch name from the file name
                        String batchName = extractBatchName(filePath.getFileName().toString());
                        
                        // Add the file under its batch name and file type if not already present
                        Map<String, String> files = batchMap.computeIfAbsent(batchName, k -> new LinkedHashMap<>());
                        if (files.containsKey(fileType)) {
                            // Avoid overwriting if duplicate file exists for same batch in incoming directory
                            System.out.println("[InwardIngestion] Duplicate incoming file ignored for batch " + batchName + ": " + filePath.getFileName());
                        } else {
                            files.put(fileType, filePath.toString());
                        }
                    });
            
        } catch (IOException e) {
        	
            throw new IllegalStateException("Failed to scan incoming directory: " + directory, e);
            
        }
    }

    private String extractBatchName(String fileName) {
    	
    	// Remove the file extension before extracting the batch name
        String nameWithoutExtension = fileName.contains(".")
                ? fileName.substring(0, fileName.lastIndexOf('.'))
                : fileName;

        // Split the file name into parts using underscore
        String[] parts = nameWithoutExtension.split("_");
        
        // Find and return the part that starts with BATCH
        for (String part : parts) {
            if (part.toUpperCase().startsWith("BATCH")) {
                return part.toUpperCase();
            }
        }
        
        // Return the file name if no batch name is found
        return nameWithoutExtension.toUpperCase();
    }

}
