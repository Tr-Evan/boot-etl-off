package com.etloff.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.util.Objects;

/**
 * Base type for the deduplicated reference entities of the model
 * ({@link Category}, {@link Brand}, {@link Ingredient}, {@link Allergen},
 * {@link Additive}).
 *
 * <p>Each reference is uniquely identified in the database by its business
 * {@code name} (enforced with a unique constraint on the concrete tables), and
 * both {@code equals}/{@code hashCode} are built on that name — not on the
 * surrogate {@code id} — so instances behave correctly in {@link java.util.Set}
 * collections even before they are persisted.</p>
 *
 * <p>Subclasses declare their own {@code SEQUENCE} generator. Sequences (rather
 * than {@code IDENTITY}) are deliberate: they let Hibernate pre-allocate ids and
 * keep JDBC batch inserts enabled.</p>
 */
@MappedSuperclass
public abstract class AbstractReferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String name;

    protected AbstractReferenceEntity() {
    }

    protected AbstractReferenceEntity(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Business-key equality: two references of the <em>same concrete type</em>
     * are equal when they carry the same name.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractReferenceEntity that = (AbstractReferenceEntity) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", name='" + name + "'}";
    }
}
