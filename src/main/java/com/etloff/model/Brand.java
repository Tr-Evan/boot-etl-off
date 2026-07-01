package com.etloff.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.HashSet;
import java.util.Set;

/**
 * A product brand (e.g. {@code "La Patelière"}), unique by name.
 */
@Entity
@Table(name = "brand", uniqueConstraints = @UniqueConstraint(name = "uk_brand_name", columnNames = "name"))
public class Brand extends AbstractReferenceEntity {

    /** Inverse side of {@link Product#getBrand()} — never eagerly loaded. */
    @OneToMany(mappedBy = "brand", fetch = FetchType.LAZY)
    private Set<Product> products = new HashSet<>();

    protected Brand() {
    }

    public Brand(String name) {
        super(name);
    }

    public Set<Product> getProducts() {
        return products;
    }
}
