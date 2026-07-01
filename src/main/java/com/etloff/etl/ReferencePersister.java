package com.etloff.etl;

import com.etloff.model.Additive;
import com.etloff.model.Allergen;
import com.etloff.model.Brand;
import com.etloff.model.Category;
import com.etloff.model.Ingredient;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Collects every distinct reference name from the parsed rows and persists the reference
 * entities <em>once</em>, up front, in batched inserts — then hands back a
 * {@link ReferenceCaches} the parallel product writer can resolve against.
 *
 * <p>Doing this before touching products serves two purposes:</p>
 * <ul>
 *     <li><b>No repeated {@code SELECT}s</b>: each reference is created exactly once and
 *         cached by name.</li>
 *     <li><b>No unique-constraint races</b>: because references are inserted single-threaded
 *         before the fan-out, concurrent product-writing threads never try to insert the
 *         same category/ingredient/… twice.</li>
 * </ul>
 */
@Service
public class ReferencePersister {

    private static final Logger log = LoggerFactory.getLogger(ReferencePersister.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final int batchSize;

    public ReferencePersister(@Value("${spring.jpa.properties.hibernate.jdbc.batch_size:100}") int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * Builds the distinct name sets from the rows, persists all references and returns the
     * populated caches. Runs in a single transaction so the batched inserts share one flush
     * cadence.
     *
     * @param rows all parsed product rows
     * @return caches mapping each reference name to its persisted entity
     */
    @Transactional
    public ReferenceCaches persistReferences(List<ProductRow> rows) {
        // TreeSet => deterministic, sorted insertion order (nicer ids, stable logs).
        Set<String> categoryNames = new TreeSet<>();
        Set<String> brandNames = new TreeSet<>();
        Set<String> ingredientNames = new TreeSet<>();
        Set<String> allergenNames = new TreeSet<>();
        Set<String> additiveNames = new TreeSet<>();

        for (ProductRow row : rows) {
            if (row.categoryName() != null) {
                categoryNames.add(row.categoryName());
            }
            if (row.brandName() != null) {
                brandNames.add(row.brandName());
            }
            ingredientNames.addAll(row.ingredientNames());
            allergenNames.addAll(row.allergenNames());
            additiveNames.addAll(row.additiveNames());
        }

        ReferenceCaches caches = new ReferenceCaches(
                persistAll(categoryNames, Category::new),
                persistAll(brandNames, Brand::new),
                persistAll(ingredientNames, Ingredient::new),
                persistAll(allergenNames, Allergen::new),
                persistAll(additiveNames, Additive::new));

        // Flush the last partial batch and detach everything; the caches keep the entities.
        entityManager.flush();
        entityManager.clear();

        log.info("Persisted references — categories={}, brands={}, ingredients={}, allergens={}, additives={}",
                caches.categories().size(), caches.brands().size(), caches.ingredients().size(),
                caches.allergens().size(), caches.additives().size());
        return caches;
    }

    /**
     * Persists one reference type with periodic batch flushes and returns a name→entity map.
     *
     * @param names   the distinct names to persist
     * @param factory constructs a new entity from a name
     * @param <T>     the reference entity type
     * @return a concurrent map from name to the persisted entity
     */
    private <T> ConcurrentHashMap<String, T> persistAll(Set<String> names, Function<String, T> factory) {
        ConcurrentHashMap<String, T> cache = new ConcurrentHashMap<>(Math.max(16, names.size() * 4 / 3));
        int i = 0;
        for (String name : names) {
            T entity = factory.apply(name);
            entityManager.persist(entity);
            cache.put(name, entity);
            if (++i % batchSize == 0) {
                entityManager.flush();
            }
        }
        return cache;
    }
}