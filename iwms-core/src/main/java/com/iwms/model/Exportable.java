package com.iwms.model;

/**
 * OOP: Interface for entities capable of CSV serialization.
 */
public interface Exportable {
    String[] toCsvRow();
}
