package com.iwms.model;

import java.util.EnumSet;

/**
 * OOP Principle: Inheritance & Polymorphism.
 * Warehouse Operations Manager with inventory, reporting, and financial oversight,
 * excluding user management and root system administration.
 */
public class ManagerUser extends User {

    public ManagerUser(String id, String username, String passwordHash, byte[] salt, String fullName) {
        super(id, username, passwordHash, salt, fullName, Role.MANAGER);
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
                Permission.GENERATE_FLOOR_REPORT,
                Permission.VIEW_FINANCIALS,
                Permission.VIEW_COST_PRICES,
                Permission.DELETE_PRODUCTS
        );
    }
}
