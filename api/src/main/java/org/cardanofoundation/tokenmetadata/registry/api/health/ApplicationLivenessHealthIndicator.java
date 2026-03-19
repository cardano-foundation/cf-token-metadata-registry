package org.cardanofoundation.tokenmetadata.registry.api.health;

import com.bloxbean.cardano.yaci.store.common.domain.HealthStatus;
import com.bloxbean.cardano.yaci.store.core.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Liveness health indicator that checks if the on-chain indexer is fundamentally broken.
 * If the Yaci indexer connection is lost or has errored out, the application is considered
 * broken and Kubernetes should restart the pod after the configured failureThreshold.
 */
@Component
@RequiredArgsConstructor
public class ApplicationLivenessHealthIndicator implements HealthIndicator {

    private final HealthService healthService;

    @Override
    public Health health() {
        HealthStatus status = healthService.getHealthStatus();

        if (status.isScheduleToStop()) {
            return Health.up()
                    .withDetail("indexerStatus", "Scheduled to stop")
                    .build();
        }

        if (status.isError() || !status.isConnectionAlive()) {
            return Health.down()
                    .withDetail("indexerStatus", "Connection lost or sync error")
                    .withDetail("connectionAlive", status.isConnectionAlive())
                    .withDetail("error", status.isError())
                    .build();
        }

        return Health.up()
                .withDetail("indexerStatus", "Healthy")
                .build();
    }

}
