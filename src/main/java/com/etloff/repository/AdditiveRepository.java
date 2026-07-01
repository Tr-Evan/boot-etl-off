package com.etloff.repository;

import com.etloff.dto.NameCountDto;
import com.etloff.model.Additive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Data-access layer for the unique {@link Additive} references. */
public interface AdditiveRepository extends JpaRepository<Additive, Long> {

    /**
     * The most frequently used additives across all products.
     *
     * @param pageable page holding the desired limit
     * @return additive labels with their product counts, most frequent first
     */
    @Query("""
            SELECT new com.etloff.dto.NameCountDto(a.name, COUNT(p.id))
            FROM Additive a JOIN a.products p
            GROUP BY a.id, a.name
            ORDER BY COUNT(p.id) DESC
            """)
    List<NameCountDto> findMostFrequent(Pageable pageable);
}
