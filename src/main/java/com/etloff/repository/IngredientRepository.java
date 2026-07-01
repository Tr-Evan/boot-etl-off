package com.etloff.repository;

import com.etloff.dto.NameCountDto;
import com.etloff.model.Ingredient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Data-access layer for the unique {@link Ingredient} references. */
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    /**
     * The most frequently used ingredients across all products.
     *
     * @param pageable page holding the desired limit (use {@code PageRequest.of(0, limit)})
     * @return ingredient names with their product counts, most frequent first
     */
    @Query("""
            SELECT new com.etloff.dto.NameCountDto(i.name, COUNT(p.id))
            FROM Ingredient i JOIN i.products p
            GROUP BY i.id, i.name
            ORDER BY COUNT(p.id) DESC
            """)
    List<NameCountDto> findMostFrequent(Pageable pageable);
}
