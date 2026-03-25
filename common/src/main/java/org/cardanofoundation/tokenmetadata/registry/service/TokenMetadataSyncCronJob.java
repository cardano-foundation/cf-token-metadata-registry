package org.cardanofoundation.tokenmetadata.registry.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TokenMetadataSyncCronJob implements Runnable {

    private final TokenMetadataSyncService tokenMetadataSyncService;
    private final boolean enabled;

    public TokenMetadataSyncCronJob(TokenMetadataSyncService tokenMetadataSyncService,
                                    @Value("${token.metadata.job.enabled:false}") boolean enabled) {
        this.tokenMetadataSyncService = tokenMetadataSyncService;
        this.enabled = enabled;
    }

    @Override
    @Scheduled(timeUnit = TimeUnit.MINUTES, initialDelay = 1L, fixedDelay = 60L)
    public void run() {
        if (!enabled) {
            return;
        }
        log.info("about to start syncing");
        tokenMetadataSyncService.synchronizeDatabase();
        log.info("syncing completed");
    }

    @PostConstruct
    public void logInitMessage() {
        if (enabled) {
            log.info("Cronjob initialised");
        } else {
            log.info("Cronjob disabled (token.metadata.job.enabled=false)");
        }
    }

}
