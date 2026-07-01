package com.etloff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Declares the executor used to parallelize the ETL over the CSV rows.
 *
 * <p>A {@link Executors#newVirtualThreadPerTaskExecutor() virtual-thread-per-task}
 * executor is ideal here: each ingestion chunk spends most of its wall-clock time
 * blocked on JDBC I/O (flushing batches to the database). Virtual threads let us
 * dispatch hundreds of concurrent chunks without exhausting OS threads, and they
 * unmount from their carrier thread while blocked — so the real concurrency limit
 * becomes the JDBC connection pool, not the thread count.</p>
 */
@Configuration
public class ExecutorConfig {

    /**
     * Dedicated virtual-thread executor for the ETL fan-out.
     *
     * <p>Exposed as a bean so Spring closes it on shutdown. It is intentionally
     * separate from the auto-configured request executor so a long ingestion run
     * never starves the web tier.</p>
     *
     * @return a virtual-thread-per-task executor named {@code etl-vt-*}
     */
    @Bean(name = "etlExecutor", destroyMethod = "close")
    public ExecutorService etlExecutor() {
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("etl-vt-", 0).factory());
    }
}
