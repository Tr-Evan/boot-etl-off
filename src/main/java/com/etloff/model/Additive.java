package com.etloff.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.HashSet;
import java.util.Set;

/**
 * A food additive, kept with its full label (e.g. {@code "E500 - Carbonates de sodium"}),
 * unique by name. The leading E-code is preserved because it is the stable identifier.
 */
@Entity
@Table(name = "additive", uniqueConstraints = @UniqueConstraint(name = "uk_additive_name", columnNames = "name"))
public class Additive extends AbstractReferenceEntity {

    /** Inverse side of {@link Product#getAdditives()} — used by the "top" query. */
    @ManyToMany(mappedBy = "additives", fetch = FetchType.LAZY)
    private Set<Product> products = new HashSet<>();

    protected Additive() {
    }

    public Additive(String name) {
        super(name);
    }

    public Set<Product> getProducts() {
        return products;
    }
}
