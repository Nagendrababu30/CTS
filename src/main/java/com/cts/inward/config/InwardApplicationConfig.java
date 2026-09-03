package com.cts.inward.config;

public class InwardApplicationConfig {

    private final ApplicationConfiguration applicationConfiguration;

    private final FileConfiguration fileConfiguration;

    private InwardApplicationConfig(
            ApplicationConfiguration applicationConfiguration,
            FileConfiguration fileConfiguration) {

        this.applicationConfiguration = applicationConfiguration;
        this.fileConfiguration = fileConfiguration;
    }

    public static InwardApplicationConfig of(
            ApplicationConfiguration applicationConfiguration,
            FileConfiguration fileConfiguration) {

        return new InwardApplicationConfig(
                applicationConfiguration,
                fileConfiguration);
    }

    public ApplicationConfiguration getApplicationConfiguration() {
        return applicationConfiguration;
    }

    public FileConfiguration getFileConfiguration() {
        return fileConfiguration;
    }
}