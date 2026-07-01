package com.etloff.controller;

import com.etloff.etl.EtlResult;
import com.etloff.etl.EtlService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint to trigger the ingestion on demand (in addition to the automatic startup run).
 * Useful for benchmarking the ETL repeatedly without restarting the application.
 */
@RestController
@RequestMapping("/etl")
public class EtlController {

    private final EtlService etlService;

    public EtlController(EtlService etlService) {
        this.etlService = etlService;
    }

    /**
     * {@code POST /etl/run} — runs the full ETL and returns timing + counts.
     *
     * @return the {@link EtlResult} summary of the run
     */
    @PostMapping("/run")
    public EtlResult run() {
        return etlService.run();
    }
}
