package com.etloff.etl;

import com.etloff.model.Additive;
import com.etloff.model.Allergen;
import com.etloff.model.Ingredient;
import com.etloff.model.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persists a chunk of {@link ProductRow}s to the database inside its own transaction.
 *
 * <p>Invoked concurrently by many virtual threads (see {@link EtlService}). Each call
 * gets its own {@link EntityManager}/transaction, so the chunks are fully independent and
 * inserts batch cleanly. Associations are resolved from the pre-built
 * {@link ReferenceCaches} — no {@code SELECT} is issued per row.</p>
 *
 * <p>The batch is flushed and the persistence context cleared every {@code batch_size}
 * rows to bound heap usage and let Hibernate ship JDBC batches steadily.</p>
 */
@Service
public class ProductBatchWriter {

    @PersistenceContext
    private EntityManager entityManager;

    private final int batchSize;

    public ProductBatchWriter(@Value("${spring.jpa.properties.hibernate.jdbc.batch_size:100}") int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * Persists all rows in the chunk within a single new transaction.
     *
     * @param rows   the rows to persist
     * @param caches the shared, read-only reference caches
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeChunk(List<ProductRow> rows, ReferenceCaches caches) {
        int i = 0;
        for (ProductRow row : rows) {
            entityManager.persist(toProduct(row, caches));
            if (++i % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Builds a managed-ready {@link Product} from a row, linking it to the cached
     * (detached, already-persisted) reference entities by name.
     */
    private Product toProduct(ProductRow row, ReferenceCaches caches) {
        Product product = new Product(row.nom());
        product.setNutritionGradeFr(row.nutritionGradeFr());
        product.setPresenceHuilePalme(row.presenceHuilePalme());
        product.setNutriments(row.nutriments());

        if (row.categoryName() != null) {
            product.setCategory(caches.categories().get(row.categoryName()));
        }
        if (row.brandName() != null) {
            product.setBrand(caches.brands().get(row.brandName()));
        }

        for (String name : row.ingredientNames()) {
            Ingredient ingredient = caches.ingredients().get(name);
            if (ingredient != null) {
                product.getIngredients().add(ingredient);
            }
        }
        for (String name : row.allergenNames()) {
            Allergen allergen = caches.allergens().get(name);
            if (allergen != null) {
                product.getAllergens().add(allergen);
            }
        }
        for (String name : row.additiveNames()) {
            Additive additive = caches.additives().get(name);
            if (additive != null) {
                product.getAdditives().add(additive);
            }
        }
        return product;
    }
}
