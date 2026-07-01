package com.etloff.service;

import com.etloff.dto.NameCountDto;
import com.etloff.repository.AdditiveRepository;
import com.etloff.repository.AllergenRepository;
import com.etloff.repository.IngredientRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only service backing the "most frequent" endpoints for ingredients, allergens and
 * additives. Results are cached by limit since the ingested data is immutable.
 */
@Service
public class ReferenceQueryService {

    private static final int MAX_LIMIT = 1000;

    private final IngredientRepository ingredientRepository;
    private final AllergenRepository allergenRepository;
    private final AdditiveRepository additiveRepository;

    public ReferenceQueryService(IngredientRepository ingredientRepository,
                                 AllergenRepository allergenRepository,
                                 AdditiveRepository additiveRepository) {
        this.ingredientRepository = ingredientRepository;
        this.allergenRepository = allergenRepository;
        this.additiveRepository = additiveRepository;
    }

    /**
     * @param limit maximum number of ingredients
     * @return the most frequently used ingredients, most frequent first
     */
    @Cacheable("topIngredients")
    @Transactional(readOnly = true)
    public List<NameCountDto> topIngredients(int limit) {
        return ingredientRepository.findMostFrequent(page(limit));
    }

    /**
     * @param limit maximum number of allergens
     * @return the most frequently occurring allergens, most frequent first
     */
    @Cacheable("topAllergens")
    @Transactional(readOnly = true)
    public List<NameCountDto> topAllergens(int limit) {
        return allergenRepository.findMostFrequent(page(limit));
    }

    /**
     * @param limit maximum number of additives
     * @return the most frequently used additives, most frequent first
     */
    @Cacheable("topAdditives")
    @Transactional(readOnly = true)
    public List<NameCountDto> topAdditives(int limit) {
        return additiveRepository.findMostFrequent(page(limit));
    }

    private static Pageable page(int limit) {
        int safe = Math.min(Math.max(limit, 1), MAX_LIMIT);
        return PageRequest.of(0, safe);
    }
}
