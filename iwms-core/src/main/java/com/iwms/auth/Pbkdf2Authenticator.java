package com.iwms.auth;

import com.iwms.exception.UnauthorizedException;
import com.iwms.model.User;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Objects;

/**
 * PBKDF2 with HMAC-SHA256 Authenticator implementation.
 * Uses 100,000 iterations and 256-bit key length for cryptographic resistance against brute-force attacks.
 * Tracks failed login attempts and enforces automatic account lockout.
 */
public class Pbkdf2Authenticator implements Authenticator {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 100_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        return salt;
    }

    @Override
    public String hashPassword(String rawPassword, byte[] salt) {
        Objects.requireNonNull(rawPassword, "Password cannot be null");
        Objects.requireNonNull(salt, "Salt cannot be null");

        try {
            PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to hash password with algorithm: " + ALGORITHM, e);
        }
    }

    @Override
    public boolean verifyPassword(String rawPassword, String storedHash, byte[] salt) {
        if (rawPassword == null || storedHash == null || salt == null) {
            return false;
        }
        String computedHash = hashPassword(rawPassword, salt);
        return slowEquals(computedHash.getBytes(), storedHash.getBytes());
    }

    @Override
    public boolean authenticate(User user, String rawPassword) {
        Objects.requireNonNull(user, "User cannot be null");

        if (user.isAccountLocked()) {
            throw new UnauthorizedException("Account is locked due to excessive failed attempts. Please try again later.");
        }

        boolean valid = verifyPassword(rawPassword, user.getPasswordHash(), user.getSalt());
        if (valid) {
            user.resetFailedLogins();
            return true;
        } else {
            user.recordFailedLogin();
            return false;
        }
    }

    /**
     * Constant-time byte comparison to defend against timing side-channel attacks.
     */
    private boolean slowEquals(byte[] a, byte[] b) {
        int diff = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
