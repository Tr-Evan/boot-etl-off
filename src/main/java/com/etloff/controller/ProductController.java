package com.etloff.controller;

import com.etloff.dto.ProductSummaryDto;
import com.etloff.service.ProductQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints exposing the product "top-N" rankings, ordered by Nutri-Score.
 */
@RestController
@RequestMapping("/products")
public class ProductController {

    private static final int DEFAULT_LIMIT = 10;

    private final ProductQueryService productQueryService;

    public ProductController(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
    }

    /**
     * {@code GET /products/top-by-brand?brand=X&limit=N} — top N products of a brand.
     *
     * @param brand the brand name (required)
     * @param limit maximum number of results (default 10)
     * @return the ranked products
     */
    @GetMapping("/top-by-brand")
    public List<ProductSummaryDto> topByBrand(@RequestParam String brand,
                                              @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        return productQueryService.topByBrand(brand, limit);
    }

    /**
     * {@code GET /products/top-by-category?category=X&limit=N} — top N products of a category.
     *
     * @param category the category name (required)
     * @param limit    maximum number of results (default 10)
     * @return the ranked products
     */
    @GetMapping("/top-by-category")
    public List<ProductSummaryDto> topByCategory(@RequestParam String category,
                                                 @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        return productQueryService.topByCategory(category, limit);
    }

    /**
     * {@code GET /products/top-by-brand-category?brand=X&category=Y&limit=N} —
     * top N products matching both a brand and a category.
     *
     * @param brand    the brand name (required)
     * @param category the category name (required)
     * @param limit    maximum number of results (default 10)
     * @return the ranked products
     */
    @GetMapping("/top-by-brand-category")
    public List<ProductSummaryDto> topByBrandAndCategory(@RequestParam String brand,
                                                         @RequestParam String category,
                                                         @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        return productQueryService.topByBrandAndCategory(brand, category, limit);
    }
}
