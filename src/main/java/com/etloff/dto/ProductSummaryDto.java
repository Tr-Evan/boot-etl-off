package com.etloff.dto;

/**
 * Lightweight, read-optimized view of a product for the REST API.
 *
 * <p>Built with a JPQL constructor expression so the "top-N" queries never trigger
 * lazy loading of associations or serialize whole entity graphs.</p>
 *
 * @param nom              the product name
 * @param brand            the brand name (may be {@code null})
 * @param category         the category name (may be {@code null})
 * @param nutritionGradeFr the Nutri-Score grade a..e (may be {@code null})
 * @param energie100g      energy per 100g (may be {@code null})
 */
public record ProductSummaryDto(
        String nom,
        String brand,
        String category,
        String nutritionGradeFr,
        Double energie100g) {
}
