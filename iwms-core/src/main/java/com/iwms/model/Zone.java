package com.iwms.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * OOP Principle: Encapsulation & Composition.
 * Represents a warehouse zone with an associated category preference and contained bins.
 */
public class Zone {
    private final String id;
    private final String name;
    private final Category categoryPreference;
    private final List<Bin> bins;

    public Zone(String id, String name, Category categoryPreference) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Zone ID cannot be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Zone name cannot be blank");
        }

        this.id = id.trim();
        this.name = name.trim();
        this.categoryPreference = categoryPreference != null ? categoryPreference : Category.GENERAL;
        this.bins = new ArrayList<>();
    }

    public synchronized void addBin(Bin bin) {
        Objects.requireNonNull(bin, "Bin cannot be null");
        if (bins.contains(bin)) {
            throw new IllegalArgumentException("Bin already exists in zone: " + bin.getId());
        }
        bins.add(bin);
    }

    public synchronized Optional<Bin> findBin(String binId) {
        return bins.stream().filter(b -> b.getId().equalsIgnoreCase(binId)).findFirst();
    }

    public synchronized double getFillPercentage() {
        if (bins.isEmpty()) {
            return 0.0;
        }
        int totalCapacity = bins.stream().mapToInt(Bin::getCapacityQuantity).sum();
        int currentQty = bins.stream().mapToInt(Bin::getCurrentQuantity).sum();
        return totalCapacity > 0 ? ((double) currentQty / totalCapacity) * 100.0 : 0.0;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Category getCategoryPreference() {
        return categoryPreference;
    }

    public synchronized List<Bin> getBins() {
        return Collections.unmodifiableList(bins);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Zone zone = (Zone) o;
        return Objects.equals(id, zone.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
