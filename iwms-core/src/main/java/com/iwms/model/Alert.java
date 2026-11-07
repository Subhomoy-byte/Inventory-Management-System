package com.iwms.model;

import java.time.Instant;

/**
 * OOP Principle: Encapsulation & Comparable Interface.
 * Represents an inventory alert with severity levels.
 * Implements Comparable for automated PriorityQueue prioritization.
 */
public record Alert(
        String id,
        String sku,
        String message,
        Severity severity,
        Instant createdAt
) implements Comparable<Alert> {

    public enum Severity {
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        CRITICAL(4);

        private final int priorityWeight;

        Severity(int priorityWeight) {
            this.priorityWeight = priorityWeight;
        }

        public int getPriorityWeight() {
            return priorityWeight;
        }
    }

    public Alert {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Alert ID cannot be blank");
        }
        if (severity == null) {
            severity = Severity.MEDIUM;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @Override
    public int compareTo(Alert other) {
        // Higher severity first, then older timestamps first
        int severityComparison = Integer.compare(other.severity.getPriorityWeight(), this.severity.getPriorityWeight());
        if (severityComparison != 0) {
            return severityComparison;
        }
        return this.createdAt.compareTo(other.createdAt);
    }
}
