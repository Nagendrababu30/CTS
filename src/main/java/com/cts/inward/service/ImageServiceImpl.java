package com.cts.inward.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.cts.inward.config.FileConfiguration;
import com.cts.inward.model.ChequeImagePaths;
import com.cts.inward.model.PibfImageData;

/**
 * Implementation placeholder.
 * Business implementation is intentionally deferred.
 */
public class ImageServiceImpl implements ImageService {
	
	private final FileConfiguration fileConfiguration;

    private ImageServiceImpl(
            FileConfiguration fileConfiguration) {

        this.fileConfiguration = fileConfiguration;
    }

    public static ImageServiceImpl of(
            FileConfiguration fileConfiguration) {

        return new ImageServiceImpl(
                fileConfiguration);
    }

	@Override
	public String getFrontImagePath(String chequeId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getBackImagePath(String chequeId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ChequeImagePaths saveChequeImages(String batchId, String chequeNumber, PibfImageData imageData) {
		try {

            Path chequeImageDirectory =
                    fileConfiguration.getChequeImagePath(batchId, chequeNumber);

            Files.createDirectories(chequeImageDirectory);

            Path frontImagePath = chequeImageDirectory.resolve("front.jpg");
            Path backImagePath  = chequeImageDirectory.resolve("back.jpg");

            Files.write(frontImagePath, imageData.getFrontImage());
            Files.write(backImagePath,  imageData.getBackImage());

            // Also keep src/main/webapp in sync during development if running from workspace
            try {
                Path devDir = Path.of("src/main/webapp/inward-files/images", batchId, chequeNumber);
                if (Files.exists(Path.of("src/main/webapp")) && !devDir.toAbsolutePath().equals(chequeImageDirectory.toAbsolutePath())) {
                    Files.createDirectories(devDir);
                    Files.write(devDir.resolve("front.jpg"), imageData.getFrontImage());
                    Files.write(devDir.resolve("back.jpg"),  imageData.getBackImage());
                }
            } catch (Exception ignored) {}

            String relativeFront = "inward-files/images/" + batchId + "/" + chequeNumber + "/front.jpg";
            String relativeBack  = "inward-files/images/" + batchId + "/" + chequeNumber + "/back.jpg";

            return ChequeImagePaths.of(
                    relativeFront,
                    relativeBack);

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to save images for cheque: "
                            + chequeNumber, e);
        }
    }
}
