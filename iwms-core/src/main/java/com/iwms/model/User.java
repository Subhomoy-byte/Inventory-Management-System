package com.iwms.model;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;

/**
 * OOP Principle: Abstraction & Encapsulation.
 * Abstract base class for all system users in the IWMS application.
 * Defines identity, credentials, security state, and abstract RBAC permissions.
 */
public abstract class User {

    private final String id;
    private final String username;
    private String passwordHash;
    private byte[] salt;
    private String fullName;
    private final Role role;
    private int failedLoginAttempts;
    private Instant lockedUntil;
    private boolean active;
    private final Instant createdAt;

    protected User(String id, String username, String passwordHash, byte[] salt, String fullName, Role role) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("User ID cannot be blank");
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username cannot be blank");
        if (role == null) throw new IllegalArgumentException("Role cannot be null");

        this.id = id.trim();
        this.username = username.trim().toLowerCase();
        this.passwordHash = passwordHash;
        this.salt = salt != null ? salt.clone() : new byte[0];
        this.fullName = fullName != null ? fullName.trim() : username;
        this.role = role;
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.active = true;
        this.createdAt = Instant.now();
    }

    /**
     * Polymorphic method implemented by subclasses to specify granted permissions.
     */
    public abstract EnumSet<Permission> getPermissions();

    public boolean hasPermission(Permission permission) {
        return getPermissions().contains(permission);
    }

    public boolean isAccountLocked() {
        if (lockedUntil == null) {
            return false;
        }
        if (Instant.now().isAfter(lockedUntil)) {
            // Lock expired, reset
            lockedUntil = null;
            failedLoginAttempts = 0;
            return false;
        }
        return true;
    }

    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = Instant.now().plus(Duration.ofMinutes(15));
        }
    }

    public void resetFailedLogins() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public byte[] getSalt() {
        return salt.clone();
    }

    public void setSalt(byte[] salt) {
        this.salt = salt != null ? salt.clone() : new byte[0];
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
