package org.cardanofoundation.tokenmetadata.registry.api.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for on-chain token sync via Yaci indexer.
 * Covers CIP-68, CIP-113, and future on-chain token standards.
 * TODO: Implement actual Yaci Store sync status check (e.g. tip recency, indexer lag).
 */
@Component
public class OnchainSyncHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up()
                .withDetail("syncStatus", "Not yet implemented - assuming healthy")
                .build();
    }

}
