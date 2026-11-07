package com.iwms.model;

import java.util.EnumSet;

/**
 * OOP Principle: Inheritance & Polymorphism.
 * System Administrator with full unbounded access across all operational, financial, and management domains.
 */
public class AdminUser extends User {

    public AdminUser(String id, String username, String passwordHash, byte[] salt, String fullName) {
        super(id, username, passwordHash, salt, fullName, Role.ADMIN);
    }

    @Override
    public EnumSet<Permission> getPermissions() {
        return EnumSet.allOf(Permission.class);
    }
}
