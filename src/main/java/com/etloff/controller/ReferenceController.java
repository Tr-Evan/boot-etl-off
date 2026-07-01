package com.etloff.controller;

import com.etloff.dto.NameCountDto;
import com.etloff.service.ReferenceQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints exposing the "most frequent" rankings for ingredients, allergens and
 * additives.
 *
 * <p>The paths ({@code /ingredients/top}, {@code /allergens/top}, {@code /additives/top})
 * are declared directly on the handler methods, so a single controller cleanly serves all
 * three reference families.</p>
 */
@RestController
public class ReferenceController {

    private static final int DEFAULT_LIMIT = 10;

    private final ReferenceQueryService referenceQueryService;

    public ReferenceController(ReferenceQueryService referenceQueryService) {
        this.referenceQueryService = referenceQueryService;
    }

    /**
     * {@code GET /ingredients/top?limit=N} — the N most frequent ingredients.
     *
     * @param limit maximum number of results (default 10)
     * @return ingredient names with their product counts
     */
    @GetMapping("/ingredients/top")
    public List<NameCountDto> topIngredients(@RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        return referenceQueryService.topIngredients(limit);
    }

    /**
     * {@code GET /allergens/top?limit=N} — the N most frequent allergens.
     *
     * @param limit maximum number of results (default 10)
     * @return allergen names with their product counts
     */
    @GetMapping("/allergens/top")
    public List<NameCountDto> topAllergens(@RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        return referenceQueryService.topAllergens(limit);
    }

    /**
     * {@code GET /additives/top?limit=N} — the N most frequent additives.
     *
     * @param limit maximum number of results (default 10)
     * @return additive labels with their product counts
     */
    @GetMapping("/additives/top")
    public List<NameCountDto> topAdditives(@RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        return referenceQueryService.topAdditives(limit);
    }
}
