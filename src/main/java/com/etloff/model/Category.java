package com.etloff.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.HashSet;
import java.util.Set;

/**
 * A product category (e.g. {@code "Additifs alimentaires"}), unique by name.
 */
@Entity
@Table(name = "category", uniqueConstraints = @UniqueConstraint(name = "uk_category_name", columnNames = "name"))
public class Category extends AbstractReferenceEntity {

    /** Inverse side of {@link Product#getCategory()} — never eagerly loaded. */
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private Set<Product> products = new HashSet<>();

    protected Category() {
    }

    public Category(String name) {
        super(name);
    }

    public Set<Product> getProducts() {
        return products;
    }
}