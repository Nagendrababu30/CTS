package com.cts.inward.parser;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.cts.inward.model.PibfImageData;

public class PibfProcessorImpl implements PibfProcessor {

    private PibfProcessorImpl() {
    }

    public static PibfProcessorImpl of() {
        return new PibfProcessorImpl();
    }

    @Override
    public List<PibfImageData> extractImages(
            String filePath) {

        List<PibfImageData> images =
                new ArrayList<>();

        try (DataInputStream inputStream =
                     new DataInputStream(
                             Files.newInputStream(
                                     Path.of(filePath)))) {

            while (true) {

                try {

                    int frontImageLength =
                            inputStream.readInt();

                    validateImageLength(
                            frontImageLength);

                    byte[] frontImage =
                            inputStream.readNBytes(
                                    frontImageLength);

                    validateImageData(
                            frontImage,
                            frontImageLength);

                    int backImageLength =
                            inputStream.readInt();

                    validateImageLength(
                            backImageLength);

                    byte[] backImage =
                            inputStream.readNBytes(
                                    backImageLength);

                    validateImageData(
                            backImage,
                            backImageLength);

                    PibfImageData imageData =
                            PibfImageData.of(
                                    frontImage,
                                    backImage);

                    images.add(imageData);

                } catch (EOFException e) {

                    break;
                }
            }

            if (images.isEmpty()) {
                throw new IllegalStateException(
                        "PIBF file contains no cheque images: "
                                + filePath);
            }

            return images;

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to read PIBF file: "
                            + filePath,
                    e);
        }
    }

    private void validateImageLength(
            int imageLength) {

        if (imageLength <= 0) {

            throw new IllegalStateException(
                    "Invalid image length in PIBF file: "
                            + imageLength);
        }
    }

    private void validateImageData(
            byte[] image,
            int expectedLength) {

        if (image.length != expectedLength) {

            throw new IllegalStateException(
                    "Incomplete image data in PIBF file");
        }
    }
}