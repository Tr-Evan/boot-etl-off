package com.etloff.etl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persists a chunk of reference entities (any of {@code Category}, {@code Brand},
 * {@code Ingredient}, {@code Allergen}, {@code Additive}) in its own transaction, with
 * periodic batch flushes.
 *
 * <p>Because the distinct names were already deduplicated in memory before the fan-out,
 * two concurrent chunks can never insert the same name — so parallel reference insertion is
 * race-free. Reference tables have no outgoing foreign keys, so there is no lock contention
 * between chunks either.</p>
 *
 * <p>The {@code SEQUENCE} generator assigns each entity its id at {@code persist()} time, so
 * the shared cache (which holds these very instances) sees the ids even though the context
 * is cleared after each batch.</p>
 */
@Service
public class ReferenceChunkWriter {

    @PersistenceContext
    private EntityManager entityManager;

    private final int batchSize;

    public ReferenceChunkWriter(@Value("${spring.jpa.properties.hibernate.jdbc.batch_size:100}") int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * Persists every entity in the chunk within a single new transaction.
     *
     * @param entities the reference entities to persist
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistChunk(List<Object> entities) {
        int i = 0;
        for (Object entity : entities) {
            entityManager.persist(entity);
            if (++i % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();
    }
}
