package com.etloff.etl;

import com.etloff.model.Additive;
import com.etloff.model.Allergen;
import com.etloff.model.Brand;
import com.etloff.model.Category;
import com.etloff.model.Ingredient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Collects every distinct reference name from the parsed rows and persists the reference
 * entities <em>once</em>, up front — then hands back a {@link ReferenceCaches} the parallel
 * product writer can resolve against.
 *
 * <p>Doing this before touching products serves two purposes:</p>
 * <ul>
 *     <li><b>No repeated {@code SELECT}s</b>: each reference is created exactly once and
 *         cached by name.</li>
 *     <li><b>No unique-constraint races</b>: names are deduplicated in memory before any
 *         insert, so concurrent chunks never insert the same value twice.</li>
 * </ul>
 *
 * <p>The distinct names are gathered single-threaded (cheap), entity instances are created
 * and cached, then the inserts are fanned out over virtual threads via
 * {@link ParallelChunkProcessor} — this turns what used to be the ETL's biggest sequential
 * cost into a fully parallel phase.</p>
 */
@Service
public class ReferencePersister {

    private static final Logger log = LoggerFactory.getLogger(ReferencePersister.class);

    private final ReferenceChunkWriter chunkWriter;
    private final ParallelChunkProcessor parallelChunkProcessor;
    private final int chunkSize;
    private final int maxConcurrency;

    public ReferencePersister(ReferenceChunkWriter chunkWriter,
                              ParallelChunkProcessor parallelChunkProcessor,
                              @Value("${etl.ref-chunk-size:500}") int chunkSize,
                              @Value("${etl.max-concurrency:16}") int maxConcurrency) {
        this.chunkWriter = chunkWriter;
        this.parallelChunkProcessor = parallelChunkProcessor;
        this.chunkSize = chunkSize;
        this.maxConcurrency = maxConcurrency;
    }

    /**
     * Builds the distinct name sets from the rows, persists all references in parallel and
     * returns the populated caches.
     *
     * @param rows all parsed product rows
     * @return caches mapping each reference name to its persisted entity
     */
    public ReferenceCaches persistReferences(List<ProductRow> rows) {
        Set<String> categoryNames = new HashSet<>();
        Set<String> brandNames = new HashSet<>();
        Set<String> ingredientNames = new HashSet<>(32_768);
        Set<String> allergenNames = new HashSet<>();
        Set<String> additiveNames = new HashSet<>();

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

        // Create entity instances, cache them by name, and collect them all for insertion.
        // The cache holds the very same instances that get persisted, so once the fan-out
        // assigns their ids the caches expose them transparently.
        List<Object> toPersist = new ArrayList<>(
                categoryNames.size() + brandNames.size() + ingredientNames.size()
                        + allergenNames.size() + additiveNames.size());

        ReferenceCaches caches = new ReferenceCaches(
                buildCache(categoryNames, Category::new, toPersist),
                buildCache(brandNames, Brand::new, toPersist),
                buildCache(ingredientNames, Ingredient::new, toPersist),
                buildCache(allergenNames, Allergen::new, toPersist),
                buildCache(additiveNames, Additive::new, toPersist));

        int failedChunks = parallelChunkProcessor.process(
                toPersist, chunkSize, maxConcurrency, chunkWriter::persistChunk);
        if (failedChunks > 0) {
            log.warn("{} reference chunk(s) failed to persist", failedChunks);
        }

        log.info("Persisted references — categories={}, brands={}, ingredients={}, allergens={}, additives={}",
                caches.categories().size(), caches.brands().size(), caches.ingredients().size(),
                caches.allergens().size(), caches.additives().size());
        return caches;
    }

    /**
     * Creates one entity per name, registers it in a fresh cache and appends it to the flat
     * list of entities to persist.
     *
     * @param names     the distinct names of this reference type
     * @param factory   builds an entity from a name
     * @param collector accumulator receiving every created entity (for the fan-out)
     * @param <T>       the reference entity type
     * @return the populated name→entity cache
     */
    private <T> ConcurrentHashMap<String, T> buildCache(Set<String> names,
                                                        Function<String, T> factory,
                                                        List<Object> collector) {
        ConcurrentHashMap<String, T> cache = new ConcurrentHashMap<>(Math.max(16, names.size() * 4 / 3));
        for (String name : names) {
            T entity = factory.apply(name);
            cache.put(name, entity);
            collector.add(entity);
        }
        return cache;
    }
}
