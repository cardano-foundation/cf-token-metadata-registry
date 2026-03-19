package org.cardanofoundation.tokenmetadata.registry.api.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom liveness health indicator for internal application state.
 * Liveness checks should only reflect whether the app is fundamentally broken
 * and needs a restart — never external dependencies (DB, indexer, etc.).
 * TODO: Implement checks such as thread deadlock detection, memory pressure, etc.
 */
@Component
public class ApplicationLivenessHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up().build();
    }

}
