package com.iwms.repository;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.iwms.exception.StorageException;
import com.iwms.model.*;
import com.iwms.storage.AtomicFileWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/**
 * File-backed repository that caches entities in memory and persists them atomically as JSON.
 *
 * @param <T>  Entity type
 * @param <ID> Primary identifier type
 */
public class FileBackedRepository<T, ID> extends InMemoryRepository<T, ID> {

    private final Path storageFile;
    private final Class<T> entityClass;
    private final ObjectMapper objectMapper;

    public FileBackedRepository(Path storageFile, Class<T> entityClass, Function<T, ID> idExtractor) {
        super(idExtractor);
        this.storageFile = Objects.requireNonNull(storageFile, "Storage file cannot be null");
        this.entityClass = Objects.requireNonNull(entityClass, "Entity class cannot be null");
        this.objectMapper = createObjectMapper();

        loadFromFile();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Register polymorphic custom deserializer for Product hierarchy if handling products
        if (Product.class.isAssignableFrom(entityClass)) {
            SimpleModule productModule = new SimpleModule();
            productModule.addDeserializer(Product.class, new ProductDeserializer());
            mapper.registerModule(productModule);
        }

        return mapper;
    }

    private synchronized void loadFromFile() {
        if (!Files.exists(storageFile)) {
            return;
        }

        try {
            String json = Files.readString(storageFile);
            if (json.isBlank()) {
                return;
            }

            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, entityClass);
            List<T> entities = objectMapper.readValue(json, listType);
            for (T entity : entities) {
                ID id = idExtractor.apply(entity);
                if (id != null) {
                    storage.put(id, entity);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load repository entities from: " + storageFile, e);
        }
    }

    private synchronized void persistToFile() {
        try {
            List<T> allEntities = new ArrayList<>(storage.values());
            String json = objectMapper.writeValueAsString(allEntities);
            AtomicFileWriter.writeString(storageFile, json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist repository entities to: " + storageFile, e);
        }
    }

    @Override
    public T save(T entity) {
        T saved = super.save(entity);
        persistToFile();
        return saved;
    }

    @Override
    public void deleteById(ID id) {
        super.deleteById(id);
        persistToFile();
    }

    @Override
    public void clear() {
        super.clear();
        persistToFile();
    }

    /**
     * Custom Jackson deserializer to handle abstract Product and its polymorphic subclasses.
     */
    private static class ProductDeserializer extends JsonDeserializer<Product> {
        @Override
        public Product deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            String sku = node.has("sku") ? node.get("sku").asText() : "";
            String name = node.has("name") ? node.get("name").asText() : "";
            Category category = node.has("category") ? Category.valueOf(node.get("category").asText()) : Category.GENERAL;
            double unitPrice = node.has("unitPrice") ? node.get("unitPrice").asDouble() : 0.0;
            double costPrice = node.has("costPrice") ? node.get("costPrice").asDouble() : 0.0;
            int minStockLevel = node.has("minStockLevel") ? node.get("minStockLevel").asInt() : 0;
            String createdBy = node.has("createdBy") ? node.get("createdBy").asText() : "system";
            Instant createdAt = node.has("createdAt") ? Instant.parse(node.get("createdAt").asText()) : Instant.now();

            switch (category) {
                case PERISHABLE -> {
                    int shelfLife = node.has("shelfLifeDays") ? node.get("shelfLifeDays").asInt() : 14;
                    double temp = node.has("storageTempCelsius") ? node.get("storageTempCelsius").asDouble() : 4.0;
                    boolean refrig = node.has("requiresRefrigeration") && node.get("requiresRefrigeration").asBoolean();
                    return PerishableProduct.builder()
                            .sku(sku).name(name).category(category).unitPrice(unitPrice).costPrice(costPrice)
                            .minStockLevel(minStockLevel).createdBy(createdBy).createdAt(createdAt)
                            .shelfLifeDays(shelfLife).storageTempCelsius(temp).requiresRefrigeration(refrig)
                            .build();
                }
                case ELECTRONICS -> {
                    int warranty = node.has("warrantyMonths") ? node.get("warrantyMonths").asInt() : 12;
                    int voltage = node.has("voltage") ? node.get("voltage").asInt() : 110;
                    boolean batt = node.has("hasLithiumBattery") && node.get("hasLithiumBattery").asBoolean();
                    return ElectronicProduct.builder()
                            .sku(sku).name(name).category(category).unitPrice(unitPrice).costPrice(costPrice)
                            .minStockLevel(minStockLevel).createdBy(createdBy).createdAt(createdAt)
                            .warrantyMonths(warranty).voltage(voltage).hasLithiumBattery(batt)
                            .build();
                }
                case FRAGILE -> {
                    int fragility = node.has("fragilityIndex") ? node.get("fragilityIndex").asInt() : 5;
                    int maxStack = node.has("maxStackHeight") ? node.get("maxStackHeight").asInt() : 2;
                    return FragileProduct.builder()
                            .sku(sku).name(name).category(category).unitPrice(unitPrice).costPrice(costPrice)
                            .minStockLevel(minStockLevel).createdBy(createdBy).createdAt(createdAt)
                            .fragilityIndex(fragility).maxStackHeight(maxStack)
                            .build();
                }
                case BULK -> {
                    double weight = node.has("weightKg") ? node.get("weightKg").asDouble() : 10.0;
                    double vol = node.has("volumeCubicMeters") ? node.get("volumeCubicMeters").asDouble() : 0.5;
                    String uom = node.has("unitOfMeasure") ? node.get("unitOfMeasure").asText() : "UNIT";
                    return BulkProduct.builder()
                            .sku(sku).name(name).category(category).unitPrice(unitPrice).costPrice(costPrice)
                            .minStockLevel(minStockLevel).createdBy(createdBy).createdAt(createdAt)
                            .weightKg(weight).volumeCubicMeters(vol).unitOfMeasure(uom)
                            .build();
                }
                default -> {
                    return BulkProduct.builder()
                            .sku(sku).name(name).category(Category.BULK).unitPrice(unitPrice).costPrice(costPrice)
                            .minStockLevel(minStockLevel).createdBy(createdBy).createdAt(createdAt)
                            .weightKg(1.0).volumeCubicMeters(0.01).unitOfMeasure("UNIT")
                            .build();
                }
            }
        }
    }
}
