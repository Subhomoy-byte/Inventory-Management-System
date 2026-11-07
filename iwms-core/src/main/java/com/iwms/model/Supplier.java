package com.iwms.model;

import java.time.Instant;
import java.util.Objects;

/**
 * OOP Principle: Encapsulation.
 * Represents an approved goods supplier.
 */
public class Supplier implements Storable, Auditable {
    private final String id;
    private String name;
    private String contactEmail;
    private String phone;
    private int leadTimeDays;
    private final String createdBy;
    private final Instant createdAt;

    public Supplier(String id, String name, String contactEmail, String phone, int leadTimeDays, String createdBy) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Supplier ID cannot be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Supplier name cannot be blank");
        }

        this.id = id.trim();
        this.name = name.trim();
        this.contactEmail = contactEmail != null ? contactEmail.trim() : "";
        this.phone = phone != null ? phone.trim() : "";
        this.leadTimeDays = Math.max(1, leadTimeDays);
        this.createdBy = createdBy != null ? createdBy : "admin";
        this.createdAt = Instant.now();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getStorageKey() {
        return "suppliers";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(int leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Supplier supplier = (Supplier) o;
        return Objects.equals(id, supplier.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
