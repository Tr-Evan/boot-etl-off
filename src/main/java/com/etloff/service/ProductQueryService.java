package com.etloff.service;

import com.etloff.dto.ProductSummaryDto;
import com.etloff.repository.ProductRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only service backing the product "top-N" endpoints.
 *
 * <p>Results are cached (Spring Cache) keyed by their parameters, since the underlying
 * data is immutable after ingestion — repeated identical queries are served from memory.</p>
 */
@Service
public class ProductQueryService {

    /** Hard upper bound on {@code limit} to protect the service from abusive requests. */
    private static final int MAX_LIMIT = 1000;

    private final ProductRepository productRepository;

    public ProductQueryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * @param brand the brand name
     * @param limit the maximum number of products to return
     * @return the top products of the brand, best Nutri-Score first
     */
    @Cacheable("topByBrand")
    @Transactional(readOnly = true)
    public List<ProductSummaryDto> topByBrand(String brand, int limit) {
        return productRepository.findTopByBrand(brand, page(limit));
    }

    /**
     * @param category the category name
     * @param limit    the maximum number of products to return
     * @return the top products of the category, best Nutri-Score first
     */
    @Cacheable("topByCategory")
    @Transactional(readOnly = true)
    public List<ProductSummaryDto> topByCategory(String category, int limit) {
        return productRepository.findTopByCategory(category, page(limit));
    }

    /**
     * @param brand    the brand name
     * @param category the category name
     * @param limit    the maximum number of products to return
     * @return the top products matching both brand and category, best Nutri-Score first
     */
    @Cacheable("topByBrandCategory")
    @Transactional(readOnly = true)
    public List<ProductSummaryDto> topByBrandAndCategory(String brand, String category, int limit) {
        return productRepository.findTopByBrandAndCategory(brand, category, page(limit));
    }

    private static Pageable page(int limit) {
        int safe = Math.min(Math.max(limit, 1), MAX_LIMIT);
        return PageRequest.of(0, safe);
    }
}
