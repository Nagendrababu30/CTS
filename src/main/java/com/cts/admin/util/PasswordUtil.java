package com.cts.admin.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

	private static final int BCRYPT_ROUNDS = 12;

	private PasswordUtil() {

	}

	// PASSWORD STRENGTH VALIDATION

	public static boolean isStrongPassword(String password) {

		if (password == null || password.isEmpty()) {
			return false;
		}

		boolean hasMinimumLength = password.length() >= 8;
		boolean hasUppercase = password.matches(".*[A-Z].*");
		boolean hasLowercase = password.matches(".*[a-z].*");
		boolean hasNumber = password.matches(".*[0-9].*");
		boolean hasSpecial = password.matches(".*[^a-zA-Z0-9].*");

		return hasMinimumLength && hasUppercase && hasLowercase && hasNumber && hasSpecial;
	}

	// HASH PASSWORD

	public static String hashPassword(String plainPassword) {

		if (plainPassword == null || plainPassword.isEmpty()) {

			throw new IllegalArgumentException("Password cannot be empty.");

		}

		return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
	}

	// VERIFY PASSWORD

	public static boolean verifyPassword(String plainPassword, String storedHash) {

		if (plainPassword == null || storedHash == null || plainPassword.isEmpty() || storedHash.isEmpty()) {

			return false;
		}

		try {

			return BCrypt.checkpw(plainPassword, storedHash);

		} catch (IllegalArgumentException e) {

			return false;

		}

	}

}