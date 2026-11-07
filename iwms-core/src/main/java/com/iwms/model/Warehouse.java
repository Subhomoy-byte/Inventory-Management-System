package com.iwms.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * OOP Principle: Encapsulation & Composition.
 * Represents a physical warehouse facility containing zones and storage bins.
 */
public class Warehouse {
    private final String id;
    private final String name;
    private final String location;
    private final List<Zone> zones;

    public Warehouse(String id, String name, String location) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Warehouse ID cannot be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Warehouse name cannot be blank");
        }

        this.id = id.trim();
        this.name = name.trim();
        this.location = location != null ? location.trim() : "Default Location";
        this.zones = new ArrayList<>();
    }

    public synchronized void addZone(Zone zone) {
        Objects.requireNonNull(zone, "Zone cannot be null");
        if (zones.contains(zone)) {
            throw new IllegalArgumentException("Zone already registered in warehouse: " + zone.getId());
        }
        zones.add(zone);
    }

    public synchronized Optional<Bin> findBin(String binId) {
        for (Zone zone : zones) {
            Optional<Bin> found = zone.findBin(binId);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public synchronized List<Zone> getZones() {
        return Collections.unmodifiableList(zones);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Warehouse warehouse = (Warehouse) o;
        return Objects.equals(id, warehouse.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
