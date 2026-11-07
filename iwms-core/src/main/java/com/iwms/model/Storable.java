package com.iwms.model;

/**
 * OOP: Interface for entities that can be persisted to file-based storage.
 */
public interface Storable {
    /**
     * Unique identifier for the storable entity.
     */
    String getId();

    /**
     * Storage key prefix or grouping for the entity.
     */
    String getStorageKey();
}
