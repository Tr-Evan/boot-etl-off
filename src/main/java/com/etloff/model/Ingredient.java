package com.etloff.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.HashSet;
import java.util.Set;

/**
 * A cleaned ingredient (e.g. {@code "Sucre"}), unique by name.
 */
@Entity
@Table(name = "ingredient", uniqueConstraints = @UniqueConstraint(name = "uk_ingredient_name", columnNames = "name"))
public class Ingredient extends AbstractReferenceEntity {

    /** Inverse side of {@link Product#getIngredients()} — used by the "top" query. */
    @ManyToMany(mappedBy = "ingredients", fetch = FetchType.LAZY)
    private Set<Product> products = new HashSet<>();

    protected Ingredient() {
    }

    public Ingredient(String name) {
        super(name);
    }

    public Set<Product> getProducts() {
        return products;
    }
}
