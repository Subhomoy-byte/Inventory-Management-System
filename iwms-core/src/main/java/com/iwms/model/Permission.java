package com.iwms.model;

/**
 * OOP: Enum defining granular permissions for access evaluation.
 * Evaluated efficiently using EnumSet.
 */
public enum Permission {
    STOCK_IN,
    STOCK_OUT,
    STOCK_TRANSFER,
    STOCK_ADJUST,
    CYCLE_COUNT,
    VIEW_ALERTS,
    VIEW_OWN_MOVEMENTS,
    GENERATE_FLOOR_REPORT,
    VIEW_FINANCIALS,
    VIEW_COST_PRICES,
    MANAGE_USERS,
    DELETE_PRODUCTS,
    MANAGE_SYSTEM
}
