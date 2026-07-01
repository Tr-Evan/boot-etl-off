package com.etloff.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * A food product ingested from the Open Food Facts CSV.
 *
 * <p>Holds its own scalar attributes (name, Nutri-Score grade, palm-oil flag)
 * and the {@link Nutriments per-100g values}, plus associations to the shared,
 * deduplicated reference entities:</p>
 * <ul>
 *     <li>{@link Category} and {@link Brand} — {@code @ManyToOne};</li>
 *     <li>{@link Ingredient}, {@link Allergen}, {@link Additive} — {@code @ManyToMany}.</li>
 * </ul>
 *
 * <p>Every association is {@link FetchType#LAZY} and carries <strong>no cascade</strong>:
 * reference rows are persisted once, up front, by the ETL. A product merely links to
 * pre-existing references, so it never triggers a cascading insert/merge of them.</p>
 *
 * <p>The many-to-many collections are the owning sides; the FK columns and the
 * {@code nutritionGradeFr} column are indexed to keep the "top-N" queries fast.</p>
 */
@Entity
@Table(name = "product", indexes = {
        @Index(name = "idx_product_brand", columnList = "brand_id"),
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_grade", columnList = "nutrition_grade_fr")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, length = 1024)
    private String nom;

    /** Nutri-Score grade a..e (a is best). {@code null} when unknown — sorts last. */
    @Column(name = "nutrition_grade_fr", length = 1)
    private String nutritionGradeFr;

    /** Palm-oil presence flag from the source (0 = absent, 1 = present). */
    @Column(name = "presence_huile_palme")
    private Integer presenceHuilePalme;

    @Embedded
    private Nutriments nutriments = new Nutriments();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", foreignKey = @jakarta.persistence.ForeignKey(name = "fk_product_category"))
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", foreignKey = @jakarta.persistence.ForeignKey(name = "fk_product_brand"))
    private Brand brand;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "product_ingredient",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "ingredient_id"))
    private Set<Ingredient> ingredients = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "product_allergen",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "allergen_id"))
    private Set<Allergen> allergens = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "product_additive",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "additive_id"))
    private Set<Additive> additives = new HashSet<>();

    public Product() {
    }

    public Product(String nom) {
        this.nom = nom;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getNutritionGradeFr() {
        return nutritionGradeFr;
    }

    public void setNutritionGradeFr(String nutritionGradeFr) {
        this.nutritionGradeFr = nutritionGradeFr;
    }

    public Integer getPresenceHuilePalme() {
        return presenceHuilePalme;
    }

    public void setPresenceHuilePalme(Integer presenceHuilePalme) {
        this.presenceHuilePalme = presenceHuilePalme;
    }

    public Nutriments getNutriments() {
        return nutriments;
    }

    public void setNutriments(Nutriments nutriments) {
        this.nutriments = nutriments;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Brand getBrand() {
        return brand;
    }

    public void setBrand(Brand brand) {
        this.brand = brand;
    }

    public Set<Ingredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(Set<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

    public Set<Allergen> getAllergens() {
        return allergens;
    }

    public void setAllergens(Set<Allergen> allergens) {
        this.allergens = allergens;
    }

    public Set<Additive> getAdditives() {
        return additives;
    }

    public void setAdditives(Set<Additive> additives) {
        this.additives = additives;
    }

    /**
     * Identity equality: products have no natural business key (names repeat),
     * so equality is based on the surrogate id once it is assigned.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Product product)) {
            return false;
        }
        return id != null && id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Product{id=" + id + ", nom='" + nom + "', grade='" + nutritionGradeFr + "'}";
    }
}
