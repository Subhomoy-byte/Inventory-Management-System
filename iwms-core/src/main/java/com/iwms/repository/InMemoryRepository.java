package com.iwms.repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Thread-safe In-Memory implementation of Repository backed by ConcurrentHashMap.
 *
 * @param <T>  Entity type
 * @param <ID> Primary identifier type
 */
public class InMemoryRepository<T, ID> implements Repository<T, ID> {

    protected final Map<ID, T> storage = new ConcurrentHashMap<>();
    protected final Function<T, ID> idExtractor;

    public InMemoryRepository(Function<T, ID> idExtractor) {
        this.idExtractor = Objects.requireNonNull(idExtractor, "idExtractor cannot be null");
    }

    @Override
    public T save(T entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        ID id = idExtractor.apply(entity);
        Objects.requireNonNull(id, "Extracted ID cannot be null");
        storage.put(id, entity);
        return entity;
    }

    @Override
    public Optional<T> findById(ID id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public void deleteById(ID id) {
        if (id != null) {
            storage.remove(id);
        }
    }

    @Override
    public boolean existsById(ID id) {
        return id != null && storage.containsKey(id);
    }

    @Override
    public long count() {
        return storage.size();
    }

    @Override
    public void clear() {
        storage.clear();
    }
}
