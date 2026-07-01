package com.etloff.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.HashSet;
import java.util.Set;

/**
 * A cleaned allergen (e.g. {@code "Lait"}), unique by name.
 */
@Entity
@Table(name = "allergen", uniqueConstraints = @UniqueConstraint(name = "uk_allergen_name", columnNames = "name"))
public class Allergen extends AbstractReferenceEntity {

    /** Inverse side of {@link Product#getAllergens()} — used by the "top" query. */
    @ManyToMany(mappedBy = "allergens", fetch = FetchType.LAZY)
    private Set<Product> products = new HashSet<>();

    protected Allergen() {
    }

    public Allergen(String name) {
        super(name);
    }

    public Set<Product> getProducts() {
        return products;
    }
}
