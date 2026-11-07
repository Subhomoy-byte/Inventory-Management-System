package com.iwms.model;

/**
 * OOP: Enum defining User Roles for Role-Based Access Control (RBAC).
 */
public enum Role {
    ADMIN("System Administrator"),
    MANAGER("Warehouse Manager"),
    EMPLOYEE("Warehouse Floor Employee");

    private final String title;

    Role(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
