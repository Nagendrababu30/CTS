package com.cts.inward.config;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.sql.DataSource;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class ConnectionPool {

    private static final ComboPooledDataSource DATA_SOURCE =
            createDataSource();

    private ConnectionPool() {
    }

    public static DataSource getDataSource() {
        return DATA_SOURCE;
    }

    private static ComboPooledDataSource createDataSource() {

        try (InputStream inputStream =
                     ConnectionPool.class
                             .getClassLoader()
                             .getResourceAsStream(
                                     "application.properties")) {

            if (inputStream == null) {

                throw new IllegalStateException(
                        "application.properties not found");
            }

            Properties properties =
                    new Properties();

            properties.load(inputStream);

            ComboPooledDataSource dataSource =
                    new ComboPooledDataSource();

            dataSource.setDriverClass(
                    getRequiredProperty(
                            properties,
                            "db.driver.class"));

            dataSource.setJdbcUrl(
                    getRequiredProperty(
                            properties,
                            "db.url"));

            dataSource.setUser(
                    getRequiredProperty(
                            properties,
                            "db.username"));

            dataSource.setPassword(
                    getRequiredProperty(
                            properties,
                            "db.password"));

            dataSource.setInitialPoolSize(5);
            dataSource.setMinPoolSize(5);
            dataSource.setAcquireIncrement(5);
            dataSource.setMaxPoolSize(20);

            return dataSource;

        } catch (IOException |
                 PropertyVetoException e) {

            throw new IllegalStateException(
                    "Failed to initialize database "
                    + "connection pool",
                    e);
        }
    }

    private static String getRequiredProperty(
            Properties properties,
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
}