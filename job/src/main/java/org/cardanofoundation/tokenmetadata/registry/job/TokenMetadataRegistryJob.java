package org.cardanofoundation.tokenmetadata.registry.job;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service.TokenMetadataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * One-shot job that triggers a single CIP-26 GitHub sync via assets-ext's
 * {@link TokenMetadataSyncService} and exits. Intended to be run from a
 * Kubernetes CronJob; when used, disable the in-process scheduled sync in
 * long-running services by setting {@code store.assets.ext.cip26.enabled=false}
 * there so the two paths don't race.
 */
@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class TokenMetadataRegistryJob implements CommandLineRunner {

    private final TokenMetadataSyncService tokenMetadataSyncService;

    public static void main(String[] args) {
        SpringApplication.run(TokenMetadataRegistryJob.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("Starting one-shot CIP-26 sync...");
        tokenMetadataSyncService.synchronizeDatabase();
        log.info("One-shot CIP-26 sync complete.");
    }

}
