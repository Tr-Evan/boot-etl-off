package com.etloff.repository;

import com.etloff.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data-access layer for the unique {@link Category} references. */
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
