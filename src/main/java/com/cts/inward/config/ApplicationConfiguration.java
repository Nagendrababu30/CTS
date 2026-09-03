package com.cts.inward.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ApplicationConfiguration {

    private final Properties properties =
            new Properties();

    private ApplicationConfiguration() {
        loadProperties();
    }

    public static ApplicationConfiguration of() {
        return new ApplicationConfiguration();
    }

    private void loadProperties() {

        try (InputStream inputStream =
                     getClass()
                             .getClassLoader()
                             .getResourceAsStream(
                                     "application.properties")) {

            if (inputStream == null) {
                throw new IllegalStateException(
                        "application.properties not found");
            }

            properties.load(inputStream);

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to load application.properties",
                    e);
        }
    }

    public String getInwardRootPath() {

        return getRequiredProperty(
                "inward.root.path");
    }

    public int getFileProcessingThreadPoolSize() {

        return getRequiredIntegerProperty(
                "inward.file.processing.thread.pool.size");
    }

    public String getDatabaseDriverClass() {

        return getRequiredProperty(
                "db.driver.class");
    }

    public String getDatabaseUrl() {

        return getRequiredProperty(
                "db.url");
    }

    public String getDatabaseUsername() {

        return getRequiredProperty(
                "db.username");
    }

    public String getDatabasePassword() {

        return getRequiredProperty(
                "db.password");
    }

    private String getRequiredProperty(
            String key) {

        String value =
                properties.getProperty(key);

        if (value == null || value.isBlank()) {

            throw new IllegalStateException(
                    "Property '" + key
                    + "' is not configured");
        }

        return value;
    }

    private int getRequiredIntegerProperty(
            String key) {

        String value =
                getRequiredProperty(key);

        try {

            int result =
                    Integer.parseInt(value);

            if (result <= 0) {

                throw new IllegalStateException(
                        "Property '" + key
                        + "' must be greater than zero");
            }

            return result;

        } catch (NumberFormatException e) {

            throw new IllegalStateException(
                    "Invalid integer property: "
                    + key,
                    e);
        }
    }
}