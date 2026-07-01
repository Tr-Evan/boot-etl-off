package com.etloff.etl;

/**
 * Summary of a completed ETL run, returned by {@link EtlService#run()} and by the ETL
 * REST endpoint.
 *
 * @param products      number of products persisted
 * @param categories    number of distinct categories
 * @param brands        number of distinct brands
 * @param ingredients   number of distinct ingredients
 * @param allergens     number of distinct allergens
 * @param additives     number of distinct additives
 * @param failedChunks  number of chunks that failed to persist (0 on a clean run)
 * @param durationMillis total wall-clock time of the run, in milliseconds
 */
public record EtlResult(
        int products,
        int categories,
        int brands,
        int ingredients,
        int allergens,
        int additives,
        int failedChunks,
        long durationMillis) {

    /** @return a human-friendly {@code m}m {@code s}s representation of the duration. */
    public String formattedDuration() {
        long totalSeconds = durationMillis / 1000;
        return "%dm %02ds (%d ms)".formatted(totalSeconds / 60, totalSeconds % 60, durationMillis);
    }
}
