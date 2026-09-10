package com.iispl.cts.service.outward;

import com.iispl.cts.dao.outward.checker.CheckerBatchDAO;
import com.iispl.cts.model.outward.OutwardCheque;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CTSFileGenerationService {

    private final CheckerBatchDAO checkerBatchDAO;

    private final CXFXMLWriter cxfXmlWriter;

    private final CIBFWriter cibfWriter;

    private final RRFXmlWriter rrfXmlWriter;


    public CTSFileGenerationService() {

        checkerBatchDAO =
                new CheckerBatchDAO();

        cxfXmlWriter =
                new CXFXMLWriter();

        cibfWriter =
                new CIBFWriter();

        rrfXmlWriter =
                new RRFXmlWriter();
    }


    /**
     * Generate normal outward presentment files.
     *
     * CXF.XML
     * CIBF.IMG
     */
    public Map<String, File> generateOutwardFiles(
            String batchNumber,
            String outputDirectory) throws Exception {

        List<OutwardCheque> cheques =
                checkerBatchDAO
                        .getChequesByBatchId(batchNumber);

        if (cheques == null
                || cheques.isEmpty()) {

            throw new Exception(
                    "No cheques found for batch: "
                            + batchNumber
            );
        }

        File batchDirectory =
                new File(
                        outputDirectory,
                        batchNumber
                );

        if (!batchDirectory.exists()
                && !batchDirectory.mkdirs()) {

            throw new Exception(
                    "Unable to create batch directory: "
                            + batchDirectory
            );
        }


        // ==============================
        // GENERATE CXF
        // ==============================

        File cxfFile =
                cxfXmlWriter.generateCXF(
                        batchNumber,
                        cheques,
                        batchDirectory
                                .getAbsolutePath()
                );


        // ==============================
        // GENERATE CIBF
        // ==============================

        File cibfFile =
                cibfWriter.generateCIBF(
                        batchNumber,
                        cheques,
                        batchDirectory
                                .getAbsolutePath()
                );


        Map<String, File> result =
                new HashMap<>();

        result.put(
                "CXF",
                cxfFile
        );

        result.put(
                "CIBF",
                cibfFile
        );

        return result;
    }


    /**
     * Generate RRF for return processing.
     */
    public File generateReturnFile(
            String batchNumber,
            String outputDirectory)
            throws Exception {

        List<OutwardCheque> cheques =
                checkerBatchDAO
                        .getChequesByBatchId(
                                batchNumber
                        );

        if (cheques == null
                || cheques.isEmpty()) {

            throw new Exception(
                    "No cheques found for batch: "
                            + batchNumber
            );
        }

        File batchDirectory =
                new File(
                        outputDirectory,
                        batchNumber
                );

        if (!batchDirectory.exists()
                && !batchDirectory.mkdirs()) {

            throw new Exception(
                    "Unable to create batch directory: "
                            + batchDirectory
            );
        }

        return rrfXmlWriter.generateRRF(
                batchNumber,
                cheques,
                batchDirectory
                        .getAbsolutePath()
        );
    }
}