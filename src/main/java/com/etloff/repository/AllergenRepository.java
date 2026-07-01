package com.etloff.repository;

import com.etloff.dto.NameCountDto;
import com.etloff.model.Allergen;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Data-access layer for the unique {@link Allergen} references. */
public interface AllergenRepository extends JpaRepository<Allergen, Long> {

    /**
     * The most frequently occurring allergens across all products.
     *
     * @param pageable page holding the desired limit
     * @return allergen names with their product counts, most frequent first
     */
    @Query("""
            SELECT new com.etloff.dto.NameCountDto(a.name, COUNT(p.id))
            FROM Allergen a JOIN a.products p
            GROUP BY a.id, a.name
            ORDER BY COUNT(p.id) DESC
            """)
    List<NameCountDto> findMostFrequent(Pageable pageable);
}
