package com.etloff.etl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

/**
 * Generic fan-out helper: partitions a list into chunks and processes each chunk on the
 * shared virtual-thread executor, bounding the number of chunks running at once.
 *
 * <p>Factored out so both ETL write phases — reference persistence and product
 * persistence — share the exact same parallelism, back-pressure and error-collection
 * logic instead of duplicating it.</p>
 */
@Component
public class ParallelChunkProcessor {

    private static final Logger log = LoggerFactory.getLogger(ParallelChunkProcessor.class);

    private final ExecutorService executor;

    public ParallelChunkProcessor(ExecutorService etlExecutor) {
        this.executor = etlExecutor;
    }

    /**
     * Splits {@code items} into chunks of {@code chunkSize} and runs {@code action} on each
     * chunk concurrently (at most {@code maxConcurrency} at a time). Blocks until all chunks
     * finish.
     *
     * @param items          the items to process
     * @param chunkSize      number of items per chunk
     * @param maxConcurrency maximum chunks processed simultaneously
     * @param action         the processing applied to each chunk (typically a transactional write)
     * @param <T>            the item type
     * @return the number of chunks that failed (0 on a clean run)
     */
    public <T> int process(List<T> items, int chunkSize, int maxConcurrency, Consumer<List<T>> action) {
        List<List<T>> chunks = partition(items, Math.max(1, chunkSize));
        Semaphore permits = new Semaphore(Math.max(1, maxConcurrency));
        List<Future<?>> futures = new ArrayList<>(chunks.size());

        for (List<T> chunk : chunks) {
            futures.add(executor.submit(() -> {
                permits.acquire();
                try {
                    action.accept(chunk);
                    return null;
                } finally {
                    permits.release();
                }
            }));
        }

        int failures = 0;
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                failures++;
                log.error("A chunk failed to process", e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while awaiting chunk completion", e);
            }
        }
        return failures;
    }

    /** Partitions a list into consecutive {@link List#subList(int, int)} views (no copy). */
    private static <T> List<List<T>> partition(List<T> source, int size) {
        List<List<T>> chunks = new ArrayList<>((source.size() + size - 1) / size);
        for (int start = 0; start < source.size(); start += size) {
            chunks.add(source.subList(start, Math.min(start + size, source.size())));
        }
        return chunks;
    }
}
