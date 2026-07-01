package com.etloff.repository;

import com.etloff.dto.ProductSummaryDto;
import com.etloff.model.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Data-access layer for {@link Product}.
 *
 * <p>All "top-N" queries return {@link ProductSummaryDto} through JPQL constructor
 * expressions so no lazy association is ever touched, and rely on {@link Pageable}
 * ({@code PageRequest.of(0, limit)}) to translate into a SQL {@code LIMIT}. Results
 * are ordered by the Nutri-Score grade ascending ({@code a} = best), with unknown
 * grades pushed to the end via {@code NULLS LAST}.</p>
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Top products of a given brand, best Nutri-Score first.
     *
     * @param brand    the exact brand name
     * @param pageable page holding the desired limit (use {@code PageRequest.of(0, limit)})
     * @return matching product summaries, best-graded first
     */
    @Query("""
            SELECT new com.etloff.dto.ProductSummaryDto(
                p.nom, b.name, c.name, p.nutritionGradeFr, p.nutriments.energie100g)
            FROM Product p
                JOIN p.brand b
                LEFT JOIN p.category c
            WHERE b.name = :brand
            ORDER BY p.nutritionGradeFr ASC NULLS LAST
            """)
    List<ProductSummaryDto> findTopByBrand(@Param("brand") String brand, Pageable pageable);

    /**
     * Top products of a given category, best Nutri-Score first.
     *
     * @param category the exact category name
     * @param pageable page holding the desired limit
     * @return matching product summaries, best-graded first
     */
    @Query("""
            SELECT new com.etloff.dto.ProductSummaryDto(
                p.nom, b.name, c.name, p.nutritionGradeFr, p.nutriments.energie100g)
            FROM Product p
                JOIN p.category c
                LEFT JOIN p.brand b
            WHERE c.name = :category
            ORDER BY p.nutritionGradeFr ASC NULLS LAST
            """)
    List<ProductSummaryDto> findTopByCategory(@Param("category") String category, Pageable pageable);

    /**
     * Top products matching both a brand and a category, best Nutri-Score first.
     *
     * @param brand    the exact brand name
     * @param category the exact category name
     * @param pageable page holding the desired limit
     * @return matching product summaries, best-graded first
     */
    @Query("""
            SELECT new com.etloff.dto.ProductSummaryDto(
                p.nom, b.name, c.name, p.nutritionGradeFr, p.nutriments.energie100g)
            FROM Product p
                JOIN p.brand b
                JOIN p.category c
            WHERE b.name = :brand AND c.name = :category
            ORDER BY p.nutritionGradeFr ASC NULLS LAST
            """)
    List<ProductSummaryDto> findTopByBrandAndCategory(@Param("brand") String brand,
                                                      @Param("category") String category,
                                                      Pageable pageable);
}
