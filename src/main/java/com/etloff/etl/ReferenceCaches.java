package com.etloff.etl;

import com.etloff.model.Additive;
import com.etloff.model.Allergen;
import com.etloff.model.Brand;
import com.etloff.model.Category;
import com.etloff.model.Ingredient;

import java.util.Map;

/**
 * In-memory, name-keyed cache of every reference entity, built once at the start of an
 * ETL run.
 *
 * <p>This is the core "avoid repeated {@code SELECT}s" optimization: instead of querying
 * the database for a category/brand/ingredient/… before each product insert, the writer
 * resolves references by name from these maps in O(1). The referenced entities are
 * already persisted (they carry database ids) but detached — the writer only reads their
 * id/name, which is safe to share across the parallel write threads.</p>
 *
 * @param categories  name → persisted {@link Category}
 * @param brands      name → persisted {@link Brand}
 * @param ingredients name → persisted {@link Ingredient}
 * @param allergens   name → persisted {@link Allergen}
 * @param additives   name → persisted {@link Additive}
 */
public record ReferenceCaches(
        Map<String, Category> categories,
        Map<String, Brand> brands,
        Map<String, Ingredient> ingredients,
        Map<String, Allergen> allergens,
        Map<String, Additive> additives) {
}
