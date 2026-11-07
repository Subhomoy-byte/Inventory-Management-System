package com.iwms.model;

import java.time.Instant;

/**
 * OOP: Interface for entities that maintain an audit trail.
 */
public interface Auditable {
    String getCreatedBy();
    Instant getCreatedAt();
}
