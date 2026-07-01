package com.etloff.repository;

import com.etloff.model.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data-access layer for the unique {@link Brand} references. */
public interface BrandRepository extends JpaRepository<Brand, Long> {
}
