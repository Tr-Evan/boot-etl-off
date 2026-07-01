package com.etloff.etl;

import com.etloff.model.Nutriments;

import java.util.Set;

/**
 * An immutable, fully parsed and cleaned representation of one CSV line, produced by
 * {@link CsvParser} and consumed by the batch writer.
 *
 * <p>Free-text columns are already split and cleaned into name sets, and the numeric
 * columns are already parsed into a {@link Nutriments} value object, so the parallel
 * write phase does no string work at all.</p>
 *
 * @param nom                the product name
 * @param nutritionGradeFr   the Nutri-Score grade a..e, or {@code null} if unknown
 * @param presenceHuilePalme palm-oil flag (0/1), or {@code null}
 * @param categoryName       the cleaned category name, or {@code null}
 * @param brandName          the cleaned brand name, or {@code null}
 * @param nutriments         the parsed per-100g nutritional values
 * @param ingredientNames    cleaned, deduplicated ingredient names
 * @param allergenNames      cleaned, deduplicated allergen names
 * @param additiveNames      additive labels (E-code + name)
 */
public record ProductRow(
        String nom,
        String nutritionGradeFr,
        Integer presenceHuilePalme,
        String categoryName,
        String brandName,
        Nutriments nutriments,
        Set<String> ingredientNames,
        Set<String> allergenNames,
        Set<String> additiveNames) {
}
