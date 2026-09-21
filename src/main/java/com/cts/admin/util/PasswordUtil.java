package com.cts.admin.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    private static final int BCRYPT_ROUNDS = 12;

    private PasswordUtil() {
        // Utility class
    }

    public static String hashPassword(String plainPassword) {

        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        return BCrypt.hashpw(
                plainPassword,
                BCrypt.gensalt(BCRYPT_ROUNDS)
        );
    }

    public static boolean verifyPassword(
            String plainPassword,
            String storedHash) {

        if (plainPassword == null || storedHash == null
                || plainPassword.isEmpty()
                || storedHash.isEmpty()) {
            return false;
        }

        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}