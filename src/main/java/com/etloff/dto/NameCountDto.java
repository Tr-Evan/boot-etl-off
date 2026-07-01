package com.etloff.dto;

/**
 * A reference name together with the number of products that use it.
 * Populated directly by JPQL constructor-expression queries.
 *
 * @param name  the reference name (ingredient, allergen or additive)
 * @param count the number of distinct products referencing it
 */
public record NameCountDto(String name, long count) {
}
