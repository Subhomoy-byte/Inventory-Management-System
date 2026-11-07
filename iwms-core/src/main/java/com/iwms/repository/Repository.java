package com.iwms.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic Repository abstraction for entities in the IWMS domain.
 * Decouples domain services from underlying storage mechanism (In-Memory, File-Backed JSON).
 *
 * @param <T>  Entity type
 * @param <ID> Primary identifier type
 */
public interface Repository<T, ID> {

    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    void deleteById(ID id);

    boolean existsById(ID id);

    long count();

    void clear();
}
