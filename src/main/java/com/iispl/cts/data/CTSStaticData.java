package com.iispl.cts.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class CTSStaticData {
	private static final String DRIVER ="org.postgresql.Driver";

    /*
     * Supabase Transaction Pooler
     *uygmm
     * 6543 = Transaction mode
     * 5432 = Session mode
     */
    private static final String DB_URL =
            "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres"
            + "?sslmode=require"
            + "&prepareThreshold=0";

    private static final String DB_USER =
            "postgres.bijnscklhxftxritxdrc";

    private static final String DB_PASSWORD ="Sushmabandari@123";

    static {

        try {

            Class.forName(DRIVER);

            System.out.println(
                    "PostgreSQL JDBC Driver loaded successfully."
            );

        } catch (ClassNotFoundException e) {

            throw new ExceptionInInitializerError(e);
        }
    }

    private CTSStaticData() {
        // Utility class
    }

    public static Connection getConnection()
            throws SQLException {

        if (DB_PASSWORD == null
                || DB_PASSWORD.trim().isEmpty()) {

            throw new SQLException(
                    "CTS_DB_PASSWORD environment variable is not set."
            );
        }

        Connection connection =
                DriverManager.getConnection(
                        DB_URL,
                        DB_USER,
                        DB_PASSWORD
                );

        /*
         * Make sure every connection starts with
         * auto-commit enabled.
         */
        connection.setAutoCommit(true);

        return connection;
    }
}