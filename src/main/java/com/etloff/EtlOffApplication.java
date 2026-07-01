package com.etloff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Entry point of the {@code etl-off} application.
 *
 * <p>Bootstraps an Open Food Facts ingestion pipeline (ETL) tuned for throughput
 * — Java 21 virtual threads for parallelism, an in-memory reference cache to
 * eliminate repeated {@code SELECT}s, and Hibernate JDBC batching for inserts —
 * together with a small read-optimized REST API.</p>
 */
@SpringBootApplication
@EnableCaching
public class EtlOffApplication {

    public static void main(String[] args) {
        SpringApplication.run(EtlOffApplication.class, args);
    }
}
