package com.etloff.etl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Kicks off the ingestion once the application context is ready, when
 * {@code etl.run-on-startup=true} (the default). The ETL can also be triggered on demand
 * via the REST endpoint {@code POST /etl/run}.
 */
@Component
public class EtlRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EtlRunner.class);

    private final EtlService etlService;
    private final boolean runOnStartup;

    public EtlRunner(EtlService etlService,
                     @org.springframework.beans.factory.annotation.Value("${etl.run-on-startup:true}") boolean runOnStartup) {
        this.etlService = etlService;
        this.runOnStartup = runOnStartup;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!runOnStartup) {
            log.info("ETL startup ingestion disabled (etl.run-on-startup=false)");
            return;
        }
        etlService.run();
    }
}
