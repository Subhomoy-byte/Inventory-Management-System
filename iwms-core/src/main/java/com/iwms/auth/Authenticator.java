package com.iwms.auth;

import com.iwms.model.User;

/**
 * Authentication strategy interface for password hashing and credential verification.
 */
public interface Authenticator {

    byte[] generateSalt();

    String hashPassword(String rawPassword, byte[] salt);

    boolean verifyPassword(String rawPassword, String storedHash, byte[] salt);

    boolean authenticate(User user, String rawPassword);
}
