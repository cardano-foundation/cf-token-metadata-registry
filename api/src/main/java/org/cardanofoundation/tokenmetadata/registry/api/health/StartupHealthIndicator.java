package org.cardanofoundation.tokenmetadata.registry.api.health;

import com.bloxbean.cardano.yaci.store.common.domain.HealthStatus;
import com.bloxbean.cardano.yaci.store.core.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Startup health indicator — checks that the application has initialized correctly:
 * Flyway migrations ran (implied by Spring context being up), Yaci Store connection
 * is alive and blocks are being received. Does NOT check sync progress.
 */
@Component
@RequiredArgsConstructor
public class StartupHealthIndicator implements HealthIndicator {

    private static final String REASON = "reason";

    private final HealthService healthService;

    @Override
    public Health health() {
        HealthStatus status;
        try {
            status = healthService.getHealthStatus();
        } catch (NullPointerException _) {
            return Health.down()
                    .withDetail(REASON, "Block fetcher not initialized")
                    .build();
        }

        Health.Builder builder = new Health.Builder()
                .withDetail("connectionAlive", status.isConnectionAlive())
                .withDetail("receivingBlocks", status.isReceivingBlocks());

        if (!status.isConnectionAlive()) {
            return builder.down()
                    .withDetail(REASON, "Yaci Store connection not alive")
                    .build();
        }

        if (!status.isReceivingBlocks()) {
            return builder.down()
                    .withDetail(REASON, "Not receiving blocks from node")
                    .build();
        }

        if (status.isScheduleToStop()) {
            return builder.down()
                    .withDetail(REASON, "Sync scheduled to stop")
                    .build();
        }

        return builder.up().build();
    }

}
