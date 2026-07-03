package com.etloff.etl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Orchestrates the full ingestion pipeline:
 *
 * <ol>
 *     <li><b>Extract</b> — parse and clean the CSV into {@link ProductRow}s
 *         ({@link CsvParser}).</li>
 *     <li><b>Load references</b> — persist every distinct category/brand/ingredient/
 *         allergen/additive once and cache them by name
 *         ({@link ReferencePersister}).</li>
 *     <li><b>Load products (parallel)</b> — split the rows into chunks and dispatch each
 *         chunk to a virtual thread, where it is persisted in its own transaction with
 *         JDBC batching ({@link ProductBatchWriter}).</li>
 * </ol>
 *
 * <p>The heavy per-row string work happens once, single-threaded, during extraction; the
 * parallel phase is pure batched database I/O — exactly the workload virtual threads
 * excel at.</p>
 */
@Service
public class EtlService {

    private static final Logger log = LoggerFactory.getLogger(EtlService.class);

    private final CsvParser csvParser;
    private final ReferencePersister referencePersister;
    private final ProductBatchWriter productBatchWriter;
    private final ParallelChunkProcessor parallelChunkProcessor;
    private final JdbcTemplate jdbcTemplate;
    private final String csvPath;
    private final int chunkSize;
    private final int maxConcurrency;
    private final boolean disableReferentialIntegrity;

    // --- Observabilité (Micrometer / Prometheus) ---
    /** Duration of the last completed run, in milliseconds — exposed as gauge {@code etl.last.duration}. */
    private final AtomicLong lastDurationMillis = new AtomicLong(0);
    /** Products persisted by the last run — exposed as gauge {@code etl.last.products}. */
    private final AtomicLong lastProducts = new AtomicLong(0);
    /** Total number of ETL runs — exposed as counter {@code etl.runs.total}. */
    private final Counter runsCounter;

    public EtlService(CsvParser csvParser,
                      ReferencePersister referencePersister,
                      ProductBatchWriter productBatchWriter,
                      ParallelChunkProcessor parallelChunkProcessor,
                      JdbcTemplate jdbcTemplate,
                      MeterRegistry meterRegistry,
                      @Value("${etl.csv-path:open-food-facts.csv}") String csvPath,
                      @Value("${etl.chunk-size:250}") int chunkSize,
                      @Value("${etl.max-concurrency:16}") int maxConcurrency,
                      @Value("${etl.disable-referential-integrity-during-load:true}") boolean disableReferentialIntegrity) {
        this.csvParser = csvParser;
        this.referencePersister = referencePersister;
        this.productBatchWriter = productBatchWriter;
        this.parallelChunkProcessor = parallelChunkProcessor;
        this.jdbcTemplate = jdbcTemplate;
        this.csvPath = csvPath;
        this.chunkSize = chunkSize;
        this.maxConcurrency = maxConcurrency;
        this.disableReferentialIntegrity = disableReferentialIntegrity;

        meterRegistry.gauge("etl.last.duration", lastDurationMillis, AtomicLong::doubleValue);
        meterRegistry.gauge("etl.last.products", lastProducts, AtomicLong::doubleValue);
        this.runsCounter = Counter.builder("etl.runs.total")
                .description("Number of ETL ingestion runs")
                .register(meterRegistry);
    }

    /**
     * Runs the complete ETL and returns a summary of what was ingested.
     *
     * @return the {@link EtlResult} describing counts and elapsed time
     */
    public EtlResult run() {
        long startNanos = System.nanoTime();
        log.info("=== ETL START === (file={}, chunkSize={})", csvPath, chunkSize);

        // 0. Make the run idempotent so it can be re-triggered via POST /etl/run.
        clearExistingData();

        // 1. Extract + clean.
        List<ProductRow> rows = csvParser.parse(Path.of(csvPath));

        // 2. Load references and build the caches.
        ReferenceCaches caches = referencePersister.persistReferences(rows);

        // 3. Load products in parallel over virtual threads.
        int failedChunks = writeProducts(rows, caches);

        long durationMillis = (System.nanoTime() - startNanos) / 1_000_000L;
        EtlResult result = new EtlResult(
                rows.size(),
                caches.categories().size(),
                caches.brands().size(),
                caches.ingredients().size(),
                caches.allergens().size(),
                caches.additives().size(),
                failedChunks,
                durationMillis);

        publishMetrics(result);
        log.info("=== ETL DONE === {} products in {} ({} failed chunks)",
                result.products(), result.formattedDuration(), failedChunks);
        return result;
    }

    /**
     * Publishes the run's metrics to Micrometer and logs a performance summary
     * (temps total, threads, mémoire) as required by the "analyse des performances"
     * objective.
     */
    private void publishMetrics(EtlResult result) {
        lastDurationMillis.set(result.durationMillis());
        lastProducts.set(result.products());
        runsCounter.increment();

        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMb = runtime.maxMemory() / (1024 * 1024);
        log.info("Perf — durée={} ms, produits={}, débit={} produits/s, threads actifs={}, mémoire={}/{} Mo, CPU={} cœurs",
                result.durationMillis(),
                result.products(),
                result.durationMillis() > 0 ? (result.products() * 1000L / result.durationMillis()) : 0,
                Thread.activeCount(),
                usedMb, maxMb,
                runtime.availableProcessors());
    }

    /**
     * Persists the products concurrently over virtual threads (via
     * {@link ParallelChunkProcessor}), one transaction per chunk.
     *
     * <p>Referential-integrity checking is turned off for the duration of the load
     * (re-enabled in a {@code finally}), which removes the FK parent-row locks that
     * otherwise make concurrent many-to-many join-table inserts deadlock on H2. Integrity
     * is still guaranteed: every referenced id comes from the pre-built cache of
     * already-persisted rows.</p>
     *
     * @return the number of chunks that failed
     */
    private int writeProducts(List<ProductRow> rows, ReferenceCaches caches) {
        setReferentialIntegrity(false);
        try {
            return parallelChunkProcessor.process(rows, chunkSize, maxConcurrency,
                    chunk -> productBatchWriter.writeChunk(chunk, caches));
        } finally {
            setReferentialIntegrity(true);
        }
    }

    /**
     * Empties every table so a fresh ingestion can run, making {@code POST /etl/run}
     * safely repeatable. Referential integrity is toggled off around the truncates so
     * order does not matter.
     */
    private void clearExistingData() {
        setReferentialIntegrity(false);
        try {
            for (String table : new String[]{
                    "product_ingredient", "product_allergen", "product_additive",
                    "product", "category", "brand", "ingredient", "allergen", "additive"}) {
                try {
                    jdbcTemplate.execute("TRUNCATE TABLE " + table);
                } catch (RuntimeException e) {
                    log.debug("Could not truncate {} (ignored): {}", table, e.getMessage());
                }
            }
        } finally {
            setReferentialIntegrity(true);
        }
    }

    /**
     * Toggles database referential-integrity checking (H2 syntax). Best-effort: on engines
     * that do not support the statement (e.g. PostgreSQL, which handles concurrent FK
     * inserts without deadlocking anyway) the failure is logged and ignored.
     */
    private void setReferentialIntegrity(boolean enabled) {
        if (!disableReferentialIntegrity) {
            return;
        }
        try {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY " + enabled);
        } catch (RuntimeException e) {
            log.warn("Could not set referential integrity to {} (ignored): {}", enabled, e.getMessage());
        }
    }
}
