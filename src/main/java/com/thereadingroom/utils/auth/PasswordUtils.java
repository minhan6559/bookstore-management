package com.thereadingroom.utils.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class PasswordUtils {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(12);

    private PasswordUtils() {
    }

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null)
            return null;
        return ENCODER.encode(plainPassword);
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null)
            return false;
        return ENCODER.matches(plainPassword, hashedPassword);
    }

    public static boolean isBcryptHash(String value) {
        if (value == null)
            return false;
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
