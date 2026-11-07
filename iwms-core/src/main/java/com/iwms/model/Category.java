package com.iwms.model;

/**
 * OOP: Enum defining warehouse product classification.
 */
public enum Category {
    PERISHABLE("Perishable Foods & Beverages"),
    ELECTRONICS("Consumer Electronics & Hardware"),
    FRAGILE("Glassware, Ceramics & Precision Goods"),
    BULK("Bulk Raw Materials & Commodities"),
    GENERAL("General Merchandise");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
