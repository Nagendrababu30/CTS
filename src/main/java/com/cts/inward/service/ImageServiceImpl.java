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
                    fileConfiguration
                            .getImagesPath()
                            .resolve(batchId)
                            .resolve(chequeNumber);

            Files.createDirectories(
                    chequeImageDirectory);

            Path frontImagePath =
                    chequeImageDirectory.resolve(
                            "front.jpg");

            Path backImagePath =
                    chequeImageDirectory.resolve(
                            "back.jpg");

            Files.write(
                    frontImagePath,
                    imageData.getFrontImage());

            Files.write(
                    backImagePath,
                    imageData.getBackImage());

            return ChequeImagePaths.of(
                    frontImagePath.toString(),
                    backImagePath.toString());

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to save images for cheque: "
                            + chequeNumber,
                    e);
        }
    }
}
