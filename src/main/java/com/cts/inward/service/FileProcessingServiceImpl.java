package com.cts.inward.service;

import java.nio.file.Path;
import java.util.List;

import com.cts.inward.enums.FileType;
import com.cts.inward.model.ChequeImage;
import com.cts.inward.model.ChequeImagePaths;
import com.cts.inward.model.InwardCheque;
import com.cts.inward.model.NpciBatchData;
import com.cts.inward.model.NpciChequeData;
import com.cts.inward.model.OcrBatchData;
import com.cts.inward.model.OcrChequeData;
import com.cts.inward.model.PibfImageData;
import com.cts.inward.parser.OcrParser;
import com.cts.inward.parser.PibfProcessor;
import com.cts.inward.parser.PxfParser;
import com.cts.inward.service.BatchService;
import com.cts.inward.service.ChequeImageService;
import com.cts.inward.service.ChequeService;
import com.cts.inward.service.FileProcessingService;
import com.cts.inward.service.ImageService;
import com.cts.inward.service.OcrBatchService;
import com.cts.inward.service.OcrChequeService;

public class FileProcessingServiceImpl
        implements FileProcessingService {

    private final PxfParser pxfParser;
    private final PibfProcessor pibfProcessor;
    private final OcrParser ocrParser;

    private final BatchService batchService;
    private final ChequeService chequeService;

    private final OcrBatchService ocrBatchService;
    private final OcrChequeService ocrChequeService;

    private final ImageService imageService;
    private final ChequeImageService chequeImageService;

    private FileProcessingServiceImpl(
            PxfParser pxfParser,
            PibfProcessor pibfProcessor,
            OcrParser ocrParser,
            BatchService batchService,
            ChequeService chequeService,
            OcrBatchService ocrBatchService,
            OcrChequeService ocrChequeService,
            ImageService imageService,
            ChequeImageService chequeImageService) {

        this.pxfParser = pxfParser;
        this.pibfProcessor = pibfProcessor;
        this.ocrParser = ocrParser;
        this.batchService = batchService;
        this.chequeService = chequeService;
        this.ocrBatchService = ocrBatchService;
        this.ocrChequeService = ocrChequeService;
        this.imageService = imageService;
        this.chequeImageService = chequeImageService;
    }

    public static FileProcessingServiceImpl of(
            PxfParser pxfParser,
            PibfProcessor pibfProcessor,
            OcrParser ocrParser,
            BatchService batchService,
            ChequeService chequeService,
            OcrBatchService ocrBatchService,
            OcrChequeService ocrChequeService,
            ImageService imageService,
            ChequeImageService chequeImageService) {

        return new FileProcessingServiceImpl(
                pxfParser,
                pibfProcessor,
                ocrParser,
                batchService,
                chequeService,
                ocrBatchService,
                ocrChequeService,
                imageService,
                chequeImageService);
    }

    @Override
    public void processFile(String filePath) {

        FileType fileType =
                resolveFileType(filePath);

        switch (fileType) {

        case PXF:
            processPxfFile(filePath);
            break;

        case PIBF:
            processPibfFile(filePath);
            break;

        case OCR:
            processOcrFile(filePath);
            break;

        default:
            throw new IllegalStateException(
                    "Unsupported file type: "
                            + fileType);
        }
    }

    @Override
    public void processPxfFile(String filePath) {

        NpciBatchData batchData =
                pxfParser.parse(filePath);

        batchService.saveBatch(batchData);

        for (NpciChequeData chequeData :
                batchData.getCheques()) {

            chequeService.saveCheque(
                    chequeData);
        }
    }

    @Override
    public void processPibfFile(String filePath) {

        String batchId =
                extractBatchId(filePath);

        List<PibfImageData> imageDataList =
                pibfProcessor.extractImages(
                        filePath);

        List<InwardCheque> cheques =
                chequeService.getChequesForBatch(
                        batchId);

        if (imageDataList.size()
                != cheques.size()) {

            throw new IllegalStateException(
                    "PIBF image count does not match "
                    + "cheque count for batch: "
                    + batchId);
        }

        for (int index = 0;
                index < cheques.size();
                index++) {

            InwardCheque cheque =
                    cheques.get(index);

            PibfImageData imageData =
                    imageDataList.get(index);

            String chequeNumber =
                    cheque.getChequeNumber();

            ChequeImagePaths imagePaths =
                    imageService.saveChequeImages(
                            batchId,
                            chequeNumber,
                            imageData);

            ChequeImage chequeImage =
                    ChequeImage.of(
                            chequeNumber,
                            imagePaths.getFrontImagePath(),
                            imagePaths.getBackImagePath());

            chequeImageService.saveImage(
                    chequeImage);
        }
    }

    @Override
    public void processOcrFile(String filePath) {

        OcrBatchData batchData =
                ocrParser.parse(filePath);

        ocrBatchService.saveBatch(
                batchData);

        for (OcrChequeData chequeData :
                batchData.getCheques()) {

            ocrChequeService.saveCheque(
                    chequeData);
        }
    }

    private String extractBatchId(
            String filePath) {

        Path file =
                Path.of(filePath);

        String fileName =
                file.getFileName()
                        .toString();

        /*
         * Expected test PIBF filename:
         *
         * wPIBF_BATCH001_01.img
         */

        String[] parts =
                fileName.split("_");

        if (parts.length < 3) {

            throw new IllegalArgumentException(
                    "Invalid PIBF filename: "
                            + fileName);
        }

        return parts[1];
    }

    private FileType resolveFileType(
            String filePath) {

        Path file =
                Path.of(filePath);

        Path parentDirectory =
                file.getParent();

        if (parentDirectory == null) {

            throw new IllegalArgumentException(
                    "File parent directory not found: "
                            + filePath);
        }

        String directoryName =
                parentDirectory
                        .getFileName()
                        .toString()
                        .toLowerCase();

        switch (directoryName) {

        case "pxf":
            return FileType.PXF;

        case "pibf":
            return FileType.PIBF;

        case "ocr":
            return FileType.OCR;

        default:
            throw new IllegalArgumentException(
                    "Unable to determine file type from path: "
                            + filePath);
        }
    }
}