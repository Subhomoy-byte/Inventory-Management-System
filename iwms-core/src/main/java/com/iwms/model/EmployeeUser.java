package com.iwms.model;

import java.util.EnumSet;

/**
 * OOP Principle: Inheritance & Polymorphism.
 * Warehouse Floor Associate with day-to-day scanning, receiving, and picking access.
 * Strictly excludes financial valuation, profit margins, cost price visibility, and system management.
 */
public class EmployeeUser extends User {

    public EmployeeUser(String id, String username, String passwordHash, byte[] salt, String fullName) {
        super(id, username, passwordHash, salt, fullName, Role.EMPLOYEE);
    }

    @Override
    public EnumSet<Permission> getPermissions() {
        return EnumSet.of(
                Permission.STOCK_IN,
                Permission.STOCK_OUT,
                Permission.STOCK_TRANSFER,
                Permission.STOCK_ADJUST,
                Permission.CYCLE_COUNT,
                Permission.VIEW_ALERTS,
                Permission.VIEW_OWN_MOVEMENTS,
                Permission.GENERATE_FLOOR_REPORT
        );
    }
}
